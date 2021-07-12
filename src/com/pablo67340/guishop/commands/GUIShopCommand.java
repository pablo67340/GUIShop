package com.pablo67340.guishop.commands;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.listenable.Value;
import com.pablo67340.guishop.util.ItemUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;

public class GUIShopCommand implements CommandExecutor {

    /**
     * Gets the permission corresponding to a subcommand of /guishop
     *
     * @param subCommand the subcommand, null for the base command
     * @return the permission
     */
    private String getRequiredPermission(String subCommand) {
        if (subCommand == null) {
            return "guishop.admin";
        }
        if ("reload".equalsIgnoreCase(subCommand)) {
            return "guishop.reload";
        }
        return "guishop.admin";
    }

    /**
     * Whether the command commandSender has the permission for a subcommand of
     * /guishop
     *
     * @param commandSender the command commandSender
     * @param subCommand    the sub command, null for the base command
     * @return true if permitted, false otherwise
     */
    private boolean hasRequiredPermission(CommandSender commandSender, String subCommand) {
        GUIShop.debugLog("commandSender is op: " + commandSender.isOp());
        return commandSender.hasPermission(getRequiredPermission(subCommand)) || commandSender.isOp();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (GUIShop.isNoEconomySystem()) {
            GUIShop.sendPrefix(commandSender, "no-economy-system");
            return true;
        }

        GUIShop.debugLog("Checking if commandSender is op");
        if (!hasRequiredPermission(commandSender, (args.length >= 1) ? args[0] : null)) {
            GUIShop.sendPrefix(commandSender, "no-permission");
            return true;
        }

        if (args.length >= 1) {
            if (!(commandSender instanceof Player)) {
                if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r")) {
                    GUIShop.getINSTANCE().reload(commandSender, false);
                } else {
                    GUIShop.sendPrefix(commandSender, "only-player");
                }
                return true;
            }

            Player player = (Player) commandSender;

            if (args[0].equalsIgnoreCase("reload")) {
                GUIShop.getINSTANCE().reload(commandSender, false);
            } else if (args[0].equalsIgnoreCase("parsemob")) {
                if (args.length >= 2) {
                    boolean isValid = false;

                    EntityType type = EntityType.fromName(args[1]);
                    if (type != null) {
                        isValid = true;
                    } else {
                        try {
                            EntityType.valueOf(args[1]);
                            isValid = true;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    GUIShop.sendPrefix(player, "parsemob.return", args[1],
                            isValid ? GUIShop.getINSTANCE().messageSystem.translate("messages.parsemob.return.valid") :
                                    GUIShop.getINSTANCE().messageSystem.translate("messages.parsemob.return.invalid"));
                } else {
                    GUIShop.sendPrefix(player, "parsemob.usage");
                }
            } else if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("e")) {
                if (args.length >= 2) {
                    Shop openShop = new Shop(player, args[1], new Menu());
                    openShop.loadItems(false);

                    if (!openShop.open(player)) {
                        if (args.length >= 3) {
                            try {
                                int page = Integer.parseInt(args[2]);
                                ((ShopPane) openShop.currentPane.getPanes().toArray()[openShop.currentPane.getPage()]).setVisible(false);
                                openShop.currentPane.setPage(page);
                                ((ShopPane) openShop.currentPane.getPanes().toArray()[openShop.currentPane.getPage()]).setVisible(true);
                                openShop.GUI.update();
                                GUIShop.getCREATOR().add(player.getUniqueId());
                                GUIShop.debugLog("Added player " + player.getName() + " to creator mode");
                            } catch (NumberFormatException numberFormatException) {
                                GUIShop.sendPrefix(player, "edit.no-number");
                            } catch (ArrayIndexOutOfBoundsException exception) {
                                GUIShop.sendPrefix(player, "edit.invalid-page", openShop.currentPane.getPages());
                            }
                        }
                    } else {
                        Menu menu = PlayerListener.INSTANCE.openMenu(player);
                        if (menu.hasMultiplePages()) {
                            try {
                                int page = Integer.parseInt(args[1]);
                                ((ShopPane) openShop.currentPane.getPanes().toArray()[openShop.currentPane.getPage()]).setVisible(false);
                                menu.currentPane.setPage(page);
                                ((ShopPane) menu.currentPane.getPanes().toArray()[menu.currentPane.getPage()]).setVisible(true);
                                menu.GUI.update();
                                GUIShop.getCREATOR().add(player.getUniqueId());
                                GUIShop.debugLog("Added player " + player.getName() + " to creator mode");
                            } catch (NumberFormatException numberFormatException) {
                                GUIShop.sendPrefix(player, "edit.no-number");
                            } catch (ArrayIndexOutOfBoundsException exception) {
                                GUIShop.sendPrefix(player, "edit.invalid-page", menu.currentPane.getPages());
                            }
                        }
                    }
                } else {
                    PlayerListener.INSTANCE.openMenu(player);
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    GUIShop.debugLog("Added player " + player.getName() + " to creator mode");
                }
            } else if (args[0].equalsIgnoreCase("b") || args[0].equalsIgnoreCase("buyprice") || args[0].equalsIgnoreCase("buy")) {
                if (args.length >= 2) {
                    Object result;
                    if (args[1].equalsIgnoreCase("false")) {
                        result = false;
                    } else {
                        try {
                            result = BigDecimal.valueOf(Double.parseDouble(args[1]));
                        } catch (NumberFormatException ex) {
                            try {
                                result = Integer.parseInt(args[1]);
                            } catch (NumberFormatException ex2) {
                                GUIShop.sendPrefix(player, "buy-price.no-number");
                                return true;
                            }
                        }
                    }
                    ItemUtil.setBuyPrice(result, player);
                } else {
                    GUIShop.sendPrefix(player, "buy-price.invalid-input");
                }
            } else if (args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("sellprice")) {
                if (args.length >= 2) {
                    Object result = null;
                    if (args[1].equalsIgnoreCase("false")) {
                        result = false;
                    } else {
                        try {
                            result = BigDecimal.valueOf(Double.parseDouble(args[1]));
                        } catch (NumberFormatException ex) {
                            try {
                                result = Integer.parseInt(args[1]);
                            } catch (NumberFormatException ex2) {
                                GUIShop.sendPrefix(player, "sell-price.no-number");
                            }
                        }
                    }
                    ItemUtil.setSellPrice(result, player);
                } else {
                    GUIShop.sendPrefix(player, "sell-price.invalid-input");
                }
            } else if (args[0].equalsIgnoreCase("sn") || args[0].equalsIgnoreCase("shopname")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    if (args.length == 2) {
                        boolean hasValue;
                        if (args[1].equalsIgnoreCase("false")) {
                            ItemUtil.setShopName(false, player);
                        } else {
                            ItemUtil.setShopName(args[1], player);
                        }
                    } else {
                        ItemUtil.setShopName(line.toString(), player);
                    }
                } else {
                    GUIShop.sendPrefix(player, "shop-name.usage");
                }
            } else if (args[0].equalsIgnoreCase("n") || args[0].equalsIgnoreCase("name")) {
                if (args.length >= 2) {
                    String line = String.join(" ", Arrays.asList(args).subList(1, args.length - 1));

                    if (args.length == 2) {
                        if (args[1].equalsIgnoreCase("false")) {
                            ItemUtil.setName(false, player);
                        } else {
                            ItemUtil.setName(args[1], player);
                        }
                    } else {
                        ItemUtil.setName(line, player);
                    }
                } else {
                    GUIShop.sendPrefix(player, "name.usage");
                }
            } else if (args[0].equalsIgnoreCase("bn") || args[0].equalsIgnoreCase("buyname")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    if (args.length == 2) {
                        if (args[1].equalsIgnoreCase("false")) {
                            ItemUtil.setBuyName(false, player);
                        } else {
                            ItemUtil.setBuyName(args[1], player);
                        }
                    } else {
                        ItemUtil.setBuyName(line, player);
                    }
                } else {
                    GUIShop.sendPrefix(player, "buy-name.usage");
                }
            } else if (args[0].equalsIgnoreCase("en") || args[0].equalsIgnoreCase("enchant")) {
                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("false")) {
                        ItemUtil.setEnchantments(false, player);
                    } else {
                        String enchantments = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                        ItemUtil.setEnchantments(StringUtils.isBlank(enchantments) ? false : enchantments, player);
                    }
                } else {
                    GUIShop.sendPrefix(player, "enchant.usage");
                }
            } else if (args[0].equalsIgnoreCase("asll") || args[0].equalsIgnoreCase("addshoploreline")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addToShopLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.sendPrefix(player, "add-shop-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("dsll") || args[0].equalsIgnoreCase("deleteshoploreline")) {
                if (args.length >= 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "delete-shop-lore.invalid-number", args[1]);
                        return true;
                    }

                    ItemUtil.deleteShopLore(slot, player);
                } else {
                    GUIShop.sendPrefix(player, "delete-shop-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("esll") || args[0].equalsIgnoreCase("editshoploreline")) {
                if (args.length > 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "edit-shop-lore.invalid-number", args[1]);
                        return true;
                    }

                    String line = String.join(" ", Arrays.asList(args).subList(2, args.length - 1));

                    ItemUtil.editShopLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    GUIShop.sendPrefix(player, "edit-shop-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("addloreline")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addToLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.sendPrefix(player, "add-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("dll") || args[0].equalsIgnoreCase("deleteloreline")) {
                if (args.length >= 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "delete-lore.invalid-number", args[1]);
                        return true;
                    }

                    ItemUtil.deleteLore(slot, player);
                } else {
                    GUIShop.sendPrefix(player, "delete-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("ell") || args[0].equalsIgnoreCase("editloreline")) {
                if (args.length > 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "edit-lore.invalid-number", args[1]);
                        return true;
                    }

                    String line = String.join(" ", Arrays.asList(args).subList(2, args.length - 1));

                    ItemUtil.editLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    GUIShop.sendPrefix(player, "edit-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("abll") || args[0].equalsIgnoreCase("addbuyloreline")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addToBuyLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.sendPrefix(player, "add-buy-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("ebll") || args[0].equalsIgnoreCase("editbuyloreline")) {
                if (args.length > 2) {
                    int slot;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "edit-buy-lore.invalid", args[1]);
                        return true;
                    }

                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(2, args.length - 1)));
                    ItemUtil.editBuyLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.sendPrefix(player, "edit-buy-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("dbll") || args[0].equalsIgnoreCase("deletebuyloreline")) {
                if (args.length >= 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "delete-buy-lore.invalid", args[1]);
                        return true;
                    }

                    ItemUtil.deleteBuyLore(slot, player);
                } else {
                    GUIShop.sendPrefix(player, "delete-buy-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("t") || args[0].equalsIgnoreCase("type")) {
                if (args.length >= 2) {
                    String type = args[1].toUpperCase(Locale.ENGLISH);
                    try {
                        ItemType.valueOf(type);
                    } catch (IllegalArgumentException exception) {
                        type = "DUMMY";
                    }
                    ItemUtil.setType(type, player);
                } else {
                    GUIShop.sendPrefix(player, "type.usage");
                }
            } else if (args[0].equalsIgnoreCase("ac") || args[0].equalsIgnoreCase("addcommand")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addCommand(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.sendPrefix(player, "add-command.usage");
                }
            } else if (args[0].equalsIgnoreCase("ec") || args[0].equalsIgnoreCase("editcommand")) {
                if (args.length >= 3) {
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "edit-command.invalid-number", args[1]);
                    }

                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(2, args.length - 1)));
                    ItemUtil.editCommand(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    GUIShop.sendPrefix(player, "edit-command.usage");
                }
            } else if (args[0].equalsIgnoreCase("dc") || args[0].equalsIgnoreCase("deletecommand")) {
                if (args.length >= 2) {
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendPrefix(player, "delete-command.invalid-number", args[1]);
                    }
                    ItemUtil.deleteCommand(slot, player);
                } else {
                    GUIShop.sendPrefix(player, "delete-command.usage");
                }
            } else if (args[0].equalsIgnoreCase("mt") || args[0].equalsIgnoreCase("mobtype")) {
                if (args.length == 2) {
                    ItemUtil.setMobType(args[1], player);
                } else {
                    GUIShop.sendPrefix(player, "mob-type.usage");
                }
            } else if (args[0].equalsIgnoreCase("ts") || args[0].equalsIgnoreCase("targetShop")) {
                if (args.length == 2) {
                    if (GUIShop.getINSTANCE().getShopConfig().getKeys(false).contains(args[1])) {
                        ItemUtil.setTargetShop(args[1], player);
                    } else {
                        GUIShop.sendPrefix(player, "target-shop.invalid-shop", args[1]);
                    }
                } else {
                    GUIShop.sendPrefix(player, "target-shop.usage");
                }
            } else if (args[0].equalsIgnoreCase("printnbt")) {
                if (GUIShop.isMainHandNull(player)) {
                    ItemStack item;

                    if (XMaterial.isNewVersion()) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    GUIShop.sendPrefix(player, "printnbt.print", new NBTItem(item).getCompound().toString());
                } else {
                    GUIShop.sendPrefix(player, "need-item");
                }
            } else if (args[0].equalsIgnoreCase("nb") || args[0].equalsIgnoreCase("nbt")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }

                    if (args[1].equalsIgnoreCase("false")) {
                        ItemUtil.setNBT(null, player);
                    } else {
                        ItemUtil.setNBT(line.toString().trim(), player);
                    }
                } else {
                    GUIShop.sendPrefix(player, "nbt.usage");
                }
            } else if (args[0].equalsIgnoreCase("shops") || args[0].equalsIgnoreCase("listshops") || args[0].equalsIgnoreCase("ls")) {
                GUIShop.sendPrefix(player, "list-shops.print",
                        GUIShop.getINSTANCE().loadedShops.isEmpty() ?
                                GUIShop.getINSTANCE().messageSystem.translate("messages.list-shops.none") :
                                String.join(", ", GUIShop.getINSTANCE().loadedShops.keySet()));
            } else if (args[0].equalsIgnoreCase("commands") || args[0].equalsIgnoreCase("listcommands") || args[0].equalsIgnoreCase("lc")) {
                ItemStack item;

                if (GUIShop.isMainHandNull(player)) {
                    GUIShop.sendPrefix(player, "need-item");
                    return true;
                }

                if (XMaterial.isNewVersion()) {
                    item = player.getEquipment().getItemInMainHand();
                } else {
                    item = player.getItemInHand();
                }

                StringBuilder replacement = new StringBuilder();

                NBTItem comp = new NBTItem(item);
                if (comp.hasKey("commands")) {
                    String[] commands = comp.getString("commands").split("::");
                    int index = 0;
                    for (String commandString : commands) {
                        replacement.append("\n").append(GUIShop.getINSTANCE().messageSystem.translate("messages.list-commands.command", index++, commandString));
                    }
                } else {
                    replacement.append(GUIShop.getINSTANCE().messageSystem.translate("messages.list-commands.none"));
                }

                GUIShop.sendPrefix(player, "list-commands.print", replacement.toString().trim());
            } else if (args[0].equalsIgnoreCase("potion") || args[0].equalsIgnoreCase("potioninfo") || args[0].equalsIgnoreCase("pi")) {
                if (args.length >= 5) {
                    String name = args[1];
                    try {
                        XPotion.matchXPotion(name).get().parsePotionEffectType();
                    } catch (NoSuchElementException | NullPointerException exception) {
                        GUIShop.sendPrefix(player, "potion-info.invalid", name);
                        return true;
                    }

                    boolean splash = "true".equalsIgnoreCase(args[2]);
                    boolean extended = "true".equalsIgnoreCase(args[3]);
                    boolean upgraded = "true".equalsIgnoreCase(args[4]);
                    PotionInfo potionInfo = new PotionInfo(name, splash, extended, upgraded);

                    ItemUtil.setPotionInfo(potionInfo, player);
                } else {
                    GUIShop.sendPrefix(player, "potion-info.usage");
                }
            } else if (args[0].equalsIgnoreCase("quantity") || args[0].equalsIgnoreCase("disablequantity") || args[0].equalsIgnoreCase("dq")) {
                if (args.length >= 2) {
                    boolean disableQuantity = "true".equalsIgnoreCase(args[1]);

                    ItemStack item;

                    if (GUIShop.isMainHandNull(player)) {
                        GUIShop.sendPrefix(player, "need-item");
                        return true;
                    }

                    if (XMaterial.isNewVersion()) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    NBTItem comp = new NBTItem(item);
                    comp.setBoolean("disableQuantity", disableQuantity);

                    item = comp.getItem();

                    if (XMaterial.isNewVersion()) {
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.setItemInHand(item);
                    }

                    GUIShop.sendPrefix(player, "disable-quantity.successful", disableQuantity);
                } else {
                    GUIShop.sendPrefix(player, "disable-quantity.usage");
                }
            } else if (args[0].equalsIgnoreCase("skulluuid") || args[0].equalsIgnoreCase("skull") || args[0].equalsIgnoreCase("head") ||
                    args[0].equalsIgnoreCase("headuuid") || args[0].equalsIgnoreCase("su") || args[0].equalsIgnoreCase("hu")) {
                if (args.length >= 2) {
                    String uuid = args[1];

                    ItemStack item;

                    if (GUIShop.isMainHandNull(player)) {
                        GUIShop.sendPrefix(player, "need-item");
                        return true;
                    }

                    if (XMaterial.isNewVersion()) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    NBTItem comp = new NBTItem(item);
                    comp.setString("skullUUID", uuid);

                    item = comp.getItem();
                    if (XMaterial.isNewVersion()) {
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.setItemInHand(item);
                    }

                    GUIShop.sendPrefix(player, "skull-uuid.successful", uuid);
                } else {
                    GUIShop.sendPrefix(player, "skull-uuid.usage");
                }
            } else if (args[0].equalsIgnoreCase("value") || args[0].equalsIgnoreCase("val") || args[0].equalsIgnoreCase("v")) {
                if (GUIShop.isMainHandNull(player)) {
                    GUIShop.sendPrefix(player, "need-item");
                } else {
                    ItemStack item;

                    if (XMaterial.isNewVersion()) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    String targetMaterial = item.getType().toString();
                    Value value = new Value(player, targetMaterial);
                    value.loadItems();
                }
            } else {
                PlayerListener.INSTANCE.printUsage(player);
            }
        } else {
            PlayerListener.INSTANCE.printUsage(commandSender);
        }
        return true;
    }
}
