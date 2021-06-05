package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.listenable.Value;
import com.pablo67340.guishop.util.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ValueCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command cmd, String label, String[] args) {
        if (GUIShop.isNoEconomySystem()) {
            GUIShop.sendMessage(commandSender, "&4The plugin didn't detect an economy system! \n" +
                    "&7Please contact a server administrator or setup an economy system.");
            return true;
        }

        if (!(commandSender instanceof Player)) {
            GUIShop.sendMessage(commandSender, "&4You can only run this command as a player!");
            return true;
        }

        Player player = (Player) commandSender;
        if (cmd.getName().equalsIgnoreCase("value")) {
            if (player.hasPermission("guishop.value")) {
                if (player.getEquipment().getItemInMainHand() != null) {
                    ItemStack target = player.getEquipment().getItemInMainHand();
                    String targetMaterial = target.getType().toString();
                    Value value = new Value(player, targetMaterial);
                    value.loadItems();
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou must be holding an item."));
                }
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', Config.getNoPermission()));
            }
            return true;
        }
        return false;
    }
}
