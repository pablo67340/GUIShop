package com.pablo67340.GUIShop.Listeners;


import com.pablo67340.GUIShop.Handlers.Spawners;

import com.pablo67340.GUIShop.Main.Main;
import de.dustplanet.util.SilkUtil;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;


public class PlayerListener
implements Listener {
	Main plugin;
	public ArrayList<String> menuOpen = new ArrayList<String>();
	public ArrayList<String> shopOpen = new ArrayList<String>();
	public String title;
	public boolean one;
	public boolean close = false;

	public PlayerListener(Main main) {
		plugin = main;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String command = event.getMessage();
		if (command.equalsIgnoreCase("/" + plugin.utils.getCommand())) {
			if (player.hasPermission("guishop.use") || player.isOp()) {
				if (plugin.getConfig().getBoolean("sign-only")) {
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("sign-only-message")));
					event.setCancelled(true);
				} else {
					plugin.menu.loadMenu(player);
					event.setCancelled(true);
					if (shopOpen.contains(player.getName())) {
						shopOpen.remove(player.getName());
					}
					if (!menuOpen.contains(player.getName())) {
						menuOpen.add(player.getName());
					}
				}
			} else {
				player.sendMessage(String.valueOf(plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
			}
		}
		if (command.equalsIgnoreCase("/" + plugin.utils.getCommand() + " reload")) {
			if (player.isOp()) {
				if (plugin.cache.flushData()) {
					if (plugin.utils.getVerbose().booleanValue()) {
						System.out.println("Shop Data exists! Flushing data!");
					}
				} else if (plugin.utils.getVerbose().booleanValue()) {
					System.out.println("No Shop data existed! No Shops have been opened and cached yet!");
				}
				plugin.reloadConfig();
				plugin.reloadCustomConfig();
				event.setCancelled(true);
				if (plugin.utils.getVerbose().booleanValue()) {
					System.out.println("Reload Complete!");
				}
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', (String.valueOf(plugin.utils.getPrefix()) + " &aReloaded!")));
			} else {
				player.sendMessage("No Permission! You must be OP!");
			}
		}
		if (command.equalsIgnoreCase("/" + plugin.utils.getSellCommand())) {
			System.out.println("ELSE ELSE ELSE");
			if (player.isOp() || player.hasPermission("guishop.use")) {
				if (player.hasPermission("guishop.sell") || player.isOp()) {
					plugin.sell.loadSell(player);
					event.setCancelled(true);
				} else {
					event.setCancelled(true);
					player.sendMessage(String.valueOf(plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
				}
			} else {
				event.setCancelled(true);
				player.sendMessage(String.valueOf(plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
			}
		}
		if (command.equalsIgnoreCase("/" + plugin.utils.getCommand() + " cache flush")) {
			if (player.isOp()) {
				plugin.cache.flushData();
				event.setCancelled(true);
			} else {
				event.setCancelled(true);
				player.sendMessage(String.valueOf(plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
			}
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onClick(InventoryClickEvent e) {
		if (e.getClickedInventory()==null){
			
		}else{
			if (e.getWhoClicked() instanceof Player) {
				if (e.getInventory().getTitle().equalsIgnoreCase(plugin.utils.getSellTitle())) {
					e.setCancelled(false);
				} else {
					Player p = (Player)e.getWhoClicked();
					if (!shopOpen.contains(p.getName())) {

						if (menuOpen.contains(p.getName())) {
							if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
								e.setCancelled(true);
								close = false;
								menuOpen.remove(p.getName());
								shopOpen.remove(p.getName());
								p.closeInventory();
								
							}
							close = true;
							if (plugin.utils.getVerbose().booleanValue()) {
								System.out.println("MenuOpen passed. Player removed");
							}
							if (e.getInventory().getTitle().contains(plugin.utils.getMenuName())) {
								if (plugin.utils.getVerbose().booleanValue()) {
									System.out.println("Title contains menu name");
								}
								if (e.getClick().isKeyboardClick()) {
									e.setCancelled(true);
									p.closeInventory();
								}
								if (e.getSlotType() == InventoryType.SlotType.CONTAINER) {
									if (e.getClickedInventory().getType() == e.getView().getType()) {
										if (e.isLeftClick() || e.isShiftClick() || e.isRightClick() && !e.getClick().isKeyboardClick()) {
											@SuppressWarnings("unused")
											int[] row1;
											for (int slot : row1 = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28}) {
												if (e.getRawSlot() != slot - 1) continue;
												if (plugin.getConfig().getString(String.valueOf(slot) + ".Enabled") == "true") {
													if (p.hasPermission("guishop.slot." + slot) || p.isOp()) {
														e.setCancelled(true);
														String shop = plugin.getConfig().getString(String.valueOf(slot) + ".Shop");
														title = "";
														title = plugin.getConfig().getString(String.valueOf(slot) + ".Shop");
														String shopn = String.valueOf(shop) + ".";
														plugin.shop.setShopName(shopn);
														plugin.dataLoader.loadData(shopn);
														e.setCancelled(true);
														plugin.delayShop(p);
														menuOpen.remove(p.getName());
														shopOpen.add(p.getName());
														
													} else {
														break;
													}
												} else {
													plugin.closeInventory(p);
												}
												break;
											}
										} else {
											e.getClick().isKeyboardClick();
										}
									} else {
										plugin.closeInventory(p);
									}
								} else {
									plugin.closeInventory(p);
								}
							}
						} else {
							e.setCancelled(false);
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority=EventPriority.LOWEST)
	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			if (plugin.utils.getVerbose().booleanValue()) {
				System.out.println("e is a player onShopClick");
			}
			final Player p = (Player)e.getWhoClicked();
			if (e.getInventory().getTitle().equalsIgnoreCase(plugin.utils.getSellTitle())) {
				e.setCancelled(false);
			} else if (!shopOpen.contains(p.getName())) {
				if (plugin.utils.getVerbose().booleanValue()) {
					System.out.println("shopOpen DOES contain onShopClick");
				}
			} else {
				
				String properName = plugin.shop.getShopName().replace(".", "");
				if (e.getClick().isKeyboardClick()) {
					e.setCancelled(true);
					p.closeInventory();
				}
				if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
					e.setCancelled(true);
					close = false;
					menuOpen.remove(p.getName());
					shopOpen.remove(p.getName());
					p.closeInventory();
					
				}
				if (e.getInventory().getTitle().contains(properName) && !e.getInventory().getTitle().contains(plugin.utils.getMenuName())) {
					ItemStack item;
					if (plugin.utils.getVerbose().booleanValue()) {
						System.out.println("Shop name is shop name and not menu name");
						System.out.println("Menu name: " + plugin.utils.getMenuName() + " Compared to: " + e.getInventory().getTitle());
					}
					e.setCancelled(true);
					if (e.getSlot() != -999 && (item = e.getCurrentItem()) != null) {
						if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
							if (!item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")))) {
								if (e.getInventory().getItem(e.getSlot()) != null) {
									if (item.getItemMeta().hasLore()) {
										item.getItemMeta().getLore().toString().contains(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost")));
										int price = Integer.MAX_VALUE;
										String lorestring2 = ChatColor.stripColor(item.getItemMeta().getLore().toString());
										List<String> items = Arrays.asList(lorestring2.split("\\s*,\\s*"));
										String lorestring = ChatColor.stripColor(item.getItemMeta().getLore().toString().replace("[", "").replace("]", "").replace(",", "").replace("To sell, click the item in your inv.", "").replace("Must be the same quantity!", "").replace("Shift+Click to buy 1 item", ""));
										lorestring = StringUtils.substringBefore(lorestring, ".");
										String mobid = StringUtils.substringAfter(lorestring2, "ID: ").replace("]", "");
										if (plugin.utils.getVerbose().booleanValue()) {
											System.out.println("LORE LORE LORE LORE!!!!!!!! " + mobid);
										}
										price = Integer.parseInt(items.get(1));
										if (e.isLeftClick() && e.isShiftClick() && !e.getClick().isKeyboardClick()) {
											e.setCancelled(true);
											ItemStack dupeitem = item.clone();
											int ammount = dupeitem.getAmount() / dupeitem.getAmount();
											int price2 = price / dupeitem.getAmount();
											dupeitem.setAmount(ammount);
											e.setCancelled(true);
											if (plugin.econ.getBalance(p.getName()) >= price2) {
												EconomyResponse r = plugin.econ.withdrawPlayer(p.getName(), price2);
												if (r.transactionSuccess()) {
													e.setCancelled(true);
													if (dupeitem.getType() == Material.MOB_SPAWNER) {
														if (plugin.getServer().getPluginManager().getPlugin("SilkSpawners") == null) {
															if (plugin.utils.getVerbose().booleanValue()) {
																System.out.println("ERROR: You are trying to purchase a MobSpawner without SilkSpawners installed!");
															}
														} else {
															if (plugin.utils.getVerbose().booleanValue()) {
																System.out.println("Item IS a MOB_SPAWNER");
															}
															SilkUtil su = SilkUtil.hookIntoSilkSpanwers();

															ItemStack dupeitem2 = stripMeta(dupeitem, dupeitem.getAmount());
															p.getInventory().addItem(new ItemStack[]{su.setSpawnerType(dupeitem2, Short.parseShort(mobid), String.valueOf(Spawners.getMobName(Integer.parseInt(mobid))) + " Spawner")});
														}
													} else {
														if (plugin.utils.getVerbose().booleanValue()) {
															System.out.println("No Mob Spawner here...");
														}
														p.getInventory().addItem(new ItemStack[]{stripMeta(dupeitem, dupeitem.getAmount())});
													}
													p.sendMessage(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix())) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("purchased")) + ammount + " " + item.getType().toString().toLowerCase() + "\u00a7f!");
													p.sendMessage(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix())) + " " + "\u00a7c$" + price2 + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("taken")));
												}
											} else {
												p.setItemInHand(null);
												double dif = price2 - plugin.econ.getBalance(p.getName());
												p.sendMessage(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix())) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-pre")) + dif + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-post")));
												one = false;
											}
										}
										if (!(!e.isLeftClick() || e.isShiftClick() || e.getClick().isKeyboardClick())) {
											if (one) {
												e.setCancelled(true);
											} else if (plugin.econ.getBalance(p.getName()) >= price) {
												EconomyResponse r = plugin.econ.withdrawPlayer(p.getName(), price);
												if (r.transactionSuccess()) {
													ItemStack dupeitem = item.clone();
													if (dupeitem.getType() == Material.MOB_SPAWNER) {
														if (plugin.getServer().getPluginManager().getPlugin("SilkSpawners") == null) {
															if (plugin.utils.getVerbose().booleanValue()) {
																System.out.println("ERROR: You are trying to purchase a MobSpawner without SilkSpawners installed!");
															}
														} else {
															if (plugin.utils.getVerbose().booleanValue()) {
																System.out.println("Item IS a MOB_SPAWNER");
															}
															SilkUtil su = SilkUtil.hookIntoSilkSpanwers();

															ItemStack dupeitem2 = stripMeta(dupeitem, dupeitem.getAmount());
															p.getInventory().addItem(new ItemStack[]{su.setSpawnerType(dupeitem2, Short.parseShort(mobid), String.valueOf(Spawners.getMobName(Integer.parseInt(mobid))) + " Spawner")});
														}
													} else {
														if (plugin.utils.getVerbose().booleanValue()) {
															System.out.println("No Mob Spawner here...");
														}
														p.getInventory().addItem(new ItemStack[]{stripMeta(dupeitem, dupeitem.getAmount())});
													}
													p.sendMessage(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix())) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("purchased")) + item.getAmount() + " " + item.getType().toString().toLowerCase() + "\u00a7f!");
													p.sendMessage(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix())) + " " + "\u00a7c$" + price + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("taken")));
												}
											} else {
												double dif = price - plugin.econ.getBalance(p.getName());
												p.sendMessage(String.valueOf(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix())) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-pre")) + dif + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-post")));
												p.setItemOnCursor(new ItemStack(Material.AIR));
											}
										}
									} else {
										plugin.shop.trySell(p, item);
									}
								}
							} else {
								e.setCancelled(true);
								plugin.closeInventory(p);
								menuOpen.add(p.getName());
								plugin.delayMenu(p);
							}
						} else {
							plugin.shop.trySell(p, item);
						}
					}
				} else {
					if (!(e.getClickedInventory().getTitle().contains(properName) || e.getClickedInventory().getTitle().contains(plugin.utils.getMenuName()))) {
						shopOpen.remove(p.getName());
					}
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onItemDrop(PlayerDropItemEvent e) {
		if (close) {
			e.getItemDrop().remove();
			close = false;
		}
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		Player p = (Player)e.getPlayer();
		if (e.getInventory().getTitle().equalsIgnoreCase(plugin.utils.getSellTitle())) {
			plugin.sell.trySell(p, plugin.sell.getSellInv());
		}
	}

	public ItemStack stripMeta(ItemStack item, Integer amount) {
		ItemMeta itm = item.getItemMeta();
		itm.setLore(null);
		item.setItemMeta(itm);
		item.setAmount(amount.intValue());
		return item;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void eventSignChanged(SignChangeEvent event){  
		String title = event.getLine(0);
		Player p = event.getPlayer();
		if (title.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("sign-title")))){
			if (p.hasPermission("guishop.sign.place") || p.isOp()){
				event.setCancelled(false);
			}else{
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		Block block = e.getClickedBlock();
		if (block != null){
			if (block.getState() != null){
				if(block.getState() instanceof Sign) {
					Sign sign = (Sign) block.getState();
					String line1 = ChatColor.translateAlternateColorCodes('&',sign.getLine(0));
					if (plugin.utils.getVerbose()){
						System.out.println("Player Clicked sign with line1: "+line1 +" compared to "+ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("sign-title")));
					}
					if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("sign-title")))){
						if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use") || player.isOp()){
							plugin.menu.loadMenu(player);
							e.setCancelled(true);
						}else{
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
							e.setCancelled(true);
						}

					}
				}
			}
		}
	}

}

