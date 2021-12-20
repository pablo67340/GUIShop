package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class BuyCommand extends BukkitCommand {

    public BuyCommand(ArrayList<String> aliases) {
        super(aliases.remove(0));
        this.description = "Opens the shops menu.";
        this.setPermission("guishop.use");
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

        if (Config.isSignsOnly()) {
            GUIShop.sendPrefix(commandSender, "signs-only", Config.getTitlesConfig().getSignTitle());
            return true;
        }

        Player player = (Player) commandSender;

        GUIShop.getINSTANCE().getUserCommands().buyCommand(player, (args.length >= 1) ? args[0] : null);

        return true;
    }
}
