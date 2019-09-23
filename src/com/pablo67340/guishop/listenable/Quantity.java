package com.pablo67340.guishop.listenable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;

import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;

import com.pablo67340.guishop.definition.Enchantments;

import com.pablo67340.guishop.handler.Item;
import com.pablo67340.guishop.main.Main;
import com.pablo67340.guishop.util.Config;

import com.pablo67340.guishop.util.XMaterial;

import me.ialistannen.mininbt.ItemNBTUtil;
import me.ialistannen.mininbt.NBTWrappers.NBTTagCompound;

public class Quantity {

	/**
	 * The item currently being targetted.
	 */
	private Item item;

	/**
	 * The GUI that will be displayed.
	 */
	private Gui GUI;

	/**
	 * The map containing the sell increments.
	 */
	private Map<Integer, Integer> qty = new HashMap<>();

	/**
	 * The instance of the {@link Shop} that spawned this Quantity.
	 */
	private Shop currentShop;

	public Quantity(Item item, Shop shop) {
		this.item = item;
		this.currentShop = shop;
	}

	/**
	 * Opens the GUI to sell the items in.
	 */
	public void open(Player player) {
		GUI.setOnClose(event -> onClose(event));
		GUI.show(player);
	}

	/**
	 * Preloads the inventory to display items.
	 */
	public void loadInventory() {
		GUI = new Gui(Main.getInstance(), 6, Config.getQtyTitle());
		Integer multiplier = 1;
		OutlinePane page = new OutlinePane(0, 0, 9, 6);
		for (int x = 19; x <= 25; x++) {
			ItemStack itemStack = XMaterial.valueOf(item.getMaterial()).parseItem();
			itemStack.setAmount(multiplier);
			GuiItem gItem = new GuiItem(itemStack, event -> onQuantityClick(event));
			ItemMeta itemMeta = gItem.getItem().getItemMeta();
			List<String> lore = new ArrayList<>();

			if (item.canBuyItem()) {
				if (item.getBuyPrice() != 0) {

					lore.add(Config.getBuyLore().replace("{amount}",
							Config.getCurrency() + (item.getBuyPrice() * multiplier) + Config.getCurrencySuffix()));
				} else if (item.getBuyPrice() == 0) {
					lore.add(Config.geFreeLore());
				}
			} else {
				lore.add(Config.getCannotBuyLore());
			}

			if (item.getSellPrice() != 0) {
				lore.add(Config.getSellLore().replace("{amount}",
						Config.getCurrency() + (item.getSellPrice() * multiplier) + Config.getCurrencySuffix()));
			} else if (item.getSellPrice() == 0) {
				lore.add(Config.getCannotSellLore());
			}

			itemMeta.setLore(lore);

			String type = itemStack.getType().toString();
			if ((type.contains("CHESTPLATE") || type.contains("LEGGINGS") || type.contains("BOOTS")
					|| type.contains("HELMET")) && x >= 20) {
				break;
			}

			if (item.getName() != null) {
				itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));
			} else if (itemStack.getType() == XMaterial.SPAWNER.parseMaterial()) {
				String mobName = item.getMobType();
				mobName = mobName.toLowerCase();
				mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
				itemMeta.setDisplayName(mobName + " Spawner");
			}

			gItem.getItem().setItemMeta(itemMeta);

