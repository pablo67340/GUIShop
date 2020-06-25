package com.pablo67340.guishop.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.Config;

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

    		} else if (Main.getINSTANCE().getShops().containsKey(shop.toLowerCase())) {

    			ShopDef shopDef = Main.getINSTANCE().getShops().get(shop.toLowerCase());

    			if (player.hasPermission("guishop.shop." + shopDef.getShop())
    					|| player.hasPermission("guishop.shop.*") || player.isOp()) {
    				new Menu().openShop(player, shopDef);
    			} else {
    				player.sendMessage(Config.getNoPermission());
    			}

    		} else {
    			Main.sendMessage(player, "&cShop not found");
    		}

    	} else {
    		player.sendMessage(Config.getNoPermission());
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
    		player.sendMessage(Config.getNoPermission());
    	}
	}

}
