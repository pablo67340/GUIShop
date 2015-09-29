package com.pablo67340.GUIShop.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.GUIShop.Main.Main;


public class Shop {
	static Main plugin;

	protected String itemString;
	protected HashMap<String, Double> price = new HashMap <>();
	protected HashMap<String, Double> sell = new HashMap <>();
	protected HashMap<String, Integer> slot = new HashMap <>();
	protected HashMap<String, Integer> itemID = new HashMap <>();
	protected HashMap<String, Integer> dataID = new HashMap <>();
	protected HashMap<String, Integer> qty = new HashMap <>();
	protected HashMap<String, String> enchants = new HashMap <>();
	protected Inventory shop;
	ArrayList<String> sopen = new ArrayList<String>();
	HashMap<Integer, Inventory> shopinv = new HashMap<Integer, Inventory>();
	String[] enc;
	int lvl = 0;
	String ench = "";
	String shopn = "";




	public Shop(Main main){
		plugin = main;
		itemString = "";
	}

	public void setShopName(String input){
		shopn = input;
	}

	public String getShopName(){
		return shopn;
	}

	public boolean setName(String input){
		itemString = input;
		return true;
	}

	public Boolean addItem(ItemStack item, Integer slot){
		shop.setItem(slot, item);
		return true;

	}

