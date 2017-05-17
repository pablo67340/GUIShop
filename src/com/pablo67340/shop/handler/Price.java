package com.pablo67340.shop.handler;

public final class Price {

	private final double buyPrice, sellPrice;
	
	private final Integer quantity;

	public Price(double buyPrice, double sellPrice, Integer quantity) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
	}

	public double getBuyPrice() {
		return buyPrice;
	}

	public double getSellPrice() {
		return sellPrice;
	}
	
	public Integer getQuantity(){
		return quantity;
	}

}
