package com.pablo67340.guishop.listenable;

import java.util.*;

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
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.pablo67340.guishop.definition.Enchantments;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.handler.Item;
import com.pablo67340.guishop.handler.Page;
import com.pablo67340.guishop.main.Main;
import com.pablo67340.guishop.util.Config;

import com.pablo67340.guishop.util.XMaterial;

public class Shop {

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

	/**
	 * The list of {@link Page}'s in this {@link Shop}.
	 */
	private Gui GUI;

	private Menu menuInstance;

	private Boolean hasClicked = false;

	private Integer menuSlot;

	private int pageC = 0;

	/**
	 * The constructor for a {@link Shop}.
	 * 
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore, Integer slot, Menu menuInstance) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
		this.menuSlot = slot;
	}

	/**
	 * The constructor for a {@link Shop}.
	 * 
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore, Integer slot, Menu menuInstance,
			List<Item> items) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
		this.menuSlot = slot;
		this.ITEMS = items;
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
	 * Load the specified shop
	 * 
	 */
	@SuppressWarnings({ "unchecked" })
	public void loadItems(Player player) {

		if (ITEMS == null) {

			ITEMS = new ArrayList<>();

			Integer index = 0;

			Item item = new Item();

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
						} else if (map.containsKey("name")) {
							item.setName((String) map.get("name"));
						} else if (map.containsKey("enchantments")) {
							String preEnc = (String) map.get("enchantments");
							if (!preEnc.equalsIgnoreCase("")) {
								String[] enchants = preEnc.split(" ");
								item.setEnchantments(enchants);
							}
						} else if (map.containsKey("buy-price")) {
							item.setBuyPrice(map.get("buy-price"));
						} else if (map.containsKey("sell-price")) {
							item.setSellPrice(map.get("sell-price"));
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

				ITEMS.add(item);
			}
			loadShop(player);
		} else {
			loadShop(player);
		}

	}

	public void loadShop(Player player) {
		Integer index = 0, lastIndex = 0;
		OutlinePane page = new OutlinePane(0, 0, 9, 6);
		this.GUI = new Gui(Main.getInstance(), 6,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());
		PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
		for (Item item : ITEMS) {

			ItemStack itemStack = XMaterial.valueOf(item.getMaterial()).parseItem();

			GuiItem gItem = new GuiItem(itemStack, event -> onShopClick(event, pane));

			ItemMeta itemMeta = gItem.getItem().getItemMeta();

			List<String> lore = new ArrayList<>();

			if (item.canBuyItem()) {
				if (item.getBuyPrice() != 0) {
					lore.add(Config.getBuyLore().replace("{amount}",
							Config.getCurrency() + item.getBuyPrice() + Config.getCurrencySuffix()));
				} else {
					lore.add(Config.geFreeLore());
				}
			} else {
				lore.add(Config.getCannotBuyLore());
			}

			if (item.canSellItem()) {
				lore.add(Config.getSellLore().replace("{amount}",
						Config.getCurrency() + item.getSellPrice() + Config.getCurrencySuffix()));
			} else {
				lore.add(Config.getCannotSellLore());
			}

			itemMeta.setLore(lore);

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

			if (item.getName() != null) {
				itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));
			} else if (item.isMobSpawner()) {
				String mobName = item.getMobType();
				mobName = mobName.toLowerCase();
				mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
				itemMeta.setDisplayName(mobName + " Spawner");
			}

			if (item.getEnchantments() != null) {

				for (String enc : item.getEnchantments()) {
					String enchantment = StringUtils.substringBefore(enc, ":");
					String level = StringUtils.substringAfter(enc, ":");
					itemStack.addUnsafeEnchantment(Enchantments.getByName(enchantment), Integer.parseInt(level));

				}
			}

			gItem.getItem().setItemMeta(itemMeta);
			// Create Page
			if (index == ITEMS.size() || ((index) - lastIndex) == 44) {
				page.addItem(gItem);
				if (ITEMS.size() > 45) {
					applyButtons(pane, page, player);
				}
				lastIndex = index;
				pane.addPane(pageC, page);
				pageC += 1;
				page = new OutlinePane(0, 0, 9, 6);
			} else {
				page.addItem(gItem);
			}

			if (index + 1 == ITEMS.size()) {
				pane.addPane(pageC, page);
				applyButtons(pane, page, player);
				GUI.addPane(pane);
				Main.getInstance().getLoadedShops().put(menuSlot, ITEMS);
			}
			index += 1;
		}

	}

	public void applyButtons(PaginatedPane pane, OutlinePane page, Player player) {
		if (page.getItems().size() == 45) {
			for (int x = page.getItems().size(); x <= 54; x++) {
				page.addItem(new GuiItem(new ItemStack(Material.AIR)));
			}
			page.insertItem(new GuiItem(new ItemStack(Material.ARROW), event -> {
				hasClicked = true;

				Pane[] arr = new Pane[54];
				arr = pane.getPanes().toArray(arr);
				arr[pane.getPage()].setVisible(false);
				pane.setPage(pane.getPage() + 1);

				arr[pane.getPage()].setVisible(true);

				GUI.update();
			}), 51);
		}
		if (pageC > 0) {

			for (int x = page.getItems().size(); x <= 54; x++) {
				page.addItem(new GuiItem(new ItemStack(Material.AIR)));
			}
			page.insertItem(new GuiItem(new ItemStack(Material.ARROW), event -> {
				hasClicked = true;
				Pane[] arr = new Pane[54];
				arr = pane.getPanes().toArray(arr);
				arr[pane.getPage()].setVisible(false);
				pane.setPage(pane.getPage() - 1);

				arr[pane.getPage()].setVisible(true);
				GUI.update();
			}), 47);
		}
		if (!Config.getEscapeOnly()) {

			ItemStack backButtonItem = new ItemStack(XMaterial.valueOf(Config.getBackButtonItem()).parseMaterial());

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			backButtonMeta.setDisplayName(Config.getBackButtonText());

			backButtonItem.setItemMeta(backButtonMeta);

			GuiItem item = new GuiItem(backButtonItem, event -> {
				menuInstance.open(player);
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
		GUI.show(input);
		GUI.setOnClose(event -> onClose(event));
	}

	public void onShopClick(InventoryClickEvent e, PaginatedPane pane) {
		e.setCancelled(true);
		Player player = (Player) e.getWhoClicked();
		hasClicked = true;

		/*
		 * If the player's inventory is full
		 */
		if (player.getInventory().firstEmpty() == -1) {
			player.sendMessage(Config.getFull());
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

			Item item = getItems().get((pane.getPage() * 45) + e.getSlot());
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
				Quantity qty = new Quantity(item, this);
				qty.loadInventory();
				qty.open(player);
			}
		}
	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	public void onClose(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		if (Config.getEscapeOnly() && !hasClicked) {
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
				@Override
				public void run() {
					menuInstance.open(player);
				}
			}, 1L);

		} else {
			hasClicked = false;
		}
		return;

	}

}
