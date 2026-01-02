package com.pablo67340.guishop.api;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.SellType;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.economy.EconomyManager;
import com.pablo67340.guishop.statistics.PlayerStats;
import com.pablo67340.guishop.statistics.StatisticsManager;
import com.pablo67340.guishop.worth.WorthDisplayManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Officially supported API for interacting with GuiShop. <br>
 * <br>
 * Accessing GuiShop internals is not supported and liable to change at any
 * time.
 */
public abstract class GUIShopAPI {

    /**
     * Sells the specified items for the player as if the player had used the
     * sell GUI and inserted the items there. <br>
     * <br>
     * The items' total sale price is summed and the reward is given to the
     * player. If an item cannot be sold, the items are added back to the
     * player's inventory. <br>
     * Use check {@link #canBeSold(ItemStack)} to check if an item cannot be
     * sold. No items are <i>removed</i> from the players' inventory; the caller
     * is trusted with such. <br>
     * <br>
     * This may be useful if, for example, you want to make an autosell feature
     * which uses the same prices from GuiShop, so you do not have to create a
     * separate config.
     *
     * @param player the player for whom to sell
     * @param type the resource the contents came from
     * @param items the items which are sold
     */
    public static void sellItems(Player player, SellType type, ItemStack... items) {
        Sell.sellItems(player, items, type);
    }

    /**
     * Determines whether the specified item could be sold through the sell GUI.
     * <br>
     * <br>
     * Formally, if an item is listed in the shops.yml with a nonzero sell
     * price, it can be sold.
     *
     * @param item the itemstack which would be sold
     * @return whether it can be sold
     */
    public static boolean canBeSold(ItemStack item) {
        Item shopItem = null;
        String itemString = item.getType().toString();
        List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(itemString);

        if (itemList != null) {
            for (Item iterator : itemList) {
                if (iterator.isItemFromItemStack(item)) {
                    shopItem = iterator;
                }
            }
        }
        return shopItem != null && shopItem.hasSellPrice();
    }

    /**
     * Determines whether the specified item could be bought (has a buy price).
     * An item is considered to be able to be purchased even if it is not
     * displayed in the GUI. <br>
     * <br>
     * Formally, if an item is listed in the shops.yml with a defined buy price,
     * it can be bought.
     *
     * @param item the itemstack which would be bought
     * @return whether it can be bought
     */
    public static boolean canBeBought(ItemStack item) {
        Item shopItem = null;
        String itemString = item.getType().toString();
        List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(itemString);

        if (itemList != null) {
            for (Item iterator : itemList) {
                if (iterator.isItemFromItemStack(item)) {
                    shopItem = iterator;
                }
            }
        }
        return shopItem != null && shopItem.hasBuyPrice();
    }

    /**
     * Gets the buy price for an item with specified quantity. <br>
     * If the item does not exist or does not have a buy price, <code>-1</code>
     * is returned.
     *
     * @param item the itemstack
     * @param quantity the quantity which would be purchased
     * @return the buy price or minus 1 if not set
     */
    public static BigDecimal getBuyPrice(ItemStack item, int quantity) {
        Item shopItem = null;
        String itemString = item.getType().toString();
        List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(itemString);

        if (itemList != null) {
            for (Item iterator : itemList) {
                if (iterator.isItemFromItemStack(item)) {
                    shopItem = iterator;
                }
            }
        }
        return (shopItem != null && shopItem.hasBuyPrice()) ? shopItem.calculateBuyPrice(quantity) : BigDecimal.valueOf(-1);
    }

    /**
     * Gets the sell price for an item with specified quantity. <br>
     * If the item does not exist or does not have a sell price, <code>-1</code>
     * is returned.
     *
     * @param item the itemstack
     * @param quantity the quantity which would be sold
     * @return the sell price or minus 1 if not set
     */
    public static BigDecimal getSellPrice(ItemStack item, int quantity) {
        Item shopItem = null;
        String itemString = item.getType().toString();
        List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(itemString);

        if (itemList != null) {
            for (Item iterator : itemList) {
                if (iterator.isItemFromItemStack(item)) {
                    shopItem = iterator;
                }
            }
        }
        return (shopItem != null && shopItem.hasSellPrice()) ? shopItem.calculateSellPrice(quantity) : BigDecimal.valueOf(-1);
    }

