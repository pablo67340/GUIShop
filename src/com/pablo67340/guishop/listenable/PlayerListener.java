package com.pablo67340.guishop.listenable;

import java.util.Objects;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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

import com.pablo67340.guishop.handler.*;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.util.Config;
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
				if (cut.length == 2) {
					if (cut[1].equalsIgnoreCase("edit")) {
						if (player.hasPermission("guishop.admin")) {
							Main.getCREATOR().put(player.getName(), new Creator(player));
							openShop(player);
						}
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
