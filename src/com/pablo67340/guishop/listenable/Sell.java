package com.pablo67340.guishop.listenable;

import org.bukkit.ChatColor;

import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;

import com.github.stefvanschie.inventoryframework.Gui;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;

public final class Sell {

	private Gui GUI;

	/**
	 * Open the {@link Sell} GUI.
	 */
	public void open(Player player) {
		GUI = new Gui(Main.getINSTANCE(), 6, ChatColor.translateAlternateColorCodes('&', "Menu &f> &rSell"));
		GUI.setOnClose(this::onSellClose);
		StaticPane pane = new StaticPane(0, 0, 9, 6);
		GUI.addPane(pane);
		GUI.show(player);
	}

	/**
	 * Sell items inside the {@link Sell} GUI.
	 */
	public void sell(Player player) {
		sellItems(player, GUI.getInventory().getContents());
		GUI.getInventory().clear();
	}
	
	/**
	 * Sells the specified items on the behalf of a player
	 * 
	 * @param player the player
	 * @param items the items
	 */
	public static void sellItems(Player player, ItemStack[] items) {
		double moneyToGive = 0;
		boolean couldntSell = false;
		int countSell = 0;
		for (ItemStack item : items) {

			if (item == null) {
				continue;
			}

			String itemString = Item.getItemStringForItemStack(item);

			Item shopItem = Main.getINSTANCE().getITEMTABLE().get(itemString);
			if (shopItem == null || !shopItem.hasSellPrice()) {
				countSell += 1;
				couldntSell = true;
				player.getInventory().addItem(item);
				continue;
			}

			int quantity = item.getAmount();

			// buy price must be defined for dynamic pricing to work
			if (Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()) {
				moneyToGive += Main.getDYNAMICPRICING().calculateSellPrice(itemString, quantity,
						shopItem.getBuyPriceAsDouble(), shopItem.getSellPriceAsDouble());
				Main.getDYNAMICPRICING().sellItem(itemString, quantity);
			} else {
				moneyToGive += quantity * shopItem.getSellPriceAsDouble();
			}

		}

		if (couldntSell) {
			player.sendMessage(Config.getPrefix() + " " + Config.getCantSell().replace("{count}", countSell + ""));
		}

		Double moneyToGiveRounded = (double) Math.round(moneyToGive * 100) / 100;

		if (moneyToGiveRounded > 0) {
			Main.getECONOMY().depositPlayer(player, moneyToGiveRounded);
			
			player.sendMessage(Config.getSold() + moneyToGiveRounded + Config.getAdded());
		}
	}

	private void onSellClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		sell(player);
	}

}
