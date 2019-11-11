package com.pablo67340.guishop.listenable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;

import com.github.stefvanschie.inventoryframework.Gui;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.definition.MobType;
import com.pablo67340.guishop.handler.Item;
import com.pablo67340.guishop.handler.Price;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;

public final class Sell {

	private final Map<String, Price> PRICETABLE = new HashMap<>();

	private Gui GUI;

	/**
	 * Constructor, set player and load GUI.
	 */
	Sell() {
		load();
	}

	/**
	 * Open the {@link Sell} GUI.
	 */
	void open(Player player) {
		GUI.show(player);
	}

	void load() {
		GUI = new Gui(Main.getINSTANCE(), 6, ChatColor.translateAlternateColorCodes('&', "Menu &f> &rSell"));
		GUI.setOnClose(this::onSellClose);
		StaticPane pane = new StaticPane(0, 0, 9, 6);
		GUI.addPane(pane);
		Item item;

		ConfigurationSection config = Main.getINSTANCE().getCustomConfig();

		for (String str : config.getKeys(true)) {
			item = new Item();
			List<Map<?, ?>> citem = config.getMapList(str);
			for (Map<?, ?> map : citem) {

				try {
					if (map.containsKey("id")) {
						String itemID = (String) map.get("id");
						item.setMaterial(itemID);
					} else if (map.containsKey("mobType")) {
						item.setMobType((String) map.get("mobType"));
					} else if (map.containsKey("buy-price")) {
						item.setBuyPrice(map.get("buy-price"));
					} else if (map.containsKey("sell-price")) {
						item.setSellPrice(map.get("sell-price"));
					}
				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}

			if (item.getSellPrice() != null && (Double) item.getSellPrice() != 0.0) {
				if (item.isMobSpawner()) {
					PRICETABLE.put(item.getMaterial() + ":" + item.getMobType().toLowerCase(),
							new Price((Double) item.getBuyPrice(), (Double) item.getSellPrice()));
				} else {
					PRICETABLE.put(item.getMaterial(),
							new Price((Double) item.getBuyPrice(), (Double) item.getSellPrice()));
				}
			}
		}
	}

	/**
	 * Sell items inside the {@link Sell} GUI.
	 */
	@SuppressWarnings("deprecation")
	public void sell(Player player) {

		double moneyToGive = 0;
		boolean couldntSell = false;
		int countSell = 0;
		for (ItemStack item : GUI.getInventory().getContents()) {
			Object data;
			if (item == null) {
				continue;
			}

			Double sellPrice;

			if (Objects.requireNonNull(item.getData()).getItemType().getId() == 52) {

				NBTTagCompound cmp = ItemNBTUtil.getTag(item);
				data = MobType.valueOf(cmp.getString("EntityId"));
				System.out.println("DATA: " + data);

				if (!PRICETABLE.containsKey(item.getType().toString() + ":" + data)) {
					player.getInventory().addItem(item);
					player.sendMessage(Config.getPrefix() + Config.getCantSell());
					continue;

				}

				sellPrice = PRICETABLE.get(item.getType().toString() + ":" + data).getSellPrice();

			} else {
				if (!PRICETABLE.containsKey(item.getType().toString())) {
					countSell += 1;
					couldntSell = true;
					player.getInventory().addItem(item);
					continue;
				}

				sellPrice = PRICETABLE.get(item.getType().toString()).getSellPrice();

			}

			Integer quantity = item.getAmount();

			moneyToGive += quantity * sellPrice;

		}

		if (couldntSell) {
			player.sendMessage(Config.getPrefix() + " " + Config.getCantSell().replace("{count}", countSell + ""));
		}

		Main.getECONOMY().depositPlayer(player.getName(), moneyToGive);

		if (moneyToGive > 0) {
			player.sendMessage(Config.getSold() + moneyToGive + Config.getAdded());
		}
		GUI.getInventory().clear();
	}

	private void onSellClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		sell(player);
	}

}
