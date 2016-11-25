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
			if (item.getDurability() != 0){
				if (!Main.PRICES.containsKey(Integer.toString(item.getTypeId())+":"+Integer.toString(item.getDurability()))) {
					player.getInventory().addItem(item);
					continue;


				}
			}else{
				if (!Main.PRICES.containsKey(Integer.toString(item.getTypeId()))) {
					player.getInventory().addItem(item);
					continue;


				}
			}
			
			
			
			if (item.getDurability() != 0){
				moneyToGive += Main.PRICES.get(item.getTypeId()+":"+item.getDurability()).getSellPrice() * item.getAmount();
			}else{
				moneyToGive += Main.PRICES.get(Integer.toString(item.getTypeId())).getSellPrice() * item.getAmount();
			}

			
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
