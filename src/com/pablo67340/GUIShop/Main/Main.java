package com.pablo67340.GUIShop.Main;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.GUIShop.Handlers.Cache;
import com.pablo67340.GUIShop.Handlers.Item;
import com.pablo67340.GUIShop.Handlers.Menu;
import com.pablo67340.GUIShop.Handlers.Shop;
import com.pablo67340.GUIShop.Handlers.Utils;
import com.pablo67340.GUIShop.Listeners.PlayerListener;

public class Main extends JavaPlugin{


	//VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES VARIABLES
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	private File defaultConfigFile = null;
	public Economy econ;
	public Item item = new Item(this);
	public Utils utils = new Utils(this);
	public Cache cache = new Cache(this);
	public Shop shop = new Shop(this);
	public Menu menu = new Menu(this);
	public List<String> sellitems = new ArrayList<String>();

	//METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS METHODS

	public void onEnable(){
		if (!setupEconomy()){
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		saveDefaultConfig();
		loadDefaults();
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
	}

	private boolean setupEconomy(){
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = ((Economy)rsp.getProvider());
		return econ != null;
	}

	public void loadDefaults(){
		utils.setCommand(getConfig().getString("Command"));
		utils.setMenuName(ChatColor.translateAlternateColorCodes('&', getConfig().getString("menuname")));
		utils.setPrefix(ChatColor.translateAlternateColorCodes('&', getConfig().getString("tag")));
		utils.setSignOnly(getConfig().getBoolean("sign-only"));
		utils.setSignTitle(ChatColor.translateAlternateColorCodes('&', getConfig().getString("sign-title")));
		utils.setVerbose(getConfig().getBoolean("Verbose"));
	}

	public void closeInventory(final Player p){
		// This schedules the inventory to be closed on the next tick, Reason is this can cause dupes, And is a proper close method.
		// Player.closeInventory() on the same tick is improper. Sometimes it wont close, Sometimes it will.
		// This properly closes the players inventory. Your welcome developers who are making plugins similar to this :)
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			public void run()
			{
				p.closeInventory();
			}
		}, 1L);
	}

	public void delayMenu(final Player p){
		// This schedules the menu to be opened on the next tick, Reason is this can cause dupes, And is a proper close method.
		// For some reason if one inventory is closed and one is opened on the same tick, the inventory listeners will not register
		// on the newly opened inventory. Therefor the items can be taken out of the menu. Scheduling on next tick fixes this.
		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			public void run()
			{
				menu.loadMenu(p);
			}
		}, 1L);
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


