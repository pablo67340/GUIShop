package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tab completer for the /guishop (/gs) command.
 */
public class GuishopTabCompleter implements TabCompleter {

    private static final List<String> BASE_COMMANDS = Arrays.asList(
        "reload", "edit", "parsemob", "toggleworth", "iteminfo", "eco"
    );
    
    private static final List<String> EDIT_TARGETS = Arrays.asList(
        "menu", "transaction"
    );
    
    private static final List<String> ECO_SUBCOMMANDS = Arrays.asList(
        "give", "take", "set", "balance", "reset"
    );
    
    private static final List<String> AMOUNT_SUGGESTIONS = Arrays.asList(
        "100", "500", "1k", "5k", "10k", "50k", "100k", "500k", "1m", "10m", "100m"
    );

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                       @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - base commands
            String partial = args[0].toLowerCase();
            for (String cmd : BASE_COMMANDS) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            
            if (subCommand.equals("edit") || subCommand.equals("e")) {
                // /gs edit <target>
                // Add special targets
                for (String target : EDIT_TARGETS) {
                    if (target.startsWith(partial)) {
                        completions.add(target);
                    }
                }
                // Add shop names from ConfigManager
                java.util.Set<String> shopNames = GUIShop.getINSTANCE().getConfigManager().getShopNames();
                if (shopNames != null) {
                    for (String shop : shopNames) {
                        if (shop.toLowerCase().startsWith(partial)) {
                            completions.add(shop);
                        }
                    }
                }
            } else if (subCommand.equals("eco") || subCommand.equals("economy")) {
                // /gs eco <subcommand>
                for (String ecoCmd : ECO_SUBCOMMANDS) {
                    if (ecoCmd.startsWith(partial)) {
                        completions.add(ecoCmd);
                    }
                }
            } else if (subCommand.equals("parsemob")) {
                // /gs parsemob <entity>
                for (org.bukkit.entity.EntityType type : org.bukkit.entity.EntityType.values()) {
                    if (type.isAlive() && type.name().toLowerCase().startsWith(partial)) {
                        completions.add(type.name());
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();
            String partial = args[2].toLowerCase();
            
            if (subCommand.equals("edit") || subCommand.equals("e")) {
                // /gs edit <shop> <page>
                // Suggest page numbers
                for (int i = 0; i <= 10; i++) {
                    String pageNum = String.valueOf(i);
                    if (pageNum.startsWith(partial)) {
                        completions.add(pageNum);
                    }
                }
            } else if (subCommand.equals("eco") || subCommand.equals("economy")) {
                // /gs eco <subcommand> <player>
                if (ECO_SUBCOMMANDS.contains(subSubCommand)) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(partial)) {
                            completions.add(player.getName());
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String subSubCommand = args[1].toLowerCase();
            
            if (subCommand.equals("eco") || subCommand.equals("economy")) {
                // /gs eco give/take/set <player> <amount>
                if (subSubCommand.equals("give") || subSubCommand.equals("take") || subSubCommand.equals("set")) {
                    String partial = args[3].toLowerCase();
                    for (String amount : AMOUNT_SUGGESTIONS) {
                        if (amount.startsWith(partial)) {
                            completions.add(amount);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}
