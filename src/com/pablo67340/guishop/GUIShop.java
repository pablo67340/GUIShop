package com.pablo67340.guishop;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.commands.*;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.CommandsMode;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.MenuItem;
import com.pablo67340.guishop.definition.MenuPage;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.messages.MessageSystem;
import com.pablo67340.guishop.util.RowChart;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;

public final class GUIShop extends JavaPlugin {

    /**
     * The overridden config file objects.
     */
    @Getter
    private File configFile, shopFile, menuFile, cacheFile, dictionaryFile, inventoryFile, messagesFile;

    /**
     * The configs FileConfiguration object.
     */
    @Getter
    private FileConfiguration mainConfig, shopConfig, menuConfig, cacheConfig, inventoryConfig, messagesConfig;

    /**
     * An instance Vault's Economy.
     */
    @Getter
    private static Economy ECONOMY;
    
    @Getter
    private static Permission perms;

    /**
     * the instance of the dynamic price provider, if dynamic pricing is used
     */
    @Getter
    private static DynamicPriceProvider DYNAMICPRICING;

    /**
     * An instance of this class.
     */
    @Getter
    public static GUIShop INSTANCE;

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
    @Setter
    public static boolean noEconomySystem = false;

    @Getter
    public Map<String, Object> loadedShops = new HashMap<>();

    @Getter
    @Setter
    private MenuItem loadedMenu = null;

    @Getter
    private final Map<String, List<Item>> ITEMTABLE = new HashMap<>();

    @Getter
    private final Map<String, String> cachedHeads = new HashMap<>();

    /**
     * A {@link Map} that will store our Creators when the server first starts.
     */
    @Getter
    public static final List<UUID> CREATOR = new ArrayList<>();

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public static final RowChart rowChart = new RowChart();

    @Getter
    @Setter
    public static ArrayList<String> debugLogCache = new ArrayList<>();

    @Getter
    @Setter
    public static ArrayList<String> transactionLogCache = new ArrayList<>();

    @Getter
    @Setter
    public static ArrayList<String> mainLogCache = new ArrayList<>();

    public MessageSystem messageSystem;

    private BuyCommand buyCommand = null;

    private SellCommand sellCommand = null;

    @Getter
    @Setter
    public Boolean isReload = false;

    @Override
    public void onEnable() {
        INSTANCE = this;
        messageSystem = new MessageSystem(this, getResource("internal_messages.yml"));
        createFiles();

        initWriteCache();

        if (!setupEconomy()) {
            getLogger().log(Level.WARNING, "Vault could not detect an economy plugin!");
            setNoEconomySystem(true);
            return;
        }

        getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
        getServer().getPluginCommand("guishop").setExecutor(new GuishopCommand());
        getServer().getPluginCommand("guishopuser").setExecutor(new UserCommand());
    }

    /**
     * Register/unregister the GUIShop commands with the bukkit server. <br>
     * Accesses the command map via reflection
     *
     * @param onlyUnregister if true, new commands will not be registered, only,
     *                       existing ones will be unregistered
     */
    private void registerCommands(boolean onlyUnregister) {
        if (onlyUnregister && buyCommand == null && sellCommand == null) {
            // Nothing is registered, no need to do anything
            return;
        }

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

            if (onlyUnregister) {
                return;
            }

            // Register buy commands if there are any
            if (!GUIShop.BUY_COMMANDS.isEmpty()) {
                debugLog("Registering/unregistering shop commands: " + StringUtils.join(GUIShop.BUY_COMMANDS, ", "));
                buyCommand = new BuyCommand(new ArrayList<>(GUIShop.BUY_COMMANDS));
                commandMap.register(buyCommand.getName(), buyCommand);
            }

            // Register sell commands if there are any
            if (!GUIShop.SELL_COMMANDS.isEmpty()) {
                debugLog("Registering/unregistering sell commands: " + StringUtils.join(GUIShop.SELL_COMMANDS, ", "));
                sellCommand = new SellCommand(new ArrayList<>(GUIShop.SELL_COMMANDS));
                commandMap.register(sellCommand.getName(), sellCommand);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log("Error registering commands: " + e.getMessage());
        }
    }

