package com.pablo67340.shop.listener;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import com.pablo67340.shop.handler.*;
import com.pablo67340.shop.main.Main;

public final class PlayerListener implements Listener {

	/**
	 * An instance of a {@link PlayerListener} that will be used to handle this
	 * specific object reference from other classes, even though methods here will
	 * be static.
	 */
	public static final PlayerListener INSTANCE = new PlayerListener();

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoinLoad(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!Main.getInstance().getDebugger().hasExploded()) {
			// Plugin loaded up fine.

			Main.MENUS.put(player.getName(), new Menu(player));
			Main.SELLS.put(player.getName(), new Sell(player));
		} else {
			if (player.isOp() || player.hasPermission("guishop.use")) {
				player.sendMessage("§c" + Main.getInstance().getDebugger().getErrorMessage());
			}
		}
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
						Main.MENUS.get(player.getName()).open();
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
						Main.SELLS.get(player.getName()).open();
						e.setCancelled(true);
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
								Shop.loadShops();

								for (Player p : Bukkit.getOnlinePlayers()) {
									Main.MENUS.get(p.getName()).load();
								}
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

	/**
	 * Handle global inventory click events, check if inventory is for GUIShop, if
	 * so, run logic.
	 */
	@SuppressWarnings({ "deprecation", "unused" })
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (e.getClickedInventory() != null) {
				Player player = (Player) e.getWhoClicked();

				// If the player is in the sell menu

				if (Main.HAS_SELL_OPEN.contains(player.getName())) {

					Integer itemID = 0;
					byte dataID = 0;

					if (e.getCurrentItem().getType() != Material.AIR) {
						itemID = e.getCurrentItem().getTypeId();
						dataID = e.getCurrentItem().getData().getData();
					} else if (e.getCursor().getType() != Material.AIR) {
						itemID = e.getCursor().getTypeId();
						dataID = e.getCursor().getData().getData();
					} else {
						// Clicked empty slot
					}
					float isSellable = 0;
					String selectedShop = "";
					for (Entry<String, Map<String, Price>> cmap : Main.PRICETABLE.entrySet()) {
						if (cmap.getValue().containsKey(itemID + ":" + dataID)) {
							isSellable = 1;
							selectedShop = cmap.getKey();
						}
					}

					// If the item has a loaded price, or in GUIShop at all
					if (Main.PRICETABLE.get(selectedShop).containsKey(itemID + ":" + dataID)) {
						// If the price is set to 0, or disabled.
						if (Main.PRICETABLE.get(selectedShop).get(itemID + ":" + dataID).getSellPrice() == 0) {
							e.setCancelled(true);
							player.sendMessage(Utils.getPrefix() + " " + Utils.getCantSell());
							return;
						}
					} else {
						e.setCancelled(true);
						player.sendMessage(Utils.getPrefix() + " " + Utils.getCantSell());
						return;
					}

				} else {

					/*
					 * If the player has the menu open.
					 */
					if (Main.HAS_MENU_OPEN.contains(player.getName())) {
						/*
						 * If the player clicks somewhere where we don't want them to, cancel the event.
						 */
						if (!Main.SHOPS.containsKey(e.getSlot())) {
							e.setCancelled(true);
							return;
						}

						/*
						 * If the player clicks on an empty slot, then cancel the event.
						 */
						if (e.getCurrentItem() != null) {
							if (e.getCurrentItem().getType() == Material.AIR) {
								e.setCancelled(true);
								return;
							}
						}

						/*
						 * If the player clicks in their own inventory, we want to cancel the event.
						 */
						if (e.getClickedInventory() == player.getInventory()) {
							e.setCancelled(true);
							return;
						}

						if (Main.HAS_MENU_OPEN.remove(player.getName())) {

							Main.SHOPS.get(e.getSlot()).open(player);
						}

						e.setCancelled(true);
						return;
					} else

					/*
					 * If the player has the shop open.
					 */
					if (Main.HAS_SHOP_OPEN.containsKey(player.getName())) {

						if (player.getInventory().firstEmpty() == -1) {
							e.setCancelled(true);
							player.sendMessage(Utils.getFull());
							return;
						}
						/*
						 * If the player clicks on an empty slot, then cancel the event.
						 */
						if (e.getCurrentItem() != null) {
							if (e.getCurrentItem().getType() == Material.AIR) {
								e.setCancelled(true);
								return;
							}
						}

						/*
						 * If the player clicks in their own inventory, we want to cancel the event.
						 */
						if (e.getClickedInventory() == player.getInventory()) {
							e.setCancelled(true);
							return;
						}

						Shop shop = Main.HAS_SHOP_OPEN.get(player.getName());

						if (e.getSlot() >= 0 && e.getSlot() < shop.getGUI().getSize()) {
							/**
							 * If the player clicks the 'back' button, then open the menu. Otherwise, If the
							 * user clicks the forward button, load and open next page, Otherwise, If the
							 * user clicks the backward button, load and open the previous page, Otherwise
							 * Attempt to purchase the clicked item.
							 */
							if (e.getSlot() == shop.getGUI().getSize() - 1) {
								shop.closeAndOpenMenu(player);
							} else if (e.getSlot() == shop.getGUI().getSize() - 2) {
								e.setCancelled(true);
								if (e.getCurrentItem().getData().getData() != 14) {
									Main.HAS_SHOP_OPEN.get(player.getName())
											.loadPage(Main.HAS_SHOP_OPEN.get(player.getName()).getCurrentPage() + 1);
								}

							} else if (e.getSlot() == 46) {
								e.setCancelled(true);
								if (e.getCurrentItem().getData().getData() != 14) {
									Main.HAS_SHOP_OPEN.get(player.getName())
											.loadPage(Main.HAS_SHOP_OPEN.get(player.getName()).getCurrentPage() - 1);
								}
							} else {

								/*
								 * If the player has enough money to purchase the item, then allow them to.
								 */
								Item item;
								if (shop.hasPages()) {
									item = shop.getPage(shop.getCurrentPage()).getContents()[e.getSlot()];
								} else {
									item = shop.getItems()[e.getSlot()];
								}

								// Check if the item is disabled, or price is 0
								if (item.getBuyPrice() == 0) {
									player.sendMessage(Utils.getPrefix() + " " + Utils.getCantBuy());
									player.setItemOnCursor(new ItemStack(Material.AIR));
									e.setCancelled(true);
									return;
								}

								// Does the quantity work out?
								int quantity = (int) (Main.getEconomy().getBalance(player.getName())
										/ item.getBuyPrice());

								quantity = Math.min(quantity,
										e.isShiftClick() ? new ItemStack(item.getId()).getMaxStackSize() : 1);

								// If the quantity is 0
								if (quantity == 0) {
									player.sendMessage(Utils.getPrefix() + " " + Utils.getNotEnoughPre()
											+ item.getBuyPrice() + Utils.getNotEnoughPost());
									player.setItemOnCursor(new ItemStack(Material.AIR));
									e.setCancelled(true);
									return;
								}

								Map<Integer, ItemStack> returnedItems;

								// If the item is not a mob spawner
								if (item.getId() != 52) {

									// if the item has a data
									if (item.getData() > 0) {
										ItemStack itemStack = new ItemStack(item.getId(), quantity,
												(short) item.getData());
										if (item.getEnchantments() != null) {
											for (String enc : item.getEnchantments()) {
												String enchantment = StringUtils.substringBefore(enc, ":");
												String level = StringUtils.substringAfter(enc, ":");
												itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment),
														Integer.parseInt(level));
											}
										}

										itemStack.setAmount(item.getQty());
										// If is shift clicking, buy 1
										if (e.isShiftClick())
											itemStack.setAmount(1);

										returnedItems = player.getInventory().addItem(itemStack);
									} else {
										ItemStack itemStack = new ItemStack(item.getId(), quantity);
										// If the item has enchantments
										if (item.getEnchantments() != null) {
											for (String enc : item.getEnchantments()) {
												String enchantment = StringUtils.substringBefore(enc, ":");
												String level = StringUtils.substringAfter(enc, ":");
												itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment),
														Integer.parseInt(level));
											}
										}
										itemStack.setAmount(item.getQty());
										// If is shift clicking, buy 1.
										if (e.isShiftClick())
											itemStack.setAmount(1);
										returnedItems = player.getInventory().addItem(itemStack);
									}
								} else {
									ItemStack spawner = new ItemStack(item.getId(), quantity);

									returnedItems = player.getInventory()
											.addItem(new ItemStack[] { Main.getInstance().su.setSpawnerType(spawner,
													(short) item.getData(),
													String.valueOf(Spawners.getMobName(item.getData()))
															+ " Spawner") });
								}

								double priceToPay = 0;

								/*
								 * If the map is empty, then the items purchased don't overflow the player's
								 * inventory. Otherwise, we need to reimburse the player (subtract it from
								 * priceToPay).
								 */
								if (returnedItems.isEmpty()) {
									priceToPay = item.getBuyPrice();
									// If the player is shift clicking, take the
									// price, divide by quantity
									if (e.isShiftClick())
										priceToPay = item.getBuyPrice() / item.getQty();
								} else {
									double priceToReimburse = 0D;

									// if the item is not a shift click
									if (!e.isShiftClick()) {

										for (ItemStack i : returnedItems.values()) {
											priceToReimburse += item.getBuyPrice();
										}

										priceToPay = item.getBuyPrice() - priceToReimburse;

									} else {
										// Add all the prices for all purchased
										// items
										for (ItemStack i : returnedItems.values()) {
											priceToReimburse += (item.getBuyPrice() / item.getQty());
										}
										priceToPay = (item.getBuyPrice() / item.getQty()) - priceToReimburse;

									}
								}

								// Check if the transition was successful
								if (Main.getEconomy().withdrawPlayer(player.getName(), priceToPay)
										.transactionSuccess()) {
									// If the player has the sound enabled, play
									// it!
									if (Utils.isSoundEnabled()) {
										try {
											player.playSound(player.getLocation(), Sound.valueOf(Utils.getSound()), 1,
													1);
										} catch (Exception ex) {
											Main.getInstance().getLogger().warning(
													"Incorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
										}
									}
									player.sendMessage(Utils.getPurchased() + priceToPay + Utils.getTaken());
								}
								e.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}

	// When the inventory closes

	@EventHandler(priority = EventPriority.HIGH)
	public void onClose(InventoryCloseEvent e) {
		final String playerName = e.getPlayer().getName();
		// If escape only is enabled
		if (Utils.getEscapeOnly()) {
			// If the player has the shop open
			if (Main.HAS_SHOP_OPEN.containsKey(playerName)) {
				Main.HAS_SHOP_OPEN.remove(playerName);
				e.getPlayer().closeInventory();

				BukkitScheduler scheduler = Main.INSTANCE.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(Main.INSTANCE, new Runnable() {
					@Override
					public void run() {
						// Do something
						Main.MENUS.get(playerName).open();
					}
				}, 1L);

				return;
				// If the player has the menu open
			} else if (Main.HAS_MENU_OPEN.contains(playerName)) {
				Main.HAS_MENU_OPEN.remove(playerName);
				Main.HAS_SHOP_OPEN.remove(playerName);
				return;

			}
			// If the player has the sell menu open
			if (Main.HAS_SELL_OPEN.remove(playerName)) {
				Main.SELLS.get(playerName).sell();
				Main.HAS_SELL_OPEN.remove(playerName);
			}
			return;
			// If something else, purge all values, restart
		} else {
			Main.HAS_MENU_OPEN.remove(playerName);
			Main.HAS_SHOP_OPEN.remove(playerName);
			// The player had the sell menu open, Sell items
			if (Main.HAS_SELL_OPEN.remove(playerName)) {
				Main.SELLS.get(playerName).sell();
				Main.HAS_SELL_OPEN.remove(playerName);
			}
			return;
		}

	}

	// When the player clicks a sign
	@SuppressWarnings("unlikely-arg-type")
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
							Main.MENUS.get(player).open();
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
