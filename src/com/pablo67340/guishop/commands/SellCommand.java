package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.Config;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SellCommand extends BukkitCommand {
    public SellCommand(ArrayList<String> aliases) {
        super(aliases.remove(0));
        this.description = "Opens the sell menu.";
        this.setPermission("guishop.sell");
        this.setAliases(aliases);
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) commandSender;

        if (player.hasPermission("guishop.sell") || player.isOp()) {
            new Sell().open(player);
        } else {
            player.sendMessage(Config.getNoPermission());
        }

        return true;
    }
}
