package com.pablo67340.guishop;

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
import com.pablo67340.guishop.gui.GuiListener;
import com.pablo67340.guishop.economy.DynamicPricingManager;
import com.pablo67340.guishop.economy.EconomyCommands;
import com.pablo67340.guishop.economy.EconomyConfig;
import com.pablo67340.guishop.economy.EconomyManager;
import com.pablo67340.guishop.economy.GUIShopEconomy;
import com.pablo67340.guishop.listenable.editor.ChatInputHandler;
import com.pablo67340.guishop.statistics.GUIShopPlaceholderExpansion;
import com.pablo67340.guishop.statistics.StatisticsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import com.pablo67340.guishop.util.ConfigManager;
import com.pablo67340.guishop.util.LogUtil;
import com.pablo67340.guishop.util.MiscUtils;
import com.pablo67340.guishop.util.RowChart;
import com.pablo67340.guishop.util.SchedulerUtil;
import com.pablo67340.guishop.worth.WorthDisplayManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class GUIShop extends JavaPlugin {

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

    /**
     * A {@link Map} that will store our Creators when the server first starts.
     */
    @Getter
    public static final List<UUID> CREATOR = new ArrayList<>();
    
    /**
     * A {@link Set} that tracks players who have item info debug mode enabled.
     * When enabled, inventory interactions will log PDC/NBT data to console.
     */
    @Getter
    public static final Set<UUID> ITEM_INFO_DEBUG = new HashSet<>();

    public static final RowChart rowChart = new RowChart();

    @Getter
    public CommandManager commandManager;
    
    private GUIShopPlaceholderExpansion placeholderExpansion;

    @Getter
    public ConfigManager configManager;

    @Getter
    public MiscUtils miscUtils;

    @Getter
    @Setter
    public Boolean isReload = false;

    @Getter
    @Setter
    public LogUtil logUtil;

    @Getter
    @Setter
    private WorthDisplayManager worthDisplayManager;
    
    /**
     * The statistics manager for tracking player shop transactions.
     */
    @Getter
    private StatisticsManager statisticsManager;
    
    /**
     * The economy manager for the internal economy system.
     */
    @Getter
    private EconomyManager economyManager;
    
    /**
     * The economy config for the internal economy system.
     */
    @Getter
    private EconomyConfig economyConfig;

    /**
     * The scheduled task for log flushing, used to cancel on disable.
     */
    private final SchedulerUtil.TaskHolder logFlushTask = new SchedulerUtil.TaskHolder();

    @Override
    public void onEnable() {
        INSTANCE = this;
        
        // Initialize Folia/Paper/Spigot scheduler compatibility
        SchedulerUtil.init(this);

        this.configManager = new ConfigManager();
        this.logUtil = new LogUtil();
        this.commandManager = new CommandManager();
        this.miscUtils = new MiscUtils(); // Must be initialized before initConfigs() for dynamic pricing
        this.configManager.initConfigs();
        

        warmup();
        initWriteCache();
        
        // Initialize internal economy (if enabled) before checking for economy plugins
        initInternalEconomy();

        if (!getMiscUtils().setupEconomy()) {
            getLogUtil().log("Vault could not detect an economy plugin!");
            setNoEconomySystem(true);
            return;
        }

        getServer().getPluginManager().registerEvents(PlayerListener.INSTANCE, this);
        getServer().getPluginManager().registerEvents(GuiListener.getInstance(), this);
        
        // Register guishop command with tab completion
        getServer().getPluginCommand("guishop").setExecutor(new GuishopCommand());
        getServer().getPluginCommand("guishop").setTabCompleter(new com.pablo67340.guishop.commands.GuishopTabCompleter());
        
        getServer().getPluginCommand("guishopuser").setExecutor(new UserCommand());
        
        // Register value command with tab completion
        ValueCommand valueCommand = new ValueCommand();
        getServer().getPluginCommand("value").setExecutor(valueCommand);
        getServer().getPluginCommand("value").setTabCompleter(valueCommand);

        // Initialize Worth Display System (requires PacketEvents)
        initWorthDisplay();
        
        // Initialize Statistics System
        initStatistics();
    }

    @Override
    public void onDisable() {
        // Cancel the log flush task
        SchedulerUtil.cancelTask(logFlushTask);

        // Flush any remaining logs to disk
        if (logUtil != null) {
            logUtil.flushLogs();
        }

        // IMPORTANT: Reset static instances FIRST so getInstance() returns null
        // This prevents "connection closed" errors during plugin reload
        WorthDisplayManager.resetInstance();
        StatisticsManager.resetInstance();
        DynamicPricingManager.resetInstance();
        EconomyManager.resetInstance();
        EconomyConfig.resetInstance();
        GuiListener.resetInstance();
        ChatInputHandler.resetInstance();

        // Unregister PlaceholderAPI expansion
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
        
        // Unregister worth display system
        if (worthDisplayManager != null && worthDisplayManager.isRegistered()) {
            worthDisplayManager.unregister();
        }
        
        // Shutdown statistics system
        if (statisticsManager != null) {
            statisticsManager.shutdown();
        }
        
        // Shutdown built-in dynamic pricing system
        if (dynamicPricingManager != null) {
            dynamicPricingManager.shutdown();
        }
        
        // Shutdown internal economy system
        if (economyManager != null) {
            economyManager.shutdown();
        }
    }

    /**
     * Initialize the Worth Display system if ProtocolLib is available.
     */
    private void initWorthDisplay() {
        if (getServer().getPluginManager().getPlugin("packetevents") == null) {
            getLogUtil().log("PacketEvents not found - Worth display feature disabled.");
            getLogUtil().log("Install PacketEvents to show item worth in lore.");
            return;
        }

        try {
            worthDisplayManager = new WorthDisplayManager(this);
            worthDisplayManager.register();
        } catch (Exception e) {
            getLogUtil().log("Failed to initialize Worth Display: " + e.getMessage());
            if (Config.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Initialize the Statistics system and PlaceholderAPI expansion.
     */
    private void initStatistics() {
        try {
            statisticsManager = new StatisticsManager(this);
            statisticsManager.initialize();
            
            // Register PlaceholderAPI expansion if available
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                // Unregister old expansion first (important for PlugMan reloads)
                if (placeholderExpansion != null) {
                    placeholderExpansion.unregister();
                }
                placeholderExpansion = new GUIShopPlaceholderExpansion(this);
                placeholderExpansion.register();
                getLogUtil().log("PlaceholderAPI expansion registered.");
            }
            
            // Load stats for all currently online players (important for PlugMan reloads)
            for (Player player : Bukkit.getOnlinePlayers()) {
                statisticsManager.loadPlayerCache(player);
                statisticsManager.loadPreferencesCache(player.getUniqueId());
            }
            
        } catch (Exception e) {
            getLogUtil().log("Failed to initialize Statistics: " + e.getMessage());
            if (Config.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Initialize the internal economy system if enabled in economy.yml.
     */
    private void initInternalEconomy() {
        try {
            // Load economy config
            economyConfig = new EconomyConfig(this);
            economyConfig.load();
            
            // Check if internal economy is enabled
            if (!economyConfig.isEnabled()) {
                getLogUtil().log("Internal economy is disabled. Using external economy plugin.");
                return;
            }
            
            // Check if Vault is available
            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                getLogUtil().log("Vault not found. Internal economy cannot be registered.");
                return;
            }
            
            // Check if another economy plugin is already registered
            RegisteredServiceProvider<Economy> existingEconomy = getServer().getServicesManager().getRegistration(Economy.class);
            if (existingEconomy != null) {
                String existingPlugin = existingEconomy.getPlugin().getName();
                getLogUtil().log("=========================================");
                getLogUtil().log("NOTICE: Another economy plugin detected!");
                getLogUtil().log("Detected: " + existingPlugin);
                getLogUtil().log("");
                getLogUtil().log("GUIShop's internal economy is currently ENABLED");
                getLogUtil().log("and will override " + existingPlugin + ".");
                getLogUtil().log("");
                getLogUtil().log("To use " + existingPlugin + " instead:");
                getLogUtil().log("  1. Open plugins/GUIShop/economy.yml");
                getLogUtil().log("  2. Set 'enabled: false'");
                getLogUtil().log("  3. Restart the server");
                getLogUtil().log("=========================================");
            }
            
            // Initialize economy manager
            economyManager = new EconomyManager(this);
            if (!economyManager.initialize()) {
                getLogUtil().log("Failed to initialize internal economy database.");
                economyManager = null;
                return;
            }
            
            // Register with Vault
            GUIShopEconomy vaultEconomy = new GUIShopEconomy(this);
            getServer().getServicesManager().register(
                Economy.class, 
                vaultEconomy, 
                this, 
                ServicePriority.High
            );
            
            getLogUtil().log("Internal economy enabled and registered with Vault.");
            getLogUtil().log("Currency: " + economyConfig.getCurrencySymbol() + " (" + economyConfig.getCurrencyName() + ")");
            getLogUtil().log("Starting balance: " + economyConfig.formatBalance(economyConfig.getStartingBalance()));
            
            // Register economy commands dynamically (/bal, /pay, /togglepay)
            // These are only registered when internal economy is enabled
            registerEconomyCommands();
            
            // Load balances for all currently online players (important for PlugMan reloads)
            int loadedCount = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                economyManager.loadPlayerCache(player);
                loadedCount++;
            }
            if (loadedCount > 0) {
                getLogUtil().log("Loaded economy cache for " + loadedCount + " online player(s).");
            }
            
        } catch (Exception e) {
            getLogUtil().log("Failed to initialize internal economy: " + e.getMessage());
            if (Config.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Dynamically registers economy commands (/bal, /pay, /togglepay).
     * These are only registered when internal economy is enabled to avoid
     * conflicting with other economy plugins' commands.
     */
    private void registerEconomyCommands() {
        try {
            EconomyCommands ecoCommands = new EconomyCommands(this);
            
            // Get the command map via reflection
            java.lang.reflect.Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(Bukkit.getServer());
            
            // Register /bal command with aliases
            org.bukkit.command.Command balCommand = new org.bukkit.command.Command("bal", 
                    "Check your balance (internal economy)", 
                    "/bal [player]", 
                    java.util.Arrays.asList("balance", "money")) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return ecoCommands.onCommand(sender, this, label, args);
                }
            };
            balCommand.setPermission("guishop.economy.balance");
            commandMap.register("guishop", balCommand);
            
            // Register /pay command with aliases
            org.bukkit.command.Command payCommand = new org.bukkit.command.Command("pay", 
                    "Send money to another player (internal economy)", 
                    "/pay <player> <amount>", 
                    java.util.Arrays.asList("send")) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return ecoCommands.onCommand(sender, this, label, args);
                }
            };
            payCommand.setPermission("guishop.economy.pay");
            commandMap.register("guishop", payCommand);
            
            // Register /togglepay command with aliases
            org.bukkit.command.Command togglePayCommand = new org.bukkit.command.Command("togglepay", 
                    "Toggle payment notifications on/off", 
                    "/togglepay", 
                    java.util.Arrays.asList("paytoggle")) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    return ecoCommands.onCommand(sender, this, label, args);
                }
            };
            togglePayCommand.setPermission("guishop.economy.pay");
            commandMap.register("guishop", togglePayCommand);
            
            getLogUtil().log("Economy commands registered: /bal, /balance, /money, /pay, /send, /togglepay");
            
        } catch (Exception e) {
            getLogUtil().log("Failed to register economy commands: " + e.getMessage());
            if (Config.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
    
    @Getter
    private com.pablo67340.guishop.economy.DynamicPricingManager dynamicPricingManager;
    
    /**
     * Initialize the built-in dynamic pricing system.
     * This is used when no external DynamicPriceProvider is found.
     */
    public void initBuiltInDynamicPricing() {
        try {
            dynamicPricingManager = new com.pablo67340.guishop.economy.DynamicPricingManager(this);
            if (dynamicPricingManager.initialize()) {
                // Set it as the dynamic pricing provider in MiscUtils
                miscUtils.setDYNAMICPRICING(dynamicPricingManager);
                getLogUtil().log("Built-in dynamic pricing enabled.");
                getLogUtil().log("  Price change per item: " + (dynamicPricingManager.getPriceChangePerTransaction() * 100) + "%");
                getLogUtil().log("  Price bounds: " + (dynamicPricingManager.getMinPriceMultiplier() * 100) + "% - " + (dynamicPricingManager.getMaxPriceMultiplier() * 100) + "%");
            } else {
                getLogUtil().log("Failed to initialize built-in dynamic pricing.");
                dynamicPricingManager = null;
            }
        } catch (Exception e) {
            getLogUtil().log("Error initializing built-in dynamic pricing: " + e.getMessage());
            if (Config.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    public UserCommand getUserCommands() {
        return (UserCommand) getServer().getPluginCommand("guishopuser").getExecutor();
    }

    public void warmup() {
        long startTime = System.currentTimeMillis();
        
        try {
        new Menu().loadItems(true);
        } catch (Exception e) {
            getLogUtil().log("[Critical] Failed to load menu: " + e.getMessage());
            if (Config.isDebugMode()) {
                e.printStackTrace();
            }
        }
        
        // Only process menu items if menu loaded successfully
        if (loadedMenu != null && loadedMenu.getPages() != null) {
            // First, load shops linked from menu items
        for (MenuPage page : loadedMenu.getPages().values()) {
            for (Item item : page.getItems().values()) {
                if (item.getTargetShop() != null) {
                        try {
                    getLogUtil().debugLog("Starting Warmup for Shop: " + item.getTargetShop());
                    new Shop(item.getTargetShop()).loadItems(true);
                        } catch (Exception e) {
                            getLogUtil().log("[Warning] Failed to load shop '" + item.getTargetShop() + "': " + e.getMessage());
            }
        }
                }
            }
        } else {
            getLogUtil().log("[Warning] Menu failed to load - shops linked from menu won't be loaded.");
        }
        
        // Also load ALL shops from shops folder (including hidden ones not linked in menu)
        // This ensures items from all shops are registered in ITEMTABLE for selling/worth
        try {
            Set<String> shopNames = configManager.getShopNames();
            for (String shopName : shopNames) {
                if (!loadedShops.containsKey(shopName)) {
                    try {
                        getLogUtil().debugLog("Loading unlinked shop for worth/sell registration: " + shopName);
                        new Shop(shopName).loadItems(true);
                    } catch (Exception e) {
                        getLogUtil().log("[Warning] Failed to load shop '" + shopName + "': " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to load shops from config: " + e.getMessage());
        }
        
        long estimatedTime = System.currentTimeMillis() - startTime;
        getLogUtil().debugLog("Item warming completed in: " + estimatedTime + "ms");
    }

    public void reload(CommandSender sender, boolean ignoreCreator) {
        this.setIsReload(true);
        boolean hadErrors = false;
        long startTime = System.currentTimeMillis();
        
        getLogUtil().log("Starting hard reload - destroying and recreating all systems...");
        
        // ========== PHASE 1: Close all GUIShop inventories ==========
        try {
            String menuTitle = Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "");
            String shopTitle = Config.getTitlesConfig().getShopTitle().replace("%shopname%", "");
            String qtyTitle = Config.getTitlesConfig().getQtyTitle();
            String sellTitle = Config.getTitlesConfig().getSellTitle();
            String altSellTitle = Config.getAltSellConfig().getTitle();
            String valueTitle = Config.getTitlesConfig().getValueTitle();
            
            Bukkit.getOnlinePlayers().stream().filter(player -> {
                if (player.getOpenInventory() == null) return false;
                String title = player.getOpenInventory().getTitle();
                return title.contains(menuTitle)
                        || title.contains(shopTitle)
                        || title.contains(qtyTitle)
                        || title.contains(sellTitle)
                        || title.contains(altSellTitle)
                        || title.contains(valueTitle);
            }).forEach(Player::closeInventory);
            getLogUtil().debugLog("Closed all GUIShop inventories");
        } catch (Exception e) {
            getLogUtil().debugLog("Error closing inventories during reload: " + e.getMessage());
        }

        // ========== PHASE 2: Shutdown all singletons ==========
        
        // Shutdown worth display system
        try {
            if (worthDisplayManager != null && worthDisplayManager.isRegistered()) {
                worthDisplayManager.unregister();
            }
            WorthDisplayManager.resetInstance();
            worthDisplayManager = null;
            getLogUtil().debugLog("Worth display manager shutdown");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error shutting down worth display: " + e.getMessage());
        }
        
        // Shutdown statistics system
        try {
            if (statisticsManager != null) {
                statisticsManager.shutdown();
            }
            StatisticsManager.resetInstance();
            statisticsManager = null;
            getLogUtil().debugLog("Statistics manager shutdown");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error shutting down statistics: " + e.getMessage());
        }
        
        // Shutdown dynamic pricing system
        try {
            if (dynamicPricingManager != null) {
                dynamicPricingManager.shutdown();
            }
            DynamicPricingManager.resetInstance();
            dynamicPricingManager = null;
            getLogUtil().debugLog("Dynamic pricing manager shutdown");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error shutting down dynamic pricing: " + e.getMessage());
        }
        
        // Shutdown economy manager
        try {
            if (economyManager != null) {
                economyManager.shutdown();
            }
            EconomyManager.resetInstance();
            economyManager = null;
            getLogUtil().debugLog("Economy manager shutdown");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error shutting down economy manager: " + e.getMessage());
        }
        
        // Reset economy config
        try {
            EconomyConfig.resetInstance();
            economyConfig = null;
            getLogUtil().debugLog("Economy config reset");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error resetting economy config: " + e.getMessage());
        }
        
        // Reset GUI listener
        try {
            GuiListener.resetInstance();
            getLogUtil().debugLog("GUI listener reset");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error resetting GUI listener: " + e.getMessage());
        }
        
        // Reset chat input handler
        try {
            ChatInputHandler.resetInstance();
            getLogUtil().debugLog("Chat input handler reset");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Error resetting chat input handler: " + e.getMessage());
        }

        // ========== PHASE 3: Clear all cached data ==========
        ITEMTABLE.clear();
        BUY_COMMANDS.clear();
        SELL_COMMANDS.clear();
        loadedShops.clear();
        loadedMenu = null;
        ITEM_INFO_DEBUG.clear();

        if (!ignoreCreator) {
            CREATOR.clear();
        }
        getLogUtil().debugLog("All caches cleared");

        // ========== PHASE 4: Reload configuration files ==========
        try {
            configManager.reloadConfigs();
            getLogUtil().debugLog("Configs reloaded");
        } catch (Exception e) {
            getLogUtil().log("[Critical] Failed to reload configs: " + e.getMessage());
            hadErrors = true;
        }

        // ========== PHASE 5: Reinitialize all systems ==========
        
        // Reinitialize GUI listener
        try {
            getServer().getPluginManager().registerEvents(GuiListener.getInstance(), this);
            getLogUtil().debugLog("GUI listener reinitialized");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to reinitialize GUI listener: " + e.getMessage());
            hadErrors = true;
        }
        
        // Reinitialize internal economy
        try {
            initInternalEconomy();
            getLogUtil().debugLog("Internal economy reinitialized");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to reinitialize internal economy: " + e.getMessage());
        }

        // Reload all shops and menu items (warmup)
        warmup();

        // ALWAYS re-register commands, even if config loading failed
        try {
            CommandsMode cmdMode = Config.getCommandsMode();
            commandManager.unregisterAll();

            if (cmdMode == CommandsMode.REGISTER) {
                commandManager.registerCommands();
            }

            // Handle command interception
            if (cmdMode == CommandsMode.INTERCEPT) {
                CommandsInterceptor.register();
            } else {
                CommandsInterceptor.unregister();
            }
            getLogUtil().debugLog("Commands reregistered");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to register commands: " + e.getMessage());
            hadErrors = true;
        }

        // Reinitialize worth display system
        try {
            initWorthDisplay();
            getLogUtil().debugLog("Worth display reinitialized");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to reload worth display: " + e.getMessage());
        }
        
        // Reinitialize statistics system
        try {
            initStatistics();
            getLogUtil().debugLog("Statistics system reinitialized");
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to reinitialize statistics: " + e.getMessage());
        }

        // ========== PHASE 6: Report results ==========
        long elapsed = System.currentTimeMillis() - startTime;
        
        if (hadErrors) {
            logUtil.log("GUIShop hard reload completed with errors in " + elapsed + "ms! Check the logs above.");
            if (sender != null) {
                getMiscUtils().sendPrefix(sender, "reload.execute");
                sender.sendMessage(ChatColor.RED + "[GUIShop] Reload completed with errors - check console!");
            }
        } else {
            logUtil.log("GUIShop hard reload completed successfully in " + elapsed + "ms!");
            if (sender != null) {
                getMiscUtils().sendPrefix(sender, "reload.execute");
            }
        }

        this.setIsReload(false);
    }

    public void initWriteCache() {
        // Cancel any existing task from a previous load/reload
        SchedulerUtil.cancelTask(logFlushTask);

        // Schedule periodic log flushing and rotation (every 5 minutes = 6000 ticks)
        SchedulerUtil.runTaskTimerAsync(logFlushTask, () -> {
            if (logUtil != null) {
                // Flush cached logs to disk
                logUtil.flushLogs();
                // Check and rotate oversized log files
                logUtil.checkAndRotateLogs();
            }
        }, 6000, 6000);
    }
}
