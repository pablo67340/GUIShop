package com.pablo67340.guishop.platform;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.util.LogUtil;
import com.pablo67340.guishop.util.SchedulerUtil;

import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Bukkit's implementation of the services core needs.
 */
public final class BukkitContext implements GuiShopContext {

    private final GUIShop plugin;
    private final PlatformLogger logger;
    private final PlatformScheduler scheduler;

    public BukkitContext(GUIShop plugin) {
        this.plugin = plugin;
        this.logger = new PluginLogger(plugin);
        this.scheduler = new FoliaLibScheduler();
    }

    @Override
    public Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public PlatformLogger logger() {
        return logger;
    }

    @Override
    public LogUtil logUtil() {
        return plugin.getLogUtil();
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public boolean isDebugMode() {
        return Config.isDebugMode();
    }

    @Override
    public boolean isTransactionLog() {
        return Config.isTransactionLog();
    }

    private static final class PluginLogger implements PlatformLogger {

        private final GUIShop plugin;

        private PluginLogger(GUIShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public void info(String message) {
            plugin.getLogger().log(Level.INFO, message);
        }

        @Override
        public void warning(String message) {
            plugin.getLogger().log(Level.WARNING, message);
        }
    }

    /**
     * Delegates to SchedulerUtil so Folia's regionized schedulers keep being
     * used, and adapts its TaskHolder callback into a cancel handle.
     */
    private static final class FoliaLibScheduler implements PlatformScheduler {

        @Override
        public CancellableTask runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
            SchedulerUtil.TaskHolder holder = new SchedulerUtil.TaskHolder();
            SchedulerUtil.runTaskTimerAsync(holder, task, delayTicks, periodTicks);
            return () -> SchedulerUtil.cancelTask(holder);
        }
    }
}
