package com.pablo67340.guishop.commands;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.economy.EconomyConfig;
import com.pablo67340.guishop.economy.EconomyManager;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.listenable.Value;
import com.pablo67340.guishop.listenable.editor.TransactionEditor;
import com.pablo67340.guishop.util.ItemUtil;
import com.pablo67340.guishop.util.NameUtil;
import com.pablo67340.guishop.util.PDCUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;
import com.pablo67340.guishop.util.StringUtil;

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
     * @param subCommand the sub command, null for the base command
     * @return true if permitted, false otherwise
     */
    private boolean hasRequiredPermission(CommandSender commandSender, String subCommand) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("commandSender is op: " + commandSender.isOp());
        return GUIShop.getINSTANCE().getMiscUtils().getPerms().has(commandSender, getRequiredPermission(subCommand)) || commandSender.isOp();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (GUIShop.isNoEconomySystem()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(commandSender, "no-economy-system");
            return true;
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Checking if commandSender is op");
        if (!hasRequiredPermission(commandSender, (args.length >= 1) ? args[0] : null)) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(commandSender, "no-permission");
            return true;
        }

        if (args.length >= 1) {
            if (!(commandSender instanceof Player)) {
                // Console-allowed commands
                if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r")) {
                    GUIShop.getINSTANCE().reload(commandSender, false);
                } else if (args[0].equalsIgnoreCase("eco") || args[0].equalsIgnoreCase("economy")) {
                    handleEcoCommand(commandSender, args);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(commandSender, "only-player");
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

                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "parsemob.return", args[1],
                            isValid ? GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.parsemob.valid")
                                    : GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.parsemob.invalid"));
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "parsemob.usage");
                }
            } else if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("e")) {
                if (args.length >= 2) {
                    // Explicit "menu" argument opens the menu in edit mode
                    if (args[1].equalsIgnoreCase("menu")) {
                        // Check for optional page number
                        if (args.length >= 3) {
                            editMenu(args[2], player);
                        } else {
                            GUIShop.getCREATOR().add(player.getUniqueId());
                            GUIShop.getINSTANCE().getLogUtil().debugLog("Added player " + player.getName() + " to creator mode (menu)");
                            PlayerListener.INSTANCE.openMenuForEdit(player);
                        }
                        return true;
                    }
                    
                    // Explicit "transaction" argument opens the transaction GUI in edit mode
                    if (args[1].equalsIgnoreCase("transaction") || args[1].equalsIgnoreCase("trans")) {
                        GUIShop.getCREATOR().add(player.getUniqueId());
                        GUIShop.getINSTANCE().getLogUtil().log("Opening transaction editor for " + player.getName());
                        openTransactionEditor(player);
                        return true;
                    }
                    
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Edit command arg[1]: '" + args[1] + "'");

                    String nearestShop = NameUtil.nearestShop(args[1]);

                    if (nearestShop != null) {
                        // Add to CREATOR before opening so the shop sets up editor handlers
                        GUIShop.getCREATOR().add(player.getUniqueId());
                        GUIShop.getINSTANCE().getLogUtil().debugLog("Added player " + player.getName() + " to creator mode");
                        
                        Shop openShop = new Shop(player, nearestShop, new Menu());
                        openShop.loadItems(false);

                        if (!openShop.open(player)) {
                            GUIShop.getCREATOR().remove(player.getUniqueId()); // Remove if open failed
                            editMenu(args[1], player);
                            return true;
                        }

                        if (args.length >= 3) {
                            try {
                                int page = Integer.parseInt(args[2]);
                                if (!openShop.GUI.goToPage(page)) {
                                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit.invalid-page", openShop.GUI.getPageCount());
                                }
                            } catch (NumberFormatException numberFormatException) {
                                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit.no-number");
                            }
                        }
                    } else {
                        editMenu(args[1], player);
                    }
                } else {
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Added player " + player.getName() + " to creator mode");
                    PlayerListener.INSTANCE.openMenuForEdit(player);
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
                                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "buy-price.no-number");
                                return true;
                            }
                        }
                    }
                    ItemUtil.setBuyPrice(result, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "buy-price.invalid-input");
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
                                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "sell-price.no-number");
                            }
                        }
                    }
                    ItemUtil.setSellPrice(result, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "sell-price.invalid-input");
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
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "shop-name.usage");
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
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "name.usage");
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
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "buy-name.usage");
                }
            } else if (args[0].equalsIgnoreCase("en") || args[0].equalsIgnoreCase("enchant")) {
                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("false")) {
                        ItemUtil.setEnchantments(false, player);
                    } else {
                        String enchantments = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                        ItemUtil.setEnchantments(StringUtil.isBlank(enchantments) ? false : enchantments, player);
                    }
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "enchant.usage");
                }
            } else if (args[0].equalsIgnoreCase("asll") || args[0].equalsIgnoreCase("addshoploreline")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addToShopLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "add-shop-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("dsll") || args[0].equalsIgnoreCase("deleteshoploreline")) {
                if (args.length >= 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-shop-lore.invalid-number", args[1]);
                        return true;
                    }

                    ItemUtil.deleteShopLore(slot, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-shop-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("esll") || args[0].equalsIgnoreCase("editshoploreline")) {
                if (args.length > 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-shop-lore.invalid-number", args[1]);
                        return true;
                    }

                    String line = String.join(" ", Arrays.asList(args).subList(2, args.length - 1));

                    ItemUtil.editShopLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-shop-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("addloreline")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addToLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "add-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("dll") || args[0].equalsIgnoreCase("deleteloreline")) {
                if (args.length >= 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-lore.invalid-number", args[1]);
                        return true;
                    }

                    ItemUtil.deleteLore(slot, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("ell") || args[0].equalsIgnoreCase("editloreline")) {
                if (args.length > 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-lore.invalid-number", args[1]);
                        return true;
                    }

                    String line = String.join(" ", Arrays.asList(args).subList(2, args.length - 1));

                    ItemUtil.editLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("abll") || args[0].equalsIgnoreCase("addbuyloreline")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addToBuyLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "add-buy-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("ebll") || args[0].equalsIgnoreCase("editbuyloreline")) {
                if (args.length > 2) {
                    int slot;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-buy-lore.invalid", args[1]);
                        return true;
                    }

                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(2, args.length - 1)));
                    ItemUtil.editBuyLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-buy-lore.usage");
                }
            } else if (args[0].equalsIgnoreCase("dbll") || args[0].equalsIgnoreCase("deletebuyloreline")) {
                if (args.length >= 2) {
                    int slot;
                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-buy-lore.invalid", args[1]);
                        return true;
                    }

                    ItemUtil.deleteBuyLore(slot, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-buy-lore.usage");
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
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "type.usage");
                }
            } else if (args[0].equalsIgnoreCase("ac") || args[0].equalsIgnoreCase("addcommand")) {
                if (args.length >= 2) {
                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(1, args.length - 1)));
                    ItemUtil.addCommand(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "add-command.usage");
                }
            } else if (args[0].equalsIgnoreCase("ec") || args[0].equalsIgnoreCase("editcommand")) {
                if (args.length >= 3) {
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-command.invalid-number", args[1]);
                    }

                    String line = String.join(" ", new ArrayList<>(Arrays.asList(args).subList(2, args.length - 1)));
                    ItemUtil.editCommand(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
                            player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit-command.usage");
                }
            } else if (args[0].equalsIgnoreCase("dc") || args[0].equalsIgnoreCase("deletecommand")) {
                if (args.length >= 2) {
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-command.invalid-number", args[1]);
                    }
                    ItemUtil.deleteCommand(slot, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "delete-command.usage");
                }
            } else if (args[0].equalsIgnoreCase("mt") || args[0].equalsIgnoreCase("mobtype")) {
                if (args.length == 2) {
                    ItemUtil.setMobType(args[1], player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "mob-type.usage");
                }
            } else if (args[0].equalsIgnoreCase("ts") || args[0].equalsIgnoreCase("targetShop")) {
                if (args.length == 2) {
                    if (GUIShop.getINSTANCE().configManager.shopExists(args[1])) {
                        ItemUtil.setTargetShop(args[1], player);
                    } else {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "target-shop.invalid-shop", args[1]);
                    }
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "target-shop.usage");
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
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "nbt.usage");
                }
            } else if (args[0].equalsIgnoreCase("shops") || args[0].equalsIgnoreCase("listshops") || args[0].equalsIgnoreCase("ls")) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "list-shops.print",
                        GUIShop.getINSTANCE().loadedShops.isEmpty()
                        ? GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.list-shops.none")
                        : String.join(", ", GUIShop.getINSTANCE().loadedShops.keySet()));
            } else if (args[0].equalsIgnoreCase("commands") || args[0].equalsIgnoreCase("listcommands") || args[0].equalsIgnoreCase("lc")) {
                ItemStack item;

                if (GUIShop.getINSTANCE().getMiscUtils().isMainHandNull(player)) {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "need-item");
                    return true;
                }

                if (XMaterial.getVersion() > 18) {
                    item = player.getEquipment().getItemInMainHand();
                } else {
                    item = player.getItemInHand();
                }

                StringBuilder replacement = new StringBuilder();

                String commandsStr = PDCUtil.getString(item, PDCUtil.KEY_COMMANDS);
                if (commandsStr != null) {
                    String[] commands = commandsStr.split("::");
                    int index = 0;
                    for (String commandString : commands) {
                        replacement.append("\n").append(GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.list-commands.command", index++, commandString));
                    }
                } else {
                    replacement.append(GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.list-commands.none"));
                }

                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "list-commands.print", replacement.toString().trim());
            } else if (args[0].equalsIgnoreCase("potion") || args[0].equalsIgnoreCase("potioninfo") || args[0].equalsIgnoreCase("pi")) {
                if (args.length >= 5) {
                    String name = args[1];
                    try {
                        XPotion.matchXPotion(name).get().getPotionEffectType();
                    } catch (NoSuchElementException | NullPointerException exception) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "potion-info.invalid", name);
                        return true;
                    }

                    boolean splash = "true".equalsIgnoreCase(args[2]);
                    boolean extended = "true".equalsIgnoreCase(args[3]);
                    boolean upgraded = "true".equalsIgnoreCase(args[4]);
                    PotionInfo potionInfo = new PotionInfo(name, splash, extended, upgraded);

                    ItemUtil.setPotionInfo(potionInfo, player);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "potion-info.usage");
                }
            // Note: /gs quantity command removed - quantity selection is now handled by the Transaction GUI
            } else if (args[0].equalsIgnoreCase("skulluuid") || args[0].equalsIgnoreCase("skull") || args[0].equalsIgnoreCase("head")
                    || args[0].equalsIgnoreCase("headuuid") || args[0].equalsIgnoreCase("su") || args[0].equalsIgnoreCase("hu")) {
                if (args.length >= 2) {
                    String uuid = args[1];

                    ItemStack item;

                    if (GUIShop.getINSTANCE().getMiscUtils().isMainHandNull(player)) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "need-item");
                        return true;
                    }

                    if (XMaterial.getVersion() > 18) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    ItemMeta im = item.getItemMeta();

                    List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
                    int index = 0;
                    boolean hasReplaced = false;
                    for (String str : lore) {
                        if (str.contains(Config.getLoreConfig().lores.get("skull-uuid").replace("%uuid%", ""))) {
                            lore.set(index, Config.getLoreConfig().lores.get("skull-uuid").replace("%uuid%", uuid));
                            hasReplaced = true;
                            break;
                        }
                        index += 1;
                    }

                    if (!hasReplaced) {
                        lore.add(Config.getLoreConfig().lores.get("skull-uuid").replace("%uuid%", uuid));
                    }

                    im.setLore(lore);

                    item.setItemMeta(im);

                    PDCUtil.setString(item, PDCUtil.KEY_SKULL_UUID, uuid);

                    if (XMaterial.getVersion() > 18) {
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.setItemInHand(item);
                    }

                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "skull-uuid.successful", uuid);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "skull-uuid.usage");
                }
            } else if (args[0].equalsIgnoreCase("value") || args[0].equalsIgnoreCase("val") || args[0].equalsIgnoreCase("v")) {
                if (GUIShop.getINSTANCE().getMiscUtils().isMainHandNull(player)) {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "need-item");
                } else {
                    ItemStack item;

                    if (XMaterial.getVersion() > 18) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    String targetMaterial = item.getType().toString();
                    Value value = new Value(player, targetMaterial);
                    value.loadItems();
                }
            } else if (args[0].equalsIgnoreCase("permission") || args[0].equalsIgnoreCase("perm") || args[0].equalsIgnoreCase("p")) {
                if (args.length >= 2) {
                    String permission = args[1];

                    ItemStack item;

                    if (GUIShop.getINSTANCE().getMiscUtils().isMainHandNull(player)) {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "need-item");
                        return true;
                    }

                    if (XMaterial.getVersion() > 18) {
                        item = player.getEquipment().getItemInMainHand();
                    } else {
                        item = player.getItemInHand();
                    }

                    ItemMeta im = item.getItemMeta();

                    List<String> lore = im.getLore() != null ? im.getLore() : new ArrayList<>();
                    int index = 0;
                    boolean hasReplaced = false;
                    for (String str : lore) {
                        if (str.contains(Config.getLoreConfig().lores.get("permission").replace("%permission%", ""))) {
                            lore.set(index, Config.getLoreConfig().lores.get("permission").replace("%permission%", permission));
                            hasReplaced = true;
                            break;
                        }
                        index += 1;
                    }

                    if (!hasReplaced) {
                        lore.add(Config.getLoreConfig().lores.get("permission").replace("%permission%", permission));
                    }

                    im.setLore(lore);

                    item.setItemMeta(im);

                    PDCUtil.setString(item, PDCUtil.KEY_PERMISSION, permission);

                    if (XMaterial.getVersion() > 18) {
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        player.setItemInHand(item);
                    }

                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "permission.successful", permission);
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "permission.usage");
                }
            } else if (args[0].equalsIgnoreCase("toggleworth") || args[0].equalsIgnoreCase("tw")) {
                // Toggle worth display for the player (session-only)
                if (GUIShop.getINSTANCE().getWorthDisplayManager() == null || !GUIShop.getINSTANCE().getWorthDisplayManager().isRegistered()) {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "toggleworth.disabled");
                    return true;
                }
                
                boolean nowEnabled = GUIShop.getINSTANCE().getWorthDisplayManager().toggleWorthForPlayer(player);
                if (nowEnabled) {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "toggleworth.enabled");
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "toggleworth.disabled-player");
                }
                
                // Force refresh inventory to apply the change immediately
                player.updateInventory();
            } else if (args[0].equalsIgnoreCase("iteminfo") || args[0].equalsIgnoreCase("ii") || args[0].equalsIgnoreCase("info")) {
                // Display comprehensive item information
                printItemInfo(player);
            } else if (args[0].equalsIgnoreCase("eco") || args[0].equalsIgnoreCase("economy")) {
                // Economy management commands
                handleEcoCommand(commandSender, args);
            } else {
                PlayerListener.INSTANCE.printUsage(player);
            }
        } else {
            PlayerListener.INSTANCE.printUsage(commandSender);
        }
        return true;
    }
    
    /**
     * Handle /gs eco subcommands.
     */
    private void handleEcoCommand(CommandSender sender, String[] args) {
        EconomyManager ecoManager = EconomyManager.getInstance();
        
        // Check if internal economy is enabled
        if (ecoManager == null || !ecoManager.isAvailable()) {
            sender.sendMessage(ChatColor.RED + "Internal economy is not enabled. Set 'enabled: true' in economy.yml");
            return;
        }
        
        // /gs eco help or /gs eco
        if (args.length < 2) {
            sendEcoHelp(sender);
            return;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "give", "add" -> handleEcoGive(sender, args);
            case "take", "remove", "withdraw" -> handleEcoTake(sender, args);
            case "set" -> handleEcoSet(sender, args);
            case "balance", "bal", "check" -> handleEcoBalance(sender, args);
            case "reset" -> handleEcoReset(sender, args);
            default -> sendEcoHelp(sender);
        }
    }
    
    private void sendEcoHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== GUIShop Economy Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/gs eco give <player> <amount>" + ChatColor.GRAY + " - Give money to a player");
        sender.sendMessage(ChatColor.YELLOW + "/gs eco take <player> <amount>" + ChatColor.GRAY + " - Take money from a player");
        sender.sendMessage(ChatColor.YELLOW + "/gs eco set <player> <amount>" + ChatColor.GRAY + " - Set a player's balance");
        sender.sendMessage(ChatColor.YELLOW + "/gs eco balance <player>" + ChatColor.GRAY + " - Check a player's balance");
        sender.sendMessage(ChatColor.YELLOW + "/gs eco reset <player>" + ChatColor.GRAY + " - Reset a player's balance");
        sender.sendMessage(ChatColor.GRAY + "Amounts support abbreviations: 1k, 1.5M, 100B, etc.");
    }
    
    private void handleEcoGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /gs eco give <player> <amount>");
            return;
        }
        
        EconomyManager ecoManager = EconomyManager.getInstance();
        String playerName = args[2];
        String amountStr = args[3];
        
        UUID targetUUID = ecoManager.getUUIDByUsername(playerName);
        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found.");
            return;
        }
        
        // Ensure account exists
        if (!ecoManager.hasAccount(targetUUID)) {
            ecoManager.createAccount(targetUUID, playerName);
        }
        
        BigDecimal amount = ecoManager.parseAmount(amountStr);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
            return;
        }
        
        boolean success = ecoManager.deposit(targetUUID, amount);
        if (success) {
            BigDecimal newBalance = ecoManager.getBalance(targetUUID);
            sender.sendMessage(ChatColor.GREEN + "Gave " + ecoManager.format(amount) + " to " + playerName + ".");
            sender.sendMessage(ChatColor.GRAY + "New balance: " + ecoManager.format(newBalance));
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to give money to " + playerName + ".");
        }
    }
    
    private void handleEcoTake(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /gs eco take <player> <amount>");
            return;
        }
        
        EconomyManager ecoManager = EconomyManager.getInstance();
        String playerName = args[2];
        String amountStr = args[3];
        
        UUID targetUUID = ecoManager.getUUIDByUsername(playerName);
        if (targetUUID == null || !ecoManager.hasAccount(targetUUID)) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found or has no account.");
            return;
        }
        
        BigDecimal amount = ecoManager.parseAmount(amountStr);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
            return;
        }
        
        boolean success = ecoManager.withdraw(targetUUID, amount);
        if (success) {
            BigDecimal newBalance = ecoManager.getBalance(targetUUID);
            sender.sendMessage(ChatColor.GREEN + "Took " + ecoManager.format(amount) + " from " + playerName + ".");
            sender.sendMessage(ChatColor.GRAY + "New balance: " + ecoManager.format(newBalance));
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to take money from " + playerName + ". (Insufficient funds or limit reached)");
        }
    }
    
    private void handleEcoSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /gs eco set <player> <amount>");
            return;
        }
        
        EconomyManager ecoManager = EconomyManager.getInstance();
        String playerName = args[2];
        String amountStr = args[3];
        
        UUID targetUUID = ecoManager.getUUIDByUsername(playerName);
        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found.");
            return;
        }
        
        // Ensure account exists
        if (!ecoManager.hasAccount(targetUUID)) {
            ecoManager.createAccount(targetUUID, playerName);
        }
        
        BigDecimal amount = ecoManager.parseAmount(amountStr);
        if (amount == null) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + amountStr);
            return;
        }
        
        boolean success = ecoManager.setBalance(targetUUID, amount);
        if (success) {
            BigDecimal newBalance = ecoManager.getBalance(targetUUID);
            sender.sendMessage(ChatColor.GREEN + "Set " + playerName + "'s balance to " + ecoManager.format(newBalance) + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to set balance for " + playerName + ".");
        }
    }
    
    private void handleEcoBalance(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /gs eco balance <player>");
            return;
        }
        
        EconomyManager ecoManager = EconomyManager.getInstance();
        String playerName = args[2];
        
        UUID targetUUID = ecoManager.getUUIDByUsername(playerName);
        if (targetUUID == null || !ecoManager.hasAccount(targetUUID)) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found or has no account.");
            return;
        }
        
        BigDecimal balance = ecoManager.getBalance(targetUUID);
        sender.sendMessage(ChatColor.GOLD + playerName + "'s balance: " + ChatColor.GREEN + ecoManager.format(balance));
    }
    
    private void handleEcoReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /gs eco reset <player>");
            return;
        }
        
        EconomyManager ecoManager = EconomyManager.getInstance();
        EconomyConfig ecoConfig = EconomyConfig.getInstance();
        String playerName = args[2];
        
        UUID targetUUID = ecoManager.getUUIDByUsername(playerName);
        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + playerName + "' not found.");
            return;
        }
        
        // Ensure account exists
        if (!ecoManager.hasAccount(targetUUID)) {
            ecoManager.createAccount(targetUUID, playerName);
        }
        
        BigDecimal startingBalance = ecoConfig.getStartingBalance();
        boolean success = ecoManager.setBalance(targetUUID, startingBalance);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Reset " + playerName + "'s balance to " + ecoManager.format(startingBalance) + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to reset balance for " + playerName + ".");
        }
    }

    protected void editMenu(String number, Player player) {
        // Add to CREATOR before opening so the menu sets up editor handlers
        GUIShop.getCREATOR().add(player.getUniqueId());
        GUIShop.getINSTANCE().getLogUtil().debugLog("Added player " + player.getName() + " to creator mode");
        
        Menu menu = PlayerListener.INSTANCE.openMenuForEdit(player);
        if (menu.hasMultiplePages()) {
            try {
                int page = Integer.parseInt(number);
                if (!menu.GUI.goToPage(page)) {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit.invalid-page", menu.GUI.getPageCount());
                }
            } catch (NumberFormatException numberFormatException) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "edit.no-number");
            }
        }
    }
    
    /**
     * Opens the Transaction GUI editor.
     */
    protected void openTransactionEditor(Player player) {
        TransactionEditor editor = new TransactionEditor(player);
        editor.open();
    }

    /**
     * Prints comprehensive item information to the player in a nicely formatted way.
     * Includes basic info, lore, enchantments, PDC data, and other metadata.
     */
    private void printItemInfo(Player player) {
        if (GUIShop.getINSTANCE().getMiscUtils().isMainHandNull(player)) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "need-item");
            return;
        }

        ItemStack item;
        if (XMaterial.getVersion() > 18) {
            item = player.getEquipment().getItemInMainHand();
        } else {
            item = player.getItemInHand();
        }

        ItemMeta meta = item.getItemMeta();
        
        // Header
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "=========" + ChatColor.RESET + ChatColor.YELLOW + " Item Info " + ChatColor.GOLD + ChatColor.STRIKETHROUGH + "=========");
        player.sendMessage("");

        // ===== BASIC INFO =====
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Basic Information");
        sendCopyableLine(player, "Material", item.getType().toString());
        sendCopyableLine(player, "Amount", String.valueOf(item.getAmount()));
        
        if (meta != null && meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            sendCopyableLine(player, "Display Name", displayName);
            // Also show the raw name with color codes
            String rawName = displayName.replace(ChatColor.COLOR_CHAR, '&');
            sendCopyableLine(player, "Display Name (Raw)", rawName);
        }
        
        // Durability
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.hasDamage()) {
                int maxDurability = item.getType().getMaxDurability();
                int currentDamage = damageable.getDamage();
                int remaining = maxDurability - currentDamage;
                sendCopyableLine(player, "Durability", remaining + "/" + maxDurability);
            }
        }
        
        // Custom Model Data
        if (meta != null && meta.hasCustomModelData()) {
            sendCopyableLine(player, "Custom Model Data", String.valueOf(meta.getCustomModelData()));
        }

        // ===== LORE =====
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Lore");
            List<String> lore = meta.getLore();
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                String rawLine = line.replace(ChatColor.COLOR_CHAR, '&');
                sendCopyableLine(player, "Line " + i, rawLine);
            }
        }

        // ===== ENCHANTMENTS =====
        if (!item.getEnchantments().isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Enchantments");
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                sendCopyableLine(player, entry.getKey().getKey().getKey(), "Level " + entry.getValue());
            }
            // Show compact format
            StringBuilder compactEnchants = new StringBuilder();
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                if (compactEnchants.length() > 0) compactEnchants.append(" ");
                compactEnchants.append(entry.getKey().getKey().getKey().toUpperCase()).append(":").append(entry.getValue());
            }
            sendCopyableLine(player, "Compact Format", compactEnchants.toString());
        }

        // ===== ITEM FLAGS =====
        if (meta != null && !meta.getItemFlags().isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Item Flags");
            StringBuilder flags = new StringBuilder();
            for (ItemFlag flag : meta.getItemFlags()) {
                if (flags.length() > 0) flags.append(" ");
                flags.append(flag.name());
            }
            sendCopyableLine(player, "Flags", flags.toString());
        }

        // ===== GUISHOP PDC DATA =====
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "GUIShop PDC Data");
        
        boolean hasPdcData = false;
        
        // Check each known GUIShop PDC key
        String buyPrice = PDCUtil.getString(item, PDCUtil.KEY_BUY_PRICE);
        if (buyPrice != null) { sendCopyableLine(player, "Buy Price", buyPrice); hasPdcData = true; }
        
        Double buyPriceDbl = PDCUtil.getDouble(item, PDCUtil.KEY_BUY_PRICE);
        if (buyPriceDbl != null) { sendCopyableLine(player, "Buy Price", buyPriceDbl.toString()); hasPdcData = true; }
        
        String sellPrice = PDCUtil.getString(item, PDCUtil.KEY_SELL_PRICE);
        if (sellPrice != null) { sendCopyableLine(player, "Sell Price", sellPrice); hasPdcData = true; }
        
        Double sellPriceDbl = PDCUtil.getDouble(item, PDCUtil.KEY_SELL_PRICE);
        if (sellPriceDbl != null) { sendCopyableLine(player, "Sell Price", sellPriceDbl.toString()); hasPdcData = true; }
        
        String shopName = PDCUtil.getString(item, PDCUtil.KEY_SHOP_NAME);
        if (shopName != null) { sendCopyableLine(player, "Shop Name", shopName); hasPdcData = true; }
        
        String buyName = PDCUtil.getString(item, PDCUtil.KEY_BUY_NAME);
        if (buyName != null) { sendCopyableLine(player, "Buy Name", buyName); hasPdcData = true; }
        
        String itemType = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
        if (itemType != null) { sendCopyableLine(player, "Item Type", itemType); hasPdcData = true; }
        
        String targetShop = PDCUtil.getString(item, PDCUtil.KEY_TARGET_SHOP);
        if (targetShop != null) { sendCopyableLine(player, "Target Shop", targetShop); hasPdcData = true; }
        
        String mobType = PDCUtil.getString(item, PDCUtil.KEY_MOB_TYPE);
        if (mobType != null) { sendCopyableLine(player, "Mob Type", mobType); hasPdcData = true; }
        
        String spawnerMob = PDCUtil.getString(item, PDCUtil.KEY_SPAWNER_MOB);
        if (spawnerMob != null) { sendCopyableLine(player, "Spawner Mob", spawnerMob); hasPdcData = true; }
        
        String commands = PDCUtil.getString(item, PDCUtil.KEY_COMMANDS);
        if (commands != null) { 
            String[] cmdArray = commands.split("::");
            for (int i = 0; i < cmdArray.length; i++) {
                sendCopyableLine(player, "Command " + i, cmdArray[i]);
            }
            hasPdcData = true; 
        }
        
        String enchantments = PDCUtil.getString(item, PDCUtil.KEY_ENCHANTMENTS);
        if (enchantments != null) { sendCopyableLine(player, "Enchantments (PDC)", enchantments); hasPdcData = true; }
        
        String customNbt = PDCUtil.getString(item, PDCUtil.KEY_CUSTOM_NBT);
        if (customNbt != null) { sendCopyableLine(player, "Custom NBT", customNbt); hasPdcData = true; }
        
        Integer quantity = PDCUtil.getInteger(item, PDCUtil.KEY_QUANTITY);
        if (quantity != null) { sendCopyableLine(player, "Quantity", quantity.toString()); hasPdcData = true; }
        
        String skullUuid = PDCUtil.getString(item, PDCUtil.KEY_SKULL_UUID);
        if (skullUuid != null) { sendCopyableLine(player, "Skull UUID", skullUuid); hasPdcData = true; }
        
        String permission = PDCUtil.getString(item, PDCUtil.KEY_PERMISSION);
        if (permission != null) { sendCopyableLine(player, "Permission", permission); hasPdcData = true; }
        
        String potionInfo = PDCUtil.getString(item, PDCUtil.KEY_POTION);
        if (potionInfo != null) { sendCopyableLine(player, "Potion Info", potionInfo); hasPdcData = true; }
        
        String shopLore = PDCUtil.getString(item, PDCUtil.KEY_SHOP_LORE_LINES);
        if (shopLore != null) { sendCopyableLine(player, "Shop Lore Lines", shopLore); hasPdcData = true; }
        
        String buyLore = PDCUtil.getString(item, PDCUtil.KEY_BUY_LORE_LINES);
        if (buyLore != null) { sendCopyableLine(player, "Buy Lore Lines", buyLore); hasPdcData = true; }
        
        if (!hasPdcData) {
            player.sendMessage(ChatColor.GRAY + "  No GUIShop data found on this item.");
        }

        // ===== ALL PDC KEYS (from any plugin) =====
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Set<NamespacedKey> keys = pdc.getKeys();
            if (!keys.isEmpty()) {
                player.sendMessage("");
                player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "All PDC Keys");
                for (NamespacedKey key : keys) {
                    String value = getPdcValueAsString(pdc, key);
                    sendCopyableLine(player, key.toString(), value);
                }
            }
        }

        // ===== NBT DATA =====
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "NBT Data");
        
        String nbtString = getNbtAsString(item);
        if (nbtString != null && !nbtString.isEmpty()) {
            // If NBT is very long, show a truncated version in chat but full in copy
            if (nbtString.length() > 200) {
                String truncated = nbtString.substring(0, 200) + "...";
                sendCopyableNbtLine(player, "Full NBT (truncated)", truncated, nbtString);
            } else {
                sendCopyableLine(player, "Full NBT", nbtString);
            }
            
            // Try to extract and display key NBT components nicely
            displayNbtComponents(player, item, nbtString);
        } else {
            player.sendMessage(ChatColor.GRAY + "  No custom NBT data (vanilla item).");
        }

        // Footer
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "================================");
        player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Click on values to copy them to clipboard.");
        player.sendMessage("");
    }

    /**
     * Sends a line with a label and copyable value.
     */
    private void sendCopyableLine(Player player, String label, String value) {
        TextComponent labelComponent = new TextComponent(ChatColor.GRAY + "  " + label + ": ");
        TextComponent valueComponent = new TextComponent(ChatColor.WHITE + value);
        
        // Add click event to copy value
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value));
        
        // Add hover text
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new Text(ChatColor.YELLOW + "Click to copy: " + ChatColor.WHITE + value)));
        
        player.spigot().sendMessage(labelComponent, valueComponent);
    }

    /**
     * Attempts to get a PDC value as a string, trying different data types.
     */
    private String getPdcValueAsString(PersistentDataContainer pdc, NamespacedKey key) {
        try {
            if (pdc.has(key, PersistentDataType.STRING)) {
                return pdc.get(key, PersistentDataType.STRING);
            } else if (pdc.has(key, PersistentDataType.INTEGER)) {
                Integer val = pdc.get(key, PersistentDataType.INTEGER);
                return val != null ? val.toString() : "null";
            } else if (pdc.has(key, PersistentDataType.DOUBLE)) {
                Double val = pdc.get(key, PersistentDataType.DOUBLE);
                return val != null ? val.toString() : "null";
            } else if (pdc.has(key, PersistentDataType.LONG)) {
                Long val = pdc.get(key, PersistentDataType.LONG);
                return val != null ? val.toString() : "null";
            } else if (pdc.has(key, PersistentDataType.BYTE)) {
                Byte val = pdc.get(key, PersistentDataType.BYTE);
                return val != null ? val.toString() : "null";
            } else if (pdc.has(key, PersistentDataType.FLOAT)) {
                Float val = pdc.get(key, PersistentDataType.FLOAT);
                return val != null ? val.toString() : "null";
            } else if (pdc.has(key, PersistentDataType.SHORT)) {
                Short val = pdc.get(key, PersistentDataType.SHORT);
                return val != null ? val.toString() : "null";
            } else if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
                byte[] val = pdc.get(key, PersistentDataType.BYTE_ARRAY);
                return val != null ? "[byte array, length=" + val.length + "]" : "null";
            } else if (pdc.has(key, PersistentDataType.INTEGER_ARRAY)) {
                int[] val = pdc.get(key, PersistentDataType.INTEGER_ARRAY);
                return val != null ? "[int array, length=" + val.length + "]" : "null";
            } else if (pdc.has(key, PersistentDataType.LONG_ARRAY)) {
                long[] val = pdc.get(key, PersistentDataType.LONG_ARRAY);
                return val != null ? "[long array, length=" + val.length + "]" : "null";
            }
        } catch (Exception e) {
            return "[error reading value]";
        }
        return "[unknown type]";
    }

    /**
     * Sends a line with a label, truncated display, but copies the full value.
     */
    private void sendCopyableNbtLine(Player player, String label, String displayValue, String fullValue) {
        TextComponent labelComponent = new TextComponent(ChatColor.GRAY + "  " + label + ": ");
        TextComponent valueComponent = new TextComponent(ChatColor.WHITE + displayValue);
        
        // Add click event to copy FULL value
        valueComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, fullValue));
        
        // Add hover text showing it will copy full value
        valueComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new Text(ChatColor.YELLOW + "Click to copy full NBT (" + fullValue.length() + " chars)")));
        
        player.spigot().sendMessage(labelComponent, valueComponent);
    }

    /**
     * Gets the NBT data of an ItemStack as a string using reflection.
     * Works across different Minecraft versions by trying multiple approaches.
     */
    private String getNbtAsString(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        // Try multiple methods to get NBT string
        String nbt = null;

        // Method 1: Try Paper's ItemMeta.getAsString() (Paper 1.18.2+)
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                java.lang.reflect.Method getAsString = meta.getClass().getMethod("getAsString");
                Object result = getAsString.invoke(meta);
                if (result != null) {
                    nbt = result.toString();
                    if (!nbt.equals("{}")) {
                        return nbt;
                    }
                }
            }
        } catch (Exception ignored) {
            // Not Paper or method not available
        }

        // Method 2: Try CraftItemStack -> NMS ItemStack -> getTag/getComponents
        try {
            // Get CraftItemStack class
            String version = getServerVersion();
            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
            
            // Convert to NMS ItemStack
            java.lang.reflect.Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            Object nmsItem = asNMSCopy.invoke(null, item);
            
            if (nmsItem != null) {
                // Try different methods based on version
                // 1.20.5+ uses components, older uses NBT tags
                nbt = tryGetNbtFromNmsItem(nmsItem);
                if (nbt != null && !nbt.isEmpty() && !nbt.equals("{}")) {
                    return nbt;
                }
            }
        } catch (Exception e) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("NBT extraction method 2 failed: " + e.getMessage());
        }

        // Method 3: Try using Bukkit's serialization as a fallback
        try {
            Map<String, Object> serialized = item.serialize();
            // Remove basic fields to show only interesting data
            serialized.remove("type");
            serialized.remove("amount");
            if (!serialized.isEmpty()) {
                return serialized.toString();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Try to extract NBT string from NMS ItemStack using various methods.
     */
    private String tryGetNbtFromNmsItem(Object nmsItem) {
        // Try getTag() for 1.20.4 and below
        try {
            java.lang.reflect.Method getTag = nmsItem.getClass().getMethod("getTag");
            Object tag = getTag.invoke(nmsItem);
            if (tag != null) {
                return tag.toString();
            }
        } catch (Exception ignored) {
        }

        // Try u() or similar obfuscated method names (varies by version)
        for (String methodName : new String[]{"u", "v", "w", "getOrCreateTag", "save"}) {
            try {
                java.lang.reflect.Method method = nmsItem.getClass().getMethod(methodName);
                Object result = method.invoke(nmsItem);
                if (result != null) {
                    String str = result.toString();
                    if (str.contains("{") && str.contains("}")) {
                        return str;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Try getComponents() for 1.20.5+
        try {
            java.lang.reflect.Method getComponents = nmsItem.getClass().getMethod("getComponents");
            Object components = getComponents.invoke(nmsItem);
            if (components != null) {
                return components.toString();
            }
        } catch (Exception ignored) {
        }

        // Try a() method (common obfuscated name)
        try {
            java.lang.reflect.Method[] methods = nmsItem.getClass().getMethods();
            for (java.lang.reflect.Method m : methods) {
                if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().contains("Tag")) {
                    Object result = m.invoke(nmsItem);
                    if (result != null) {
                        return result.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the server version string (e.g., "v1_21_R1").
     */
    private String getServerVersion() {
        String packageName = org.bukkit.Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    /**
     * Displays extracted NBT components in a more readable format,
     * with config-ready YAML snippets for easy copying.
     */
    private void displayNbtComponents(Player player, ItemStack item, String nbtString) {
        // Extract common NBT components and display them nicely
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Unbreakable
        if (meta.isUnbreakable()) {
            sendCopyableLine(player, "Unbreakable", "true");
        }

        // Attribute Modifiers
        if (meta.hasAttributeModifiers() && meta.getAttributeModifiers() != null) {
            player.sendMessage(ChatColor.GRAY + "  Attribute Modifiers:");
            meta.getAttributeModifiers().forEach((attr, mod) -> {
                sendCopyableLine(player, "    " + attr.getKey().getKey(), mod.getAmount() + " " + mod.getOperation().name());
            });
        }

        // ===== ENCHANTMENTS (Config-Ready Format) =====
        if (!item.getEnchantments().isEmpty()) {
            displayEnchantmentsConfigFormat(player, item.getEnchantments(), "enchantments");
        }

        // ===== POTION (Config-Ready Format) =====
        if (meta instanceof org.bukkit.inventory.meta.PotionMeta) {
            displayPotionConfigFormat(player, (org.bukkit.inventory.meta.PotionMeta) meta);
        }

        // ===== SKULL =====
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
            if (skullMeta.getOwningPlayer() != null) {
                sendCopyableLine(player, "Skull Owner", skullMeta.getOwningPlayer().getName());
                if (skullMeta.getOwningPlayer().getUniqueId() != null) {
                    String uuid = skullMeta.getOwningPlayer().getUniqueId().toString();
                    sendCopyableLine(player, "skull-uuid (config)", uuid);
                }
            }
        }

        // ===== FIREWORK (Config-Ready Format) =====
        if (meta instanceof org.bukkit.inventory.meta.FireworkMeta) {
            displayFireworkConfigFormat(player, (org.bukkit.inventory.meta.FireworkMeta) meta);
        }

        // ===== ENCHANTED BOOK (Config-Ready Format) =====
        if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta = 
                (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
            if (bookMeta.hasStoredEnchants()) {
                displayEnchantmentsConfigFormat(player, bookMeta.getStoredEnchants(), "enchantments (book)");
            }
        }

        // ===== SPAWNER =====
        if (meta instanceof org.bukkit.inventory.meta.BlockStateMeta) {
            org.bukkit.inventory.meta.BlockStateMeta blockMeta = 
                (org.bukkit.inventory.meta.BlockStateMeta) meta;
            if (blockMeta.hasBlockState()) {
                org.bukkit.block.BlockState state = blockMeta.getBlockState();
                if (state instanceof org.bukkit.block.CreatureSpawner) {
                    org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) state;
                    if (spawner.getSpawnedType() != null) {
                        String mobType = spawner.getSpawnedType().name();
                        sendCopyableLine(player, "mob-type (config)", mobType);
                    }
                }
            }
        }
    }

    /**
     * Displays enchantments in GUIShop config-ready format.
     * Format: "ENCHANT:LEVEL ENCHANT2:LEVEL2"
     */
    private void displayEnchantmentsConfigFormat(Player player, Map<Enchantment, Integer> enchants, String label) {
        if (enchants.isEmpty()) return;
        
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Config-Ready: Enchantments");
        
        // Build compact format: "SHARP:5 FIRE_ASPECT:2"
        StringBuilder compact = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (compact.length() > 0) compact.append(" ");
            String enchantName = entry.getKey().getKey().getKey().toUpperCase();
            compact.append(enchantName).append(":").append(entry.getValue());
        }
        
        String configLine = "enchantments: '" + compact.toString() + "'";
        sendCopyableLine(player, label, compact.toString());
        
        // Show the full YAML line
        player.sendMessage(ChatColor.DARK_GRAY + "  Copy for config:");
        sendCopyableLine(player, "  YAML", configLine);
    }

    /**
     * Displays potion info in GUIShop config-ready YAML format.
     */
    private void displayPotionConfigFormat(Player player, org.bukkit.inventory.meta.PotionMeta potionMeta) {
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Config-Ready: potion-info");
        
        String potionType = "WATER";
        boolean splash = false;
        boolean extended = false;
        boolean upgraded = false;
        
        // Get base potion type
        if (potionMeta.getBasePotionType() != null) {
            org.bukkit.potion.PotionType baseType = potionMeta.getBasePotionType();
            potionType = baseType.name();
            
            // Check if extended or upgraded based on potion type name
            String typeName = baseType.name();
            if (typeName.contains("LONG") || typeName.contains("EXTENDED")) {
                extended = true;
                potionType = typeName.replace("LONG_", "").replace("_LONG", "");
            }
            if (typeName.contains("STRONG") || typeName.contains("II")) {
                upgraded = true;
                potionType = typeName.replace("STRONG_", "").replace("_STRONG", "").replace("_II", "");
            }
        }
        
        // Display individual values
        sendCopyableLine(player, "type", potionType);
        sendCopyableLine(player, "splash", String.valueOf(splash));
        sendCopyableLine(player, "extended", String.valueOf(extended));
        sendCopyableLine(player, "upgraded", String.valueOf(upgraded));
        
        // Build full YAML block
        StringBuilder yaml = new StringBuilder();
        yaml.append("potion-info:\\n");
        yaml.append("  type: ").append(potionType).append("\\n");
        yaml.append("  splash: ").append(splash).append("\\n");
        yaml.append("  extended: ").append(extended).append("\\n");
        yaml.append("  upgraded: ").append(upgraded);
        
        player.sendMessage(ChatColor.DARK_GRAY + "  Copy for config:");
        sendCopyableMultiLine(player, "potion-info", yaml.toString());
        
        // Show custom effects if any
        if (potionMeta.hasCustomEffects()) {
            player.sendMessage(ChatColor.GRAY + "  Custom Effects:");
            for (org.bukkit.potion.PotionEffect effect : potionMeta.getCustomEffects()) {
                String effectStr = effect.getType().getKey().getKey().toUpperCase() + " Lvl " + 
                    (effect.getAmplifier() + 1) + " (" + (effect.getDuration() / 20) + "s)";
                sendCopyableLine(player, "    Effect", effectStr);
            }
        }
    }

    /**
     * Displays firework info in GUIShop config-ready YAML format.
     */
    private void displayFireworkConfigFormat(Player player, org.bukkit.inventory.meta.FireworkMeta fwMeta) {
        player.sendMessage("");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Config-Ready: firework-info");
        
        int flight = fwMeta.getPower();
        sendCopyableLine(player, "flight", String.valueOf(flight));
        
        // Build YAML for each explosion
        StringBuilder yaml = new StringBuilder();
        yaml.append("firework-info:\\n");
        yaml.append("  flight: ").append(flight).append("\\n");
        yaml.append("  explosions:");
        
        if (fwMeta.hasEffects()) {
            for (org.bukkit.FireworkEffect effect : fwMeta.getEffects()) {
                yaml.append("\\n    - shape: ").append(getFireworkShapeName(effect.getType()));
                yaml.append("\\n      flicker: ").append(effect.hasFlicker());
                yaml.append("\\n      trail: ").append(effect.hasTrail());
                
                // Colors as RGB integers
                if (!effect.getColors().isEmpty()) {
                    yaml.append("\\n      colors: [");
                    boolean first = true;
                    for (org.bukkit.Color color : effect.getColors()) {
                        if (!first) yaml.append(", ");
                        yaml.append(color.asRGB());
                        first = false;
                    }
                    yaml.append("]");
                }
                
                // Fade colors
                if (!effect.getFadeColors().isEmpty()) {
                    yaml.append("\\n      fade-colors: [");
                    boolean first = true;
                    for (org.bukkit.Color color : effect.getFadeColors()) {
                        if (!first) yaml.append(", ");
                        yaml.append(color.asRGB());
                        first = false;
                    }
                    yaml.append("]");
                }
                
                // Also display each explosion in chat
                player.sendMessage(ChatColor.GRAY + "  Explosion:");
                sendCopyableLine(player, "    shape", getFireworkShapeName(effect.getType()));
                sendCopyableLine(player, "    flicker", String.valueOf(effect.hasFlicker()));
                sendCopyableLine(player, "    trail", String.valueOf(effect.hasTrail()));
                
                if (!effect.getColors().isEmpty()) {
                    StringBuilder colorsStr = new StringBuilder("[");
                    boolean first = true;
                    for (org.bukkit.Color color : effect.getColors()) {
                        if (!first) colorsStr.append(", ");
                        colorsStr.append(color.asRGB());
                        first = false;
                    }
                    colorsStr.append("]");
                    sendCopyableLine(player, "    colors", colorsStr.toString());
                }
                
                if (!effect.getFadeColors().isEmpty()) {
                    StringBuilder fadeStr = new StringBuilder("[");
                    boolean first = true;
                    for (org.bukkit.Color color : effect.getFadeColors()) {
                        if (!first) fadeStr.append(", ");
                        fadeStr.append(color.asRGB());
                        first = false;
                    }
                    fadeStr.append("]");
                    sendCopyableLine(player, "    fade-colors", fadeStr.toString());
                }
            }
        } else {
            yaml.append(" []");
        }
        
        player.sendMessage(ChatColor.DARK_GRAY + "  Copy for config:");
        sendCopyableMultiLine(player, "firework-info", yaml.toString());
    }

    /**
     * Converts FireworkEffect.Type to the config shape name.
     */
    private String getFireworkShapeName(org.bukkit.FireworkEffect.Type type) {
        switch (type) {
            case BALL: return "ball";
            case BALL_LARGE: return "ball_large";
            case STAR: return "star";
            case BURST: return "burst";
            case CREEPER: return "creeper";
            default: return type.name().toLowerCase();
        }
    }

    /**
     * Sends a multiline YAML block that can be copied.
     */
    private void sendCopyableMultiLine(Player player, String label, String yamlContent) {
        // Replace \\n with actual newlines for the copy
        String copyValue = yamlContent.replace("\\n", "\n");
        
        TextComponent labelComponent = new TextComponent(ChatColor.GRAY + "  [" + ChatColor.GREEN + "Click to copy " + label + ChatColor.GRAY + "]");
        
        labelComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyValue));
        labelComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new Text(ChatColor.YELLOW + "Click to copy full YAML block:\n" + ChatColor.WHITE + copyValue)));
        
        player.spigot().sendMessage(labelComponent);
    }
}