	public void loadShop(Player plyr){
		if (!this.sopen.contains(plyr.getName())) {
			this.sopen.add(plyr.getName());
		}
		this.shopinv.clear();

		int row = 9;
		int size = 5;
		if (plugin.menu.title.length() > 16) {
			plugin.menu.title.substring(0, 16);
		}
		Inventory shop = Bukkit.getServer().createInventory(plyr, row * size, getShopName().replace(".", ""));
		String saved = getShopName().replaceAll("[\\s.]", "");
		if (plugin.cache.isSaved(saved))
		{
			if (plugin.utils.getVerbose()) {
				System.out.println("isSaved check passed! Attempting to open shop with the saved inventory: " + saved);
			}
			shop.setContents(plugin.cache.getShop(saved));
			plyr.openInventory(shop);
		}else{
			if (this.shopinv.isEmpty()) {
				for (int i = 0; i <= 43; i++){
					String name = "null";
					String data = "0";
					String qty = "1";
					String slot = "0";
					String price = "0";
					String sell = "0";
					String item = "0";
					Boolean isSpawner = false;
					if (plugin.utils.getVerbose()){
						System.out.println("TEST TEST: "+getShopName());
					}
					if (plugin.getCustomConfig().get(getShopName() + i) != null){

						List<String> nodes = plugin.getCustomConfig().getStringList(getShopName() + i);
						if (plugin.utils.getVerbose()) {
							System.out.println("Final item built: " + nodes + " Active!");
						}
						if (nodes != null) {
							for (int nodeapi = 0; nodeapi < nodes.size(); nodeapi++){
								if (plugin.utils.getVerbose()) {
									System.out.println("Scanning shops.yml");
								}
								if (((String)nodes.get(nodeapi)).contains("item:")){
									item = ((String)nodes.get(nodeapi)).replace("item:", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Item ID found: " + item);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("slot:")){
									slot = ((String)nodes.get(nodeapi)).replace("slot:", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Slot found: " + slot);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("name:")){
									name = ((String)nodes.get(nodeapi)).replace("name:", "").replace("'", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Item name found: " + name);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("price:")){
									price = ((String)nodes.get(nodeapi)).replace("price:", "").replace("'", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Item price found: " + price);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("data:")){
									data = ((String)nodes.get(nodeapi)).replace("data:", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Data value found: " + data);
									}
									if (item.equalsIgnoreCase("52")){
										isSpawner = true;
										if (plugin.utils.verbose){
											System.out.println("Item IS a mob spawner! Beginning alternate data organizing!");
										}

									}else{
										if (plugin.utils.verbose){
											System.out.println("Bypassed item ID code, Was not a spawner!");
										}
									}
								}
								if (((String)nodes.get(nodeapi)).contains("enchantments:")){
									ench = ((String)nodes.get(nodeapi)).replace("enchantments:", "").replace("'", "");
									this.enc = ench.split(":| ");
									if (plugin.utils.getVerbose()) {
										System.out.println("Optional enchants found!: " + this.ench);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("qty:")){
									qty = ((String)nodes.get(nodeapi)).replace("qty:", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Item quantity found: " + qty);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("sell:")){
									sell = ((String)nodes.get(nodeapi)).replace("sell:", "").replace("'", "");
									if (plugin.utils.getVerbose()) {
										System.out.println("Item sell price found: " + sell);
									}
									if (!plugin.sellitems.contains(item + "$" + sell + ")" + qty)) {
										plugin.sellitems.add(item + "$" + sell + ")" + qty);
									}
								}
							}
						}
						if (Integer.parseInt(data) != 0){
							if (plugin.utils.getVerbose()){
								System.out.println("ItemWithDataSelected!!!!!");
							}
							ItemStack itemwithdata = new ItemStack(Material.getMaterial(Integer.parseInt(item)), Integer.parseInt(qty), (short)Integer.parseInt(data));
							if (plugin.utils.getVerbose()) {
								System.out.println("Adding item: " + nodes + " To inventory");
							}
							if (!(isInteger(sell))){
								if (isSpawner){
									plugin.item.addPrice2(itemwithdata, Integer.valueOf(Integer.parseInt(price)), true, Integer.parseInt(data));

								}else{
									plugin.item.addPrice2(itemwithdata, Integer.valueOf(Integer.parseInt(price)), false, 0);
								}
								itemwithdata = plugin.item.item;
							}else{

								if (isSpawner){
									plugin.item.addPrice(itemwithdata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)), true, Integer.parseInt(data));
								}else{
									plugin.item.addPrice(itemwithdata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)), false, 0);

								}
								itemwithdata = plugin.item.item;
							}

							if (plugin.utils.getVerbose()) {
								System.out.println("AddPrice Method Passed! ");
							}
							if (name != "null"){
								ItemMeta itemmeta = itemwithdata.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
								itemwithdata.setItemMeta(itemmeta);
								if (plugin.utils.getVerbose()) {
									System.out.println("Item name found! Item meta added!");
								}
							}
							else if (plugin.utils.getVerbose()){
								System.out.println("NO Item name found! Breaking!");
							}
							if (this.enc != null) {
								for (int e = -1; e < this.enc.length; e += 2) {
									if (e >= 0){
										if (plugin.utils.getVerbose()) {
											System.out.println("Enchants split into values!: ");
										}
										if ((this.enc[(e - 1)] == null) && (this.enc[e] == null)){
											if (plugin.utils.getVerbose()) {
												System.out.println("Enchantments are null!");
											}
										}else{
											this.lvl = Integer.parseInt(this.enc[e]);
											itemwithdata.addUnsafeEnchantment(Enchantments.getByName(this.enc[(e - 1)]), this.lvl);
											if (plugin.utils.getVerbose()) {
												System.out.println("Enchant values: Enchant name: " + this.enc[(e - 1)] + " Enchant Level: " + this.lvl);
											}
											this.enc[e] = null;
											this.enc[(e - 1)] = null;
										}
									}
								}
							}
							if (Integer.parseInt(slot) != 44){

								if (plugin.getConfig().getString("back-button-item").contains(":")){
									shop.setItem(Integer.parseInt(slot), itemwithdata);
									String[] backi = plugin.getConfig().getString("back-button-item").split(":");
									int bid = Integer.parseInt(backi[0]);
									int bme = Integer.parseInt(backi[1]);
									ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)bme);
									ItemMeta itemmeta = backbutton.getItemMeta();
									itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
									backbutton.setItemMeta(itemmeta);
									shop.setItem(44, backbutton);

									if (plugin.utils.getVerbose()) {
										System.out.println("Item with no data: " + itemwithdata + " Added to shop!");
									}
								}else{
									int bid = Integer.parseInt("back-button-item");
									ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)0);
									ItemMeta itemmeta = backbutton.getItemMeta();
									itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
									backbutton.setItemMeta(itemmeta);
									shop.setItem(44, backbutton);

									if (plugin.utils.getVerbose()) {
										System.out.println("Item with no data: " + itemwithdata + " Added to shop!");
									}
								}
							}else if (plugin.utils.getVerbose()){
								System.out.println("ERROR: An Item tried to overwrite button slot!");
							}
						}else{
							if (plugin.utils.getVerbose()){
								System.out.println("ITEM WITHOUT DATA!");
							}
							if (Material.getMaterial(i) == null) {
								System.out.print(i);
							}
							if (Integer.parseInt(qty) < 1) {

							}
							ItemStack itemnodata = new ItemStack(Material.getMaterial(Integer.parseInt(item)), Integer.parseInt(qty));
							if (!(isInteger(sell))){
								if (isSpawner){
									plugin.item.addPrice2(itemnodata, Integer.valueOf(Integer.parseInt(price)), true, 90);
								}else{
									plugin.item.addPrice2(itemnodata, Integer.valueOf(Integer.parseInt(price)), false, 90);
								}

								itemnodata = plugin.item.item;
							}else{
								if (isSpawner){
									plugin.item.addPrice(itemnodata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)), true, 90);
								}else{
									plugin.item.addPrice(itemnodata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)), false, 0);
								}

								itemnodata = plugin.item.item;
							}

