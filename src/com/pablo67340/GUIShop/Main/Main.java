package com.pablo67340.GUIShop.Main;


import com.pablo67340.GUIShop.Handlers.Item;
import com.pablo67340.GUIShop.Handlers.Menu;
import com.pablo67340.GUIShop.Handlers.Sell;
import com.pablo67340.GUIShop.Handlers.Shop;
import com.pablo67340.GUIShop.Handlers.Utils;
import com.pablo67340.GUIShop.Listeners.PlayerListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import org.bukkit.plugin.RegisteredServiceProvider;

import org.bukkit.plugin.java.JavaPlugin;


public class Main extends JavaPlugin {
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	private File defaultConfigFile = null;
	public Economy econ;
	public Item item;
	public Utils utils;
	public Shop shop;
	public Menu menu;
	public Sell sell;
	public List<String> sellitems;
	public PlayerListener listene = new PlayerListener(this);

	public Main() {
		item = new Item(this);
		utils = new Utils(this);
		shop = new Shop(this);
		menu = new Menu(this);
		sell = new Sell(this);
		sellitems = new ArrayList<String>();
	}

	public void onEnable() {
		if (!setupEconomy()) {
			System.out.println("Plugin couldnt detect Vault or a Economy plugin (Such as Essentials ECO). Disabling.");
			getServer().getPluginManager().disablePlugin((Plugin)this);
			return;
		}
		saveDefaultConfig();
		loadDefaults();
		getServer().getPluginManager().registerEvents((Listener)listene, (Plugin)this);
	}

	public void onDisable() {

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider rsp = getServer().getServicesManager().getRegistration((Class)Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = (Economy)rsp.getProvider();
		if (econ != null) {
			return true;
		}
		return false;
	}

	public void loadDefaults() {
		utils.setCommand(getConfig().getString("Command"));
		utils.setMenuName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("menuname")));
		utils.setPrefix(ChatColor.translateAlternateColorCodes('&', getConfig().getString("tag")));
		utils.setSignOnly(getConfig().getBoolean("sign-only"));
		utils.setSignTitle(ChatColor.translateAlternateColorCodes('&', getConfig().getString("sign-title")));
		utils.setVerbose(getConfig().getBoolean("Verbose"));
		utils.setSellCommand(getConfig().getString("sell-command"));
		utils.setSellTitle(getConfig().getString("sell-title"));
	}

	public void closeInventory(final Player p) {
		getServer().getScheduler().scheduleSyncDelayedTask((Plugin)this, new Runnable(){

			@Override
			public void run() {
				p.closeInventory();
			}
		}, 1);
	}

	public void delayMenu(final Player p) {
		getServer().getScheduler().scheduleSyncDelayedTask((Plugin)this, new Runnable(){

			@Override
			public void run() {
				menu.openMenu(p);
			}
		}, 1);
	}

	public void delayShop(final Player p) {
		getServer().getScheduler().scheduleSyncDelayedTask((Plugin)this, new Runnable(){

			@Override
			public void run() {
				shop.openShop(p);
			}
		}, 1);
	}

	@SuppressWarnings("deprecation")
	public void reloadCustomConfig() {
		if (customConfigFile == null) {
			customConfigFile = new File(getDataFolder(), "shops.yml");
		}
		customConfig = YamlConfiguration.loadConfiguration((File)customConfigFile);
		InputStream defConfigStream = getResource("shops.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration((InputStream)defConfigStream);
			customConfig.setDefaults((Configuration)defConfig);
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

}

