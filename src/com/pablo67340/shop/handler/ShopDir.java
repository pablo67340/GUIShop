package com.pablo67340.shop.handler;

import java.util.List;

public class ShopDir {
	
	public String name, shop, description;
	
	public List<String> lore;
	
	public ShopDir(String shop, String name, String description, List<String> lore) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
	}
	
	public String getName() {
		return name;
	}
	
	public String getShop() {
		return shop;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<String> getLore(){
		return lore;
	}

}