							if (name != "null"){
								ItemMeta itemmeta = itemnodata.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
								itemnodata.setItemMeta(itemmeta);
							}
							if (this.enc != null) {
								for (int e = -1; e < this.enc.length; e += 2) {
									if (e >= 0){
										if (plugin.utils.getVerbose()) {
											System.out.println("Enchants split into values!: ");
										}
										if ((this.enc[(e - 1)] == null) && (this.enc[e] == null)){
											if (plugin.utils.getVerbose()) {
												System.out.println("Enchantments are null!");
											}
										}else{
											this.lvl = Integer.parseInt(this.enc[e]);
											itemnodata.addUnsafeEnchantment(Enchantments.getByName(this.enc[(e - 1)]), this.lvl);
											if (plugin.utils.getVerbose()) {
												System.out.println("Enchant values: Enchant name: " + this.enc[(e - 1)] + " Enchant Level: " + this.lvl);
											}
											this.enc[(e - 1)] = null;
											this.enc[e] = null;
										}
									}
								}
							}
							shop.setItem(Integer.parseInt(slot), itemnodata);
							if (plugin.getConfig().getString("back-button-item").contains(":")){
								String[] backi = plugin.getConfig().getString("back-button-item").split(":");
								int bid = Integer.parseInt(backi[0]);
								int bme = Integer.parseInt(backi[1]);
								ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)bme);
								ItemMeta itemmeta = backbutton.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
								backbutton.setItemMeta(itemmeta);
								shop.setItem(44, backbutton);

								if (plugin.utils.getVerbose()) {
									System.out.println("Item with no data: " + itemnodata + " Added to shop!");
								}
							}else{
								int bid = Integer.parseInt("back-button-item");
								ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)0);
								ItemMeta itemmeta = backbutton.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
								backbutton.setItemMeta(itemmeta);
								shop.setItem(44, backbutton);

								if (plugin.utils.getVerbose()) {
									System.out.println("Item with no data: " + itemnodata + " Added to shop!");
								}
							}

						}

						this.shopinv.put(Integer.valueOf(1), shop);
						plyr.openInventory(shop);
						plyr.updateInventory();
					}
				}
			}

			if (plugin.cache.saveShop(saved, shop)){
				if (plugin.utils.getVerbose()){
					System.out.println("Saved Shop: " + getShopName());
					System.out.println("SHOP CONTENTS: ");
					System.out.println(plugin.cache.getShop(plugin.menu.shopn));
				}
			}else{
				if (plugin.utils.getVerbose()) {
					System.out.println("Shop already exists!");
				}
			}
		}
	}

	public void trySell(Player p, ItemStack item)
	{
		boolean tally = false;
		boolean err = false;
		if (plugin.utils.verbose){
			System.out.println("TrySell: "+item.getTypeId());
		}
		if (((item != null) || (item == null)) && (!item.getItemMeta().hasLore()) && (p.getInventory().contains(item)))
		{
			if (plugin.utils.verbose){
				System.out.println("Item passed secondary checks!");
			}
			for (String str : plugin.sellitems)
			{
				String itemid = StringUtils.substringBefore(str, "$");
				if (plugin.utils.verbose) {
					System.out.println("Item in hand: " + item.getTypeId() + " compared: " + itemid);
				}
				if (item.getTypeId() == Integer.parseInt(itemid))
				{
					tally = true;
					String amount = StringUtils.substringAfter(str, ")");
					if (Integer.parseInt(amount) == item.getAmount())
					{
						String preprice = StringUtils.substringAfter(str, "$");
						String price = StringUtils.substringBefore(preprice, ")");

						if (!(isInteger(price))){
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cant-sell")));
						}else{
							p.getInventory().removeItem(new ItemStack[] { item });
							EconomyResponse r = plugin.econ.depositPlayer(p.getName(), Integer.parseInt(price));
							if (r.transactionSuccess())
							{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("sold")) + amount + " " + item.getType().toString().toLowerCase() + "§f!");
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + "§a$" + price + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("added")));
							}
							else
							{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("something-wrong")));
							}
						}
					}
					else
					{
						int dif = Integer.parseInt(amount);
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("in-stacks")) + dif);
						err = true;
					}
				}else{
					if (plugin.utils.verbose){
						System.out.println("Compared "+item + " to "+itemid + " And did not match!");
					}
				}
			}
			if (!tally)
			{
				if (err != true){
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cant-sell")));
					tally = true;
				}else{

				}
			}
		}
		else if (plugin.utils.verbose)
		{
			System.out.println("Else Triggered for item!");
		}

	}

	public static boolean isInteger(String s) {
		boolean isInt = plugin.utils.isInteger(s,10);
		return isInt; 
	}


}
