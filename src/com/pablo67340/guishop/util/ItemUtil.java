package com.pablo67340.guishop.util;

import java.util.ArrayList;
import java.util.Arrays;
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
			
			im.getLore().forEach((str) -> {
				if (!str.contains(ChatColor.stripColor(Config.getBuyLore().replace("{amount}", "")))) {
					lore.add(str);
				}else {
					lore.add(Config.getBuyLore().replace("{amount}", price + ""));
				}
			});
		}

		im.setLore(lore);
		item.setItemMeta(im);

		NBTTagCompound comp = ItemNBTUtil.getTag(item);

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
	 * @param name   The Item Name
	 * @param player The player who clicked the item.
	 *               <p>
	 * 
	 *               Set an item's shop-name
	 * 
	 *               TODO: Add support for spaces in name
	 */
	@SuppressWarnings("deprecation")
	public static void setShopName(String name, Player player) {
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

	/**
	 * @param name   The Item Name
	 * @param player The player who clicked the item.
	 *               <p>
	 * 
	 *               Set an item's buy-name
	 * 
	 *               TODO: Add support for spaces in name
	 */
	@SuppressWarnings("deprecation")
	public static void setBuyName(String name, Player player) {
		ItemStack item;
		name = ChatColor.translateAlternateColorCodes('&', name);
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		comp.setString("buyName", name);
		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Buy-Name set: " + name);
	}

	/**
	 * @param name   The Item Name
	 * @param player The player who clicked the item.
	 *               <p>
	 * 
	 *               Set an item's buy-name
	 * 
	 *               TODO: Add support for spaces in name
	 */
	@SuppressWarnings("deprecation")
	public static void setEnchantments(String enchantments, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		comp.setString("enchantments", enchantments);
		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Enchantments set: " + enchantments.trim());
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void addToShopLore(String line, Player player) {
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

		lore.add(ChatColor.translateAlternateColorCodes('&', line));

		im.setLore(lore);
		item.setItemMeta(im);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Added line to lore: " + line);
		player.sendMessage(Config.getPrefix() + " Current Lore:");
		for (String str : lore) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void editShopLore(Integer index, String line, Player player) {
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

		lore.set(index, ChatColor.translateAlternateColorCodes('&', line));

		im.setLore(lore);
		item.setItemMeta(im);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Edited line in lore: " + line);
		player.sendMessage(Config.getPrefix() + " Current Lore:");
		for (String str : lore) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void deleteShopLore(int index, Player player) {
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

		String line = lore.get(index);

		lore.remove(index);

		im.setLore(lore);
		item.setItemMeta(im);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Deleted line to lore: " + line);
		player.sendMessage(Config.getPrefix() + " Current Lore:");
		for (String str : lore) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void addToBuyLore(String line, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		line = ChatColor.translateAlternateColorCodes('&', line);
		String addedLine = line;

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		if (comp.hasKey("loreLines")) {
			line = comp.getString("loreLines") + "::" + line;
		}
		String[] lines = line.split("::");
		comp.setString("loreLines", line);
		ItemStack fnl = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(fnl);
		} else {
			player.setItemInHand(fnl);
		}

		player.sendMessage(Config.getPrefix() + " Added line to lore: " + addedLine);
		player.sendMessage(Config.getPrefix() + " Current Lore:");
		for (String str : lines) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void editBuyLore(Integer slot, String line, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		line = ChatColor.translateAlternateColorCodes('&', line);

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		String[] lines = null;
		if (comp.hasKey("loreLines")) {
			lines = comp.getString("loreLines").split("::");
		}

		List<String> lines2 = Arrays.asList(lines);
		lines2.set(slot, line);

		String fnl = "";
		for (String str : lines2) {
			fnl += str + "::";
		}

		comp.setString("loreLines", fnl);
		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Added line to lore: " + line);
		player.sendMessage(Config.getPrefix() + " Current Lore:");
		for (String str : lines2) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void deleteBuyLore(int slot, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		String[] lines = null;
		if (comp.hasKey("loreLines")) {
			lines = comp.getString("loreLines").split("::");
		}

		List<String> lines2 = Arrays.asList(lines);
		String line = lines2.get(slot);
		lines2.remove(slot);

		String fnl = "";
		for (String str : lines2) {
			fnl += str + "::";
		}

		comp.setString("loreLines", fnl);
		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Removed line to lore: " + line);
		player.sendMessage(Config.getPrefix() + " Current Lore:");
		for (String str : lines2) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void setType(String type, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		comp.setString("itemType", type);

		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Set Item Type: " + type);
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void addCommand(String line, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		line = ChatColor.translateAlternateColorCodes('&', line);
		String addedLine = line;

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		if (comp.hasKey("loreLines")) {
			line = comp.getString("commands") + "::" + line;
		}
		String[] lines = line.split("::");
		comp.setString("commands", line);
		ItemStack fnl = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(fnl);
		} else {
			player.setItemInHand(fnl);
		}

		player.sendMessage(Config.getPrefix() + " Added Command to item: " + addedLine);
		player.sendMessage(Config.getPrefix() + " Current Commands:");
		for (String str : lines) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void editCommand(Integer slot, String line, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		line = ChatColor.translateAlternateColorCodes('&', line);

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		String[] lines = null;
		if (comp.hasKey("commands")) {
			lines = comp.getString("commands").split("::");
		}

		List<String> lines2 = Arrays.asList(lines);
		lines2.set(slot, line);

		String fnl = "";
		for (String str : lines2) {
			fnl += str + "::";
		}

		comp.setString("commands", fnl);
		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Added command to item: " + line);
		player.sendMessage(Config.getPrefix() + " Current Commands:");
		for (String str : lines2) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void deleteCommand(int slot, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		NBTTagCompound comp = ItemNBTUtil.getTag(item);
		String[] lines = null;
		if (comp.hasKey("commands")) {
			lines = comp.getString("commands").split("::");
		}

		List<String> lines2 = Arrays.asList(lines);
		String line = lines2.get(slot);
		lines2.remove(slot);

		String fnl = "";
		for (String str : lines2) {
			fnl += str + "::";
		}

		comp.setString("commands", fnl);
		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Removed command from item: " + line);
		player.sendMessage(Config.getPrefix() + " Current Commands:");
		for (String str : lines2) {
			player.sendMessage(Config.getPrefix() + " - " + str);
		}
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public static void setMobType(String type, Player player) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getItemInHand();
		}

		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		comp.setString("mobType", type);

		item = ItemNBTUtil.setNBTTag(comp, item);

		if (XMaterial.isNewVersion()) {
			player.getInventory().setItemInMainHand(item);
		} else {
			player.setItemInHand(item);
		}

		player.sendMessage(Config.getPrefix() + " Set Item Mob Type: " + type);
	}

}
