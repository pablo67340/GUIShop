package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.util.Config;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import java.util.ArrayList;

public class SellCommand extends BukkitCommand {

    public SellCommand(ArrayList<String> aliases) {
        super(aliases.remove(0));
        this.description = "Opens the sell menu.";
        this.setPermission("guishop.sell");
        this.setAliases(aliases);
    }

    @Override
    public boolean execute(CommandSender commandSender, String label, String[] args) {
        if (GUIShop.isNoEconomySystem()) {
            GUIShop.sendMessage(commandSender, Config.getPrefix() + " " + "&4The plugin didn't detect an economy system! \n" +
                    "&7Please contact a server administrator or setup an economy system.");
            return true;
        }

        if (!(commandSender instanceof Player)) {
            GUIShop.sendMessage(commandSender, Config.getPrefix() + " " + "&4You can only run this command as a player!");
            return true;
        }

        Player player = (Player) commandSender;

        GUIShop.getINSTANCE().getUserCommands().sellCommand(player);

        return true;
    }
}
