package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
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
        Player player = (Player) commandSender;

        GUIShop.getINSTANCE().getUserCommands().buyCommand(player, (args.length >= 1) ? args[0] : null);

        return true;
    }
}
