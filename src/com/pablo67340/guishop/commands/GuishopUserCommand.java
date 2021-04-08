package com.pablo67340.guishop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.ConfigUtil;

public class GuishopUserCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Main.sendMessage(sender, "&cPlayers only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1) {
            if (Main.BUY_COMMANDS.contains(args[0].toLowerCase())) {

                buyCommand(player, (args.length >= 2) ? args[1] : null);
                return true;

            } else if (Main.SELL_COMMANDS.contains(args[0].toLowerCase())) {

                sellCommand(player);
                return true;
            }
        }
        Main.sendMessage(player, "&cUnknown command.");
        return true;
    }

    /**
     * Whether a player uses a buy command. <br>
     * Includes permission checks
     *
     * @param player the player
     * @param shop the command argument for a specific shop, can be null
     */
    void buyCommand(Player player, String shop) {
        if (player.hasPermission("guishop.use") || player.isOp()) {

            if (shop == null) {
                PlayerListener.INSTANCE.openShop(player);

            } else{

                if (player.hasPermission("guishop.shop." + shop.toLowerCase())
                        || player.hasPermission("guishop.shop.*") || player.isOp()) {
                    new Menu(player).openShop(player, shop);
                } else {
                    player.sendMessage(ConfigUtil.getNoPermission());
                }
            }


        } else {
            player.sendMessage(ConfigUtil.getNoPermission());
        }
    }

    /**
     * When a player uses a sell command. <br>
     * Includes permission checks
     *
     * @param player the player
     */
    void sellCommand(Player player) {
        if (player.hasPermission("guishop.sell") || player.isOp()) {
            new Sell().open(player);
        } else {
            player.sendMessage(ConfigUtil.getNoPermission());
        }
    }

}
