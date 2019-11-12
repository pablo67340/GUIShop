package com.pablo67340.guishop.listenable;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.handler.ShopDir;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.ShopPane;
import com.pablo67340.guishop.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@SuppressWarnings({ "JavaDoc", "SpellCheckingInspection" })
public final class Menu {

	/**
	 * The GUI that is projected onto the screen when a {@link Player} opens the
	 * {@link Menu}.
	 */
	private Gui GUI;

	/**
	 * A {@link Map} that will store our {@link Shop}s when the server first starts.
	 *
	 * @key The index on the {@link Menu} that this shop is located at.
	 * @value The shop.
	 */

	public Menu() {
		this.GUI = new Gui(Main.getINSTANCE(), Config.getMenuRows(), "Menu");
	}

	/**
	 * Preloads the configs into their corresponding objects.
	 */
	public void preLoad(Player player) {

		ShopPane page = new ShopPane(9, 1);
		
		Integer slot = 0;

		for (ShopDir shopDef : Main.getINSTANCE().getShops().values()) {


			if (shopDef.getItemType() == ItemType.SHOP) {
				if (player.hasPermission("guishop.slot." + slot) || player.isOp()
						|| player.hasPermission("guishop.slot.*")) {
					page.addItem(buildMenuItem(shopDef.getItemID(), shopDef));
				}
			}else {
				page.addItem(buildMenuItem(shopDef.getItemID(), shopDef));
			}
			slot+=1;
		}

		GUI.addPane(page);

	}
	
	
	public GuiItem buildMenuItem(String itemID, ShopDir shopDef) {
		

		ItemStack itemStack = XMaterial.valueOf(itemID).parseItem();

		GuiItem gItem = new GuiItem(itemStack);
		setName(gItem, shopDef.getName(), shopDef.getLore(), shopDef);
		return gItem;
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
	private void setName(GuiItem item, String name, List<String> lore, ShopDir shopDef) {
		ItemMeta IM = item.getItem().getItemMeta();

		if (name != null) {
			assert IM != null;
			IM.setDisplayName(name);
		}

		if (lore != null && !lore.isEmpty() && shopDef.getItemType() == ItemType.SHOP) {
			assert IM != null;
			IM.setLore(lore);
		}

		item.getItem().setItemMeta(IM);

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
		
		ShopDir shopDef = new ArrayList<>(Main.getINSTANCE().getShops().values()).get(e.getSlot());

		openShop(player, shopDef);

	}

	public void openShop(Player player, ShopDir shopDef) {
		
		if (!shopDef.getShop().equalsIgnoreCase("")) {
			/*
			 * The currently open shop associated with this Menu instance.
			 */
			Shop openShop;
			if (!Main.getINSTANCE().getLoadedShops().containsKey(shopDef.getName())) {
				openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore(), this);
			} else {
				openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore(), this, Main.getINSTANCE().getLoadedShops().get(shopDef.getName()));
			}
			openShop.loadItems();
			openShop.open(player);
		}
	}

}
