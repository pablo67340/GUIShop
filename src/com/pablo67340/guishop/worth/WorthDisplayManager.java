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
import java.util.List;

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

    public WorthDisplayManager(GUIShop plugin) {
        this.plugin = plugin;
        instance = this;
    }

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

                // Check if worth should be displayed for this inventory
                if (!shouldDisplayWorth(player)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                ItemStack item = packet.getItemModifier().read(0);

                if (item != null && item.getType() != Material.AIR) {
                    ItemStack modified = addWorthLore(item);
                    if (modified != null) {
                        packet.getItemModifier().write(0, modified);
                    }
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

                // Check if worth should be displayed for this inventory
                if (!shouldDisplayWorth(player)) {
                    return;
                }

                PacketContainer packet = event.getPacket();
                List<ItemStack> items = packet.getItemListModifier().read(0);

                if (items != null && !items.isEmpty()) {
                    List<ItemStack> modifiedItems = new ArrayList<>();
                    boolean anyModified = false;

                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.AIR) {
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
                    }

                    if (anyModified) {
                        packet.getItemListModifier().write(0, modifiedItems);
                    }
                }
            }
        };

        protocolManager.addPacketListener(setSlotListener);
        protocolManager.addPacketListener(windowItemsListener);
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

        // Calculate the worth
        int quantity = item.getAmount();
        BigDecimal totalWorth = shopItem.calculateSellPrice(quantity);
        BigDecimal singleWorth = shopItem.calculateSellPrice(1);

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

