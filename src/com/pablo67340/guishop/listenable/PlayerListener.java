package com.pablo67340.guishop.listenable;

import java.util.Objects;

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
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.ItemUtil;
import com.pablo67340.guishop.util.XMaterial;

public final class PlayerListener implements Listener {

	/**
	 * An instance of a {@link PlayerListener} that will be used to handle this
	 * specific object reference from other classes, even though methods here will
	 * be static.
	 */
	public static final PlayerListener INSTANCE = new PlayerListener();

	private void openShop(Player player) {
		Menu menu = new Menu();
		menu.open(player);
	}

	/**
	 * Handle any commands sent into the chat, splice and compare to set GUIShop
	 * commands in config.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();

		if (!e.isCancelled()) {

			String command = e.getMessage().substring(1);
			String[] cut = command.split(" ");

			if (Main.BUY_COMMANDS.contains(cut[0])) {
				if (cut.length == 1) {
					if (player.hasPermission("guishop.use") || player.isOp()) {
						e.setCancelled(true);
						openShop(player);
						return;
					} else {
						e.setCancelled(true);
						player.sendMessage(Config.getNoPermission());
						return;
					}
				} else {
					e.setCancelled(true);
					if (Main.getINSTANCE().getShops().containsKey(cut[1].toLowerCase())) {

						ShopDef shopDef = Main.getINSTANCE().getShops().get(cut[1].toLowerCase());

						if (player.hasPermission("guishop.shop." + shopDef.getShop()) || player.isOp()
								|| player.hasPermission("guishop.slot.*")) {
							new Menu().openShop(player, shopDef);
						} else {
							player.sendMessage(Config.getNoPermission());
						}

					} else {
						player.sendMessage("§cShop not found");
					}
				}
			}

			if (Main.SELL_COMMANDS.contains(command)) {
				if (player.hasPermission("guishop.sell") || player.isOp()) {
					e.setCancelled(true);
					new Sell().open(player);
					return;
				} else {
					player.sendMessage(Config.getNoPermission());
					e.setCancelled(true);
					return;
				}
			}

			if (cut[0].equalsIgnoreCase("guishop") || cut[0].equalsIgnoreCase("gs")) {
				e.setCancelled(true);
				if (cut.length >= 2) {
					if (cut[1].equalsIgnoreCase("edit")) {
						if (player.hasPermission("guishop.admin")) {
							Main.getCREATOR().add(player.getName());
							openShop(player);
						}
					} else if (cut[1].equalsIgnoreCase("p") || cut[1].equalsIgnoreCase("price")) {
						Object result = null;
						if (cut[2].equalsIgnoreCase("false")) {
							result = false;
						} else {
							try {
								result = Double.parseDouble(cut[2]);
							} catch (Exception ex) {
								try {
									result = Integer.parseInt(cut[2]);
								} catch (Exception ex2) {
									player.sendMessage("Please use a valid value");
								}
							}
						}
						ItemUtil.setPrice(result, player);

					} else if (cut[1].equalsIgnoreCase("s") || cut[1].equalsIgnoreCase("sell")) {
						Object result = null;
						if (cut[2].equalsIgnoreCase("false")) {
							result = false;
						} else {
							try {
								result = Double.parseDouble(cut[2]);
							} catch (Exception ex) {
								try {
									result = Integer.parseInt(cut[2]);
								} catch (Exception ex2) {
									player.sendMessage("Please use a valid value");
								}
							}
						}
						ItemUtil.setSell(result, player);

					} else if (cut[1].equalsIgnoreCase("sn") || cut[1].equalsIgnoreCase("shopname")) {
						if (cut.length >= 3) {
							String line = "";
							for (int x = 2; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.setShopName(line, player);
						} else {
							player.sendMessage("Please specify a custom sell-name.");
						}
					} else if (cut[1].equalsIgnoreCase("bn") || cut[1].equalsIgnoreCase("buyname")) {
						if (cut.length >= 3) {
							String line = "";
							for (int x = 2; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.setBuyName(line, player);
						} else {
							player.sendMessage("Please specify a custom buy-name.");
						}
					} else if (cut[1].equalsIgnoreCase("e") || cut[1].equalsIgnoreCase("enchant")) {
						if (cut.length >= 3) {
							String enchantments = "";
							for (int x = 2; x <= cut.length - 1; x++) {
								enchantments += cut[x] + " ";
							}
							ItemUtil.setEnchantments(enchantments.trim(), player);
						} else {
							player.sendMessage("Please specify enchantments. E.G 'dura:1 sharp:2'");
						}
					} else if (cut[1].equalsIgnoreCase("asll")) {
						if (cut.length >= 3) {
							String line = "";
							for (int x = 2; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.addToShopLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("dsll")) {
						if (cut.length >= 3) {
							int slot = Integer.parseInt(cut[2]);
							ItemUtil.deleteShopLore(slot, player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("esll")) {
						if (cut.length >= 3) {
							String line = "";
							int slot = Integer.parseInt(cut[2]);
							for (int x = 3; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.editShopLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
									player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("abll")) {
						if (cut.length >= 3) {
							String line = "";
							for (int x = 2; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.addToBuyLore(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("dsll")) {
						if (cut.length >= 3) {
							int slot = Integer.parseInt(cut[2]);
							ItemUtil.deleteBuyLore(slot, player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("ebll")) {
						if (cut.length >= 3) {
							String line = "";
							int slot = Integer.parseInt(cut[2]);
							for (int x = 3; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.editBuyLore(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
									player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("t")) {
						if (cut.length >= 3) {
							String type = cut[2];
							if (ItemType.valueOf(type) == null) {
								player.sendMessage("Invalid Type! Accepted: SHOP, COMMAND, BLANK, DUMMY");
								return;
							}
							ItemUtil.setType(cut[2], player);
						} else {
							player.sendMessage("Please specify a type.");
						}
					} else if (cut[1].equalsIgnoreCase("ac")) {
						if (cut.length >= 3) {
							String line = "";
							for (int x = 2; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.addCommand(ChatColor.translateAlternateColorCodes('&', line.trim()), player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("ec")) {
						if (cut.length >= 3) {
							String line = "";
							int slot = Integer.parseInt(cut[2]);
							for (int x = 3; x <= cut.length - 1; x++) {
								line += cut[x] + " ";
							}
							ItemUtil.editCommand(slot, ChatColor.translateAlternateColorCodes('&', line.trim()),
									player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("dc")) {
						if (cut.length >= 3) {
							int slot = Integer.parseInt(cut[2]);
							ItemUtil.deleteCommand(slot, player);
						} else {
							player.sendMessage("Please specify a line.");
						}
					} else if (cut[1].equalsIgnoreCase("mt")) {
						if (cut.length == 3) {
							ItemUtil.setMobType(cut[2], player);
						} else {
							player.sendMessage("Please specify a type");
						}
					} else {
						printUsage(player);
					}
				}
			}

		}
	}

	/**
	 * Print the usage of the plugin to the player.
	 */
	private void printUsage(Player player) {
		player.sendMessage("        Proper Usage:        ");
		player.sendMessage("/guishop edit - Opens in Editor Mode");
		player.sendMessage("/guishop price/p {price} - Set item in hand's buy price");
		player.sendMessage("/guishop sell/s {price} - Set item in hand's sell price");
		player.sendMessage("/guishop shopname/sn {name} - Set item in hand's Shop-Name");
		player.sendMessage("/guishop buyname/bn {name} - Set item in hand's Buy-Name");
		player.sendMessage("/guishop enchant/e {enchants} - Set item in hand's Enchantments");
		player.sendMessage("/guishop asll {line} - Add Shop Lore Line");
		player.sendMessage("/guishop dsll {lineNumber} - Delete Shop Lore Line. Starts at 0");
		player.sendMessage("/guishop esll {lineNumber} {line} - Edit Shop Lore Line. Starts at 0");
		player.sendMessage("/guishop abll {line} - Add Buy Lore Line");
		player.sendMessage("/guishop dbll {lineNumber} - Delete Buy Lore Line. Starts at 0");
		player.sendMessage("/guishop ebll {lineNumber} {line} - Edit Buy Lore Line. Starts at 0");
		player.sendMessage("/guishop ac {command} - Add Command to item");
		player.sendMessage("/guishop dc {lineNumber} - Delete Command by line. Starts at 0");
		player.sendMessage("/guishop ec {lineNumber} {cmd} - Edit Command by line. Starts at 0");
		player.sendMessage("/guishop mt {type} - Set an item's mob type. Used for Spawners/Eggs.");
		player.sendMessage("/guishop t {type} - Set an item's type. BLANK, SHOP, COMMAND, DUMMY");
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
			block.getState();
			// If the block state is a Sign
			if (block.getState() instanceof Sign) {
				Sign sign = (Sign) block.getState();
				String line1 = ChatColor.translateAlternateColorCodes('&', sign.getLine(0));
				// Check if the sign is a GUIShop sign
				if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',
						Objects.requireNonNull(Main.INSTANCE.getMainConfig().getString("sign-title"))))) {
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

	/**
	 * Custom MobSpawner placement method.
	 */
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getItemInHand().getType() == XMaterial.SPAWNER.parseMaterial()) {

			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> {
				ItemStack item = event.getItemInHand();
				NBTTagCompound cmp = ItemNBTUtil.getTag(item);
				if (cmp.hasKey("GUIShopSpawner")) {

					String mobId = cmp.getString("GUIShopSpawner");
					Block block = event.getBlockPlaced();
					CreatureSpawner cs = (CreatureSpawner) block.getState();
					cs.setSpawnedType(Objects.requireNonNull(EntityType.fromName(mobId)));
					cs.update();
				}
			}, 1L);

		}
	}

}
