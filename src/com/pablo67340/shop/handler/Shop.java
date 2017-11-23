package com.pablo67340.shop.handler;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.shop.main.Main;

public final class Shop {

	/**
	 * Number of rows for the GUI.,
	 */
	public static final int ROW = 6;

	/**
	 * Number of columns for the GUI
	 */
	public static final int COL = 9;

	/**
	 * The name of this {@link Shop}.
	 */
	private final String name;

	/**
	 * The shop name of this {@link Shop}.
	 */
	private final String shop;

	/**
	 * The description of this {@link Shop}.
	 */
	private final String description;

	/**
	 * The lore of this {@link Shop}.
	 */
	private final List<String> lore;

	/**
	 * The GUI that will hold every {@link Item} in this {@link Shop}.
	 */
	private Inventory GUI;

	/**
	 * The list of {@link Item}s in this {@link Shop}.
	 */
	private Item[] ITEMS;

	/**
	 * The list of {@link Page}'s in this {@link Shop}.
	 */
	private Page[] pages = new Page[20];

	/**
	 * True/False if the current {@link Shop} has more than 1 page.
	 */
	private Boolean hasPages = false;

	/**
	 * The current page number a user is currently browsing in their {@link Shop}
	 */
	private Integer currentPage;

	/**
	 * Total loaded page count of this {@link Shop}
	 */
	private Integer pageCount;

	/**
	 * The constructor for a {@link Shop}.
	 * 
	 * @param name
	 *            The name of the shop.
	 * @param description
	 *            The description of the shop.
	 * @param lore
	 *            The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
	}

	public Integer getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(Integer input) {
		currentPage = input;
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
	 * Gets the shop of the {@link Shop}.
	 * 
	 * @return the shop's shop.
	 */
	public String getShop() {
		return shop;
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

	/**
	 * Loads all global shops.
	 * 
	 */
	public static void loadShops() {
		Main.SHOPS.clear();
		int numberOfShops = Utils.getMenuRows() * 9;

		for (int i = 0; i < numberOfShops; i++) {
			if (!Main.INSTANCE.getMainConfig().getBoolean(String.valueOf(i + 1) + ".Enabled")) {
				continue;
			}

			String shop = ChatColor.translateAlternateColorCodes('&',
					Main.INSTANCE.getMainConfig().getString(String.valueOf(i + 1) + ".Shop"));

			String name = ChatColor.translateAlternateColorCodes('&',
					Main.INSTANCE.getMainConfig().getString(String.valueOf(i + 1) + ".Name"));

			String description = ChatColor.translateAlternateColorCodes('&',
					Main.INSTANCE.getMainConfig().getString(String.valueOf(i + 1) + ".Desc"));

			List<String> lore = new ArrayList<>();

			if (description != null && description.length() > 0) {
				lore.add(description);
			}

			Main.SHOPS.put(i, new Shop(shop, name, description, lore));
		}

		for (Shop s : Main.SHOPS.values()) {
			s.loadShop2();
		}
	}

	/**
	 * Load the specified shop
	 * 
	 */
	@SuppressWarnings({ "deprecation" })
	public void loadShop2() {

		GUI = Bukkit.getServer().createInventory(null, ROW * COL,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());

		ITEMS = new Item[45];

		pageCount = 0;

		Integer lastIndex = 0;

		Integer index = 0;

		for (String str : Main.getInstance().getCustomConfig().getKeys(true)) {
			if (str.contains(".") && str.contains(getShop())) {

				Item item = new Item();

				List<Map<?, ?>> citem = Main.getInstance().getCustomConfig().getMapList(str);

				index += 1;

				for (Map<?, ?> map : citem) {

					try {
						if (map.containsKey("id")) {
							String itemID = (String) map.get("id");
							if (itemID.contains(":")) {
								itemID = StringUtils.substringBefore(itemID, ":");
							}
							String data = (String) map.get("id");
							data = StringUtils.substringAfter(data, ":");
							item.setId(Integer.parseInt(itemID));
							item.setData(Integer.parseInt(data));
						} else if (map.containsKey("slot")) {
							item.setSlot((Integer) map.get("slot"));
						} else if (map.containsKey("qty")) {
							item.setQty((Integer) map.get("qty"));
						} else if (map.containsKey("name")) {
							item.setName((String) map.get("name"));
						} else if (map.containsKey("enchantments")) {
							String preEnc = (String) map.get("enchantments");
							if (!preEnc.equalsIgnoreCase("")) {
								String[] enchants = preEnc.split(" ");
								item.setEnchantments(enchants);
							}
						} else if (map.containsKey("buy-price")) {
							Integer buy;
							Double buy2;
							try {
								buy2 = (Double) map.get("buy-price");
								item.setBuyPrice(buy2);
							} catch (Exception e) {
								buy = (Integer) map.get("buy-price");
								item.setBuyPrice(buy);
							}
						} else if (map.containsKey("sell-price")) {
							Double sell2;
							Integer sell3;
							try {
								sell2 = (Double) map.get("sell-price");
								item.setSellPrice(sell2);
							} catch (Exception e) {
								sell3 = (Integer) map.get("sell-price");
								item.setSellPrice(sell3);
							}
						}
					} catch (Exception e) {
						Main.getInstance().getLogger().warning(
								"Error occured while reading item: " + (index - 1) + " from shop: " + getShop());
						Main.getInstance().getLogger()
								.warning("This plugin will not function properly until error is addressed!");
						Main.getInstance().getDebugger().setHasExploded(true);
						Main.getInstance().getDebugger().setErrorMessage(
								"Error occured while reading item: " + (index - 1) + " from shop: " + getShop());
					}
				}

				Main.PRICES.put(item.getId() + ":" + item.getData(),
						new Price(item.getBuyPrice(), item.getSellPrice(), item.getQty()));

				ITEMS[item.getSlot()] = item;
				ItemStack itemStack = new ItemStack(item.getId(), item.getQty(), (short) item.getData());
				
				if (item.getId() == 52) {
					itemStack = Main.getInstance().su.setSpawnerType(itemStack, (short) item.getData(),
							Spawners.getMobName(item.getData()));

				}

				ItemMeta itemMeta = itemStack.getItemMeta();

				if (item.getBuyPrice() != 0 && item.getSellPrice() != 0) {

					itemMeta.setLore(Arrays.asList(
							ChatColor.translateAlternateColorCodes('&',
									"&fBuy: &c" + Utils.getCurrency() + item.getBuyPrice()),
							ChatColor.translateAlternateColorCodes('&',
									"&fSell: &a" + Utils.getCurrency() + item.getSellPrice())));
				} else if (item.getBuyPrice() == 0) {
					itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cCannot be purchased"),
							ChatColor.translateAlternateColorCodes('&',
									"&fSell: &a" + Utils.getCurrency() + item.getSellPrice())));
				} else {
					itemMeta.setLore(Arrays.asList(
							ChatColor.translateAlternateColorCodes('&',
									"&fBuy: &c" + Utils.getCurrency() + item.getBuyPrice()),
							ChatColor.translateAlternateColorCodes('&', "&cCannot be sold")));
				}

				if (item.getName() != null)
					itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));

