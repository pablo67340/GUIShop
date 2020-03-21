package com.pablo67340.guishop.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.Price;
import com.pablo67340.guishop.listenable.Sell;

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
	 * Use check {@link #canBeSold(ItemStack} to check if an item cannot be sold.
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
		Sell.sellItems(player, items);
	}
	
	/**
	 * Determines whether the specified item could be sold
	 * through the sell GUI.
	 * 
	 * @param item the itemstack which would be sold
	 * @return whether it can be sold
	 */
	public static boolean canBeSold(ItemStack item) {
		Price price = Main.getINSTANCE().getPRICETABLE().get(Item.getItemStringForItemStack(item));
		return price != null && price.getSellPrice() >= 0;
	}
	
}