    /**
     * Indicates to GUIShop that the item has been purchased with the specified
     * quantity. <br>
     * If dynamic pricing is enabled, GUIShop will then inform the dynamic
     * pricing provider that the purchase has occurred. (If disabled, nothing
     * happens) <br>
     * <br>
     * Note that even if you are not using dynamic pricing, calling this method
     * is recommended because it automatically ensures compatibility with
     * dynamic pricing.
     *
     * @param item the itemstack
     * @param quantity the quantity which was purchased
     */
    public static void indicateBoughtItems(ItemStack item, int quantity) {
        Item shopItem = null;
        String itemString = item.getType().toString();
        List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(itemString);

        if (itemList != null) {
            for (Item iterator : itemList) {
                if (iterator.isItemFromItemStack(item)) {
                    shopItem = iterator;
                }
            }
        }

        if (shopItem != null && Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()
                && shopItem.hasSellPrice()) {
            GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().buyItem(itemString, quantity);
        }
    }

    /**
     * Indicates to GUIShop that the item has been sold with the specified
     * quantity. <br>
     * If dynamic pricing is enabled, GUIShop will then inform the dynamic
     * pricing provider that the purchase has occurred. (If disabled, nothing
     * happens) <br>
     * <br>
     * Note that even if you are not using dynamic pricing, calling this method
     * is recommended because it automatically ensures compatibility with
     * dynamic pricing.
     *
     * @param item the itemstack
     * @param quantity the quantity which was purchased
     */
    public static void indicateSoldItems(ItemStack item, int quantity) {
        Item shopItem = null;
        String itemString = item.getType().toString();
        List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(itemString);

        if (itemList != null) {
            for (Item iterator : itemList) {
                if (iterator.isItemFromItemStack(item)) {
                    shopItem = iterator;
                }
            }
        }

        if (shopItem != null && Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()
                && shopItem.hasSellPrice()) {
            GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().sellItem(itemString, quantity);
        }
    }

