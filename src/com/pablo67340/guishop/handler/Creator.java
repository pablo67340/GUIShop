package com.pablo67340.guishop.handler;

import java.util.*;

import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopDef;

import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.XMaterial;

import lombok.Getter;

public final class Creator {

	private final Player player;
	@Getter
	public Chest chest;
	public String name;
	private List<String> lore = new ArrayList<>(2);
	@Getter
	private Shop shop;
	@Getter
	private ShopDef shopDef;

	public Creator(Player p) {
		this.player = p;
	}

	/**
	 * Set the chest needed to config GUIShop
	 */
	public void setChest() {
		BlockState potentialchest = this.player.getTargetBlock(null, 100).getState();
		this.chest = (Chest) potentialchest;
		this.player.sendMessage(Config.getPrefix() + " Target chest has been set!");
	}

	/**
	 * Open the player's chest
	 */
	@SuppressWarnings("unused")
	public void openChest() {
		this.player.openInventory(chest.getInventory());
	}

	/**
	 * Clear the player's chest
	 */
	@SuppressWarnings("unused")
	public void clearChest() {
		this.chest.getInventory().clear();
	}

	/**
	 * @param input shop name
	 *              <p>
	 *              Set the current edited shop
	 */
	public void setShopName(String input) {
		if (Main.getINSTANCE().getShops().containsKey(input)) {
			shopDef = Main.getINSTANCE().getShops().get(input);
		} else {
			shopDef = new ShopDef(input, input, "", new ArrayList<>(), ItemType.SHOP, "AIR");
		}
		this.name = input;
		this.player.sendMessage(Config.getPrefix() + " Shop name set!");
	}

	/**
	 * @param price Price
	 *              <p>
	 *              Set an item's buy price
	 */
	@SuppressWarnings("deprecation")
	public void setPrice(Double price) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = this.player.getInventory().getItemInMainHand();
		} else {
			item = this.player.getItemInHand();
		}

		this.player.sendMessage(Config.getPrefix() + " Price set: " + price);
	}

	/**
	 * @param sell Sell value
	 *             <p>
	 *             Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public void setSell(Double sell) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = this.player.getInventory().getItemInMainHand();
		} else {
			item = this.player.getItemInHand();
		}

		this.player.sendMessage(Config.getPrefix() + " Sell value set: " + sell);
	}

	/**
	 * @param name Name Set the item's name
	 */
	@SuppressWarnings("deprecation")
	public void setName(String name) {
		ItemStack item;
		if (XMaterial.isNewVersion()) {
			item = this.player.getInventory().getItemInMainHand();
		} else {
			item = this.player.getItemInHand();
		}
		ItemMeta im = item.getItemMeta();
		assert im != null;
		if (im.getLore() == null) {
			this.lore.set(0, " ");
			this.lore.set(1, " ");
			this.lore.set(2, " ");
		}
		this.lore.set(2, name);
		im.setLore(lore);
		item.setItemMeta(im);
		this.player.sendMessage(Config.getPrefix() + " name value set: " + name);
	}

	public void loadShop() {
		this.shop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(), shopDef.getLore());
		this.shop.setIsEditor(true);
		this.shop.loadItems();
	}

	public void saveShop() {

	}

	public void openShop() {
		shop.open(player);
	}

}