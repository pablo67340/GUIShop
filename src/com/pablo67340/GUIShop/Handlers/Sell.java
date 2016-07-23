package com.pablo67340.GUIShop.Handlers;


import com.pablo67340.GUIShop.Main.Main;


import net.milkbowl.vault.economy.EconomyResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
	protected ArrayList<String> itemComp = new ArrayList<String>();
	protected ArrayList<Double> itemSell = new ArrayList<Double>();
	protected HashMap<String, String> sellable = new HashMap<String, String>();
	protected HashMap<String, Double> sellitems = new HashMap<String, Double>();
	protected Map<String, Integer> sellqty = new HashMap<String, Integer>();
	protected Double price;


	public Sell(Main main) {
		plugin = main;
		price = 0.0;
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

	public void addSell(String item, String sell) {
		sellable.put(item, sell);
	}

	public Double getSell(String input, Integer qty) {
		if (plugin.utils.getVerbose()){
			System.out.println("Sellables: "+sellable);
		}
		if (sellable.containsKey(input)){
			if (plugin.utils.getVerbose()){
				System.out.println("Sellables contains item");
				System.out.println("SELLABLE PRICE: "+sellable.get(input));
			}
			Double price = Double.parseDouble(sellable.get(input));
			return (price / qty);
		}
		return 0.0;
	}

	@SuppressWarnings("deprecation")
	public void trySell(Player p, Inventory inv) {
		int slot = -1;
		for (ItemStack is : inv) {
			slot++;
			if (is == null) {
				if (plugin.utils.getVerbose()) {
					System.out.println("Slot: " + slot + " Was null!");
				}
			}else{
				int itemID = is.getTypeId();
				short dataID = is.getDurability();
				String parsedItem = String.valueOf(Integer.toString(itemID)) + ":" + Integer.toString(dataID);
				if (plugin.utils.getVerbose()) {
					System.out.println("Slot: " + slot + " was NOT null! Parsed item: " + parsedItem);
				}

				double pricep = getSell(parsedItem, compareQty(parsedItem));
				if (plugin.utils.getVerbose()) {
					System.out.println("SELL PER BLOCK: " + pricep);
				}
				price = price + pricep*is.getAmount();

			}
			if (price==0 || price < 1){
				System.out.println("[GUIShop]");
				Material itm;
				Integer am = 1;
				if(inv.getItem(slot) != null){
					itm = inv.getItem(slot).getType();
					am = inv.getItem(slot).getAmount();
					p.getInventory().addItem(new ItemStack(itm, am));
					inv.setItem(slot, new ItemStack(Material.AIR));
					p.sendMessage(plugin.utils.getPrefix()+ChatColor.translateAlternateColorCodes('&', " "+plugin.getConfig().getString("cant-sell")));
				}
			}
		}
		if (plugin.utils.getVerbose()) {
			System.out.println("Total payout: "+price);
		}
		if (price != 0){
			EconomyResponse r = plugin.econ.depositPlayer(p.getName(), price);
			if (r.transactionSuccess()) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("sold")) + " $" + price + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("added")));
				price = 0.0;
			}
		}


	}

	// Need to check if items can be sold and if not, return the item to the player... Economy should work fine. Hopefully. If not thats what i was working on
	// 103 - 104

	public Integer compareQty(String input) {
		if (sellqty.containsKey(input)) {
			return sellqty.get(input);
		}
		return null;
	}
}

