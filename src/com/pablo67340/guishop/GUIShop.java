package com.pablo67340.guishop;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.commands.*;
import com.pablo67340.guishop.commands.CommandsInterceptor;
import com.pablo67340.guishop.definition.CommandsMode;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.MenuItem;
import com.pablo67340.guishop.definition.MenuPage;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.Config;
import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

public final class GUIShop extends JavaPlugin {

    /**
     * The overridden config file objects.
     */
    @Getter
    private File configf, shopf, menuf, cachef, dictionaryf;

    /**
     * The configs FileConfiguration object.
     */
    @Getter
    private FileConfiguration mainConfig, shopConfig, menuConfig, cacheConfig;

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
     * A {@link Map} that will store our Creators when the server first
     * starts.
     */
    @Getter
    public static final List<String> CREATOR = new ArrayList<>();

    /**
     * The current buy command
     */
    private BuyCommand buyCommand = null;

    /**
     * The current sell command
     */
    private SellCommand sellCommand = null;

    @Override
    public void onEnable() {
        INSTANCE = this;
        createFiles();

        if (!setupEconomy()) {
            getLogger().log(Level.WARNING, "Vault could not detect an economy plugin!");
            setNoEconomySystem(true);
//          getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
        getServer().getPluginCommand("guishop").setExecutor(new GUIShopCommand());
        getServer().getPluginCommand("guishopuser").setExecutor(new UserCommand());
        getServer().getPluginCommand("value").setExecutor(new ValueCommand());
    }

    /**
     * Register/unregister the GUIShop commands with the bukkit server. <br>
     * Accesses the command map via reflection
     *
     * @param onlyUnregister if true, new commands will not be registered, only,
     * existing ones will be unregistered
     */
    private void registerCommands(boolean onlyUnregister) {
        if (onlyUnregister && buyCommand == null && sellCommand == null) {
            // Nothing is registered, no need to do anything
            return;
        }
        getLogger().log(Level.INFO, "Registering/unregistering commands {0} and {1}", new Object[]{StringUtils.join(GUIShop.BUY_COMMANDS, "|"), StringUtils.join(GUIShop.SELL_COMMANDS, "|")});

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
            // Register new commands
            buyCommand = new BuyCommand(new ArrayList<>(GUIShop.BUY_COMMANDS));
            commandMap.register(buyCommand.getName(), buyCommand);

            sellCommand = new SellCommand(new ArrayList<>(GUIShop.SELL_COMMANDS));
            commandMap.register(sellCommand.getName(), sellCommand);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            log("Error registering commands: " + e.getMessage());
        }
    }

    public UserCommand getUserCommands() {
        return (UserCommand) getServer().getPluginCommand("guishopuser").getExecutor();
    }

