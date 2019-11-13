package com.pablo67340.guishop.listenable;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public final class Menu {

	/**
	 * The GUI that is projected onto the screen when a {@link Player} opens the
	 * {@link Menu}.
	 */
	private Gui GUI;

	public static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

	/**
	 * A {@link Map} that will store our {@link Shop}s when the server first starts.
	 *
	 * @key The index on the {@link Menu} that this shop is located at.
	 * @value The shop.
	 */

	public Menu() {
		this.GUI = new Gui(Main.getINSTANCE(), Config.getMenuRows(), "Menu");
	}

	public void itemWarmup() {
		Thread t1 = new Thread(() -> {
			Main.getINSTANCE().getLogger().log(Level.INFO, "Warming Items...");
			long startTime = System.currentTimeMillis();
			ShopPane page = new ShopPane(9, 1);

			// We need to load 1 single item to get spigot to load
			// whatever item utils it needs too. Usually takes about 4s.
			for (ShopDef shopDef : Main.getINSTANCE().getShops().values()) {
				page.addItem(buildMenuItem(shopDef.getItemID(), shopDef));
				break;
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			Main.getINSTANCE().getLogger().log(Level.INFO, "Item warming completed in: " + estimatedTime + "ms");
		});
		t1.start();

	}

	/**
	 * Preloads the configs into their corresponding objects.
	 */
	public void preLoad(Player player) {

		ShopPane page = new ShopPane(9, 1);

		for (ShopDef shopDef : Main.getINSTANCE().getShops().values()) {

			if (shopDef.getItemType() == ItemType.SHOP) {
				if (player.hasPermission("guishop.shop." + shopDef.getShop()) || player.isOp()
						|| player.hasPermission("guishop.shop.*")) {
					page.addItem(buildMenuItem(shopDef.getItemID(), shopDef));
				}
			} else {
				page.addItem(buildMenuItem(shopDef.getItemID(), shopDef));
			}
		}

		GUI.addPane(page);

	}

	public GuiItem buildMenuItem(String itemID, ShopDef shopDef) {

		ItemStack itemStack = XMaterial.matchXMaterial(itemID).parseItem();

		if (shopDef.getItemType() != ItemType.BLANK) {
			setName(itemStack, shopDef.getName(), shopDef.getLore(), shopDef);
		}
		return new GuiItem(itemStack);
	}

	/**
	 * Opens the GUI in this {@link Menu}.
	 */
	public void open(Player player) {

		if (!player.hasPermission("guishop.use") && !player.isOp()) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("no-permission"))));
			return;
		}

		if (Main.getINSTANCE().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&',
					Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("disabled-world"))));
			return;
		}

		preLoad(player);

		GUI.setOnTopClick(this::onShopClick);
		GUI.setOnBottomClick(event -> {
			event.setCancelled(true);
		});
		GUI.show(player);

	}

	/**
	 * Sets the item's display name.
	 */
	private ItemStack setName(ItemStack item, String name, List<String> lore, ShopDef shopDef) {
		ItemMeta IM = item.getItemMeta();

		if (name != null) {
			assert IM != null;
			IM.setDisplayName(name);
		}

		if (lore != null && !lore.isEmpty() && shopDef.getItemType() == ItemType.SHOP) {
			assert IM != null;
			IM.setLore(lore);
		}

		item.setItemMeta(IM);

		return item;

	}

	/**
	 * Handle global inventory click events, check if inventory is for GUIShop, if
	 * so, run logic.
	 */
	private void onShopClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		e.setCancelled(true);

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
			return;
		}

		ShopDef shopDef = new ArrayList<>(Main.getINSTANCE().getShops().values()).get(e.getSlot());

		if (shopDef.getItemType() == ItemType.SHOP) {
			openShop(player, shopDef);
		}

	}

	public void openShop(Player player, ShopDef shopDef) {

		if (shopDef.getItemType() == ItemType.SHOP) {
			/*
			 * The currently open shop associated with this Menu instance.
			 */
			Shop openShop;
			if (!Main.getINSTANCE().getLoadedShops().containsKey(shopDef.getName())) {
				openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore(),
						this);
			} else {
				openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore(),
						this, Main.getINSTANCE().getLoadedShops().get(shopDef.getName()));
			}
			openShop.loadItems();
			openShop.open(player);
		}
	}

}
