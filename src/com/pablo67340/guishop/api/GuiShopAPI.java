package com.pablo67340.guishop.api;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.Config;

/**
 * Officially supported API for interacting with GuiShop. <br>
 * <br>
 * Accessing GuiShop internals is not supported and liable to change at any time.
 *
 */
public class GuiShopAPI {

	private GuiShopAPI() {}
	
	/**
	 * Sells the specified items for the player as if the player
	 * had used the sell GUI and inserted the items there. <br>
	 * <br>
	 * The items' total sale price is summed and the reward is given to the player.
	 * If an item cannot be sold, the items are added back to the player's inventory. <br>
	 * Use check {@link #canBeSold(ItemStack)} to check if an item cannot be sold.
	 * No items are <i>removed</i> from the players' inventory; the caller is trusted with such. <br>
	 * <br>
	 * This may be useful if, for example, you want to make an
	 * autosell feature which uses the same prices from GuiShop,
	 * so you do not have to create a separate config.
	 * 
	 * @param player the player for whom to sell
	 * @param items the items which are sold
	 */
	public static void sellItems(Player player, ItemStack...items) {
		List<ItemStack> unsold = Sell.sellItems(player, items);
		player.getInventory().addItem(unsold.toArray(new ItemStack[0]));
	}
	
	/**
	 * Determines whether the specified item could be sold
	 * through the sell GUI. <br>
	 * <br>
	 * Formally, if an item is listed in the shops.yml with
	 * a nonzero sell price, it can be sold.
	 * 
	 * @param item the itemstack which would be sold
	 * @return whether it can be sold
	 */
	public static boolean canBeSold(ItemStack item) {
		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(Item.getItemStringForItemStack(item));
		return shopItem != null && shopItem.hasSellPrice();
	}
	
	/**
	 * Determines whether the specified item could be bought
	 * (has a buy price). An item is considered to be able
	 * to be purchased even if it is not displayed in the GUI. <br>
	 * <br>
	 * Formally, if an item is listed in the shops.yml with a
	 * defined buy price, it can be bought.
	 * 
	 * @param item the itemstack which would be bought
	 * @return whether it can be bought
	 */
	public static boolean canBeBought(ItemStack item) {
		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(Item.getItemStringForItemStack(item));
		return shopItem != null && shopItem.hasBuyPrice();
	}
	
	/**
	 * Gets the buy price for an item with specified quantity. <br>
	 * If the item does not exist or does not have a buy price, <code>-1</code> is returned.
	 * 
	 * @param item the itemstack
	 * @param quantity the quantity which would be purchased
	 * @return the buy price or minus 1 if not set
	 */
	public static double getBuyPrice(ItemStack item, int quantity) {
		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(Item.getItemStringForItemStack(item));
		return (shopItem != null && shopItem.hasBuyPrice()) ? shopItem.calculateBuyPrice(quantity) : -1;
	}
	
	/**
	 * Gets the sell price for an item with specified quantity. <br>
	 * If the item does not exist or does not have a sell price, <code>-1</code> is returned.
	 * 
	 * @param item the itemstack
	 * @param quantity the quantity which would be sold
	 * @return the sell price or minus 1 if not set
	 */
	public static double getSellPrice(ItemStack item, int quantity) {
		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(Item.getItemStringForItemStack(item));
		return (shopItem != null && shopItem.hasSellPrice()) ? shopItem.calculateSellPrice(quantity) : -1;
	}
	
	/**
	 * Indicates to GUIShop that the item has been purchased with the specified quantity. <br>
	 * If dynamic pricing is enabled, GUIShop will then inform the dynamic pricing provider
	 * that the purchase has occurred. (If disabled, nothing happens) <br>
	 * <br>
	 * Note that even if you are not using dynamic pricing, calling this method is recommended
	 * because it automatically ensures compatibility with dynamic pricing.
	 * 
	 * @param item the itemstack
	 * @param quantity the quantity which was purchased
	 */
	public static void indicateBoughtItems(ItemStack item, int quantity) {
		String itemString = Item.getItemStringForItemStack(item);
		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(itemString);

		if (shopItem != null && Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()
				&& shopItem.hasSellPrice()) {
			Main.getDYNAMICPRICING().buyItem(itemString, quantity);
		}
	}
	
	/**
	 * Indicates to GUIShop that the item has been sold with the specified quantity. <br>
	 * If dynamic pricing is enabled, GUIShop will then inform the dynamic pricing provider
	 * that the purchase has occurred. (If disabled, nothing happens) <br>
	 * <br>
	 * Note that even if you are not using dynamic pricing, calling this method is recommended
	 * because it automatically ensures compatibility with dynamic pricing.
	 * 
	 * @param item the itemstack
	 * @param quantity the quantity which was purchased
	 */
	public static void indicateSoldItems(ItemStack item, int quantity) {
		String itemString = Item.getItemStringForItemStack(item);
		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(itemString);

		if (shopItem != null && Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()
				&& shopItem.hasSellPrice()) {
			Main.getDYNAMICPRICING().sellItem(itemString, quantity);
		}
	}
	
}
