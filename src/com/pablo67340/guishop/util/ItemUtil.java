package com.pablo67340.guishop.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;

public final class ItemUtil {

	/**
	 * @param price Price
	 *              <p>
	 *              Set an item's buy price
	 */
	@SuppressWarnings("deprecation")
	public static void setPrice(Object price, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		List<String> lore = new ArrayList<>();

		ItemMeta im = item.getItemMeta();
		if (im.getLore() != null) {
			lore.addAll(im.getLore());
		}

		lore.add(Config.getBuyLore().replace("{amount}", price + ""));

		im.setLore(lore);
		item.setItemMeta(im);

		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		System.out.println("Setting");

		if (price instanceof Double) {
			comp.setDouble("buyPrice", (Double) price);
		} else if (price instanceof Integer) {
			comp.setDouble("buyPrice", ((Integer) price).doubleValue());
		} else if (price instanceof Boolean) {
			comp.remove("buyPrice");
		} else {
			player.sendMessage(
					Config.getPrefix() + " Pleas enter valid data. Accepted Value Example: (0.0, 100.0, 100, false)");
		}

		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Price set: " + price);
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void setSell(Object price, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		List<String> lore = new ArrayList<>();

		ItemMeta im = item.getItemMeta();
		if (im.getLore() != null) {
			lore.addAll(im.getLore());
		}

		lore.add(Config.getSellLore().replace("{amount}", price + ""));

		im.setLore(lore);
		item.setItemMeta(im);

		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		System.out.println("Setting");

		if (price instanceof Double) {
			comp.setDouble("sellPrice", (Double) price);
		} else if (price instanceof Integer) {
			comp.setDouble("sellPrice", ((Integer) price).doubleValue());
		} else if (price instanceof Boolean) {
			comp.remove("sellPrice");
		} else {
			player.sendMessage(
					Config.getPrefix() + " Pleas enter valid data. Accepted Value Example: (0.0, 100.0, 100, false)");
		}

		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Sell set: " + price);
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 * 
	 *             Set an item's sell price
	 *             
	 *             TODO: Add support for spaces in name
	 */
	@SuppressWarnings("deprecation")
	public static void setName(String name, Player player) {
		ItemStack item;
		name = ChatColor.translateAlternateColorCodes('&', name);
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		ItemMeta im = item.getItemMeta();

		im.setDisplayName(name);

		item.setItemMeta(im);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Name set: " + name);
	}

}
