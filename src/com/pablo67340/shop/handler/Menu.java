package com.pablo67340.shop.handler;

import java.util.*;

import org.bukkit.*;
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

import com.pablo67340.shop.main.Main;

public final class Menu implements Listener {

	/**
	 * The GUI that is projected onto the screen when a {@link Player} opens the
	 * {@link Menu}.
	 */
	private Inventory GUI;

	/**
	 * True/False if coming from Quantity, void one listener.
	 */
	private Boolean dupePatch = false;

	/**
	 * The {@link Player} that this {@link Menu} is created for.
	 */
	private final Player player;

	/**
	 * The {@link Shop} the player is currently utilizing.
	 */
	private Integer currentShop;

	/**
	 * The loaded shops read from the config.
	 */
	private Map<Integer, Shop> shops = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Shop}s when the server first starts.
	 * 
	 * @key The index on the {@link Menu} that this shop is located at.
	 * @value The shop.
	 */

	public Menu(String player) {
		this.player = Bukkit.getPlayer(player);
		preLoad();
	}

	/**
	 * Preloads the configs into their corresponding objects.
	 */
	@SuppressWarnings("deprecation")
	public void preLoad() {

		GUI = Bukkit.getServer().createInventory(null, 9 * Utils.getMenuRows(), "Menu");
		/**
		 * Loads all global shops.
		 * 
		 */
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

			Shop shop2 = new Shop(shop, name, description, lore);
			shop2.loadShop2();
			shops.put(i, shop2);

			if (player.hasPermission("guishop.slot." + (i + 1)) || player.isOp()
					|| player.hasPermission("guishop.slot.*")) {
				String itemID = Main.INSTANCE.getMainConfig().getString(String.valueOf(i + 1) + ".Item");
				if (itemID.contains(":")) {
					String[] ids = itemID.split(":");
					GUI.setItem(i, setName(
							new ItemStack(Material.getMaterial(Integer.parseInt(ids[0])), 1, Short.parseShort(ids[1])),
							name, lore));
					continue;
				} else {
					GUI.setItem(i,
							setName(new ItemStack(Material.getMaterial(Integer.parseInt(itemID)), 1), name, lore));
					continue;
				}
			}

			GUI.setItem(i, setName(new ItemStack(Material.getMaterial(36), 1), name, lore));

		}
		Bukkit.getServer().getPluginManager().registerEvents(this, Main.getInstance());
	}

	/**
	 * Opens the GUI in this {@link Menu}.
	 */
	public void open() {
		if (!player.hasPermission("guishop.use") && !player.isOp()) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Main.INSTANCE.getMainConfig().getString("no-permission")));
			return;
		}

		if (Main.INSTANCE.getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Main.INSTANCE.getMainConfig().getString("disabled-world")));
			return;
		}

		Main.HAS_MENU_OPEN.add(player.getName());

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
			@Override
			public void run() {
				player.openInventory(GUI);
			}
		}, 1L);

	}

	/**
	 * Sets the item's display name.
	 */
	private static ItemStack setName(ItemStack is, String name, List<String> lore) {
		ItemMeta IM = is.getItemMeta();

		if (name != null) {
			IM.setDisplayName(name);
		}

		if (lore != null && !lore.isEmpty()) {
			IM.setLore(lore);
		}

		is.setItemMeta(IM);

		return is;
	}

	/**
	 * Handle global inventory click events, check if inventory is for GUIShop, if
	 * so, run logic.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (e.getClickedInventory() != null) {
				Player player = (Player) e.getWhoClicked();

				/*
				 * If the player has the menu open.
				 */
				if (Main.HAS_MENU_OPEN.contains(player.getName())) {
					e.setCancelled(true);
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

					dupePatch = true;
					unregisterClass(player.getName());
					Bukkit.getServer().getPluginManager().registerEvents(shops.get(e.getSlot()), Main.getInstance());
					shops.get(e.getSlot()).open(player, e.getSlot());

					currentShop = e.getSlot();
					return;
				}
			}
		}
	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onClose(InventoryCloseEvent e) {
		String playerName = e.getPlayer().getName();
		if (Main.HAS_SHOP_OPEN.contains(playerName) && !Main.HAS_QTY_OPEN.contains(playerName)) {
			if (Utils.getEscapeOnly()) {
				HandlerList.unregisterAll(shops.get(currentShop));
				Main.HAS_SHOP_OPEN.remove(playerName);
				open();
			}
			return;
		} else if (Main.HAS_MENU_OPEN.contains(playerName)) {
			if (!dupePatch) {
				unregisterClass(playerName);
			} else {
				dupePatch = false;
			}
			return;
		}
	}

	/**
	 * Stop listening and garbage this class.
	 */
	public void unregisterClass(String playerName) {
		HandlerList.unregisterAll(this);
		Main.HAS_MENU_OPEN.remove(playerName);
	}

}
