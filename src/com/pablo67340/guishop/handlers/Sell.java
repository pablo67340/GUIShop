package com.pablo67340.guishop.handlers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

import com.pablo67340.guishop.Main;

public final class Sell {

	/**
	 * Number of rows for the {@link Sell} GUI.
	 * 
	 */
	public static final int ROWS = 4;

	/**
	 * Number of columns for the {@link Sell} GUI.
	 * 
	 */
	public static final int COLS = 9;

	/**
	 * Inventory object for the {@link Sell} GUI.
	 * 
	 */
	private Inventory GUI;

	/**
	 * {@link Player} who is selling items in {@link sell}
	 * 
	 */
	private final Player player;

	/**
	 * Constructor, set player and load GUI.
	 * 
	 * @param {@link
	 * 			Player}
	 * 
	 */
	public Sell(Player player) {
		this.player = player;

		load();
	}

	/**
	 * Load the {@link Sell} GUI.
	 * 
	 */
	public void load() {
		GUI = player.getServer().createInventory(null, ROWS * COLS,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &rSell"));
	}

	/**
	 * Open the {@link Sell} GUI.
	 * 
	 */
	public void open() {
		player.openInventory(GUI);

		Main.HAS_SELL_OPEN.add(player.getName());
	}

	/**
	 * Sell items inside the {@link Sell} GUI.
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void sell() {
		double moneyToGive = 0;

		for (ItemStack item : GUI.getContents()) {
			if (item == null) {
				continue;
			}
			if (!Main.PRICES.containsKey(item.getData().getItemTypeId() + ":" + item.getData().getData())) {
				player.getInventory().addItem(item);
				player.sendMessage(Utils.getPrefix() + Utils.getCantSell());
				continue;

			}
			Double sellPrice = Main.PRICES.get(item.getTypeId() + ":" + item.getData().getData()).getSellPrice();

			Integer quantity = Main.PRICES.get(item.getTypeId() + ":" + item.getData().getData()).getQuantity();

			Double perEach = sellPrice / quantity;

			moneyToGive += perEach * item.getAmount();

		}

		Main.getEconomy().depositPlayer(player.getName(), moneyToGive);

		if (moneyToGive > 0) {
			player.sendMessage(Utils.getSold() + moneyToGive + Utils.getAdded());
		}

		GUI.clear();
	}

	/**
	 * Get the current {@link Sell} GUI.
	 * 
	 * @return Inventory
	 * 
	 */
	public Inventory getGUI() {
		return GUI;
	}

}
