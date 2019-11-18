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
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagByte;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagDouble;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagInt;
import com.pablo67340.guishop.definition.Enchantments;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.Price;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.XMaterial;

import lombok.Getter;

public class Shop {

	/**
	 * The name of this {@link Shop}.
	 */
	@Getter
	private final String name;

	/**
	 * The shop name of this {@link Shop}.
	 */
	@Getter
	private final String shop;

	/**
	 * The description of this {@link Shop}.
	 */
	@Getter
	private final String description;

	/**
	 * The lore of this {@link Shop}.
	 */
	@Getter
	private final List<String> lore;

	/**
	 * The list of {@link Item}s in this {@link Shop}.
	 */
	@Getter
	private List<Item> items;

	/**
	 * The list of {@link Page}'s in this {@link Shop}.
	 */
	private Gui GUI;

	private Menu menuInstance;

	private Boolean hasClicked = false;

	private int pageC = 0;

	private PaginatedPane currentPane;

	int oldPage = 0;

	/**
	 * The constructor for a {@link Shop}.
	 *
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	Shop(String shop, String name, String description, List<String> lore, Menu menuInstance) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
	}

	/**
	 * The constructor for a {@link Shop}.
	 *
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = null;
	}

	/**
	 * The constructor for a {@link Shop}.
	 *
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	Shop(String shop, String name, String description, List<String> lore, Menu menuInstance, List<Item> items) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
		this.items = items;
	}

	/**
	 * Load the specified shop
	 */
	public void loadItems() {

		if (items == null) {

			items = new ArrayList<>();

			Integer index = 0;

			Item item;

			ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);

			assert config != null;
			for (String str : config.getKeys(false)) {

				item = new Item();
				index += 1;

				ConfigurationSection section = config.getConfigurationSection(str);

				item.setSlot((Integer.parseInt(str) - 1));

				item.setMaterial((section.contains("id") ? (String) section.get("id") : "AIR"));
				item.setMobType((section.contains("mobType") ? (String) section.get("mobType") : "PIG"));
				item.setShopName((section.contains("shop-name") ? (String) section.get("shop-name") : " "));
				item.setBuyName((section.contains("buy-name") ? (String) section.get("buy-name") : " "));
				item.setEnchantments(
						(section.contains("enchantments") ? (String) section.get("enchantments") : "").split(" "));

				item.setBuyPrice((section.contains("buy-price") ? section.get("buy-price") : false));

				item.setSellPrice((section.contains("sell-price") ? section.get("sell-price") : false));

				item.setItemType(
						section.contains("type") ? ItemType.valueOf((String) section.get("type")) : ItemType.SHOP);

				item.setShopLore(
						(section.contains("shop-lore") ? section.getStringList("shop-lore") : new ArrayList<>()));
				item.setBuyLore((section.contains("buy-lore") ? section.getStringList("buy-lore") : new ArrayList<>()));
				item.setCommands(
						(section.contains("commands") ? section.getStringList("commands") : new ArrayList<>()));

				if (!Main.getINSTANCE().getPRICETABLE().containsKey(item.getMaterial())
						&& (!(item.getSellPrice() instanceof Boolean))) {
					Double sellPrice = item.getSellPrice() instanceof Integer
							? ((Integer) item.getSellPrice()).doubleValue()
							: ((Double) item.getSellPrice());

					if (item.isMobSpawner()) {
						Main.getINSTANCE().getPRICETABLE()
								.put(item.getMaterial() + ":" + item.getMobType().toLowerCase(), new Price(sellPrice));
					} else {
						Main.getINSTANCE().getPRICETABLE().put(item.getMaterial(), new Price(sellPrice));
					}
				}
				System.out.println("Added: " + item.getMaterial());
				items.add(item);
			}
			loadShop();
		} else {
			loadShop();
		}

	}

	private void loadShop() {
		Integer index = 0, lastIndex = 0;
		ShopPane page = new ShopPane(9, 6);

		this.GUI = new Gui(Main.getINSTANCE(), 6,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());
		PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);

		for (Item item : items) {

			ItemStack itemStack;
			GuiItem gItem = null;
			if (item.getItemType() == ItemType.SHOP) {
				itemStack = XMaterial.matchXMaterial(item.getMaterial()).parseItem();

				assert itemStack != null;

				ItemMeta itemMeta = itemStack.getItemMeta();

				List<String> lore = new ArrayList<>();

				if (item.canBuyItem()) {
					if (item.getBuyPrice() instanceof Double) {
						if ((Double) item.getBuyPrice() != 0.0) {
							lore.add(Config.getBuyLore().replace("{amount}",
									Config.getCurrency() + item.getBuyPrice() + Config.getCurrencySuffix()));
						} else {
							lore.add(Config.getFreeLore());
						}
					} else {
						if ((Integer) item.getBuyPrice() != 0.0) {
							lore.add(Config.getBuyLore().replace("{amount}", Config.getCurrency()
									+ ((Integer) item.getBuyPrice()).doubleValue() + Config.getCurrencySuffix()));
						} else {
							lore.add(Config.getFreeLore());
						}

					}
				} else {
					lore.add(Config.getCannotBuy());
				}

				if (item.canSellItem()) {
					lore.add(Config.getSellLore().replace("{amount}",
							Config.getCurrency() + item.getSellPrice() + Config.getCurrencySuffix()));
				} else {
					lore.add(Config.getCannotSell());
				}

				item.getShopLore().forEach(str -> {
					lore.add(ChatColor.translateAlternateColorCodes('&', str));
				});

				if (!lore.isEmpty()) {
					assert itemMeta != null;
					itemMeta.setLore(lore);
				}

				if (item.hasShopName()) {
					assert itemMeta != null;
					itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getShopName()));
				} else if (item.isMobSpawner()) {
					String mobName = item.getMobType();
					mobName = mobName.toLowerCase();
					mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
					assert itemMeta != null;
					itemMeta.setDisplayName(mobName + " Spawner");
				}

				itemStack.setItemMeta(itemMeta);

				gItem = new GuiItem(itemStack);

				if (item.getEnchantments().length > 1) {
					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						gItem.getItem().addUnsafeEnchantment(Enchantments.getByName(enchantment),
								Integer.parseInt(level));
					}
				}
			}

			// Create Page
			if (index == items.size() || ((index) - lastIndex) == 44) {
				if (item.getItemType() == ItemType.SHOP) {
					page.setItem(gItem, item.getSlot());
				}

				if (items.size() > 45) {
					applyButtons(page);
				}
				lastIndex = index;
				System.out.println("Adding page to: " + pageC);
				pane.addPane(pageC, page);
				pageC += 1;
				page = new ShopPane(9, 6);
			} else {
				if (pageC == 0) {
					if (item.getItemType() == ItemType.SHOP) {
						page.setItem(gItem, item.getSlot() - lastIndex);
					}
				} else {

					if (item.getItemType() == ItemType.SHOP) {
						page.setItem(gItem, item.getSlot() - lastIndex - 1);
					}
				}
			}

			if (index + 1 == items.size()) {
				System.out.println("Adding page to: " + pageC);
				pane.addPane(pageC, page);
				applyButtons(page);
				GUI.addPane(pane);
				Main.getINSTANCE().getLoadedShops().put(name, items);
			}
			index += 1;

		}

		this.currentPane = pane;

	}

	private void applyButtons(ShopPane page) {
		System.out.println("Applying Buttons");
		if (page.getItems().size() == 45) {
			page.setItem(new GuiItem(new ItemStack(Material.ARROW)), 51);
		}
		if (pageC > 0) {
			page.setItem(new GuiItem(new ItemStack(Material.ARROW)), 47);
		}
		if (!Config.isEscapeOnly()) {

			ItemStack backButtonItem = new ItemStack(
					Objects.requireNonNull(XMaterial.matchXMaterial(Config.getBackButtonItem()).parseMaterial()));

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			assert backButtonMeta != null;
			backButtonMeta.setDisplayName(Config.getBackButtonText());

			backButtonItem.setItemMeta(backButtonMeta);

			GuiItem item = new GuiItem(backButtonItem);

			page.setItem(item, 53);
		}
	}

	/**
	 * Open the player's shop
	 */
	public void open(Player input) {
		GUI.show(input);
		if (!Main.getCREATOR().containsKey(input.getName())) {
			GUI.setOnBottomClick(event -> {
				event.setCancelled(true);
			});
		}
		GUI.setOnClose(this::onClose);
		GUI.setOnTopClick(this::onShopClick);

	}

	private void onShopClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		if (!Main.getCREATOR().containsKey(player.getName())) {
			e.setCancelled(true);
		}

		/*
		 * If the player's inventory is full
		 */
		if (player.getInventory().firstEmpty() == -1) {
			player.sendMessage(Config.getFull());
			return;
		}

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
			return;
		}

		if (e.getSlot() >= 0 && e.getSlot() < GUI.getItems().size()) {
			/*
			 * If the player clicks the 'back' button, then open the menu. Otherwise, If the
			 * user clicks the forward button, load and open next page, Otherwise, If the
			 * user clicks the backward button, load and open the previous page, Otherwise
			 * Attempt to purchase the clicked item.
			 */

			// Forward Button
			if (e.getSlot() == 51) {
				hasClicked = true;

				ItemStack[] items = GUI.getInventory().getContents();

				int slot = 0;
				for (ItemStack item : items) {
					((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setItem(new GuiItem(item),
							slot);
					slot += 1;
					System.out.println("Slot: " + slot);
				}
				((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
				System.out.println("Switched to page: " + (currentPane.getPage() + 1));
				currentPane.setPage(currentPane.getPage() + 1);

				((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
				GUI.update();
				return;
				// Backward Button
			} else if (e.getSlot() == 47) {
				hasClicked = true;

				ItemStack[] items = GUI.getInventory().getContents();

				int slot = 0;
				for (ItemStack item : items) {
					((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setItem(new GuiItem(item),
							slot);
					slot += 1;
				}
				((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
				System.out.println("Switched to page: " + (currentPane.getPage() - 1));
				currentPane.setPage(currentPane.getPage() - 1);

				((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
				GUI.update();
				return;
				// Back Button
			} else if (e.getSlot() == 53 && !Config.isEscapeOnly()) {
				if (menuInstance != null && !Main.getCREATOR().containsKey(player.getName())) {
					menuInstance.open(player);
				}
				return;
			}

			/*
			 * If the player has enough money to purchase the item, then allow them to.
			 */
			if (!Main.getCREATOR().containsKey(player.getName())) {
				Item item = getItems().get((currentPane.getPage() * 45) + e.getSlot());

				if (item.getItemType() == ItemType.SHOP && item.canBuyItem()) {
					new Quantity(item, this, player).loadInventory().open();
				} else if (item.getItemType() == ItemType.COMMAND) {
					if (Main.getECONOMY().withdrawPlayer(player, (Double) item.getBuyPrice()).transactionSuccess()) {
						item.getCommands().forEach(str -> {
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
									Main.placeholderIfy(str, player, item));
						});
					} else {
						player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre() + item.getBuyPrice()
								+ Config.getNotEnoughPost());
					}
				} else {
					player.sendMessage(Config.getPrefix() + " " + Config.getCannotBuy());
				}
			}
		}
	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	private void onClose(InventoryCloseEvent e) {
		System.out.println("Close");
		Player player = (Player) e.getPlayer();
		if (!Main.CREATOR.containsKey(player.getName())) {
			if (Config.isEscapeOnly() && !hasClicked) {
				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> menuInstance.open(player), 1L);

			} else {
				hasClicked = false;
			}
		} else {
			if (!hasClicked) {
				System.out.println("Saving Edits");
				saveShop(e);
			} else {
				System.out.println("was clicked");
				hasClicked = false;
			}
		}

	}

	private void saveShop(InventoryCloseEvent e) {
		Main.getINSTANCE().getCustomConfig().set(shop, null);
		Main.getINSTANCE().getCustomConfig().createSection(shop);
		ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);
		int slots = GUI.getInventory().getSize();
		for (Integer slot = 0; slot <= (slots - 1); slot++) {
			ItemStack itemStack = GUI.getInventory().getItem(slot);
			Item item = new Item();
			ConfigurationSection section = config.createSection(slot.toString());
			if (itemStack != null) {
				ItemMeta im = itemStack.getItemMeta();

				item.setItemType(ItemType.SHOP);
				item.setMaterial(itemStack.getType().toString());

				Object buyPrice = getBuyPrice(itemStack);

				if (buyPrice instanceof Boolean) {
					item.setBuyPrice((Boolean) buyPrice);
				} else {
					item.setBuyPrice((Double) buyPrice);
				}

				Object sellPrice = getSellPrice(itemStack);

				if (sellPrice instanceof Boolean) {
					item.setSellPrice((Boolean) sellPrice);
				} else {
					item.setSellPrice((Double) sellPrice);
				}

				if (im.hasDisplayName()) {
					item.setShopName(im.getDisplayName());
				}

				if (im.hasLore()) {
					item.setShopLore(im.getLore());
				}

				System.out.println("Saving: " + itemStack.getType());
				items.set(slot, item);
			} else {
				Item blank = new Item();
				blank.setItemType(ItemType.BLANK);
				items.set(slot, blank);
			}
		}
		saveItems();
	}

	/**
	 * Load the specified shop
	 */
	public void saveItems() {

		Integer index = 0;

		Main.getINSTANCE().getCustomConfig().set(shop, null);

		ConfigurationSection config = Main.getINSTANCE().getCustomConfig().createSection(shop);

		assert config != null;
		for (Item item : items) {
			ConfigurationSection section = config.createSection(index.toString());
			section.set("type", item.getItemType().toString());
			section.set("id", item.getMaterial());
			section.set("buy-price", item.getBuyPrice());
			section.set("sell-price", item.getSellPrice());
			section.set("shop-lore", item.getShopLore());
			section.set("shop-name", item.getShopName());

			index += 1;
		}
		try {
			Main.getINSTANCE().getCustomConfig().save(Main.getINSTANCE().getSpecialf());
		} catch (Exception ex) {
			System.out.println("Error Saving: " + ex.getMessage());
		}

	}

	private Object getBuyPrice(ItemStack item) {
		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		if (comp.hasKey("buyPrice")) {
			Double vl = comp.getDouble("buyPrice");
			return vl;
		} else {
			return false;
		}

	}

	private Object getSellPrice(ItemStack item) {
		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		if (comp.hasKey("sellPrice")) {
			Double vl = comp.getDouble("sellPrice");
			return vl;
		} else {
			return false;
		}
	}

}
