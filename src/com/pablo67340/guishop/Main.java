package com.pablo67340.guishop;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.SQLiteLib.Main.SQLiteLib;
import com.pablo67340.guishop.definition.ItemCommand;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.handler.*;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;

import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.Debugger;

import lombok.Getter;

@SuppressWarnings("SpellCheckingInspection")
public final class Main extends JavaPlugin {

	/**
	 * The overridden config file objects.
	 */
	@Getter
	private File configf, specialf;

	/**
	 * The loaded shops read from the config.
	 */
	@Getter
	private Map<String, ShopDir> shops;

	/**
	 * The configs FileConfiguration object.
	 */
	@Getter
	private FileConfiguration mainConfig, customConfig;

	/**
	 * An instance Vault's Economy.
	 */
	@Getter
	private static Economy ECONOMY;

	/**
	 * An instance of this class.
	 */
	@Getter
	public static Main INSTANCE;

	/**
	 * An instance of the Debugger class.
	 */
	@Getter
	public static Debugger debugger = new Debugger();

	/**
	 * A {@link Set} that will store every command that can be used by a
	 * {@link Player} to open the {@link Menu}.
	 */
	@Getter
	public static final Set<String> BUY_COMMANDS = new HashSet<>();

	/**
	 * A {@link Set} that will store every command that can be used by a
	 * {@link Player} to open the {@link Sell} GUI.
	 */
	@Getter
	public static final Set<String> SELL_COMMANDS = new HashSet<>();

