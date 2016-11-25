package com.pablo67340.shop.handler;

public final class Price {

	private final double buyPrice, sellPrice;

	public Price(double buyPrice, double sellPrice) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
	}

	public double getBuyPrice() {
		return buyPrice;
	}

	public double getSellPrice() {
		return sellPrice;
	}

}
