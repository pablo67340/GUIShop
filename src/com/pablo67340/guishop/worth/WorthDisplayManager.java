package com.pablo67340.guishop.worth;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.config.WorthConfig;
import com.pablo67340.guishop.definition.Item;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Manages the display of item worth in item lore using ProtocolLib packet interception.
 * This modifies items client-side only, preserving server-side item data and stacking behavior.
 */
public class WorthDisplayManager {

    @Getter
    private static WorthDisplayManager instance;

    private final GUIShop plugin;
    private ProtocolManager protocolManager;
    private PacketAdapter setSlotListener;
    private PacketAdapter windowItemsListener;
    private Listener inventoryClickListener;

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

    public WorthDisplayManager(GUIShop plugin) {
        this.plugin = plugin;
        instance = this;
    }

    // ==================== Per-Player Worth Toggle API ====================

    /**
     * Check if worth display is enabled for a specific player.
     * This checks both session toggles and external plugin hooks.
     *
     * @param player The player to check
     * @return true if worth display is enabled for this player
     */
    public boolean isWorthEnabledForPlayer(Player player) {
        if (player == null) return true;

        // Check session-based disable (from /gs toggleworth command)
        if (sessionDisabledPlayers.contains(player.getUniqueId())) {
            return false;
        }

        // Check external plugin hook
        if (externalDisableCheck != null) {
            try {
                // External check returns true to DISABLE, so we invert
                if (externalDisableCheck.test(player)) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogUtil().debugLog("External worth check failed for " + player.getName() + ": " + e.getMessage());
            }
        }

        return true;
    }

