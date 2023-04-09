package com.pablo67340.guishop.util;

import com.cryptomorin.xseries.XSound;
import com.pablo67340.guishop.GUIShop;
import static com.pablo67340.guishop.GUIShop.BUY_COMMANDS;
import static com.pablo67340.guishop.GUIShop.SELL_COMMANDS;
import static com.pablo67340.guishop.GUIShop.debugLog;
import static com.pablo67340.guishop.GUIShop.log;
import com.pablo67340.guishop.commands.CommandsInterceptor;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.CommandsMode;
import com.pablo67340.guishop.messages.MessageSystem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigManager {

    /**
     * True/False if GUIShop should use signs only.
     */
    @Getter
    @Setter
    private static boolean signsOnly, disableBackButton, disableEscapeBack, alternateSellEnabled, soundEnabled,
            dynamicPricing, debugMode, sellSkullUUID;

    /**
     * The commands mode, determines whether to intercept commands, register
     * them, or neither
     *
     */
    @Getter
    @Setter
    private static CommandsMode commandsMode;

    /**
     * Common Language strings set in configuration.
     *
     */
    @Getter
    @Setter
    private static String added, cantSell, cantBuy, prefix, purchased, menuName, notEnoughPre, notEnoughPost, signTitle,
            sellCommand, menuTitle, shopTitle, sellTitle, altSellTitle, sold, taken, sound, full, currency,
            noPermission, qtyTitle, currencySuffix, backButtonItem, backButtonText, cannotSell, cannotBuy, buyLore,
            sellLore, freeLore, forwardPageButtonName, backwardPageButtonName, altSellAddMaterial,
            altSellRemoveMaterial, altSellIndicatorMaterial, altSellConfirmMaterial, altSellCancelMaterial,
            altSellConfirmName, altSellCancelName, altSellNotEnough, menuShopPageNumber, altSellIncreaseTitle, altSellDecreaseTitle;

    /**
     * Integers from the config
     *
     */
    @Getter
    @Setter
    private static int altSellQuantity1, altSellQuantity2, altSellQuantity3;

    @Getter
    @Setter
    private static List<String> disabledWorlds;

    /**
     * The overridden config file objects.
     */
    @Getter
    private static File configFile, shopFile, menuFile, cacheFile, dictionaryFile, inventoryFile, messagesFile;

    /**
     * The configs FileConfiguration object.
     */
    @Getter
    private static FileConfiguration mainConfig, shopConfig, menuConfig, cacheConfig, inventoryConfig, messagesConfig;

    @Getter    
    public MessageSystem messageSystem;

    @Getter
    public File dataFolder;

    public ConfigManager() {
        this.messageSystem = new MessageSystem();
        this.dataFolder = GUIShop.getINSTANCE().getDataFolder();
    }

    public void initConfigs() {
        mainConfig = new YamlConfiguration();
        shopConfig = new YamlConfiguration();
        menuConfig = new YamlConfiguration();
        cacheConfig = new YamlConfiguration();
        inventoryConfig = new YamlConfiguration();
        messagesConfig = new YamlConfiguration();

        configFile = new File(this.dataFolder, "config.yml");
        shopFile = new File(this.dataFolder, "shops.yml");
        menuFile = new File(this.dataFolder, "menu.yml");
        cacheFile = new File(this.dataFolder, "/Data/cache.yml");
        inventoryFile = new File(this.dataFolder.getPath(), "/Data/inventories.yml");
        messagesFile = new File(getDataFolder(), "messages.yml");

        configFile.getParentFile().mkdirs();

        if (!configFile.exists()) {
            GUIShop.getINSTANCE().saveResource("config.yml", false);
        }

        if (!shopFile.exists()) {
            GUIShop.getINSTANCE().saveResource("shops.yml", false);
        }

        if (!menuFile.exists()) {
            GUIShop.getINSTANCE().saveResource("menu.yml", false);
        }

        if (!cacheFile.exists()) {
            cacheFile.getParentFile().mkdirs();
            copy("cache.yml", GUIShop.getINSTANCE().getClass().getClassLoader().getResourceAsStream("cache.yml"), cacheFile.getPath());
        }

        if (!inventoryFile.exists()) {
            inventoryFile.getParentFile().mkdirs();
            copy("inventories.yml", GUIShop.getINSTANCE().getClass().getClassLoader().getResourceAsStream("inventories.yml"), inventoryFile.getPath());
        }

        if (!messagesFile.exists()) {
            GUIShop.getINSTANCE().saveResource("messages.yml", false);
        }

        try {
            mainConfig.load(configFile);
            shopConfig.load(shopFile);
            menuConfig.load(menuFile);
            cacheConfig.load(cacheFile);
            inventoryConfig.load(inventoryFile);
            messagesConfig.load(messagesFile);
            messageSystem.loadCustomMessages(messagesConfig);
            loadCache();
            loadDefaults();
            warmup();
            initWriteCache();
            initDictionary();
        } catch (IOException | InvalidConfigurationException e) {
            log("Error Main config: " + e.getMessage());
        }

    }

    public void loadCache() {
        debugLog("Loading Cache...");
        if (getCacheConfig().contains("player-heads")) {
            ConfigurationSection config = getCacheConfig().getConfigurationSection("player-heads");
            for (Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
                cachedHeads.put(entry.getKey(), (String) entry.getValue());
            }
        }
        debugLog("Cache loaded successfully!");
    }

    public void reloadConfigs() {
        try {
            menuConfig.load(menuFile);
            cacheConfig.load(cacheFile);
            shopConfig.load(shopFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
        }
    }

    /**
     * Load all default config values, translate colors, store.
     */
    public void loadDefaults() {
        if (mainConfig == null) {
            log("An error occurred while loading the config.yml! Please correct the errors and try again.");
            return;
        }

        // All buy commands
        if (mainConfig.get("buy-commands") instanceof List) {
            BUY_COMMANDS.addAll(mainConfig.getStringList("buy-commands"));
        } else {
            BUY_COMMANDS.add(mainConfig.getString("buy-commands"));
        }

        // All sell commands
        if (mainConfig.get("sell-commands") instanceof List) {
            SELL_COMMANDS.addAll(mainConfig.getStringList("sell-commands"));
        } else {
            SELL_COMMANDS.add(mainConfig.getString("sell-commands"));
        }

        // The command mode GUIShop should use
        Config.setCommandsMode(CommandsMode.parseFromConfig(mainConfig.getString("commands-mode", "REGISTER")));

        // Signs only?
        Config.setSignsOnly(mainConfig.getBoolean("signs-only", false));

        // The title for signs
        Config.getTitlesConfig().setSignTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(mainConfig.getString("titles.sign", "&f[&cGUIShop&f]"))));

        // Disable the back button
        Config.setDisableBackButton(mainConfig.getBoolean("disable-back-button", true));

        // Disable the feature to escape back
        Config.setDisableEscapeBack(mainConfig.getBoolean("disable-escape-back", false));

        // Disable the feature to escape back from quantity (If the upper one is disabled, this is enabled)
        Config.setDisableEscapeBackQuantity(mainConfig.getBoolean("disable-escape-back-quantity", true));

        // Enable alternate sell
        Config.setAlternateSellEnabled(mainConfig.getBoolean("alternate-sell-enable", false));

        // The sound when buying something
        try {
            XSound.matchXSound(mainConfig.getString("purchase-sound", "ENTITY_PLAYER_LEVELUP")).get().parseSound();
            Config.setSound(mainConfig.getString("purchase-sound", "ENTITY_PLAYER_LEVELUP"));
        } catch (NoSuchElementException | NullPointerException exception) {
            Config.setSound("ENTITY_PLAYER_LEVELUP");
            log("&cThe buy sound input in the config.yml is NOT valid! \n&fCurrent: &c"
                    + mainConfig.getString("purchase-sound", "ENTITY_PLAYER_LEVELUP") + " &f| Using default: &cENTITY_PLAYER_LEVELUP");
        }

        // If the sound should be enabled
        Config.setSoundEnabled(mainConfig.getBoolean("enable-sound", true));

        // Menu title
        Config.getTitlesConfig().setMenuTitle(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("titles.menu", "Menu")));

        // Menu title when there are multiple pages
        Config.getTitlesConfig().setMenuShopPageNumber(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("titles.menu-shop-pagenumber", "&f> Page: &e%number%")));

        // Shop title
        Config.getTitlesConfig().setShopTitle(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("titles.shop", "Menu &f> &r%shopname%")));

        // Sell title
        Config.getTitlesConfig().setSellTitle(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("titles.sell", "Menu &f> &rSell")));

        // Alternate sell title
        Config.getAltSellConfig().setTitle(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("titles.alt-sell", "Menu &f> &rSell")));

        // Quantity title
        Config.getTitlesConfig().setQtyTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(mainConfig.getString("titles.qty", "&4Select amount"))));

        // Value title
        Config.getTitlesConfig().setValueTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(mainConfig.getString("titles.value", "&2Item values"))));

        // The material for the indicator
        Config.getAltSellConfig().setIndicatorMaterial(mainConfig.getString("alt-sell.indicator-material", "EMERALD"));

        // Alternate sell add item material
        Config.getAltSellConfig().setAddMaterial(mainConfig.getString("alt-sell.add-material", "GREEN_STAINED_GLASS_PANE"));

        // Alternate sell remove item material
        Config.getAltSellConfig().setRemoveMaterial(mainConfig.getString("alt-sell.remove-material", "RED_STAINED_GLASS_PANE"));

        // Alternate sell quantities
        Config.getAltSellConfig().setQuantity1(mainConfig.getInt("alt-sell.quantity-1", 1));
        Config.getAltSellConfig().setQuantity2(mainConfig.getInt("alt-sell.quantity-2", 10));
        Config.getAltSellConfig().setQuantity3(mainConfig.getInt("alt-sell.quantity-3", 64));

        // Alternate sell confirm item material
        Config.getAltSellConfig().setConfirmMaterial(mainConfig.getString("alt-sell.confirm-material", "EMERALD_BLOCK"));

        // Alternate sell cancel item material
        Config.getAltSellConfig().setCancelMaterial(mainConfig.getString("alt-sell.cancel-material", "REDSTONE_BLOCK"));

        // Alternate sell confirm item display name
        Config.getAltSellConfig().setConfirmName(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("alt-sell.confirm-name", "&a&lConfirm")));

        // Alternate sell cancel item display name
        Config.getAltSellConfig().setCancelName(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("alt-sell.cancel-name", "&c&lCancel")));

        // If dynamic pricing should be enabled
        Config.setDynamicPricing(mainConfig.getBoolean("dynamic-pricing", false));

        // If debug should be enabled
        Config.setDebugMode(mainConfig.getBoolean("debug-mode", false));

        // The disabled worlds for GUIShop
        Config.setDisabledWorlds(mainConfig.getStringList("disabled-worlds"));

        // Separate uuid of heads when selling
        Config.setSellSkullUUID(!mainConfig.contains("skull-uuid-selling") || mainConfig.getBoolean("skull-uuid-selling", true));

        // Disables dynamic pricing if no provider was found
        if (Config.isDynamicPricing() && !setupDynamicPricing()) {
            log("Could not find a DynamicPriceProvider! Disabling dynamic pricing...");
            Config.setDynamicPricing(false);
        }

        // Increase item display name
        Config.getAltSellConfig().setIncreaseTitle(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("alt-sell.increase-title", "&aIncrease quantity by %amount%")));

        // Decrease item display name
        Config.getAltSellConfig().setDecreaseTitle(ChatColor.translateAlternateColorCodes('&',
                mainConfig.getString("alt-sell.decrease-title", "&aDecrease quantity by %amount%")));

        // If the transaction logging to the console should be enabled
        Config.setTransactionLog(mainConfig.getBoolean("transaction-log", false));

        // Register commands
        if (Config.getCommandsMode() == CommandsMode.INTERCEPT) {
            CommandsInterceptor.register();
        } else {
            GUIShop.getINSTANCE().commandManager.registerCommands();
        }

        // Load the buttons
        Config.getButtonConfig().createButtons();

        // Load the lores
        Map<String, String> lores = new ConcurrentHashMap<>();

        for (Map.Entry<String, Object> entry : mainConfig.getConfigurationSection("lores").getValues(false).entrySet()) {
            lores.put(entry.getKey(), ChatColor.translateAlternateColorCodes('&', entry.getValue().toString()));
        }

        for (Map.Entry<String, Object> entry : mainConfig.getConfigurationSection("lores").getValues(false).entrySet()) {
            lores.put(entry.getKey(), ChatColor.translateAlternateColorCodes('&', entry.getValue().toString()));
        }

        Config.getLoreConfig().lores.putAll(lores);
    }
    
    public void handleDictionary() {
        dictionaryFile = new File(getDataFolder().getPath() + "/Dictionary");
        if (!dictionaryFile.exists()) {
            dictionaryFile.mkdirs();
        }

        File potionDest = new File(dictionaryFile.getPath() + "/potion-names.txt");
        File spawnerDest = new File(dictionaryFile.getPath() + "/spawner-names.txt");
        File materialsDest = new File(dictionaryFile.getPath() + "/material-names.txt");
        File enchantmentDest = new File(dictionaryFile.getPath() + "/enchantment-names.txt");
        File flagDest = new File(dictionaryFile.getPath() + "/item-flags.txt");
        File readmeDest = new File(getDataFolder().getPath() + "/README.txt");

        if (!potionDest.exists()) {
            copy("potion-names.txt", getClass().getClassLoader().getResourceAsStream("potion-names.txt"), potionDest.getPath());
        }
        if (!spawnerDest.exists()) {
            copy("spawner-names.txt", getClass().getClassLoader().getResourceAsStream("spawner-names.txt"), spawnerDest.getPath());
        }
        if (!materialsDest.exists()) {
            copy("material-names.txt", getClass().getClassLoader().getResourceAsStream("material-names.txt"), materialsDest.getPath());
        }
        if (!enchantmentDest.exists()) {
            copy("enchantment-names.txt", getClass().getClassLoader().getResourceAsStream("enchantment-names.txt"), enchantmentDest.getPath());
        }
        if (!flagDest.exists()) {
            copy("item-flags.txt", getClass().getClassLoader().getResourceAsStream("item-flags.txt"), flagDest.getPath());
        }
        if (!readmeDest.exists()) {
            copy("README.txt", getClass().getClassLoader().getResourceAsStream("README.txt"), readmeDest.getPath());
        }
    }

    /**
     * Copy a file from source to destination.
     *
     * @param source the source
     * @param destination the destination
     * @param name the name of the file
     */
    public void copy(String name, InputStream source, String destination) {
        debugLog("Extracting: " + name + " -> " + "/plugins/GUIShop/Dictionary/" + name);
        try {
            Files.copy(source, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log("Error extracting Dictionary files: " + ex.getMessage());
        }
    }

}
