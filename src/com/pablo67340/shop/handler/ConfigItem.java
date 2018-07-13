package com.pablo67340.shop.handler;

import org.apache.commons.lang.StringUtils;

public class ConfigItem {

	/**
	 * The name of this {@link Item}
	 */
	private String name;

	/**
	 * The shop name of this {@link Item}
	 */
	private String shop;

	/**
	 * The Item TypeID of this {@link Item}
	 */
	private String id;

	/**
	 * The description of this {@link Item}
	 */
	private String description;

	/**
	 * The slot of this {@link Item}
	 */
	private int slot;

	/**
	 * The buy price of this {@link Item}
	 */
	private Double buy;

	/**
	 * The sell value of this {@link Item}
	 */
	private Double sell;

	/**
	 * True/False if this {@link Item} is enabled in the shop.
	 */
	private Boolean enabled;

	/**
	 * True/False if this {@link Item} is a {@link Menu} item.
	 */
	private Boolean isMenu;

	/**
	 * Set the {@link Item}'s name.
	 * 
	 * @param Name
	 */
	public void setName(String input) {
		name = input;
	}

	/**
	 * Get the {@link Item}'s name.
	 * 
	 * @return Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the {@link Item}'s shop name.
	 * 
	 * @param ShopName
	 */
	public void setShop(String input) {
		shop = input;
	}

	/**
	 * Get the {@link Item}'s shop name.
	 * 
	 * @return ShopName
	 */
	public String getShop() {
		return shop;
	}

	/**
	 * Set the {@link Item}'s description.
	 * 
	 * @param description
	 */
	public void setDescription(String input) {
		description = input;
	}

	/**
	 * Get the {@link Item}'s description.
	 * 
	 * @return Description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the {@link Item}'s enabled value.
	 * 
	 * @param Enabled
	 */
	public void setEnabled(Boolean input) {
		enabled = input;
	}

	/**
	 * Get the {@link Item}'s enabled value.
	 * 
	 * @return isEnabled
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * Set the {@link Item}'s menu boolean value.
	 * 
	 * @param isMenu
	 */
	public void setIsMenu(Boolean input) {
		isMenu = input;
	}

	/**
	 * Get the {@link Item}'s menu value.
	 * 
	 * @return isMenu
	 */
	public Boolean isMenu() {
		return isMenu;
	}

	/**
	 * Set the {@link Item}'s slot.
	 * 
	 * @param Slot
	 */
	public void setSlot(Integer input) {
		slot = input;
	}

	/**
	 * Get the {@link Item}'s slot number.
	 * 
	 * @return Slot
	 */
	public Integer getSlot() {
		return slot;
	}

	/**
	 * Set the {@link Item}'s Item TypeID.
	 * 
	 * @param ItemID
	 */
	public void setID(String input) {
		id = input;
	}

	/**
	 * Get the {@link Item}'s TypeID.
	 * 
	 * @return TypeID
	 */
	public String getID() {
		return StringUtils.substringBefore(id, ":");
	}

	/**
	 * Get the {@link Item}'s DataID.
	 * 
	 * @return DataID
	 */
	public String getData() {
		return StringUtils.substringAfter(id, ":");
	}

	/**
	 * Set the {@link Item}'s Buy price.
	 * 
	 * @param BuyPrice
	 */
	public void setBuy(Double input) {
		buy = input;
	}

	/**
	 * Get the {@link Item}'s buy price.
	 * 
	 * @return BuyPrice
	 */
	public Double getBuy() {
		return buy;
	}

	/**
	 * Set the {@link Item}'s sell value.
	 * 
	 * @param SellPrice
	 */
	public void setSell(Double input) {
		sell = input;
	}

	/**
	 * Get the {@link Item}'s sell value.
	 * 
	 * @return SellValue
	 */
	public Double getSell() {
		return sell;
	}

}
