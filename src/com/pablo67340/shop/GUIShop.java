package com.pablo67340.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GUIShop extends JavaPlugin implements Listener {
	public Economy econ;
	List<String> sellitems = new ArrayList<String>();
	HashMap<Integer,Inventory> shopinv = new HashMap<Integer,Inventory>();
	String tag = this.getConfig().getString("tag");
	String title2 = this.getConfig().getString("title");
	String title = ChatColor.translateAlternateColorCodes('&', title2);
	public void onEnable(){
		sellitems.clear();
		shopinv.clear();
		this.getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy() ) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		this.saveDefaultConfig();
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
		if(sender instanceof Player){
			Player p = (Player) sender;
			if(command.getName().equalsIgnoreCase("shop") && p.hasPermission("gshop.use")){
				loadShop(p);
			}else{
				p.sendMessage("§cNo Permission!");
			}
		}
		if(command.getName().equalsIgnoreCase("shopreload")){
			if(sender.isOp()) {
				this.reloadConfig();
				sender.sendMessage("Configuration reloaded.");
			}
		}
		return false;
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	@SuppressWarnings("deprecation")
	public void loadShop(Player plyr)
	{
		int row = 9;
		int size = 5;

		

		if(title.length() > 16) {
			title.substring(0,16);
		}

		Inventory shop = Bukkit.getServer().createInventory(plyr, row*size, title);

		if(shopinv.isEmpty()) {
			for(int i=0;i<407;i++)
			{
				String name = "null";
				String data = "0";
				String qty = "1";
				String slot = "0";
				String price = "0";
				String sell = "0";

				if(this.getConfig().get(i+"") != null)
				{
					List<String> nodes = this.getConfig().getStringList(i+"");
					if(nodes != null) {
						for(int nodeapi=0;nodeapi<nodes.size();nodeapi++) {
							if(nodes.get(nodeapi).contains("slot:"))
							{
								slot = nodes.get(nodeapi).replace("slot:", "");
							}
							if(nodes.get(nodeapi).contains("name:"))
							{
								name = nodes.get(nodeapi).replace("name:", "").replace("'", "");
							}
							if(nodes.get(nodeapi).contains("price:"))
							{
								price = nodes.get(nodeapi).replace("price:", "").replace("'", "");
							}
							if(nodes.get(nodeapi).contains("data:"))
							{
								data = nodes.get(nodeapi).replace("data:", "");
							}
							if(nodes.get(nodeapi).contains("qty:"))
							{
								qty = nodes.get(nodeapi).replace("qty:", "");
							}
							if(nodes.get(nodeapi).contains("sell:"))
							{
								sell = nodes.get(nodeapi).replace("sell:", "").replace("'", "");
								if(!sellitems.contains(i+"$"+sell+")"+qty)) {
									sellitems.add(i+"$"+sell+")"+qty);
								}
							}
						}
					}
					if(Integer.parseInt(data) != 0)
					{
						ItemStack itemwithdata = new ItemStack(Material.getMaterial(i), Integer.parseInt(qty), (short)Integer.parseInt(data));
						addPrice(itemwithdata,Integer.parseInt(price),Integer.parseInt(sell));
						if(name!= "null")
						{
							ItemMeta itemmeta = itemwithdata.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
							itemwithdata.setItemMeta(itemmeta);
						}
						shop.setItem(Integer.parseInt(slot), itemwithdata);
					}else{
						if(Material.getMaterial(i) == null) {
							System.out.print(i);
						}
						if(Integer.parseInt(qty) < 1) {
							System.out.print("its 0");
						}
						ItemStack itemnodata = new ItemStack(Material.getMaterial(i), Integer.parseInt(qty));
						addPrice(itemnodata,Integer.parseInt(price),Integer.parseInt(sell));
						if(name!= "null")
						{
							ItemMeta itemmeta = itemnodata.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
							itemnodata.setItemMeta(itemmeta);
						}
						shop.setItem(Integer.parseInt(slot),itemnodata);
					}
					shopinv.put(1, shop);
					plyr.openInventory(shop);
				}
				plyr.updateInventory();
			}
		}else{
			plyr.openInventory(shopinv.get(1));
		}
	}

	public void addPrice(ItemStack item, Integer price, Integer sell) {

		ItemMeta itm = item.getItemMeta();
		List<String> itmlore = Arrays.asList("§7Price: §c$§0,§c" + price, "§7Sell: §a$§0.§a" + sell, "§8To sell, click the item in your inv.", "§9Must be the same quantity!");
		itm.setLore(itmlore);
		item.setItemMeta(itm);
	}

	public ItemStack stripMeta(ItemStack item, Integer amount) {
		ItemMeta itm = item.getItemMeta();
		itm.setLore(null);
		item.setItemMeta(itm);
		item.setAmount(amount);
		return item;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onClick(InventoryClickEvent e)
	{
		
		if(e.getWhoClicked() instanceof Player)
		{

			Player p = (Player) e.getWhoClicked();

			if(e.getInventory().getTitle().contains(title))
			{
				e.setCancelled(true);
				p.closeInventory();

				if(e.getSlot() != -999) {
					ItemStack item = e.getCurrentItem();
					if(item != null) {
						trySell(p,item);
						if(e.getInventory().getItem(e.getSlot()) != null) {
							if(item.hasItemMeta())
							{
								if(item.getItemMeta().hasLore())
								{
									if(item.getItemMeta().getLore().toString().contains("Price")) {
										int price = Integer.MAX_VALUE;
										String lorestring = ChatColor.stripColor(item.getItemMeta().getLore().toString().replace("[", "").replace("]", "").replace(",", "").replace("To sell, click the item in your inv.", "").replace("Must be the same quantity!", ""));
										lorestring = StringUtils.substringBefore(lorestring, ".");
										price = Integer.parseInt(lorestring.replace("Price: ", "").replace("Sell:", "").replace(" ", "").replace("$", ""));
										if(econ.getBalance(p.getName()) >= price) {
											EconomyResponse r = econ.withdrawPlayer(p.getName(), price);
											if(r.transactionSuccess()) {
												ItemStack dupeitem = item.clone();
												p.getInventory().addItem(stripMeta(dupeitem,dupeitem.getAmount()));
												p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§fYou purchased, §c" + item.getAmount() + " " + item.getType().toString().toLowerCase() + "§f!");
												p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§c$" + price + "§f taken from your account.");
											}else{
												double dif = price-econ.getBalance(p.getName());
												p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§fYou need §c$" + dif + "§f more money.");
											}
										}else{
											double dif = price-econ.getBalance(p.getName());
											p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§fYou need §c$" + dif + "§f more money.");
										}
									}
								}
							}
							loadShop(p);
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void trySell(Player p, ItemStack item) {
		boolean tally = false;
		if(!item.getItemMeta().hasLore()){
			if(p.getInventory().contains(item)){
				for(String str : sellitems) {
					String itemid = StringUtils.substringBefore(str, "$");
					if(item.getTypeId() == Integer.parseInt(itemid)) {
						tally = true;
						String amount = StringUtils.substringAfter(str, ")");
						if(Integer.parseInt(amount) == item.getAmount()) {
							String preprice = StringUtils.substringAfter(str, "$");
							String price = StringUtils.substringBefore(preprice, ")");
							p.getInventory().removeItem(item);
							EconomyResponse r = econ.depositPlayer(p.getName(), Integer.parseInt(price));
							if(r.transactionSuccess()) {
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§fYou sold, §c" + amount + " " + item.getType().toString().toLowerCase() + "§f!");
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§a$" + price + "§f added to your account.");
							}else{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', tag) + " " + "§fSomething went wrong, contact an admin.");
							}
						}else{
							int dif = Integer.parseInt(amount);
							p.sendMessage(tag + " " + "§fPlease sell in stacks of §c" + dif + " §fplease.");
						}
					}
				}
				if(tally == false){
					p.sendMessage(tag + " " + "§fSorry, you can't sell that item.");
					tally = true;
				}
			}
		}else{
			
		}
	}
}
