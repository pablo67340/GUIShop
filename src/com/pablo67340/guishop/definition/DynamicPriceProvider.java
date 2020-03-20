package com.pablo67340.guishop.definition;

public interface DynamicPriceProvider {
	
	double calculateBuyPrice(String item, int quantity, Price staticPricing);
	
	double calculateSellPrice(String item, int quantity, Price staticPricing);
	
	void buyItem(int quantity);
	
	void sellItem(int quantity);
	
}
