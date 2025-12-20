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
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

                // Get the slot number from the packet - needed for armor check and debug
                // SET_SLOT packet structure:
                // 1.17+: windowId (int 0), stateId (int 1), slot (int 2), item
                // Pre-1.17: windowId (int 0), slot (int 1), item
                int protocolSlot = -1;
                try {
                    int integerCount = packet.getIntegers().size();
                    if (integerCount >= 3) {
                        // 1.17+ with state ID: slot is at index 2
                        protocolSlot = packet.getIntegers().read(2);
                    } else if (integerCount >= 2) {
                        // Pre-1.17 without state ID: slot is at index 1
                        protocolSlot = packet.getIntegers().read(1);
                    }
                } catch (Exception e) {
                    // Fallback - try both indices
                    try {
                        protocolSlot = packet.getIntegers().read(2);
                    } catch (Exception e2) {
                        try {
                            protocolSlot = packet.getIntegers().read(1);
                        } catch (Exception e3) {
                            protocolSlot = -1;
                        }
                    }
                }

                // Window ID 0 = player inventory, -1 or -2 = special slots (cursor, etc.)
                // For containers (windowId > 0), also check if it's a GUIShop inventory
                if (windowId > 0) {
                    if (!shouldDisplayWorth(player)) {
                        return;
                    }
                }

                // Check if this is an armor slot and hide-armor-slots is enabled
                // Protocol slots 5-8 are armor slots (head=5, chest=6, legs=7, feet=8)
                boolean isArmorSlot = windowId == 0 && protocolSlot >= 5 && protocolSlot <= 8;
                if (isArmorSlot && WorthConfig.isHideArmorSlots()) {
                    if (WorthConfig.isDebug()) {
                        WorthDisplayManager.this.plugin.getLogUtil().debugLog("SET_SLOT: Armor slot " + protocolSlot + " item=" + item.getType() + " - stripping worth");
                    }
                    // Strip any existing worth lines from armor items
                    ItemStack stripped = stripWorthFromItem(item);
                    if (stripped != null) {
                        packet.getItemModifier().write(0, stripped);
                    }
                    return;
                }

                // Clone the item and add worth lore
                ItemStack modified = addWorthLore(item);
                if (modified != null) {
                    packet.getItemModifier().write(0, modified);
                    
                    if (WorthConfig.isDebug()) {
                        WorthDisplayManager.this.plugin.getLogUtil().debugLog("SET_SLOT: Modified " + item.getType() + 
                            " amount=" + item.getAmount() + " windowId=" + windowId + " slot=" + protocolSlot);
                    }
                } else if (WorthConfig.isDebug()) {
                    WorthDisplayManager.this.plugin.getLogUtil().debugLog("SET_SLOT: No modification for " + item.getType() + 
                        " amount=" + item.getAmount() + " slot=" + protocolSlot);
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
                // For containers, check if it's a GUIShop inventory or blacklisted
                boolean isPlayerInventory = (windowId == 0);
                boolean shouldDisplayForContainer = isPlayerInventory || shouldDisplayWorth(player);

                List<ItemStack> items = packet.getItemListModifier().read(0);

                if (items != null && !items.isEmpty()) {
                    List<ItemStack> modifiedItems = new ArrayList<>();
                    boolean anyModified = false;
                    
                    // Calculate where player inventory slots begin in this packet
                    // Player inventory always occupies the last 36 slots (27 main + 9 hotbar) in container windows
                    // For player inventory window (windowId 0), all slots are "player slots"
                    int playerSlotStart = isPlayerInventory ? 0 : Math.max(0, items.size() - 36);
                    
                    if (WorthConfig.isDebug()) {
                        WorthDisplayManager.this.plugin.getLogUtil().debugLog("WINDOW_ITEMS: windowId=" + windowId + 
                            " totalSlots=" + items.size() + " playerSlotStart=" + playerSlotStart + 
                            " shouldDisplayForContainer=" + shouldDisplayForContainer);
                    }

                    for (int i = 0; i < items.size(); i++) {
                        ItemStack item = items.get(i);
                        
                        if (item != null && item.getType() != Material.AIR) {
                            // For containers, player inventory slots start at (totalSlots - 36)
                            // This correctly handles all container sizes (single chest, double chest, etc.)
                            boolean isPlayerSlot = (i >= playerSlotStart);
                            boolean shouldAddWorth = isPlayerSlot || shouldDisplayForContainer;
                            
                            // Check if this is an armor slot and hide-armor-slots is enabled
                            // In player inventory (windowId 0), armor slots are 5-8
                            boolean isArmorSlot = isPlayerInventory && i >= 5 && i <= 8;
                            if (isArmorSlot && WorthConfig.isHideArmorSlots()) {
                                if (WorthConfig.isDebug()) {
                                    WorthDisplayManager.this.plugin.getLogUtil().debugLog("WINDOW_ITEMS: Armor slot " + i + " item=" + item.getType() + " - stripping worth");
                                }
                                // Strip any existing worth lines from armor items
                                ItemStack stripped = stripWorthFromItem(item);
                                if (stripped != null) {
                                    modifiedItems.add(stripped);
                                    anyModified = true;
                                } else {
                                    modifiedItems.add(item);
                                }
                                continue; // Skip to next item
                            }
                            
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
        
        // NOTE: We intentionally do NOT register an inventory click listener here.
        // The packet interceptors (SET_SLOT and WINDOW_ITEMS) are sufficient.
        // Sending additional packets on every click caused bandwidth issues and
        // triggered anti-exploit plugins (ExploitFixer, etc.).
        // The worth lore updates whenever the server naturally sends inventory packets.
        
        registered = true;

        StringBuilder logMsg = new StringBuilder("Worth display system enabled (ProtocolLib)");
        if (WorthConfig.isHideArmorSlots()) {
            logMsg.append(" [Armor hidden]");
        }
        if (WorthConfig.isPlayerInventoryOnly()) {
            logMsg.append(" [Player inv only]");
        }
        if (!WorthConfig.getBlacklistedItemNames().isEmpty()) {
            logMsg.append(" [").append(WorthConfig.getBlacklistedItemNames().size()).append(" item names blacklisted]");
        }
        plugin.getLogUtil().log(logMsg.toString());
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
        // Safety check - if configs aren't loaded yet, don't match
        if (Config.getTitlesConfig() == null || Config.getAltSellConfig() == null) {
            return false;
        }

        try {
            // Get GUIShop's configured inventory titles with null safety
            String menuTitleRaw = Config.getTitlesConfig().getMenuTitle();
            String shopTitleRaw = Config.getTitlesConfig().getShopTitle();
            String sellTitleRaw = Config.getTitlesConfig().getSellTitle();
            String qtyTitleRaw = Config.getTitlesConfig().getQtyTitle();
            String valueTitleRaw = Config.getTitlesConfig().getValueTitle();
            String altSellTitleRaw = Config.getAltSellConfig().getTitle();

            // Check menu title
            if (menuTitleRaw != null) {
                String menuTitle = ChatColor.stripColor(menuTitleRaw.replace("%page-number%", "")).toLowerCase().trim();
                if (strippedTitle.startsWith(menuTitle)) return true;
            }

            // Check shop title
            if (shopTitleRaw != null) {
                String shopTitle = ChatColor.stripColor(shopTitleRaw.replace("%shopname%", "")).toLowerCase().trim();
                if (strippedTitle.startsWith(shopTitle)) return true;
            }

            // Check sell title
            if (sellTitleRaw != null) {
                String sellTitle = ChatColor.stripColor(sellTitleRaw).toLowerCase();
                if (strippedTitle.equals(sellTitle)) return true;
            }

            // Check quantity title
            if (qtyTitleRaw != null) {
                String qtyTitle = ChatColor.stripColor(qtyTitleRaw).toLowerCase();
                if (strippedTitle.equals(qtyTitle)) return true;
            }

            // Check value title
            if (valueTitleRaw != null) {
                String valueTitle = ChatColor.stripColor(valueTitleRaw).toLowerCase();
                if (strippedTitle.equals(valueTitle)) return true;
            }

            // Check alt sell title
            if (altSellTitleRaw != null) {
                String altSellTitle = ChatColor.stripColor(altSellTitleRaw).toLowerCase();
                if (strippedTitle.equals(altSellTitle)) return true;
            }
        } catch (Exception e) {
            // If any error occurs, safely return false
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Error checking GUIShop inventory title: " + e.getMessage());
            }
            return false;
        }

        return false;
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
        registered = false;
        plugin.getLogUtil().debugLog("Worth display system disabled.");
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

        // Check if item name is blacklisted
        if (hasBlacklistedName(item)) {
            if (WorthConfig.isDebug()) {
                plugin.getLogUtil().debugLog("Skipping item with blacklisted name: " + item.getType());
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
     * Check if the item has a display name that is blacklisted (partial match).
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
     * This method first removes any existing worth lines to prevent duplicates.
     */
    private ItemStack applyWorthLore(ItemStack item, String worthLine) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return null;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        // Remove any existing worth lines to prevent duplicates
        // Also remove blank lines that were added before worth lines
        lore = stripExistingWorthLines(lore);

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
     * Strip any existing worth lines from the lore to prevent duplicates.
     * This detects worth lines by checking common patterns like "Worth:", currency symbols, etc.
     */
    private List<String> stripExistingWorthLines(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> cleanedLore = new ArrayList<>();
        
        // Get patterns to detect worth lines
        // We check: "worth:", "not sellable", and the format prefix from config
        String formatRaw = WorthConfig.getFormat();
        String notSellableRaw = WorthConfig.getNotSellableFormat();
        
        // Extract the start of the format to use as a pattern (e.g., "&7Worth:" -> "worth:")
        String formatPrefix = "";
        if (formatRaw != null && !formatRaw.isEmpty()) {
            // Strip color codes and get the first part before any placeholder
            String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', formatRaw));
            int placeholderIdx = stripped.indexOf('%');
            if (placeholderIdx > 0) {
                formatPrefix = stripped.substring(0, placeholderIdx).trim().toLowerCase();
            } else {
                formatPrefix = stripped.trim().toLowerCase();
            }
        }
        
        String notSellablePrefix = "";
        if (notSellableRaw != null && !notSellableRaw.isEmpty()) {
            notSellablePrefix = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', notSellableRaw)).toLowerCase();
        }
        
        boolean lastLineWasBlank = false;
        boolean removedWorthLine = false;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String strippedLine = ChatColor.stripColor(line).toLowerCase().trim();
            
            // Check if this line looks like a worth line
            boolean isWorthLine = false;
            
            // Check against format prefix (e.g., "worth:")
            if (!formatPrefix.isEmpty() && strippedLine.startsWith(formatPrefix)) {
                isWorthLine = true;
            }
            
            // Check against not sellable format
            if (!notSellablePrefix.isEmpty() && strippedLine.equals(notSellablePrefix)) {
                isWorthLine = true;
            }
            
            // Also check for common worth patterns as fallback
            if (strippedLine.startsWith("worth:") || strippedLine.startsWith("sell value:")) {
                isWorthLine = true;
            }
            
            if (isWorthLine) {
                removedWorthLine = true;
                // Skip this line (don't add to cleaned lore)
                // Also skip the blank line before it if we're at the bottom
                if (lastLineWasBlank && !cleanedLore.isEmpty()) {
                    cleanedLore.remove(cleanedLore.size() - 1);
                }
                continue;
            }
            
            // Track if this line is blank (for removing blank lines before worth)
            lastLineWasBlank = strippedLine.isEmpty();
            
            cleanedLore.add(line);
        }
        
        // If we're adding at top and the first line is blank, remove it
        if (removedWorthLine && !cleanedLore.isEmpty()) {
            String firstLine = ChatColor.stripColor(cleanedLore.get(0)).trim();
            if (firstLine.isEmpty()) {
                cleanedLore.remove(0);
            }
        }
        
        return cleanedLore;
    }

    /**
     * Strip all worth lines from an item without adding new ones.
     * Used for armor slots when hide-armor-slots is enabled.
     * 
     * @param item The item to strip worth from
     * @return A cloned item without worth lines, or null if no changes needed
     */
    private ItemStack stripWorthFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        if (!item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }
        
        List<String> originalLore = meta.getLore();
        if (originalLore == null || originalLore.isEmpty()) {
            return null;
        }
        
        // Strip worth lines
        List<String> cleanedLore = stripExistingWorthLines(originalLore);
        
        // Check if anything changed
        if (cleanedLore.size() == originalLore.size()) {
            // No worth lines were removed
            return null;
        }
        
        // Create modified clone
        ItemStack clone = item.clone();
        ItemMeta cloneMeta = clone.getItemMeta();
        if (cloneMeta != null) {
            cloneMeta.setLore(cleanedLore.isEmpty() ? null : cleanedLore);
            clone.setItemMeta(cloneMeta);
        }
        
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

