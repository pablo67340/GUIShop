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
import com.pablo67340.guishop.definition.MenuPage;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.PlayerListener;
import com.pablo67340.guishop.listenable.Sell;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.listenable.Value;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.RowChart;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class Main extends JavaPlugin implements CommandExecutor {

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
    private MenuItem loadedMenu = null;

    @Getter
    private final Map<String, List<Item>> ITEMTABLE = new HashMap<>();

    @Getter
    private final Map<String, String> cachedHeads = new HashMap<>();

    /**
     * A {@link Map} that will store our {@link Creator}s when the server first
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
    
    public static RowChart rowchart = new RowChart();

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
        ConfigUtil.setDisableBackButton(getMainConfig().getBoolean("disable-back-button"));
        ConfigUtil.setDisableEscapeBack(getMainConfig().getBoolean("disable-escape-back"));
        ConfigUtil.setAlternateSellEnabled(getMainConfig().getBoolean("alternate-sell-enable", false));
        ConfigUtil.setSound(getMainConfig().getString("purchase-sound"));
        ConfigUtil.setSoundEnabled(getMainConfig().getBoolean("enable-sound"));
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
        ConfigUtil.setDynamicPricing(getMainConfig().getBoolean("dynamic-pricing", false));
        ConfigUtil.setDebugMode(getMainConfig().getBoolean("debug-mode"));
        ConfigUtil.setDisabledWorlds(getMainConfig().getStringList("disabled-worlds"));
        ConfigUtil.setSellSkullUUID(getMainConfig().contains("skull-uuid-selling") ? getMainConfig().getBoolean("skull-uuid-selling") : true);
        if (ConfigUtil.isDynamicPricing() && !setupDynamicPricing()) {
            getLogger().log(Level.INFO, "Could not find a DynamicPriceProvider! Disabling dynamic pricing...");
            ConfigUtil.setDynamicPricing(false);
        }
        
        ConfigUtil.setAltSellIncreaseTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-increase-title", "&aIncrease quantity by {amount}")));
        
        ConfigUtil.setAltSellDecreaseTitle(ChatColor.translateAlternateColorCodes('&',
                getMainConfig().getString("alt-sell-decrease-title", "&aDecrease quantity by {amount}")));

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
            debugLog("Error Main config: " + e.getMessage());
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
            debugLog("Error loading Shop Config: " + e.getMessage());
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
            debugLog("Error loading Menu config: " + e.getMessage());
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
            debugLog("Error loading Cache config: " + e.getMessage());
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
        new Thread(() -> {
            handleConfig();
        }).start();
        new Thread(() -> {
            handleCacheConfig();
        }).start();
        new Thread(() -> {
            handleDictionary();
        }).start();
        new Thread(() -> {
            handleMenuConfig();
        }).start();
        new Thread(() -> {
            handleShopsConfig();
        }).start();
    }

    /**
     * Copy a file from source to destination.
     *
     * @param source the source
     * @param destination the destination
     * @param name the name of the file
     */
    public void copy(String name, InputStream source, String destination) {
        log("Extracting: " + name + " -> " + "/plugins/GUIShop/Dictionary/" + name);

        try {
            Files.copy(source, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            debugLog("Error extracting Dictionary files: " + ex.getMessage());
        }
    }

    public void warmup() {
        long startTime = System.currentTimeMillis();
        new Menu().loadItems(true);
        for (MenuPage page : loadedMenu.getPages().values()) {
            for (Item item : page.getItems().values()) {
                if (item.getTargetShop() != null) {
                    Main.debugLog("Starting Warmup for Shop: " + item.getTargetShop());
                    new Shop(item.getTargetShop()).loadItems(true);
                }
            }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        getLogger().log(Level.INFO, "Item warming completed in: {0}ms", estimatedTime);
    }

    public void reload(Player player, Boolean ignoreCreator) {
        Main.debugLog("GUIShop Reloaded");

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

    public void loadCache() {
        log("Loading Cache...");
        if (getCacheConfig().contains("player-heads")) {
            ConfigurationSection config = getCacheConfig().getConfigurationSection("player-heads");
            for (Entry<String, Object> entry : config.getValues(false).entrySet()) {
                cachedHeads.put(entry.getKey(), (String) entry.getValue());
            }
        }
        log("Cache loaded successfully!");
    }

    public void reloadShopConfig() {
        try {
            shopConfig.load(shopf);
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
        str = str.replace("{CURRENCY_SYMBOL}", ConfigUtil.getCurrency());
        str = str.replace("{CURRENCY_SUFFIX}", ConfigUtil.getCurrencySuffix());

        return str;
    }

    public static void log(String input) {
        Main.getINSTANCE().getLogger().log(Level.INFO, ": {0}", input);
    }

    public static void debugLog(String input) {
        if (ConfigUtil.isDebugMode()) {
            Main.getINSTANCE().getLogger().log(Level.INFO, "[DEBUG]: {0}", input);
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (cmd.getName().equalsIgnoreCase("value")) {
                if (player.hasPermission("guishop.value")) {
                    if (player.getItemInHand() != null) {
                        ItemStack target = player.getItemInHand();
                        String targetMaterial = target.getType().toString();
                        Value value = new Value(player, targetMaterial);
                        value.loadItems();
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou must be holding an item."));
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getNoPermission()));
                }
                return true;
            }
        } else {
            sender.sendMessage("You must run this command in-game.");
            return false;
        }
        return false;
    }
    
    

}
