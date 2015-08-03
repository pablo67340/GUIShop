package com.pablo67340.GUIShop.Handlers;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.GUIShop.Main.Main;

public class Item {
    static Main plugin;

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

	public Item(Main main){
		plugin = main;
	}

	public void buildItem(String name1, Integer itemID1, Integer qty1, String sell1, Integer buy1, Integer slot1, String line11, String line21, String line31, String line41){
		item = new ItemStack(Material.getMaterial(itemID), qty, data);
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
		if (!(isInteger(sell))){
			addPrice2(item, Integer.valueOf(buy));
		}else{
			addPrice(item, buy, Integer.parseInt(sell));
		}
	}


	public void addPrice(ItemStack item2, Integer price, Integer sell){
		ItemMeta itm = item2.getItemMeta();
		List<String> itmlore = Arrays.asList(new String[] { ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost")) +" §c$§0,§c" + price, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("return"))+" §a$§0.§a" + sell, ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line4")) });
		itm.setLore(itmlore);
		item2.setItemMeta(itm);
		item = item2;
	}

	public void addPrice2(ItemStack item2, Integer price){
		ItemMeta itm = item2.getItemMeta();
		List<String> itmlore = Arrays.asList(new String[] { ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost"))+" §c$§0,§c" + price, "§7Cannot Resell", ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("line4")) });
		itm.setLore(itmlore);
		item2.setItemMeta(itm);
		item = item2;
	}
	
	public static boolean isInteger(String s) {
		boolean isInt = plugin.utils.isInteger(s,10);
		return isInt; 
	}
	
	public ItemStack getItem(){
		return item;
	}
	
	public int getSlot(){
		return slot;
	}
	


}
