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
import com.pablo67340.guishop.economy.EconomyCommands;
import com.pablo67340.guishop.economy.EconomyConfig;
import com.pablo67340.guishop.economy.EconomyManager;
import com.pablo67340.guishop.economy.GUIShopEconomy;
import com.pablo67340.guishop.statistics.GUIShopPlaceholderExpansion;
import com.pablo67340.guishop.statistics.StatisticsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;
import com.pablo67340.guishop.util.ConfigManager;
import com.pablo67340.guishop.util.LogUtil;
import com.pablo67340.guishop.util.MiscUtils;
import com.pablo67340.guishop.util.RowChart;
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

    public static final RowChart rowChart = new RowChart();

    @Getter
    public CommandManager commandManager;

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
     * The scheduled task ID for log flushing, used to cancel on disable.
     */
    private int logFlushTaskId = -1;

    @Override
    public void onEnable() {
        INSTANCE = this;

        this.configManager = new ConfigManager();
        this.logUtil = new LogUtil();
        this.commandManager = new CommandManager();
        this.configManager.initConfigs();
        
        this.miscUtils = new MiscUtils();
        

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
        getServer().getPluginCommand("guishop").setExecutor(new GuishopCommand());
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
        if (logFlushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(logFlushTaskId);
            logFlushTaskId = -1;
        }

        // Flush any remaining logs to disk
        if (logUtil != null) {
            logUtil.flushLogs();
        }

        // Unregister worth display system
        if (worthDisplayManager != null && worthDisplayManager.isRegistered()) {
            worthDisplayManager.unregister();
        }
        
        // Shutdown statistics system
        if (statisticsManager != null) {
            statisticsManager.shutdown();
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
                new GUIShopPlaceholderExpansion(this).register();
                getLogUtil().log("PlaceholderAPI expansion registered.");
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
            
            // Register economy commands (/bal, /pay)
            EconomyCommands ecoCommands = new EconomyCommands(this);
            if (getCommand("bal") != null) {
                getCommand("bal").setExecutor(ecoCommands);
            }
            if (getCommand("pay") != null) {
                getCommand("pay").setExecutor(ecoCommands);
            }
            getLogUtil().log("Economy commands registered: /bal, /balance, /pay");
            
        } catch (Exception e) {
            getLogUtil().log("Failed to initialize internal economy: " + e.getMessage());
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
        
        // Also load ALL shops from shops.yml (including hidden ones not linked in menu)
        // This ensures items from all shops are registered in ITEMTABLE for selling/worth
        try {
            Set<String> shopKeys = configManager.getShopConfig().getKeys(false);
            for (String shopName : shopKeys) {
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
        
        // Close all GUIShop inventories for online players
        // Must do this BEFORE clearing data to avoid NPEs
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
        } catch (Exception e) {
            getLogUtil().debugLog("Error closing inventories during reload: " + e.getMessage());
        }

        // Clear all cached data
        ITEMTABLE.clear();
        BUY_COMMANDS.clear();
        SELL_COMMANDS.clear();
        loadedShops.clear();
        loadedMenu = null;

        if (!ignoreCreator) {
            CREATOR.clear();
        }

        // Reload all configuration files and defaults
        try {
        configManager.reloadConfigs();
        } catch (Exception e) {
            getLogUtil().log("[Critical] Failed to reload configs: " + e.getMessage());
            hadErrors = true;
        }

        // Reload all shops and menu items (warmup)
        // This is wrapped in try-catch inside warmup() itself
        warmup();

        // ALWAYS re-register commands, even if config loading failed
        // This ensures /gs reload is still available to fix config issues
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
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to register commands: " + e.getMessage());
            hadErrors = true;
        }

        // Reload worth display system
        try {
        if (worthDisplayManager != null && worthDisplayManager.isRegistered()) {
            worthDisplayManager.unregister();
        }
        initWorthDisplay();
        } catch (Exception e) {
            getLogUtil().log("[Warning] Failed to reload worth display: " + e.getMessage());
        }

        if (hadErrors) {
            logUtil.log("GUIShop reloaded with errors! Check the logs above.");
            if (sender != null) {
                getMiscUtils().sendPrefix(sender, "reload.execute");
                sender.sendMessage(ChatColor.RED + "[GUIShop] Reload completed with errors - check console!");
            }
        } else {
            logUtil.log("GUIShop reloaded successfully!");
            if (sender != null) {
                getMiscUtils().sendPrefix(sender, "reload.execute");
            }
        }

        this.setIsReload(false);
    }

    public void initWriteCache() {
        // Cancel any existing task from a previous load/reload
        if (logFlushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(logFlushTaskId);
        }

        // Schedule periodic log flushing and rotation (every 5 minutes = 6000 ticks)
        logFlushTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (logUtil != null) {
                // Flush cached logs to disk
                logUtil.flushLogs();
                // Check and rotate oversized log files
                logUtil.checkAndRotateLogs();
            }
        }, 6000, 6000).getTaskId();
    }
}
