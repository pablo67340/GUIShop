package com.pablo67340.shop.main;

import java.io.*;
import java.util.*;


import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;


import com.pablo67340.shop.handler.*;
import com.pablo67340.shop.listener.PlayerListener;

import de.dustplanet.util.SilkUtil;

public final class Main extends JavaPlugin {

	public File defaultConfigFile;


	private static Economy ECONOMY;
	
	
	/**
	 * The config converted to JSON
	 */
	private static String shops;

	/**
	 * An instance of this class.
	 */
	public static Main INSTANCE;

	/**
	 * A {@link Set} that will store every command that can
	 * be used by a {@link Player} to open the {@link Menu}.
	 */
	public static final Set<String> BUY_COMMANDS = new HashSet<>();

	/**
	 * A {@link Set} that will store every command that can
	 * be used by a {@link Player} to open the {@link Sell}
	 * GUI.
	 */
	public static final Set<String> SELL_COMMANDS = new HashSet<>();

	/**
	 * A {@link Set} that holds the names of each {@link Player}
	 * that currently has the {@link Menu} open.
	 */
	public static final Set<String> HAS_MENU_OPEN = new HashSet<>();

	/**
	 * A {@link Set} that holds the names of each {@link Player}
	 * that currently has the {@link Sell} GUI open.
	 */
	public static final Set<String> HAS_SELL_OPEN = new HashSet<>();

	/**
	 * A {@link Map} that holds the names of each {@link Player}
	 * that currently has a {@link Shop} open as well as the shop
	 * that is open.
	 */
	public static final Map<String, Shop> HAS_SHOP_OPEN = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Menu}s
	 * when the server first starts.
	 * 
	 * @key
	 * 		The name of the {@link Player}.
	 * @value
	 * 		The menu.
	 */
	public static final Map<String, Menu> MENUS = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Creator}s
	 * when the server first starts.
	 * 
	 * @key
	 * 		The name of the {@link Player}.
	 * @value
	 * 		The creator.
	 */
	public static final Map<String, Creator> CREATOR = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Sell}s
	 * when the server first starts.
	 * 
	 * @key
	 * 		The name of the {@link Player}.
	 * @value
	 * 		The sell menu.
	 */
	public static final Map<String, Sell> SELLS = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Shop}s
	 * when the server first starts.
	 * 
	 * @key
	 * 		The index on the {@link Menu} that this shop
	 * 		is located at.
	 * @value
	 * 		The shop.
	 */
	public static final Map<Integer, Shop> SHOPS = new HashMap<>();

	/**
	 * A {@link Map} that holds the prices to buy and sell
	 * an {@link Item} to/from a {@link Shop}.
	 * 
	 * @key
	 * 		The item's ID.
	 * @value
	 * 		The item's price object.
	 */
	public static final Map<String, Price> PRICES = new HashMap<>();
	
	public SilkUtil su;

	@Override
	public void onEnable() {
		INSTANCE = this;

		if (!setupEconomy()) {
			getLogger().info("Plugin couldn't detect Vault or an Economy plugin (Such as Essentials ECO). Disabling.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		createFiles();
		loadDefaults();
		getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
		Shop.loadShops();
		su = SilkUtil.hookIntoSilkSpanwers();
	}

	@Override
	public void onDisable() {

	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

		if (rsp == null) {
			return false;
		}

		return (ECONOMY = (Economy) rsp.getProvider()) != null;
	}

	public void loadDefaults() {
		BUY_COMMANDS.addAll(getMainConfig().getStringList("buy-commands"));
		SELL_COMMANDS.addAll(getMainConfig().getStringList("sell-commands"));
		Utils.setPrefix(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("prefix")));
		Utils.setSignsOnly(getMainConfig().getBoolean("signs-only"));
		Utils.setSignTitle(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("sign-title")));
		Utils.setNotEnoughPre(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("not-enough-pre")));
		Utils.setNotEnoughPost(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("not-enough-post")));
		Utils.setPurchased(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("purchased")));
		Utils.setTaken(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("taken")));
		Utils.setSold(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("sold")));
		Utils.setAdded(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("added")));
		Utils.setCantSell(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("cant-sell")));
		Utils.setEscapeOnly(getMainConfig().getBoolean("escape-only"));
		Utils.setSound(getMainConfig().getString("purchase-sound"));
		Utils.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));
		Utils.setCreatorEnabled(getMainConfig().getBoolean("ingame-config"));
		getDataFolder();
	}

	public File configf, specialf;
	private FileConfiguration config, special;


	public FileConfiguration getCustomConfig() {
		return this.special;
	}

	public FileConfiguration getMainConfig() {
		return this.config;
	}

	public void createFiles() {

		configf = new File(getDataFolder(), "config.yml");
		specialf = new File(getDataFolder(), "shops.yml");

		if (!configf.exists()) {
			configf.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}

		if (!specialf.exists()) {
			specialf.getParentFile().mkdirs();
			saveResource("shops.yml", false);
		}

		config = new YamlConfiguration();
		special = new YamlConfiguration();
		try {
			try {
				config.load(configf);
				special.load(specialf);
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static Economy getEconomy() {
		return ECONOMY;
	}
	
	public static Main getInstance(){
		return INSTANCE;
	}
	
	public void setJSON(String input){
		shops = input;
	}
	
	public String getJSON(){
		return shops;
	}

}
