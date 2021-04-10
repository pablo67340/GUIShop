package com.pablo67340.guishop;

import com.cryptomorin.xseries.XMaterial;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.commands.BuyCommand;
import com.pablo67340.guishop.commands.CommandsInterceptor;
import com.pablo67340.guishop.commands.GuishopCommand;
import com.pablo67340.guishop.commands.GuishopUserCommand;
import com.pablo67340.guishop.commands.SellCommand;
import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.CommandsMode;
import com.pablo67340.guishop.definition.MenuItem;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.MatLib;
import java.net.URL;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

public final class Main extends JavaPlugin {

    /**
     * The overridden config file objects.
     */
    @Getter
    private File configf, specialf, menuf, cachef;

    @Getter
    File dictionaryf;

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
    public Map<String, Object> loadedShops = new HashMap<>();

    @Getter
    @Setter
    MenuItem loadedMenu = null;

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
    private final Map<UUID, Shop> openShopInstances = new HashMap<>();

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
            getLogger().log(Level.INFO, "Vault could not detect an economy plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
        getServer().getPluginCommand("guishop").setExecutor(new GuishopCommand());
        getServer().getPluginCommand("guishopuser").setExecutor(new GuishopUserCommand());
        loadDefaults();
        if (ConfigUtil.isDynamicPricing() && !setupDynamicPricing()) {
            getLogger().log(Level.INFO, "Could not find a DynamicPriceProvider! Disabling dynamic pricing...");
            ConfigUtil.setDynamicPricing(false);
        }

        switch (ConfigUtil.getCommandsMode()) {
            case INTERCEPT:
                CommandsInterceptor.register();
                break;
            case REGISTER:
                registerCommands(false);
                break;
            default:
                break;
        }
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
        getLogger().log(Level.INFO, "Registering/unregistering commands {0} and {1}", new Object[]{StringUtils.join(Main.BUY_COMMANDS, "|"), StringUtils.join(Main.SELL_COMMANDS, "|")});

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
            buyCommand = new BuyCommand(new ArrayList<>(Main.BUY_COMMANDS));
            commandMap.register(buyCommand.getName(), buyCommand);

            sellCommand = new SellCommand(new ArrayList<>(Main.SELL_COMMANDS));
            commandMap.register(sellCommand.getName(), sellCommand);

        } catch (IllegalAccessException | NoSuchFieldException e) {
            debugLog("Error registering commands: " + e.getMessage());
        }
    }

    public GuishopUserCommand getUserCommands() {
        return (GuishopUserCommand) getServer().getPluginCommand("guishopuser").getExecutor();
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
        ConfigUtil.setCommandsMode(CommandsMode.parseFromConfig(getMainConfig().getString("commands-mode")));
        ConfigUtil.setPrefix(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("prefix"))));
        ConfigUtil.setSignsOnly(getMainConfig().getBoolean("signs-only"));
        ConfigUtil.setSignTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("sign-title"))));
        ConfigUtil.setNotEnoughPre(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("not-enough-pre"))));
        ConfigUtil.setNotEnoughPost(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("not-enough-post"))));
        ConfigUtil.setPurchased(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("purchased"))));
        ConfigUtil.setTaken(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("taken"))));
        ConfigUtil.setSold(
                ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("sold"))));
        ConfigUtil.setAdded(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("added"))));
        ConfigUtil.setCantSell(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cant-sell"))));
        ConfigUtil.setEscapeOnly(getMainConfig().getBoolean("escape-only"));
        ConfigUtil.setAlternateSellEnabled(getMainConfig().getBoolean("alternate-sell-enable", false));
        ConfigUtil.setSound(getMainConfig().getString("purchase-sound"));
        ConfigUtil.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));
        ConfigUtil.setEnableCreator(getMainConfig().getBoolean("ingame-config"));
        ConfigUtil.setCantBuy(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cant-buy"))));
        ConfigUtil.setFull(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("full-inventory"))));
        ConfigUtil.setNoPermission(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("no-permission"))));
        ConfigUtil.setCurrency(getMainConfig().getString("currency"));
        ConfigUtil.setCurrencySuffix(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("currency-suffix"))));
        ConfigUtil.setMenuTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("menu-title", "Menu")));
        ConfigUtil.setMenuShopPageNumber(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("menu-shop-pagenumber", "&f> Page: &e{number}")));
        ConfigUtil.setShopTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("shop-title", "Menu &f> &r{shopname}")));
        ConfigUtil.setSellTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("sell-title", "Menu &f> &rSell")));
        ConfigUtil.setAltSellTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-title", "Menu &f> &rSell")));
        ConfigUtil.setQtyTitle(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("qty-title"))));
        ConfigUtil.setBackButtonItem(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("back-button-item"))));
        ConfigUtil.setBackButtonText(
                ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(getMainConfig().getString("back"))));
        ConfigUtil.setBuyLore(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("buy-lore"))));
        ConfigUtil.setSellLore(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("sell-lore"))));
        ConfigUtil.setFreeLore(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("free-lore"))));

        ConfigUtil.setCannotBuy(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cannot-buy"))));
        ConfigUtil.setCannotSell(ChatColor.translateAlternateColorCodes('&',
                Objects.requireNonNull(getMainConfig().getString("cannot-sell"))));
        ConfigUtil.setForwardPageButtonName(
                ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("forward-page-button-name", "")));
        ConfigUtil.setBackwardPageButtonName(
                ChatColor.translateAlternateColorCodes('&', getMainConfig().getString("backward-page-button-name", "")));
        ConfigUtil.setAltSellIndicatorMaterial(getMainConfig().getString("alt-sell-indicator-material", "EMERALD"));
        ConfigUtil.setAltSellAddMaterial(getMainConfig().getString("alt-sell-add-material", "GREEN_STAINED_GLASS_PANE"));
        ConfigUtil.setAltSellRemoveMaterial(getMainConfig().getString("alt-sell-remove-material", "RED_STAINED_GLASS_PANE"));
        ConfigUtil.setAltSellQuantity1(getMainConfig().getInt("alt-sell-quantity-1", 1));
        ConfigUtil.setAltSellQuantity2(getMainConfig().getInt("alt-sell-quantity-2", 10));
        ConfigUtil.setAltSellQuantity3(getMainConfig().getInt("alt-sell-quantity-3", 64));
        ConfigUtil.setAltSellConfirmMaterial(getMainConfig().getString("alt-sell-confirm-material", "EMERALD_BLOCK"));
        ConfigUtil.setAltSellCancelMaterial(getMainConfig().getString("alt-sell-cancel-material", "REDSTONE_BLOCK"));
        ConfigUtil.setAltSellConfirmName(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-confirm-name", "&a&lConfirm")));
        ConfigUtil.setAltSellCancelName(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-cancel-name", "&c&lCancel")));
        ConfigUtil.setAltSellNotEnough(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-not-enough", "&cYou do not have enough items to sell.")));
        ConfigUtil.getDisabledQty().addAll(getMainConfig().getStringList("disabled-qty-items"));
        ConfigUtil.setDynamicPricing(getMainConfig().getBoolean("dynamic-pricing", false));
        ConfigUtil.setDebugMode(getMainConfig().getBoolean("debug-mode"));
    }

    /**
     * Force create all YML files.
     */
    public void createFiles() {

        configf = new File(getDataFolder(), "config.yml");
        specialf = new File(getDataFolder(), "shops.yml");
        menuf = new File(getDataFolder(), "menu.yml");
        cachef = new File(getDataFolder(), "cache.yml");
        dictionaryf = new File(getDataFolder().getPath() + "/Dictionary");

        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        if (!specialf.exists()) {
            specialf.getParentFile().mkdirs();
            saveResource("shops.yml", false);
        }

        if (!menuf.exists()) {
            menuf.getParentFile().mkdirs();
            saveResource("menu.yml", false);
        }

        if (!cachef.exists()) {
            cachef.getParentFile().mkdirs();
            saveResource("cache.yml", false);
        }

        if (!dictionaryf.exists()) {
            dictionaryf.mkdirs();
            URL potionInput = getClass().getClassLoader().getResource("potion-names.txt");
            File potionDest = new File(dictionaryf.getPath() + "/potion-names.txt");

            URL spawnerInput = getClass().getClassLoader().getResource("spawner-names.txt");
            File spawnerDest = new File(dictionaryf.getPath() + "/spawner-names.txt");

            URL materialsInput = getClass().getClassLoader().getResource("material-names.txt");
            File materialsDest = new File(dictionaryf.getPath() + "/material-names.txt");

            URL enchantmentInput = getClass().getClassLoader().getResource("enchantment-names.txt");
            File enchantmentDest = new File(dictionaryf.getPath() + "/enchantment-names.txt");

            try {
                FileUtils.copyURLToFile(potionInput, potionDest);
                FileUtils.copyURLToFile(spawnerInput, spawnerDest);
                FileUtils.copyURLToFile(materialsInput, materialsDest);
                FileUtils.copyURLToFile(enchantmentInput, enchantmentDest);
            } catch (IOException ex) {
                Main.debugLog("Error copying Dictionary files: " + ex.getMessage());
            }
        }

        mainConfig = new YamlConfiguration();
        shopConfig = new YamlConfiguration();
        menuConfig = new YamlConfiguration();
        cacheConfig = new YamlConfiguration();

        try {
            mainConfig.load(configf);
            shopConfig.load(specialf);
            menuConfig.load(menuf);
            cacheConfig.load(cachef);
        } catch (InvalidConfigurationException | IOException e) {
            debugLog("Error creating configs: " + e.getMessage());
        }

    }

    public void reload(Player player, Boolean ignoreCreator) {
        Main.debugLog("GUIShop Reloaded");
        createFiles();
        ITEMTABLE.clear();
        BUY_COMMANDS.clear();
        SELL_COMMANDS.clear();
        loadedShops.clear();
        if (!ignoreCreator) {
            CREATOR.clear();
        }
        reloadConfig();
        reloadShopConfig();
        reloadMenuConfig();
        reloadCacheConfig();
        loadDefaults();

        // If the CommandsMode is REGISTER, register/re-register the commands
        // Otherwise, unregister the commands
        CommandsMode cmdsMode = ConfigUtil.getCommandsMode();
        registerCommands(cmdsMode != CommandsMode.REGISTER);

        // Intercept commands using the listener, if configured
        if (cmdsMode == CommandsMode.INTERCEPT) {
            CommandsInterceptor.register();

        } else {
            // Unregisters previous command listener
            CommandsInterceptor.unregister();
        }
        sendMessage(player, "&aGUIShop Reloaded");

    }

    public void reloadShopConfig() {
        try {
            shopConfig.load(specialf);
        } catch (IOException | InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            debugLog("Error loading custom config: " + e.getMessage());
        }
    }

    public void reloadMenuConfig() {
        try {
            menuConfig.load(menuf);
        } catch (IOException | InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            debugLog("Error loading custom config: " + e.getMessage());
        }
    }

    public void reloadCacheConfig() {
        try {
            cacheConfig.load(cachef);
        } catch (IOException | InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            debugLog("Error loading custom config: " + e.getMessage());
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
        str = str.replace("{CURRENCY_SYMBOL}", ConfigUtil.getCurrency());
        str = str.replace("{CURRENCY_SUFFIX}", ConfigUtil.getCurrencySuffix());

        return str;
    }

    public static void log(String input) {
        Main.getINSTANCE().getLogger().log(Level.WARNING, ": {0}", input);
    }

    public static void debugLog(String input) {
        if (ConfigUtil.isDebugMode()) {
            Main.getINSTANCE().getLogger().log(Level.WARNING, "[DEBUG]: {0}", input);
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
