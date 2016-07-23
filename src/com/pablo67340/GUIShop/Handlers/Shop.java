package com.pablo67340.GUIShop.Handlers;


import com.pablo67340.GUIShop.Handlers.Enchantments;

import com.pablo67340.GUIShop.Main.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;

public class Shop {
	protected Main plugin;
	protected String itemString;
	protected HashMap<String, Double> price = new HashMap<>();
	protected HashMap<String, Double> sell = new HashMap<>();
	protected HashMap<String, Integer> slot = new HashMap<>();
	protected HashMap<String, Integer> itemID = new HashMap<>();
	protected HashMap<String, Integer> dataID = new HashMap<>();
	protected HashMap<String, Integer> qty = new HashMap<>();
	protected HashMap<String, String> enchants = new HashMap<>();
	protected ArrayList<String> sopen = new ArrayList<>();
	protected Inventory shop;
	protected HashMap<String, Inventory> shops = new HashMap<>();
	protected HashMap<Integer, Inventory> shopinv = new HashMap<>();
	protected String[] enc;
	protected int lvl = 0;
	protected String ench = "";
	protected String shopn = "";
	protected List<String> nodes;
	protected String openedShop;

	public Shop(Main main) {
		plugin = main;
		itemString = "";
	}

	public void setShopName(String input) {
		shopn = input;
	}

	public String getShopName() {
		return shopn;
	}

	public boolean setName(String input) {
		itemString = input;
		return true;
	}

	public Boolean addItem(ItemStack item, Integer slot) {
		shop.setItem(slot.intValue(), item);
		return true;
	}

	public Inventory getShop(String input){
		return shops.get(input);
	}

	public void setOpenedShop(String input){
		openedShop = input;
	}

	public String getOpenedShop(){
		return openedShop;
	}

