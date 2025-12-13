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
import com.pablo67340.guishop.util.ConfigManager;
import com.pablo67340.guishop.util.LogUtil;
import com.pablo67340.guishop.util.MiscUtils;
import com.pablo67340.guishop.util.RowChart;
import com.pablo67340.guishop.worth.WorthDisplayManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
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

        // Initialize Worth Display System (requires ProtocolLib)
        initWorthDisplay();
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
    }

    /**
     * Initialize the Worth Display system if ProtocolLib is available.
     */
    private void initWorthDisplay() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogUtil().log("ProtocolLib not found - Worth display feature disabled.");
            getLogUtil().log("Install ProtocolLib to show item worth in lore.");
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

    public UserCommand getUserCommands() {
        return (UserCommand) getServer().getPluginCommand("guishopuser").getExecutor();
    }

    public void warmup() {
        long startTime = System.currentTimeMillis();
        new Menu().loadItems(true);
        for (MenuPage page : loadedMenu.getPages().values()) {
            for (Item item : page.getItems().values()) {
                if (item.getTargetShop() != null) {
                    getLogUtil().debugLog("Starting Warmup for Shop: " + item.getTargetShop());
                    new Shop(item.getTargetShop()).loadItems(true);
                }
            }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        getLogUtil().debugLog("Item warming completed in: " + estimatedTime + "ms");
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

        logUtil.log("GUIShop reloaded");

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

        // Reload worth display system
        if (worthDisplayManager != null && worthDisplayManager.isRegistered()) {
            worthDisplayManager.unregister();
        }
        initWorthDisplay();

        getMiscUtils().sendPrefix(sender, "reload.execute");

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
