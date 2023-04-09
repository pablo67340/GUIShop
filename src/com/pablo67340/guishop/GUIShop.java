package com.pablo67340.guishop;

import com.cryptomorin.xseries.XMaterial;
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
import com.pablo67340.guishop.util.ConfigManager;
import com.pablo67340.guishop.util.RowChart;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import net.milkbowl.vault.permission.Permission;

public final class GUIShop extends JavaPlugin {

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

    private BuyCommand buyCommand = null;

    private SellCommand sellCommand = null;

    @Getter
    public CommandManager commandManager;
    
    @Getter
    public ConfigManager configManager;

    @Getter
    @Setter
    public Boolean isReload = false;

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.configManager = new ConfigManager();
        new Thread(configManager::initConfigs).start();

        

        if (!setupEconomy()) {
            getLogger().log(Level.WARNING, "Vault could not detect an economy plugin!");
            setNoEconomySystem(true);
            return;
        }

        getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
        getServer().getPluginCommand("guishop").setExecutor(new GuishopCommand());
        getServer().getPluginCommand("guishopuser").setExecutor(new UserCommand());

        this.commandManager = new CommandManager();
        

        System.out.println("XSeries: " + XMaterial.getVersion());
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
        Bukkit.getOnlinePlayers().stream().filter(player -> player.getOpenInventory() != null
                && (player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getMenuTitle().replace("%page-number%", ""))
                || player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getShopTitle().replace("%shopname%", ""))
                || player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getQtyTitle())
                || player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getSellTitle())
                || player.getOpenInventory().getTitle().contains(Config.getAltSellConfig().getTitle())
                || player.getOpenInventory().getTitle().contains(Config.getTitlesConfig().getValueTitle()))
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

        configManager.reloadConfigs();

        // If the CommandsMode is REGISTER, register/re-register the commands
        // Otherwise, unregister the commands
        CommandsMode cmdMode = Config.getCommandsMode();

        commandManager.unregisterAll();

        if (cmdMode == CommandsMode.REGISTER) {
            commandManager.registerCommands();
        }

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

        string = string.replace("%currency_symbol%", getConfigManager().getMessageSystem().translate("messages.currency-prefix"));
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
     * @param path The path to the message
     * @param params Optional, the placeholder replacements
     */
    public static void sendPrefix(CommandSender sender, String path, Object... params) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getINSTANCE().messageSystem.translate("messages.prefix") + " " + getINSTANCE().messageSystem.translate("messages." + path, params)));
    }

    /**
     * Sends a message to the sender with the prefix from the config
     *
     * @param sender The sender the message should be sent to
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
        if (XMaterial.supports(0)) {
            if (player.getEquipment() != null) {
                return player.getEquipment().getItemInMainHand().getType() == Material.AIR;
            }
        } else {
            return player.getItemInHand().getType() == Material.AIR;
        }
        return true;
    }
}
