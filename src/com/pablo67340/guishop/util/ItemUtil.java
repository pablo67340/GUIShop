package com.pablo67340.guishop.util;

import com.cryptomorin.xseries.XEnchantment;
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
import org.apache.commons.lang.StringUtils;
import org.bukkit.enchantments.Enchantment;

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
        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(ConfigUtil.getBuyLore().replace("{amount}", "")) || str.contains(ConfigUtil.getCannotBuy())) {
                if (price instanceof Boolean) {
                    lore.set(index, ConfigUtil.getCannotBuy());
                    hasReplaced = true;
                } else {
                    lore.set(index, ConfigUtil.getBuyLore().replace("{amount}", ConfigUtil.getCurrency() + price + ""));
                    hasReplaced = true;
                }

            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(ConfigUtil.getBuyLore().replace("{amount}", ConfigUtil.getCurrency() + price + ""));
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
        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(ConfigUtil.getSellLore().replace("{amount}", "")) || str.contains(ConfigUtil.getCannotSell())) {
                if (price instanceof Boolean) {
                    lore.set(index, ConfigUtil.getCannotSell());
                    hasReplaced = true;
                } else {
                    lore.set(index, ConfigUtil.getSellLore().replace("{amount}", ConfigUtil.getCurrency() + price + ""));
                    hasReplaced = true;
                }

            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(ConfigUtil.getSellLore().replace("{amount}", ConfigUtil.getCurrency() + price + ""));
        }

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
    public static void setShopName(Object name, Player player) {
        ItemStack item;
        if (XMaterial.isNewVersion()) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }
        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains("Shop Name: ")) {
                if (name instanceof Boolean) {
                    lore.remove(index);
                    hasReplaced = true;
                } else {
                    lore.set(index, "Shop Name: " + name);
                    hasReplaced = true;
                }

            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add("Shop Name: " + name);
        }
        im.setLore(lore);

        item.setItemMeta(im);

        NBTTagCompound comp = ItemNBTUtil.getTag(item);

        if (name instanceof Boolean) {
            comp.remove("shopName");
        } else {
            comp.setString("shopName", (String) name);
        }

        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        if (!(name instanceof Boolean)) {
            player.sendMessage(ConfigUtil.getPrefix() + " Shop Name set: " + name);
        } else {
            player.sendMessage(ConfigUtil.getPrefix() + " Shop Name Removed");
        }
    }

    /**
     * @param name The Item Name
     * @param player The player who clicked the item.
     *
     * Set an item's buy-name
     *
     */
    @SuppressWarnings("deprecation")
    public static void setBuyName(Object name, Player player) {
        ItemStack item;

        if (XMaterial.isNewVersion()) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains("Buy Name: ")) {
                if (name instanceof Boolean) {
                    lore.remove(index);
                    hasReplaced = true;
                } else {
                    lore.set(index, "Buy Name: " + name);
                    hasReplaced = true;
                }

            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add("Buy Name: " + name);
        }
        im.setLore(lore);

        item.setItemMeta(im);

        NBTTagCompound comp = ItemNBTUtil.getTag(item);

        if (name instanceof Boolean) {
            comp.remove("buyName");
        } else {
            comp.setString("buyName", (String) name);
        }

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
    public static void setEnchantments(Object enchantments, Player player) {
        ItemStack item;
        if (XMaterial.isNewVersion()) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains("Enchantments: ")) {
                if (enchantments instanceof Boolean) {
                    lore.remove(index);
                    hasReplaced = true;
                } else {
                    lore.set(index, "Enchantments: " + enchantments);
                    hasReplaced = true;
                }

            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add("Enchantments: " + enchantments);
        }
        im.setLore(lore);

        if (enchantments instanceof Boolean) {
            for (Enchantment e : item.getEnchantments().keySet()) {
                im.removeEnchant(e);
            }
        } else {
            String[] enc = ((String) enchantments).split(" ");
            for (String str : enc) {
                String enchantment = StringUtils.substringBefore(str, ":");
                String level = StringUtils.substringAfter(str, ":");
                im.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
            }
        }

        item.setItemMeta(im);

        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        if (enchantments instanceof Boolean) {
            comp.remove("enchantments");

        } else {
            comp.setString("enchantments", (String) enchantments);
        }

        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }
        if (enchantments instanceof Boolean) {
            player.sendMessage(ConfigUtil.getPrefix() + " Enchantments removed ");
        } else {
            player.sendMessage(ConfigUtil.getPrefix() + " Enchantments set: " + ((String) enchantments).trim());
        }
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

        line = ChatColor.translateAlternateColorCodes('&', line);
        String addedLine = line;

        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        if (comp.hasKey("shopLoreLines")) {
            line = comp.getString("shopLoreLines") + "::" + line;
        }
        String[] lines = line.split("::");
        comp.setString("shopLoreLines", line);
        ItemStack fnl = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(fnl);
        } else {
            player.setItemInHand(fnl);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Added line to Shop lore: " + addedLine);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Shop Lore:");
        for (String str : lines) {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        }
    }

    /**
     * @param slot - The line number the lore will be updated on
     * @param line - The text that will be saved to the specified line
     * @param player - The player who is setting a Shop Lore
     *
     * Edit an item's Shop lore
     */
    @SuppressWarnings("deprecation")
    public static void editShopLore(Integer slot, String line, Player player) {
        ItemStack item;
        if (XMaterial.isNewVersion()) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        String[] lines = null;
        if (comp.hasKey("shopLoreLines")) {
            lines = comp.getString("shopLoreLines").split("::");
        }

        List<String> lines2 = Arrays.asList(lines);
        lines2.set(slot, line);

        String fnl = "";
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("shopLoreLines", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Edited line in Shop Lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current ShopLore:");
        lines2.forEach(str -> {
            player.sendMessage(ConfigUtil.getPrefix() + " - " + str);
        });
    }

    /**
     * @param slot - The index the lore line will be deleted
     * @param player - The player who is deleting a shop lore line
     *
     * Delete an item's shop lore
     */
    @SuppressWarnings("deprecation")
    public static void deleteShopLore(int slot, Player player) {
        ItemStack item;
        if (XMaterial.isNewVersion()) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        String[] lines = null;
        if (comp.hasKey("shopLoreLines")) {
            lines = comp.getString("shopLoreLines").split("::");
        }

        List<String> lines2 = Arrays.asList(lines);
        String line = lines2.get(slot);
        lines2.remove(slot);

        String fnl = "";
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("shopLoreLines", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Removed line in Shop Lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Shop Lore:");
        lines2.forEach(str -> {
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

        player.sendMessage(ConfigUtil.getPrefix() + " Added line to Buy Lore: " + addedLine);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Buy Lore:");
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

        player.sendMessage(ConfigUtil.getPrefix() + " Added line to Buy Lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Buy Lore:");
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

        player.sendMessage(ConfigUtil.getPrefix() + " Removed line to Buy Lore: " + line);
        player.sendMessage(ConfigUtil.getPrefix() + " Current Buy Lore:");
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

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains("Item Type: ")) {
                lore.set(index, ChatColor.translateAlternateColorCodes('&', "&fItem Type: &r" + type));
                hasReplaced = true;
            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "Item Type: " + type));
        }

        item.setItemMeta(im);

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

        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        if (comp.hasKey("commands")) {
            line = comp.getString("commands") + "::" + line;
        }
        String[] lines = line.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains("Commands")) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = 0; i < lines.length - 1; i++) {
                    lore.remove(index - 1);
                }

            }
            index += 1;
        }

        lore.add(" ");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&fCommands: &r"));
        for (String str : lines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', "/" + str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add("/" + str);
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = ItemNBTUtil.getTag(item);
        comp.setString("commands", line);
        ItemStack fnl = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(fnl);
        } else {
            player.setItemInHand(fnl);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Added Command to item: " + line);
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
        String preParsedLine = "";
        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        if (comp.hasKey("commands")) {
            preParsedLine = comp.getString("commands");
        }
        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains("Commands")) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = 0; i < lines.length; i++) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> lines2 = Arrays.asList(lines);
        lines2.set(slot, line);
        String editedLine = lines2.get(slot);

        lore.add(" ");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&fCommands: &r"));
        for (String str : lines2) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', "/" + str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add("/" + str);
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = ItemNBTUtil.getTag(item);

        String fnl = "";
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("commands", fnl);
        item = ItemNBTUtil.setNBTTag(comp, item);

        if (XMaterial.isNewVersion()) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        player.sendMessage(ConfigUtil.getPrefix() + " Added command to item: " + editedLine);
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

        String preParsedLine = "";
        NBTTagCompound comp = ItemNBTUtil.getTag(item);
        if (comp.hasKey("commands")) {
            preParsedLine = comp.getString("commands");
        }
        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains("Commands")) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = 0; i < lines.length; i++) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> lines2 = new ArrayList<>(Arrays.asList(lines));
        String line = lines2.get(slot);
        lines2.remove(slot);


        lore.add(" ");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&fCommands: &r"));
        for (String str : lines2) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', "/" + str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add("/" + str);
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = ItemNBTUtil.getTag(item);

        

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
