package com.pablo67340.guishop.util;

import com.cryptomorin.xseries.XMaterial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.Main;

public final class ItemUtil {

    /**
     * @param price Price
     * @param player - The player who is setting an item price
     *
     * Set an item's buy price
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
                if (!str.contains(ChatColor.stripColor(ConfigUtil.getBuyLore().replace("{amount}", "")))) {
                    lore.add(str);
                } else {
                    lore.add(ConfigUtil.getBuyLore().replace("{amount}", price + ""));
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
                    ConfigUtil.getPrefix() + " Pleas enter valid data. Accepted Value Example: (0.0, 100.0, 100, false)");
        }

        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Price set: " + price);
    }

    /**
     * @param price Sell value
     * @param player - The player who is setting an item price
     *
     * Set an item's sell price
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
            for (String str : im.getLore()){
                if (!(price instanceof Boolean)){
                    Main.debugLog("Checking if "+str+" is "+ConfigUtil.getCannotSell());
                    if (!str.equalsIgnoreCase(ConfigUtil.getCannotSell())){
                        lore.add(str);
                    }
                }
            }
        }

        lore.add(ConfigUtil.getSellLore().replace("{amount}", price + ""));

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
                    ConfigUtil.getPrefix() + " Please enter valid data. Accepted Value Example: (0.0, 100.0, 100, false)");
        }

        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Sell set: " + price);
    }

    /**
     * @param name The Item Name
     * @param player The player who clicked the item.
     *
     * Set an item's shop-name
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

        player.sendMessage(ConfigUtil.getPrefix() + " Name set: " + name);
    }

    /**
     * @param name The Item Name
     * @param player The player who clicked the item.
     *
     * Set an item's buy-name
     *
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

        player.sendMessage(ConfigUtil.getPrefix() + " Buy-Name set: " + name);
    }

    /**
     * @param enchantments The enchantments to add
     * @param player The player who clicked the item.
     *
     * Set an item's enchantments
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

        player.sendMessage(ConfigUtil.getPrefix() + " Enchantments set: " + enchantments.trim());
    }

    /**
     * @param line The line to add to the shop lore
     * @param player - The player who is setting the shop lore
     *
     * Add a line to an item's Shop lore
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

        player.sendMessage(ConfigUtil.getPrefix() + " Added line to lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Lore:");
        lore.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param index - The line number the lore will be updated on
     * @param line - The text that will be saved to the specified line
     * @param player - The player who is setting a Shop Lore
     *
     * Edit an item's Shop lore
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

        player.sendMessage(ConfigUtil.getPrefix() + " Edited line in lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Lore:");
        lore.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param index - The index the lore line will be deleted
     * @param player - The player who is deleting a shop lore line
     *
     * Delete an item's shop lore
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

        player.sendMessage(ConfigUtil.getPrefix() + " Deleted line to lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Lore:");
        lore.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param line - The line to be added to the lore
     * @param player - The player who is adding a line to the lore
     *
     * Add a line to an item's buy lore
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

        player.sendMessage(ConfigUtil.getPrefix() + " Added line to lore: " + addedLine);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Lore:");
        for (String str : lines) {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        }
    }

    /**
     * @param slot - The slot number of the item being edited
     * @param line - The line that will be added to the lore
     * @param player - The player who is setting an item lore
     *
     * Edit an item's lore line
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
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("loreLines", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Added line to lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Lore:");
        lines2.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param slot - The slot containing the item the lore will be removed on.
     * @param player - The player who is setting an item price
     *
     * Delete an Item's lore line
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
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("loreLines", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Removed line to lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Lore:");
        lines2.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param type - The Material name to set the item to
     * @param player - The player setting type of the item
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

        player.sendMessage(ConfigUtil.getPrefix() + " Set Item Type: " + type);
    }

    /**
     * @param line - The line to be added to the item's runnable commands
     * @param player - The player who is setting the commands
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

        player.sendMessage(ConfigUtil.getPrefix() + " Added Command to item: " + addedLine);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Commands:");
        for (String str : lines) {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        }
    }

    /**
     * @param slot - The slot containing the item that commands will be edited
     * in
     * @param line - The command line that will be added to the item
     * @param player - The player who is setting the command
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
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("commands", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Added command to item: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Commands:");
        lines2.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param slot - The slot containing the item a command will be deleted from
     * @param player - The player who is deleting a command
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
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("commands", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Removed command from item: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Commands:");
        lines2.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param type - The entity name of a mob
     * @param player - The player who is setting the mob entity type
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

        player.sendMessage(ConfigUtil.getPrefix() + " Set Item Mob Type: " + type);
    }

}
