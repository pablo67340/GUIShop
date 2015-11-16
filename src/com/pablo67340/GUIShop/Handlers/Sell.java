package com.pablo67340.GUIShop.Handlers;


import com.pablo67340.GUIShop.Main.Main;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import org.bukkit.inventory.ItemStack;


public class Sell {
	protected Main plugin;
	protected String title;
	protected String shopn;
	protected ArrayList<String> slopen = new ArrayList<String>();
	protected Inventory shop;
	ArrayList<String> itemComp = new ArrayList<String>();
	ArrayList<Double> itemSell = new ArrayList<Double>();

	public Sell(Main main) {
		plugin = main;
	}

	public void loadSell(Player p) {
		if (!slopen.contains(p.getName())) {
			slopen.add(p.getName());
		}
		int row = 9;
		int size = 5;
		if (plugin.utils.getSellTitle().length() > 16) {
			plugin.utils.getSellTitle().substring(0, 16);
		}
		shop = Bukkit.getServer().createInventory((InventoryHolder)p, row * size, ChatColor.translateAlternateColorCodes('&', plugin.utils.getSellTitle()));
		p.openInventory(shop);
	}

	public Inventory getSellInv() {
		return shop;
	}

	public void addSell(String item, Double sell) {
		itemComp.add(item);
		itemSell.add(sell);
	}

	public Double getSell(String input, Integer qty) {
		int i = 0;
		if (i < itemComp.size()) {
			String item2 = itemComp.get(i);
			if (plugin.utils.getVerbose().booleanValue()) {
				System.out.println("Comparing item: " + item2 + " to " + input);
			}
			if (input.equals(item2)) {
				if (plugin.utils.getVerbose().booleanValue()) {
					System.out.println("Price found! Loading!");
				}
				return itemSell.get(i) / (double)qty.intValue();
			}
			return 0.0;
		}
		return 0.0;
	}

	@SuppressWarnings("deprecation")
	public void trySell(Player p, Inventory inv) {
		int slot = -1;
		for (ItemStack is : inv) {
			slot++;
			if (is == null) {
				if (!plugin.utils.getVerbose().booleanValue()) continue;
				System.out.println("Slot: " + slot + " Was null!");
				continue;
			}
			int itemID = is.getTypeId();
			short dataID = is.getDurability();
			String parsedItem = String.valueOf(Integer.toString(itemID)) + ":" + Integer.toString(dataID);
			if (plugin.utils.getVerbose().booleanValue()) {
				System.out.println("Slot: " + slot + " was NOT null! Parsed item: " + parsedItem);
			}
			if (itemComp.contains(parsedItem)) continue;
			itemComp.add(parsedItem);
			double price = getSell(parsedItem, plugin.dataLoader.compareQty(parsedItem));
			itemSell.add(price);
			System.out.println("SELL PER BLOCK: " + price);
		}
	}
}

