package com.pablo67340.shop.handler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

import com.pablo67340.shop.main.Main;

public final class Sell {

	public static final int ROWS = 4;

	public static final int COLS = 9;

	private Inventory GUI;

	private final Player player;

	public Sell(Player player) {
		this.player = player;

		load();
	}

	public void load() {
		GUI = player.getServer().createInventory(null, ROWS * COLS, 
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &rSell"));
	}

	public void open() {
		player.openInventory(GUI);

		Main.HAS_SELL_OPEN.add(player.getName());
	}

	@SuppressWarnings("deprecation")
	public void sell() {
		double moneyToGive = 0;

		for (ItemStack item : GUI.getContents()) {
			if (item == null) {
				continue;
			}
			if (!Main.PRICES.containsKey(item.getData().getItemTypeId()+":"+item.getData().getData())) {
				player.getInventory().addItem(item);
				player.sendMessage(Utils.getPrefix()+Utils.getCantSell());
				continue;


			}
			Double sellPrice = Main.PRICES.get(item.getTypeId()+":"+item.getData().getData()).getSellPrice();

			Integer quantity = Main.PRICES.get(item.getTypeId()+":"+item.getData().getData()).getQuantity();

			Double perEach = sellPrice / quantity;
			
			moneyToGive += perEach * item.getAmount();
			

		}

		Main.getEconomy().depositPlayer(player.getName(), moneyToGive);

		if (moneyToGive > 0) {
			player.sendMessage(Utils.getSold() + moneyToGive + Utils.getAdded());
		}

		GUI.clear();
	}

	public Inventory getGUI() {
		return GUI;
	}

}
