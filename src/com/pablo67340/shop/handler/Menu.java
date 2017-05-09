package com.pablo67340.shop.handler;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.*;

import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.shop.main.Main;

public final class Menu {

	/**
	 * The GUI that is projected onto the screen when
	 * a {@link Player} opens the {@link Menu}.
	 */
	private Inventory GUI;

	/**
	 * The {@link Player} that this {@link Menu} is 
	 * created for.
	 */
	private final Player player;

	public Menu(Player player) {
		this.player = player;

		load();
	}

	@SuppressWarnings("deprecation")
	public void load() {
		if (Main.SHOPS.size() < 9){
			GUI = player.getServer().createInventory(null, 9, "Menu");
		}else{
			GUI = player.getServer().createInventory(null, Main.SHOPS.size(), "Menu");
		}


		for (Entry<Integer, Shop> e : Main.SHOPS.entrySet()) {
			if (player.hasPermission("guishop.slot." + (e.getKey() + 1)) || player.isOp()) {
				String itemID = Main.INSTANCE.getMainConfig().getString(String.valueOf(e.getKey() + 1) + ".Item");
				if (itemID.contains(":")){
					String[] ids = itemID.split(":");
					GUI.setItem(e.getKey(), setName(new ItemStack(Material.getMaterial(Integer.parseInt(ids[0])), 1, Short.parseShort(ids[1])), e.getValue().getName(), e.getValue().getLore()));
					continue;
				}else{
					GUI.setItem(e.getKey(), setName(new ItemStack(Material.getMaterial(Integer.parseInt(itemID)), 1), e.getValue().getName(), e.getValue().getLore()));
					continue;
				}
			}

			List<String> lore = new ArrayList<>();

			lore.add(ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getMainConfig().getString("no-permission")));

			GUI.setItem(e.getKey(), setName(new ItemStack(Material.getMaterial(36), 1), e.getValue().getName(), lore));
		}
	}

	public void open() {
		if (!player.hasPermission("guishop.use") && !player.isOp()) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getMainConfig().getString("no-permission")));
			return;
		}

		if (Main.INSTANCE.getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getMainConfig().getString("disabled-world")));
			return;
		}

		player.openInventory(GUI);

		Main.HAS_MENU_OPEN.add(player.getName());
	}

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

}
