package com.pablo67340.guishop.util;

import java.util.Map;

import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.pablo67340.guishop.handler.Price;

public class ShopData {

	private Map<Integer, Price> cachedPricetable;

	private Pane cachedPane;

	public Map<Integer, Price> getCachedPricetable() {
		return cachedPricetable;
	}

	public Pane getCachedPane() {
		return cachedPane;
	}

	public void setCachedPricetable(Map<Integer, Price> input) {
		this.cachedPricetable = input;
	}

	public void setCachedPane(Pane input) {
		this.cachedPane = input;
	}
	
	public ShopData(Pane inputPane, Map<Integer, Price> inputPricetable) {
		this.cachedPane = inputPane;
		this.cachedPricetable = inputPricetable;
	}

}
