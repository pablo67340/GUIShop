package com.pablo67340.guishop.main;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.SQLiteLib.Main.SQLiteLib;
import com.pablo67340.guishop.definition.ItemCommand;
import com.pablo67340.guishop.handler.*;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.Debugger;
import com.pablo67340.guishop.util.Dependencies;
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
	 * the {@link Sell} GUI open.
	 */
	public static final Set<String> HAS_SELL_OPEN = new HashSet<>();

	/**
	 * A {@link Map} that will store our {@link Creator}s when the server first
	 * starts.
	 * 
	 * @key The name of the {@link Player}.
	 * @value The creator.
	 */
	public static final Map<String, Creator> CREATOR = new HashMap<>();

	public static final Set<String> protectedCommands = new HashSet<>();

	private SQLiteLib sqlLib;

	/**
	 * A {@link Map} that holds the prices to buy and sell an {@link Item} to/from a
	 * {@link Shop}.
	 * 
	 * @key The item's ID.
	 * @value The item's price object.
	 */
	// public static final Map<String, Map<String, Price>> PRICETABLE = new
	// HashMap<>();

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
		sqlLib = new SQLiteLib();

		sqlLib.initializeDatabase(this, "guishop_commands",
				"CREATE TABLE IF NOT EXISTS commands (`id` INTEGER PRIMARY KEY, `uuid` varchar(256) not null, `command` text not null, `duration` varchar(16) not null, `start` varchar(16) not null)");

	}

	public void addCommand(UUID uuid, String cmd, String duration, String startDate) {

		String statement = "INSERT INTO commands (uuid, command, duration, start) VALUES ('" + uuid.toString() + "','"
				+ cmd + "','" + duration + "','" + startDate + "')";
		sqlLib.getDatabase("guishop_commands").executeStatement(statement);

	}

	public void removeCommand(UUID uuid, String command) {

		sqlLib.getDatabase("guishop_commands")
				.executeStatement("REMOVE FROM commands WHERE command = " + command + " AND uuid = " + uuid.toString());

	}

	public ItemCommand loadCommands(UUID uuid) {
		List<String> commands = new ArrayList<>();
		List<Object> commandRow = sqlLib.getDatabase("guishop_commands")
				.queryRow("SELECT command FROM commands WHERE uuid = '" + uuid + "'", "command");
		List<Object> durationRow = sqlLib.getDatabase("guishop_commands")
				.queryRow("SELECT duration FROM commands WHERE uuid = '" + uuid + "'", "duration");
		List<Object> startRow = sqlLib.getDatabase("guishop_commands")
				.queryRow("SELECT start FROM commands WHERE uuid = '" + uuid + "'", "start");
		Integer index = -1;
		String startDate = "";
		for (Object object : commandRow) {
			index += 1;
			String cmd = (String) object;
			String duration = (String) durationRow.get(index);
			String finalCommand = cmd + "::" + duration;
			commands.add(finalCommand);
			startDate = (String) startRow.get(index);
		}

		ItemCommand itemCommand = new ItemCommand(commands, uuid, false, startDate);
		// Change to dateTimeFormatter. Close Deprecation warning

		return itemCommand;

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
				getLogger().warning("The config version is outdated! Automatically updating config...");
				getMainConfig().set("ver", 1.6);
				getMainConfig().set("command-already", "&4 You already own one or more of the specified commands!");
				getMainConfig().set("command-time-remaining", "&4 Command Expires in: &e{TIME}");
				getMainConfig().set("command-expired", "&4Command has expired! Please purchase from shop!");
				List<String> commands = new ArrayList<>();
				commands.add("/suicide");
				commands.add("/fly");
				getMainConfig().set("protected-commands", commands);
				updateConfig();
				saveMainConfig();
				getLogger().warning("Config update successful!");
				return true;
			} else if (ver == 1.6) {
				getLogger().info("The config version is outdated! Automatically updating config...");
				getMainConfig().set("ver", 1.7);
				getMainConfig().set("command-already", "&4You already own one or more of the specified commands!");
				getMainConfig().set("command-time-remaining", "&4Command Expires in: &e{TIME}");
				getMainConfig().set("command-expired", "&4Command has expired! Please purchase from shop!");
				getMainConfig().set("command-purchase", "&4This command can only be purchased from our shop!");
				getMainConfig().set("access-to", "&aAccess to Commands:");
				updateConfig();
				saveMainConfig();
				getLogger().warning("Config update successful!");
				return true;
			} else if (ver == 1.7) {
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
		Config.setPrefix(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("prefix")));
		Config.setSignsOnly(getMainConfig().getBoolean("signs-only"));
		Config.setSignTitle(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("sign-title")));
		Config.setNotEnoughPre(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("not-enough-pre")));
		Config.setNotEnoughPost(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("not-enough-post")));
		Config.setPurchased(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("purchased")));
		Config.setTaken(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("taken")));
		Config.setSold(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("sold")));
		Config.setAdded(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("added")));
		Config.setCantSell(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("cant-sell")));
		Config.setEscapeOnly(getMainConfig().getBoolean("escape-only"));
		Config.setSound(getMainConfig().getString("purchase-sound"));
		Config.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));
		Config.setCreatorEnabled(getMainConfig().getBoolean("ingame-config"));
		Config.setCantBuy(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("cant-buy")));
		Config.setMenuRows(getMainConfig().getInt("menu-rows"));
		Config.setFull(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("full-inventory")));
		Config.setNoPermission(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("no-permission")));
		Config.setCurrency(getMainConfig().getString("currency"));
		Config.setCurrencySuffix(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("currency-suffix")));
		Config.setQtyTitle(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("qty-title")));
		Config.setSilkSpawners(getConfig().getBoolean("silkspawners"));
		Config.setCommandAlready(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("command-already")));
		Config.setCommandRemaining(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("command-time-remaining")));
		Config.setCommandExpired(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("command-expired")));
		Config.setCommandPurchase(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("command-purchase")));
		Config.setBackButtonItem(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("back-button-item")));
		Config.setBackButtonText(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("back")));
		Config.setAccessTo(ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("access-to")));
		getDataFolder();
		for (String cmd : getMainConfig().getStringList("protected-commands")) {
			protectedCommands.add(cmd);
		}
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
	 * Force save the shop config.
	 */
	public void saveShopConfig() {
		try {
			getCustomConfig().save(specialf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Force create all YML files.
	 */
	public void createFiles() {
		this.getDataFolder();

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

	public SQLiteLib getSqlLite() {
		return sqlLib;
	}

	public Boolean purchaseCommands(UUID uuid, List<String> commands) {
		ItemCommand itemCommand = loadCommands(uuid);

		if (itemCommand != null) {
			for (String cmd : commands) {
				String command = StringUtils.substringBefore(cmd, "::");
				if (itemCommand.getValidCommands().contains(command)) {
					return false;
				}
			}
		}

		Date date = new Date();
		new ItemCommand(commands, uuid, true, date.toString());
		return true;

	}

}