	@Getter
	public Map<String, List<Item>> loadedShops = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Creator}s when the server first
	 * starts.
	 */
	public static final Map<String, Creator> CREATOR = new HashMap<>();

	@Getter
	public static final Set<String> protectedCommands = new HashSet<>();

	@Getter
	private SQLiteLib sqlLib;

	@Override
	public void onEnable() {
		INSTANCE = this;
		shops = new LinkedHashMap<>();
		createFiles();
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("Vault")) {

			if (!setupEconomy()) {
				getLogger().log(Level.INFO, "Vault could not detect an economy plugin!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			if (updateConfig()) {
				getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
				loadDefaults();
			}

		} else {
			getLogger().log(Level.WARNING, "Vault is required to run this plugin!");
		}
		sqlLib = new SQLiteLib();

		sqlLib.initializeDatabase(this, "guishop_commands",
				"CREATE TABLE IF NOT EXISTS commands (`id` INTEGER PRIMARY KEY, `uuid` varchar(256) not null, `command` text not null, `duration` varchar(16) not null, `start` varchar(16) not null)");

		loadShopDefs();
	}

	public void loadShopDefs() {

		ConfigurationSection menuItems = Main.getINSTANCE().getConfig().getConfigurationSection("menu-items");

		for (String key : menuItems.getKeys(false)) {

			String name = menuItems.getString(key + ".Name") != null ? ChatColor.translateAlternateColorCodes('&',
					Objects.requireNonNull(menuItems.getString(key + ".Name"))) : "";

			String description = menuItems.getString(key + ".Desc") != null ? ChatColor
					.translateAlternateColorCodes('&', Objects.requireNonNull(menuItems.getString(key + ".Desc"))) : "";

			ItemType itemType = menuItems.getString(key + ".Type") != null
					? ItemType.valueOf(menuItems.getString(key + ".Type"))
					: ItemType.SHOP;

			if (itemType == ItemType.SHOP) {
				if (!menuItems.getBoolean(key + ".Enabled")) {
					continue;
				}
			}

			System.out.println("ItemType: " + itemType + " name: " + key);

			String itemID = itemType != ItemType.BLANK ? menuItems.getString(key + ".Item") : "AIR";

			List<String> lore = new ArrayList<>();

			if (description.length() > 0) {
				lore.add(description);
			}

			shops.put(key.toLowerCase(), new ShopDir(key, name, description, lore, itemType, itemID));
		}
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
		int index = -1;
		String startDate = "";
		for (Object object : commandRow) {
			index += 1;
			String cmd = (String) object;
			String duration = (String) durationRow.get(index);
			String finalCommand = cmd + "::" + duration;
			commands.add(finalCommand);
			startDate = (String) startRow.get(index);
		}

		// Change to dateTimeFormatter. Close Deprecation warning

		return new ItemCommand(commands, uuid, false, startDate);

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

		ECONOMY = rsp.getProvider();

		return true;
	}

	/**
	 * Check if the config is up to date. If not, Attempt recursive auto-update.
	 */
	@SuppressWarnings("unused")
	private Boolean updateConfig() {
		double ver = getMainConfig().getDouble("ver");
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
	}

	/**
	 * Load all deault config values, translate colors, store.
	 */
	public void loadDefaults() {
		BUY_COMMANDS.addAll(getMainConfig().getStringList("buy-commands"));
		SELL_COMMANDS.addAll(getMainConfig().getStringList("sell-commands"));
		Config.setPrefix(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("prefix"))));
		Config.setSignsOnly(getMainConfig().getBoolean("signs-only"));
		Config.setSignTitle(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("sign-title"))));
		Config.setNotEnoughPre(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("not-enough-pre"))));
		Config.setNotEnoughPost(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("not-enough-post"))));
		Config.setPurchased(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("purchased"))));
		Config.setTaken(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("taken"))));
		Config.setSold(
				ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("sold"))));
		Config.setAdded(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("added"))));
		Config.setCantSell(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("cant-sell"))));
		Config.setEscapeOnly(getMainConfig().getBoolean("escape-only"));
		Config.setSound(getMainConfig().getString("purchase-sound"));
		Config.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));
		Config.setEnableCreator(getMainConfig().getBoolean("ingame-config"));
		Config.setCantBuy(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("cant-buy"))));
		Config.setMenuRows(getMainConfig().getInt("menu-rows"));
		Config.setFull(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("full-inventory"))));
		Config.setNoPermission(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("no-permission"))));
		Config.setCurrency(getMainConfig().getString("currency"));
		Config.setCurrencySuffix(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("currency-suffix"))));
		Config.setQtyTitle(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("qty-title"))));
		Config.setCommandAlready(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("command-already"))));
		Config.setCommandRemaining(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("command-time-remaining"))));
		Config.setCommandExpired(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("command-expired"))));
		Config.setCommandPurchase(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("command-purchase"))));
		Config.setBackButtonItem(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("back-button-item"))));
		Config.setBackButtonText(
				ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("back"))));
		Config.setAccessTo(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("access-to"))));
		Config.setBuyLore(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("buy-lore"))));
		Config.setSellLore(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("sell-lore"))));
		Config.setFreeLore(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("free-lore"))));

		Config.setCannotBuy(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("cannot-buy"))));
		Config.setCannotSell(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("cannot-sell"))));
		Config.getDisabledQty().addAll(getMainConfig().getStringList("disabled-qty-items"));
		protectedCommands.addAll(getMainConfig().getStringList("protected-commands"));
	}

	/**
	 * Force save the main config.
	 */
	private void saveMainConfig() {
		try {
			getMainConfig().save(configf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Force save the shop config.
	 */
	@SuppressWarnings("unused")
	public void saveShopConfig() {
		try {
			getCustomConfig().save(specialf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Force create all YML files.
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
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

		mainConfig = new YamlConfiguration();
		customConfig = new YamlConfiguration();
		try {
			try {
				mainConfig.load(configf);
				customConfig.load(specialf);
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Boolean purchaseCommands(UUID uuid, List<String> commands) {
		ItemCommand itemCommand = loadCommands(uuid);

		for (String cmd : commands) {
			String command = StringUtils.substringBefore(cmd, "::");
			if (itemCommand.getValidCommands().contains(command)) {
				return false;
			}
		}

		Date date = new Date();
		new ItemCommand(commands, uuid, true, date.toString());
		return true;

	}

}
