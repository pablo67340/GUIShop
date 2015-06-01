package com.pablo67340.shop;


import java.util.ArrayList;



import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Shops {

	protected ArrayList cached = new ArrayList();
	protected ArrayList shopn = new ArrayList();

	public Shops(){

	}

	public boolean saveShop(String input, Inventory shop){
		if (shopn.contains(input)){
			shopn.clear();
			cached.clear();
			if (GUIShop.verbose){
				System.out.println("ISSaved Failed! Flushing tables on saveShop to prevent Errors!");
			}
			return false;
		}else{
			cached.add(shop.getContents());
			shopn.add(input);
			if (GUIShop.verbose){
				System.out.println("Shops saved without error! Shop: "+input);
			}
			return true;
		}
	}

	public boolean isSaved(String input){
		if (shopn.isEmpty()){
			if (GUIShop.verbose){
				System.out.println("HashMap keys didnt exist, Returning false! Shop: "+input);
			}
			return false;
		}else{
			if (shopn.contains(input)){
				if (GUIShop.verbose){
					System.out.println("Shop Existed in Array, Loading inventory: "+input);
				}
				return true;
			}
			if (GUIShop.verbose){
				System.out.println("No Condition could be met, Skipping method to avoid error! Shop: "+input);
			}
			return false;
		}
	}

	public ItemStack[] getShop(String input){
		if (GUIShop.verbose){
			System.out.println("Getting items for SAVED Shop!");
		}
		for (int i = 0; i < shopn.size(); i++)
		{
			String item = (String) shopn.get(i);
			if (GUIShop.verbose){
				System.out.println("Comparing item: "+item +" to "+input);
			}
			if (input.equals(item))
			{
				if (GUIShop.verbose){
					System.out.println("Inventory found! Loading!");
				}
				return (ItemStack[]) cached.get(i);
			}
		} 
		return null;
	}

	public boolean flushData(){
		if (shopn.isEmpty()){
			return false;
		}else{
			cached.clear();
			shopn.clear();
			return true;
		}
	}
}

