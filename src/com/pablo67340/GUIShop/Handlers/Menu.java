package com.pablo67340.GUIShop.Handlers;


import com.pablo67340.GUIShop.Main.Main;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Menu {
	protected Main plugin;
	protected String title;
	protected String shopn;
	public ArrayList<String> shops = new ArrayList<>();
	protected Inventory chest;

	public Menu(Main main) {
		plugin = main;
	}

	@SuppressWarnings("deprecation")
	public void loadMenu(Player p) {
		title = plugin.utils.getMenuName();
		shopn = "";
		if (p.hasPermission("guishop.use") || p.isOp()) {
			if (plugin.getConfig().getStringList("Disabled-Worlds").contains(p.getWorld().getName())) {
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disabled-world")));
			} else {
				String Name;
				ArrayList<String> Ls = new ArrayList<String>();
				chest = p.getPlayer().getServer().createInventory(null, plugin.getConfig().getInt("Rows") * 9, title);
				int[] row1 = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
				int[] row2 = new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18};
				int[] row3 = new int[]{19, 20, 21, 23, 24, 25, 26, 27, 28};
				for (int slot2 : row1) {
					if (plugin.getConfig().getString(String.valueOf(slot2) + ".Enabled") != "true") continue;
					Ls.clear();
					Name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Name"));

					if (!shops.contains(plugin.getConfig().getString(String.valueOf(slot2) + ".Shop"))){
						shops.add(plugin.getConfig().getString(String.valueOf(slot2) + ".Shop"));
					}

					if (ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Desc")) != "null") {
						Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Desc")));

					}
					if (p.hasPermission("guishop.slot." + slot2) || p.isOp()) {
						chest.setItem(slot2 - 1, setName(new ItemStack(Material.getMaterial(plugin.getConfig().getString(String.valueOf(slot2) + ".Item")), 1), Name, Ls));
						continue;
					}

					Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
					chest.setItem(slot2 - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
				}
				if (plugin.getConfig().getInt("Rows") >= 2) {
					for (int slot2 : row2) {
						if (plugin.getConfig().getString(String.valueOf(slot2) + ".Enabled") != "true") continue;
						Ls.clear();
						Name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Name"));
						if (!shops.contains(plugin.getConfig().getString(String.valueOf(slot2) + ".Shop"))){
							shops.add(plugin.getConfig().getString(String.valueOf(slot2) + ".Shop"));
						}
						if (plugin.getConfig().getString(String.valueOf(slot2) + ".Desc") != "null") {
							Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Desc")));
						}
						if (p.hasPermission("guishop.slot." + slot2) || p.isOp()) {
							chest.setItem(slot2 - 1, setName(new ItemStack(Material.getMaterial(plugin.getConfig().getString(String.valueOf(slot2) + ".Item")), 1), Name, Ls));
							continue;
						}

						Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
						chest.setItem(slot2 - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
					}
				}
				if (plugin.getConfig().getInt("Rows") >= 3) {
					for (int slot2 : row3) {
						if (plugin.getConfig().getString(String.valueOf(slot2) + ".Enabled") != "true") continue;
						Ls.clear();
						Name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Name"));
						if (!shops.contains(plugin.getConfig().getString(String.valueOf(slot2) + ".Shop"))){
							shops.add(plugin.getConfig().getString(String.valueOf(slot2) + ".Shop"));
						}
						if (plugin.getConfig().getString(String.valueOf(slot2) + ".Desc") != "null") {
							Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(String.valueOf(slot2) + ".Desc")));
						}
						if (p.hasPermission("guishop.slot." + slot2) || p.isOp()) {
							chest.setItem(slot2 - 1, setName(new ItemStack(Material.getMaterial(plugin.getConfig().getString(String.valueOf(slot2) + ".Item")), 1), Name, Ls));
							continue;
						}

						Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
						chest.setItem(slot2 - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
					}
				}
			}
		} else {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
		}
	}

	public void openMenu(Player p){
		plugin.listene.menuOpen.add(p.getName());
		p.openInventory(chest);
	}

	private ItemStack setName(ItemStack is, String name, List<String> lore) {
		ItemMeta IM = is.getItemMeta();
		if (name != null) {
			IM.setDisplayName(name);
		}
		if (lore != null) {
			IM.setLore(lore);
		}
		is.setItemMeta(IM);
		return is;
	}
}

