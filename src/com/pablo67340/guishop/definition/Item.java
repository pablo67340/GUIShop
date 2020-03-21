package com.pablo67340.guishop.definition;

import java.util.List;

import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;

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
	
	@Getter
	@Setter
	private int configSlot;

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
	
	public Boolean hasMobType() {
		if (mobType == null) {
			return false;
		}
		if (mobType.equalsIgnoreCase("")) {
			return false;
		}
		return true;
	}

	/**
	 * Assuming the buy price is an integer or a double, get it as a double.
	 * Remember to check {@link #hasBuyPrice()} first
	 * 
	 * @return the buy price as a double
	 */
	public double getBuyPriceAsDouble() {
		return (buyPrice instanceof Double) ? (Double) buyPrice : ((Integer) buyPrice).doubleValue();
	}

	/**
	 * Assuming the sell price is an integer or a double, get it as a double.
	 * Remember to check {@link #hasSellPrice()} first
	 * 
	 * @return the sell price as a double
	 */
	public double getSellPriceAsDouble() {
		return (sellPrice instanceof Double) ? (Double) sellPrice : ((Integer) sellPrice).doubleValue();
	}
	
	/**
	 * Assumming {@link #hasBuyPrice()} = <code>true</code>,
	 * calculate the buy price taking based on the given quantity. <br>
	 * If dynamic pricing is enabled, the DynamicPriceProvider is used for calculations.
	 * Otherwise, the buy price and the quantity are simply multiplied.
	 * 
	 * @param quantity the quantity of the item
	 * @return the calculated buy price
	 */
	public double calculateBuyPrice(int quantity) {
		// sell price must be defined and nonzero for dynamic pricing to work
		if (Config.isDynamicPricing() && hasSellPrice()) {
			
			return Main.getDYNAMICPRICING().calculateBuyPrice(getItemString(), quantity, getBuyPriceAsDouble(),
					getSellPriceAsDouble());
		}
		// default to fixed pricing
		return getBuyPriceAsDouble() * quantity;
	}
	
	/**
	 * Assumming {@link #hasSellPrice()} = <code>true</code>,
	 * calculate the sell price taking based on the given quantity. <br>
	 * If dynamic pricing is enabled, the DynamicPriceProvider is used for calculations.
	 * Otherwise, the sell price and the quantity are simply mmultiplied.
	 * 
	 * @param quantity the quantity of the item
	 * @return the calculated sell price
	 */
	public double calculateSellPrice(int quantity) {
		// buy price must be defined for dynamic pricing to work
		if (Config.isDynamicPricing() && hasBuyPrice()) {
			
			return Main.getDYNAMICPRICING().calculateSellPrice(getItemString(), quantity, getBuyPriceAsDouble(),
					getSellPriceAsDouble());
		}
		// default to fixed pricing
		return getSellPriceAsDouble() * quantity;
	}
	
	/**
	 * Gets the lore display for this item's buy price. <br>
	 * If there is no buy price, <code>Config.getCannotBuy()</code> is returned.
	 * If free, <code>Config.getFreeLore</code> is returned.
	 * Otherwise, the buy price is calculated based on the quantity, and the
	 * lore displaying the calculated buy price is returned. Takes into
	 * account dynamic pricing, if enabled.
	 * 
	 * @param quantity the quantity of the item
	 * @return the buy price lore
	 */
	public String getBuyLore(int quantity) {
		if (hasBuyPrice()) {

			double buyPriceAsDouble = getBuyPriceAsDouble();
			if (buyPriceAsDouble != 0) {

				return Config.getBuyLore().replace("{amount}",
						Config.getCurrency() + calculateBuyPrice(quantity) + Config.getCurrencySuffix());
			}
			return Config.getFreeLore();
		}
		return Config.getCannotBuy();
	}
	
	/**
	 * Gets the lore display for this item's sell price. <br>
	 * If there is no sell price, <code>Config.getCannotSell()</code> is returned.
	 * Otherwise, the sell price is calculated based on the quantity, and the
	 * lore displaying the calculated sell price is returned. Takes into
	 * account dynamic pricing, if enabled.
	 * 
	 * @param quantity the quantity of the item
	 * @return the sell price lore
	 */
	public String getSellLore(int quantity) {
		if (hasSellPrice()) {
			return Config.getSellLore().replace("{amount}",
					Config.getCurrency() + calculateSellPrice(quantity) + Config.getCurrencySuffix());
		}
		return Config.getCannotSell();
	}
	
	/**
	 * If the item is a mob spawner, <code>getMaterial().toUpperCase
	 * + ":" + getMobType().toLowerCase()</code> is returned.
	 * Otherwise, <code>getMaterial().toUpperCase</code> is simply returned.
	 * 
	 * @return the item string representation
	 */
	public String getItemString() {
		return (isMobSpawner()) ? material.toUpperCase() + ":" + getMobType().toLowerCase() : material.toUpperCase();
	}
	
	/**
	 * Assuming the sell price is an integer or a double,
	 * create a corresponding {@link Price} for this item. <br>
	 * If this item has no buy price, the result's
	 * {@link Price#getBuyPrice()} will return <code>0</code>.
	 * 
	 * @return a price object reflecting this item's pricing
	 */
	public Price generatePricing() {
		if (hasBuyPrice()) {
			return new Price(getBuyPriceAsDouble(), getSellPriceAsDouble());
		}
		return new Price(getSellPriceAsDouble());
	}

}