	@SuppressWarnings("deprecation")
	public void preLoadShops(){
		int row = 9;
		int size = 5;
		if (plugin.menu.title.length() > 16) {
			plugin.menu.title.substring(0, 16);
		}


		for (int x = 0;x<=plugin.menu.shops.size()-1;x++){
			setShopName(plugin.menu.shops.get(x)+".");
			if (!shops.containsKey(getShopName())){
				if (plugin.utils.getVerbose()){
					System.out.println("[GUIShop] Shops have not pre-loaded! Pre-loading shops...");

				}
				shop = Bukkit.getServer().createInventory((InventoryHolder)null, row * size, getShopName().replace(".", ""));
				for (int i = 0; i <= 43; ++i) {
					ItemMeta itemmeta;
					int bid;
					ItemMeta itemmeta2;
					ItemStack backbutton;
					String[] backi;
					int bme;
					String name = "null";
					String data = "0";
					String qty = "1";
					String slot = "0";
					String price = "0";
					String sell = "0";
					String item = "0";
					Boolean isSpawner = false;
					if (plugin.getCustomConfig().get(String.valueOf(getShopName()) + i) == null) continue;
					nodes = plugin.getCustomConfig().getStringList(String.valueOf(getShopName()) + i);
					if (plugin.utils.getVerbose()) {
						System.out.println("[GUIShop] Final item built: " + nodes + " Active!");
					}
					if (nodes != null) {
						for (int nodeapi = 0; nodeapi < nodes.size(); ++nodeapi) {
							if (plugin.utils.getVerbose()) {
								System.out.println("[GUIShop] Scanning shops.yml");
							}
							if ((nodes.get(nodeapi)).contains("item:")) {
								item = (nodes.get(nodeapi)).replace("item:", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item ID found: " + item);
								}
							}
							if ((nodes.get(nodeapi)).contains("slot:")) {
								slot = (nodes.get(nodeapi)).replace("slot:", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Slot found: " + slot);
								}
							}
							if ((nodes.get(nodeapi)).contains("name:")) {
								name = (nodes.get(nodeapi)).replace("name:", "").replace("'", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item name found: " + name);
								}
							}
							if ((nodes.get(nodeapi)).contains("price:")) {
								price = (nodes.get(nodeapi)).replace("price:", "").replace("'", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item price found: " + price);
								}
							}
							if ((nodes.get(nodeapi)).contains("data:")) {
								data = (nodes.get(nodeapi)).replace("data:", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Data value found: " + data);
								}
								if (item.equalsIgnoreCase("52")) {
									isSpawner = true;
									if (plugin.utils.verbose) {
										System.out.println("[GUIShop] Item IS a mob spawner! Beginning alternate data organizing!");
									}
								} else if (plugin.utils.verbose) {
									System.out.println("[GUIShop] Bypassed item ID code, Was not a spawner!");
								}
							}
							if ((nodes.get(nodeapi)).contains("enchantments:")) {
								ench = (nodes.get(nodeapi)).replace("enchantments:", "").replace("'", "");
								enc = ench.split(":| ");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Optional enchants found!: " + ench);
								}
							}
							if ((nodes.get(nodeapi)).contains("qty:")) {
								qty = (nodes.get(nodeapi)).replace("qty:", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item quantity found: " + qty);
								}
							}
							if ((nodes.get(nodeapi)).contains("sell:")){
								sell = (nodes.get(nodeapi)).replace("sell:", "").replace("'", "");
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item sell price found: " + sell);
								}

								if (plugin.sellitems.contains(String.valueOf(item) + "$" + sell + ")" + qty)){
									plugin.sellitems.add(String.valueOf(item) + "$" + sell + ")" + qty);
								}
							}
						}
					}
					String sellparse = item + ":" + data;
					plugin.sell.addSell(sellparse, sell);
					if (plugin.utils.getVerbose()) {
						System.out.println("[GUIShop] Added ITEM TO SELLABLES: "+sellparse);
					}
					plugin.sell.sellqty.put(sellparse, Integer.parseInt(qty));
					if (Integer.parseInt(data) != 0) {
						if (plugin.utils.getVerbose()) {
							System.out.println("[GUIShop] ItemWithDataSelected!!!!!");
						}
						ItemStack itemwithdata = new ItemStack(Material.getMaterial((int)Integer.parseInt(item)), Integer.parseInt(qty), (short)Integer.parseInt(data));
						if (plugin.utils.getVerbose()) {
							System.out.println("[GUIShop] Adding item: " + nodes + " To inventory");
						}
						if (!isInteger(sell)) {
							if (isSpawner) {
								plugin.item.addPrice2(itemwithdata, Integer.parseInt(price), true, Integer.parseInt(data));
							} else {
								plugin.item.addPrice2(itemwithdata, Integer.parseInt(price), false, 0);
							}
							itemwithdata = plugin.item.item;
						} else {
							if (isSpawner) {
								plugin.item.addPrice(itemwithdata, Integer.parseInt(price), Integer.parseInt(sell), true, Integer.parseInt(data));
							} else {
								plugin.item.addPrice(itemwithdata, Integer.parseInt(price), Integer.parseInt(sell), false, 0);
							}
							itemwithdata = plugin.item.item;
						}
						if (plugin.utils.getVerbose()) {
							System.out.println("[GUIShop] AddPrice Method Passed! ");
						}
						if (name != "null") {
							itemmeta = itemwithdata.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
							itemwithdata.setItemMeta(itemmeta);
							if (plugin.utils.getVerbose()) {
								System.out.println("[GUIShop] Item name found! Item meta added!");
							}
						} else if (plugin.utils.getVerbose()) {
							System.out.println("[GUIShop] NO Item name found! Breaking!");
						}
						if (enc != null) {
							for (int e = -1; e < enc.length; e+=2) {
								if (e < 0) continue;
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Enchants split into values!: ");
								}
								if (enc[e - 1] == null && enc[e] == null) {
									if (!plugin.utils.getVerbose()) continue;
									System.out.println("[GUIShop] Enchantments are null!");
									continue;
								}
								lvl = Integer.parseInt(enc[e]);
								itemwithdata.addUnsafeEnchantment(Enchantments.getByName(enc[e - 1]), lvl);
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Enchant values: Enchant name: " + enc[e - 1] + " Enchant Level: " + lvl);
								}
								enc[e] = null;
								enc[e - 1] = null;
							}
						}
						if (Integer.parseInt(slot) != 44) {
							if (plugin.getConfig().getString("back-button-item").contains(":")) {
								shop.setItem(Integer.parseInt(slot), itemwithdata);
								backi = plugin.getConfig().getString("back-button-item").split(":");
								bid = Integer.parseInt(backi[0]);
								bme = Integer.parseInt(backi[1]);
								backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)bme);
								itemmeta2 = backbutton.getItemMeta();
								itemmeta2.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
								backbutton.setItemMeta(itemmeta2);
								shop.setItem(44, backbutton);
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item with no data: " + (Object)itemwithdata + " Added to shop!");
								}
							} else {
								int bid2 = Integer.parseInt("back-button-item");
								ItemStack backbutton2 = new ItemStack(Material.getMaterial(bid2), 1, (short)0);
								ItemMeta itemmeta3 = backbutton2.getItemMeta();
								itemmeta3.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
								backbutton2.setItemMeta(itemmeta3);
								shop.setItem(44, backbutton2);
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Item with no data: " + (Object)itemwithdata + " Added to shop!");
								}
							}
						} else if (plugin.utils.getVerbose()) {
							System.out.println("[GUIShop] ERROR: An Item tried to overwrite button slot!");
						}
					} else {
						if (plugin.utils.getVerbose()) {
							System.out.println("[GUIShop] ITEM WITHOUT DATA!");
						}
						if (Material.getMaterial((int)i) == null) {
							System.out.print(i);
						}
						Integer.parseInt(qty);
						ItemStack itemnodata = new ItemStack(Material.getMaterial((int)Integer.parseInt(item)), Integer.parseInt(qty));
						if (!isInteger(sell)) {
							if (isSpawner) {
								plugin.item.addPrice2(itemnodata, Integer.parseInt(price), true, 90);
							} else {
								plugin.item.addPrice2(itemnodata, Integer.parseInt(price), false, 90);
							}
							itemnodata = plugin.item.item;
						} else {
							if (isSpawner) {
								plugin.item.addPrice(itemnodata, Integer.parseInt(price), Integer.parseInt(sell), true, 90);
							} else {
								plugin.item.addPrice(itemnodata, Integer.parseInt(price), Integer.parseInt(sell), false, 0);
							}
							itemnodata = plugin.item.item;
						}
						if (name != "null") {
							itemmeta = itemnodata.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
							itemnodata.setItemMeta(itemmeta);
						}
						if (enc != null) {
							for (int e = -1; e < enc.length; e+=2) {
								if (e < 0) continue;
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Enchants split into values!: ");
								}
								if (enc[e - 1] == null && enc[e] == null) {
									if (!plugin.utils.getVerbose()) continue;
									System.out.println("[GUIShop] Enchantments are null!");
									continue;
								}
								lvl = Integer.parseInt(enc[e]);
								itemnodata.addUnsafeEnchantment(Enchantments.getByName(enc[e - 1]), lvl);
								if (plugin.utils.getVerbose()) {
									System.out.println("[GUIShop] Enchant values: Enchant name: " + enc[e - 1] + " Enchant Level: " + lvl);
								}
								enc[e - 1] = null;
								enc[e] = null;
							}
						}
						shop.setItem(Integer.parseInt(slot), itemnodata);
						if (plugin.getConfig().getString("back-button-item").contains(":")) {
							backi = plugin.getConfig().getString("back-button-item").split(":");
							bid = Integer.parseInt(backi[0]);
							bme = Integer.parseInt(backi[1]);
							backbutton = new ItemStack(Material.getMaterial((int)bid), 1, (short)bme);
							itemmeta2 = backbutton.getItemMeta();
							itemmeta2.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
							backbutton.setItemMeta(itemmeta2);
							shop.setItem(44, backbutton);
							if (plugin.utils.getVerbose()) {
								System.out.println("[GUIShop] Item with no data: " + (Object)itemnodata + " Added to shop!");
							}
						} else {
							int bid3 = Integer.parseInt("back-button-item");
							ItemStack backbutton3 = new ItemStack(Material.getMaterial(bid3), 1, (short)0);
							ItemMeta itemmeta4 = backbutton3.getItemMeta();
							itemmeta4.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")));
							backbutton3.setItemMeta(itemmeta4);
							shop.setItem(44, backbutton3);
							if (plugin.utils.getVerbose()) {
								System.out.println("[GUIShop] Item with no data: " + (Object)itemnodata + " Added to shop!");
							}
						}
					}
					shopinv.put(1, shop);
				}
				shops.put(getShopName(), shop);
			}else{
				shop = shops.get(getShopName());
				if (plugin.utils.getVerbose()){
					System.out.println("[GUIShop] Shops already loaded! Using pre-loaded shops.");
				}

			}
		}
	}

	public void openShop(Player p){
		shop = shops.get(getOpenedShop());
		if (plugin.listene.shopOpen.contains(p.getName())){
			
		}else{
			plugin.listene.shopOpen.add(p.getName());
		}
		p.openInventory(shop);
	}

	public boolean isInteger(String s) {
		return plugin.utils.isInteger(s, 10);
	}

	public void addOpen(String input){
		sopen.add(input);
	}

	public void removeOpen(String input){
		sopen.remove(input);
	}
}

