package com.pablo67340.guishop.api;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.SellType;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.worth.WorthDisplayManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.List;

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

}
