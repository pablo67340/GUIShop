package com.pablo67340.guishop;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.Price;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.util.Config;

import lombok.Getter;

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
	private Map<String, ShopDef> shops;

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
	
	@Getter
	private final Map<String, Price> PRICETABLE = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Creator}s when the server first
	 * starts.
	 */
	@Getter
	public static final List<String> CREATOR = new ArrayList<>();

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

			getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
			loadDefaults();

		} else {
			getLogger().log(Level.WARNING, "Vault is required to run this plugin!");
		}

		loadShopDefs();
	}

	public void loadShopDefs() {

		ConfigurationSection menuItems = Main.getINSTANCE().getConfig().getConfigurationSection("menu-items");

		for (String key : menuItems.getKeys(false)) {

			String name = menuItems.getString(key + ".Name") != null ? ChatColor.translateAlternateColorCodes('&',
					Objects.requireNonNull(menuItems.getString(key + ".Name"))) : " ";

			String description = menuItems.getString(key + ".Desc") != null ? ChatColor.translateAlternateColorCodes(
					'&', Objects.requireNonNull(menuItems.getString(key + ".Desc"))) : " ";

			ItemType itemType = menuItems.getString(key + ".Type") != null
					? ItemType.valueOf(menuItems.getString(key + ".Type"))
					: ItemType.SHOP;

			if (!menuItems.getBoolean(key + ".Enabled") && itemType == ItemType.SHOP) {
				continue;

			}

			String itemID = itemType != ItemType.BLANK ? menuItems.getString(key + ".Item") : "AIR";

			List<String> lore = new ArrayList<>();

			if (description.length() > 0) {
				lore.add(description);
			}

			shops.put(key.toLowerCase(), new ShopDef(key, name, description, lore, itemType, itemID));
		}

		new Menu().itemWarmup();
		loadPRICETABLE();
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
		Config.setBackButtonItem(ChatColor.translateAlternateColorCodes('&',
				Objects.requireNonNull(getMainConfig().getString("back-button-item"))));
		Config.setBackButtonText(
				ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("back"))));
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
	}
	
	
	private void loadPRICETABLE() {
		Item item;

		for (String shop : Main.getINSTANCE().getCustomConfig().getKeys(false)) {

			ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);

			assert config != null;
			for (String str : config.getKeys(false)) {

				item = new Item();

				ConfigurationSection section = config.getConfigurationSection(str);

				item.setMaterial((section.contains("id") ? (String) section.get("id") : "AIR"));
				item.setMobType((section.contains("mobType") ? (String) section.get("mobType") : "PIG"));

				item.setBuyPrice((section.contains("buy-price") ? section.get("buy-price") : false));

				item.setSellPrice((section.contains("sell-price") ? section.get("sell-price") : false));

				item.setItemType(
						section.contains("type") ? ItemType.valueOf((String) section.get("type")) : ItemType.SHOP);

				if (item.getSellPrice() != null && (!(item.getSellPrice() instanceof Boolean))) {

					Double sellPrice = item.getSellPrice() instanceof Integer
							? ((Integer) item.getSellPrice()).doubleValue()
							: ((Double) item.getSellPrice());

					if (item.isMobSpawner()) {
						PRICETABLE.put(item.getMaterial() + ":" + item.getMobType().toLowerCase(),
								new Price(sellPrice));
					} else {
						System.out.println("Added: "+item.getMaterial());
						PRICETABLE.put(item.getMaterial(), new Price(sellPrice));
					}
				}
			}
		}
	}

	/**
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

		mainConfig = new YamlConfiguration();
		customConfig = new YamlConfiguration();

		try {
			mainConfig.load(configf);
			customConfig.load(specialf);
		} catch (InvalidConfigurationException | IOException e) {
			e.printStackTrace();
		}

	}
	
	public static String placeholderIfy(String input, Player player, Item item) {
		String str = input;

		str = str.replace("{PLAYER_NAME}", player.getName());
		str = str.replace("{PLAYER_UUID}", player.getUniqueId().toString());
		str = str.replace("{ITEM_SHOP_NAME}", item.getShopName());
		str = str.replace("{ITEM_BUY_NAME}", item.getBuyName());
		str = str.replace("{BUY_PRICE}", item.getBuyPrice().toString());
		str = str.replace("{CURRENCY_SYMBOL}", Config.getCurrency());
		str = str.replace("{CURRENCY_SUFFIX}", Config.getCurrencySuffix());

		return str;
	}

}