    /**
     * Check if Vault is present, Check if an Economy plugin is present, if so
     * Hook.
     */
    private Boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        } else {
            rsp.getProvider();
        }

        ECONOMY = rsp.getProvider();

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
     * Load all deault config values, translate colors, store.
     */
    public void loadDefaults() {
        if (getMainConfig() == null) {
            getLogger().log(Level.WARNING, "An error occurred while loading the config.yml! Please correct the errors and try again.");
            return;
        }

        // All buy commands
        BUY_COMMANDS.addAll(getMainConfig().getStringList("buy-commands"));

        // All sell commands
        SELL_COMMANDS.addAll(getMainConfig().getStringList("sell-commands"));

        // The command mode GUIShop should use
        Config.setCommandsMode(CommandsMode.parseFromConfig(getMainConfig().getString("commands-mode")));
        Config.setPrefix(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("prefix"))));

        // Signs only?
        Config.setSignsOnly(getMainConfig().getBoolean("signs-only"));

        // The title for signs
        Config.setSignTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("sign-title"))));

        // Message for not enough money - pre
        Config.setNotEnoughPre(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("not-enough-pre"))));

        // Message for not enough money - post
        Config.setNotEnoughPost(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("not-enough-post"))));

        // Message for buying - pre
        Config.setPurchased(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("purchased"))));

        // Message for buying - post
        Config.setTaken(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("taken"))));

        // Message for selling - pre
        Config.setSold(
                ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("sold"))));

        // Message for selling - post
        Config.setAdded(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("added"))));

        // Message if something cant sell
        Config.setCantSell(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cant-sell-message", "&f({count}) item(s) failed to sell and have been returned to your inventory."))));

        // Message if something cant buy
        Config.setCantBuy(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cant-buy-message", "&fSorry, you are not able to buy that item."))));

        // Disable the back button
        Config.setDisableBackButton(getMainConfig().getBoolean("disable-back-button"));

        // Disable the feature to escape back
        Config.setDisableEscapeBack(getMainConfig().getBoolean("disable-escape-back"));

        // Disable the feature to escape back from quantity (If the upper one is disabled, this is enabled)
        Config.setDisableEscapeBackQuantity(getMainConfig().getBoolean("disable-escape-back-quantity", true));

        // Enable alternate sell
        Config.setAlternateSellEnabled(getMainConfig().getBoolean("alternate-sell-enable", false));

        // The sound when buying something
        Config.setSound(getMainConfig().getString("purchase-sound"));

        // If the sound should be enabled
        Config.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));

        // The message sent when your inventory is too full
        Config.setFull(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("full-inventory"))));

        // The message sent when the user doesn't have the correct permission
        Config.setNoPermission(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("no-permission"))));

        // Currency prefix
        Config.setCurrency(Objects.requireNonNull(getMainConfig().getString("currency")));

        // Currency suffix
        Config.setCurrencySuffix(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("currency-suffix"))));

        // Menu title
        Config.setMenuTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("menu-title", "Menu")));

        // Menu title when there are multiple pages
        Config.setMenuShopPageNumber(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("menu-shop-pagenumber", "&f> Page: &e{number}")));

        // Shop title
        Config.setShopTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("shop-title", "Menu &f> &r{shopname}")));

        // Sell title
        Config.setSellTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("sell-title", "Menu &f> &rSell")));

        // Alternate sell title
        Config.setAltSellTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-title", "Menu &f> &rSell")));

        // Quantity title
        Config.setQtyTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("qty-title"))));

        // Back button item
        Config.setBackButtonItem(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("back-button-item"))));

        // Back button display name
        Config.setBackButtonText(
                ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("back"))));

        // Lore when you can buy the item
        Config.setBuyLore(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("buy-lore"))));

        // Lore when you can sell the item
        Config.setSellLore(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("sell-lore"))));

        // Lore when you can buy the item for free
        Config.setFreeLore(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("free-lore"))));

        // Lore when you can't buy the item
        Config.setCannotBuy(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cannot-buy"))));

        // Lore when you can't sell the item
        Config.setCannotSell(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cannot-sell"))));

        // Forward button display name
        Config.setForwardPageButtonName(
                ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("forward-page-button-name", "")));

        // Backward button display name
        Config.setBackwardPageButtonName(
                ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("backward-page-button-name", "")));
        Config.setAltSellIndicatorMaterial(getMainConfig().getString("alt-sell-indicator-material", "EMERALD"));

        // Alternate sell add item material
        Config.setAltSellAddMaterial(getMainConfig().getString("alt-sell-add-material", "GREEN_STAINED_GLASS_PANE"));

        // Alternate sell remove item material
        Config.setAltSellRemoveMaterial(getMainConfig().getString("alt-sell-remove-material", "RED_STAINED_GLASS_PANE"));

        // Alternate sell quantities
        Config.setAltSellQuantity1(getMainConfig().getInt("alt-sell-quantity-1", 1));
        Config.setAltSellQuantity2(getMainConfig().getInt("alt-sell-quantity-2", 10));
        Config.setAltSellQuantity3(getMainConfig().getInt("alt-sell-quantity-3", 64));

        // Alternate sell confirm item material
        Config.setAltSellConfirmMaterial(getMainConfig().getString("alt-sell-confirm-material", "EMERALD_BLOCK"));

        // Alternate sell cancel item material
        Config.setAltSellCancelMaterial(getMainConfig().getString("alt-sell-cancel-material", "REDSTONE_BLOCK"));

        // Alternate sell confirm item display name
        Config.setAltSellConfirmName(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-confirm-name", "&a&lConfirm")));

        // Alternate sell cancel item display name
        Config.setAltSellCancelName(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-cancel-name", "&c&lCancel")));

        // Alternate sell not enough items message
        Config.setAltSellNotEnough(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-not-enough", "&cYou do not have enough items to sell.")));

        // If dynamic pricing should be enabled
        Config.setDynamicPricing(getMainConfig().getBoolean("dynamic-pricing", false));

        // If debug should be enabled
        Config.setDebugMode(getMainConfig().getBoolean("debug-mode"));

        // The disabled worlds for GUIShop
        Config.setDisabledWorlds(getMainConfig().getStringList("disabled-worlds"));

        // Separate uuid of heads when selling
        Config.setSellSkullUUID(!getMainConfig().contains("skull-uuid-selling") || getMainConfig().getBoolean("skull-uuid-selling"));

        // Disables dynamic pricing if no provider was found
        if (Config.isDynamicPricing() && !setupDynamicPricing()) {
            getLogger().log(Level.INFO, "Could not find a DynamicPriceProvider! Disabling dynamic pricing...");
            Config.setDynamicPricing(false);
        }

        // Increase item display name
        Config.setAltSellIncreaseTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-increase-title", "&aIncrease quantity by {amount}")));

        // Decrease item display name
        Config.setAltSellDecreaseTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-decrease-title", "&aDecrease quantity by {amount}")));

        // Register commands
        if (Config.getCommandsMode() == CommandsMode.INTERCEPT) {
            CommandsInterceptor.register();
        } else {
            registerCommands(false);
        }
    }

    /**
     * Force create all YML files.
     */
    public void handleConfig() {
        configf = new File(getDataFolder(), "config.yml");
        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        mainConfig = new YamlConfiguration();
        try {
            mainConfig.load(configf);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error Main config: " + e.getMessage());
        }
        loadDefaults();
    }

    public void handleShopsConfig() {
        shopf = new File(getDataFolder(), "shops.yml");
        if (!shopf.exists()) {
            shopf.getParentFile().mkdirs();
            saveResource("shops.yml", false);
        }
        shopConfig = new YamlConfiguration();
        try {
            shopConfig.load(shopf);
            warmup();
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Shop Config: " + e.getMessage());
        }
    }

    public void handleMenuConfig() {
        menuf = new File(getDataFolder(), "menu.yml");
        if (!menuf.exists()) {
            menuf.getParentFile().mkdirs();
            saveResource("menu.yml", false);
        }
        menuConfig = new YamlConfiguration();
        try {
            menuConfig.load(menuf);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Menu config: " + e.getMessage());
        }
    }

    public void handleCacheConfig() {
        cachef = new File(getDataFolder(), "cache.yml");
        if (!cachef.exists()) {
            cachef.getParentFile().mkdirs();
            saveResource("cache.yml", false);
        }
        cacheConfig = new YamlConfiguration();

        try {
            cacheConfig.load(cachef);
            loadCache();
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading Cache config: " + e.getMessage());
        }
    }

    public void handleDictionary() {
        dictionaryf = new File(getDataFolder().getPath() + "/Dictionary");
        if (!dictionaryf.exists()) {
            dictionaryf.mkdirs();

            File potionDest = new File(dictionaryf.getPath() + "/potion-names.txt");
            File spawnerDest = new File(dictionaryf.getPath() + "/spawner-names.txt");
            File materialsDest = new File(dictionaryf.getPath() + "/material-names.txt");
            File enchantmentDest = new File(dictionaryf.getPath() + "/enchantment-names.txt");
            File flagDest = new File(dictionaryf.getPath() + "/item-flags.txt");

            copy("potion-names.txt", getClass().getClassLoader().getResourceAsStream("potion-names.txt"), potionDest.getPath());
            copy("spawner-names.txt", getClass().getClassLoader().getResourceAsStream("spawner-names.txt"), spawnerDest.getPath());
            copy("material-names.txt", getClass().getClassLoader().getResourceAsStream("material-names.txt"), materialsDest.getPath());
            copy("enchantment-names.txt", getClass().getClassLoader().getResourceAsStream("enchantment-names.txt"), enchantmentDest.getPath());
            copy("item-flags.txt", getClass().getClassLoader().getResourceAsStream("item-flags.txt"), flagDest.getPath());
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
        new Thread(this::handleMenuConfig).start();
        new Thread(this::handleShopsConfig).start();
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
        debugLog("Item warming completed in: {0}ms" + estimatedTime);
    }

    public void reload(CommandSender sender, Boolean ignoreCreator) {
        log("GUIShop Reloaded");

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
        sendMessage(sender, Config.getPrefix() + " " + "&aGUIShop Reloaded");

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
            shopConfig.load(shopf);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
        }
    }

    public void reloadMenuConfig() {
        try {
            menuConfig.load(menuf);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
        }
    }

    public void reloadCacheConfig() {
        try {
            cacheConfig.load(cachef);
        } catch (IOException | InvalidConfigurationException e) {
            log("Error loading custom config: " + e.getMessage());
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
        String str = input;

        if (player != null) {
            str = str.replace("{PLAYER_NAME}", player.getName());
            str = str.replace("{PLAYER_UUID}", player.getUniqueId().toString());
        }
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
            str = str.replace("{BUY_PRICE}", item.calculateBuyPrice(1).toPlainString());
        }
        if (item.hasSellPrice()) {
            str = str.replace("{SELL_PRICE}", item.calculateSellPrice(1).toPlainString());
        }
        str = str.replace("{CURRENCY_SYMBOL}", Config.getCurrency());
        str = str.replace("{CURRENCY_SUFFIX}", Config.getCurrencySuffix());

        return str;
    }

    public static void log(String input) {
        GUIShop.getINSTANCE().getLogger().log(Level.INFO, ": {0}", input);
    }

    public static void debugLog(String input) {
        if (Config.isDebugMode()) {
            GUIShop.getINSTANCE().getLogger().log(Level.INFO, "[DEBUG]: {0}", input);
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