			for (int i = 0; i <= 54; i++) {
				page.addItem(new GuiItem(new ItemStack(Material.AIR)));
			}
			page.insertItem(gItem, x);
			qty.put(x, multiplier);
			multiplier *= 2;
		}

		if (!Config.getEscapeOnly()) {

			ItemStack backButtonItem = XMaterial.valueOf(Config.getBackButtonItem()).parseItem();

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			backButtonMeta.setDisplayName(
					ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getConfig().getString("back")));

			backButtonItem.setItemMeta(backButtonMeta);

			GuiItem gItem = new GuiItem(backButtonItem, event -> onQuantityClick(event));
			page.insertItem(gItem, 53);

		}
		GUI.addPane(page);

	}

	/**
	 * Executes when an item is clicked inside the Quantity Inventory.
	 */
	public void onQuantityClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		e.setCancelled(true);
		if (!Config.getEscapeOnly()) {
			if (e.getSlot() == 53) {
				currentShop.open(player);
				return;
			}
		}

		if (player.getInventory().firstEmpty() == -1) {
			player.sendMessage(Config.getFull());
			return;
		}

		if (!item.canBuyItem()) {
			player.sendMessage(Config.getCantBuy());
			return;
		}

		// Does the quantity work out?
		int quantity = qty.get(e.getSlot());

		// If the quantity is 0
		if (quantity == 0) {
			player.sendMessage(Config.getPrefix() + " " + Config.getNotEnoughPre() + item.getBuyPrice()
					+ Config.getNotEnoughPost());
			player.setItemOnCursor(new ItemStack(Material.AIR));
			return;
		}

		Map<Integer, ItemStack> returnedItems = new HashMap<>();

		ItemStack itemStack = null;

		// If the item is not a mob spawner
		if (!item.isMobSpawner()) {

			itemStack = XMaterial.valueOf(item.getMaterial()).parseItem();
			// If the item has enchantments
			if (item.getEnchantments() != null) {
				if (itemStack.getType() == Material.ENCHANTED_BOOK) {
					EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						meta.addStoredEnchant(Enchantments.getByName(enchantment), Integer.parseInt(level), true);

					}
					itemStack.setItemMeta(meta);
				} else {

					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						itemStack.addUnsafeEnchantment(Enchantments.getByName(enchantment), Integer.parseInt(level));

					}

				}
			}
			itemStack.setAmount(e.getCurrentItem().getAmount());
			// If is shift clicking, buy 1.

		}

		ItemMeta itemMeta = itemStack.getItemMeta();
		if (item.getName() != null) {
			itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));
		} else if (itemStack.getType() == XMaterial.SPAWNER.parseMaterial()) {
			String mobName = item.getMobType();
			mobName = mobName.toLowerCase();
			mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
			itemMeta.setDisplayName(mobName + " Spawner");
		}

		itemStack.setItemMeta(itemMeta);

		double priceToPay = 0;

		/*
		 * If the map is empty, then the items purchased don't overflow the player's
		 * inventory. Otherwise, we need to reimburse the player (subtract it from
		 * priceToPay).
		 */

		double priceToReimburse = 0D;

		// if the item is not a shift click

		while (returnedItems.values().iterator().hasNext()) {
			priceToReimburse += (item.getBuyPrice() / e.getCurrentItem().getAmount());
		}
		priceToPay = (item.getBuyPrice() * e.getCurrentItem().getAmount()) - priceToReimburse;

		// Check if the transition was successful

		if (Main.getEconomy().withdrawPlayer(player, priceToPay).transactionSuccess()) {
			// If the player has the sound enabled, play
			// it!
			if (Config.isSoundEnabled()) {
				try {
					player.playSound(player.getLocation(), Sound.valueOf(Config.getSound()), 1, 1);

				} catch (Exception ex) {
					Main.getInstance().getLogger().warning(
							"Incorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
				}
			}
			player.sendMessage(Config.getPrefix() + Config.getPurchased() + priceToPay + Config.getTaken()
					+ Config.getCurrencySuffix());

			if (item.isMobSpawner()) {
				NBTTagCompound tag = ItemNBTUtil.getTag(itemStack);
				tag.setString("GUIShopSpawner", item.getMobType());

				itemStack = ItemNBTUtil.setNBTTag(tag, itemStack);
			}

			returnedItems = player.getInventory().addItem(itemStack);

		} else {
			player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre() + priceToPay + Config.getNotEnoughPost());
		}

	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	public void onClose(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		if (Config.getEscapeOnly()) {
			GUI.setOnClose(null);
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
				@Override
				public void run() {
					currentShop.open(player);
				}
			}, 1L);

		}
		return;

	}

}
