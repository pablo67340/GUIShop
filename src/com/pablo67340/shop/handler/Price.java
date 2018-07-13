package com.pablo67340.shop.handler;

public final class Price {

	/**
	 * Buy & Sell price of the {@link Item}
	 * 
	 */
	private final double buyPrice, sellPrice;

	/**
	 * Quantity of item based on price of {@link Item}.
	 * 
	 */
	private final Integer quantity;

	/**
	 * Constructor, build entire {@link Price} at once.
	 */
	public Price(double buyPrice, double sellPrice, Integer quantity) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
	}

	/**
	 * Get the {@link Item}'s buy price.
	 * 
	 * @return BuyPrice
	 */
	public double getBuyPrice() {
		return buyPrice;
	}

	/**
	 * Get the {@link Item}'s sell value.
	 * 
	 * @return SellValue
	 */
	public double getSellPrice() {
		return sellPrice;
	}

	/**
	 * Get the {@link Item}'s quantity.
	 * 
	 * @return Quantity
	 */
	public Integer getQuantity() {
		return quantity;
	}

}
