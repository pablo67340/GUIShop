package com.pablo67340.guishop.listenable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
	private static BufferedWriter econLogWriter;
	public static SimpleDateFormat format = new SimpleDateFormat("MM-dd hh:mm:ss");

	static {
		try {
			econLogWriter =
					new BufferedWriter(new FileWriter(Main.getINSTANCE().getEconLogFile(), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void log(String o) {
		if (econLogWriter != null) {
			try {
				econLogWriter.write(o);
				econLogWriter.newLine();
				econLogWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Open the {@link Sell} GUI.
	 */
	public void open(Player player) {
		GUI = new Gui(Main.getINSTANCE(), 6, Config.getSellTitle());
		GUI.setOnClose(this::onSellClose);
		StaticPane pane = new StaticPane(0, 0, 9, 6);
		GUI.addPane(pane);
		GUI.show(player);
	}

	/**
	 * Sell items inside the {@link Sell} GUI.
	 */
	public void sell(Player player) {
		log("[" + format.format(new Date()) + "] Player " + player.getName() + " is trying to sell" +
				" " + Arrays.toString(GUI.getInventory().getContents()));
		giveBackItems(player.getInventory(), sellItems(player, GUI.getInventory().getContents()));
		GUI.getInventory().clear();
	}

//	public static List<ItemStack> sellItems(Player player, ItemStack[] toSell)

	/**
	 * Sells the specified items on the behalf of a player. Returns a list of item stacks
	 * that could not be sold.
	 *
	 * @param player the player
	 * @param items the items
	 */
	public static List<ItemStack> sellItems(Player player, ItemStack[] items) {
		double moneyToGive = 0;
		boolean couldntSell = false;
		int cantSellCount = 0;
		List<ItemStack> couldntSellItems = new ArrayList<>();
		for (ItemStack item : items) {
			Double sellPrice = calculateSellPrice(item);
			if(sellPrice == null) {
				couldntSellItems.add(item);
				couldntSell = true;
				cantSellCount += 1;
				continue;
			}
			moneyToGive += sellPrice;
		}

		if (couldntSell) {
			player.sendMessage(Config.getPrefix() + " " + Config.getCantSell().replace("{count}", cantSellCount + ""));
		}
		roundAndGiveMoney(player, moneyToGive);

		return couldntSellItems;
	}

	private static void giveBackItems(Inventory retInv, List<ItemStack> items) {
		retInv.addItem(items.toArray(new ItemStack[0]));
	}

	private static Double calculateSellPrice(ItemStack item) {
		if (item == null) {
			return 0.0;
		}

		String itemString = Item.getItemStringForItemStack(item);

		Item shopItem = Main.getINSTANCE().getITEMTABLE().get(itemString);
		if (shopItem == null || !shopItem.hasSellPrice()) {
			return null;
		}

		int quantity = item.getAmount();

		// buy price must be defined for dynamic pricing to work
		if (Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()) {
			double ret = Main.getDYNAMICPRICING().calculateSellPrice(itemString, quantity,
					shopItem.getBuyPriceAsDouble(), shopItem.getSellPriceAsDouble());
			Main.getDYNAMICPRICING().sellItem(itemString, quantity);
			return ret;
		} else {
			return quantity * shopItem.getSellPriceAsDouble();
		}
	}

	/**
	 * Rounds the amount and deposits it on behalf of the player.
	 *
	 * @param player the player
	 * @param moneyToGive the amount to give
	 */
	public static void roundAndGiveMoney(Player player, double moneyToGive) {
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
