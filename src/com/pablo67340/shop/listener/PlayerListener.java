package com.pablo67340.shop.listener;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;

import com.pablo67340.shop.handler.*;
import com.pablo67340.shop.main.Main;

public final class PlayerListener implements Listener {

	/**
	 * An instance of a {@link PlayerListener} that will be used to handle this
	 * specific object reference from other classes, even though methods here will
	 * be static.
	 */
	public static final PlayerListener INSTANCE = new PlayerListener();

	public void openShop(Player player) {
		Menu menu = new Menu(player.getName());
		menu.open();
	}

	/**
	 * Handle any commands sent into the chat, splice and compare to set GUIShop
	 * commands in config.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();

		if (!e.isCancelled()) {

			if (!Main.getInstance().getDebugger().hasExploded()) {

				String command = e.getMessage().substring(1);
				String[] cut = command.split(" ");

				if (Main.BUY_COMMANDS.contains(command)) {
					if (player.hasPermission("guishop.use") || player.isOp()) {
						openShop(player);
						e.setCancelled(true);
						return;
					} else {
						player.sendMessage(Utils.getNoPermission());
						e.setCancelled(true);
						return;
					}
				}

				if (Main.SELL_COMMANDS.contains(command)) {
					if (player.hasPermission("guishop.sell") || player.isOp()) {
						e.setCancelled(true);
						@SuppressWarnings("unused")
						Sell sell = new Sell(player);

						return;
					} else {
						player.sendMessage(Utils.getNoPermission());
						e.setCancelled(true);
						return;
					}
				}

				if (cut[0].equalsIgnoreCase("guishop") || cut[0].equalsIgnoreCase("gs")) {
					e.setCancelled(true);

					if (cut.length > 1) {

						if (cut[1].equalsIgnoreCase("start")) {
							if (player.hasPermission("guishop.creator") || player.isOp()) {
								player.sendMessage(Utils.getPrefix() + " Entered creator mode!");
								Main.CREATOR.put(player.getName(), new Creator(player));
							} else {
								player.sendMessage(Utils.getPrefix() + " " + Utils.getNoPermission());
								e.setCancelled(true);
								return;
							}
						} else if (cut[1].equalsIgnoreCase("stop")) {
							if (player.hasPermission("guishop.creator")) {
								player.sendMessage(Utils.getPrefix() + " Exited creator mode!");
								Main.CREATOR.remove(player.getName());
							} else {
								player.sendMessage(Utils.getPrefix() + " " + Utils.getNoPermission());
							}
						} else if (cut[1].equalsIgnoreCase("setchest")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setChest();
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("setshopname")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setShopName(cut[2]);
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("saveshop")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								if (Main.CREATOR.get(player.getName()).name == null) {
									player.sendMessage(Utils.getPrefix() + "Set a shop name!");
								} else if (Main.CREATOR.get(player.getName()).chest == null) {
									player.sendMessage(Utils.getPrefix() + " Set a chest location!");
								} else {
									Main.CREATOR.get(player.getName()).saveShop();
								}
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("p")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setPrice(Double.parseDouble(cut[2]));
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("s")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setSell(Double.parseDouble(cut[2]));
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("n")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								String name = "";
								for (String str : cut) {
									if (!str.equalsIgnoreCase(cut[2])) {
										name += str + " ";
									}
								}
								Main.CREATOR.get(player.getName()).setName(name.trim());
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("loadshop")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								if (Main.CREATOR.get(player.getName()).name == null) {
									player.sendMessage(Utils.getPrefix() + "Set a shop name!");
								} else if (Main.CREATOR.get(player.getName()).chest == null) {
									player.sendMessage(Utils.getPrefix() + " Set a chest location!");
								} else {
									Main.CREATOR.get(player.getName()).loadShop();
								}
							} else {
								player.sendMessage(Utils.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("reload")) {
							if (player.hasPermission("guishop.reload") || player.isOp()) {
								Main.INSTANCE.reloadConfig();
								Main.INSTANCE.createFiles();
								Main.INSTANCE.loadDefaults();

								player.sendMessage("§aGUIShop has been reloaded!");
							}
						} else {
							printUsage(player);
						}
					} else {
						printUsage(player);
					}
				}
			} else {
				e.setCancelled(true);
				player.sendMessage("§c" + Main.getInstance().getDebugger().getErrorMessage());
			}
		}
	}

	/**
	 * Print the usage of the plugin to the player.
	 */
	public void printUsage(Player player) {
		player.sendMessage("        Proper Usage:        ");
		player.sendMessage("/guishop start - Starts creator session");
		player.sendMessage("/guishop setchest - Sets chest location to chest you look at");
		player.sendMessage("/guishop setshopname - Sets the current shop you're working in");
		player.sendMessage("/guishop loadshop - Loads current shop into chest!");
		player.sendMessage("/guishop p - Set item in hand's buy price");
		player.sendMessage("/guishop s - Set item in hand's sell price");
		player.sendMessage("/guishop n - Set item in hand's name");
	}

	// When the inventory closes

	// When the player clicks a sign
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		Block block = e.getClickedBlock();
		// If the block exists
		if (block != null) {
			// If the block has a state
			if (block.getState() != null) {
				// If the block state is a Sign
				if (block.getState() instanceof Sign) {
					Sign sign = (Sign) block.getState();
					String line1 = ChatColor.translateAlternateColorCodes('&', sign.getLine(0));
					// Check if the sign is a GUIShop sign
					if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',
							Main.INSTANCE.getMainConfig().getString("sign-title")))) {
						// If the player has Permission to use sign
						if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use")
								|| player.isOp()) {
							Menu menu = new Menu(player.getName());
							menu.open();
							e.setCancelled(true);
						} else {
							player.sendMessage(Utils.getPrefix() + " " + Utils.getNoPermission());
							e.setCancelled(true);
						}

					}
				}
			}
		}
	}

}
