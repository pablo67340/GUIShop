package com.pablo67340.guishop.util;

import java.util.List;
import java.util.Map;

import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.pablo67340.guishop.handler.Price;

public class ShopData {

	private Map<Integer, Price> cachedPricetable;

	private Pane cachedPane;

	private List<OutlinePane> outlinePages;

	public List<OutlinePane> getCachedOutlinePages() {
		return outlinePages;
	}

	public void setCachedOutlinePages(List<OutlinePane> input) {
		this.outlinePages = input;
	}

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

	public ShopData(Pane inputPane, Map<Integer, Price> inputPricetable, List<OutlinePane> outlinePane) {
		this.cachedPane = inputPane;
		this.cachedPricetable = inputPricetable;
		this.outlinePages = outlinePane;
	}

}
