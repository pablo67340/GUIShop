package com.pablo67340.guishop.handler;

import java.util.List;

import com.pablo67340.guishop.definition.ItemType;

public final class Item {

	/**
	 * The slot of this {@link Item} when presented on the GUI.
	 */
	private int slot;

	/**
	 * The name of this {@link Item} when presented on the GUI.
	 */
	private String name;

	/**
	 * The Material Name of this {@link Item}.
	 */
	private String material;

	/**
	 * The price to buy this {@link Item}.
	 */
	private double buyPrice;
	
	/**
	 * The mob ID of this item if it's a spawner {@link Item}.
	 */
	private String mobType;

	/**
	 * The amount of money given when selling this {@link Item}.
	 */
	private double sellPrice;
	
	/**
	 * The slot of this {@link Item} when presented on the GUI.
	 */
	private ItemType itemType;

	/**
	 * The enchantsments on this {@link Item}.
	 */
	private String[] enchantments;
	
	private List<String> commands;

	/**
	 * Gets the slot of this {@link Item} on the GUI.
	 * 
	 * @return the item's slot.
	 */
	public int getSlot() {
		return slot;
	}

	/**
	 * Sets the slot of this {@link Item} on the GUI.
	 * 
	 * @param slot
	 *            The slot to set.
	 */
	public void setSlot(int slot) {
		this.slot = slot;
	}

	/**
	 * Sets the enchantments of this {@link Item}
	 * 
	 * @param enchantments
	 *            The enchantments to set.
	 */
	public void setEnchantments(String[] input) {
		enchantments = input;
	}

	/**
	 * Gets the enchantments of this {@link Item}
	 * 
	 * @return the item's enchantments.
	 */
	public String[] getEnchantments() {
		return enchantments;
	}

	/**
	 * Gets the name of this {@link Item} on the GUI.
	 * 
	 * @return the item's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this {@link Item} on the GUI.
	 * 
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the ID of this {@link Item}.
	 * 
	 * @return this item's ID.
	 */
	public String getMaterial() {
		return material;
	}

	/**
	 * Sets the Material Name of this {@link Item}.
	 * 
	 * @param id
	 *            The Name to set.
	 */
	public void setMaterial(String input) {
		material = input;
	}

	/**
	 * Gets the price to buy this {@link Item}.
	 * 
	 * @return this item's buy price.
	 */
	public double getBuyPrice() {
		return buyPrice;
	}

	/**
	 * Sets the price to buy this {@link Item}.
	 * 
	 * @param buyPrice
	 *            The price to set.
	 */
	public void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}

	/**
	 * Gets the money given for selling this {@link Item}.
	 * 
	 * @return this item's sell price.
	 */
	public double getSellPrice() {
		return sellPrice;
	}

	/**
	 * Sets the money given for selling this {@link Item}.
	 * 
	 * @param sellPrice
	 *            The price to set.
	 */
	public void setSellPrice(double sellPrice) {
		this.sellPrice = sellPrice;
	}
	
	/**
	 * Gets the type of item that's being sold. {@link Item}.
	 * 
	 * @return ItemType
	 */
	public ItemType getItemType() {
		return itemType;
	}

	/**
	 * Sets the item type for this {@link Item}.
	 * 
	 * @param ItemType
	 */
	public void setType(ItemType input) {
		this.itemType = input;
	}
	
	public void setCommands(List<String> input) {
		commands = input;
	}
	
	public List<String> getCommands(){
		return commands;
	}
	
	public void setMobType(String id) {
		mobType = id;
	}
	
	public String getMobType() {
		return mobType;
	}
	
	public Boolean isMobSpawner() {
		return mobType != null;
	}

}