    /**
     * Gets the worth (sell value) of a single item. <br>
     * This is a convenience method that returns the sell price for quantity 1.
     *
     * @param item the itemstack to check
     * @return the worth per item, or null if not sellable
     */
    public static BigDecimal getItemWorth(ItemStack item) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager != null) {
            return manager.getItemWorth(item);
        }
        // Fallback if worth manager isn't initialized
        BigDecimal price = getSellPrice(item, 1);
        return price.compareTo(BigDecimal.valueOf(-1)) > 0 ? price : null;
    }

    /**
     * Gets the total worth (sell value) of an item stack. <br>
     * This accounts for the stack size and any dynamic pricing.
     *
     * @param item the itemstack to check (uses stack amount)
     * @return the total worth for the stack, or null if not sellable
     */
    public static BigDecimal getStackWorth(ItemStack item) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager != null) {
            return manager.getStackWorth(item);
        }
        // Fallback if worth manager isn't initialized
        BigDecimal price = getSellPrice(item, item.getAmount());
        return price.compareTo(BigDecimal.valueOf(-1)) > 0 ? price : null;
    }

    /**
     * Check if the worth display system is enabled and active.
     *
     * @return true if the worth display system is running
     */
    public static boolean isWorthDisplayEnabled() {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        return manager != null && manager.isRegistered();
    }

    // ==================== Per-Player Worth Display API ====================

    /**
     * Check if worth display is enabled for a specific player.
     * This checks both session toggles (from /gs toggleworth) and external plugin hooks.
     *
     * @param player The player to check
     * @return true if worth display is enabled for this player
     */
    public static boolean isWorthEnabledForPlayer(Player player) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager == null || !manager.isRegistered()) {
            return false;
        }
        return manager.isWorthEnabledForPlayer(player);
    }

    /**
     * Toggle worth display for a player (session-only, resets on server restart).
     * For persistent settings, use {@link #setExternalWorthCheck(java.util.function.Predicate)}.
     *
     * @param player The player to toggle
     * @return true if worth is now enabled, false if now disabled
     */
    public static boolean toggleWorthForPlayer(Player player) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager == null || !manager.isRegistered()) {
            return false;
        }
        return manager.toggleWorthForPlayer(player);
    }

    /**
     * Enable worth display for a player (session-only).
     *
     * @param player The player to enable worth for
     */
    public static void enableWorthForPlayer(Player player) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager != null && manager.isRegistered()) {
            manager.enableWorthForPlayer(player);
        }
    }

    /**
     * Disable worth display for a player (session-only).
     *
     * @param player The player to disable worth for
     */
    public static void disableWorthForPlayer(Player player) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager != null && manager.isRegistered()) {
            manager.disableWorthForPlayer(player);
        }
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
     * GUIShopAPI.setExternalWorthCheck(player -> {
     *     // Return true to disable worth for this player
     *     return myPlugin.hasWorthDisabled(player.getUniqueId());
     * });
     * </pre>
     *
     * @param check The predicate, or null to remove the hook
     */
    public static void setExternalWorthCheck(java.util.function.Predicate<Player> check) {
        WorthDisplayManager manager = WorthDisplayManager.getInstance();
        if (manager != null) {
            manager.setExternalDisableCheck(check);
        }
    }

    /**
     * Remove any external worth check hook that was previously set.
     */
    public static void removeExternalWorthCheck() {
        setExternalWorthCheck(null);
    }

    // ==================== Statistics API ====================

    /**
     * Check if the statistics system is available and initialized.
     *
     * @return true if statistics tracking is available
     */
    public static boolean isStatisticsEnabled() {
        StatisticsManager manager = StatisticsManager.getInstance();
        return manager != null && manager.isAvailable();
    }

    /**
     * Get a player's shop statistics.
     *
     * @param player The player (online or offline)
     * @return PlayerStats object containing all statistics, or null if unavailable
     */
    public static PlayerStats getPlayerStats(OfflinePlayer player) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return null;
        }
        return manager.getStats(player.getUniqueId());
    }

    /**
     * Get a player's shop statistics by UUID.
     *
     * @param uuid The player's UUID
     * @return PlayerStats object containing all statistics, or null if unavailable
     */
    public static PlayerStats getPlayerStats(UUID uuid) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return null;
        }
        return manager.getStats(uuid);
    }

    /**
     * Get the total money a player has spent in shops.
     *
     * @param player The player
     * @return Total spent, or BigDecimal.ZERO if unavailable
     */
    public static BigDecimal getTotalSpent(OfflinePlayer player) {
        PlayerStats stats = getPlayerStats(player);
        return stats != null ? stats.getTotalSpent() : BigDecimal.ZERO;
    }

    /**
     * Get the total money a player has earned from selling.
     *
     * @param player The player
     * @return Total earned, or BigDecimal.ZERO if unavailable
     */
    public static BigDecimal getTotalEarned(OfflinePlayer player) {
        PlayerStats stats = getPlayerStats(player);
        return stats != null ? stats.getTotalEarned() : BigDecimal.ZERO;
    }

    /**
     * Get the total number of items a player has bought.
     *
     * @param player The player
     * @return Total items bought, or 0 if unavailable
     */
    public static int getItemsBought(OfflinePlayer player) {
        PlayerStats stats = getPlayerStats(player);
        return stats != null ? stats.getItemsBought() : 0;
    }

    /**
     * Get the total number of items a player has sold.
     *
     * @param player The player
     * @return Total items sold, or 0 if unavailable
     */
    public static int getItemsSold(OfflinePlayer player) {
        PlayerStats stats = getPlayerStats(player);
        return stats != null ? stats.getItemsSold() : 0;
    }

    /**
     * Get the total spent formatted as a string.
     *
     * @param player The player
     * @param abbreviated If true, format as 1.5K, 2M, etc. If false, use commas
     * @return Formatted string
     */
    public static String getTotalSpentFormatted(OfflinePlayer player, boolean abbreviated) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return "0";
        }
        return manager.getTotalSpentFormatted(player.getUniqueId(), abbreviated);
    }

    /**
     * Get the total earned formatted as a string.
     *
     * @param player The player
     * @param abbreviated If true, format as 1.5K, 2M, etc. If false, use commas
     * @return Formatted string
     */
    public static String getTotalEarnedFormatted(OfflinePlayer player, boolean abbreviated) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return "0";
        }
        return manager.getTotalEarnedFormatted(player.getUniqueId(), abbreviated);
    }

    /**
     * Get top spenders on the server.
     *
     * @param limit Number of players to return (max 100)
     * @return List of UUID -> Amount spent pairs, sorted descending
     */
    public static List<Map.Entry<UUID, BigDecimal>> getTopSpenders(int limit) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return List.of();
        }
        return manager.getTopSpenders(Math.min(limit, 100));
    }

    /**
     * Get top earners on the server.
     *
     * @param limit Number of players to return (max 100)
     * @return List of UUID -> Amount earned pairs, sorted descending
     */
    public static List<Map.Entry<UUID, BigDecimal>> getTopEarners(int limit) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return List.of();
        }
        return manager.getTopEarners(Math.min(limit, 100));
    }

    /**
     * Reset a player's statistics.
     *
     * @param player The player whose stats to reset
     */
    public static void resetPlayerStats(OfflinePlayer player) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager != null && manager.isAvailable()) {
            manager.resetStats(player.getUniqueId());
        }
    }

    // ==================== Internal Economy API ====================
    
    /**
     * Check if GUIShop's internal economy is enabled and available.
     * 
     * @return true if internal economy is enabled and functional
     */
    public static boolean isInternalEconomyEnabled() {
        EconomyManager manager = EconomyManager.getInstance();
        return manager != null && manager.isAvailable();
    }
    
    /**
     * Get a player's balance using the internal economy.
     * 
     * @param player the player
     * @return the player's balance, or BigDecimal.ZERO if not available
     */
    public static BigDecimal getInternalBalance(OfflinePlayer player) {
        EconomyManager manager = EconomyManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return BigDecimal.ZERO;
        }
        return manager.getBalance(player.getUniqueId());
    }
    
    /**
     * Set a player's balance using the internal economy.
     * 
     * @param player the player
     * @param amount the new balance
     * @return true if successful
     */
    public static boolean setInternalBalance(OfflinePlayer player, BigDecimal amount) {
        EconomyManager manager = EconomyManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return false;
        }
        return manager.setBalance(player.getUniqueId(), amount);
    }
    
    /**
     * Give money to a player using the internal economy.
     * 
     * @param player the player
     * @param amount the amount to give
     * @return true if successful
     */
    public static boolean giveInternalMoney(OfflinePlayer player, BigDecimal amount) {
        EconomyManager manager = EconomyManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return false;
        }
        return manager.deposit(player.getUniqueId(), amount);
    }
    
    /**
     * Take money from a player using the internal economy.
     * 
     * @param player the player
     * @param amount the amount to take
     * @return true if successful (player has sufficient funds)
     */
    public static boolean takeInternalMoney(OfflinePlayer player, BigDecimal amount) {
        EconomyManager manager = EconomyManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return false;
        }
        return manager.withdraw(player.getUniqueId(), amount);
    }
    
    /**
     * Format a balance using the internal economy's settings.
     * 
     * @param amount the amount to format
     * @return formatted string (e.g., "$1,000.00" or "1.5M")
     */
    public static String formatInternalBalance(BigDecimal amount) {
        EconomyManager manager = EconomyManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return amount.toPlainString();
        }
        return manager.format(amount);
    }

    // ==================== Payment Notification Preferences ====================
    
    /**
     * Check if a player has payment notifications enabled.
     * 
     * @param player the player to check
     * @return true if notifications are enabled (default true)
     */
    public static boolean isPayNotificationsEnabled(OfflinePlayer player) {
        return isPayNotificationsEnabled(player.getUniqueId());
    }
    
    /**
     * Check if a player has payment notifications enabled.
     * 
     * @param uuid the player's UUID
     * @return true if notifications are enabled (default true)
     */
    public static boolean isPayNotificationsEnabled(UUID uuid) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return true; // Default enabled
        }
        return manager.isPayNotificationsEnabled(uuid);
    }
    
    /**
     * Set whether a player has payment notifications enabled.
     * 
     * @param player the player
     * @param enabled true to enable, false to disable
     */
    public static void setPayNotificationsEnabled(OfflinePlayer player, boolean enabled) {
        setPayNotificationsEnabled(player.getUniqueId(), enabled);
    }
    
    /**
     * Set whether a player has payment notifications enabled.
     * 
     * @param uuid the player's UUID
     * @param enabled true to enable, false to disable
     */
    public static void setPayNotificationsEnabled(UUID uuid, boolean enabled) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager != null && manager.isAvailable()) {
            manager.setPayNotificationsEnabled(uuid, enabled);
        }
    }
    
    /**
     * Toggle payment notifications for a player.
     * 
     * @param player the player
     * @return the new state (true = enabled)
     */
    public static boolean togglePayNotifications(OfflinePlayer player) {
        return togglePayNotifications(player.getUniqueId());
    }
    
    /**
     * Toggle payment notifications for a player.
     * 
     * @param uuid the player's UUID
     * @return the new state (true = enabled)
     */
    public static boolean togglePayNotifications(UUID uuid) {
        StatisticsManager manager = StatisticsManager.getInstance();
        if (manager == null || !manager.isAvailable()) {
            return true; // Default enabled
        }
        return manager.togglePayNotifications(uuid);
    }

}