    /**
     * Toggle worth display for a player (session-only, resets on server restart).
     *
     * @param player The player to toggle
     * @return true if worth is now enabled, false if now disabled
     */
    public boolean toggleWorthForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (sessionDisabledPlayers.contains(uuid)) {
            sessionDisabledPlayers.remove(uuid);
            return true; // Now enabled
        } else {
            sessionDisabledPlayers.add(uuid);
            return false; // Now disabled
        }
    }

    /**
     * Enable worth display for a player (session-only).
     *
     * @param player The player to enable worth for
     */
    public void enableWorthForPlayer(Player player) {
        sessionDisabledPlayers.remove(player.getUniqueId());
    }

    /**
     * Disable worth display for a player (session-only).
     *
     * @param player The player to disable worth for
     */
    public void disableWorthForPlayer(Player player) {
        sessionDisabledPlayers.add(player.getUniqueId());
    }

    /**
     * Check if a player has disabled worth display via the session toggle.
     *
     * @param player The player to check
     * @return true if the player has disabled worth via session toggle
     */
    public boolean isSessionDisabled(Player player) {
        return sessionDisabledPlayers.contains(player.getUniqueId());
    }

    /**
     * Set an external predicate to check if worth should be disabled for a player.
     * This allows other plugins to hook in and provide persistent per-player settings.
     * <p>
     * The predicate should return TRUE to DISABLE worth display for the player,
     * or FALSE to allow it (defer to other checks).
     * <p>
     * Example usage from another plugin:
     * <pre>
     * WorthDisplayManager.getInstance().setExternalDisableCheck(player -> {
     *     // Return true to disable worth for this player
     *     return myPlugin.hasWorthDisabled(player.getUniqueId());
     * });
     * </pre>
     *
     * @param check The predicate, or null to remove the hook
     */
    public void setExternalDisableCheck(Predicate<Player> check) {
        this.externalDisableCheck = check;
    }

    /**
     * Get the current external disable check predicate.
     *
     * @return The current predicate, or null if none set
     */
    public Predicate<Player> getExternalDisableCheck() {
        return this.externalDisableCheck;
    }

    /**
     * Clear all session-based worth toggles (useful for reload).
     */
    public void clearSessionToggles() {
        sessionDisabledPlayers.clear();
    }

    // ==================== End Per-Player API ====================

    /**
     * Initialize and register the packet listeners.
     * Should only be called if ProtocolLib is available.
     */
    public void register() {
        if (!WorthConfig.isEnabled()) {
            plugin.getLogUtil().debugLog("Worth display is disabled in config.");
            return;
        }

        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
        } catch (Exception e) {
            plugin.getLogUtil().log("Failed to get ProtocolLib manager: " + e.getMessage());
            return;
        }

        // Listener for SET_SLOT packets (single slot updates)
        setSlotListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) return;

                Player player = event.getPlayer();
                if (player == null) return;

                PacketContainer packet = event.getPacket();
                
                // Read the item FIRST and clone immediately to get fresh data
                ItemStack originalItem = packet.getItemModifier().read(0);
                
                if (originalItem == null || originalItem.getType() == Material.AIR) {
                    return;
                }
                
                // Clone to ensure we have independent data
                ItemStack item = originalItem.clone();
                
                // Get window ID - use bytes for older versions, integers for newer
                int windowId;
                try {
                    // Try reading as integer first (1.17+)
                    windowId = packet.getIntegers().read(0);
                } catch (Exception e) {
                    // Fall back to byte for older versions
                    try {
                        windowId = packet.getBytes().read(0).intValue();
                    } catch (Exception e2) {
                        windowId = 0; // Default to player inventory
                    }
                }
                
                // Check per-player worth toggle FIRST (applies to all inventory types)
                if (!isWorthEnabledForPlayer(player)) {
                    return;
                }

                // Window ID 0 = player inventory, -1 or -2 = special slots (cursor, etc.)
                // For containers (windowId > 0), also check if it's a GUIShop inventory
                if (windowId > 0) {
                    if (!shouldDisplayWorth(player)) {
                        return;
                    }
                }

                // Clone the item and add worth lore
                ItemStack modified = addWorthLore(item);
                if (modified != null) {
                    packet.getItemModifier().write(0, modified);
                    
                    if (WorthConfig.isDebug()) {
                        WorthDisplayManager.this.plugin.getLogUtil().debugLog("SET_SLOT: Modified " + item.getType() + 
                            " amount=" + item.getAmount() + " windowId=" + windowId);
                    }
                } else if (WorthConfig.isDebug()) {
                    WorthDisplayManager.this.plugin.getLogUtil().debugLog("SET_SLOT: No modification for " + item.getType() + 
                        " amount=" + item.getAmount());
                }
            }
        };

        // Listener for WINDOW_ITEMS packets (full inventory updates)
        windowItemsListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) return;

                Player player = event.getPlayer();
                if (player == null) return;

                PacketContainer packet = event.getPacket();
                
                // Check per-player worth toggle FIRST (applies to all inventory types)
                if (!isWorthEnabledForPlayer(player)) {
                    return;
                }

                // Get window ID
                int windowId;
                try {
                    windowId = packet.getIntegers().read(0);
                } catch (Exception e) {
                    try {
                        windowId = packet.getBytes().read(0).intValue();
                    } catch (Exception e2) {
                        windowId = 0;
                    }
                }
                
                // For player inventory (windowId 0), always add worth to all items
                // For containers, check if it's a GUIShop inventory
                boolean isPlayerInventory = (windowId == 0);
                boolean shouldDisplayForContainer = isPlayerInventory || shouldDisplayWorth(player);

                List<ItemStack> items = packet.getItemListModifier().read(0);

                if (items != null && !items.isEmpty()) {
                    List<ItemStack> modifiedItems = new ArrayList<>();
                    boolean anyModified = false;

                    for (int i = 0; i < items.size(); i++) {
                        ItemStack item = items.get(i);
                        
                        if (item != null && item.getType() != Material.AIR) {
                            // For containers, player inventory slots start after container slots
                            // Typically slot 36+ for single chests, 54+ for double chests, etc.
                            boolean isPlayerSlot = isPlayerInventory || (i >= 36);
                            boolean shouldAddWorth = isPlayerSlot || shouldDisplayForContainer;
                            
                            if (shouldAddWorth) {
                                ItemStack modified = addWorthLore(item);
                                if (modified != null) {
                                    modifiedItems.add(modified);
                                    anyModified = true;
                                } else {
                                    modifiedItems.add(item);
                                }
                            } else {
                                modifiedItems.add(item);
                            }
                        } else {
                            modifiedItems.add(item);
                        }
                    }

                    if (anyModified) {
                        packet.getItemListModifier().write(0, modifiedItems);
                        
                        if (WorthConfig.isDebug()) {
                            WorthDisplayManager.this.plugin.getLogUtil().debugLog("WINDOW_ITEMS: Modified " + modifiedItems.size() + 
                                " items in windowId=" + windowId);
                        }
                    }
                }
            }
        };

        protocolManager.addPacketListener(setSlotListener);
        protocolManager.addPacketListener(windowItemsListener);
        
        // Register inventory click listener for player inventory operations
        // This is needed because when players manipulate their own inventory (split stacks, etc.),
        // Paper may not send SET_SLOT packets since the client handles it locally
        // We manually send SET_SLOT packets with worth lore to update the display
        inventoryClickListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onInventoryClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player)) return;
                
                Player player = (Player) event.getWhoClicked();
                
                // Schedule after the click is processed to get updated slot contents
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    refreshPlayerInventory(player);
                }, 1L);
            }
        };
        Bukkit.getPluginManager().registerEvents(inventoryClickListener, plugin);
        
        registered = true;

        plugin.getLogUtil().log("Worth display system enabled (using ProtocolLib).");
    }

    /**
     * Check if worth should be displayed for the player's current inventory.
     *
     * @param player The player to check
     * @return true if worth should be displayed, false otherwise
     */
    private boolean shouldDisplayWorth(Player player) {
        if (player == null || player.getOpenInventory() == null) {
            return true; // Default to showing worth if we can't determine
        }

        // Check per-player worth toggle (session-based and external hooks)
        if (!isWorthEnabledForPlayer(player)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Skipping worth display - disabled for player: " + player.getName());
            }
            return false;
        }

        // If player-inventory-only is enabled, only show worth in player's own inventory
        if (WorthConfig.isPlayerInventoryOnly()) {
            InventoryType topType = player.getOpenInventory().getTopInventory().getType();
            if (topType != InventoryType.CRAFTING) {
                // CRAFTING type means the player is just looking at their inventory (no container open)
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("Skipping worth display - player-inventory-only mode and container open: " + topType);
                }
                return false;
            }
            return true;
        }

        String title = player.getOpenInventory().getTitle();
        if (title == null || title.isEmpty()) {
            return true;
        }

        // Strip color codes for comparison
        String strippedTitle = ChatColor.stripColor(title).toLowerCase();

        // Check if it's a GUIShop inventory (Menu, Shop, Sell, Quantity, Value, AltSell)
        if (isGUIShopInventory(strippedTitle)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Skipping worth display - GUIShop inventory: " + title);
            }
            return false;
        }

        // Check against blacklisted inventories
        for (String blacklisted : WorthConfig.getBlacklistedInventories()) {
            if (strippedTitle.contains(blacklisted.toLowerCase())) {
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("Skipping worth display - blacklisted inventory: " + title);
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Check if the inventory title matches any GUIShop inventory.
     */
    private boolean isGUIShopInventory(String strippedTitle) {
        // Get GUIShop's configured inventory titles
        String menuTitle = ChatColor.stripColor(Config.getTitlesConfig().getMenuTitle()
                .replace("%page-number%", "")).toLowerCase().trim();
        String shopTitle = ChatColor.stripColor(Config.getTitlesConfig().getShopTitle()
                .replace("%shopname%", "")).toLowerCase().trim();
        String sellTitle = ChatColor.stripColor(Config.getTitlesConfig().getSellTitle()).toLowerCase();
        String qtyTitle = ChatColor.stripColor(Config.getTitlesConfig().getQtyTitle()).toLowerCase();
        String valueTitle = ChatColor.stripColor(Config.getTitlesConfig().getValueTitle()).toLowerCase();
        String altSellTitle = ChatColor.stripColor(Config.getAltSellConfig().getTitle()).toLowerCase();

        // Check if the current inventory title starts with or matches any GUIShop title
        return strippedTitle.startsWith(menuTitle) ||
               strippedTitle.startsWith(shopTitle) ||
               strippedTitle.equals(sellTitle) ||
               strippedTitle.equals(qtyTitle) ||
               strippedTitle.equals(valueTitle) ||
               strippedTitle.equals(altSellTitle);
    }

    /**
     * Unregister the packet listeners.
     */
    public void unregister() {
        if (protocolManager != null) {
            if (setSlotListener != null) {
                protocolManager.removePacketListener(setSlotListener);
            }
            if (windowItemsListener != null) {
                protocolManager.removePacketListener(windowItemsListener);
            }
        }
        if (inventoryClickListener != null) {
            HandlerList.unregisterAll(inventoryClickListener);
        }
        registered = false;
        plugin.getLogUtil().debugLog("Worth display system disabled.");
    }

    /**
     * Refresh all items in a player's inventory by sending SET_SLOT packets with worth lore.
     * This ensures all items display their current worth based on stack size.
     *
     * @param player The player whose inventory to refresh
     */
    private void refreshPlayerInventory(Player player) {
        if (protocolManager == null) return;

        // Check per-player worth toggle - if disabled, don't refresh with worth lore
        if (!isWorthEnabledForPlayer(player)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Skipping worth refresh - disabled for player: " + player.getName());
            }
            return;
        }

        // Update all slots in player inventory (0-40)
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                sendSlotUpdate(player, slot);
            }
        }

        // Update cursor if held
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            sendCursorUpdate(player, cursor);
        }

        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("Refreshed inventory worth display for " + player.getName());
        }
    }

    /**
     * Send a SET_SLOT packet for a specific inventory slot to update the client's view.
     * This adds worth lore to the item and sends it directly to the player.
     *
     * @param player The player to send the update to
     * @param slot The inventory slot to update
     */
    private void sendSlotUpdate(Player player, int slot) {
        if (protocolManager == null) return;
        
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        
        ItemStack modified = addWorthLore(item);
        if (modified == null) return;
        
        try {
            // Create SET_SLOT packet
            // Window ID 0 = player inventory
            // Slot needs to be converted to protocol slot format
            int protocolSlot = convertToProtocolSlot(slot);
            
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
            packet.getIntegers().write(0, 0); // Window ID 0 = player inventory
            packet.getIntegers().write(1, 0); // State ID (1.17+)
            packet.getIntegers().write(2, protocolSlot); // Slot
            packet.getItemModifier().write(0, modified);
            
            protocolManager.sendServerPacket(player, packet);
            
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Sent slot update: slot=" + slot + " -> protocolSlot=" + protocolSlot + 
                    " item=" + item.getType() + " x" + item.getAmount());
            }
        } catch (Exception e) {
            // Try alternative packet structure for different versions
            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
                packet.getIntegers().write(0, 0); // Window ID
                packet.getIntegers().write(1, convertToProtocolSlot(slot)); // Slot (no state ID)
                packet.getItemModifier().write(0, modified);
                
                protocolManager.sendServerPacket(player, packet);
            } catch (Exception e2) {
                if (WorthConfig.isDebug()) {
                    plugin.getLogUtil().debugLog("Failed to send slot update: " + e2.getMessage());
                }
            }
        }
    }

    /**
     * Send a cursor item update to the player.
     *
     * @param player The player
     * @param cursor The cursor item
     */
    private void sendCursorUpdate(Player player, ItemStack cursor) {
        if (protocolManager == null || cursor == null || cursor.getType() == Material.AIR) return;
        
        ItemStack modified = addWorthLore(cursor);
        if (modified == null) return;
        
        try {
            // Cursor is slot -1 in SET_SLOT packet
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
            packet.getIntegers().write(0, -1); // Window ID -1 for cursor
            packet.getIntegers().write(1, 0);  // State ID
            packet.getIntegers().write(2, -1); // Slot -1 for cursor
            packet.getItemModifier().write(0, modified);
            
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            // Ignore cursor update failures
        }
    }

    /**
     * Convert Bukkit inventory slot to Minecraft protocol slot.
     * Player inventory slots in protocol are arranged differently.
     * 
     * Bukkit: 0-8 = hotbar, 9-35 = main inventory, 36-39 = armor (boots->helmet), 40 = offhand
     * Protocol (window 0): 36-44 = hotbar, 9-35 = main inventory, 5-8 = armor (head->feet), 45 = offhand
     * 
     * IMPORTANT: Bukkit armor order is boots(36)->helmet(39), but protocol is head(5)->feet(8)
     * So the armor slots must be reversed!
     */
    private int convertToProtocolSlot(int bukkitSlot) {
        if (bukkitSlot >= 0 && bukkitSlot <= 8) {
            // Hotbar: Bukkit 0-8 -> Protocol 36-44
            return bukkitSlot + 36;
        } else if (bukkitSlot >= 9 && bukkitSlot <= 35) {
            // Main inventory: same in both
            return bukkitSlot;
        } else if (bukkitSlot >= 36 && bukkitSlot <= 39) {
            // Armor slots - REVERSED order:
            // Bukkit 36 (boots) -> Protocol 8 (feet)
            // Bukkit 37 (leggings) -> Protocol 7 (legs)
            // Bukkit 38 (chestplate) -> Protocol 6 (chest)
            // Bukkit 39 (helmet) -> Protocol 5 (head)
            return 44 - bukkitSlot;
        } else if (bukkitSlot == 40) {
            // Offhand: Bukkit 40 -> Protocol 45
            return 45;
        }
        return bukkitSlot;
    }

    /**
     * Add the worth lore to an item.
     * Returns a cloned item with modified lore, or null if no modification needed.
     *
     * @param item The original item
     * @return The modified item clone, or null if no worth to display
     */
    private ItemStack addWorthLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        // Check if item already has a worth line (from ignore list)
        if (hasIgnoredLore(item)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Skipping item with ignored lore: " + item.getType());
            }
            return null;
        }

        // Find the shop item for this material
        Item shopItem = findShopItem(item);

        if (shopItem == null) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("No shop item found for: " + item.getType());
            }
            // If we want to show "not sellable" for items not in shop
            if (!WorthConfig.isOnlyShowSellable()) {
                return addNotSellableLore(item);
            }
            return null;
        }

        if (!shopItem.hasSellPrice()) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("No sell price for: " + item.getType());
            }
            if (!WorthConfig.isOnlyShowSellable()) {
                return addNotSellableLore(item);
            }
            return null;
        }

        // Calculate the worth based on actual stack size
        int quantity = item.getAmount();
        BigDecimal totalWorth = shopItem.calculateSellPrice(quantity);
        BigDecimal singleWorth = shopItem.calculateSellPrice(1);
        
        if (WorthConfig.isDebug()) {
            plugin.getLogUtil().debugLog("Worth calc: " + item.getType() + " x" + quantity + " = " + totalWorth + " (single: " + singleWorth + ")");
        }

        // Format the worth line
        String worthLine = formatWorthLine(totalWorth, singleWorth, quantity);

        // Clone and modify the item
        return applyWorthLore(item, worthLine);
    }

    /**
     * Find a matching shop item for the given ItemStack.
     */
    private Item findShopItem(ItemStack item) {
        String materialKey = Item.getItemStringForItemStack(item);
        
        // First try exact match with spawner info
        List<Item> itemList = plugin.getITEMTABLE().get(materialKey);
        
        // If not found and it's a spawner, try just the base material
        if (itemList == null) {
            itemList = plugin.getITEMTABLE().get(item.getType().toString());
        }

        if (itemList == null || itemList.isEmpty()) {
            return null;
        }

        // Find the matching shop item
        for (Item shopItem : itemList) {
            if (shopItem.isItemFromItemStack(item)) {
                return shopItem;
            }
        }

        // Return first item if no exact match (for simple items without special attributes)
        return itemList.get(0);
    }

    /**
     * Check if the item has lore that should be ignored.
     */
    private boolean hasIgnoredLore(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;

        List<String> lore = meta.getLore();
        if (lore == null) return false;

        List<String> ignoreList = WorthConfig.getIgnoreLoreContaining();
        for (String loreLine : lore) {
            String stripped = ChatColor.stripColor(loreLine).toLowerCase();
            for (String ignore : ignoreList) {
                if (stripped.contains(ignore.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
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
     * Apply the worth lore to a cloned item.
     */
    private ItemStack applyWorthLore(ItemStack item, String worthLine) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return null;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        if (WorthConfig.isPositionTop()) {
            // Add at the top
            if (WorthConfig.isAddBlankLine() && !lore.isEmpty()) {
                lore.add(0, "");
                lore.add(0, worthLine);
            } else {
                lore.add(0, worthLine);
            }
        } else {
            // Add at the bottom (default)
            if (WorthConfig.isAddBlankLine() && !lore.isEmpty()) {
                lore.add("");
            }
            lore.add(worthLine);
        }

        meta.setLore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Add a "not sellable" lore to items that can't be sold.
     */
    private ItemStack addNotSellableLore(ItemStack item) {
        String notSellableLine = ChatColor.translateAlternateColorCodes('&', WorthConfig.getNotSellableFormat());
        return applyWorthLore(item, notSellableLine);
    }

    /**
     * Get the sell price for an item (API method for other plugins).
     *
     * @param item The item to check
     * @return The sell price per item, or null if not sellable
     */
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

    /**
     * Get the total sell price for a stack of items (API method).
     *
     * @param item The item stack to check
     * @return The total sell price for the stack, or null if not sellable
     */
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

