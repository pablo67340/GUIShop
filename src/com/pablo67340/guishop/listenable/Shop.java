package com.pablo67340.guishop.listenable;

import java.util.*;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.pablo67340.guishop.definition.Enchantments;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.handler.Item;
import com.pablo67340.guishop.handler.Page;
import com.pablo67340.guishop.handler.Price;
import com.pablo67340.guishop.main.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.XMaterial;

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
	 * The list of {@link Item}s in this {@link Shop}.
	 */
	private List<Item> ITEMS;

	private List<OutlinePane> outlinePages = new ArrayList<OutlinePane>();

	/**
	 * The list of {@link Page}'s in this {@link Shop}.
	 */
	private Page[] pages = new Page[20];

	/**
	 * The current page number a user is currently browsing in their {@link Shop}
	 */
	private Integer currentPage = 0;

	private Gui GUI;

	private Menu menuInstance;

	private Boolean hasClicked = false;

	/**
	 * The constructor for a {@link Shop}.
	 * 
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore, Integer slot, Player player,
			Menu menuInstance) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
	}

	/**
	 * Return the current page number of this {@link Shop}
	 */
	public Integer getCurrentPage() {
		return currentPage;
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
	 * Gets the items of the {@link Shop}.
	 * 
	 * @return the shop's items.
	 */
	public List<Item> getItems() {
		return ITEMS;
	}

	/**
	 * Returns the page object for this {@link Shop}
	 */
	public Page getPage(Integer input) {
		return pages[input];
	}

	private final Map<Integer, Price> PRICETABLE = new HashMap<>();

	private int pageC = 0;

	/**
	 * Load the specified shop
	 * 
	 */
	@SuppressWarnings({ "unchecked" })
	public void loadShop() {

		ITEMS = new ArrayList<>();

		Integer index = 0, lastIndex = 0;

		Item item = new Item();

		ConfigurationSection config = Main.getInstance().getCustomConfig().getConfigurationSection(shop);

		GUI = new Gui(Main.getInstance(), 6, ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());

		PaginatedPane pane = new PaginatedPane(0, 0, COL, ROW);

		OutlinePane page = new OutlinePane(0, 0, COL, ROW);

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
				// Toggle pending for save.
			}

			PRICETABLE.put(item.getSlot(), new Price(item.getBuyPrice(), item.getSellPrice()));
			ITEMS.add(item);
			Material material = null;
			if (material == null) {
				if ((material = XMaterial.valueOf(item.getMaterial()).parseMaterial()) == null) {
					Main.getInstance().getLogger().log(Level.WARNING,
							"Could not parse material: " + item.getMaterial() + " for item #: " + item.getSlot() + 1);
					continue;
				}
			}

			ItemStack itemStack = new ItemStack(material, 1);

			ItemMeta itemMeta = itemStack.getItemMeta();

			if (item.getBuyPrice() != 0 && item.getSellPrice() != 0) {

				itemMeta.setLore(Arrays.asList(
						ChatColor.translateAlternateColorCodes('&',
								"&fBuy: &c" + Config.getCurrency() + item.getBuyPrice()),
						ChatColor.translateAlternateColorCodes('&',
								"&fSell: &a" + Config.getCurrency() + item.getSellPrice())));
				if (item.getCommands() != null) {
					List<String> currentLore = itemMeta.getLore();
					List<String> commands = item.getCommands();
					List<String> newCommands = new ArrayList<>();
					for (String cmd : commands) {
						newCommands.add(
								StringUtils.substringBefore(cmd, "::") + "   " + StringUtils.substringAfter(cmd, "::"));
					}
					currentLore.add(" ");
					currentLore.add(Config.getAccessTo());
					currentLore.addAll(newCommands);
					itemMeta.setLore(currentLore);
				}
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

			// Create Page
			GuiItem gItem = new GuiItem(itemStack, event -> onShopClick(event));
			if (index == config.getKeys(true).size() || ((index - 1) - lastIndex) == 44) {
				page.addItem(gItem);
				if (config.getKeys(true).size() > 45) {
					applyButtons(pane, page);
				}
				lastIndex = index;
				pane.addPane(pageC, page);
				pageC += 1;
				outlinePages.add(page);
				page = new OutlinePane(0, 0, COL, ROW);
			} else {
				page.addItem(gItem);
			}

			if (index == config.getKeys(true).size()) {
				GUI.addPane(pane);
			}

		}

	}

	public void applyButtons(PaginatedPane pane, OutlinePane page) {
		if (page.getItems().size() == 45) {
			for (int x = page.getItems().size(); x <= 54; x++) {
				page.addItem(new GuiItem(new ItemStack(Material.AIR)));
			}
			page.insertItem(new GuiItem(new ItemStack(Material.ARROW), event -> {
				pane.setPage(pane.getPage() + 1);

				if (pane.getPage() == pane.getPages() - 1) {
					outlinePages.get(currentPage).setVisible(false);
				}
				currentPage += 1;
				hasClicked = true;
				outlinePages.get(currentPage).setVisible(true);

				GUI.update();
			}), 51);
		}
		if (pageC > 0) {
			for (int x = page.getItems().size(); x <= 54; x++) {
				page.addItem(new GuiItem(new ItemStack(Material.AIR)));
			}
			page.insertItem(new GuiItem(new ItemStack(Material.ARROW), event -> {
				pane.setPage(pane.getPage() - 1);

				if (pane.getPage() == 0) {
					outlinePages.get(currentPage).setVisible(false);
				}
				currentPage -= 1;
				hasClicked = true;
				outlinePages.get(currentPage).setVisible(true);

				GUI.update();
			}), 47);
		}
		if (!Config.getEscapeOnly()) {

			ItemStack backButtonItem = new ItemStack(XMaterial.valueOf(Config.getBackButtonItem()).parseMaterial());

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			backButtonMeta.setDisplayName(Config.getBackButtonText());

			backButtonItem.setItemMeta(backButtonMeta);
			
			GuiItem item = new GuiItem(backButtonItem, event -> {
				menuInstance.open();
				return;
			});

			page.insertItem(item, 53);
		}
	}

	/**
	 * Open the player's shop
	 * 
	 */
	public void open(Player input) {
		GUI.setOnClose(event -> onClose(event));
		GUI.show(input);
	}

	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (e.getClickedInventory() != null) {
				Player player = (Player) e.getWhoClicked();
				hasClicked = true;

				/*
				 * If the player has the shop open.
				 */
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

				if (e.getSlot() >= 0 && e.getSlot() < GUI.getItems().size()) {
					/**
					 * If the player clicks the 'back' button, then open the menu. Otherwise, If the
					 * user clicks the forward button, load and open next page, Otherwise, If the
					 * user clicks the backward button, load and open the previous page, Otherwise
					 * Attempt to purchase the clicked item.
					 */

					/*
					 * If the player has enough money to purchase the item, then allow them to.
					 */

					Item item = getItems().get((currentPage * 45) + e.getSlot());

					if (item.getItemType() == ItemType.COMMAND) {

						if (Main.getInstance().purchaseCommands(player.getUniqueId(), item.getCommands())) {
							if (Main.getEconomy().withdrawPlayer(player, item.getBuyPrice()).transactionSuccess()) {
								// If the player has the sound enabled, play
								// it!
								if (Config.isSoundEnabled()) {
									try {
										player.playSound(player.getLocation(), Sound.valueOf(Config.getSound()), 1, 1);

									} catch (Exception ex) {
										Main.getInstance().getLogger().warning(
												"§cIncorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
									}
								}
								player.sendMessage(Config.getPrefix() + Config.getPurchased() + item.getBuyPrice()
										+ Config.getTaken() + Config.getCurrencySuffix());
							} else {
								player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre() + item.getBuyPrice()
										+ Config.getNotEnoughPost());
							}
						} else {
							player.sendMessage(Config.getCommandAlready());
						}

					} else {

						Quantity qty = new Quantity(player.getName(), item, this);
						qty.loadInventory();
						qty.open();

					}

				}
			}

		}

	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	public void onClose(InventoryCloseEvent e) {
		GUI.setOnClose(null);
		if (Config.getEscapeOnly() && !hasClicked) {
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
				@Override
				public void run() {
					menuInstance.open();
				}
			}, 1L);

		} else {
			hasClicked = false;
		}
		return;

	}

}
