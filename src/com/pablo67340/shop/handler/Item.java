package com.pablo67340.shop.handler;

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
	 * The ID of this {@link Item}.
	 */
	private int id;

	/**
	 * The Data of this {@link Item}.
	 */
	private int data;

	/**
	 * The price to buy this {@link Item}.
	 */
	private double buyPrice;

	/**
	 * The amount of money given when selling this {@link Item}.
	 */
	private double sellPrice;

	/**
	 * The enchantsments on this {@link Item}.
	 */
	private String[] enchantments;

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
	public int getId() {
		return id;
	}

	/**
	 * Sets the ID of this {@link Item}.
	 * 
	 * @param id
	 *            The ID to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Sets the Data of this {@link Item}.
	 * 
	 * @param id
	 *            The Data to set.
	 */

	public void setData(int data) {
		this.data = data;
	}

	/**
	 * Gets the data for this {@link Item}.
	 * 
	 * @return this item's data.
	 */
	public int getData() {
		return data;
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

}
