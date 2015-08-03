package com.pablo67340.GUIShop.Listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.GUIShop.Main.Main;
public class PlayerListener implements Listener{
	Main plugin;

	public ArrayList menuOpen = new ArrayList();
	public String title;
	public boolean one;

	public PlayerListener(Main main){
		plugin = main;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent event){
		Player player = event.getPlayer();
		String command = event.getMessage();
		if (command.equalsIgnoreCase("/" + plugin.getConfig().getString("Command"))) {
			if ((player.hasPermission("guishop.use")) || (player.isOp())){
				if (plugin.getConfig().getBoolean("sign-only")){
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("sign-only-message")));
					event.setCancelled(true);
				}else{
					plugin.menu.loadMenu(player);
					event.setCancelled(true);
					if (menuOpen.contains(player.getName())){
						// Idk what i could add in here? Any ideas devs?
					}else{
						menuOpen.add(player.getName());
					}
				}
			}else{
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("no-permission")));
			}
		}else if((command.equalsIgnoreCase("/" + plugin.getConfig().getString("Command") + " reload"))){
			if (player.isOp()){
				if (plugin.cache.flushData()){
					if (plugin.utils.getVerbose()){
						System.out.println("Shop Data exists! Flushing data!");
					}
				}else{
					if (plugin.utils.getVerbose()){
						System.out.println("No Shop data existed! No Shops have been opened and cached yet!");
					}
				}
				plugin.reloadConfig();
				plugin.reloadCustomConfig();
				event.setCancelled(true);
				if (plugin.utils.getVerbose()){
					System.out.println("Reload Complete!");
				}
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix() + " &aReloaded!"));

			}else{
				player.sendMessage("No Permission! You must be OP!");
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent e){
		if ((e.getWhoClicked() instanceof Player)){
			final Player p = (Player)e.getWhoClicked();
			if (e.getInventory().getType() == InventoryType.PLAYER){
				e.setCancelled(false);
				if (menuOpen.contains(p.getName())){
					menuOpen.remove(p.getName());
				}
			}
			if (menuOpen.contains(p.getName()))
			{
				menuOpen.remove(p.getName());
				if (e.getInventory().getTitle().contains(plugin.utils.getMenuName())){
					if (e.getSlotType() == InventoryType.SlotType.CONTAINER){
						if (e.getClickedInventory().getType() == e.getView().getType()){
							if (e.isLeftClick() || e.isShiftClick() || e.isRightClick()){
								int[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28 };
								for (int slot : row1) {
									if (e.getRawSlot() == slot - 1){
										if (plugin.getConfig().getString(slot + ".Enabled") == "true"){
											if ((!p.hasPermission("guishop.slot." + slot)) && (!p.isOp())) {
												break;
											}
											e.setCancelled(true);
											String shop = plugin.getConfig().getString(slot + ".Shop");
											title = "";
											title = plugin.getConfig().getString(slot + ".Shop");
											String shopn = (shop + ".");
											plugin.shop.setShopName(shopn);
											plugin.shop.loadShop(p);
											e.setCancelled(true);
											break;
										}
										plugin.closeInventory(p);
										break;
									}
								}
							}else{
								plugin.closeInventory(p);
							}
						}else{
							plugin.closeInventory(p);
						}
					}else{
						plugin.closeInventory(p);
					}
				}else{
					e.setCancelled(true);
				}
				if ((e.getInventory().getTitle().contains(title)) && (!e.getInventory().getTitle().contains(plugin.utils.getMenuName()))){
					e.setCancelled(true);
					plugin.closeInventory(p);
					if (e.getSlot() != -999){
						ItemStack item = e.getCurrentItem();
						if (item != null) {
							if ((item.hasItemMeta()) && (item.getItemMeta().hasDisplayName())) {
								if (!item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("back")))){
									if (e.getInventory().getItem(e.getSlot()) != null){
										if (item.getItemMeta().hasLore()){
											if (item.getItemMeta().getLore().toString().contains(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("cost")))) {}
											int price = 2147483647;
											String lorestring2 = ChatColor.stripColor(item.getItemMeta().getLore().toString());
											List<String> items = Arrays.asList(lorestring2.split("\\s*,\\s*"));
											String lorestring = ChatColor.stripColor(item.getItemMeta().getLore().toString().replace("[", "").replace("]", "").replace(",", "").replace("To sell, click the item in your inv.", "").replace("Must be the same quantity!", "").replace("Shift+Click to buy 1 item", ""));
											lorestring = StringUtils.substringBefore(lorestring, ".");
											price = Integer.parseInt(items.get(1));
											if ((e.isLeftClick()) && (e.isShiftClick())){
												ItemStack dupeitem = item.clone();
												int ammount = dupeitem.getAmount() / dupeitem.getAmount();
												int price2 = price / dupeitem.getAmount();
												dupeitem.setAmount(ammount);
												if (plugin.econ.getBalance(p.getName()) >= price2){
													EconomyResponse r = plugin.econ.withdrawPlayer(p.getName(), price2);
													if (r.transactionSuccess()){
														p.getInventory().addItem(new ItemStack[] { stripMeta(dupeitem, Integer.valueOf(dupeitem.getAmount())) });
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("purchased")) + ammount + " " + item.getType().toString().toLowerCase() + "§f!");
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + "§c$" + price2 + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("taken")));
													}
												}else{
													double dif = price2 - plugin.econ.getBalance(p.getName());
													p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-pre")) + dif + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-post")));
													one = Boolean.valueOf(false);
												}
											}
											if ((e.isLeftClick()) && (!e.isShiftClick())) {
												if (one){
													e.setCancelled(true);
												}else if (plugin.econ.getBalance(p.getName()) >= price){
													EconomyResponse r = plugin.econ.withdrawPlayer(p.getName(), price);
													if (r.transactionSuccess()){
														ItemStack dupeitem = item.clone();
														p.getInventory().addItem(new ItemStack[] { stripMeta(dupeitem, Integer.valueOf(dupeitem.getAmount())) });
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("purchased")) + item.getAmount() + " " + item.getType().toString().toLowerCase() + "§f!");
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + "§c$" + price + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("taken")));
													}
												}else{
													double dif = price - plugin.econ.getBalance(p.getName());
													p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.utils.getPrefix()) + " " + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-pre")) + dif + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("not-enough-post")));
												}
											}
										}else{
											plugin.shop.trySell(p, item);
										}
									}
								}else{
									plugin.closeInventory(p);
									Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
									{
										public void run()
										{

											plugin.delayMenu(p);
										}
									}, 1L);

								}
							}else{
								plugin.shop.trySell(p, item);
							}
						}
					}
				}
				else
				{
					e.setCancelled(true);
				}
			}
			else
			{
				e.setCancelled(false);
			}
		}
	}

	public ItemStack stripMeta(ItemStack item, Integer amount){
		ItemMeta itm = item.getItemMeta();
		itm.setLore(null);
		item.setItemMeta(itm);
		item.setAmount(amount.intValue());
		return item;
	}
}
