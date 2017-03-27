package com.pablo67340.shop.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.shop.main.Main;



public final class Creator {

	private final Player player;
	private Chest chest;
	private String name;
	private ArrayList<ItemStack> items = new ArrayList<>();
	private List<String> lore = new ArrayList<>(2);

	public Creator(Player p){
		this.player = p;
		this.lore.add(" ");
		this.lore.add(" ");
	}

	@SuppressWarnings("deprecation")
	public void setChest(){
		BlockState potentialchest = this.player.getTargetBlock((HashSet<Byte>)null, 100).getState();
		this.chest = (Chest) potentialchest;
		this.player.sendMessage(Utils.getPrefix()+" Target chest has been set!");
	}

	public void openChest(){
		this.player.openInventory(chest.getInventory());
	}

	public void clearChest(){
		this.chest.getInventory().clear();
	}

	public void setShopName(String input){
		this.name = input;
		this.player.sendMessage(Utils.getPrefix()+" Shop name set!");
	}

	@SuppressWarnings("deprecation")
	public void setPrice(Integer price){
		ItemStack item = this.player.getItemInHand();
		ItemMeta im = item.getItemMeta();
		if (im.getLore()==null){
			this.lore.set(0, " ");
			this.lore.set(1, " ");
		}
		this.lore.set(0, price.toString());
		im.setLore(lore);
		item.setItemMeta(im);
		this.player.sendMessage(Utils.getPrefix()+" Price set: "+price);
	}

	@SuppressWarnings("deprecation")
	public void setSell(Integer sell){
		ItemStack item = this.player.getItemInHand();
		ItemMeta im = item.getItemMeta();
		if (im.getLore()==null){
			this.lore.set(0, " ");
			this.lore.set(1, " ");
		}
		this.lore.set(1, sell.toString());
		im.setLore(lore);
		item.setItemMeta(im);
		this.player.sendMessage(Utils.getPrefix()+" Sell value set: "+sell);
	}

	// Line 82 is giving null. Check if chest is returning items inside of it
	// If so, find a new way to check if item has durability

	public void saveShop(){
		Integer index = -1;
		for (ItemStack item : this.chest.getInventory().getContents()){
			if (item!=null){
				System.out.println("Item count: "+this.chest.getInventory().getContents().length);
				index+=1;
				//Main.INSTANCE.getCustomConfig().getConfigurationSection(this.name+"."+index)
				HashMap<String, String> id = new HashMap<String, String>();
				HashMap<String, Integer> buyPrice = new HashMap<String, Integer>();
				HashMap<String, Integer> sellPrice = new HashMap<String, Integer>();
				HashMap<String, Integer> slot = new HashMap<String, Integer>();
				
				
				if (item.getDurability() != 0){
					id.put("id", item.getTypeId()+":"+item.getDurability());
					
				}else{

					id.put("id", item.getTypeId()+"");
					

				}

				slot.put("slot", index);
				buyPrice.put("buy-price", Integer.parseInt(item.getItemMeta().getLore().get(0)));

				if (item.getItemMeta().getLore().size()==2){
					sellPrice.put("sell-price", Integer.parseInt(item.getItemMeta().getLore().get(1)));

				}
				

				try {
					Main.INSTANCE.getCustomConfig().save(Main.customConfigFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				System.out.println("Material was null: "+index);
			}
		}
	}

}
