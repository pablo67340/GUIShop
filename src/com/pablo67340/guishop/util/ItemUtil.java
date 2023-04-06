package com.pablo67340.guishop.util;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.PotionInfo;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public final class ItemUtil {

    /**
     * @param price  Price
     * @param player The player who is setting an item price
     *               <p>
     *               Set an item's buy price
     */
    @SuppressWarnings("deprecation")
    public static void setBuyPrice(Object price, Player player) {
        ItemStack item;

        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        NBTItem comp = new NBTItem(item);

        if (price instanceof BigDecimal) {
            comp.setDouble("buyPrice", ((BigDecimal) price).doubleValue());
        } else if (price instanceof Integer) {
            comp.setDouble("buyPrice", ((Integer) price).doubleValue());
        } else if (price instanceof Boolean) {
            comp.removeKey("buyPrice");
        } else {
            return;
        }

        item = comp.getItem();

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(Config.getLoreConfig().lores.get("buy").replace("%amount%", "")) || str.contains(Config.getLoreConfig().lores.get("cannot-buy")) || str.contains(Config.getLoreConfig().lores.get("free"))) {
                if (price instanceof Boolean) {
                    lore.set(index, Config.getLoreConfig().lores.get("cannot-buy"));
                } else {
                    if (price.equals(new BigDecimal(0)) || (Integer) price == 0) {
                        lore.set(index, Config.getLoreConfig().lores.get("free"));
                    } else {
                        lore.set(index, Config.getLoreConfig().lores.get("buy")
                                .replace("%amount%", GUIShop.getINSTANCE().messageSystem.translate("currency-prefix")
                                        + price + GUIShop.getINSTANCE().messageSystem.translate("currency-suffix")));
                    }
                }
                hasReplaced = true;
                break;
            }
            index += 1;
        }
        if (!hasReplaced) {
            if (price instanceof Boolean) {
                lore.add(Config.getLoreConfig().lores.get("cannot-buy"));
            } else {
                if (price.equals(new BigDecimal(0)) || (Integer) price == 0) {
                    lore.add(Config.getLoreConfig().lores.get("free"));
                } else {
                    lore.add(Config.getLoreConfig().lores.get("buy")
                            .replace("%amount%", GUIShop.getINSTANCE().messageSystem.translate("messages.currency-prefix")
                                    + price + GUIShop.getINSTANCE().messageSystem.translate("messages.currency-suffix")));
                }
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "buy-price.successful", price instanceof Boolean
                ? GUIShop.getINSTANCE().messageSystem.translate("messages.buy-price.removed") : ((BigDecimal) price).toPlainString());
    }

    /**
     * @param price  Sell value
     * @param player The player who is setting an item price
     *               <p>
     *               Set an item's sell price
     */
    @SuppressWarnings("deprecation")
    public static void setSellPrice(Object price, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        NBTItem comp = new NBTItem(item);

        if (price instanceof BigDecimal) {
            comp.setDouble("sellPrice", ((BigDecimal) price).doubleValue());
        } else if (price instanceof Integer) {
            comp.setDouble("sellPrice", ((Integer) price).doubleValue());
        } else if (price instanceof Boolean) {
            comp.removeKey("sellPrice");
        } else {
            return;
        }

        item = comp.getItem();

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(Config.getLoreConfig().lores.get("sell").replace("%amount%", "")) || str.contains(Config.getLoreConfig().lores.get("cannot-sell"))) {
                if (price instanceof Boolean) {
                    lore.set(index, Config.getLoreConfig().lores.get("cannot-buy"));
                } else {
                    lore.set(index, Config.getLoreConfig().lores.get("sell")
                            .replace("%amount%", GUIShop.getINSTANCE().messageSystem.translate("currency-prefix")
                                    + price + GUIShop.getINSTANCE().messageSystem.translate("messages.currency-prefix")));
                }
                hasReplaced = true;
                break;
            }
            index += 1;
        }
        if (!hasReplaced) {
            if (price instanceof Boolean) {
                lore.add(Config.getLoreConfig().lores.get("cannot-buy"));
            } else {
                lore.add(Config.getLoreConfig().lores.get("sell")
                        .replace("%amount%", GUIShop.getINSTANCE().messageSystem.translate("currency-prefix")
                                + price + GUIShop.getINSTANCE().messageSystem.translate("messages.currency-prefix")));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "sell-price.successful", price instanceof Boolean
                ? GUIShop.getINSTANCE().messageSystem.translate("messages.sell-price.removed") : ((BigDecimal) price).toPlainString());
    }

    /**
     * @param name   The Item Name
     * @param player The player who clicked the item.
     *               <p>
     *               Set an item's shop-name
     */
    @SuppressWarnings("deprecation")
    public static void setNBT(Object name, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("nbt").replace("%nbt%", ""))) {
                if (name instanceof Boolean) {
                    lore.remove(index);
                    hasReplaced = true;
                } else {
                    lore.set(index, Config.getLoreConfig().lores.get("nbt").replace("%nbt%", name.toString()));
                    hasReplaced = true;
                    break;
                }
            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("nbt").replace("%nbt%", name.toString()));
        }
        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        if (name instanceof Boolean) {
            comp.removeKey("customNBT");
        } else {
            comp.setString("customNBT", (String) name);
        }

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "nbt.successful", name instanceof Boolean ? GUIShop.getINSTANCE().messageSystem.translate("messages.nbt.none") : name);
    }

    /**
     * @param name   The Item Name
     * @param player The player who clicked the item.
     *               <p>
     *               Set an item's shop-name
     */
    @SuppressWarnings("deprecation")
    public static void setShopName(Object name, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("shop-name").replace("%shop%", ""))) {
                if (name instanceof Boolean) {
                    lore.remove(index);
                } else {
                    lore.set(index, Config.getLoreConfig().lores.get("shop-name").replace("%shop%", name.toString()));
                }
                hasReplaced = true;
                break;
            }
            index += 1;
        }

        if (!hasReplaced) {
            lore.set(index, Config.getLoreConfig().lores.get("shop-name").replace("%shop%", name.toString()));
        }
        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        if (name instanceof Boolean) {
            comp.removeKey("shopName");
        } else {
            comp.setString("shopName", (String) name);
        }

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "shop-name.successful", name instanceof Boolean
                ? GUIShop.getINSTANCE().messageSystem.translate("messages.shop-name.removed") : name);
    }

    /**
     * @param name   The Item Name
     * @param player The player who clicked the item.
     *               <p>
     *               Set an item's shop-name
     */
    @SuppressWarnings("deprecation")
    public static void setName(Object name, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("name").replace("%name%", ""))) {
                if (name instanceof Boolean) {
                    lore.remove(index);
                    hasReplaced = true;
                } else {
                    lore.set(index, Config.getLoreConfig().lores.get("name").replace("%name%", name.toString()));
                    hasReplaced = true;
                    break;
                }
            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("name").replace("%name%", name.toString()));
        }
        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        if (name instanceof Boolean) {
            comp.removeKey("name");
        } else {
            comp.setString("name", (String) name);
        }

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "name.successful", name instanceof Boolean
                ? GUIShop.getINSTANCE().messageSystem.translate("messages.name.removed") : name);
    }

    /**
     * @param name   The Item Name
     * @param player The player who clicked the item.
     *               <p>
     *               Set an item's buy-name
     */
    @SuppressWarnings("deprecation")
    public static void setBuyName(Object name, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("buy-name").replace("%name%", ""))) {
                if (name instanceof Boolean) {
                    lore.remove(index);
                    hasReplaced = true;
                } else {
                    lore.set(index, Config.getLoreConfig().lores.get("buy-name").replace("%name%", name.toString()));
                    hasReplaced = true;
                    break;
                }
            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("buy-name").replace("%name%", name.toString()));
        }
        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        if (name instanceof Boolean) {
            comp.removeKey("buyName");
        } else {
            comp.setString("buyName", (String) name);
        }

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "buy-name.successful", name instanceof Boolean
                ? GUIShop.getINSTANCE().messageSystem.translate("messages.buy-name.removed") : name);
    }

    /**
     * @param enchantments The enchantments to add
     * @param player       The player who clicked the item.
     *                     <p>
     *                     Set an item's enchantments
     */
    @SuppressWarnings("deprecation")
    public static void setEnchantments(Object enchantments, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();
        Map<Enchantment, Integer> enchantmentMap = new ConcurrentHashMap<>();

        if (enchantments instanceof Boolean) {
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                im.removeEnchant(enchantment);
            }
        } else {
            String[] enc = ((String) enchantments).split(" ");
            // This map is here to NOT add any of the enchantments before every is checked
            for (String str : enc) {
                String enchantment = StringUtils.substringBefore(str, ":");
                String level = StringUtils.substringAfter(str, ":");
                try {
                    enchantmentMap.put(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level));
                } catch (NoSuchElementException | NullPointerException exception) {
                    GUIShop.sendPrefix(player, "enchant.invalid-enchantment", str);
                } catch (NumberFormatException exception) {
                    GUIShop.sendPrefix(player, "enchant.no-number");
                    return;
                }
            }
            if (!enchantmentMap.isEmpty()) {
                item.addEnchantments(enchantmentMap);
            }
        }

        List<String> enchantmentList = enchantmentMap.keySet().stream().map(Enchantment::getName).collect(Collectors.toList());

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("enchantments").replace("%enchantments%", ""))) {
                if (enchantments instanceof Boolean || enchantmentMap.isEmpty()) {
                    lore.remove(index);
                } else {
                    lore.set(index, Config.getLoreConfig().lores.get("enchantments").replace("%enchantments%", String.join(", ", enchantmentList)));
                }
                hasReplaced = true;
                break;
            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("enchantments").replace("%enchantments%", String.join(", ", enchantmentList)));
        }

        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        if (enchantments instanceof Boolean || enchantmentMap.isEmpty()) {
            comp.removeKey("enchantments");
        } else {
            comp.setString("enchantments", String.join(" ", enchantmentList));
        }

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "enchant.successful", enchantments instanceof Boolean || enchantmentMap.isEmpty()
                ? GUIShop.getINSTANCE().messageSystem.translate("messages.enchant.removed") : enchantments);
    }

    /**
     * @param line   The line to add to the shop lore
     * @param player The player who is setting the shop lore
     *               <p>
     *               Add a line to an item's Shop lore
     */
    @SuppressWarnings("deprecation")
    public static void addToShopLore(String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("shopLoreLines")) {
            line = comp.getString("shopLoreLines") + "::" + line;
        }
        String[] lines = line.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("shop-lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = index - 1; i < lines.length - 1; i++) {
                    lore.remove(index);
                }
            }
            index += 1;
        }

        lore.add(Config.getLoreConfig().lores.get("shop-lore"));
        for (String str : lines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, 20);
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        comp.setString("shopLoreLines", line);
        ItemStack fnl = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(fnl);
        } else {
            player.setItemInHand(fnl);
        }

        GUIShop.sendPrefix(player, "add-shop-lore.successful", line);
    }

    /**
     * @param slot   The line number the lore will be updated on
     * @param line   The text that will be saved to the specified line
     * @param player The player who is setting a Shop Lore
     *               <p>
     *               Edit an item's Shop lore
     */
    @SuppressWarnings("deprecation")
    public static void editShopLore(int slot, String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("shopLoreLines")) {
            preParsedLine = comp.getString("shopLoreLines");
        }

        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("shop-lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String line1 : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> tempLines = Arrays.asList(lines);
        tempLines.set(slot, line);
        String editedLine = tempLines.get(slot);

        lore.add(Config.getLoreConfig().lores.get("shop-lore"));
        for (String str : tempLines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = tempLines.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("shopLoreLines", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "edit-shop-lore.successful", slot, line);
    }

    /**
     * @param slot   The index the lore line will be deleted
     * @param player The player who is deleting a shop lore line
     *               <p>
     *               Delete an item's shop lore
     */
    @SuppressWarnings("deprecation")
    public static void deleteShopLore(int slot, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("shopLoreLines")) {
            preParsedLine = comp.getString("shopLoreLines");
        }
        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("shop-lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> linesList = new ArrayList<>(Arrays.asList(lines));
        linesList.remove(slot);

        lore.add(Config.getLoreConfig().lores.get("shop-lore"));
        for (String str : linesList) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = linesList.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("shopLoreLines", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "delete-shop-lore.successful", slot);
    }

    /**
     * @param line   The line to be added to the lore
     * @param player The player who is adding a line to the lore
     *               <p>
     *               Add a line to an item's buy lore
     */
    @SuppressWarnings("deprecation")
    public static void addToBuyLore(String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("buyLoreLines")) {
            line = comp.getString("buyLoreLines") + "::" + line;
        }
        String[] lines = line.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("buy-lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = 0; i < lines.length - 1; i++) {
                    lore.remove(index - 1);
                }

            }
            index += 1;
        }

        lore.add(Config.getLoreConfig().lores.get("buy-lore"));
        for (String str : lines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        comp.setString("buyLoreLines", line);
        ItemStack fnl = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(fnl);
        } else {
            player.setItemInHand(fnl);
        }

        GUIShop.sendPrefix(player, "add-buy-lore.successful", line);
    }

    /**
     * @param slot   The slot number of the item being edited
     * @param line   The line that will be added to the lore
     * @param player The player who is setting an item lore
     *               <p>
     *               Edit an item's lore line
     */
    @SuppressWarnings("deprecation")
    public static void editBuyLore(Integer slot, String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("buyLoreLines")) {
            preParsedLine = comp.getString("buyLoreLines");
        }
        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("buy-lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> tempLines = Arrays.asList(lines);
        tempLines.set(slot, line);

        lore.add(Config.getLoreConfig().lores.get("buy-lore"));
        for (String str : tempLines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = tempLines.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("buyLoreLines", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "edit-buy-lore.successful", slot, ChatColor.translateAlternateColorCodes('&', line));
    }

    /**
     * @param slot   The slot containing the item the lore will be removed on.
     * @param player The player who is setting an item price
     *               <p>
     *               Delete an Item's lore line
     */
    @SuppressWarnings("deprecation")
    public static void deleteBuyLore(int slot, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("buyLoreLines")) {
            preParsedLine = comp.getString("buyLoreLines");
        }

        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("buy-lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> lines2 = new ArrayList<>(Arrays.asList(lines));
        String line = lines2.get(slot);
        lines2.remove(slot);

        lore.add(Config.getLoreConfig().lores.get("buy-lore"));
        for (String str : lines2) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("buyLoreLines", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "delete-buy-lore.successful", slot);
    }

    /**
     * @param type   The Material name to set the item to
     * @param player The player setting type of the item
     */
    @SuppressWarnings("deprecation")
    public static void setType(String type, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(Config.getLoreConfig().lores.get("type").replace("%type%", ""))) {
                lore.set(index, Config.getLoreConfig().lores.get("type-lore").replace("type", type.toUpperCase(Locale.ROOT)));
                hasReplaced = true;
                break;
            }
            index += 1;
        }
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("type").replace("type", type.toUpperCase(Locale.ROOT)));
        }

        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        comp.setString("itemType", type);

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "type.successful", type.toUpperCase(Locale.ROOT));
    }

    /**
     * @param line   The line to be added to the item's runnable commands
     * @param player The player who is setting the commands
     */
    @SuppressWarnings("deprecation")
    public static void addCommand(String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("commands")) {
            line = comp.getString("commands") + "::" + line;
        }

        String[] lines = line.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("commands"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = 0; i < lines.length - 1; i++) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        lore.add(Config.getLoreConfig().lores.get("commands"));
        for (String str : lines) {
            if (str.length() > 20) {
                String s = "/" + str;
                s = s.substring(0, 20);
                lore.add(s + "...");
            } else {
                lore.add("/" + str);
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);
        comp.setString("commands", line);
        ItemStack fnl = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(fnl);
        } else {
            player.setItemInHand(fnl);
        }

        GUIShop.sendPrefix(player, "add-command.successful", line);
    }

    /**
     * @param slot   The slot containing the item that commands will be edited in
     * @param line   The command line that will be added to the item
     * @param player The player who is setting the command
     */
    @SuppressWarnings("deprecation")
    public static void editCommand(Integer slot, String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("commands")) {
            preParsedLine = comp.getString("commands");
        }

        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("commands"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
                break;
            }
            index += 1;
        }

        List<String> linesList = Arrays.asList(lines);
        linesList.set(slot, line);

        lore.add(Config.getLoreConfig().lores.get("commands"));
        for (String str : linesList) {
            if (str.length() > 20) {
                String s = "/" + str;
                s = s.substring(0, 20);
                lore.add(s + "...");
            } else {
                lore.add("/" + str);
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = linesList.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("commands", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "edit-command.successful", slot, line);
    }

    /**
     * @param slot   The slot containing the item a command will be deleted from
     * @param player The player who is deleting a command
     */
    @SuppressWarnings("deprecation")
    public static void deleteCommand(int slot, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("commands")) {
            preParsedLine = comp.getString("commands");
        }

        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("commands"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> tempLines = new ArrayList<>(Arrays.asList(lines));
        tempLines.remove(slot);

        lore.add(Config.getLoreConfig().lores.get("commands"));
        for (String str : tempLines) {
            if (str.length() > 20) {
                String s = "/" + str;
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add("/" + str);
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = tempLines.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("commands", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "delete-command.successful", slot);
    }

    /**
     * @param type   The entity name of a mob
     * @param player The player who is setting the mob entity type
     */
    @SuppressWarnings("deprecation")
    public static void setMobType(String type, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        boolean hasReplaced = false;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("mob-type").replace("%type%", ""))) {
                lore.set(index, Config.getLoreConfig().lores.get("mob-type").replace("%type%", type.toUpperCase(Locale.ROOT)));
                hasReplaced = true;
                break;
            }
            index += 1;
        }

        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("mob-type").replace("%type%", type.toUpperCase(Locale.ROOT)));
        }

        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        comp.setString("mobType", type);

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "mob-type.successful", type.toUpperCase(Locale.ROOT));
    }

    /**
     * @param shopName The Shop the item will open
     * @param player   The player who is setting the mob entity type
     */
    @SuppressWarnings("deprecation")
    public static void setTargetShop(String shopName, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(Config.getLoreConfig().lores.get("target-shop").replace("%shop%", ""))) {
                lore.set(index, Config.getLoreConfig().lores.get("target-shop").replace("%shop%", shopName));
                hasReplaced = true;
                break;
            }
            index += 1;
        }
        im.setLore(lore);
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("target-shop").replace("%shop%", shopName));
        }

        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        comp.setString("targetShop", shopName);

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "target-shop.successful", shopName);
    }

    /**
     * Set the potion info of the item the player is holding
     *
     * @param potionInfo The info that should be set
     * @param player     The player the holding item comes from
     */
    public static void setPotionInfo(PotionInfo potionInfo, Player player) {
        ItemStack item;

        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getEquipment().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        String stringInfo = potionInfo.getType() + "::" + potionInfo.getSplash() + "::" + potionInfo.getExtended() + "::" + potionInfo.getUpgraded();

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        int index = 0;
        boolean hasReplaced = false;
        for (String str : lore) {
            if (str.contains(Config.getLoreConfig().lores.get("potion-info").replace("%info%", ""))) {
                lore.set(index, Config.getLoreConfig().lores.get("potion-info").replace("%info%", stringInfo.replace("::", " ")));
                hasReplaced = true;
                break;
            }
            index += 1;
        }
        im.setLore(lore);
        if (!hasReplaced) {
            lore.add(Config.getLoreConfig().lores.get("potion-info").replace("%info%", stringInfo.replace("::", " ")));
        }

        im.setLore(lore);

        item.setItemMeta(im);

        NBTItem comp = new NBTItem(item);

        comp.setString("potion", stringInfo);

        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "potion-info.successful", potionInfo.getType(), potionInfo.getSplash(), potionInfo.getExtended(), potionInfo.getUpgraded());
    }

    /**
     * @param line   The line to be added to the lore
     * @param player The player who is adding a line to the lore
     *               <p>
     *               Add a line to an item's buy lore
     */
    @SuppressWarnings("deprecation")
    public static void addToLore(String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("loreLines")) {
            line = comp.getString("loreLines") + "::" + line;
        }
        String[] lines = line.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (int i = 0; i < lines.length - 1; i++) {
                    lore.remove(index - 1);
                }

            }
            index += 1;
        }

        lore.add(Config.getLoreConfig().lores.get("lore"));
        for (String str : lines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        comp.setString("loreLines", line);
        ItemStack fnl = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(fnl);
        } else {
            player.setItemInHand(fnl);
        }

        GUIShop.sendPrefix(player, "add-lore.successful", line);
    }

    /**
     * @param slot   The slot number of the item being edited
     * @param line   The line that will be added to the lore
     * @param player The player who is setting an item lore
     *               <p>
     *               Edit an item's lore line
     */
    @SuppressWarnings("deprecation")
    public static void editLore(Integer slot, String line, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        line = ChatColor.translateAlternateColorCodes('&', line);

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("loreLines")) {
            preParsedLine = comp.getString("loreLines");
        }
        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> tempLines = Arrays.asList(lines);
        tempLines.set(slot, line);

        lore.add(Config.getLoreConfig().lores.get("lore"));
        for (String str : tempLines) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = tempLines.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("loreLines", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "edit-lore.successful", slot, line);
    }

    /**
     * @param slot   The slot containing the item the lore will be removed on.
     * @param player The player who is setting an item price
     *               <p>
     *               Delete an Item's lore line
     */
    @SuppressWarnings("deprecation")
    public static void deleteLore(int slot, Player player) {
        ItemStack item;
        if (GUIShop.isMainHandNull(player)) {
            GUIShop.sendPrefix(player, "need-item");
            return;
        }

        if (XMaterial.getVersion() > 18) {
            item = player.getInventory().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        String preParsedLine = "";

        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("loreLines")) {
            preParsedLine = comp.getString("loreLines");
        }

        String[] lines = preParsedLine.split("::");

        ItemMeta im = item.getItemMeta();

        List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
        List<String> tempLore = new ArrayList<>(lore);
        int index = 0;
        for (String str : tempLore) {
            if (str.contains(Config.getLoreConfig().lores.get("lore"))) {
                lore.remove(index - 1);
                lore.remove(index - 1);
                for (String ignored : lines) {
                    lore.remove(index - 1);
                }
            }
            index += 1;
        }

        List<String> lines2 = new ArrayList<>(Arrays.asList(lines));
        String line = lines2.get(slot);
        lines2.remove(slot);

        lore.add(Config.getLoreConfig().lores.get("lore"));
        for (String str : lines2) {
            if (str.length() > 20) {
                String s = ChatColor.translateAlternateColorCodes('&', str);
                s = s.substring(0, Math.min(s.length(), 20));
                lore.add(s + "...");
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            }
        }

        im.setLore(lore);
        item.setItemMeta(im);

        comp = new NBTItem(item);

        String fnl = "";
        fnl = lines2.stream().map(str -> str + "::").reduce(fnl, String::concat);

        comp.setString("loreLines", fnl);
        item = comp.getItem();

        if (XMaterial.getVersion() > 18) {
            player.getInventory().setItemInMainHand(item);
        } else {
            player.setItemInHand(item);
        }

        GUIShop.sendPrefix(player, "delete-lore.successful", slot);
    }
}
