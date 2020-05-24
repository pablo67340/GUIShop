package com.pablo67340.guishop;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.autosellstuff.ChestLocation;
import com.pablo67340.guishop.autosellstuff.SellChest;
import com.pablo67340.guishop.autosellstuff.SellWand;
import com.pablo67340.guishop.commands.BuyCommand;
import com.pablo67340.guishop.commands.GuishopCommand;
import com.pablo67340.guishop.commands.GuishopUserCommand;
import com.pablo67340.guishop.commands.SellCommand;
import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopDef;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.MatLib;
import com.pablo67340.guishop.util.XMaterial;

import lombok.Getter;
import lombok.Setter;

public final class Main extends JavaPlugin {

	/**
	 * The overridden config file objects.
	 */
	@Getter
	private File configf, specialf, sellChestF;
	@Getter public File econLogFile, autoSellerLogFile;

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
	 * the instance of the dynamic price provider, if dynamic pricing is used
	 */
	@Getter
	private static DynamicPriceProvider DYNAMICPRICING;

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
	public Map<String, Map<Integer, Item>> loadedShops = new HashMap<>();

	@Getter
	private final Map<String, Item> ITEMTABLE = new HashMap<>();

	/**
	 * A {@link Map} that will store our {@link Creator}s when the server first
	 * starts.
	 */
	@Getter
	public static final List<String> CREATOR = new ArrayList<>();

	@Getter
	private final MatLib matLib = new MatLib();

	@Getter
	@Setter
	private Boolean creatorRefresh = false;

	@Getter
	private final Map<UUID, Shop> openShopInstances = new HashMap<>();

	/**
	 * The current buy command
	 */
	private BuyCommand buyCommand = null;

	/**
	 * The current sell command
	 */
	private SellCommand sellCommand = null;

	private SellChest chest;
	private SellWand wand;

