package com.pablo67340.guishop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.config.Config;

public class UserCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (GUIShop.isNoEconomySystem()) {
            GUIShop.sendPrefix(commandSender, "no-economy-system");
            return true;
        }

        if (!(commandSender instanceof Player)) {
            GUIShop.sendPrefix(commandSender, "only-player");
            return true;
        }
        
        Player player = (Player) commandSender;

        if (args.length >= 1) {
            if (Config.getDisabledWorlds().contains(player.getWorld().getName())) {
                if (GUIShop.BUY_COMMANDS.contains(args[0].toLowerCase())) {
                    if (Config.isSignsOnly()) {
                        GUIShop.sendPrefix(commandSender, "signs-only", Config.getTitlesConfig().getSignTitle());
                        return true;
                    }

                    buyCommand(player, (args.length >= 2) ? args[1] : null);
                    return true;
                } else if (GUIShop.SELL_COMMANDS.contains(args[0].toLowerCase())) {
                    sellCommand(player);
                    return true;
                }
            } else {
                GUIShop.sendPrefix(player, "disabled-world");
                return false;
            }
        }
        GUIShop.sendPrefix(commandSender, "unknown-command");
        return true;
    }

    /**
     * Whether a player uses a buy command. <br>
     * Includes permission checks
     *
     * @param player the player
     * @param shop the command argument for a specific shop, can be null
     */
    public void buyCommand(Player player, String shop) {
        if (player.hasPermission("guishop.use") || player.isOp()) {
            if (shop == null) {
                PlayerListener.INSTANCE.openMenu(player);
            } else {
                if (player.hasPermission("guishop.shop." + shop.toLowerCase())
                        || player.hasPermission("guishop.shop.*") || player.isOp()) {
                    new Menu(player).openShop(player, shop);
                } else {
                    GUIShop.sendPrefix(player, "no-permission");
                }
            }

        } else {
            GUIShop.sendPrefix(player, "no-permission");
        }
    }

    /**
     * When a player uses a sell command. <br>
     * Includes permission checks
     *
     * @param player the player
     */
    public void sellCommand(Player player) {
        if (player.hasPermission("guishop.sell") || player.isOp()) {
            new Sell().open(player);
        } else {
            GUIShop.sendPrefix(player, "no-permission");
        }
    }
}
