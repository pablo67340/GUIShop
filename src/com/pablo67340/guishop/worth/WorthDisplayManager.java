package com.pablo67340.guishop.worth;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.WorthConfig;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.gui.GUIHolder;
import com.pablo67340.guishop.util.PDCUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import com.pablo67340.guishop.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Manages the display of item worth in item lore using PacketEvents packet interception.
 * This modifies items client-side only, preserving server-side item data and stacking behavior.
 * 
 * Approach: STRIP AND RE-ADD
 * - We intercept ALL outgoing SET_SLOT and WINDOW_ITEMS packets
 * - We STRIP any existing worth lines from item lore
 * - We ADD fresh worth lore based on current price and stack size
 * - This ensures worth is always correct and never duplicated
 */
public class WorthDisplayManager {

    @Getter
    private static WorthDisplayManager instance;

    private final GUIShop plugin;
    private PacketListenerAbstract packetListener;
    private Listener eventListener;

    @Getter
    private boolean registered = false;

    /**
     * Players who have disabled worth display via command (session-only, resets on reboot).
     */
    private final Set<UUID> sessionDisabledPlayers = new HashSet<>();

    /**
     * External predicate for checking if worth should be displayed for a player.
     * Other plugins can set this to provide persistent per-player settings.
     * Return true to DISABLE worth display for the player, false to allow it.
     */
    private Predicate<Player> externalDisableCheck = null;

    /**
     * The prefix pattern used to identify and strip existing worth lines.
     * This is the text BEFORE the price, stripped of color codes.
     */
    private String worthPrefix = "worth:";

    public WorthDisplayManager(GUIShop plugin) {
        this.plugin = plugin;
        instance = this;
    }

    // ==================== Per-Player Worth Toggle API ====================

    public boolean isWorthEnabledForPlayer(Player player) {
        if (player == null) return true;

        if (sessionDisabledPlayers.contains(player.getUniqueId())) {
            return false;
        }

        if (externalDisableCheck != null) {
            try {
                if (externalDisableCheck.test(player)) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogUtil().debugLog("External worth check failed for " + player.getName() + ": " + e.getMessage());
            }
        }

        return true;
    }

