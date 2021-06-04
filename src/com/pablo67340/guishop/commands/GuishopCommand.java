package com.pablo67340.guishop.commands;

import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class GuishopCommand implements CommandExecutor {

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
        switch (subCommand.toLowerCase()) {
            case "reload":
                return "guishop.reload";
            default:
                return "guishop.admin";
        }
    }

    /**
     * Whether the command sender has the permission for a subcommand of
     * /guishop
     *
     * @param sender     the command sender
     * @param subCommand the sub command, null for the base command
     * @return true if permitted, false otherwise
     */
    private boolean hasRequiredPermission(CommandSender sender, String subCommand) {
        GUIShop.debugLog("Sender is op: " + sender.isOp());
        return sender.hasPermission(getRequiredPermission(subCommand)) || sender.isOp();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        GUIShop.debugLog("Checking if sender is op");
        if (!hasRequiredPermission(commandSender, (args.length >= 1) ? args[0] : null)) {
            GUIShop.sendMessage(commandSender, ConfigUtil.getNoPermission());
            return true;
        }

        Player player = (Player) commandSender;

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("parsemob")) {
                if (args.length >= 2) {
                    Item tempItem = new Item();
                    tempItem.setMobType(args[1]);
                    player.sendMessage(args[1] + " is " + ((tempItem.parseMobSpawnerType() == null) ? "NOT " : "") + "a valid mob type.");
                } else {
                    player.sendMessage("Please specify a mob.");
                }

            } else if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("e")) {

                GUIShop.getCREATOR().add(player.getName());
                GUIShop.debugLog("Added player to creator mode");
                PlayerListener.INSTANCE.openShop(player);

            } else if (args[0].equalsIgnoreCase("p") || args[0].equalsIgnoreCase("price")) {
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
                            player.sendMessage("Please use a valid value");
                        }
                    }
                }
                ItemUtil.setPrice(result, player);

            } else if (args[0].equalsIgnoreCase("s") || args[0].equalsIgnoreCase("sell")) {

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
                                player.sendMessage("Please use a valid value");
                            }
                        }
                    }
                    ItemUtil.setSell(result, player);
                } else {
                    player.sendMessage("Please specify a value.");
                }

            } else if (args[0].equalsIgnoreCase("sn") || args[0].equalsIgnoreCase("shopname")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    if (args.length == 2) {
                        boolean hasValue;
                        if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("true")) {
                            hasValue = Boolean.parseBoolean(args[1]);
                            ItemUtil.setShopName(hasValue, player);
                        }
                    } else {
                        ItemUtil.setShopName(line.toString(), player);
                    }

                } else {
                    player.sendMessage("Please specify a custom sell-name.");
                }
            } else if (args[0].equalsIgnoreCase("n") || args[0].equalsIgnoreCase("name")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    if (args.length == 2) {
                        boolean hasValue;
                        if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("true")) {
                            hasValue = Boolean.parseBoolean(args[1]);
                            ItemUtil.setName(hasValue, player);
                        }
                    } else {
                        ItemUtil.setName(line.toString(), player);
                    }

                } else {
                    player.sendMessage("Please specify a custom name.");
                }
            } else if (args[0].equalsIgnoreCase("bn") || args[0].equalsIgnoreCase("buyname")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    if (args.length == 2) {
                        boolean hasValue;
                        if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("true")) {
                            hasValue = Boolean.parseBoolean(args[1]);
                            ItemUtil.setBuyName(hasValue, player);
                        }
                    } else {
                        ItemUtil.setBuyName(line.toString(), player);
                    }
                } else {
                    player.sendMessage("Please specify a custom buy-name.");
                }
            } else if (args[0].equalsIgnoreCase("en") || args[0].equalsIgnoreCase("enchant")) {
                if (args.length >= 2) {
                    StringBuilder enchantments = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        enchantments.append(args[x]).append(" ");
                    }
                    if (args.length == 2) {
                        boolean hasValue;
                        if (args[1].equalsIgnoreCase("false") || args[1].equalsIgnoreCase("true")) {
                            hasValue = Boolean.parseBoolean(args[1]);
                            ItemUtil.setEnchantments(hasValue, player);
                        }
                    } else {
                        ItemUtil.setEnchantments(enchantments.toString(), player);
                    }
                } else {
                    player.sendMessage("Please specify enchantments. E.G 'dura:1 sharp:2'");
                }
            } else if (args[0].equalsIgnoreCase("asll")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.addToShopLore(ChatColor.translateAlternateColorCodes('&', line.toString().trim()), player);
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
                    StringBuilder line = new StringBuilder();
                    int slot = Integer.parseInt(args[1]);
                    for (int x = 2; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.editCommand(slot, ChatColor.translateAlternateColorCodes('&', line.toString().trim()),
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
            } else if (args[0].equalsIgnoreCase("ts") || args[0].equalsIgnoreCase("targetShop")) {
                if (args.length == 2) {
                    ItemUtil.setTargetShop(args[1], player);
                } else {
                    player.sendMessage("Please specify a Target Shop");
                }
            } else if (args[0].equalsIgnoreCase("printnbt")) {

                ItemStack item = player.getItemInHand();
                if (item != null) {
                    String message = "Printed NBT: " + ItemNBTUtil.getTag(item).toNBT().toString();
                    GUIShop.log(message);
                    player.sendMessage(message);
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou must be holding an item."));
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
                    player.sendMessage("Please specify a custom NBT.");
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                GUIShop.getINSTANCE().reload(player, false);

            } else {
                PlayerListener.INSTANCE.printUsage(player);
            }
        } else {
            PlayerListener.INSTANCE.printUsage(player);
        }

        return true;
    }
}
