package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.util.Config;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class BuyCommand extends BukkitCommand {
    public BuyCommand(ArrayList<String> aliases) {
        super(aliases.remove(0));
        this.description = "Opens the shops menu.";
        this.setPermission("guishop.use");
        this.setAliases(aliases);
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) commandSender;

        if (args.length == 0) {
            if (player.hasPermission("guishop.use")) {
                PlayerListener.INSTANCE.openShop(player);
            } else {
                player.sendMessage(Config.getNoPermission());
            }
        } else {
            if (Main.getINSTANCE().getShops().containsKey(args[0].toLowerCase())) {

                ShopDef shopDef = Main.getINSTANCE().getShops().get(args[0].toLowerCase());

				if (player.hasPermission("guishop.shop." + shopDef.getShop())
						|| player.hasPermission("guishop.shop.*")) {
                    new Menu().openShop(player, shopDef);
                } else {
                    player.sendMessage(Config.getNoPermission());
                }

            } else {
                Main.sendMessage(player, "&cShop not found");
            }
        }

        return true;
    }
}
