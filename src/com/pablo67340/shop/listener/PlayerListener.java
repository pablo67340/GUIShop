package com.pablo67340.shop.listener;



import java.util.*;

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

import de.dustplanet.util.SilkUtil;

public final class PlayerListener implements Listener {

	/**
	 * An instance of a {@link PlayerListener} that will be
	 * used to handle this specific object reference from
	 * other classes, even though methods here will be static.
	 */
	public static final PlayerListener INSTANCE = new PlayerListener();

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoinLoad(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		Main.MENUS.put(player.getName(), new Menu(player));
		Main.SELLS.put(player.getName(), new Sell(player));
	}


	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player player = e.getPlayer();

		String command = e.getMessage().substring(1);
		if (Main.BUY_COMMANDS.contains(command)) {
			Main.MENUS.get(player.getName()).open();
			e.setCancelled(true);
			return;
		}

		if (Main.SELL_COMMANDS.contains(command)) {
			Main.SELLS.get(player.getName()).open();
			e.setCancelled(true);
			return;
		}

		if (command.contains("guishop") || command.contains("gs")){
			e.setCancelled(true);
			String preArgs = "";
			if (command.contains("guishop")){
				preArgs = StringUtils.substringAfter(command, "guishop ");
			}else if (command.contains("gs")){
				preArgs = StringUtils.substringAfter(command, "gs ");
			}
			String[] args = preArgs.split(" ");
			if (args[0].equalsIgnoreCase("start")){
				if (player.hasPermission("guishop.creator")){
					player.sendMessage(Utils.getPrefix()+" Entered creator mode!");
					Main.CREATOR.put(player.getName(), new Creator(player));
				}else{
					player.sendMessage(Utils.getPrefix()+" No permission!");
				}
			}else if (args[0].equalsIgnoreCase("stop")){
				if (player.hasPermission("guishop.creator")){
					player.sendMessage(Utils.getPrefix()+" Exited creator mode!");
					Main.CREATOR.remove(player.getName());
				}else{
					player.sendMessage(Utils.getPrefix()+" No permission!");
				}
			}else if (args[0].equalsIgnoreCase("setchest")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setChest();
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				}
			}else if (args[0].equalsIgnoreCase("setshopname")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setShopName(args[1]);
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}else if (args[0].equalsIgnoreCase("saveshop")){
				if (Main.CREATOR.containsKey(player.getName())){
					if (Main.CREATOR.get(player.getName()).name == null){
						player.sendMessage(Utils.getPrefix()+"Set a shop name!");
					}else if (Main.CREATOR.get(player.getName()).chest == null){
						player.sendMessage(Utils.getPrefix()+" Set a chest location!");
					}else{
						Main.CREATOR.get(player.getName()).saveShop();
					}
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}else if (args[0].equalsIgnoreCase("p")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setPrice(Double.parseDouble(args[1]));
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}else if (args[0].equalsIgnoreCase("s")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setSell(Double.parseDouble(args[1]));
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}else if (args[0].equalsIgnoreCase("n")){
				if (Main.CREATOR.containsKey(player.getName())){
					String name = "";
					for (String str : args){
						if (!str.equalsIgnoreCase(args[0])){
							name += str +" ";
						}
					}
					Main.CREATOR.get(player.getName()).setName(name.trim());
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}else if (args[0].equalsIgnoreCase("loadshop")){
				if (Main.CREATOR.containsKey(player.getName())){
					if (Main.CREATOR.get(player.getName()).name == null){
						player.sendMessage(Utils.getPrefix()+"Set a shop name!");
					}else if (Main.CREATOR.get(player.getName()).chest == null){
						player.sendMessage(Utils.getPrefix()+" Set a chest location!");
					}else{
						Main.CREATOR.get(player.getName()).loadShop();
					}
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}else if (args[0].equalsIgnoreCase("reload")){
				if (player.hasPermission("guishop.reload") || player.isOp()){
					Main.INSTANCE.reloadConfig();
					Main.INSTANCE.createFiles();
					Main.INSTANCE.loadDefaults();
					Shop.loadShops();

					for (Player p : Bukkit.getOnlinePlayers()){
						Main.MENUS.get(p.getName()).load();
					}
					player.sendMessage("§aGUIShop has been reloaded!");
				}
			}else{
				player.sendMessage("        Proper Usage:        ");
				player.sendMessage("/guishop start - Starts creator session");
				player.sendMessage("/guishop setchest - Sets chest location to chest you look at");
				player.sendMessage("/guishop setshopname - Sets the current shop you're working in");
				player.sendMessage("/guishop loadshop - Loads current shop into chest!");
				player.sendMessage("/guishop p - Set item in hand's buy price");
				player.sendMessage("/guishop s - Set item in hand's sell price");
				player.sendMessage("/guishop n - Set item in hand's name");
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onShopClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			Player player = (Player) e.getWhoClicked();

			if (Main.HAS_SELL_OPEN.contains(player.getName())) {
				if (e.getSlot() < 0 || e.getSlot() >= Sell.ROWS * Sell.COLS) {
					e.setCancelled(true);
					return;
				}

				/*
				 * If the player clicks on an item in their
				 * inventory that isn't sellable, then cancel
				 * the event.
				 */
				if (e.getCurrentItem().getDurability() != 0){
					if (e.getClickedInventory() == player.getInventory() && e.getCurrentItem().getType() != Material.AIR && !Main.PRICES.containsKey(e.getCurrentItem().getTypeId()+":"+e.getCurrentItem().getDurability())) {

						player.sendMessage(Utils.getCantSell());
						e.setCancelled(true);
						return;
					}
				}else{
					if (e.getClickedInventory() == player.getInventory() && e.getCurrentItem().getType() != Material.AIR && !Main.PRICES.containsKey(Integer.toString(e.getCurrentItem().getTypeId()))) {

						player.sendMessage(Utils.getCantSell());
						e.setCancelled(true);
						return;
					}
				}
			} else 

				/*
				 * If the player has the menu open.
				 */
				if (Main.HAS_MENU_OPEN.contains(player.getName())) {
					/*
					 * If the player clicks somewhere where
					 * we don't want them to, cancel the event.
					 */
					if (!Main.SHOPS.containsKey(e.getSlot())) {
						e.setCancelled(true);
						return;
					}

					/*
					 * If the player clicks on an empty slot, 
					 * then cancel the event.
					 */
					if (e.getCurrentItem() != null) {
						if (e.getCurrentItem().getType() == Material.AIR) {
							e.setCancelled(true);
							return;
						}
					}

					/*
					 * If the player clicks in their own inventory,
					 * we want to cancel the event.
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
						/*
						 * If the player clicks on an empty slot, 
						 * then cancel the event.
						 */
						if (e.getCurrentItem() != null) {
							if (e.getCurrentItem().getType() == Material.AIR) {
								e.setCancelled(true);
								return;
							}
						}

						/*
						 * If the player clicks in their own inventory,
						 * we want to cancel the event.
						 * 
						 * // TODO: Make an exception for selling items.
						 */
						if (e.getClickedInventory() == player.getInventory()) {
							e.setCancelled(true);
							return;
						}

						Shop shop = Main.HAS_SHOP_OPEN.get(player.getName());

						if (e.getSlot() >= 0 && e.getSlot() < shop.getGUI().getSize()) {
							/*
							 * If the player clicks the 'back' button,
							 * then open the menu. Otherwise, attempt
							 * to purchase the item they click on.
							 */
							if (e.getSlot() == shop.getGUI().getSize() - 1) {
								shop.closeAndOpenMenu(player);
							} else {
								/*
								 * If the player has enough money to
								 * purchase the item, then allow them to.
								 */
								Item item = shop.getItems()[e.getSlot()];

								int quantity = (int) (Main.getEconomy().getBalance(player.getName()) / item.getBuyPrice());

								quantity = Math.min(quantity, e.isShiftClick() ? new ItemStack(item.getId()).getMaxStackSize() : 1);

								if (quantity == 0) {
									player.sendMessage(Utils.getPrefix() + " " + Utils.getNotEnoughPre() + 
											item.getBuyPrice() + Utils.getNotEnoughPost());
									player.setItemOnCursor(new ItemStack(Material.AIR));
									e.setCancelled(true);
									return;
								}

								Map<Integer, ItemStack> returnedItems;

								if (item.getId() != 52){

									if (item.getData() > 0){
										ItemStack itemStack = new ItemStack(item.getId(), quantity, (short)item.getData());

										for (String enc : item.getEnchantments()){
											String enchantment = StringUtils.substringBefore(enc, ":");
											String level = StringUtils.substringAfter(enc, ":");
											itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment), Integer.parseInt(level));
										}

										itemStack.setAmount(item.getQty());

										if (e.isShiftClick()) itemStack.setAmount(1);


										returnedItems = player.getInventory().addItem(itemStack);
									}else{
										ItemStack itemStack = new ItemStack(item.getId(), quantity);
										if (item.getEnchantments() != null){
											for (String enc : item.getEnchantments()){
												String enchantment = StringUtils.substringBefore(enc, ":");
												String level = StringUtils.substringAfter(enc, ":");
												itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment), Integer.parseInt(level));
											}
										}
										itemStack.setAmount(item.getQty());
										if (e.isShiftClick()) itemStack.setAmount(1);
										returnedItems = player.getInventory().addItem(itemStack);
									}
								}else{
									ItemStack spawner = new ItemStack(item.getId(), quantity);
									System.out.println("Item Data:"+item.getData());
									returnedItems = player.getInventory().addItem(new ItemStack[]{Main.getInstance().su.setSpawnerType(spawner, (short)item.getData(), String.valueOf(Spawners.getMobName(item.getData())) + " Spawner")});
								}



								double priceToPay = 0;

								/*
								 * If the map is empty, then the items purchased
								 * don't overflow the player's inventory. Otherwise,
								 * we need to reimburse the player (subtract it
								 * from priceToPay).
								 */
								if (returnedItems.isEmpty()) {
									priceToPay = item.getBuyPrice();
									if (e.isShiftClick()) priceToPay = item.getBuyPrice() / item.getQty();
								} else {
									double priceToReimburse = 0D;

									if (!e.isShiftClick()){

										for (ItemStack i : returnedItems.values()) {
											priceToReimburse += item.getBuyPrice();
										}

										priceToPay = item.getBuyPrice() - priceToReimburse;

									}else{

										for (ItemStack i : returnedItems.values()) {
											priceToReimburse += (item.getBuyPrice() / item.getQty());
										}
										priceToPay = (item.getBuyPrice() / item.getQty()) - priceToReimburse;

									}
								}

								if (Main.getEconomy().withdrawPlayer(player.getName(), priceToPay).transactionSuccess()) {
									if (Utils.isSoundEnabled()){
										try{
											player.playSound(player.getLocation(), Sound.valueOf(Utils.getSound()), 1, 1);
										}catch(Exception ex){
											System.out.println("[GUIShop] Incorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
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

	ArrayList<String> skipOne = new ArrayList<>();

	@EventHandler(priority = EventPriority.HIGH)
	public void onClose(InventoryCloseEvent e) {
		final String playerName = e.getPlayer().getName();
		if (Utils.getEscapeOnly()){
			if (Main.HAS_SHOP_OPEN.containsKey(playerName)){
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
			}else if (Main.HAS_MENU_OPEN.contains(playerName)){
				Main.HAS_MENU_OPEN.remove(playerName);
				Main.HAS_SHOP_OPEN.remove(playerName);
				return;

			}

			if (Main.HAS_SELL_OPEN.remove(playerName)) {
				Main.SELLS.get(playerName).sell();
				Main.HAS_SELL_OPEN.remove(playerName);
			}
			return;

		}else{
			Main.HAS_MENU_OPEN.remove(playerName);
			Main.HAS_SHOP_OPEN.remove(playerName);

			if (Main.HAS_SELL_OPEN.remove(playerName)) {
				Main.SELLS.get(playerName).sell();
				Main.HAS_SELL_OPEN.remove(playerName);
			}
			return;
		}

	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		Block block = e.getClickedBlock();
		if (block != null){
			if (block.getState() != null){
				if(block.getState() instanceof Sign) {
					Sign sign = (Sign) block.getState();
					String line1 = ChatColor.translateAlternateColorCodes('&',sign.getLine(0));
					if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',Main.INSTANCE.getMainConfig().getString("sign-title")))){
						if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use") || player.isOp()){
							Main.MENUS.get(player).open();
							e.setCancelled(true);
						}else{
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getMainConfig().getString("no-permission")));
							e.setCancelled(true);
						}

					}
				}
			}
		}
	}

}
