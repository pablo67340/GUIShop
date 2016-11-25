package com.pablo67340.shop.handler;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.shop.main.Main;

public final class Shop {

	public static final int ROW = 4;

	public static final int COL = 9;

	/**
	 * The name of this {@link Shop}.
	 */
	private final String name;

	/**
	 * The description of this {@link Shop}.
	 */
	private final String description;

	/**
	 * The lore of this {@link Shop}.
	 */
	private final List<String> lore;

	/**
	 * The GUI that will hold every {@link Item}
	 * in this {@link Shop}.
	 */
	private Inventory GUI;

	/**
	 * The list of {@link Item}s in this
	 * {@link Shop}.
	 */
	private Item[] ITEMS;

	/**
	 * The constructor for a {@link Shop}.
	 * 
	 * @param name
	 * 		The name of the shop.
	 * @param description
	 * 		The description of the shop.
	 * @param lore
	 * 		The lore of the shop.
	 */
	public Shop(String name, String description, List<String> lore) {
		this.name = name;
		this.description = description;
		this.lore = lore;
	}

	/**
	 * Gets the name of the {@link Shop}.
	 * 
	 * @return the shop's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the description of the {@link Shop}.
	 * 
	 * @return the shop's description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the lore of the {@link Shop}.
	 * 
	 * @return the shop's lore.
	 */
	public List<String> getLore() {
		return lore;
	}

	/**
	 * Gets the GUI of the {@link Shop}.
	 * 
	 * @return the shop's GUI.
	 */
	public Inventory getGUI() {
		return GUI;
	}

	/**
	 * Gets the items of the {@link Shop}.
	 * 
	 * @return the shop's items.
	 */
	public Item[] getItems() {
		return ITEMS;
	}

	public static void loadShops() {
		int numberOfShops = Main.INSTANCE.getConfig().getInt("menu-rows") * Main.INSTANCE.getConfig().getInt("menu-cols");

		for (int i = 0; i < numberOfShops; i++) {
			if (!Main.INSTANCE.getConfig().getBoolean(String.valueOf(i + 1) + ".Enabled")) {
				continue;
			}

			String name = ChatColor.translateAlternateColorCodes('&', 
					Main.INSTANCE.getConfig().getString(String.valueOf(i + 1) + ".Name"));

			String description = ChatColor.translateAlternateColorCodes('&',
					Main.INSTANCE.getConfig().getString(String.valueOf(i + 1) + ".Desc"));

			List<String> lore = new ArrayList<>();

			if (description != null && description.length() > 0) {
				lore.add(description);
			}

			Main.SHOPS.put(i, new Shop(name, description, lore));
		}

		for (Shop s : Main.SHOPS.values()) {
			s.loadShop();
		}
	}

	@SuppressWarnings("deprecation")
	public void loadShop() {
		GUI = Bukkit.getServer().createInventory(null, ROW * COL, 
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());

		ITEMS = new Item[GUI.getSize() - 1];

		for (int i = 1; i < GUI.getSize(); i++) {
			String itemDef = Main.INSTANCE.getCustomConfig().getString(getName() + "." + i);

			if (itemDef == null || itemDef.length() <= 2) {
				continue;
			}


			String[] lines = itemDef.substring(1, itemDef.length() - 1).replaceAll("[{}]", "").split(", ");

			if (lines == null || lines.length == 0) {
				continue;
			}

			Item item = new Item();
			Boolean data = false;

			for (String line : lines) {
				String[] args = line.split("=");

				switch (args[0]) {
				case "slot":
					item.setSlot(Integer.parseInt(args[1]));
					break;
				case "id":
					if (args[1].contains(":")){
						String split = StringUtils.substringAfter(args[1], ":");
						String id = StringUtils.substringBefore(args[1], ":");
						item.setId(Integer.parseInt(id));
						item.setData(Integer.parseInt(split));
						data = true;
						break;
					}else{
						item.setId(Integer.parseInt(args[1]));
						break;
					}
				case "buy-price":
					item.setBuyPrice(Double.parseDouble(args[1]));
					break;
				case "sell-price":
					item.setSellPrice(Double.parseDouble(args[1]));
					if (data == true){
						Main.PRICES.put(item.getId()+":"+item.getData(), new Price(item.getBuyPrice(), item.getSellPrice()));
					}else{
						Main.PRICES.put(Integer.toString(item.getId()), new Price(item.getBuyPrice(), item.getSellPrice()));
					}
					break;
				}
			}

			ITEMS[item.getSlot()] = item;

			ItemStack itemStack = new ItemStack(Material.AIR);
			if (data == true){
				itemStack = new ItemStack(item.getId(), 1, (short)item.getData());
			}else{
				itemStack = new ItemStack(item.getId(), 1);
			}

			ItemMeta itemMeta = itemStack.getItemMeta();

			itemMeta.setLore(Arrays.asList(
					ChatColor.translateAlternateColorCodes('&', "&fBuy: &c$" + item.getBuyPrice()), 
					ChatColor.translateAlternateColorCodes('&', "&fSell: &a$" + item.getSellPrice()))
					);

			itemStack.setItemMeta(itemMeta);

			GUI.setItem(item.getSlot(), itemStack);
		}


		if (!Utils.getEscapeOnly()){
			int backButton = 0;
			short data = 0;

			String backButtonId = Main.INSTANCE.getConfig().getString("back-button-item");

			if (backButtonId.contains(":")) {
				String[] args = backButtonId.split(":");

				backButton = Integer.parseInt(args[0]);
				data = Short.parseShort(args[1]);
			}

			ItemStack backButtonItem = new ItemStack(Material.getMaterial(backButton), 1, data);

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			backButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
					Main.INSTANCE.getConfig().getString("back")));

			backButtonItem.setItemMeta(backButtonMeta);

			GUI.setItem(ROW * COL - 1, backButtonItem);
		}

	}

	public void open(Player player) {
		player.openInventory(GUI);

		Main.HAS_SHOP_OPEN.put(player.getName(), this);
	}

	public void closeAndOpenMenu(Player player) {
		Main.HAS_SHOP_OPEN.remove(player.getName());

		Main.MENUS.get(player.getName()).open();
	}

}
