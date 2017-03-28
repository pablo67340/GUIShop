package com.pablo67340.shop.listener;



import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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

		if (command.contains("guishop")){
			e.setCancelled(true);
			String preArgs = StringUtils.substringAfter(command, "guishop ");
			String[] args = preArgs.split(" ");
			if (args[0].equalsIgnoreCase("start")){
				player.sendMessage(Utils.getPrefix()+" Entered creator mode!");
				Main.CREATOR.put(player.getName(), new Creator(player));
			}
			if (args[0].equalsIgnoreCase("setchest")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setChest();
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				}
			}
			if (args[0].equalsIgnoreCase("setname")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setShopName(args[1]);
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}
			if (args[0].equalsIgnoreCase("saveshop")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).saveShop();
					// TODO: Save shop method
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}
			if (args[0].equalsIgnoreCase("p")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setPrice(Integer.parseInt(args[1]));
					// TODO: Set price
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}
			if (args[0].equalsIgnoreCase("s")){
				if (Main.CREATOR.containsKey(player.getName())){
					Main.CREATOR.get(player.getName()).setSell(Integer.parseInt(args[1]));
					// TODO: Set price
				}else{
					player.sendMessage(Utils.getPrefix()+" You need to start a creator session!");
				} 
			}
			if (args[0].equalsIgnoreCase("reload")){
				if (player.hasPermission("guishop.reload") || player.isOp()){
					Main.INSTANCE.reloadConfig();
					Main.INSTANCE.reloadCustomConfig();
					Main.INSTANCE.loadDefaults();
					Shop.loadShops();

					for (Player p : Bukkit.getOnlinePlayers()){
						Main.MENUS.get(p.getName()).load();
					}
					player.sendMessage("§aGUIShop has been reloaded!");
				}
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
										returnedItems = player.getInventory().addItem(new ItemStack(item.getId(), quantity, (short)item.getData()));
									}else{
										returnedItems = player.getInventory().addItem(new ItemStack(item.getId(), quantity));
									}
								}else{
									ItemStack spawner = new ItemStack(item.getId(), quantity);
									SilkUtil su = SilkUtil.hookIntoSilkSpanwers();
									su.setSpawnerType(spawner, (short)item.getData(), "");
									returnedItems = player.getInventory().addItem(new ItemStack[]{su.setSpawnerType(spawner, (short)item.getData(), String.valueOf(Spawners.getMobName(item.getData())) + " Spawner")});
								}



								double priceToPay;

								/*
								 * If the map is empty, then the items purchased
								 * don't overflow the player's inventory. Otherwise,
								 * we need to reimburse the player (subtract it
								 * from priceToPay).
								 */
								if (returnedItems.isEmpty()) {
									priceToPay = item.getBuyPrice() * quantity;
								} else {
									double priceToReimburse = 0D;

									for (ItemStack i : returnedItems.values()) {
										priceToReimburse += item.getBuyPrice() * i.getAmount();
									}

									priceToPay = item.getBuyPrice() * quantity - priceToReimburse;
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
							}

							e.setCancelled(true);
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
					if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',Main.INSTANCE.getConfig().getString("sign-title")))){
						if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use") || player.isOp()){
							Main.MENUS.get(player).open();
							e.setCancelled(true);
						}else{
							player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getConfig().getString("no-permission")));
							e.setCancelled(true);
						}

					}
				}
			}
		}
	}

}
