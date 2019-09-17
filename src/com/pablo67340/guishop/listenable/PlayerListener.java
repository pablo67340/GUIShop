package com.pablo67340.guishop.listenable;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.*;
import org.bukkit.block.Block;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.event.*;

import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import com.pablo67340.guishop.definition.Expires;
import com.pablo67340.guishop.definition.ItemCommand;
import com.pablo67340.guishop.handler.*;
import com.pablo67340.guishop.main.Main;
import com.pablo67340.guishop.util.Config;

import me.ialistannen.mininbt.ItemNBTUtil;
import me.ialistannen.mininbt.NBTWrappers.NBTTagCompound;

public final class PlayerListener implements Listener {

	/**
	 * An instance of a {@link PlayerListener} that will be used to handle this
	 * specific object reference from other classes, even though methods here will
	 * be static.
	 */
	public static final PlayerListener INSTANCE = new PlayerListener();

	public void openShop(Player player) {
		Menu menu = new Menu();
		menu.open(player);
	}

	/**
	 * Handle any commands sent into the chat, splice and compare to set GUIShop
	 * commands in config.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();

		if (!e.isCancelled()) {

			if (!Main.getInstance().getDebugger().hasExploded()) {

				String command = e.getMessage().substring(1);
				String[] cut = command.split(" ");
				ItemCommand itemCommand = Main.getInstance().loadCommands(e.getPlayer().getUniqueId());
				if (Main.protectedCommands.contains("/" + cut[0])) {

					if (player.hasPermission("guishop.bypass")) {
						e.setCancelled(false);
						return;
					}

					if (itemCommand.getCommands().isEmpty() || !itemCommand.getCommands().contains("/" + cut[0])) {
						e.setCancelled(true);
						player.sendMessage(Config.getCommandPurchase());
						return;
					}

					Set<String> activeCommands = itemCommand.getCommands();
					String remaining = "";
					for (String cmd : activeCommands) {
						String reParse = cut[0];
						if (cut[0].equalsIgnoreCase(reParse)) {
							Expires expires = itemCommand.getExpiration(cmd);
							if (expires.isExpired()) {
								e.setCancelled(true);
								player.sendMessage(Config.getCommandExpired());
							} else {
								remaining = "";
								Date check = new Date();
								long duration = expires.getExpiration().getTime() - check.getTime();

								long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);

								long minutes = TimeUnit.SECONDS.toMinutes(seconds);

								long hours = TimeUnit.MINUTES.toHours(minutes);

								long days = TimeUnit.HOURS.toDays(hours);

								if (days != 0) {
									remaining += days + "d ";
								}

								if (hours != 0) {
									if (hours > 24) {
										hours = (duration / 1000000) % 60;
									}
									remaining += hours + "h ";
								}

								if (minutes != 0) {
									if (minutes > 60) {
										minutes = (duration / 10000) % 60;
									}
									remaining += minutes + "m ";
								}

								if (seconds != 0) {
									if (seconds > 60) {
										seconds = (duration / 1000) % 60;
									}
									remaining += seconds + "s ";
								}

							}
						}
					}
					player.sendMessage(Config.getCommandRemaining().replace("{TIME}", remaining));

				}

				if (Main.BUY_COMMANDS.contains(command)) {
					if (player.hasPermission("guishop.use") || player.isOp()) {
						e.setCancelled(true);
						openShop(player);
						return;
					} else {
						e.setCancelled(true);
						player.sendMessage(Config.getNoPermission());
						return;
					}
				}

				if (Main.SELL_COMMANDS.contains(command)) {
					if (player.hasPermission("guishop.sell") || player.isOp()) {
						e.setCancelled(true);
						Sell sell = new Sell();
						sell.load();
						sell.open(player);
						return;
					} else {
						player.sendMessage(Config.getNoPermission());
						e.setCancelled(true);
						return;
					}
				}

				if (cut[0].equalsIgnoreCase("guishop") || cut[0].equalsIgnoreCase("gs")) {
					e.setCancelled(true);

					if (cut.length > 1) {

						if (cut[1].equalsIgnoreCase("start")) {
							if (player.hasPermission("guishop.creator") || player.isOp()) {
								player.sendMessage(Config.getPrefix() + " Entered creator mode!");
								Main.CREATOR.put(player.getName(), new Creator(player));
							} else {
								player.sendMessage(Config.getPrefix() + " " + Config.getNoPermission());
								e.setCancelled(true);
								return;
							}
						} else if (cut[1].equalsIgnoreCase("stop")) {
							if (player.hasPermission("guishop.creator")) {
								player.sendMessage(Config.getPrefix() + " Exited creator mode!");
								Main.CREATOR.remove(player.getName());
							} else {
								player.sendMessage(Config.getPrefix() + " " + Config.getNoPermission());
							}
						} else if (cut[1].equalsIgnoreCase("setchest")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setChest();
							} else {
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("setshopname")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setShopName(cut[2]);
							} else {
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("saveshop")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								if (Main.CREATOR.get(player.getName()).name == null) {
									player.sendMessage(Config.getPrefix() + "Set a shop name!");
								} else if (Main.CREATOR.get(player.getName()).chest == null) {
									player.sendMessage(Config.getPrefix() + " Set a chest location!");
								} else {
									Main.CREATOR.get(player.getName()).saveShop();
								}
							} else {
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("p")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setPrice(Double.parseDouble(cut[2]));
							} else {
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("s")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								Main.CREATOR.get(player.getName()).setSell(Double.parseDouble(cut[2]));
							} else {
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
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
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
							}
						} else if (cut[1].equalsIgnoreCase("loadshop")) {
							if (Main.CREATOR.containsKey(player.getName())) {
								if (Main.CREATOR.get(player.getName()).name == null) {
									player.sendMessage(Config.getPrefix() + "Set a shop name!");
								} else if (Main.CREATOR.get(player.getName()).chest == null) {
									player.sendMessage(Config.getPrefix() + " Set a chest location!");
								} else {
									Main.CREATOR.get(player.getName()).loadShop();
								}
							} else {
								player.sendMessage(Config.getPrefix() + " You need to start a creator session!");
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
				player.sendMessage("Â§c" + Main.getInstance().getDebugger().getErrorMessage());
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
							e.setCancelled(true);
							Menu menu = new Menu();
							menu.open(player);
						} else {
							e.setCancelled(true);
							player.sendMessage(Config.getPrefix() + " " + Config.getNoPermission());
						}

					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getItemInHand().getType() == Material.SPAWNER) {
			ItemStack item = event.getItemInHand();
			NBTTagCompound cmp = ItemNBTUtil.getTag(item);
			if (cmp.hasKey("GUIShopSpawner")) {
				String mobId = cmp.getString("GUIShopSpawner");
				Block block = event.getBlockPlaced();
				CreatureSpawner cs = (CreatureSpawner) block.getState();
				cs.setSpawnedType(EntityType.fromName(mobId));
				cs.update();
			}
		}
	}

}
