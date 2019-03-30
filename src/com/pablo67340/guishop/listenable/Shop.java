package com.pablo67340.guishop.listenable;

import java.util.*;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.pablo67340.guishop.definition.Enchantments;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.handler.Item;
import com.pablo67340.guishop.handler.Page;
import com.pablo67340.guishop.handler.Price;
import com.pablo67340.guishop.main.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.Dependencies;
import com.pablo67340.guishop.util.XMaterial;
import com.songoda.epicspawners.api.EpicSpawners;

import de.dustplanet.util.SilkUtil;

public final class Shop implements Listener {

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
	private Integer currentPage = 0;

	/**
	 * Total loaded page count of this {@link Shop}
	 */
	private Integer pageCount;

	private Player user;

	/**
	 * The constructor for a {@link Shop}.
	 * 
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore, Integer slot, Player player) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.user = player;
	}

	/**
	 * Return the current page number of this {@link Shop}
	 */
	public Integer getCurrentPage() {
		return currentPage;
	}

	/**
	 * Returns true if this {@link Shop} has pages.
	 */
	public Boolean hasPages() {
		return hasPages;
	}

	/**
	 * Sets current page number loaded {@link Shop}
	 */
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
	 * Returns the page object for this {@link Shop}
	 */
	public Page getPage(Integer input) {
		return pages[input];
	}

	private final Map<Integer, Price> PRICETABLE = new HashMap<>();

