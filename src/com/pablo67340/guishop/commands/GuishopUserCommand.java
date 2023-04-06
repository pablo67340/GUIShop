package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.ConfigUtil;
import net.md_5.bungee.api.ChatColor;

public class GuishopUserCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("&cPlayers only.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1) {
            if (ConfigUtil.getDisabledWorlds().contains(player.getWorld().getName())) {
                if (GUIShop.BUY_COMMANDS.contains(args[0].toLowerCase())) {

                    buyCommand(player, (args.length >= 2) ? args[1] : null);
                    return true;

                } else if (GUIShop.SELL_COMMANDS.contains(args[0].toLowerCase())) {

                    sellCommand(player);
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cShop cannot be used in this world"));
                return false;
            }
        }
        player.sendMessage("&cUnknown command.");
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
        if (GUIShop.getPerms().playerHas(player, "guishop.use") || player.isOp()) {

            if (shop == null) {
                PlayerListener.INSTANCE.openMenu(player);

            } else {

                if (GUIShop.getPerms().playerHas(player, "guishop.shop." + shop.toLowerCase())
                        || GUIShop.getPerms().playerHas(player, "guishop.shop.*") || player.isOp()) {
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
        if (GUIShop.getPerms().playerHas(player, "guishop.sell") || player.isOp()) {
            new Sell().open(player);
        } else {
            player.sendMessage(ConfigUtil.getNoPermission());
        }
    }

}
