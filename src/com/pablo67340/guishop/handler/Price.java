package com.pablo67340.guishop.handler;

public final class Price {

	/**
	 * Buy & Sell price of the {@link Item}
	 * 
	 */
	private final double buyPrice, sellPrice;

	/**
	 * Constructor, build entire {@link Price} at once.
	 */
	public Price(double buyPrice, double sellPrice) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
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

}
