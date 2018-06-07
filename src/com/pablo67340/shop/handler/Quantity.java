package com.pablo67340.shop.handler;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.shop.main.Main;
import com.songoda.epicspawners.api.EpicSpawners;
import com.songoda.epicspawners.api.EpicSpawnersAPI;

import de.dustplanet.util.SilkUtil;

public class Quantity implements Listener {

	/**
	 * The name of the player who is buying an item.
	 */
	private String playerName;

	/**
	 * The player who is buying an item.
	 */
	private Player player;

	/**
	 * The item currently being targetted.
	 */
	private Item item;

	/**
	 * The GUI that is projected onto the screen when a {@link Player} opens the
	 * {@link Menu}.
	 */
	private Inventory GUI;

	/**
	 * The map containing the sell increments.
	 */
	private Map<Integer, Integer> qty = new HashMap<>();

	/**
	 * The shop number the user came from.
	 */
	private Integer currentShop;

	public Quantity(String player, Item item, Integer currentShop) {
		this.item = item;
		this.playerName = player;
		this.player = Bukkit.getPlayer(player);
		this.currentShop = currentShop;
	}

	/**
	 * Opens the GUI to sell the items in.
	 */
	public void open() {
		GUI = Bukkit.getServer().createInventory(null, 9 * 6, Utils.getQtyTitle());
		packInventory();

		player.openInventory(GUI);

	}

