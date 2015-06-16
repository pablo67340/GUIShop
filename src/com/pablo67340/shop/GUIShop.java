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
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GUIShop extends JavaPlugin implements Listener{
	public Economy econ;
	List<String> sellitems = new ArrayList<String>();
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	private File defaultConfigFile = null;
	HashMap<Integer, Inventory> shopinv = new HashMap<Integer, Inventory>();
	String shopn = "";
	String tag = getConfig().getString("tag");
	String title = "";
	String menun = ChatColor.translateAlternateColorCodes('&', getConfig().getString("menuname"));
	Boolean one = Boolean.valueOf(false);
	String ench = "";
	String[] enc;
	int lvl = 0;
	public static boolean verbose = false;
	ArrayList<String> sopen = new ArrayList<String>();
	Shops ss = new Shops();

	public void onEnable(){
		this.sellitems.clear();
		this.shopinv.clear();
		if (!setupEconomy()){
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		saveDefaultConfig();
		verbose = getConfig().getBoolean("Verbose");
		getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onCommand(PlayerCommandPreprocessEvent event){
		Player player = event.getPlayer();
		String command = event.getMessage();
		if (command.equalsIgnoreCase("/" + getConfig().getString("Command"))) {
			if ((player.hasPermission("guishop.use")) || (player.isOp())){
				if (getConfig().getBoolean("sign-only")){
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("sign-only-message")));
					event.setCancelled(true);
				}else{
					loadMenu(player);
					event.setCancelled(true);
				}
			}else{
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
			}
		}else if((command.equalsIgnoreCase("/" + getConfig().getString("Command") + " reload"))){
			if (player.isOp()){
				if (ss.flushData()){
					if (verbose){
						System.out.println("Shop Data exists! Flushing data!");
					}

				}else{
					if (verbose){
						System.out.println("No Shop data existed! No Shops have been opened and cached yet!");
					}

				}
				this.reloadConfig();
				this.reloadCustomConfig();
				event.setCancelled(true);
				if (verbose){
					System.out.println("Reload Complete!");
				}
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag + " &aReloaded!"));

			}else{
				player.sendMessage("No Permission! You must be OP!");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void eventSignChanged(SignChangeEvent event){  
		String title = event.getLine(0);
		Player p = event.getPlayer();
		if (title.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',getConfig().getString("sign-title")))){
			if (p.hasPermission("guishop.sign.place") || p.isOp()){
				event.setCancelled(false);
			}else{
				p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		Block block = e.getClickedBlock();
		if(block != null && block.getState() instanceof Sign) {
			Sign sign = (Sign) block.getState();
			String line1 = ChatColor.translateAlternateColorCodes('&',sign.getLine(0));
			if (verbose){
				System.out.println("Player Clicked sign with line1: "+line1 +" compared to "+ChatColor.translateAlternateColorCodes('&',getConfig().getString("sign-title")));
			}
			if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',getConfig().getString("sign-title")))){
				if (player.hasPermission("guishop.use") && player.hasPermission("guishop.sign.use") || player.isOp()){
					loadMenu(player);
					e.setCancelled(true);
				}else{
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
					e.setCancelled(true);
				}

			}
		}

	}

	private boolean setupEconomy(){
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

	public void loadMenu(Player plyr){
		this.sopen.add(plyr.getName());
		this.title = "";
		this.shopn = "";
		if ((plyr.hasPermission("guishop.use")) || (plyr.isOp())){
			if (getConfig().getStringList("Disabled-Worlds").contains(plyr.getWorld().getName())){
				plyr.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("disabled-world")));
			}else{
				List Ls = new ArrayList();
				Inventory chest = plyr.getPlayer().getServer().createInventory(null, getConfig().getInt("Rows") * 9, this.menun);

				int[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
				int[] row2 = { 10, 11, 12, 13, 14, 15, 16, 17, 18 };
				int[] row3 = { 19, 20, 21, 23, 24, 25, 26, 27, 28 };
				for (int slot : row1) {
					if (getConfig().getString(slot + ".Enabled") == "true"){
						Ls.clear();
						String Name = ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Name"));
						if (ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Desc")) != "null") {
							Ls.add(ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Desc")));
						}
						if ((plyr.hasPermission("guishop.slot." + slot)) || (plyr.isOp())){
							chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(getConfig().getString(slot + ".Item")), 1), Name, Ls));
						}else{
							Ls.add(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
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
								Ls.add(ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Desc")));
							}
							if ((plyr.hasPermission("guishop.slot." + slot)) || (plyr.isOp())){
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(getConfig().getString(slot + ".Item")), 1), Name, Ls));
							}else{
								Ls.add(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
							}
						}
					}
				}
				if (getConfig().getInt("Rows") >= 3) {
					for (int slot : row3) {
						if (getConfig().getString(slot + ".Enabled") == "true"){
							Ls.clear();
							String Name = ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Name"));
							if (getConfig().getString(slot + ".Desc") != "null") {
								Ls.add(ChatColor.translateAlternateColorCodes('&', getConfig().getString(slot + ".Desc")));
							}
							if ((plyr.hasPermission("guishop.slot." + slot)) || (plyr.isOp())){
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(getConfig().getString(slot + ".Item")), 1), Name, Ls));
							}else{
								Ls.add(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
								chest.setItem(slot - 1, setName(new ItemStack(Material.getMaterial(36), 1), Name, Ls));
							}
						}
					}
				}
				plyr.openInventory(chest);
			}
		}else{
			plyr.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
		}
	}

	public void loadShop(Player plyr, Boolean allowUnsafe){
		if (!this.sopen.contains(plyr.getName())) {
			this.sopen.add(plyr.getName());
		}
		this.shopinv.clear();

		int row = 9;
		int size = 5;
		if (this.title.length() > 16) {
			this.title.substring(0, 16);
		}
		Inventory shop = Bukkit.getServer().createInventory(plyr, row * size, this.title);
		String saved = this.shopn.replaceAll("[\\s.]", "");
		if (this.ss.isSaved(saved))
		{
			if (verbose) {
				System.out.println("isSaved check passed! Attempting to open shop with the saved inventory: " + saved);
			}
			shop.setContents(this.ss.getShop(saved));
			plyr.openInventory(shop);
		}else{
			if (this.shopinv.isEmpty()) {
				for (int i = 0; i <= 43; i++){
					String name = "null";
					String data = "0";
					String qty = "1";
					String slot = "0";
					String price = "0";
					String sell = "0";
					String item = "0";
					if (getCustomConfig().get(this.shopn + "." + i) != null){
						List<String> nodes = getCustomConfig().getStringList(this.shopn + "." + i);
						if (verbose) {
							System.out.println("Final item built: " + nodes + " Active!");
						}
						if (nodes != null) {
							for (int nodeapi = 0; nodeapi < nodes.size(); nodeapi++){
								if (verbose) {
									System.out.println("Scanning shops.yml");
								}
								if (((String)nodes.get(nodeapi)).contains("item:")){
									item = ((String)nodes.get(nodeapi)).replace("item:", "");
									if (verbose) {
										System.out.println("Item ID found: " + item);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("slot:")){
									slot = ((String)nodes.get(nodeapi)).replace("slot:", "");
									if (verbose) {
										System.out.println("Slot found: " + slot);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("name:")){
									name = ((String)nodes.get(nodeapi)).replace("name:", "").replace("'", "");
									if (verbose) {
										System.out.println("Item name found: " + name);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("price:")){
									price = ((String)nodes.get(nodeapi)).replace("price:", "").replace("'", "");
									if (verbose) {
										System.out.println("Item price found: " + price);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("data:")){
									data = ((String)nodes.get(nodeapi)).replace("data:", "");
									if (verbose) {
										System.out.println("Data value found: " + data);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("enchantments:")){
									this.ench = ((String)nodes.get(nodeapi)).replace("enchantments:", "").replace("'", "");
									this.enc = this.ench.split(":| ");
									if (verbose) {
										System.out.println("Optional enchants found!: " + this.ench);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("qty:")){
									qty = ((String)nodes.get(nodeapi)).replace("qty:", "");
									if (verbose) {
										System.out.println("Item quantity found: " + qty);
									}
								}
								if (((String)nodes.get(nodeapi)).contains("sell:")){
									sell = ((String)nodes.get(nodeapi)).replace("sell:", "").replace("'", "");
									if (verbose) {
										System.out.println("Item sell price found: " + sell);
									}
									if (!this.sellitems.contains(item + "$" + sell + ")" + qty)) {
										this.sellitems.add(item + "$" + sell + ")" + qty);
									}
								}
							}
						}
						if (Integer.parseInt(data) != 0){
							ItemStack itemwithdata = new ItemStack(Material.getMaterial(Integer.parseInt(item)), Integer.parseInt(qty), (short)Integer.parseInt(data));
							if (verbose) {
								System.out.println("Adding item: " + nodes + " To inventory");
							}
							if (!(isInteger(sell))){
								addPrice2(itemwithdata, Integer.valueOf(Integer.parseInt(price)));
							}else{
								addPrice(itemwithdata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)));
							}

							if (verbose) {
								System.out.println("AddPrice Method Passed! ");
							}
							if (name != "null"){
								ItemMeta itemmeta = itemwithdata.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
								itemwithdata.setItemMeta(itemmeta);
								if (verbose) {
									System.out.println("Item name found! Item meta added!");
								}
							}
							else if (verbose){
								System.out.println("NO Item name found! Breaking!");
							}
							if (this.enc != null) {
								for (int e = -1; e < this.enc.length; e += 2) {
									if (e >= 0){
										if (verbose) {
											System.out.println("Enchants split into values!: ");
										}
										if ((this.enc[(e - 1)] == null) && (this.enc[e] == null)){
											if (verbose) {
												System.out.println("Enchantments are null!");
											}
										}else{
											this.lvl = Integer.parseInt(this.enc[e]);
											itemwithdata.addUnsafeEnchantment(Enchantments.getByName(this.enc[(e - 1)]), this.lvl);
											if (verbose) {
												System.out.println("Enchant values: Enchant name: " + this.enc[(e - 1)] + " Enchant Level: " + this.lvl);
											}
											this.enc[e] = null;
											this.enc[(e - 1)] = null;
										}
									}
								}
							}
							if (Integer.parseInt(slot) != 44){

								if (getConfig().getString("back-button-item").contains(":")){
									String[] backi = getConfig().getString("back-button-item").split(":");
									int bid = Integer.parseInt(backi[0]);
									int bme = Integer.parseInt(backi[1]);
									ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)bme);
									ItemMeta itemmeta = backbutton.getItemMeta();
									itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("back")));
									backbutton.setItemMeta(itemmeta);
									shop.setItem(44, backbutton);

									if (verbose) {
										System.out.println("Item with no data: " + itemwithdata + " Added to shop!");
									}
								}else{
									int bid = Integer.parseInt("back-button-item");
									ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)0);
									ItemMeta itemmeta = backbutton.getItemMeta();
									itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("back")));
									backbutton.setItemMeta(itemmeta);
									shop.setItem(44, backbutton);

									if (verbose) {
										System.out.println("Item with no data: " + itemwithdata + " Added to shop!");
									}
								}
							}else if (verbose){
								System.out.println("ERROR: An Item tried to overwrite button slot!");
							}
						}else{
							if (Material.getMaterial(i) == null) {
								System.out.print(i);
							}
							if (Integer.parseInt(qty) < 1) {
								System.out.print("its 0");
							}
							ItemStack itemnodata = new ItemStack(Material.getMaterial(Integer.parseInt(item)), Integer.parseInt(qty));
							if (!(isInteger(sell))){
								addPrice2(itemnodata, Integer.valueOf(Integer.parseInt(price)));
							}else{
								addPrice(itemnodata, Integer.valueOf(Integer.parseInt(price)), Integer.valueOf(Integer.parseInt(sell)));
							}

							if (name != "null"){
								ItemMeta itemmeta = itemnodata.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
								itemnodata.setItemMeta(itemmeta);
							}
							if (this.enc != null) {
								for (int e = -1; e < this.enc.length; e += 2) {
									if (e >= 0){
										if (verbose) {
											System.out.println("Enchants split into values!: ");
										}
										if ((this.enc[(e - 1)] == null) && (this.enc[e] == null)){
											if (verbose) {
												System.out.println("Enchantments are null!");
											}
										}else{
											this.lvl = Integer.parseInt(this.enc[e]);
											itemnodata.addUnsafeEnchantment(Enchantments.getByName(this.enc[(e - 1)]), this.lvl);
											if (verbose) {
												System.out.println("Enchant values: Enchant name: " + this.enc[(e - 1)] + " Enchant Level: " + this.lvl);
											}
											this.enc[(e - 1)] = null;
											this.enc[e] = null;
										}
									}
								}
							}
							shop.setItem(Integer.parseInt(slot), itemnodata);
							if (getConfig().getString("back-button-item").contains(":")){
								String[] backi = getConfig().getString("back-button-item").split(":");
								int bid = Integer.parseInt(backi[0]);
								int bme = Integer.parseInt(backi[1]);
								ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)bme);
								ItemMeta itemmeta = backbutton.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("back")));
								backbutton.setItemMeta(itemmeta);
								shop.setItem(44, backbutton);

								if (verbose) {
									System.out.println("Item with no data: " + itemnodata + " Added to shop!");
								}
							}else{
								int bid = Integer.parseInt("back-button-item");
								ItemStack backbutton = new ItemStack(Material.getMaterial(bid), 1, (short)0);
								ItemMeta itemmeta = backbutton.getItemMeta();
								itemmeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("back")));
								backbutton.setItemMeta(itemmeta);
								shop.setItem(44, backbutton);

								if (verbose) {
									System.out.println("Item with no data: " + itemnodata + " Added to shop!");
								}
							}

						}

						this.shopinv.put(Integer.valueOf(1), shop);
						plyr.openInventory(shop);
						plyr.updateInventory();
					}
				}
			}

			if (this.ss.saveShop(saved, shop)){
				if (verbose){
					System.out.println("Saved Shop: " + this.shopn);
					System.out.println("SHOP CONTENTS: ");
					System.out.println(this.ss.getShop(this.shopn));
				}
			}else{
				if (verbose) {
					System.out.println("Shop already exists!");
				}
			}
		}
	}

	public void addPrice(ItemStack item, Integer price, Integer sell){
		ItemMeta itm = item.getItemMeta();
		List<String> itmlore = Arrays.asList(new String[] { ChatColor.translateAlternateColorCodes('&', getConfig().getString("cost")) +" §c$§0,§c" + price, ChatColor.translateAlternateColorCodes('&', getConfig().getString("return"))+" §a$§0.§a" + sell, ChatColor.translateAlternateColorCodes('&', getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', getConfig().getString("line4")) });
		itm.setLore(itmlore);
		item.setItemMeta(itm);
	}

	public void addPrice2(ItemStack item, Integer price){
		ItemMeta itm = item.getItemMeta();
		List<String> itmlore = Arrays.asList(new String[] { ChatColor.translateAlternateColorCodes('&', getConfig().getString("cost"))+" §c$§0,§c" + price, "§7Cannot Resell", ChatColor.translateAlternateColorCodes('&', getConfig().getString("line1")), ChatColor.translateAlternateColorCodes('&', getConfig().getString("line2")), ChatColor.translateAlternateColorCodes('&', getConfig().getString("line3")), ChatColor.translateAlternateColorCodes('&', getConfig().getString("line4")) });
		itm.setLore(itmlore);
		item.setItemMeta(itm);
	}

	public void closeDupe(Player p){
		p.closeInventory();
	}

	public ItemStack stripMeta(ItemStack item, Integer amount){
		ItemMeta itm = item.getItemMeta();
		itm.setLore(null);
		item.setItemMeta(itm);
		item.setAmount(amount.intValue());
		return item;
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onClick(InventoryClickEvent e){
		if ((e.getWhoClicked() instanceof Player)){
			final Player p = (Player)e.getWhoClicked();
			if (e.getInventory().getType() == InventoryType.PLAYER){
				e.setCancelled(false);
				if (this.sopen.contains(p.getName())){
					this.sopen.remove(p.getName());
				}
			}
			if (this.sopen.contains(p.getName()))
			{
				this.sopen.remove(p.getName());
				if (e.getInventory().getTitle().contains(this.menun)){
					if (e.getSlotType() == InventoryType.SlotType.CONTAINER){
						if (e.getClickedInventory().getType() == e.getView().getType()){
							if (e.isLeftClick() || e.isShiftClick() || e.isRightClick()){
								int[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28 };
								for (int slot : row1) {
									if (e.getRawSlot() == slot - 1){
										if (getConfig().getString(slot + ".Enabled") == "true"){
											if ((!p.hasPermission("guishop.slot." + slot)) && (!p.isOp())) {
												break;
											}
											e.setCancelled(true);
											String shop = getConfig().getString(slot + ".Shop");
											this.title = "";
											this.title = getConfig().getString(slot + ".Shop");
											this.shopn = (shop + ".");
											loadShop(p, Boolean.valueOf(true));
											e.setCancelled(true);
											break;
										}
										Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
										{
											public void run()
											{
												closeDupe(p);
											}
										}, 1L);
										break;
									}
								}
							}else{
								Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
								{
									public void run()
									{
										closeDupe(p);
									}
								}, 1L);
							}
						}else{
							Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
							{
								public void run()
								{
									closeDupe(p);
								}
							}, 1L);
						}
					}else{
						Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
						{
							public void run()
							{
								closeDupe(p);
							}
						}, 1L);
					}
				}else{
					e.setCancelled(true);
				}
				if ((e.getInventory().getTitle().contains(this.title)) && (!e.getInventory().getTitle().contains(this.menun))){
					e.setCancelled(true);
					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
					{
						public void run()
						{
							loadShop(p,true);
						}
					}, 1L);
					if (e.getSlot() != -999){
						ItemStack item = e.getCurrentItem();
						if (item != null) {
							if ((item.hasItemMeta()) && (item.getItemMeta().hasDisplayName())) {
								if (!item.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&', getConfig().getString("back")))){
									if (e.getInventory().getItem(e.getSlot()) != null){
										if (item.getItemMeta().hasLore()){
											if (item.getItemMeta().getLore().toString().contains(ChatColor.translateAlternateColorCodes('&', getConfig().getString("cost")))) {}
											int price = 2147483647;
											String lorestring2 = ChatColor.stripColor(item.getItemMeta().getLore().toString());
											List<String> items = Arrays.asList(lorestring2.split("\\s*,\\s*"));
											String lorestring = ChatColor.stripColor(item.getItemMeta().getLore().toString().replace("[", "").replace("]", "").replace(",", "").replace("To sell, click the item in your inv.", "").replace("Must be the same quantity!", "").replace("Shift+Click to buy 1 item", ""));
											lorestring = StringUtils.substringBefore(lorestring, ".");
											price = Integer.parseInt(items.get(1));
											if ((e.isLeftClick()) && (e.isShiftClick())){
												ItemStack dupeitem = item.clone();
												int ammount = dupeitem.getAmount() / dupeitem.getAmount();
												int price2 = price / dupeitem.getAmount();
												dupeitem.setAmount(ammount);
												if (this.econ.getBalance(p.getName()) >= price2){
													EconomyResponse r = this.econ.withdrawPlayer(p.getName(), price2);
													if (r.transactionSuccess()){
														p.getInventory().addItem(new ItemStack[] { stripMeta(dupeitem, Integer.valueOf(dupeitem.getAmount())) });
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("purchased")) + ammount + " " + item.getType().toString().toLowerCase() + "§f!");
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§c$" + price2 + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("taken")));
													}
												}else{
													double dif = price2 - this.econ.getBalance(p.getName());
													p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("not-enough-pre")) + dif + ChatColor.translateAlternateColorCodes('&', getConfig().getString("not-enough-post")));
													this.one = Boolean.valueOf(false);
												}
											}
											if ((e.isLeftClick()) && (!e.isShiftClick())) {
												if (this.one.booleanValue()){
													e.setCancelled(true);
												}else if (this.econ.getBalance(p.getName()) >= price){
													EconomyResponse r = this.econ.withdrawPlayer(p.getName(), price);
													if (r.transactionSuccess()){
														ItemStack dupeitem = item.clone();
														p.getInventory().addItem(new ItemStack[] { stripMeta(dupeitem, Integer.valueOf(dupeitem.getAmount())) });
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("purchased")) + item.getAmount() + " " + item.getType().toString().toLowerCase() + "§f!");
														p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§c$" + price + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("taken")));
													}
												}else{
													double dif = price - this.econ.getBalance(p.getName());
													p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("not-enough-pre")) + dif + ChatColor.translateAlternateColorCodes('&', getConfig().getString("not-enough-post")));
												}
											}
										}else{
											trySell(p, item);
										}
									}
								}else{
									Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
									{
										public void run()
										{
											closeDupe(p);
											delayMenu(p);
										}
									}, 1L);

								}
							}else{
								trySell(p, item);
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

	public void delayMenu(final Player p){
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			public void run()
			{
				loadMenu(p);
			}
		}, 1L);
	}

	public static boolean isInteger(String s) {
		return isInteger(s,10);
	}

	public static boolean isInteger(String s, int radix) {
		if(s.isEmpty()) return false;
		for(int i = 0; i < s.length(); i++) {
			if(i == 0 && s.charAt(i) == '-') {
				if(s.length() == 1) return false;
				else continue;
			}
			if(Character.digit(s.charAt(i),radix) < 0) return false;
		}
		return true;
	}

	public void trySell(Player p, ItemStack item)
	{
		boolean tally = false;
		boolean err = false;
		if (verbose){
			System.out.println("TrySell: "+item.getTypeId());
		}
		if (((item != null) || (item == null)) && (!item.getItemMeta().hasLore()) && (p.getInventory().contains(item)))
		{
			if (verbose){
				System.out.println("Item passed secondary checks!");
			}
			for (String str : this.sellitems)
			{
				String itemid = StringUtils.substringBefore(str, "$");
				if (verbose) {
					System.out.println("Item in hand: " + item.getTypeId() + " compared: " + itemid);
				}
				if (item.getTypeId() == Integer.parseInt(itemid))
				{
					tally = true;
					String amount = StringUtils.substringAfter(str, ")");
					if (Integer.parseInt(amount) == item.getAmount())
					{
						String preprice = StringUtils.substringAfter(str, "$");
						String price = StringUtils.substringBefore(preprice, ")");

						if (!(isInteger(price))){
							p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("cant-sell")));
						}else{
							p.getInventory().removeItem(new ItemStack[] { item });
							EconomyResponse r = this.econ.depositPlayer(p.getName(), Integer.parseInt(price));
							if (r.transactionSuccess())
							{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("sold")) + amount + " " + item.getType().toString().toLowerCase() + "§f!");
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + "§a$" + price + ChatColor.translateAlternateColorCodes('&', getConfig().getString("added")));
							}
							else
							{
								p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("something-wrong")));
							}
						}
					}
					else
					{
						int dif = Integer.parseInt(amount);
						p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("in-stacks")) + dif);
						err = true;
					}
				}else{
					if (verbose){
						System.out.println("Compared "+item + " to "+itemid + " And did not match!");
					}
				}
			}
			if (!tally)
			{
				if (err != true){
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', this.tag) + " " + ChatColor.translateAlternateColorCodes('&', getConfig().getString("cant-sell")));
					tally = true;
				}else{

				}
			}
		}
		else if (verbose)
		{
			System.out.println("Else Triggered for item!");
		}

	}

	private ItemStack setName(ItemStack is, String name, List<String> lore){
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

	public void reloadCustomConfig(){
		if (this.customConfigFile == null) {
			this.customConfigFile = new File(getDataFolder(), "shops.yml");
		}
		this.customConfig = YamlConfiguration.loadConfiguration(this.customConfigFile);


		InputStream defConfigStream = getResource("shops.yml");
		if (defConfigStream != null){
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			this.customConfig.setDefaults(defConfig);
		}
	}

	public FileConfiguration getCustomConfig(){
		if (this.customConfig == null) {
			reloadCustomConfig();
		}
		return this.customConfig;
	}

	public void saveDefaultConfig(){
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
