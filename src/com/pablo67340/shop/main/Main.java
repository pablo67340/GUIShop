package com.pablo67340.shop.main;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.shop.handler.*;
import com.pablo67340.shop.listener.PlayerListener;
import com.songoda.epicspawners.api.EpicSpawners;

import de.dustplanet.util.SilkUtil;

public final class Main extends JavaPlugin {

	/**
	 * The default config.yml File object.
	 */
	public File defaultConfigFile;

	/**
	 * The overridden config file objects.
	 */
	public File configf, specialf;

	/**
	 * The configs FileConfiguration object.
	 */
	private FileConfiguration config, special;

	/**
	 * An instance Vault's Economy.
	 */
	private static Economy ECONOMY;

	/**
	 * An instance of this class.
	 */
	public static Main INSTANCE;

	/**
	 * An instance of a spawners plugin.
	 */
	private Object su;

	/**
	 * True/False if plugin is utilizing mob spawners.
	 */
	private Boolean useSpawners = true;

	/**
	 * True/False if Minecraft version is pre 1.9
	 */
	private Boolean isOdin = false;

	/**
	 * An instance of the Debugger class.
	 */
	public static Debugger debugger = new Debugger();

	/**
	 * A {@link Set} that will store every command that can be used by a
	 * {@link Player} to open the {@link Menu}.
	 */
	public static final Set<String> BUY_COMMANDS = new HashSet<>();

	/**
	 * A {@link Set} that will store every command that can be used by a
	 * {@link Player} to open the {@link Sell} GUI.
	 */
	public static final Set<String> SELL_COMMANDS = new HashSet<>();

	/**
	 * A {@link Set} that holds the names of each {@link Player} that currently has
	 * the {@link Menu} open.
	 */
	public static final Set<String> HAS_MENU_OPEN = new HashSet<>();

	/**
	 * A {@link Set} that holds the names of each {@link Player} that currently has
	 * the {@link Menu} open.
	 */
	public static final Set<String> HAS_QTY_OPEN = new HashSet<>();

	/**
	 * A {@link Set} that holds the names of each {@link Player} that currently has
	 * the {@link Sell} GUI open.
	 */
	public static final Set<String> HAS_SELL_OPEN = new HashSet<>();

	/**
	 * A {@link Map} that holds the names of each {@link Player} that currently has
	 * a {@link Shop} open as well as the shop that is open.
	 */
	public static final Set<String> HAS_SHOP_OPEN = new HashSet<>();

	/**
	 * A {@link Map} that will store our {@link Creator}s when the server first
	 * starts.
	 * 
	 * @key The name of the {@link Player}.
	 * @value The creator.
	 */
	public static final Map<String, Creator> CREATOR = new HashMap<>();

	/**
	 * A {@link Map} that holds the prices to buy and sell an {@link Item} to/from a
	 * {@link Shop}.
	 * 
	 * @key The item's ID.
	 * @value The item's price object.
	 */
	public static final Map<String, Map<String, Price>> PRICETABLE = new HashMap<>();

	/**
	 * Override onEnable, run GUIShop code.
	 * 
	 */
	@Override
	public void onEnable() {
		INSTANCE = this;
		createFiles();
		if (Dependencies.hasDependency("Vault")) {

			if (!setupEconomy()) {
				getLogger().log(Level.INFO, "Vault could not detect an economy plugin!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			if (updateConfig()) {
				checkServerVersion();
				getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
				loadDefaults();
			}

			if (Dependencies.hasDependency("SilkSpawners")) {
				su = (SilkUtil) Dependencies.getDependencyInstance("SilkSpawners");
				getLogger().log(Level.INFO, "SilkSpawners hooked!");
			} else if (Dependencies.hasDependency("EpicSpawners")) {
				su = (EpicSpawners) Dependencies.getDependencyInstance("EpicSpawners");
				getLogger().log(Level.INFO, "EpicSpawners hooked!");
			} else {
				getLogger().log(Level.INFO, "SilkSpawners or EpicSpawners was not installed. Spawners disabled!");
				useSpawners = false;
			}

		} else {
			getLogger().log(Level.WARNING, "Vault is required to run this plugin!");
		}
	}

	/**
	 * Check if server is pre 1.9
	 */
	public void checkServerVersion() {
		if (Bukkit.getVersion().contains("1.1")) {
			isOdin = true;
			getLogger().info("Server is 1.10+ Implementing fixes.");
		} else {
			isOdin = false;
			getLogger().info("Server is 1.9- Implementing fixes.");
		}
	}

	/**
	 * 
	 * Get debugger instance;
	 */
	public Debugger getDebugger() {
		return debugger;
	}

	/**
	 * 
	 * Gets boolean if version is post 1.8
	 */
	public Boolean isOdin() {
		return isOdin;
	}

	/**
	 * Check if Vault is present, Check if an Economy plugin is present, if so Hook.
	 */
	private Boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

		if (rsp == null) {
			return false;
		}

		ECONOMY = (Economy) rsp.getProvider();

		if (ECONOMY == null) {
			getLogger().log(Level.WARNING, "Vault was unable to hook into an economy plugin!");
			return false;
		}
		return true;
	}