    public boolean toggleWorthForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (sessionDisabledPlayers.contains(uuid)) {
            sessionDisabledPlayers.remove(uuid);
            return true;
        } else {
            sessionDisabledPlayers.add(uuid);
            return false;
        }
    }

    public void enableWorthForPlayer(Player player) {
        sessionDisabledPlayers.remove(player.getUniqueId());
    }

    public void disableWorthForPlayer(Player player) {
        sessionDisabledPlayers.add(player.getUniqueId());
    }

    public boolean isSessionDisabled(Player player) {
        return sessionDisabledPlayers.contains(player.getUniqueId());
    }

    public void setExternalDisableCheck(Predicate<Player> check) {
        this.externalDisableCheck = check;
    }

    public Predicate<Player> getExternalDisableCheck() {
        return this.externalDisableCheck;
    }

    public void clearSessionToggles() {
        sessionDisabledPlayers.clear();
    }

    // ==================== End Per-Player API ====================

    /**
     * Initialize and register the packet listeners using PacketEvents.
     */
    public void register() {
        if (!WorthConfig.isEnabled()) {
            plugin.getLogUtil().debugLog("Worth display is disabled in config.");
            return;
        }

        // Check if PacketEvents is available
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
        } catch (ClassNotFoundException e) {
            plugin.getLogUtil().log("PacketEvents not found - worth display disabled. Please install PacketEvents.");
            return;
        }

        // Extract the worth prefix pattern from config for stripping
        String format = WorthConfig.getFormat();
        // Get everything before %worth% and strip color codes
        int idx = format.toLowerCase().indexOf("%worth%");
        if (idx > 0) {
            worthPrefix = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', format.substring(0, idx))).toLowerCase().trim();
        }

        // Create packet listener
        packetListener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                // Runtime config check - allows disabling via /gs reload without restart
                if (!WorthConfig.isEnabled()) {
                    return;
                }
                
                if (!(event.getPlayer() instanceof Player)) return;
                Player player = (Player) event.getPlayer();

                // Check per-player worth toggle
                if (!isWorthEnabledForPlayer(player)) {
                    return;
                }

                if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    handleWindowItems(event, player);
                } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    handleSetSlot(event, player);
                }
            }

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                // Runtime config check - allows disabling via /gs reload without restart
                if (!WorthConfig.isEnabled()) {
                    return;
                }
                
                if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                    if (!(event.getPlayer() instanceof Player)) return;
                    Player player = (Player) event.getPlayer();
                    
                    if (!isWorthEnabledForPlayer(player)) return;
                    
                    // Skip refresh for creative mode to avoid desync
                    if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                        return;
                    }
                    
                    // Refresh inventory after 3 ticks (like the working plugin does)
                    SchedulerUtil.runAtEntityLater(player, () -> {
                        if (player.isOnline()) {
                            player.updateInventory();
                        }
                    }, 3L);
                } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
                    // When player takes/places items in creative mode, we need to refresh
                    // to apply worth lore (creative inventory doesn't use normal SET_SLOT flow)
                    if (!(event.getPlayer() instanceof Player)) return;
                    Player player = (Player) event.getPlayer();
                    
                    if (!isWorthEnabledForPlayer(player)) return;
                    
                    // Schedule a single-slot update after the action completes
                    // Use a slightly longer delay to ensure the server has processed the action
                    SchedulerUtil.runAtEntityLater(player, () -> {
                        if (player.isOnline() && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                            // Send individual SET_SLOT packets for player inventory slots with worth
                            sendCreativeWorthUpdate(player);
                        }
                    }, 2L);
                }
            }
        };

        try {
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogUtil().log("Failed to register PacketEvents listener - worth display disabled: " + e.getMessage());
            packetListener = null;
            return;
        }

        // Register event listener for inventory close, item drop, and clicks
        eventListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onInventoryClose(InventoryCloseEvent event) {
                HumanEntity entity = event.getPlayer();
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    // Skip for creative mode to avoid desync
                    if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                        return;
                    }
                    if (isWorthEnabledForPlayer(player)) {
                        // Delay to ensure inventory is fully closed
                        SchedulerUtil.runAtEntityLater(player, player::updateInventory, 1L);
                    }
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerDropItem(PlayerDropItemEvent event) {
                if (event.isCancelled()) return;
                Player player = event.getPlayer();
                // Skip for creative mode to avoid desync
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                    return;
                }
                if (isWorthEnabledForPlayer(player)) {
                    player.updateInventory();
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player)) return;
                Player player = (Player) event.getWhoClicked();
                
                if (!isWorthEnabledForPlayer(player)) return;
                
                // Skip inventory refresh for creative mode - causes item desync issues
                // Worth lore still displays fine, just don't refresh on clicks
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                    return;
                }
                
                // Refresh inventory after any click to ensure worth is updated
                SchedulerUtil.runAtEntityLater(player, () -> {
                    if (player.isOnline()) {
                        player.updateInventory();
                        if (WorthConfig.isDebug()) {
                            plugin.getLogUtil().debugLog("CLICK: Refreshed inventory for " + player.getName());
                        }
                    }
                }, 1L);
            }
        };
        Bukkit.getPluginManager().registerEvents(eventListener, plugin);

        registered = true;
        plugin.getLogUtil().log("Worth display system enabled (using PacketEvents).");
    }

    /**
     * Handle WINDOW_ITEMS packet (full inventory update).
     */
    private void handleWindowItems(PacketSendEvent event, Player player) {
        WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
        int windowId = wrapper.getWindowId();
        
        // Check if this is an inventory we should skip (based on what the player has open)
        if (shouldSkipPlayerInventory(player)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("WINDOW_ITEMS: Skipped - blacklisted inventory");
            }
            return;
        }

        List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
        List<com.github.retrooper.packetevents.protocol.item.ItemStack> newItems = new ArrayList<>();

        boolean hideArmor = WorthConfig.isHideArmorSlots();
        boolean isPlayerInventory = (windowId == 0);

        for (int i = 0; i < items.size(); i++) {
            com.github.retrooper.packetevents.protocol.item.ItemStack packetItem = items.get(i);
            
            // Debug: Log armor items and their indices
            if (WorthConfig.isDebug() && isPlayerInventory && packetItem != null) {
                ItemStack debugItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
                if (debugItem != null && isArmorMaterial(debugItem.getType())) {
                    plugin.getLogUtil().debugLog("WINDOW_ITEMS: Found " + debugItem.getType() + " at index " + i + " (isArmorSlot=" + isArmorSlot(i) + ")");
                }
            }
            
            // For armor slots (5-8), strip any existing worth lore instead of adding it
            if (isPlayerInventory && hideArmor && isArmorSlot(i)) {
                if (packetItem != null) {
                    // Actively strip worth lore from equipped armor
                    com.github.retrooper.packetevents.protocol.item.ItemStack stripped = stripWorthFromPacketItem(packetItem);
                    if (WorthConfig.isDebug()) {
                        ItemStack debugItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
                        if (debugItem != null && !debugItem.getType().isAir()) {
                            plugin.getLogUtil().debugLog("WINDOW_ITEMS: Stripped worth from armor slot " + i + " (" + debugItem.getType() + ")");
                        }
                    }
                    newItems.add(stripped);
                } else {
                    newItems.add(packetItem);
                }
                continue;
            }

            if (packetItem != null) {
                com.github.retrooper.packetevents.protocol.item.ItemStack processed = processPacketItem(packetItem);
                newItems.add(processed);
            } else {
                newItems.add(packetItem);
            }
        }

        wrapper.setItems(newItems);

        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("WINDOW_ITEMS: Processed " + newItems.size() + " items for " + player.getName() + " windowId=" + windowId);
        }
    }
    
    /**
     * Check if we should skip processing for this player based on their open inventory.
     */
    private boolean shouldSkipPlayerInventory(Player player) {
        // Check if player has an inventory open
        if (player.getOpenInventory() != null) {
            // Skip all GUIShop inventories (Menu, Shop, etc.) - no worth display in shop GUIs
            // Note: On Folia, getHolder() can throw/log errors if called from async Netty thread
            // because it tries to access world data. Only check holder on main thread.
            if (org.bukkit.Bukkit.isPrimaryThread()) {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof GUIHolder) {
                    if (WorthConfig.isDebug()) {
                        plugin.getLogUtil().debugLog("BLACKLIST CHECK: Skipping GUIShop inventory");
                    }
                    return true;
                }
            } else {
                // On async thread (Netty IO), we can't safely check holder
                // Rely on title-based blacklisting instead
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("BLACKLIST CHECK: Skipped holder check (async thread)");
                }
            }
            
            // Note: We allow worth lore in creative inventory - it displays correctly
            // Only the updateInventory() calls are skipped for creative mode to avoid desync
            
            String title = player.getOpenInventory().getTitle();
            if (title != null) {
                // Strip color codes and normalize for comparison
                String normalizedTitle = stripForComparison(title);
                
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("BLACKLIST CHECK: Raw title='" + title + "'");
                    plugin.getLogUtil().debugLog("BLACKLIST CHECK: Normalized title='" + normalizedTitle + "'");
                }
                
                // Check blacklisted inventories
                for (String blacklisted : WorthConfig.getBlacklistedInventories()) {
                    String normalizedBlacklist = stripForComparison(blacklisted);
                    
                    // Check both the raw title and normalized title for matches
                    boolean rawMatch = title.toLowerCase().contains(blacklisted.toLowerCase());
                    boolean normalizedMatch = normalizedTitle.contains(normalizedBlacklist);
                    
                    if (rawMatch || normalizedMatch) {
                        if (WorthConfig.isDebug()) {
                            plugin.getLogUtil().debugLog("BLACKLIST CHECK: Matched '" + blacklisted + "' (raw=" + rawMatch + ", normalized=" + normalizedMatch + ")");
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Strip color codes, unicode characters, and normalize string for comparison.
     * This helps match inventory titles that contain unicode icons or special characters.
     */
    private String stripForComparison(String input) {
        if (input == null) return "";
        
        String stripped = input;
        
        // Handle UTF-8 encoding issue where § appears as Â§
        stripped = stripped.replace("Â§", "§");
        
        // Strip hex color codes first (§x§R§R§G§G§B§B format - 14 chars total)
        // This format applies color per-character, need to remove aggressively
        stripped = stripped.replaceAll("§x(§[0-9a-fA-F]){6}", "");
        
        // Strip standard Minecraft color codes (§ and &)
        stripped = stripped.replaceAll("[§&][0-9a-fk-orxA-FK-ORX]", "");
        
        // Strip &#RRGGBB format
        stripped = stripped.replaceAll("&#[0-9a-fA-F]{6}", "");
        
        // Remove any remaining § or & followed by anything (catch-all)
        stripped = stripped.replaceAll("[§&].", "");
        
        // Extract only letters, numbers, and spaces - remove everything else
        StringBuilder result = new StringBuilder();
        for (char c : stripped.toCharArray()) {
            if (Character.isLetter(c) || Character.isDigit(c)) {
                result.append(c);
            } else if (Character.isWhitespace(c)) {
                result.append(' ');
            }
            // Skip all other characters (unicode symbols, punctuation, etc.)
        }
        
        // Normalize whitespace and convert to lowercase
        return result.toString().replaceAll("\\s+", " ").trim().toLowerCase();
    }

    /**
     * Handle SET_SLOT packet (single slot update).
     */
    private void handleSetSlot(PacketSendEvent event, Player player) {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        int windowId = wrapper.getWindowId();
        int slot = wrapper.getSlot();

        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("SET_SLOT: Received windowId=" + windowId + " slot=" + slot);
        }

        // Skip invalid slots (like cursor slot -1)
        if (slot < 0) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("SET_SLOT: Skipped (slot=" + slot + ")");
            }
            return;
        }
        
        // Check if this is a blacklisted inventory
        if (shouldSkipPlayerInventory(player)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("SET_SLOT: Skipped - blacklisted inventory");
            }
            return;
        }

        // Skip armor slots if configured - only for player inventory window
        boolean isPlayerInventory = (windowId == 0);
        if (isPlayerInventory && WorthConfig.isHideArmorSlots()) {
            // Debug: Log all slots for armor-type items
            if (WorthConfig.isDebug()) {
                com.github.retrooper.packetevents.protocol.item.ItemStack debugItem = wrapper.getItem();
                if (debugItem != null) {
                    ItemStack debugBukkit = SpigotConversionUtil.toBukkitItemStack(debugItem);
                    if (debugBukkit != null && isArmorMaterial(debugBukkit.getType())) {
                        plugin.getLogUtil().debugLog("SET_SLOT: Armor item " + debugBukkit.getType() + " at slot=" + slot + " (windowId=" + windowId + ")");
                    }
                }
            }
            
            if (isArmorSlot(slot)) {
                // Actively strip worth lore from equipped armor
                com.github.retrooper.packetevents.protocol.item.ItemStack armorItem = wrapper.getItem();
                if (armorItem != null) {
                    com.github.retrooper.packetevents.protocol.item.ItemStack stripped = stripWorthFromPacketItem(armorItem);
                    wrapper.setItem(stripped);
                    if (WorthConfig.isDebug()) {
                        plugin.getLogUtil().debugLog("SET_SLOT: Stripped worth from armor slot=" + slot);
                    }
                }
                return;
            }
        }

        com.github.retrooper.packetevents.protocol.item.ItemStack packetItem = wrapper.getItem();
        if (packetItem == null) return;

        com.github.retrooper.packetevents.protocol.item.ItemStack processed = processPacketItem(packetItem);
        wrapper.setItem(processed);

        if (WorthConfig.isDebug()) {
            ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
            if (bukkitItem != null && !bukkitItem.getType().isAir()) {
                plugin.getLogUtil().debugLog("SET_SLOT: Modified " + bukkitItem.getType() + " x" + bukkitItem.getAmount() + " slot=" + slot);
            }
        }
    }

    /**
     * Check if the slot is an armor slot.
     * In player inventory (window 0):
     * - Slot 5: Helmet
     * - Slot 6: Chestplate  
     * - Slot 7: Leggings
     * - Slot 8: Boots
     */
    private boolean isArmorSlot(int slot) {
        return slot >= 5 && slot <= 8;
    }
    
    /**
     * Check if the material is an armor piece (for debug logging).
     */
    private boolean isArmorMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
               name.equals("TURTLE_HELMET") || name.equals("ELYTRA");
    }
    
    /**
     * Strip worth lore from a PacketEvents ItemStack without adding it back.
     * Used for equipped armor when hide-armor-slots is enabled.
     */
    private com.github.retrooper.packetevents.protocol.item.ItemStack stripWorthFromPacketItem(
            com.github.retrooper.packetevents.protocol.item.ItemStack packetItem) {
        
        // Convert to Bukkit ItemStack for processing
        ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
        if (bukkitItem == null || bukkitItem.getType().isAir()) {
            return packetItem;
        }

        // Strip worth lore only (don't add it back)
        ItemStack stripped = stripWorthLore(bukkitItem);

        // Convert back to PacketEvents ItemStack
        return SpigotConversionUtil.fromBukkitItemStack(stripped);
    }
    
    /**
     * Send worth-enhanced SET_SLOT packets for creative mode inventory.
     * This is needed because creative inventory actions don't go through normal packet flow.
     */
    private void sendCreativeWorthUpdate(Player player) {
        try {
            // Only process hotbar and main inventory (slots 0-35 in player inventory)
            // In protocol terms: hotbar is 36-44, main inventory is 9-35, armor is 5-8
            org.bukkit.inventory.PlayerInventory inv = player.getInventory();
            
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType().isAir()) continue;
                
                // Check if this item has a sell price
                Item shopItem = findShopItem(item);
                if (shopItem == null || !shopItem.hasSellPrice()) continue;
                
                // Process the item to add worth lore
                ItemStack processed = processItem(item.clone());
                if (processed == null) continue;
                
                // Convert slot index to protocol slot
                // Player inventory slots: 9-35 are main inventory, 36-44 are hotbar
                int protocolSlot;
                if (i < 9) {
                    // Hotbar: 0-8 in inventory -> 36-44 in protocol
                    protocolSlot = 36 + i;
                } else {
                    // Main inventory: 9-35 in inventory -> 9-35 in protocol
                    protocolSlot = i;
                }
                
                // Send SET_SLOT packet
                    com.github.retrooper.packetevents.protocol.item.ItemStack packetItem = 
                    SpigotConversionUtil.fromBukkitItemStack(processed);
                WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(
                    0, // Window ID 0 = player inventory
                    0, // State ID (not used in older versions, safe to use 0)
                    protocolSlot,
                    packetItem
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, setSlot);
                
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("CREATIVE: Sent worth update for slot " + i + " -> protocol " + protocolSlot);
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            // Safely handle if PacketEvents becomes unavailable
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("CREATIVE: Error sending worth update: " + e.getMessage());
            }
        }
    }

    /**
     * Process a PacketEvents ItemStack: strip existing worth lore and add fresh worth.
     */
    private com.github.retrooper.packetevents.protocol.item.ItemStack processPacketItem(
            com.github.retrooper.packetevents.protocol.item.ItemStack packetItem) {
        
        // Convert to Bukkit ItemStack for processing
        ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
        if (bukkitItem == null || bukkitItem.getType().isAir()) {
            return packetItem;
        }

        // Clone for safety
        ItemStack clonedItem = bukkitItem.clone();

        // Process the item
        ItemStack processed = processItem(clonedItem);
        if (processed == null) {
            return packetItem;
        }

        // Convert back to PacketEvents ItemStack
        return SpigotConversionUtil.fromBukkitItemStack(processed);
    }

    /**
     * Process an item: strip any existing worth lore and add fresh worth.
     * 
     * @param item The item to process
     * @return The processed item with updated worth lore, or null if no changes needed
     */
    private ItemStack processItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        // Check if item is marked as a GUI element (should not show worth)
        if (isGuiElement(item)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("PROCESS: " + item.getType() + " is GUI element, skipping worth");
            }
            return stripWorthLore(item); // Just strip, don't add
        }

        // Check if item name is blacklisted
        if (hasBlacklistedName(item)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("PROCESS: " + item.getType() + " blacklisted by name");
            }
            return stripWorthLore(item); // Just strip, don't add
        }

        // Find the shop item for this material
        Item shopItem = findShopItem(item);
        
        // If no shop item or no sell price, handle accordingly
        if (shopItem == null || !shopItem.hasSellPrice()) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("PROCESS: " + item.getType() + " no shop item or no sell price");
            }
            if (WorthConfig.isOnlyShowSellable()) {
                return stripWorthLore(item);
            } else {
                // Add "not sellable" lore
                return processItemWithLore(item, formatNotSellableLine());
            }
        }

        // Calculate price
        BigDecimal totalWorth = shopItem.calculateSellPrice(item.getAmount());
        if (totalWorth.doubleValue() <= 0) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("PROCESS: " + item.getType() + " worth is 0 or less");
            }
            if (WorthConfig.isOnlyShowSellable()) {
                return stripWorthLore(item);
            } else {
                return processItemWithLore(item, formatNotSellableLine());
            }
        }

        // Format the worth line
        BigDecimal singleWorth = shopItem.calculateSellPrice(1);
        String worthLine = formatWorthLine(totalWorth, singleWorth, item.getAmount());

        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("PROCESS: " + item.getType() + " x" + item.getAmount() + " worth=" + totalWorth);
        }

        return processItemWithLore(item, worthLine);
    }

    /**
     * Process an item: strip existing worth lore and add the specified line.
     */
    private ItemStack processItemWithLore(ItemStack item, String newWorthLine) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return null;
        }

        List<String> lore;
        if (meta.hasLore() && meta.getLore() != null) {
            lore = new ArrayList<>(meta.getLore());
            // Strip any existing worth lines
            stripWorthFromLore(lore);
        } else {
            lore = new ArrayList<>();
        }

        // Add the new worth line
        if (WorthConfig.isPositionTop()) {
            if (WorthConfig.isAddBlankLine() && !lore.isEmpty()) {
                lore.add(0, "");
            }
            lore.add(0, newWorthLine);
        } else {
            if (WorthConfig.isAddBlankLine() && !lore.isEmpty()) {
                lore.add("");
            }
            lore.add(newWorthLine);
        }

        meta.setLore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Strip any worth-related lines from the lore list.
     */
    private void stripWorthFromLore(List<String> lore) {
        Iterator<String> iterator = lore.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line == null) continue;
            
            String stripped = ChatColor.stripColor(line).toLowerCase().trim();
            
            // Remove lines that match our worth pattern
            if (stripped.startsWith(worthPrefix) || 
                stripped.contains("worth:") ||
                stripped.contains("not sellable")) {
                iterator.remove();
            }
        }
        
        // Also remove trailing empty lines that were added as separators
        while (!lore.isEmpty() && (lore.get(lore.size() - 1) == null || lore.get(lore.size() - 1).isEmpty())) {
            lore.remove(lore.size() - 1);
        }
        while (!lore.isEmpty() && (lore.get(0) == null || lore.get(0).isEmpty())) {
            lore.remove(0);
        }
    }

    /**
     * Strip worth lore from an item without adding new worth.
     */
    private ItemStack stripWorthLore(ItemStack item) {
        if (!item.hasItemMeta()) {
            return item; // Return as-is if no meta
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return item;
        }

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return item;
        }

        int originalSize = lore.size();
        List<String> newLore = new ArrayList<>(lore);
        stripWorthFromLore(newLore);

        // Only return modified item if we actually stripped something
        if (newLore.size() != originalSize) {
            ItemStack clone = item.clone();
            ItemMeta cloneMeta = clone.getItemMeta();
            if (cloneMeta != null) {
                cloneMeta.setLore(newLore.isEmpty() ? null : newLore);
                clone.setItemMeta(cloneMeta);
                return clone;
            }
        }

        return item;
    }

    /**
     * Find a matching shop item for the given ItemStack.
     */
    private Item findShopItem(ItemStack item) {
        String materialKey = Item.getItemStringForItemStack(item);

        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("FIND_SHOP_ITEM: Looking for " + materialKey + 
                " (type=" + item.getType() + ")");
        }

        List<Item> itemList = plugin.getITEMTABLE().get(materialKey);

        if (itemList == null) {
            itemList = plugin.getITEMTABLE().get(item.getType().toString());
            if (WorthConfig.isDebug() && itemList != null) {
                plugin.getLogUtil().debugLog("FIND_SHOP_ITEM: Found via fallback key " + item.getType());
            }
        }

        if (itemList == null || itemList.isEmpty()) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("FIND_SHOP_ITEM: No items registered for " + materialKey);
            }
            return null;
        }

        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("FIND_SHOP_ITEM: Found " + itemList.size() + " candidates for " + materialKey);
        }

        for (Item shopItem : itemList) {
            if (shopItem.isItemFromItemStack(item)) {
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("FIND_SHOP_ITEM: Match found - " + shopItem.getMaterial());
                }
                return shopItem;
            }
        }

        return itemList.get(0);
    }

    /**
     * Check if the item has a display name that is blacklisted.
     */
    private boolean hasBlacklistedName(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String displayName = meta.getDisplayName();
        if (displayName == null || displayName.isEmpty()) return false;

        String strippedName = ChatColor.stripColor(displayName).toLowerCase();

        List<String> blacklist = WorthConfig.getBlacklistedItemNames();
        for (String blacklisted : blacklist) {
            if (strippedName.contains(blacklisted.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the item is marked as a GUI element (should not display worth).
     * GUI elements like buttons, indicators, and player heads are marked with a PDC tag.
     */
    private boolean isGuiElement(ItemStack item) {
        String guiElement = PDCUtil.getString(item, PDCUtil.KEY_GUI_ELEMENT);
        return "true".equals(guiElement);
    }

    /**
     * Format the worth line using the config format.
     */
    private String formatWorthLine(BigDecimal totalWorth, BigDecimal singleWorth, int amount) {
        String currencyPrefix = plugin.getConfigManager().getMessageSystem().translate("messages.currency-prefix");
        String currencySuffix = plugin.getConfigManager().getMessageSystem().translate("messages.currency-suffix");

        String formattedTotal = currencyPrefix + plugin.getMiscUtils().economyFormat(totalWorth) + currencySuffix;
        String formattedSingle = currencyPrefix + plugin.getMiscUtils().economyFormat(singleWorth) + currencySuffix;

        String line = WorthConfig.getFormat()
                .replace("%worth%", formattedTotal)
                .replace("%worth_single%", formattedSingle)
                .replace("%amount%", String.valueOf(amount))
                .replace("%currency_prefix%", currencyPrefix)
                .replace("%currency_suffix%", currencySuffix);

        return ChatColor.translateAlternateColorCodes('&', line);
    }

    /**
     * Format the "not sellable" line.
     */
    private String formatNotSellableLine() {
        return ChatColor.translateAlternateColorCodes('&', WorthConfig.getNotSellableFormat());
    }

    /**
     * Unregister the packet listeners.
     * Safely handles the case where PacketEvents might not be available.
     */
    public void unregister() {
        if (packetListener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            } catch (NoClassDefFoundError | Exception e) {
                // Ignore if PacketEvents not available or already unloaded
                plugin.getLogUtil().debugLog("PacketEvents not available for unregister: " + e.getMessage());
            }
            packetListener = null;
        }
        if (eventListener != null) {
            HandlerList.unregisterAll(eventListener);
            eventListener = null;
        }
        
        registered = false;
        plugin.getLogUtil().debugLog("Worth display system disabled.");
    }

    // ==================== API Methods ====================

    public BigDecimal getItemWorth(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        Item shopItem = findShopItem(item);
        if (shopItem == null || !shopItem.hasSellPrice()) {
            return null;
        }

        return shopItem.getSellPriceAsDecimal();
    }

    public BigDecimal getStackWorth(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        Item shopItem = findShopItem(item);
        if (shopItem == null || !shopItem.hasSellPrice()) {
            return null;
        }

        return shopItem.calculateSellPrice(item.getAmount());
    }
}
