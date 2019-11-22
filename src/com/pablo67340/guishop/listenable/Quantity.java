package com.pablo67340.guishop.listenable;

import java.util.*;

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
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.definition.Enchantments;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.XMaterial;

import lombok.Getter;

class Quantity {

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

	@Getter
	private Player player;

	Quantity(Item item, Shop shop, Player input) {
		this.item = item;
		this.currentShop = shop;
		this.player = input;
	}

	/**
	 * Opens the GUI to sell the items in.
	 */
	void open() {
		GUI.setOnClose(this::onClose);
		GUI.setOnTopClick(this::onQuantityClick);
		GUI.setOnBottomClick(event -> {
			event.setCancelled(true);
		});
		GUI.show(player);
	}

	/**
	 * Preloads the inventory to display items.
	 */
	public Quantity loadInventory() {
		GUI = new Gui(Main.getINSTANCE(), 6, Config.getQtyTitle());
		int multiplier = 1;
		ShopPane page = new ShopPane(9, 6);
		for (int x = 19; x <= 25; x++) {
			ItemStack itemStack = XMaterial.matchXMaterial(item.getMaterial()).parseItem();
			itemStack.setAmount(multiplier);
			GuiItem gItem = new GuiItem(itemStack);
			ItemMeta itemMeta = gItem.getItem().getItemMeta();
			List<String> lore = new ArrayList<>();

			if (item.hasBuyPrice()) {
				if (item.getBuyPrice() instanceof Double) {
					if ((Double) item.getBuyPrice() != 0) {

						lore.add(Config.getBuyLore().replace("{amount}", Config.getCurrency()
								+ ((double) item.getBuyPrice() * multiplier) + Config.getCurrencySuffix()));
					} else if ((double) item.getBuyPrice() == 0) {
						lore.add(Config.getFreeLore());
					}
				} else {
					if ((Integer) item.getBuyPrice() != 0) {

						lore.add(Config.getBuyLore().replace("{amount}",
								Config.getCurrency() + (((Integer) item.getBuyPrice()).doubleValue() * multiplier)
										+ Config.getCurrencySuffix()));
					} else if ((double) item.getBuyPrice() == 0) {
						lore.add(Config.getFreeLore());
					}
				}
			} else {
				lore.add(Config.getCannotBuy());
			}

			item.getShopLore().forEach(str -> {
				lore.add(ChatColor.translateAlternateColorCodes('&', str));
			});

			assert itemMeta != null;
			itemMeta.setLore(lore);

			String type = itemStack.getType().toString();

			boolean isInList = Config.getDisabledQty().stream().anyMatch(t -> spellCheck(type, t));

			if (isInList && x >= 20) {
				break;
			}

			if (item.hasShopName()) {
				itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getShopName()));
			} else if (itemStack.getType() == XMaterial.SPAWNER.parseMaterial()) {
				String mobName = item.getMobType();
				mobName = mobName.toLowerCase();
				mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
				itemMeta.setDisplayName(mobName + " Spawner");
			}

			gItem.getItem().setItemMeta(itemMeta);

			if (item.hasEnchantments()) {
				if (item.getEnchantments().length > 1) {
					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						gItem.getItem().addUnsafeEnchantment(Enchantments.getByName(enchantment),
								Integer.parseInt(level));
					}
				}
			}

			page.setItem(gItem, x);
			qty.put(x, multiplier);
			multiplier *= 2;
		}

		if (!Config.isEscapeOnly()) {

			ItemStack backButtonItem = XMaterial.matchXMaterial(Config.getBackButtonItem()).parseItem();

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			assert backButtonMeta != null;
			backButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
					Objects.requireNonNull(Main.INSTANCE.getConfig().getString("back"))));

			backButtonItem.setItemMeta(backButtonMeta);

			GuiItem gItem = new GuiItem(backButtonItem, this::onQuantityClick);
			page.setItem(gItem, 53);

		}
		GUI.addPane(page);

		return this;
	}

	private Boolean spellCheck(String type, String t) {
		return type.contains(t);
	}

	/**
	 * Executes when an item is clicked inside the Quantity Inventory.
	 */
	private void onQuantityClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		e.setCancelled(true);
		if (!Config.isEscapeOnly()) {
			if (e.getSlot() == 53) {
				currentShop.open(player);
				return;
			}
		}

		if (e.getClickedInventory() == null) {
			return;
		}

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
			return;
		}

		if (player.getInventory().firstEmpty() == -1) {
			player.sendMessage(Config.getFull());
			return;
		}

		if (!item.hasBuyPrice()) {
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

		ItemStack itemStack = new ItemStack(e.getCurrentItem().getType());
		ItemMeta itemMeta = itemStack.getItemMeta();
		// If the item is not a mob spawner
		if (!item.isMobSpawner()) {
			// If the item has enchantments
			if (item.hasEnchantments()) {
				if (itemStack.getType() == Material.ENCHANTED_BOOK) {
					EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						assert meta != null;
						meta.addStoredEnchant(Enchantments.getByName(enchantment), Integer.parseInt(level), true);

					}
				} else {
					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						itemStack.addUnsafeEnchantment(Enchantments.getByName(enchantment), Integer.parseInt(level));
					}

				}
			}
			itemStack.setAmount(e.getCurrentItem().getAmount());

		}

		List<String> lore = new ArrayList<>();

		if (item.hasBuyLore()) {
			item.getBuyLore().forEach(str -> {
				lore.add(ChatColor.translateAlternateColorCodes('&', Main.placeholderIfy(str, player, item)));
			});
		}

		itemMeta.setLore(lore);

		if (item.hasBuyName()) {
			assert itemMeta != null;
			itemMeta.setDisplayName(
					ChatColor.translateAlternateColorCodes('&', Main.placeholderIfy(item.getBuyName(), player, item)));
		} else if (itemStack.getType() == XMaterial.SPAWNER.parseMaterial()) {
			String mobName = item.getMobType();
			mobName = mobName.toLowerCase();
			mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
			assert itemMeta != null;
			itemMeta.setDisplayName(mobName + " Spawner");
		}

		itemStack.setItemMeta(itemMeta);

		double priceToPay;

		/*
		 * If the map is empty, then the items purchased don't overflow the player's
		 * inventory. Otherwise, we need to reimburse the player (subtract it from
		 * priceToPay).
		 */

		double priceToReimburse = 0D;

		// if the item is not a shift click

		if (item.getBuyPrice() instanceof Double) {

			priceToPay = ((Double) item.getBuyPrice() * e.getCurrentItem().getAmount()) - priceToReimburse;
		} else {
			priceToPay = (((Integer) item.getBuyPrice()).doubleValue() * e.getCurrentItem().getAmount())
					- priceToReimburse;
		}

		// Check if the transition was successful

		if (Main.getECONOMY().withdrawPlayer(player, priceToPay).transactionSuccess()) {
			// If the player has the sound enabled, play
			// it!
			if (Config.isSoundEnabled()) {
				try {
					player.playSound(player.getLocation(), Sound.valueOf(Config.getSound()), 1, 1);

				} catch (Exception ex) {
					Main.getINSTANCE().getLogger().warning(
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

			player.getInventory().addItem(itemStack);

		} else {
			player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre() + priceToPay + Config.getNotEnoughPost());
		}

	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	private void onClose(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		if (Config.isEscapeOnly()) {
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> currentShop.open(player), 1L);

		}

	}

}