	@Override
	public void onEnable() {
		INSTANCE = this;
		shops = new LinkedHashMap<>();
		createFiles();

		if (!setupEconomy()) {
			getLogger().log(Level.INFO, "Vault could not detect an economy plugin!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
		getServer().getPluginCommand("guishop").setExecutor(new GuishopCommand());
		getServer().getPluginCommand("guishopuser").setExecutor(new GuishopUserCommand());
		loadDefaults();
		if (Config.isDynamicPricing() && !setupDynamicPricing()) {
			getLogger().log(Level.INFO, "Could not find a DynamicPriceProvider! Disabling dynamic pricing...");
			Config.setDynamicPricing(false);
		}

		loadShopDefs();

		if (Config.isRegisterCommands()) {
			registerCommands();
		}

		chest = new SellChest(this);
		wand = new SellWand(this);
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
	 * Register the GUIShop commands with the bukkit server. <br>
	 * Accesses the command map via reflection
	 * 
	 */
	private void registerCommands() {
		getLogger().info("Registering commands " + StringUtils.join(Main.BUY_COMMANDS, "|") + " and " + StringUtils.join(Main.SELL_COMMANDS, "|"));

		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			// Unregister old commands
			if (buyCommand != null) {
				buyCommand.unregister(commandMap);
			}

			if (sellCommand != null) {
				sellCommand.unregister(commandMap);
			}

			// Register new commands
			buyCommand = new BuyCommand(new ArrayList<>(Main.BUY_COMMANDS));
			commandMap.register(buyCommand.getName(), buyCommand);

			sellCommand = new SellCommand(new ArrayList<>(Main.SELL_COMMANDS));
			commandMap.register(sellCommand.getName(), sellCommand);

		} catch (IllegalAccessException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	public GuishopUserCommand getUserCommands() {
		return (GuishopUserCommand) getServer().getPluginCommand("guishopuser").getExecutor();
	}

	/**
	 * Check if Vault is present, Check if an Economy plugin is present, if so Hook.
	 */
	private Boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

		if (rsp == null || rsp.getProvider() == null) {
			return false;
		}

		ECONOMY = rsp.getProvider();

		return true;
	}
	
	/**
	 * Find the dynamic price provider if present
	 */
	private boolean setupDynamicPricing() {
		RegisteredServiceProvider<DynamicPriceProvider> rsp = getServer().getServicesManager().getRegistration(DynamicPriceProvider.class);
		
		if (rsp == null || rsp.getProvider() == null) {
			return false;
		}
		DYNAMICPRICING = rsp.getProvider();
		
		return true;
	}

	/**
	 * Load all deault config values, translate colors, store.
	 */
	public void loadDefaults() {
		BUY_COMMANDS.addAll(getMainConfig().getStringList("buy-commands"));
		SELL_COMMANDS.addAll(getMainConfig().getStringList("sell-commands"));
		Config.setRegisterCommands(getMainConfig().getBoolean("register-commands", true));
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
		Config.setAlternateSellEnabled(getMainConfig().getBoolean("alternate-sell-enable", false));
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
		Config.setMenuTitle(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("menu-title", "Menu")));
		Config.setShopTitle(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("shop-title", "Menu &f> &r{shopname}")));
		Config.setSellTitle(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("sell-title", "Menu &f> &rSell")));
		Config.setAltSellTitle(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("alt-sell-title", "Menu &f> &rSell")));
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
		Config.setForwardPageButtonName(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("forward-page-button-name", "")));
		Config.setBackwardPageButtonName(
				ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("backward-page-button-name", "")));
		Config.setAltSellIndicatorMaterial(getMainConfig().getString("alt-sell-indicator-material", "EMERALD"));
		Config.setAltSellAddMaterial(getMainConfig().getString("alt-sell-add-material", "GREEN_STAINED_GLASS_PANE"));
		Config.setAltSellRemoveMaterial(getMainConfig().getString("alt-sell-remove-material", "RED_STAINED_GLASS_PANE"));
		Config.setAltSellQuantity1(getMainConfig().getInt("alt-sell-quantity-1", 1));
		Config.setAltSellQuantity2(getMainConfig().getInt("alt-sell-quantity-2", 10));
		Config.setAltSellQuantity3(getMainConfig().getInt("alt-sell-quantity-3", 64));
		Config.setAltSellConfirmMaterial(getMainConfig().getString("alt-sell-confirm-material", "EMERALD_BLOCK"));
		Config.setAltSellCancelMaterial(getMainConfig().getString("alt-sell-cancel-material", "REDSTONE_BLOCK"));
		Config.setAltSellConfirmName(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("alt-sell-confirm-name", "&a&lConfirm")));
		Config.setAltSellCancelName(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("alt-sell-cancel-name", "&c&lCancel")));
		Config.setAltSellNotEnough(ChatColor.translateAlternateColorCodes('&',
				getMainConfig().getString("alt-sell-not-enough", "&cYou do not have enough items to sell.")));
		Config.getDisabledQty().addAll(getMainConfig().getStringList("disabled-qty-items"));
		Config.setDynamicPricing(getMainConfig().getBoolean("dynamic-pricing", false));
		Config.setDebugMode(getMainConfig().getBoolean("debug-mode"));
	}

	private void loadPRICETABLE() {

		for (String shop : Main.getINSTANCE().getCustomConfig().getKeys(false)) {

			ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);
			if (config == null) {
				log("Check the section for shop " + shop + " in the shops.yml. It was not found.");
				continue;
			}

			assert config != null;
			for (String str : config.getKeys(false)) {

				Item item = new Item();

				ConfigurationSection section = config.getConfigurationSection(str);
				if (section == null) {
					log("Check the config section for item " + str + " in shop " + shop + " the shops.yml. It is not a valid section.");
					continue;
				}

				item.setMaterial((section.contains("id") ? (String) section.get("id") : "AIR"));
				if (item.isAnyPotion()) {
					ConfigurationSection potionSection = section.getConfigurationSection("potion-info");
					if (potionSection != null) {
						item.parsePotionType(potionSection.getString("type"),
								potionSection.getBoolean("splash", false),
								potionSection.getBoolean("extended", false), potionSection.getInt("amplifier", -1));
					}
				}
				item.setMobType((section.contains("mobType") ? (String) section.get("mobType") : "PIG"));

				item.setBuyPrice(section.get("buy-price"));

				item.setSellPrice(section.get("sell-price"));

				item.setItemType(
						section.contains("type") ? ItemType.valueOf((String) section.get("type")) : ItemType.SHOP);

				item.setUseDynamicPricing(section.getBoolean("use-dynamic-price", true));

				ITEMTABLE.put(item.getItemString(), item);
			}
		}
	}

	/**
	 * Force create all YML files.
	 */
	public void createFiles() {

		configf = new File(getDataFolder(), "config.yml");
		specialf = new File(getDataFolder(), "shops.yml");
		sellChestF = new File(getDataFolder(), "sellchest.json");
		econLogFile = new File(getDataFolder(), "economy.log");
		autoSellerLogFile = new File(getDataFolder(), "autoSellEcon.log");

		if(!sellChestF.exists()) {
			sellChestF.getParentFile().mkdir();
			sellChestF.getParentFile().mkdirs();
			try {
				sellChestF.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			saveResource("sellchest.json", false);
		}


		if(!econLogFile.exists()) {
			econLogFile.getParentFile().mkdir();
			econLogFile.getParentFile().mkdirs();
			try {
				econLogFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			saveResource("economy.log", false);
		}

		if(!autoSellerLogFile.exists()) {
			autoSellerLogFile.getParentFile().mkdir();
			autoSellerLogFile.getParentFile().mkdirs();
			try {
				autoSellerLogFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			saveResource("autoSellEcon.log", false);
		}

		if (!configf.exists()) {
			configf.getParentFile().mkdirs();
			saveResource("config.yml", false);
		}

		if (!specialf.exists()) {
			specialf.getParentFile().mkdirs();
			saveResource("shops.yml", false);
		}

		if (!sellChestF.exists()) {
			sellChestF.getParentFile().mkdirs();
			try {
				sellChestF.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				ChestLocation.getMapper().writeValue(getSellChestF(), new ArrayList<ChestLocation>());
			} catch (IOException e) {
				e.printStackTrace();
			}

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

	public void reload(Player player, Boolean ignoreCreator) {
		createFiles();
		shops.clear();
		ITEMTABLE.clear();
		BUY_COMMANDS.clear();
		SELL_COMMANDS.clear();
		loadedShops.clear();
		if (!ignoreCreator) {
			CREATOR.clear();
		}
		reloadConfig();
		reloadCustomConfig();
		loadDefaults();
		loadShopDefs();
		if (Config.isRegisterCommands()) {
			registerCommands();
		}

		if(chest != null) chest.initialize();

		sendMessage(player, "&aGUIShop Reloaded");
	}

	public void reloadCustomConfig() {
		try {
			customConfig.load(specialf);
		} catch (IOException | InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Formats money using the economy plugin's significant digits. <br>
	 * <i>Does not add currency prefixes or suffixes. </i> <br>
	 * <br>
	 * Example: 2.4193 -> 2.42 <br>
	 * Prevents scientific notation being displayed on items.
	 * 
	 * @param value what to format
	 * @return the formatted result
	 */
	public static String economyFormat(double value) {
		int digits = ECONOMY.fractionalDigits();
		return (digits == -1) ? Double.toString(value) : String.format("%." + digits + "f", value);
	}

	public static String placeholderIfy(String input, Player player, Item item) {
		String str = input;

		str = str.replace("{PLAYER_NAME}", player.getName());
		str = str.replace("{PLAYER_UUID}", player.getUniqueId().toString());
		if (item.hasShopName()) {
			str = str.replace("{ITEM_SHOP_NAME}", item.getShopName());
		} else {
			str = str.replace("{ITEM_SHOP_NAME}", XMaterial.matchXMaterial(item.getMaterial()).get().name());
		}
		if (item.hasBuyName()) {
			str = str.replace("{ITEM_BUY_NAME}", item.getBuyName());
		} else {
			str = str.replace("{ITEM_BUY_NAME}", XMaterial.matchXMaterial(item.getMaterial()).get().name());
		}
		if (item.hasBuyPrice()) {
			str = str.replace("{BUY_PRICE}", Double.toString(item.calculateBuyPrice(1)));
		}
		if (item.hasSellPrice()) {
			str = str.replace("{SELL_PRICE}", Double.toString(item.calculateSellPrice(1)));
		}
		str = str.replace("{CURRENCY_SYMBOL}", Config.getCurrency());
		str = str.replace("{CURRENCY_SUFFIX}", Config.getCurrencySuffix());

		return str;
	}

	public static void log(String input) {
		Main.getINSTANCE().getLogger().log(Level.WARNING, "[GUISHOP]: " + input);
	}

	public static void debugLog(String input) {
		if (Config.isDebugMode()) {
			Main.getINSTANCE().getLogger().log(Level.WARNING, "[GUISHOP][DEBUG]: " + input);
		}
	}
	
	/**
	 * Sends a message using '{@literal &}' colour codes instead of '§' codes.
	 * 
	 * @param commandSender the recipient
	 * @param message the message, which uses {@literal &} colour codes
	 */
	public static void sendMessage(CommandSender commandSender, String message) {
		commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

}
