package com.pablo67340.shop.handler;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;

import com.pablo67340.shop.main.Main;
import com.songoda.epicspawners.api.EpicSpawners;
import com.songoda.epicspawners.api.spawner.SpawnerData;

import de.dustplanet.util.SilkUtil;

public final class Sell implements Listener {

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
		Bukkit.getServer().getPluginManager().registerEvents(this, Main.getInstance());
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
		float isSellable = 0;
		String selectedShop = "";
		for (ItemStack item : GUI.getContents()) {
			Integer data = 0;
			if (item == null) {
				continue;
			}

			for (Entry<String, Map<String, Price>> cmap : Main.PRICETABLE.entrySet()) {

				if (item.getData().getItemTypeId() == 52) {

					if (Dependencies.hasDependency("SilkSpawners")) {
						SilkUtil su = (SilkUtil) Main.getInstance().getSpawnerObject();
						data = (int) su.getStoredSpawnerItemEntityID(item);
					} else if (Dependencies.hasDependency("EpicSpawners")) {
						EpicSpawners es = (EpicSpawners) Main.getInstance().getSpawnerObject();
						SpawnerData spawnerData = es.getSpawnerDataFromItem(item);
						String name = spawnerData.getIdentifyingName();
						data = Spawners.getMobID(name);

					}

				} else {
					data = (int) item.getData().getData();
				}

				if (cmap.getValue().containsKey(item.getData().getItemTypeId() + ":" + data)) {
					isSellable = 1;
					selectedShop = cmap.getKey();
				}
			}

			if (isSellable != 1) {
				player.getInventory().addItem(item);
				player.sendMessage(Utils.getPrefix() + Utils.getCantSell());
				continue;

			}
			Double sellPrice = Main.PRICETABLE.get(selectedShop).get(item.getTypeId() + ":" + data).getSellPrice();

			Integer quantity = Main.PRICETABLE.get(selectedShop).get(item.getTypeId() + ":" + data).getQuantity();

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

	@EventHandler(priority = EventPriority.HIGH)
	public void onSellClose(InventoryCloseEvent event) {
		if (Main.HAS_SELL_OPEN.contains(player.getName())) {
			HandlerList.unregisterAll(this);
			sell();
			Main.HAS_SELL_OPEN.remove(player.getName());
			return;
		}
	}

}
