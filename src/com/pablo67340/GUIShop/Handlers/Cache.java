package com.pablo67340.GUIShop.Handlers;


import java.util.ArrayList;




import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.pablo67340.GUIShop.Main.Main;

public class Cache {

	protected ArrayList cached = new ArrayList();
	protected ArrayList shopn = new ArrayList();

	Main plugin;
	public Cache(Main main){
		plugin = main;
	}

	public boolean saveShop(String input, Inventory shop){
		if (shopn.contains(input)){
			shopn.clear();
			cached.clear();
			if (plugin.utils.getVerbose()){
				System.out.println("ISSaved Failed! Flushing tables on saveShop to prevent Errors!");
			}
			return false;
		}else{
			cached.add(shop.getContents());
			shopn.add(input);
			if (plugin.utils.getVerbose()){
				System.out.println("Shops saved without error! Shop: "+input);
			}
			return true;
		}
	}

	public boolean isSaved(String input){
		if (shopn.isEmpty()){
			if (plugin.utils.getVerbose()){
				System.out.println("HashMap keys didnt exist, Returning false! Shop: "+input);
			}
			return false;
		}else{
			if (shopn.contains(input)){
				if (plugin.utils.getVerbose()){
					System.out.println("Shop Existed in Array, Loading inventory: "+input);
				}
				return true;
			}
			if (plugin.utils.getVerbose()){
				System.out.println("No Condition could be met, Skipping method to avoid error! Shop: "+input);
			}
			return false;
		}
	}

	public ItemStack[] getShop(String input){
		if (plugin.utils.getVerbose()){
			System.out.println("Getting items for SAVED Shop!");
		}
		for (int i = 0; i < shopn.size(); i++)
		{
			String item = (String) shopn.get(i);
			if (plugin.utils.getVerbose()){
				System.out.println("Comparing item: "+item +" to "+input);
			}
			if (input.equals(item))
			{
				if (plugin.utils.getVerbose()){
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