    public UserCommand getUserCommands() {
        return (UserCommand) getServer().getPluginCommand("guishopuser").getExecutor();
    }

    /**
     * Check if Vault is present, check if an Economy plugin is present, if so,
     * hook.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        RegisteredServiceProvider<Permission> rsp2 = getServer().getServicesManager().getRegistration(Permission.class);
        

        if (rsp == null || rsp2 == null) {
            return false;
        } 

        ECONOMY = rsp.getProvider();
        perms = rsp2.getProvider();

        return true;
    }

    /**
     * Find the dynamic price provider if present
     */
    private boolean setupDynamicPricing() {
        RegisteredServiceProvider<DynamicPriceProvider> rsp = getServer().getServicesManager().getRegistration(DynamicPriceProvider.class);

        if (rsp == null) {
            return false;
        } else {
            rsp.getProvider();
        }
        DYNAMICPRICING = rsp.getProvider();

        return true;
    }

    /**
     * Load all default config values, translate colors, store.
     */
    public void loadDefaults() {
        if (getMainConfig() == null) {
            getLogger().log(Level.WARNING, "An error occurred while loading the config.yml! Please correct the errors and try again.");
            return;
        }

        // All buy commands
        if (getMainConfig().get("buy-commands") instanceof List) {
            BUY_COMMANDS.addAll(getMainConfig().getStringList("buy-commands"));
        } else {
            BUY_COMMANDS.add(getMainConfig().getString("buy-commands"));
        }

        // All sell commands
        if (getMainConfig().get("sell-commands") instanceof List) {
            SELL_COMMANDS.addAll(getMainConfig().getStringList("sell-commands"));
        } else {
            SELL_COMMANDS.add(getMainConfig().getString("sell-commands"));
        }

        // The command mode GUIShop should use
        Config.setCommandsMode(CommandsMode.parseFromConfig(getMainConfig().getString("commands-mode", "REGISTER")));

        // Signs only?
        Config.setSignsOnly(getMainConfig().getBoolean("signs-only", false));

        // The title for signs
        Config.getTitlesConfig().setSignTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("titles.sign", "&f[&cGUIShop&f]"))));

        // Disable the back button
        Config.setDisableBackButton(getMainConfig().getBoolean("disable-back-button", true));

        // Disable the feature to escape back
        Config.setDisableEscapeBack(getMainConfig().getBoolean("disable-escape-back", false));

        // Disable the feature to escape back from quantity (If the upper one is disabled, this is enabled)
        Config.setDisableEscapeBackQuantity(getMainConfig().getBoolean("disable-escape-back-quantity", true));

        // Enable alternate sell
        Config.setAlternateSellEnabled(getMainConfig().getBoolean("alternate-sell-enable", false));

        // The sound when buying something
        try {
            XSound.matchXSound(getMainConfig().getString("purchase-sound", "ENTITY_PLAYER_LEVELUP")).get().parseSound();
            Config.setSound(getMainConfig().getString("purchase-sound", "ENTITY_PLAYER_LEVELUP"));
        } catch (NoSuchElementException | NullPointerException exception) {
            Config.setSound("ENTITY_PLAYER_LEVELUP");
            log("&cThe buy sound input in the config.yml is NOT valid! \n&fCurrent: &c"
                    + getMainConfig().getString("purchase-sound", "ENTITY_PLAYER_LEVELUP") + " &f| Using default: &cENTITY_PLAYER_LEVELUP");
        }

        // If the sound should be enabled
        Config.setSoundEnabled(getMainConfig().getBoolean("enable-sound", true));

        // Menu title
        Config.getTitlesConfig().setMenuTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("titles.menu", "Menu")));

        // Menu title when there are multiple pages
        Config.getTitlesConfig().setMenuShopPageNumber(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("titles.menu-shop-pagenumber", "&f> Page: &e%number%")));

        // Shop title
        Config.getTitlesConfig().setShopTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("titles.shop", "Menu &f> &r%shopname%")));

        // Sell title
        Config.getTitlesConfig().setSellTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("titles.sell", "Menu &f> &rSell")));

        // Alternate sell title
        Config.getAltSellConfig().setTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("titles.alt-sell", "Menu &f> &rSell")));

        // Quantity title
        Config.getTitlesConfig().setQtyTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("titles.qty", "&4Select amount"))));

        // Value title
        Config.getTitlesConfig().setValueTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("titles.value", "&2Item values"))));

        // The material for the indicator
        Config.getAltSellConfig().setIndicatorMaterial(getMainConfig().getString("alt-sell.indicator-material", "EMERALD"));

        // Alternate sell add item material
        Config.getAltSellConfig().setAddMaterial(getMainConfig().getString("alt-sell.add-material", "GREEN_STAINED_GLASS_PANE"));

        // Alternate sell remove item material
        Config.getAltSellConfig().setRemoveMaterial(getMainConfig().getString("alt-sell.remove-material", "RED_STAINED_GLASS_PANE"));

        // Alternate sell quantities
        Config.getAltSellConfig().setQuantity1(getMainConfig().getInt("alt-sell.quantity-1", 1));
        Config.getAltSellConfig().setQuantity2(getMainConfig().getInt("alt-sell.quantity-2", 10));
        Config.getAltSellConfig().setQuantity3(getMainConfig().getInt("alt-sell.quantity-3", 64));

        // Alternate sell confirm item material
        Config.getAltSellConfig().setConfirmMaterial(getMainConfig().getString("alt-sell.confirm-material", "EMERALD_BLOCK"));

        // Alternate sell cancel item material
        Config.getAltSellConfig().setCancelMaterial(getMainConfig().getString("alt-sell.cancel-material", "REDSTONE_BLOCK"));

        // Alternate sell confirm item display name
        Config.getAltSellConfig().setConfirmName(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell.confirm-name", "&a&lConfirm")));

        // Alternate sell cancel item display name
        Config.getAltSellConfig().setCancelName(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell.cancel-name", "&c&lCancel")));

        // If dynamic pricing should be enabled
        Config.setDynamicPricing(getMainConfig().getBoolean("dynamic-pricing", false));

        // If debug should be enabled
        Config.setDebugMode(getMainConfig().getBoolean("debug-mode", false));

        // The disabled worlds for GUIShop
        Config.setDisabledWorlds(getMainConfig().getStringList("disabled-worlds"));

        // Separate uuid of heads when selling
        Config.setSellSkullUUID(!getMainConfig().contains("skull-uuid-selling") || getMainConfig().getBoolean("skull-uuid-selling", true));

        // Disables dynamic pricing if no provider was found
        if (Config.isDynamicPricing() && !setupDynamicPricing()) {
            getLogger().log(Level.INFO, "Could not find a DynamicPriceProvider! Disabling dynamic pricing...");
            Config.setDynamicPricing(false);
        }

        // Increase item display name
        Config.getAltSellConfig().setIncreaseTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell.increase-title", "&aIncrease quantity by %amount%")));

        // Decrease item display name
        Config.getAltSellConfig().setDecreaseTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell.decrease-title", "&aDecrease quantity by %amount%")));

        // If the transaction logging to the console should be enabled
        Config.setTransactionLog(getMainConfig().getBoolean("transaction-log", false));

        // Register commands
        if (Config.getCommandsMode() == CommandsMode.INTERCEPT) {
            CommandsInterceptor.register();
        } else {
            registerCommands(false);
        }

        // Load the buttons
        Config.getButtonConfig().createButtons();

        // Load the lores
        Map<String, String> lores = new ConcurrentHashMap<>();

        FileConfiguration defaultConfig = new YamlConfiguration();
        try {
            defaultConfig.load(new InputStreamReader(this.getClassLoader().getResourceAsStream("config.yml")));

            for (Map.Entry<String, Object> entry : defaultConfig.getConfigurationSection("lores").getValues(false).entrySet()) {
                lores.put(entry.getKey(), ChatColor.translateAlternateColorCodes('&', entry.getValue().toString()));
            }
        } catch (Exception exception) {
            log("Error loading default config from the JAR file (Please report this to our support server!):");
            exception.printStackTrace();
        }

        for (Map.Entry<String, Object> entry : getMainConfig().getConfigurationSection("lores").getValues(false).entrySet()) {
            lores.put(entry.getKey(), ChatColor.translateAlternateColorCodes('&', entry.getValue().toString()));
        }

        Config.getLoreConfig().lores.putAll(lores);
    }

    public void handleMessages() {
        messagesConfig = new YamlConfiguration();
        messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }

        try {
            messagesConfig.load(messagesFile);
            messageSystem.loadCustomMessages(messagesConfig);
        } catch (IOException | InvalidConfigurationException exception) {
            log("Error Messages config: " + exception.getMessage());
        }
    }

    /**
     * Force create all YML files.
     */
    public void handleConfig() {
        mainConfig = new YamlConfiguration();
        configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        try {
            mainConfig.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error Main config: " + e.getMessage());
        }
        loadDefaults();
    }

    public void handleShopsConfig() {
        shopFile = new File(getDataFolder(), "shops.yml");
        if (!shopFile.exists()) {
            shopFile.getParentFile().mkdirs();
            saveResource("shops.yml", false);
        }

        shopConfig = new YamlConfiguration();
        try {
            shopConfig.load(shopFile);
            warmup();
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Shop config: " + e.getMessage());
        }
    }

    public void handleMenuConfig() {
        menuFile = new File(getDataFolder(), "menu.yml");
        if (!menuFile.exists()) {
            menuFile.getParentFile().mkdirs();
            saveResource("menu.yml", false);
        }

        menuConfig = new YamlConfiguration();
        try {
            menuConfig.load(menuFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Menu config: " + e.getMessage());
        }
    }

    public void handleCacheConfig() {
        cacheFile = new File(getDataFolder(), "/Data/cache.yml");
        if (!cacheFile.exists()) {
            cacheFile.getParentFile().mkdirs();
            copy("cache.yml", getClass().getClassLoader().getResourceAsStream("cache.yml"), cacheFile.getPath());
        }

        cacheConfig = new YamlConfiguration();
        try {
            cacheConfig.load(cacheFile);
            loadCache();
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Cache config: " + e.getMessage());
        }
    }

    public void handleInventoryConfig() {
        inventoryFile = new File(getDataFolder().getPath(), "/Data/inventories.yml");
        if (!inventoryFile.exists()) {
            inventoryFile.getParentFile().mkdirs();
            copy("inventories.yml", getClass().getClassLoader().getResourceAsStream("inventories.yml"), inventoryFile.getPath());
        }

        inventoryConfig = new YamlConfiguration();
        try {
            inventoryConfig.load(inventoryFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Inventories config: " + e.getMessage());
        }
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

    public void createFiles() {
        // Load all configs separately in their own threads, to prevent
        // blocking if one config fails to load, but others do not.
        // This will correct error logging when it's time to find out
        // which config is causing a problem.
        new Thread(this::handleConfig).start();
        new Thread(this::handleCacheConfig).start();
        new Thread(this::handleDictionary).start();
        new Thread(this::handleMessages).start();
        new Thread(this::handleMenuConfig).start();
        new Thread(this::handleShopsConfig).start();
        new Thread(this::handleInventoryConfig).start();
    }

    /**
     * Copy a file from source to destination.
     *
     * @param source      the source
     * @param destination the destination
     * @param name        the name of the file
     */
    public void copy(String name, InputStream source, String destination) {
        debugLog("Extracting: " + name + " -> " + "/plugins/GUIShop/Dictionary/" + name);
        try {
            Files.copy(source, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log("Error extracting Dictionary files: " + ex.getMessage());
        }
    }

    public void warmup() {
        long startTime = System.currentTimeMillis();
        new Menu().loadItems(true);
        for (MenuPage page : loadedMenu.getPages().values()) {
            for (Item item : page.getItems().values()) {
                if (item.getTargetShop() != null) {
                    debugLog("Starting Warmup for Shop: " + item.getTargetShop());
                    new Shop(item.getTargetShop()).loadItems(true);
                }
            }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        debugLog("Item warming completed in: " + estimatedTime + "ms");
    }

    public void reload(CommandSender sender, boolean ignoreCreator) {
        Bukkit.getOnlinePlayers().stream().filter(player -> player.getOpenInventory() != null &&
                (player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "")) ||
                        player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getShopTitle().replace("%shopname%", "")) ||
                        player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getQtyTitle()) ||
                        player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getSellTitle()) ||
                        player.getOpenInventory().getTitle().contains(Config.getAltSellConfig().getTitle()) ||
                        player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getValueTitle()))
        ).forEach(Player::closeInventory);

        this.setIsReload(true);

        log("GUIShop reloaded");

        ITEMTABLE.clear();
        BUY_COMMANDS.clear();
        SELL_COMMANDS.clear();
        loadedShops.clear();
        loadedMenu = null;

        if (!ignoreCreator) {
            CREATOR.clear();
        }

        createFiles();
        reloadConfig();
        reloadShopConfig();
        reloadMenuConfig();
        reloadCacheConfig();
        loadDefaults();
        loadCache();
        warmup();

        // If the CommandsMode is REGISTER, register/re-register the commands
        // Otherwise, unregister the commands
        CommandsMode cmdMode = Config.getCommandsMode();
        registerCommands(cmdMode != CommandsMode.REGISTER);

        // Intercept commands using the listener, if configured
        if (cmdMode == CommandsMode.INTERCEPT) {
            CommandsInterceptor.register();

        } else {
            // Unregisters previous command listener
            CommandsInterceptor.unregister();
        }

        sendPrefix(sender, "reload.execute");

        this.setIsReload(false);
    }

    public void loadCache() {
        debugLog("Loading Cache...");
        if (getCacheConfig().contains("player-heads")) {
            ConfigurationSection config = getCacheConfig().getConfigurationSection("player-heads");
            for (Entry<String, Object> entry : config.getValues(false).entrySet()) {
                cachedHeads.put(entry.getKey(), (String) entry.getValue());
            }
        }
        debugLog("Cache loaded successfully!");
    }

    public void reloadShopConfig() {
        try {
            shopConfig.load(shopFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
        }
    }

    public void reloadMenuConfig() {
        try {
            menuConfig.load(menuFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
        }
    }

    public void reloadCacheConfig() {
        try {
            cacheConfig.load(cacheFile);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
        }
    }

    public void initWriteCache() {
        File mainLog = new File(getDataFolder().getPath(), "/Logs/main.log");
        File debugLog = new File(getDataFolder().getPath(), "/Logs/debug.log");
        File transactionLog = new File(getDataFolder().getPath(), "/Logs/transaction.log");

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (!mainLog.exists()) {
                    mainLog.getParentFile().mkdirs();
                    mainLog.createNewFile();
                } else {
                    if (Files.size(mainLog.toPath()) >= 52428800) {
                        mainLog.delete();
                        mainLog.createNewFile();
                    }
                }

                if (!debugLog.exists()) {
                    transactionLog.getParentFile().mkdirs();
                    transactionLog.createNewFile();
                } else {
                    if (Files.size(debugLog.toPath()) >= 52428800) {
                        debugLog.delete();
                        debugLog.createNewFile();
                    }
                }

                if (!transactionLog.exists()) {
                    transactionLog.getParentFile().mkdirs();
                    transactionLog.createNewFile();
                } else {
                    if (Files.size(transactionLog.toPath()) >= 52428800) {
                        transactionLog.delete();
                        transactionLog.createNewFile();
                    }
                }
            } catch (IOException exception) {
                Bukkit.getLogger().warning("An error occurred while trying to crete/delete a log file!");
                exception.printStackTrace();
            }

            write(mainLog.toPath(), getMainLogCache());
            setMainLogCache(new ArrayList<>());

            write(debugLog.toPath(), getDebugLogCache());
            setDebugLogCache(new ArrayList<>());

            write(transactionLog.toPath(), getTransactionLogCache());
            setTransactionLogCache(new ArrayList<>());
        }, 40, 20);
    }

    public void write(Path path, List<String> write) {
        try {
            Files.write(
                    path,
                    write,
                    StandardCharsets.UTF_8,
                    Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (Throwable exception) {
            Bukkit.getLogger().warning("An error occurred while trying to write to a logging file! (" + path + ")");
            exception.printStackTrace();
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
    public static String economyFormat(BigDecimal value) {
        int digits = ECONOMY.fractionalDigits();
        return (digits == -1) ? value.toPlainString() : String.format("%." + digits + "f", value);
    }

    public static String placeholderIfy(String input, Player player, Item item) {
        String string = ChatColor.translateAlternateColorCodes('&', input);

        if (item.hasShopName()) {
            string = string.replace("%item_shop_name%", item.getShopName());
        } else {
            string = string.replace("%item_shop_name%", XMaterial.matchXMaterial(item.getMaterial()).get().name());
        }

        if (item.hasBuyName()) {
            string = string.replace("%item_buy_name%", item.getBuyName());
        } else {
            string = string.replace("%item_buy_name%", XMaterial.matchXMaterial(item.getMaterial()).get().name());
        }

        if (item.hasBuyPrice()) {
            string = string.replace("%buy_price%", item.calculateBuyPrice(1).toPlainString());
        }

        if (item.hasSellPrice()) {
            string = string.replace("%sell_price%", item.calculateSellPrice(1).toPlainString());
        }

        string = string.replace("%currency_symbol%", getINSTANCE().messageSystem.translate("messages.currency-prefix"));
        string = string.replace("%currency_suffix%", getINSTANCE().messageSystem.translate("messages.currency-suffix"));

        if (player != null) {
            string = string.replace("%player_name%", player.getName());
            string = string.replace("%player_uuid%", player.getUniqueId().toString());
            string = string.replace("%player_world%", player.getLocation().getWorld().getName());
            string = string.replace("%player_balance%", GUIShop.getECONOMY().format(GUIShop.getECONOMY().getBalance(player)));

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                string = PlaceholderAPI.setPlaceholders(player, string);
            }
        }

        return string;
    }

    public static void log(String input) {
        GUIShop.getINSTANCE().getLogger().log(Level.INFO, "LOG: {0}", input);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

        getMainLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] LOG: " + input);
    }

    public static void debugLog(String input) {
        if (Config.isDebugMode()) {
            GUIShop.getINSTANCE().getLogger().log(Level.INFO, "DEBUG: {0}", input);
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

        getDebugLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] DEBUG: " + input);
    }

    public static void transactionLog(String input) {
        if (Config.isTransactionLog()) {
            GUIShop.getINSTANCE().getLogger().log(Level.INFO, "TRANSACTION: {0}", input);
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

        getTransactionLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] TRANSACTION: " + input);
    }

    /**
     * Sends a message to the sender with the translated path and optional
     * placeholders
     *
     * @param sender The receiver
     * @param path   The path to the message
     * @param params Optional, the placeholder replacements
     */
    public static void sendPrefix(CommandSender sender, String path, Object... params) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getINSTANCE().messageSystem.translate("messages.prefix") + " " + getINSTANCE().messageSystem.translate("messages." + path, params)));
    }

    /**
     * Sends a message to the sender with the prefix from the config
     *
     * @param sender  The sender the message should be sent to
     * @param message The message the sender should receive
     */
    public static void sendMessagePrefix(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getINSTANCE().messageSystem.translate("messages.prefix") + " " + ChatColor.translateAlternateColorCodes('&', message)));
    }

    /**
     * A little helper to check if a players main hand is AIR
     *
     * @param player The player that the check should be ran on
     * @return If the main hand is null
     */
    public static boolean isMainHandNull(Player player) {
        if (XMaterial.isNewVersion()) {
            if (player.getEquipment() != null) {
                return player.getEquipment().getItemInMainHand().getType() == Material.AIR;
            }
        } else {
            return player.getItemInHand().getType() == Material.AIR;
        }
        return true;
    }
}