	/**
	 * 
	 * Check if the config is up to date. If not, Attempt recursive auto-update.
	 */
	@SuppressWarnings("unused")
	public Boolean updateConfig() {
		Double ver = getMainConfig().getDouble("ver");
		if (ver != null) {
			if (ver == 1.0) {
				getLogger().warning("The config version is outdated! Automatically updating config...");
				getMainConfig().set("menu-cols", null);
				getMainConfig().set("ver", 1.1);
				saveMainConfig();
				getLogger().warning("Config update successful!");
				updateConfig();
				return true;
			} else if (ver == 1.1) {
				getLogger().warning("The config version is outdated! Automatically updating config...");
				getMainConfig().set("full-inventory", "&cPlease empty your inventory!");
				getMainConfig().set("ver", 1.2);
				updateConfig();
				saveMainConfig();
				getLogger().warning("Config update successful!");
				return true;
			} else if (ver == 1.2) {
				getLogger().warning("The config version is outdated! Automatically updating config...");
				getMainConfig().set("currency", "$");
				getMainConfig().set("ver", 1.3);
				updateConfig();
				saveMainConfig();
				getLogger().warning("Config update successful!");
				return true;
			} else if (ver == 1.3) {
				getLogger().warning("The config version is outdated! Automatically updating config...");
				getMainConfig().set("ver", 1.4);
				updateConfig();
				saveMainConfig();
				getLogger().warning("Config update successful!");
				return true;
			} else if (ver == 1.4) {
				getLogger().warning("The config version is outdated! Automatically updating config...");
				getMainConfig().set("ver", 1.5);
				getMainConfig().set("currency-suffix", "");
				getMainConfig().set("qty-title", "&4Select Amount");
				updateConfig();
				saveMainConfig();
				getLogger().warning("Config update successful!");
				return true;
			} else if (ver == 1.5) {
				getLogger().info("Config all up to date!");
				return true;
			} else {

				getLogger().warning("The config version is outdated! Please delete your config.yml and restart!");
				getServer().getPluginManager().disablePlugin(this);
				return false;
			}
		} else {
			getLogger().warning("The config version is outdated! Please delete your config.yml and restart!");
			getServer().getPluginManager().disablePlugin(this);
			return false;
		}
	}

	/**
	 * 
	 * Load all deault config values, translate colors, store.
	 */
	public void loadDefaults() {
		BUY_COMMANDS.addAll(getMainConfig().getStringList("buy-commands"));
		SELL_COMMANDS.addAll(getMainConfig().getStringList("sell-commands"));
		Utils.setPrefix(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("prefix")));
		Utils.setSignsOnly(getMainConfig().getBoolean("signs-only"));
		Utils.setSignTitle(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("sign-title")));
		Utils.setNotEnoughPre(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("not-enough-pre")));
		Utils.setNotEnoughPost(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("not-enough-post")));
		Utils.setPurchased(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("purchased")));
		Utils.setTaken(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("taken")));
		Utils.setSold(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("sold")));
		Utils.setAdded(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("added")));
		Utils.setCantSell(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("cant-sell")));
		Utils.setEscapeOnly(getMainConfig().getBoolean("escape-only"));
		Utils.setSound(getMainConfig().getString("purchase-sound"));
		Utils.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));
		Utils.setCreatorEnabled(getMainConfig().getBoolean("ingame-config"));
		Utils.setCantBuy(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("cant-buy")));
		Utils.setMenuRows(getMainConfig().getInt("menu-rows"));
		Utils.setFull(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("full-inventory")));
		Utils.setNoPermission(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("no-permission")));
		Utils.setCurrency(getMainConfig().getString("currency"));
		Utils.setCurrencySuffix(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("currency-suffix")));
		Utils.setQtyTitle(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("qty-title")));
		Utils.setSilkSpawners(getConfig().getBoolean("silkspawners"));
		getDataFolder();
	}

	/**
	 * 
	 * Get the CustomConfigFile (Shops)
	 */
	public FileConfiguration getCustomConfig() {
		return this.special;
	}

	/**
	 * 
	 * Get main ConfigFile (Config)
	 */
	public FileConfiguration getMainConfig() {
		return this.config;
	}

	/**
	 * Force save the main config.
	 */
	public void saveMainConfig() {
		try {
			getMainConfig().save(configf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Force create all YML files.
	 */
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

	/**
	 * 
	 * Get the current economy object
	 */
	public static Economy getEconomy() {
		return ECONOMY;
	}

	/**
	 * 
	 * Get the Main Instance of GUIShop
	 */
	public static Main getInstance() {
		return INSTANCE;
	}

	/**
	 * Gets the global spawner object.
	 */
	public Object getSpawnerObject() {
		return su;
	}

	/**
	 * True/False if plugin utilizes mob spawners.
	 * 
	 * @return If the plugin utilizes mob spawners.
	 */
	public Boolean usesSpawners() {
		return useSpawners;
	}

}
