package com.pablo67340.shop.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
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

	private final Map<String, Price> PRICETABLE = new HashMap<>();

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

		GUI = Bukkit.getServer().createInventory(null, ROWS * COLS,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &rSell"));
		loadSellValues();

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

	public void loadSellValues() {

		Item item;

		ConfigurationSection config = Main.getInstance().getCustomConfig();

		for (String str : config.getKeys(true)) {
			item = new Item();
			List<Map<?, ?>> citem = config.getMapList(str);
			for (Map<?, ?> map : citem) {

				try {
					if (map.containsKey("id")) {
						String itemID = (String) map.get("id");
						if (itemID.contains(":")) {
							itemID = StringUtils.substringBefore(itemID, ":");
							String data = (String) map.get("id");
							data = StringUtils.substringAfter(data, ":");
							item.setId(Integer.parseInt(itemID));
							item.setData(Integer.parseInt(data));
						} else {
							item.setId(Integer.parseInt(itemID));
						}

					} else if (map.containsKey("buy-price")) {
						Integer buy;
						Double buy2;
						try {
							buy2 = (Double) map.get("buy-price");
							item.setBuyPrice(buy2);
						} catch (Exception e) {
							buy = (Integer) map.get("buy-price");
							item.setBuyPrice(buy);
						}
					} else if (map.containsKey("sell-price")) {
						Double sell2;
						Integer sell3;
						try {
							sell2 = (Double) map.get("sell-price");
							item.setSellPrice(sell2);
						} catch (Exception e) {
							sell3 = (Integer) map.get("sell-price");
							item.setSellPrice(sell3);
						}
					}
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}

			if (item.getSellPrice() != 0) {

				PRICETABLE.put(item.getId() + ":" + item.getData(),
						new Price(item.getBuyPrice(), item.getSellPrice(), 1));
			}

		}
		open();

	}

	/**
	 * Sell items inside the {@link Sell} GUI.
	 * 
	 */
	@SuppressWarnings("deprecation")
	public void sell() {

		double moneyToGive = 0;
		for (ItemStack item : GUI.getContents()) {
			Integer data = 0;
			if (item == null) {
				continue;
			}

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

			if (!PRICETABLE.containsKey(item.getData().getItemTypeId() + ":" + data)) {
				player.getInventory().addItem(item);
				player.sendMessage(Utils.getPrefix() + Utils.getCantSell());
				continue;

			}
			Double sellPrice = PRICETABLE.get(item.getTypeId() + ":" + data).getSellPrice();

			Integer quantity = item.getAmount();

			moneyToGive += quantity * sellPrice;

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
		if (event.getPlayer().getName().equalsIgnoreCase(this.player.getName())) {
			if (Main.HAS_SELL_OPEN.contains(player.getName())) {
				HandlerList.unregisterAll(this);
				sell();
				Main.HAS_SELL_OPEN.remove(player.getName());
				return;
			}
		}
	}

}
