package com.pablo67340.guishop.api;

public interface DynamicPriceProvider {
	
	double calculateBuyPrice(String item, int quantity, double staticBuyPrice, double staticSellPrice);
	
	double calculateSellPrice(String item, int quantity, double staticBuyPrice, double staticSellPrice);
	
	void buyItem(String item, int quantity);
	
	void sellItem(String item, int quantity);
	
}
