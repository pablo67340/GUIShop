package com.pablo67340.GUIShop.Handlers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.GUIShop.Main.Main;

public class Menu {

	Main plugin;
	String title;
	String shopn;

	public Menu(Main main){
		plugin = main;
	}

	public void loadMenu(Player p){
		title = plugin.utils.getMenuName();
		shopn = "";
		if ((p.hasPermission("guishop.use")) || (p.isOp())){
			if (plugin.getConfig().getStringList("Disabled-Worlds").contains(p.getWorld().getName())){
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("disabled-world")));
			}else{
				List Ls = new ArrayList();
				Inventory chest = p.getPlayer().getServer().createInventory(null, plugin.getConfig().getInt("Rows") * 9, title);

				int[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
				int[] row2 = { 10, 11, 12, 13, 14, 15, 16, 17, 18 };
				int[] row3 = { 19, 20, 21, 23, 24, 25, 26, 27, 28 };
				for (int slot : row1) {
					if (plugin.getConfig().getString(slot + ".Enabled") == "true"){
						Ls.clear();
						String Name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Name"));
						if (ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Desc")) != "null") {
							Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Desc")));
						}
						if ((p.hasPermission("guishop.slot." + slot)) || (p.isOp())){
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(plugin.getConfig().getString(slot + ".Item")), 1), Name, Ls));
						}else{
							Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
						}
					}
				}
				if (plugin.getConfig().getInt("Rows") >= 2) {
					for (int slot : row2) {
						if (plugin.getConfig().getString(slot + ".Enabled") == "true")
						{
							Ls.clear();
							String Name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Name"));
							if (plugin.getConfig().getString(slot + ".Desc") != "null") {
								Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Desc")));
							}
							if ((p.hasPermission("guishop.slot." + slot)) || (p.isOp())){
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(plugin.getConfig().getString(slot + ".Item")), 1), Name, Ls));
							}else{
								Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
							}
						}
					}
				}
				if (plugin.getConfig().getInt("Rows") >= 3) {
					for (int slot : row3) {
						if (plugin.getConfig().getString(slot + ".Enabled") == "true"){
							Ls.clear();
							String Name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Name"));
							if (plugin.getConfig().getString(slot + ".Desc") != "null") {
								Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(slot + ".Desc")));
							}
							if ((p.hasPermission("guishop.slot." + slot)) || (p.isOp())){
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(plugin.getConfig().getString(slot + ".Item")), 1), Name, Ls));
							}else{
								Ls.add(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
							}
						}
					}
				}
				p.openInventory(chest);
			}
		}else{
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
		}
	}

	private ItemStack setName(ItemStack is, String name, List<String> lore){
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
