package com.pablo67340.guishop.listenable;

import java.util.*;

import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;

import com.pablo67340.guishop.handler.ShopDir;
import com.pablo67340.guishop.main.Main;

import com.pablo67340.guishop.util.XMaterial;

public final class Menu {

	/**
	 * The GUI that is projected onto the screen when a {@link Player} opens the
	 * {@link Menu}.
	 */
	private Gui GUI = new Gui(Main.getInstance(), 1, "Menu");

	/**
	 * The {@link Player} that this {@link Menu} is created for.
	 */
	private final Player player;

	/**
	 * The loaded shops read from the config.
	 */
	private Map<Integer, ShopDir> shops = new HashMap<>();

	/**
	 * The currently open shop associated with this Menu instance.
	 */
	private Shop openShop;

	/**
	 * The instance of this Menu.
	 */
	private Menu instance;

	/**
	 * A {@link Map} that will store our {@link Shop}s when the server first starts.
	 * 
	 * @key The index on the {@link Menu} that this shop is located at.
	 * @value The shop.
	 */

	public Menu(Player player) {
		instance = this;
		this.player = player;
		preLoad();
	}

	/**
	 * Preloads the configs into their corresponding objects.
	 */
	public void preLoad() {

		GUI = new Gui(Main.getInstance(), 1, "Menu");

		OutlinePane page = new OutlinePane(0, 0, 9, 6);

		ConfigurationSection menuItems = Main.getInstance().getConfig().getConfigurationSection("menu-items");

		for (String key : menuItems.getKeys(false)) {

			if (!Main.getInstance().getMainConfig().getBoolean("menu-items." + key + ".Enabled")) {
				continue;
			}

			String shop = ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().getMainConfig().getString("menu-items." + key + ".Shop"));

			String name = ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().getMainConfig().getString("menu-items." + key + ".Name"));

			String description = ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().getMainConfig().getString("menu-items." + key + ".Desc"));

			List<String> lore = new ArrayList<>();

			if (description != null && description.length() > 0) {
				lore.add(description);
			}

			shops.put(Integer.parseInt(key), new ShopDir(shop, name, description, lore));

			if (player.hasPermission("guishop.slot." + key) || player.isOp()
					|| player.hasPermission("guishop.slot.*")) {
				String itemID = Main.getInstance().getMainConfig().getString("menu-items." + key + ".Item");

				Material material = null;

				if (material == null) {
					if ((material = XMaterial.valueOf(itemID).parseMaterial()) == null) {
						Main.getInstance().getLogger().log(Level.WARNING,
								"Could not parse material: " + itemID + " for item #: " + key);
						continue;
					}
				}

				ItemStack itemStack = setName(new ItemStack(material), name, lore);
				GuiItem gItem = new GuiItem(itemStack, event -> onShopClick(event));

				// SetItem no longer works with self created inventory object. Prefill with air?
				page.addItem(gItem);
			}
		}

		GUI.addPane(page);

	}

	/**
	 * Opens the GUI in this {@link Menu}.
	 */
	public void open() {
		if (!player.hasPermission("guishop.use") && !player.isOp()) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().getMainConfig().getString("no-permission")));
			return;
		}

		if (Main.getInstance().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Main.getInstance().getMainConfig().getString("disabled-world")));
			return;
		}
		GUI.show(player);
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
	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (e.getClickedInventory() != null) {
				Player player = (Player) e.getWhoClicked();
				if (player.getName().equals(this.player.getName())) {
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

					ShopDir shopDef = shops.get(e.getSlot());
					if (!shopDef.getShop().equalsIgnoreCase("")) {
						openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(),
								shopDef.getLore(), e.getSlot(), player, instance);
						if (!Main.getInstance().getLoadedShops().containsKey(e.getSlot())) {
							openShop.loadShop();
						}else {
							openShop = Main.getInstance().getLoadedShops().get(e.getSlot());
						}
						openShop.open();
						return;
					}
				}
			}
		}
	}

	/**
	 * The item currently being targetted.
	 * 
	 * @return {@link Menu} Menu Instance.
	 */
	public Menu getInstance() {
		return instance;
	}
}
