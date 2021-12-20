package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
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
            GUIShop.sendPrefix(commandSender, "no-economy-system");
            return true;
        }

        if (!(commandSender instanceof Player)) {
            GUIShop.sendPrefix(commandSender, "only-player");
            return true;
        }

        GUIShop.getINSTANCE().getUserCommands().sellCommand((Player) commandSender);

        return true;
    }
}