	/**
	 * Load the specified shop
	 * 
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void loadShop() {

		GUI = Bukkit.getServer().createInventory(null, ROW * COL,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());

		for (Integer i = 0; i > ROW * COL; i++) {
			GUI.addItem(new ItemStack(Material.AIR));
		}

		ITEMS = new Item[45];

		pageCount = 0;

		Integer lastIndex = 0;

		Integer index = 0;

		Item item = new Item();

		Boolean pendingSave = false;

		ConfigurationSection config = Main.getInstance().getCustomConfig().getConfigurationSection(shop);

		for (String str : config.getKeys(true)) {

			item = new Item();
			index += 1;

			List<Map<?, ?>> citem = config.getMapList(str);

			for (Map<?, ?> map : citem) {

				try {
					if (map.containsKey("id")) {
						String itemID = (String) map.get("id");
						item.setMaterial(itemID);
					} else if (map.containsKey("mobType")) {
						item.setMobType((String) map.get("mobType"));
					} else if (map.containsKey("slot")) {
						item.setSlot((Integer) map.get("slot"));
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
					} else if (map.containsKey("type")) {
						ItemType type = ItemType.valueOf((String) map.get("type"));
						item.setType(type);
					} else if (map.containsKey("commands")) {
						item.setCommands((List<String>) map.get("commands"));
					}
				} catch (Exception e) {
					Main.getInstance().getLogger().warning("§cError occured while reading item: " + (index - 1)
							+ " from shop: " + getShop() + " Error: " + e.getMessage());
					Main.getInstance().getLogger()
							.warning("§cThis plugin will not function properly until error is addressed!");
					Main.getInstance().getDebugger().setHasExploded(true);
					Main.getInstance().getDebugger().setErrorMessage(
							"§cError occured while reading item: " + (index - 1) + " from shop: " + getShop());
				}
			}
			// Update shops.yml to add type
			if (item.getItemType() == null) {
				item.setType(ItemType.ITEM);

				List<Map<?, ?>> mapList = config.getMapList(index.toString());
				Map<String, String> type = new HashMap<>();
				type.put("type", item.getItemType().toString());
				mapList.add(type);
				config.set(index.toString(), mapList);
				pendingSave = true;
			}

			PRICETABLE.put(item.getSlot(), new Price(item.getBuyPrice(), item.getSellPrice()));

			ITEMS[item.getSlot()] = item;
			Material material = null;
			System.out.println("Item: "+item.getSlot());
			if (material == null) {
				if ((material = XMaterial.valueOf(item.getMaterial()).parseMaterial()) == null) {
					Main.getInstance().getLogger().log(Level.WARNING, "Could not parse material: "+item.getMaterial()+" for item #: "+item.getSlot()+1);
					continue;
				}
			}

			ItemStack itemStack = new ItemStack(material, 1);

			if (item.isMobSpawner()) {
				if (Main.getInstance().usesSpawners()) {
					if (Dependencies.hasDependency("SilkSpawners")) {
						SilkUtil su = (SilkUtil) Main.getInstance().getSpawnerObject();
						itemStack = su.setSpawnerType(itemStack, item.getMobType().toLowerCase(), item.getMobType());
					} else if (Dependencies.hasDependency("EpicSpawners")) {
						EpicSpawners es = (EpicSpawners) Main.getInstance().getSpawnerObject();
						itemStack = es.newSpawnerItem(es.getSpawnerManager().getSpawnerData(item.getMobType()), 1);

					}
				}

			}

			ItemMeta itemMeta = itemStack.getItemMeta();

			if (item.getBuyPrice() != 0 && item.getSellPrice() != 0) {

				itemMeta.setLore(Arrays.asList(
						ChatColor.translateAlternateColorCodes('&',
								"&fBuy: &c" + Config.getCurrency() + item.getBuyPrice()),
						ChatColor.translateAlternateColorCodes('&',
								"&fSell: &a" + Config.getCurrency() + item.getSellPrice())));
			} else if (item.getBuyPrice() == 0) {
				itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cCannot be purchased"),
						ChatColor.translateAlternateColorCodes('&',
								"&fSell: &a" + Config.getCurrency() + item.getSellPrice())));
			} else {
				itemMeta.setLore(Arrays.asList(
						ChatColor.translateAlternateColorCodes('&',
								"&fBuy: &c" + Config.getCurrency() + item.getBuyPrice()),
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
			if (!Config.getEscapeOnly()) {

				String backButtonId = Main.INSTANCE.getConfig().getString("back-button-item");

				ItemStack backButtonItem = new ItemStack(Material.getMaterial(backButtonId));

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

			if (!Config.getEscapeOnly()) {
				int backButton = 0;
				short data = 0;

				String backButtonId = Main.INSTANCE.getMainConfig().getString("back-button-item");

				if (backButtonId.contains(":")) {
					String[] args = backButtonId.split(":");

					backButton = Integer.parseInt(args[0]);
					data = Short.parseShort(args[1]);
				}

				ItemStack backButtonItem = new ItemStack(Main.getInstance().findMaterial(backButton), 1, data);

				ItemMeta backButtonMeta = backButtonItem.getItemMeta();

				backButtonMeta.setDisplayName(
						ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getMainConfig().getString("back")));

				backButtonItem.setItemMeta(backButtonMeta);

				GUI.setItem(ROW * COL - 1, backButtonItem);
			}
		}
		if (pendingSave) {
			Main.getInstance().saveShopConfig();
		}

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
			@Override
			public void run() {
				open();
			}
		}, 1L);

	}

	/**
	 * Preload a page into the GUI. This is required before opening a shop.
	 */
	public void loadPage(Integer page) {
		GUI.clear();
		for (Item item : pages[page].getContents()) {
			if (item != null) {
				ItemStack itemStack;
				if (!item.isMobSpawner()) {

					itemStack = new ItemStack(Material.getMaterial(item.getMaterial()), 1);
				} else {
					itemStack = new ItemStack(Material.getMaterial(item.getMaterial()), 1);

					if (Main.getInstance().usesSpawners()) {
						if (Dependencies.hasDependency("SilkSpawners")) {
							SilkUtil su = (SilkUtil) Main.getInstance().getSpawnerObject();
							itemStack = su.setSpawnerType(itemStack, item.getMobType(), item.getMobType());
						} else if (Dependencies.hasDependency("EpicSpawners")) {
							EpicSpawners es = (EpicSpawners) Main.getInstance().getSpawnerObject();
							itemStack = es.newSpawnerItem(es.getSpawnerManager().getSpawnerData(item.getMobType()), 1);

						}
					}

				}
				ItemMeta itemMeta = itemStack.getItemMeta();

				if (item.getBuyPrice() != 0 && item.getSellPrice() != 0) {

					itemMeta.setLore(Arrays.asList(
							ChatColor.translateAlternateColorCodes('&',
									"&fBuy: &c" + Config.getCurrency() + item.getBuyPrice()),
							ChatColor.translateAlternateColorCodes('&',
									"&fSell: &a" + Config.getCurrency() + item.getSellPrice())));
				} else if (item.getBuyPrice() == 0) {
					itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cCannot be purchased"),
							ChatColor.translateAlternateColorCodes('&',
									"&fSell: &a" + Config.getCurrency() + item.getSellPrice())));
				} else {
					itemMeta.setLore(Arrays.asList(
							ChatColor.translateAlternateColorCodes('&',
									"&fBuy: &c" + Config.getCurrency() + item.getBuyPrice()),
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
				if (!Config.getEscapeOnly()) {

					String backButtonId = Main.INSTANCE.getConfig().getString("back-button-item");

					ItemStack backButtonItem = new ItemStack(Material.getMaterial(backButtonId));

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
	public void open() {
		if (hasPages) {
			currentPage = 0;

			loadPage(0);
		}
		user.openInventory(GUI);
		Main.HAS_SHOP_OPEN.add(user.getName());
	}

	/**
	 * Safely navigate back to menu.
	 * 
	 */
	public void closeAndOpenMenu(String player) {

		Bukkit.getPlayer(player).closeInventory();

		Main.HAS_MENU_OPEN.add(player);
		Menu menu = new Menu(player);
		menu.open();
		Main.HAS_SHOP_OPEN.remove(player);
	}

	/**
	 * Apply back/forward buttons to the GUI. Check if users are on last or first
	 * page adjust buttons accordingly.
	 */
	public void applyButtons() {
		ItemStack goButtonItem = new ItemStack(Material.getMaterial("BLUE_WOOL"));

		ItemMeta goButtonMeta = goButtonItem.getItemMeta();

		if (getCurrentPage() != (pageCount)) {
			goButtonItem = new ItemStack(Material.getMaterial("BLUE_WOOL"));

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ">"));

			goButtonItem.setItemMeta(goButtonMeta);

		} else {
			goButtonItem = new ItemStack(Material.getMaterial("RED_WOOL"));

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', ">"));

			goButtonItem.setItemMeta(goButtonMeta);
		}

		GUI.setItem(GUI.getSize() - 2, goButtonItem);

		if (getCurrentPage() != 0) {
			goButtonItem = new ItemStack(Material.getMaterial("BLUE_WOOL"));

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "<"));

			goButtonItem.setItemMeta(goButtonMeta);

		} else {
			goButtonItem = new ItemStack(Material.getMaterial("RED_WOOL"));

			goButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "<"));

			goButtonItem.setItemMeta(goButtonMeta);
		}
		GUI.setItem(46, goButtonItem);

	}

	/**
	 * The click listener for the Shop inventory.
	 */
	@SuppressWarnings({ "deprecation" })
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (e.getClickedInventory() != null) {
				Player player = (Player) e.getWhoClicked();
				if (player.getName().equals(this.user.getName())) {

					/*
					 * If the player has the shop open.
					 */
					if (Main.HAS_SHOP_OPEN.contains(player.getName())) {
						e.setCancelled(true);
						if (player.getInventory().firstEmpty() == -1) {
							e.setCancelled(true);
							player.sendMessage(Config.getFull());
							return;
						}
						/*
						 * If the player clicks on an empty slot, then cancel the event.
						 */
						if (e.getCurrentItem() != null) {
							if (e.getCurrentItem().getType() == Material.AIR) {
								e.setCancelled(true);
								return;
							}
						}

						/*
						 * If the player clicks in their own inventory, we want to cancel the event.
						 */
						if (e.getClickedInventory() == player.getInventory()) {
							e.setCancelled(true);
							return;
						}

						if (e.getSlot() >= 0 && e.getSlot() < getGUI().getSize()) {
							/**
							 * If the player clicks the 'back' button, then open the menu. Otherwise, If the
							 * user clicks the forward button, load and open next page, Otherwise, If the
							 * user clicks the backward button, load and open the previous page, Otherwise
							 * Attempt to purchase the clicked item.
							 */
							if (e.getSlot() == getGUI().getSize() - 1) {
								e.setCancelled(true);
								closeAndOpenMenu(player.getName());
								return;
							} else if (e.getSlot() == getGUI().getSize() - 2) {
								e.setCancelled(true);
								if (e.getCurrentItem().getData().getData() != 14) {

									loadPage(getCurrentPage() + 1);
								}

							} else if (e.getSlot() == 46) {
								e.setCancelled(true);
								if (e.getCurrentItem().getData().getData() != 14) {

									loadPage(getCurrentPage() - 1);
								}
							} else {

								/*
								 * If the player has enough money to purchase the item, then allow them to.
								 */
								Item item;
								if (hasPages()) {
									item = getPage(getCurrentPage()).getContents()[e.getSlot()];
								} else {
									item = getItems()[e.getSlot()];
								}
								if (item.getItemType() == ItemType.COMMAND) {

									if (Main.getInstance().purchaseCommands(player.getUniqueId(), item.getCommands())) {
										if (Main.getEconomy().withdrawPlayer(player, item.getBuyPrice())
												.transactionSuccess()) {
											// If the player has the sound enabled, play
											// it!
											if (Config.isSoundEnabled()) {
												try {
													player.playSound(player.getLocation(),
															Sound.valueOf(Config.getSound()), 1, 1);

												} catch (Exception ex) {
													Main.getInstance().getLogger().warning(
															"§cIncorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
												}
											}
											player.sendMessage(
													Config.getPrefix() + Config.getPurchased() + item.getBuyPrice()
															+ Config.getTaken() + Config.getCurrencySuffix());
										} else {
											player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre()
													+ item.getBuyPrice() + Config.getNotEnoughPost());
										}
									} else {
										player.sendMessage(Config.getCommandAlready());
									}

								} else {

									Quantity qty = new Quantity(player.getName(), item, this);
									Bukkit.getServer().getPluginManager().registerEvents(qty, Main.getInstance());
									unregisterClass(player.getName());
									qty.open();
									Main.HAS_QTY_OPEN.add(player.getName());
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Inventory close handler for the Shop
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onClose(InventoryCloseEvent e) {
		String playerName = e.getPlayer().getName();
		if (playerName.equals(this.user.getName())) {
			if (Main.HAS_SHOP_OPEN.contains(playerName)) {
				if (!Config.getEscapeOnly()) {
					HandlerList.unregisterAll(this);
					Main.HAS_SHOP_OPEN.remove(playerName);
					return;
				} else {
					HandlerList.unregisterAll(this);
					Main.HAS_SHOP_OPEN.remove(playerName);
					Menu men = new Menu(playerName);
					men.open();
					return;
				}
			}
		}
	}

	/**
	 * Stops listening and garbages class.
	 */
	public void unregisterClass(String playerName) {
		HandlerList.unregisterAll(this);
		Main.HAS_SHOP_OPEN.remove(playerName);
	}

}