				itemStack.setItemMeta(itemMeta);

				if (item.getEnchantments() != null) {

					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						itemStack.addUnsafeEnchantment(Enchantments.getByName(enchantment), Integer.parseInt(level));

					}
				}

				GUI.setItem(item.getSlot(), itemStack);
				if (!Utils.getEscapeOnly()) {
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

					backButtonMeta.setDisplayName(
							ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getConfig().getString("back")));

					backButtonItem.setItemMeta(backButtonMeta);

					GUI.setItem(ROW * COL - 1, backButtonItem);
				}
				if ((index - lastIndex) == 45) {
					Page pageItem = new Page();

					pageItem.setContents(ITEMS);

					pages[pageCount] = pageItem;

					ITEMS = new Item[45];

					GUI.clear();

					lastIndex = index;

					pageCount += 1;

					hasPages = true;

				} else {
					if (hasPages) {
						Page pageItem = new Page();

						pageItem.setContents(ITEMS);

						pages[pageCount] = pageItem;

						GUI.clear();
					}

				}

			}
		}

		if (!Utils.getEscapeOnly()) {
			int backButton = 0;
			short data = 0;

			String backButtonId = Main.INSTANCE.getMainConfig().getString("back-button-item");

			if (backButtonId.contains(":")) {
				String[] args = backButtonId.split(":");

				backButton = Integer.parseInt(args[0]);
				data = Short.parseShort(args[1]);
			}

			ItemStack backButtonItem = new ItemStack(Material.getMaterial(backButton), 1, data);

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			backButtonMeta.setDisplayName(
					ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getMainConfig().getString("back")));

			backButtonItem.setItemMeta(backButtonMeta);

			GUI.setItem(ROW * COL - 1, backButtonItem);
		}

	}

	/**
	 * Preload a page into the GUI. This is required before opening a shop.
	 */
	@SuppressWarnings("deprecation")
	public void loadPage(Integer page) {
		GUI.clear();
		for (Item item : pages[page].getContents()) {
			if (item != null) {
				ItemStack itemStack;
				if (item.getId() != 52) {

					itemStack = new ItemStack(item.getId(), item.getQty(), (short) item.getData());
				} else {
					itemStack = new ItemStack(item.getId(), item.getQty(), (short) item.getData());
					itemStack = Main.getInstance().su.setSpawnerType(itemStack, (short) item.getData(),
							Spawners.getMobName(item.getData()));
				}
				ItemMeta itemMeta = itemStack.getItemMeta();

				if (item.getBuyPrice() != 0 && item.getSellPrice() != 0) {

					itemMeta.setLore(Arrays.asList(
							ChatColor.translateAlternateColorCodes('&',
									"&fBuy: &c" + Utils.getCurrency() + item.getBuyPrice()),
							ChatColor.translateAlternateColorCodes('&',
									"&fSell: &a" + Utils.getCurrency() + item.getSellPrice())));
				} else if (item.getBuyPrice() == 0) {
					itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cCannot be purchased"),
							ChatColor.translateAlternateColorCodes('&',
									"&fSell: &a" + Utils.getCurrency() + item.getSellPrice())));
				} else {
					itemMeta.setLore(Arrays.asList(
							ChatColor.translateAlternateColorCodes('&',
									"&fBuy: &c" + Utils.getCurrency() + item.getBuyPrice()),
							ChatColor.translateAlternateColorCodes('&', "&cCannot be sold")));
				}

				if (item.getName() != null)
					itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));

				itemStack.setItemMeta(itemMeta);

				if (item.getEnchantments() != null) {

					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						itemStack.addUnsafeEnchantment(Enchantments.getByName(enchantment), Integer.parseInt(level));

					}
				}

				GUI.setItem(item.getSlot(), itemStack);
				if (!Utils.getEscapeOnly()) {
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

					backButtonMeta.setDisplayName(
							ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getConfig().getString("back")));

					backButtonItem.setItemMeta(backButtonMeta);

					GUI.setItem(ROW * COL - 1, backButtonItem);

				}

			}
		}
		setCurrentPage(page);
		applyButtons();
	}

	/**
	 * Open the player's shop
	 * 
	 */
	public void open(Player player) {
		if (hasPages) {
			currentPage = 0;

			loadPage(0);
		}
		player.openInventory(GUI);

		Main.HAS_SHOP_OPEN.put(player.getName(), this);
	}

	/**
	 * Safely navigate back to menu.
	 * 
	 */
	public void closeAndOpenMenu(Player player) {
		Main.HAS_SHOP_OPEN.remove(player.getName());

		Main.MENUS.get(player.getName()).open();
	}

	/**
	 * Apply back/forward buttons to the GUI. Check if users are on last or first
	 * page adjust buttons accordingly.
	 */
	@SuppressWarnings("deprecation")
	public void applyButtons() {
		ItemStack goButtonItem = new ItemStack(Material.getMaterial(35), 1, (short) 11);

		ItemMeta goButtonMeta = goButtonItem.getItemMeta();

		if (getCurrentPage() != (pageCount)) {
			goButtonItem = new ItemStack(Material.getMaterial(35), 1, (short) 11);

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ">"));

			goButtonItem.setItemMeta(goButtonMeta);

		} else {
			goButtonItem = new ItemStack(Material.getMaterial(35), 1, (short) 14);

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ">"));

			goButtonItem.setItemMeta(goButtonMeta);
		}

		GUI.setItem(GUI.getSize() - 2, goButtonItem);

		if (getCurrentPage() != 0) {

			goButtonItem = new ItemStack(Material.getMaterial(35), 1, (short) 11);

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "<"));

			goButtonItem.setItemMeta(goButtonMeta);

		} else {
			goButtonItem = new ItemStack(Material.getMaterial(35), 1, (short) 14);

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "<"));

			goButtonItem.setItemMeta(goButtonMeta);
		}
		GUI.setItem(46, goButtonItem);

	}

}
