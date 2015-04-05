package com.pablo67340.shop;


import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Shops {


	protected Map<String, ItemStack[]> shopss = new HashMap<String, ItemStack[]>();

	public Shops(){

	}

	public boolean saveShop(String input, Inventory shop){
		if (GUIShop.verbose){
			System.out.println("Attempting to save shop and run checks: "+input);
		}
		if (shopss.keySet().isEmpty()){
			if (GUIShop.verbose){
				System.out.println("Shop didnt exist upon saving, SAVED: "+input);	
			}
			shopss.put(input, shop.getContents());
			return true;
		}
		for(String key: shopss.keySet()){
			if (key.equalsIgnoreCase(input)){
				if (GUIShop.verbose){
					System.out.println("Hashmap valid, Key exist. Shop NOT saved! Key NOT Stored for shop: "+input);
				}

				return false;

			}else{
				if (GUIShop.verbose){
					System.out.println("Couldnt detect if shop existed, Running bypass to avoide further errors!");
				}

				return true;
			}
		}
		return true;

	}

	public boolean isSaved(String input){
		if (GUIShop.verbose){
			System.out.println("Attempting to check isSaved on: "+input );
		}
		if (shopss.keySet().isEmpty()){
			if (GUIShop.verbose){
				System.out.println("HashMap keys didnt exist, Returning false!");
			}
			return false;
		}
		for(String key: shopss.keySet()){
			System.out.println("Ran key: "+key);
			if (key.equalsIgnoreCase(input)){
				if (GUIShop.verbose){
					System.out.println("Shop didnt exist in hashmap, And wa saved for shop: "+input);
				}
				return true;
			}else{
				if (GUIShop.verbose){
					System.out.println("Shop hasnt been saved, Isnt contained in hashmap?");
				}
				return false;
			}
		}
		if (GUIShop.verbose){
			System.out.println("Failed to check if shop was saved, Running bypass code to prevent further errors!");
		}

		return true;
	}

	public ItemStack[] getShop(String input){
		if (GUIShop.verbose){
			System.out.println("Getting items for SAVED Shop!");
		}
		return shopss.get(input);}
}

