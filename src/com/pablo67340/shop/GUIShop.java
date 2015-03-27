package com.pablo67340.shop;

import java.io.File;
import java.io.InputStream;
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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;



public class GUIShop extends JavaPlugin implements Listener{
	public Economy econ;
	List<String> sellitems = new ArrayList();
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	private File defaultConfigFile = null;
	HashMap<Integer, Inventory> shopinv = new HashMap();
	String shopn = "";
	String tag = getConfig().getString("tag");
	String title = "";
	String menun = ChatColor.translateAlternateColorCodes('&', getConfig().getString("menuname"));
	Boolean open = Boolean.valueOf(false);
	Boolean one = Boolean.valueOf(false);
	String ench = "";
	String[] enc;
	int lvl = 0;
	boolean verbose = false;


	public void onEnable()
	{
		this.sellitems.clear();
		this.shopinv.clear();
		getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy())
		{
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		saveDefaultConfig();
		verbose = getConfig().getBoolean("Verbose");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String command = event.getMessage();
		if (command.equals("/"+getConfig().getString("Command")) && player.hasPermission("guishop.use") || player.isOp()){
			loadMenu(player);
			event.setCancelled(true);
		}else{
			player.sendMessage("§cNo Permission!");
		}
	}
	private boolean setupEconomy()
	{
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		this.econ = ((Economy)rsp.getProvider());
		return this.econ != null;
	}

