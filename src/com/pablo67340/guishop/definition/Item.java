package com.pablo67340.guishop.definition;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public final class Item {

	/**
	 * The name of this {@link Item} when presented on the GUI.
	 */
	@Getter
	@Setter
	private String shopName, buyName;

	@Getter
	@Setter
	private int slot;

	/**
	 * The Material of this {@link Item}.
	 */
	@Getter
	@Setter
	private String material;

	/**
	 * The price to buy this {@link Item}.
	 */
	@Getter
	@Setter
	private Object buyPrice;

	/**
	 * The mob ID of this item if it's a spawner {@link Item}.
	 */
	@Getter
	@Setter
	private String mobType;

	/**
	 * The amount of money given when selling this {@link Item}.
	 */
	@Getter
	@Setter
	private Object sellPrice;

	/**
	 * The slot of this {@link Item} when presented on the GUI.
	 */
	@Getter
	@Setter
	private ItemType itemType;

	@Getter
	@Setter
	private List<String> buyLore, shopLore;

	@Getter
	@Setter
	private List<String> commands;

	/**
	 * The enchantsments on this {@link Item}.
	 */
	@Getter
	@Setter
	private String[] enchantments;

	public Boolean hasShopName() {
		return (shopName != null ? shopName.equalsIgnoreCase("") ? false : true : false);
	}

	public Boolean hasBuyName() {
		return buyName != null;
	}

	public Boolean hasShopLore() {
		if (shopLore == null) {
			return false;
		}
		if (shopLore.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean hasBuyLore() {
		if (buyLore == null) {
			return false;
		}
		if (buyLore.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean hasEnchantments() {
		if (enchantments == null) {
			return false;
		}
		if (enchantments[0].equalsIgnoreCase("")) {
			return false;
		}
		return true;
	}

	public boolean hasCommands() {
		if (commands == null) {
			return false;
		}
		if (commands.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean isMobSpawner() {
		return material.equalsIgnoreCase("SPAWNER") || material.equalsIgnoreCase("MOB_SPAWNER");
	}

	public Boolean hasSellPrice() {
		if (sellPrice == null) {
			return false;
		}

		if (sellPrice instanceof Boolean) {
			if (((Boolean) sellPrice) == false) {
				return false;
			}
		}

		if (sellPrice instanceof Double) {
			if (((Double) sellPrice) == 0.0) {
				return false;
			}
		}
		return true;
	}

	public Boolean hasBuyPrice() {
		if (buyPrice == null) {
			return false;
		}

		if (buyPrice instanceof Boolean) {
			if (((Boolean) buyPrice) == false) {
				return false;
			}
		}
		return true;
	}

}