	/**
	 * Preloads the inventory to display items.
	 */
	@SuppressWarnings("deprecation")
	private void packInventory() {
		Integer multiplier = 1;
		for (int x = 19; x <= 25; x++) {
			ItemStack itemStack = new ItemStack(item.getId(), multiplier, (short) item.getData());
			ItemMeta itemMeta = itemStack.getItemMeta();
			if (item.getBuyPrice() != 0 && item.getSellPrice() != 0) {

				itemMeta.setLore(Arrays.asList(
						ChatColor.translateAlternateColorCodes('&',
								"&fBuy: &c" + Utils.getCurrency() + item.getBuyPrice() * multiplier),
						ChatColor.translateAlternateColorCodes('&',
								"&fSell: &a" + Utils.getCurrency() + item.getSellPrice() * multiplier)));
			} else if (item.getBuyPrice() == 0) {
				itemMeta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&cCannot be purchased"),
						ChatColor.translateAlternateColorCodes('&',
								"&fSell: &a" + Utils.getCurrency() + item.getSellPrice() * multiplier)));
			} else {
				itemMeta.setLore(Arrays.asList(
						ChatColor.translateAlternateColorCodes('&',
								"&fBuy: &c" + Utils.getCurrency() + item.getBuyPrice() * multiplier),
						ChatColor.translateAlternateColorCodes('&', "&cCannot be sold")));
			}
			itemStack.setItemMeta(itemMeta);
			GUI.setItem(x, itemStack);
			qty.put(x, multiplier);
			multiplier *= 2;
		}

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

			GUI.setItem(GUI.getSize() - 1, backButtonItem);

		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onQuantityClick(InventoryClickEvent e) {
		if (e.getWhoClicked().getName().equalsIgnoreCase(this.playerName)) {
			if (Main.HAS_QTY_OPEN.contains(playerName)) {
				e.setCancelled(true);
				if (!Utils.getEscapeOnly()) {
					if (e.getSlot() == (GUI.getSize() - 1)) {
						Main.HAS_QTY_OPEN.remove(playerName);
						HandlerList.unregisterAll(this);
						reOpen();
						return;
					}
				}

				if (e.getClickedInventory() == null) {
					return;
				}

				if (player.getInventory().firstEmpty() == -1) {
					player.sendMessage(Utils.getFull());
					return;
				}
				/*
				 * If the player clicks on an empty slot, then cancel the event.
				 */
				if (e.getCurrentItem() != null) {
					if (e.getCurrentItem().getType() == Material.AIR) {
						return;
					}
				}

				/*
				 * If the player clicks in their own inventory, we want to cancel the event.
				 */
				if (e.getClickedInventory() == player.getInventory()) {
					return;
				}

				// Check if the item is disabled, or price is 0
				if (item.getBuyPrice() == 0) {
					player.sendMessage(Utils.getPrefix() + " " + Utils.getCantBuy());
					player.setItemOnCursor(new ItemStack(Material.AIR));
					return;
				}

				// Does the quantity work out?
				int quantity = qty.get(e.getSlot());

				// If the quantity is 0
				if (quantity == 0) {
					player.sendMessage(Utils.getPrefix() + " " + Utils.getNotEnoughPre() + item.getBuyPrice()
							+ Utils.getNotEnoughPost());
					player.setItemOnCursor(new ItemStack(Material.AIR));
					return;
				}

				Map<Integer, ItemStack> returnedItems = new HashMap<>();

				ItemStack itemStack = null;

				// If the item is not a mob spawner
				if (item.getId() != 52) {

					// if the item has a data
					if (item.getData() > 0) {
						itemStack = new ItemStack(item.getId(), quantity, (short) item.getData());
						if (item.getEnchantments() != null) {
							for (String enc : item.getEnchantments()) {
								String enchantment = StringUtils.substringBefore(enc, ":");
								String level = StringUtils.substringAfter(enc, ":");
								itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment),
										Integer.parseInt(level));
							}
						}

						itemStack.setAmount(quantity);
						// If is shift clicking, buy 1
						if (e.isShiftClick())
							itemStack.setAmount(1);

					} else {
						itemStack = new ItemStack(item.getId(), quantity);
						// If the item has enchantments
						if (item.getEnchantments() != null) {
							for (String enc : item.getEnchantments()) {
								String enchantment = StringUtils.substringBefore(enc, ":");
								String level = StringUtils.substringAfter(enc, ":");
								itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment),
										Integer.parseInt(level));
							}
						}
						itemStack.setAmount(e.getCurrentItem().getAmount());
						// If is shift clicking, buy 1.

					}
				} else {
					itemStack = new ItemStack(item.getId(), quantity);
					if (Main.getInstance().usesSpawners()) {
						if (Dependencies.hasDependency("SilkSpawners")) {
							SilkUtil su = (SilkUtil) Main.getInstance().getSpawnerObject();
							itemStack = su.setSpawnerType(itemStack, (short) item.getData(),
									Spawners.getMobName(item.getData()));
						} else if (Dependencies.hasDependency("EpicSpawners")) {
							EpicSpawners es = (EpicSpawners) Main.getInstance().getSpawnerObject();
							itemStack = es.newSpawnerItem(EpicSpawnersAPI.getSpawnerManager()
									.getSpawnerData(Spawners.getMobName(item.getData())), quantity);

						}

					} else {
						player.sendMessage("Spawners Disabled! Dependencies not installed!");
						return;
					}
				}

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
					if (Utils.isSoundEnabled()) {
						try {
							player.playSound(player.getLocation(), Sound.valueOf(Utils.getSound()), 1, 1);

						} catch (Exception ex) {
							Main.getInstance().getLogger().warning(
									"Incorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
						}
					}
					player.sendMessage(Utils.getPrefix() + Utils.getPurchased() + priceToPay + Utils.getTaken()
							+ Utils.getCurrencySuffix());
					returnedItems = player.getInventory().addItem(itemStack);
				} else {
					player.sendMessage(
							Utils.getPrefix() + Utils.getNotEnoughPre() + priceToPay + Utils.getNotEnoughPost());
				}

			}
		}
	}

	/**
	 * Preloads the shops before opening them.
	 */
	public void reOpen() {
		/**
		 * Loads all global shops.
		 * 
		 */

		if (!Main.INSTANCE.getMainConfig().getBoolean(currentShop + ".Enabled")) {
			return;
		}

		String shop = ChatColor.translateAlternateColorCodes('&',
				Main.INSTANCE.getMainConfig().getString(currentShop + ".Shop"));

		String name = ChatColor.translateAlternateColorCodes('&',
				Main.INSTANCE.getMainConfig().getString(currentShop + ".Name"));

		String description = ChatColor.translateAlternateColorCodes('&',
				Main.INSTANCE.getMainConfig().getString(currentShop + ".Desc"));

		List<String> lore = new ArrayList<>();

		if (description != null && description.length() > 0) {
			lore.add(description);
		}

		Shop shop2 = new Shop(shop, name, description, lore, currentShop, player);
		Bukkit.getServer().getPluginManager().registerEvents(shop2, Main.getInstance());
		shop2.loadShop();

	}

	/**
	 * The inventory close listener for the quantity inventory.
	 */
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (e.getPlayer().getName().equalsIgnoreCase(this.playerName)) {
			if (Main.HAS_QTY_OPEN.contains(playerName)) {
				if (Utils.getEscapeOnly()) {
					Main.HAS_QTY_OPEN.remove(playerName);
					HandlerList.unregisterAll(this);
					reOpen();
				} else {
					Main.HAS_QTY_OPEN.remove(playerName);
					HandlerList.unregisterAll(this);
				}
			}
		}

	}

}
