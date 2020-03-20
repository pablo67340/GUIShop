package com.pablo67340.guishop.listenable;

import java.util.Objects;

import org.bukkit.ChatColor;

import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;

import com.github.stefvanschie.inventoryframework.Gui;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;

import com.pablo67340.guishop.definition.MobType;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;

public final class Sell {

	private Gui GUI;

	/**
	 * Open the {@link Sell} GUI.
	 */
	void open(Player player) {
		GUI = new Gui(Main.getINSTANCE(), 6, ChatColor.translateAlternateColorCodes('&', "Menu &f> &rSell"));
		GUI.setOnClose(this::onSellClose);
		StaticPane pane = new StaticPane(0, 0, 9, 6);
		GUI.addPane(pane);
		GUI.show(player);
	}

	/**
	 * Sell items inside the {@link Sell} GUI.
	 */
	@SuppressWarnings("deprecation")
	public void sell(Player player) {

		double moneyToGive = 0;
		boolean couldntSell = false;
		int countSell = 0;
		for (ItemStack item : GUI.getInventory().getContents()) {
			Object data;
			if (item == null) {
				continue;
			}

			Double sellPrice;

			if (item.getType().name().equals("SPAWNER") /* 1.13+ */
			        || item.getType().name().equals("MOB_SPAWNER") /* 1.7 - 1.12 */
			   ) {

				NBTTagCompound cmp = ItemNBTUtil.getTag(item);
				data = MobType.valueOf(cmp.getString("EntityId"));

				if (!Main.getINSTANCE().getPRICETABLE().containsKey(item.getType().toString() + ":" + data)) {
					player.getInventory().addItem(item);
					player.sendMessage(Config.getPrefix() + Config.getCantSell());
					continue;

				}

				sellPrice = Main.getINSTANCE().getPRICETABLE().get(item.getType().toString() + ":" + data)
						.getSellPrice();

			} else {
				if (!Main.getINSTANCE().getPRICETABLE().containsKey(item.getType().toString())) {
					countSell += 1;
					couldntSell = true;
					player.getInventory().addItem(item);
					continue;
				}

				sellPrice = Main.getINSTANCE().getPRICETABLE().get(item.getType().toString()).getSellPrice();

			}

			Integer quantity = item.getAmount();

			moneyToGive += quantity * sellPrice;

		}

		if (couldntSell) {
			player.sendMessage(Config.getPrefix() + " " + Config.getCantSell().replace("{count}", countSell + ""));
		}

		Double moneyToGiveRounded = (double) Math.round(moneyToGive * 100) / 100;

		if (moneyToGiveRounded > 0) {
			Main.getECONOMY().depositPlayer(player.getName(), moneyToGiveRounded);
			
			player.sendMessage(Config.getSold() + moneyToGiveRounded + Config.getAdded());
		}
		GUI.getInventory().clear();
	}

	private void onSellClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		sell(player);
	}

}
