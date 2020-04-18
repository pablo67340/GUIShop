package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GuishopCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Player player = (Player) commandSender;

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("e")) {
                if (player.hasPermission("guishop.admin") || player.isOp()) {
                    Main.getCREATOR().add(player.getName());
                    Main.debugLog("Added player to creator mode");
                    PlayerListener.INSTANCE.openShop(player);
                }
            } else if (args[0].equalsIgnoreCase("p") || args[0].equalsIgnoreCase("price")) {
                Object result = null;
                if (args[1].equalsIgnoreCase("false")) {
                    result = false;
                } else {
                    try {
                        result = Double.parseDouble(args[1]);
                    } catch (Exception ex) {
                        try {
                            result = Integer.parseInt(args[1]);
                        } catch (Exception ex2) {
                            player.sendMessage("Please use a valid value");
                        }
                    }
                }
                ItemUtil.setPrice(result, player);

            } else if (args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("sell")) {
                Object result = null;
                if (args[1].equalsIgnoreCase("false")) {
                    result = false;
                } else {
                    try {
                        result = Double.parseDouble(args[1]);
                    } catch (Exception ex) {
                        try {
                            result = Integer.parseInt(args[1]);
                        } catch (Exception ex2) {
                            player.sendMessage("Please use a valid value");
                        }
                    }
                }
                ItemUtil.setSell(result, player);

            } else if (args[0].equalsIgnoreCase("sn") || args[0].equalsIgnoreCase("shopname")) {
                if (args.length >= 2) {
                    String line = "";
                    for (int x = 1; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.setShopName(line, player);
                } else {
                    player.sendMessage("Please specify a custom sell-name.");
                }
            } else if (args[0].equalsIgnoreCase("bn") || args[0].equalsIgnoreCase("buyname")) {
                if (args.length >= 2) {
                    String line = "";
                    for (int x = 1; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.setBuyName(line, player);
                } else {
                    player.sendMessage("Please specify a custom buy-name.");
                }
            } else if (args[0].equalsIgnoreCase("e") || args[0].equalsIgnoreCase("enchant")) {
                if (args.length >= 2) {
                    String enchantments = "";
                    for (int x = 1; x <= args.length - 1; x++) {
                        enchantments += args[x] + " ";
                    }
                    ItemUtil.setEnchantments(enchantments.trim(), player);
                } else {
                    player.sendMessage("Please specify enchantments. E.G 'dura:1 sharp:2'");
                }
            } else if (args[0].equalsIgnoreCase("asll")) {
                if (args.length >= 2) {
                    String line = "";
                    for (int x = 1; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.addToShopLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("dsll")) {
                if (args.length >= 2) {
                    int slot = Integer.parseInt(args[1]);
                    ItemUtil.deleteShopLore(slot, player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("esll")) {
                if (args.length >= 2) {
                    String line = "";
                    int slot = Integer.parseInt(args[1]);
                    for (int x = 2; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.editShopLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("abll")) {
                if (args.length >= 2) {
                    String line = "";
                    for (int x = 1; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.addToBuyLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("dsll")) {
                if (args.length >= 2) {
                    int slot = Integer.parseInt(args[1]);
                    ItemUtil.deleteBuyLore(slot, player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("ebll")) {
                if (args.length >= 2) {
                    String line = "";
                    int slot = Integer.parseInt(args[1]);
                    for (int x = 2; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.editBuyLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("t")) {
                if (args.length >= 2) {
                    String type = args[1];
                    if (ItemType.valueOf(type) == null) {
                        player.sendMessage("Invalid Type! Accepted: SHOP, COMMAND, BLANK, DUMMY");
                        return false;
                    }
                    ItemUtil.setType(args[1], player);
                } else {
                    player.sendMessage("Please specify a type.");
                }
            } else if (args[0].equalsIgnoreCase("ac")) {
                if (args.length >= 2) {
                    String line = "";
                    for (int x = 1; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.addCommand(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("ec")) {
                if (args.length >= 2) {
                    String line = "";
                    int slot = Integer.parseInt(args[1]);
                    for (int x = 2; x <= args.length - 1; x++) {
                        line += args[x] + " ";
                    }
                    ItemUtil.editCommand(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("dc")) {
                if (args.length >= 2) {
                    int slot = Integer.parseInt(args[1]);
                    ItemUtil.deleteCommand(slot, player);
                } else {
                    player.sendMessage("Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("mt")) {
                if (args.length == 2) {
                    ItemUtil.setMobType(args[1], player);
                } else {
                    player.sendMessage("Please specify a type");
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (player.hasPermission("guishop.reload") || player.isOp()) {
                    Main.getINSTANCE().reload(player, false);
                } else {
                    Main.sendMessage(player, "&cNo Permission!");
                }
            } else {
                PlayerListener.INSTANCE.printUsage(player);
            }
        } else {
            PlayerListener.INSTANCE.printUsage(player);
        }

        return true;
    }
}
