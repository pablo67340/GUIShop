package com.pablo67340.GUIShop.Handlers;


import com.pablo67340.GUIShop.Main.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class DataLoader {
	protected String item = "";
	protected String data = "";
	protected String slot = "";
	protected String price = "";
	protected String sell = "";
	protected String name = "";
	protected String enchantments = "";
	protected String qty = "";
	protected Main plugin;
	protected Boolean isSpawner;
	protected String ench;
	protected String[] enc;
	protected int index;
	protected ArrayList<String> items = new ArrayList<String>();
	protected String shop;
	protected List<String> shops = new ArrayList<String>();
	public Map<String, Integer> sellqty = new HashMap<String, Integer>();

	public DataLoader(Main main) {
		plugin = main;
		isSpawner = false;
		ench = "";
		index = 0;
	}

	public void loadData(String shopn) {
		shop = plugin.getCustomConfig().getKeys(false).toString();
		String shop2 = shop.replace((CharSequence)"[", (CharSequence)"").replace((CharSequence)"]", (CharSequence)"");
		shops = Arrays.asList(shop2.split("\\s*,\\s*"));
		for (int i2 = 0; i2 <= shops.size() - 1; ++i2) {
			if (plugin.utils.getVerbose().booleanValue()) {
				System.out.println("SHOP " + i2 + " IS " + shops.get(i2));
			}
			for (int i = 0; i <= 43; ++i) {
				List<String> nodes = plugin.getCustomConfig().getStringList(String.valueOf(shops.get(i2)) + i);
				if (nodes == null) continue;
				for (int nodeapi = 0; nodeapi < nodes.size(); ++nodeapi) {
					if (plugin.utils.getVerbose().booleanValue()) {
						System.out.println("Scanning shops.yml");
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"item:")) {
						item = (nodes.get(nodeapi)).replace((CharSequence)"item:", (CharSequence)"");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Item ID found: " + item);
						}
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"slot:")) {
						slot = (nodes.get(nodeapi)).replace((CharSequence)"slot:", (CharSequence)"");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Slot found: " + slot);
						}
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"name:")) {
						name = (nodes.get(nodeapi)).replace((CharSequence)"name:", (CharSequence)"").replace((CharSequence)"'", (CharSequence)"");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Item name found: " + name);
						}
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"price:")) {
						price = (nodes.get(nodeapi)).replace((CharSequence)"price:", (CharSequence)"").replace((CharSequence)"'", (CharSequence)"");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Item price found: " + price);
						}
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"data:")) {
						data = (nodes.get(nodeapi)).replace((CharSequence)"data:", (CharSequence)"");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Data value found: " + data);
						}
						if (item.equalsIgnoreCase("52")) {
							isSpawner = true;
							if (plugin.utils.verbose) {
								System.out.println("Item IS a mob spawner! Beginning alternate data organizing!");
							}
						} else if (plugin.utils.verbose) {
							System.out.println("Bypassed item ID code, Was not a spawner!");
						}
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"enchantments:")) {
						ench = (nodes.get(nodeapi)).replace((CharSequence)"enchantments:", (CharSequence)"").replace((CharSequence)"'", (CharSequence)"");
						enc = ench.split(":| ");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Optional enchants found!: " + ench);
						}
					}
					if ((nodes.get(nodeapi)).contains((CharSequence)"qty:")) {
						qty = (nodes.get(nodeapi)).replace((CharSequence)"qty:", (CharSequence)"");
						if (plugin.utils.getVerbose().booleanValue()) {
							System.out.println("Item quantity found: " + qty);
						}
					}
					if (!(nodes.get(nodeapi)).contains((CharSequence)"sell:")) continue;
					sell = (nodes.get(nodeapi)).replace((CharSequence)"sell:", (CharSequence)"").replace((CharSequence)"'", (CharSequence)"");
					if (plugin.utils.getVerbose().booleanValue()) {
						System.out.println("Item sell price found: " + sell);
					}
					String parsed = "{" + item + ":" + data + "," + name + "," + slot + "," + price + "," + sell + "," + qty + "," + enchantments + "," + isSpawner + "}";
					String sellparse = String.valueOf(item) + ":" + data;
					System.out.println("PARSED: " + parsed);
					System.out.println("NODES: " + nodes);
					System.out.println("NODEAPI: " + nodeapi);
					if (sell.equalsIgnoreCase("false")) continue;
					sellqty.put(sellparse, Integer.parseInt(qty));
				}
			}
		}
	}

	public Integer compareQty(String input) {
		if (sellqty.containsKey(input)) {
			return sellqty.get(input);
		}
		return null;
	}
}

