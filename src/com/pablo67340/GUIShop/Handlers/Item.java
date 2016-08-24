package com.pablo67340.GUIShop.Handlers;


import com.pablo67340.GUIShop.Main.Main;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Item {
	protected Main plugin;
	protected String name;
	protected Integer qty;
	protected Integer itemID;
	protected String sell;
	protected Integer buy;
	protected Integer slot;
	protected Short data;
	protected String line1;
	protected String line2;
	protected String line3;
	protected String line4;
	protected ItemStack item;
	protected boolean isSpawner;
	protected int mobid;

	public Item(Main main) {
		plugin = main;
	}

	@SuppressWarnings("deprecation")
	public void buildItem(String name1, Integer itemID1, Integer qty1, String sell1, Integer buy1, Integer slot1, String line11, String line21, String line31, String line41, Boolean isSpawner2, Integer mobid2) {
		item = new ItemStack(Material.getMaterial(itemID), qty.intValue(), data.shortValue());
		name = name1;
		itemID = itemID1;
		qty = qty1;
		sell = sell1;
		buy = buy1;
		slot = slot1;
		line1 = line11;
		line2 = line21;
		line3 = line31;
		line4 = line41;
		mobid = mobid2;
		isSpawner = isSpawner2;
		if (!isInteger(sell)) {
			addPrice2(item, buy, isSpawner, mobid);
		} else {
			addPrice(item, buy, Integer.parseInt(sell), isSpawner, mobid);
		}
	}

	public void addPrice(ItemStack item2, Integer price, Integer sell, Boolean isSpawner, Integer mobid) {
		ItemMeta itm = item2.getItemMeta();
		List<String> itmlore = isSpawner != false ? Arrays.asList(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost"))) + " \u00a7c$\u00a70,\u00a7c" + price, String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("return"))) + " \u00a7a$\u00a70.\u00a7a" + sell, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line4")), "Mob ID: " + mobid) : Arrays.asList(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost"))) + " \u00a7c$\u00a70,\u00a7c" + price, String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("return"))) + " \u00a7a$\u00a70.\u00a7a" + sell, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line4")));
		itm.setLore(itmlore);
		item2.setItemMeta(itm);
		item = item2;
	}

	public void addPrice2(ItemStack item2, Integer price, Boolean isSpawner, Integer mobid) {
		ItemMeta itm = item2.getItemMeta();
		List<String> itmlore = isSpawner != false ? Arrays.asList(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost"))) + " \u00a7c$\u00a70,\u00a7c" + price, String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("return"))) + " \u00a7a$\u00a70.\u00a7a" + sell, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line4")), "Mob ID: " + mobid) : Arrays.asList(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost"))) + " \u00a7c$\u00a70,\u00a7c" + price, "\u00a77Cannot Resell", ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line4")));
		itm.setLore(itmlore);
		item2.setItemMeta(itm);
		item = item2;
	}

	public boolean isInteger(String s) {
		return plugin.utils.isInteger(s, 10);
	}

	public ItemStack getItem() {
		return item;
	}

	public int getSlot() {
		return slot;
	}
}

