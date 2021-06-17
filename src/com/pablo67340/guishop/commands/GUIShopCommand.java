package com.pablo67340.guishop.commands;

import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

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
     * @param commandSender     the command commandSender
     * @param subCommand the sub command, null for the base command
     * @return true if permitted, false otherwise
     */
    private boolean hasRequiredPermission(CommandSender commandSender, String subCommand) {
        GUIShop.debugLog("commandSender is op: " + commandSender.isOp());
        return commandSender.hasPermission(getRequiredPermission(subCommand)) || commandSender.isOp();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (GUIShop.isNoEconomySystem()) {
            commandSender.sendMessage(Config.getPrefix() + " " + ChatColor.translateAlternateColorCodes('&', "&4The plugin didn't detect an economy system! \n" +
                    "&7Please contact a server administrator or setup an economy system."));
            return true;
        }
        
        GUIShop.debugLog("Checking if commandSender is op");
        if (!hasRequiredPermission(commandSender, (args.length >= 1) ? args[0] : null)) {
            GUIShop.sendMessage(commandSender, Config.getPrefix() + " " + Config.getNoPermission());
            return true;
        }

        if (args.length >= 1) {
            if (!(commandSender instanceof Player)) {
                if (args[0].equalsIgnoreCase("reload")) {
                    GUIShop.getINSTANCE().reload(commandSender, false);
                } else {
                    GUIShop.sendMessage(commandSender, Config.getPrefix() + " " + "&4You can only run this command as a player!");
                }
                return true;
            }

            Player player = (Player) commandSender;

            if (args[0].equalsIgnoreCase("reload")) {
                GUIShop.getINSTANCE().reload(commandSender, false);
            } else if (args[0].equalsIgnoreCase("parsemob")) {
                if (args.length >= 2) {
                    Item tempItem = new Item();
                    tempItem.setMobType(args[1]);
                    player.sendMessage(Config.getPrefix() + " " + args[1] + " is " + ((tempItem.parseMobSpawnerType() == null) ? "NOT " : "") + "a valid mob type.");
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a mob.");
                }
            } else if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("e")) {
                GUIShop.getCREATOR().add(player.getName());
                GUIShop.debugLog("Added player to creator mode");

                if (args.length >= 2) {
                    Shop openShop = new Shop(player, args[1], new Menu(player));
                    openShop.loadItems(false);
                    if (!openShop.open(player)) {
                        GUIShop.sendMessage(player, "The shop &c" + args[1] + " &fdoesn't exist!");
                    } else {
                        PlayerListener.INSTANCE.openShop(player);
                    }
                } else {
                    PlayerListener.INSTANCE.openShop(player);
                }
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
                            player.sendMessage(Config.getPrefix() + " " + "Please use a valid value");
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
                                player.sendMessage(Config.getPrefix() + " " + "Please use a valid number.");
                            }
                        }
                    }
                    ItemUtil.setSell(result, player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a value.");
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
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a custom sell-name.");
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
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a custom name.");
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
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a custom buy-name.");
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
                    player.sendMessage(Config.getPrefix() + " " + "Please specify enchantments. E.G 'dura:1 sharp:2'");
                }
            } else if (args[0].equalsIgnoreCase("asll") || args[0].equalsIgnoreCase("addshoplore")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.addToShopLore(ChatColor.translateAlternateColorCodes('&', line.toString().trim()), player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("dsll") || args[0].equalsIgnoreCase("deleteshoplore")) {
                if (args.length >= 2) {
                    int slot = Integer.parseInt(args[1]);
                    ItemUtil.deleteShopLore(slot, player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("esll") || args[0].equalsIgnoreCase("editshoplore")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    int slot = Integer.parseInt(args[1]);
                    for (int x = 2; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.editShopLore(slot, ChatColor.translateAlternateColorCodes('&', line.toString().trim()),
                            player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("abll") || args[0].equalsIgnoreCase("addbuylore")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.addToBuyLore(ChatColor.translateAlternateColorCodes('&', line.toString().trim()), player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("ebll") || args[0].equalsIgnoreCase("editbuylore")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendMessage(commandSender, "&cYour number input must be a number!");
                    }

                    for (int x = 2; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.editBuyLore(slot, ChatColor.translateAlternateColorCodes('&', line.toString().trim()),
                            player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a line.");
                }
            } else if (args[0].equalsIgnoreCase("t") || args[0].equalsIgnoreCase("type")) {
                if (args.length >= 2) {
                    String type = args[1];
                    try {
                        ItemType.valueOf(type);
                    } catch (IllegalArgumentException exception) {
                        type = "DUMMY";
                    }
                    ItemUtil.setType(type, player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a type.");
                }
            } else if (args[0].equalsIgnoreCase("ac") || args[0].equalsIgnoreCase("addcommand")) {
                if (args.length >= 2) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 1; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.addCommand(ChatColor.translateAlternateColorCodes('&', line.toString().trim()), player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a command.");
                }
            } else if (args[0].equalsIgnoreCase("ec") || args[0].equalsIgnoreCase("editcommand")) {
                if (args.length >= 3) {
                    StringBuilder line = new StringBuilder();

                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendMessage(commandSender, "&cYour number input must be a number!");
                    }

                    for (int x = 2; x <= args.length - 1; x++) {
                        line.append(args[x]).append(" ");
                    }
                    ItemUtil.editCommand(slot, ChatColor.translateAlternateColorCodes('&', line.toString().trim()),
                            player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a slot and a command.");
                }
            } else if (args[0].equalsIgnoreCase("dc") || args[0].equalsIgnoreCase("deletecommand")) {
                if (args.length >= 2) {
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.sendMessage(commandSender, "&cYour number input must be a number!");
                    }
                    ItemUtil.deleteCommand(slot, player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a slot.");
                }
            } else if (args[0].equalsIgnoreCase("mt") || args[0].equalsIgnoreCase("mobtype")) {
                if (args.length == 2) {
                    ItemUtil.setMobType(args[1], player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a type");
                }
            } else if (args[0].equalsIgnoreCase("ts") || args[0].equalsIgnoreCase("targetShop")) {
                if (args.length == 2) {
                    ItemUtil.setTargetShop(args[1], player);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a Target Shop");
                }
            } else if (args[0].equalsIgnoreCase("printnbt")) {
                if (player.getEquipment().getItemInMainHand() != null) {
                    ItemStack item = player.getEquipment().getItemInMainHand();
                    String message = "Printed NBT: " + ItemNBTUtil.getTag(item).toNBT().toString();
                    GUIShop.log(message);
                    player.sendMessage(Config.getPrefix() + " " + message);
                } else {
                    player.sendMessage(Config.getPrefix() + " " + ChatColor.translateAlternateColorCodes('&', "&cYou must be holding an item."));
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
                    player.sendMessage(Config.getPrefix() + " " + "Please specify a custom NBT.");
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
