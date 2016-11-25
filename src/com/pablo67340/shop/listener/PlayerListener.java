package com.pablo67340.shop.listener;



import java.util.*;





import org.bukkit.*;

import org.bukkit.event.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;

import org.bukkit.inventory.ItemStack;

import com.pablo67340.shop.handler.*;
import com.pablo67340.shop.main.Main;

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
					for (String str : Main.PRICES.keySet()){
						System.out.println("Item: "+str);
					}
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

								if (item.getData() > 0){
									returnedItems = player.getInventory().addItem(new ItemStack(item.getId(), quantity, (short)item.getData()));
								}else{
									returnedItems = player.getInventory().addItem(new ItemStack(item.getId(), quantity));
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
									player.sendMessage(Utils.getPurchased() + priceToPay + Utils.getTaken());
								}
							}

							e.setCancelled(true);
						}
					}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onClose(InventoryCloseEvent e) {
		String playerName = e.getPlayer().getName();
		if (Utils.getEscapeOnly()){
			if (Main.HAS_SHOP_OPEN.containsKey(playerName)){
				Main.HAS_MENU_OPEN.add(playerName);
				Main.HAS_SHOP_OPEN.remove(playerName);
				Main.MENUS.get(playerName).open();
				return;
			}else if (Main.HAS_MENU_OPEN.contains(playerName)){
				Main.HAS_MENU_OPEN.remove(playerName);
				Main.HAS_SHOP_OPEN.remove(playerName);
			}

		}else{
			Main.HAS_MENU_OPEN.remove(playerName);
			Main.HAS_SHOP_OPEN.remove(playerName);

			if (Main.HAS_SELL_OPEN.remove(playerName)) {
				Main.SELLS.get(playerName).sell();
			}
		}

	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {

	}

}