	public void loadMenu(Player plyr)
	{
		this.open = Boolean.valueOf(true);
		this.title = "";
		this.shopn = "";
		if ((plyr.hasPermission("guishop.use")) || (plyr.isOp())) {
			this.one = Boolean.valueOf(false);
		}
		if (getConfig().getList("DisabledWorlds").contains(plyr.getWorld().getName()))
		{
			plyr.sendMessage(ChatColor.DARK_RED + "You cannot use the shop from this world!");
		}
		else
		{
			List Ls = new ArrayList();
			Inventory chest = plyr.getPlayer().getServer().createInventory(null, getConfig().getInt("Rows") * 9, this.menun);

			int[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			int[] row2 = { 10, 11, 12, 13, 14, 15, 16, 17, 18 };
			int[] row3 = { 19, 20, 21, 23, 24, 25, 26, 27, 28 };
			for (int slot : row1) {
				if (getConfig().getString(slot + ".Enabled") == "true")
				{
					Ls.clear();
					String Name = ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Name"));
					if (getConfig().getString(slot + ".Desc") != "null") {
						Ls.add(getConfig().getString(slot + ".Desc"));
					}
					if ((plyr.hasPermission("guishop.slot." + slot)) || (plyr.isOp()))
					{
						chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(getConfig().getString(slot + ".Item")), 1), Name, Ls));
					}
					else
					{
						Ls.add("No permission");
						chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
					}
				}
			}
			if (getConfig().getInt("Rows") >= 2) {
				for (int slot : row2) {
					if (getConfig().getString(slot + ".Enabled") == "true")
					{
						Ls.clear();
						String Name = ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Name"));
						if (getConfig().getString(slot + ".Desc") != "null") {
							Ls.add(getConfig().getString(slot + ".Desc"));
						}
						if ((plyr.hasPermission("guishop.slot." + slot)) || (plyr.isOp()))
						{
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(getConfig().getString(slot + ".Item")), 1), Name, Ls));
						}
						else
						{
							Ls.add("No permission");
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
						}
					}
				}
			}
			if (getConfig().getInt("Rows") >= 3) {
				for (int slot : row3) {
					if (getConfig().getString(slot + ".Enabled") == "true")
					{
						Ls.clear();
						String Name = ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Name"));
						if (getConfig().getString(slot + ".Desc") != "null") {
							Ls.add(getConfig().getString(slot + ".Desc"));
						}
						if ((plyr.hasPermission("guishop.slot." + slot)) || (plyr.isOp()))
						{
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(getConfig().getString(slot + ".Item")), 1), Name, Ls));
						}
						else
						{
							Ls.add("No permission");
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
						}
					}
				}
			}
			plyr.openInventory(chest);
		}
	}



	public void loadShop(Player plyr, Boolean allowUnsafe)
	{
		this.open = Boolean.valueOf(true);

		this.shopinv.clear();

		int row = 9;
		int size = 5;
		if (this.title.length() > 16) {
			this.title.substring(0, 16);
		}
		Inventory shop = Bukkit.getServer().createInventory(plyr, row * size, this.title);
		if (this.shopinv.isEmpty()) {
			for (int i = 0; i < 407; i++)
			{
				String name = "null";
				String data = "0";
				String qty = "1";
				String slot = "0";
				String price = "0";
				String sell = "0";
				String item = "0";
				if (getCustomConfig().get(this.shopn + "." + i) != null)
				{

					List<String> nodes = getCustomConfig().getStringList(this.shopn + "." + i);
					if (verbose==true){
						System.out.println("Final item built: "+nodes+" Active!");
					}
					if (nodes != null) {
						for (int nodeapi = 0; nodeapi < nodes.size(); nodeapi++)
						{
							if (verbose==true){
								System.out.println("Scanning shops.yml");
							}
							if (((String)nodes.get(nodeapi)).contains("item:")){

								item = ((String)nodes.get(nodeapi)).replace("item:", "");
								if (verbose==true){
									System.out.println("Item ID found: " + item);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("slot:")) {
								slot = ((String)nodes.get(nodeapi)).replace("slot:", "");
								if (verbose==true){
									System.out.println("Slot found: " + slot);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("name:")) {
								name = ((String)nodes.get(nodeapi)).replace("name:", "").replace("'", "");
								if (verbose==true){
									System.out.println("Item name found: " + name);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("price:")) {
								price = ((String)nodes.get(nodeapi)).replace("price:", "").replace("'", "");
								if (verbose==true){
									System.out.println("Item price found: " + price);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("data:")) {
								data = ((String)nodes.get(nodeapi)).replace("data:", "");
								if (verbose==true){
									System.out.println("Data value found: " + data);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("enchantments:"))
							{
								ench = ((String)nodes.get(nodeapi)).replace("enchantments:", "").replace("'", "");
								enc = ench.split(":| ");
								if (verbose==true){
									System.out.println("Optional enchants found!: " + ench);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("qty:")) {
								qty = ((String)nodes.get(nodeapi)).replace("qty:", "");
								if (verbose==true){
									System.out.println("Item quantity found: " + qty);
								}
							}
							if (((String)nodes.get(nodeapi)).contains("sell:"))
							{
								sell = ((String)nodes.get(nodeapi)).replace("sell:", "").replace("'", "");
								if (verbose==true){
									System.out.println("Item sell price found: " + sell);
								}
								if (!this.sellitems.contains(i + "$" + sell + ")" + qty)) {
									this.sellitems.add(i + "$" + sell + ")" + qty);
								}
							}
						}
					}
					if (Integer.parseInt(data) != 0)
					{
						ItemStack itemwithdata = new ItemStack(Material.getMaterial(Integer.parseInt(item)), Integer.parseInt(qty), (short)Integer.parseInt(data));
						if (verbose==true){
							System.out.println("Adding item: "+nodes+" To inventory");
						}
						addPrice(itemwithdata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)));
						if (verbose==true){
							System.out.println("AddPrice Method Passed! ");
						}
						if (name != "null")
						{
							ItemMeta itemmeta = itemwithdata.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
							itemwithdata.setItemMeta(itemmeta);
							if (verbose==true){
								System.out.println("Item name found! Item meta added!");
							}


						}else{
							if (verbose==true){
								System.out.println("NO Item name found! Breaking!");
							}
						}


						if (!(enc==null)){
							for(int e = -1;e<enc.length;e+=2){
								if (!(e<0)){
									if (verbose==true){
										System.out.println("Enchants split into values!: ");
									}

									if (enc[e-1] == null && enc[e] == null){
										if (verbose==true){
											System.out.println("Enchantments are null!");
										}								
									}else{
										lvl = Integer.parseInt(enc[e]);
										itemwithdata.addUnsafeEnchantment(Enchantments.getByName(enc[e-1]), lvl);
										if (verbose==true){
											System.out.println("Enchant values: Enchant name: "+enc[e-1]+" Enchant Level: "+lvl);
										}
										enc[e] = null;
										enc[e-1] = null;
									}
								}
							}
						}
						if (Integer.parseInt(slot)!=44){
							shop.setItem(Integer.parseInt(slot), itemwithdata);
							ItemStack backbutton = new ItemStack(Material.getMaterial(160), 1, (short)14);
							ItemMeta itemmeta = backbutton.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Back"));
							backbutton.setItemMeta(itemmeta);
							shop.setItem(44, backbutton);
							if (verbose==true){
								System.out.println("Item with data: "+itemwithdata+" Added to shop!");
							}
						}else{
							if (verbose==true){
								System.out.println("ERROR: An Item tried to overwrite button slot!");
							}
						}



					}
					else
					{
						if (Material.getMaterial(i) == null) {
							System.out.print(i);
						}
						if (Integer.parseInt(qty) < 1) {
							System.out.print("its 0");
						}
						ItemStack itemnodata = new ItemStack(Material.getMaterial(Integer.parseInt(item)), Integer.parseInt(qty));
						addPrice(itemnodata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)));
						if (name != "null")
						{
							ItemMeta itemmeta = itemnodata.getItemMeta();
							itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
							itemnodata.setItemMeta(itemmeta);
						}

						if (!(enc==null)){
							for(int e = -1;e<enc.length;e+=2){
								if (!(e<0)){
									if (verbose==true){
										System.out.println("Enchants split into values!: ");
									}


									if (enc[e-1] == null && enc[e] == null){
										if (verbose==true){
											System.out.println("Enchantments are null!");
										}										
									}else{
										lvl = Integer.parseInt(enc[e]);
										itemnodata.addUnsafeEnchantment(Enchantments.getByName(enc[e-1]), lvl);
										if (verbose==true){
											System.out.println("Enchant values: Enchant name: "+enc[e-1]+" Enchant Level: "+lvl);
										}
										enc[e-1] = null;
										enc[e] = null;
									}

								}
							}
						}

						shop.setItem(Integer.parseInt(slot), itemnodata);
						ItemStack backbutton = new ItemStack(Material.getMaterial(160), 1, (short)14);
						ItemMeta itemmeta = backbutton.getItemMeta();
						itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Back"));
						backbutton.setItemMeta(itemmeta);
						shop.setItem(44, backbutton);
						if (verbose==true){
							System.out.println("Item with no data: "+itemnodata+" Added to shop!");
						}
					}
					this.shopinv.put(Integer.valueOf(1), shop);
					plyr.openInventory(shop);
				}
				plyr.updateInventory();
			}
		} else {
			plyr.openInventory((Inventory)this.shopinv.get(Integer.valueOf(1)));
		}
	}

	public void addPrice(ItemStack item, Integer price, Integer sell)
	{
		ItemMeta itm = item.getItemMeta();
		List<String> itmlore = Arrays.asList(new String[] { "§7Price: §c$§0,§c" + price, "§7Sell: §a$§0.§a" + sell, "§8To sell, click the item in your inv.", "§9Must be the same quantity!" });
		itm.setLore(itmlore);
		item.setItemMeta(itm);
	}

	public ItemStack stripMeta(ItemStack item, Integer amount)
	{
		ItemMeta itm = item.getItemMeta();
		itm.setLore(null);
		item.setItemMeta(itm);
		item.setAmount(amount.intValue());
		return item;
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onClick(InventoryClickEvent e)
	{
		if ((e.getWhoClicked() instanceof Player))
		{
			if (e.getInventory().getType() == InventoryType.PLAYER)
			{
				e.setCancelled(false);
				this.open = Boolean.valueOf(false);
			}
			Player p = (Player)e.getWhoClicked();
			if (this.open.booleanValue())
			{
				this.open = Boolean.valueOf(false);
				if (e.getInventory().getTitle().contains(this.menun))
				{
					if (e.getSlotType() == InventoryType.SlotType.CONTAINER)
					{
						if (e.isLeftClick())
						{
							int[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28 };
							for (int slot : row1) {
								if (e.getRawSlot() == slot - 1)
								{
									if (getConfig().getString(slot + ".Enabled") == "true")
									{
										if ((p.hasPermission("guishop.slot." + slot)) || (p.isOp()))
										{
											e.setCancelled(true);
											String shop = getConfig().getString(slot + ".Shop");
											this.title = "";
											this.title = getConfig().getString(slot + ".Shop");
											this.shopn = (shop + ".");
											loadShop(p, this.one);
											e.setCancelled(true);
											break;
										}
										System.out.println("No permission for slots");

										break;
									}
									System.out.println("Slot not enabled!");


									e.setCancelled(true);
									break;
								}
							}

						}
						else {
							e.setCancelled(true);
						}
					}
					else {
						e.setCancelled(true);
					}
				}
				else {
					e.setCancelled(true);
				}
				if (e.getInventory().getTitle().contains(this.title))
				{
					e.setCancelled(true);
					p.closeInventory();
					if (e.getSlot() != -999)
					{
						ItemStack item = e.getCurrentItem();
						if (item != null)
						{
							if (item.hasItemMeta()) {
								if (item.getItemMeta().hasDisplayName()){
									if (!item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', "&4Back"))){
										trySell(p, item);
										if (e.getInventory().getItem(e.getSlot()) != null)
										{


											if (item.getItemMeta().hasLore())
											{
												if (item.getItemMeta().getLore().toString().contains("Price"))
												{
													int price = 2147483647;
													String lorestring = ChatColor.stripColor(item.getItemMeta().getLore().toString().replace("[", "").replace("]", "").replace(",", "").replace("To sell, click the item in your inv.", "").replace("Must be the same quantity!", "").replace("Shift+Click to buy 1 item", ""));
													lorestring = StringUtils.substringBefore(lorestring, ".");
													price = Integer.parseInt(lorestring.replace("Price: ", "").replace("Sell:", "").replace(" ", "").replace("$", ""));
													if ((e.isLeftClick()) && (e.isShiftClick()))
													{
														this.one = Boolean.valueOf(true);
														ItemStack dupeitem = item.clone();
														int ammount = dupeitem.getAmount() / dupeitem.getAmount();
														int price2 = price / dupeitem.getAmount();
														dupeitem.setAmount(ammount);
														if (this.econ.getBalance(p.getName()) >= price2)
														{
															EconomyResponse r = this.econ.withdrawPlayer(p.getName(), price2);
															if (r.transactionSuccess())
															{
																p.getInventory().addItem(new ItemStack[] { stripMeta(dupeitem, Integer.valueOf(dupeitem.getAmount())) });
																p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§fYou purchased, §c" + ammount + " " + item.getType().toString().toLowerCase() + "§f!");
																p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§c$" + price2 + "§f taken from your account.");
															}
														}
														else
														{
															double dif = price2 - this.econ.getBalance(p.getName());
															p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§fYou need §c$" + dif + "§f more money.");
															this.one = Boolean.valueOf(false);
														}
													}
													if (e.isLeftClick()) {
														if (this.one.booleanValue())
														{
															e.setCancelled(true);
														}
														else if (this.econ.getBalance(p.getName()) >= price)
														{
															EconomyResponse r = this.econ.withdrawPlayer(p.getName(), price);
															if (r.transactionSuccess())
															{
																ItemStack dupeitem = item.clone();
																p.getInventory().addItem(new ItemStack[] { stripMeta(dupeitem, Integer.valueOf(dupeitem.getAmount())) });
																p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§fYou purchased, §c" + item.getAmount() + " " + item.getType().toString().toLowerCase() + "§f!");
																p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§c$" + price + "§f taken from your account.");
															}
														}
														else
														{
															double dif = price - this.econ.getBalance(p.getName());
															p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§fYou need §c$" + dif + "§f more money.");
														}
													}
												}

											}else {
												System.out.println("Items have no lore");
											}
											/**/

											loadShop(p, Boolean.valueOf(true));
										}


									}else{
										p.closeInventory();
										loadMenu(p);
									}
								}
							}
						}
					}
				}
				else
				{
					e.setCancelled(true);
				}
			}
			else
			{
				e.setCancelled(false);
			}
		}
	}

	public void trySell(Player p, ItemStack item){
		boolean tally = false;
		if (item.hasItemMeta()){
			if (((item != null) || (item == null)) && 
					(!item.getItemMeta().hasLore()) && 
					(p.getInventory().contains(item)))
			{
				for (String str : this.sellitems)
				{
					String itemid = StringUtils.substringBefore(str, "$");
					if (item.getTypeId() == Integer.parseInt(itemid))
					{
						tally = true;
						String amount = StringUtils.substringAfter(str, ")");
						if (Integer.parseInt(amount) == item.getAmount())
						{
							String preprice = StringUtils.substringAfter(str, "$");
							String price = StringUtils.substringBefore(preprice, ")");
							p.getInventory().removeItem(new ItemStack[] { item });
							EconomyResponse r = this.econ.depositPlayer(p.getName(), Integer.parseInt(price));
							if (r.transactionSuccess())
							{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§fYou sold, §c" + amount + " " + item.getType().toString().toLowerCase() + "§f!");
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§a$" + price + "§f added to your account.");
							}
							else
							{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§fSomething went wrong, contact an admin.");
							}
						}
						else
						{
							int dif = Integer.parseInt(amount);
							p.sendMessage(this.tag + " " + "§fPlease sell in stacks of §c" + dif + " §fplease.");
						}
					}
				}
				if (!tally)
				{
					p.sendMessage(this.tag + " " + "§fSorry, you can't sell that item.");
					tally = true;
				}
			}
		}else{
			// No Item was clicked.
		}
	}

	private ItemStack setName(ItemStack is, String name, List<String> lore)
	{
		ItemMeta IM = is.getItemMeta();
		if (name != null) {
			IM.setDisplayName(name);
		}
		if (lore != null) {
			IM.setLore(lore);
		}
		is.setItemMeta(IM);
		return is;
	}

	public void reloadCustomConfig()
	{
		if (this.customConfigFile == null) {
			this.customConfigFile = new File(getDataFolder(), "shops.yml");
		}
		this.customConfig = YamlConfiguration.loadConfiguration(this.customConfigFile);


		InputStream defConfigStream = getResource("shops.yml");
		if (defConfigStream != null)
		{
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			this.customConfig.setDefaults(defConfig);
		}
	}

	public FileConfiguration getCustomConfig()
	{
		if (this.customConfig == null) {
			reloadCustomConfig();
		}
		return this.customConfig;
	}

	public void saveDefaultConfig()
	{
		if (this.customConfigFile == null)
		{
			this.customConfigFile = new File(getDataFolder(), "shops.yml");
			this.defaultConfigFile = new File(getDataFolder(), "config.yml");
		}
		if (!this.customConfigFile.exists()) {
			saveResource("shops.yml", false);
		}
		if (!this.defaultConfigFile.exists()) {
			saveResource("config.yml", false);
		}
	}
}
