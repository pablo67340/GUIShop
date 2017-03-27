package com.pablo67340.shop.main;

import java.io.*;
import java.util.*;


import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;


import com.pablo67340.shop.handler.*;
import com.pablo67340.shop.listener.PlayerListener;

public final class Main extends JavaPlugin {

	public static File customConfigFile;

	public File defaultConfigFile;

	private FileConfiguration customConfig;

	private static Economy ECONOMY;

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

	@Override
	public void onEnable() {
		INSTANCE = this;

		if (!setupEconomy()) {
			getLogger().info("Plugin couldn't detect Vault or an Economy plugin (Such as Essentials ECO). Disabling.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		saveDefaultConfig();
		loadDefaults();
		getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
		Shop.loadShops();
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

	private void loadDefaults() {
		BUY_COMMANDS.addAll(getConfig().getStringList("buy-commands"));
		SELL_COMMANDS.addAll(getConfig().getStringList("sell-commands"));
		Utils.setPrefix(ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix")));
		Utils.setSignsOnly(getConfig().getBoolean("signs-only"));
		Utils.setSignTitle(ChatColor.translateAlternateColorCodes('&', getConfig().getString("sign-title")));
		Utils.setNotEnoughPre(ChatColor.translateAlternateColorCodes('&', getConfig().getString("not-enough-pre")));
		Utils.setNotEnoughPost(ChatColor.translateAlternateColorCodes('&', getConfig().getString("not-enough-post")));
		Utils.setPurchased(ChatColor.translateAlternateColorCodes('&', getConfig().getString("purchased")));
		Utils.setTaken(ChatColor.translateAlternateColorCodes('&', getConfig().getString("taken")));
		Utils.setSold(ChatColor.translateAlternateColorCodes('&', getConfig().getString("sold")));
		Utils.setAdded(ChatColor.translateAlternateColorCodes('&', getConfig().getString("added")));
		Utils.setCantSell(ChatColor.translateAlternateColorCodes('&', getConfig().getString("cant-sell")));
		Utils.setEscapeOnly(getConfig().getBoolean("escape-only"));
		getDataFolder();
	}

	@SuppressWarnings("deprecation")
	public void reloadCustomConfig() {
		if (customConfigFile == null) {
			customConfigFile = new File(getDataFolder(), "shops.yml");
		}

		customConfig = YamlConfiguration.loadConfiguration((File) customConfigFile);

		InputStream defConfigStream = getResource("shops.yml");

		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration((InputStream) defConfigStream);
			customConfig.setDefaults((Configuration) defConfig);
		}
	}

	public FileConfiguration getCustomConfig() {
		if (customConfig == null) {
			reloadCustomConfig();
		}

		return customConfig;
	}

	public void saveDefaultConfig() {
		if (customConfigFile == null) {
			customConfigFile = new File(getDataFolder(), "shops.yml");
			defaultConfigFile = new File(getDataFolder(), "config.yml");
		}

		if (!customConfigFile.exists()) {
			saveResource("shops.yml", false);
		}

		if (!defaultConfigFile.exists()) {
			saveResource("config.yml", false);
		}
	}

	public static Economy getEconomy() {
		return ECONOMY;
	}

}
