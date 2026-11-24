package com.pablo67340.guishop.neoforge;

import com.pablo67340.guishop.platform.CancellableTask;
import com.pablo67340.guishop.platform.GuiShopContext;
import com.pablo67340.guishop.platform.PlatformLogger;
import com.pablo67340.guishop.platform.PlatformScheduler;
import com.pablo67340.guishop.util.LogUtil;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * NeoForge's implementation of the services core needs.
 */
public final class NeoForgeContext implements GuiShopContext {

    /**
     * Minecraft runs at 20 ticks per second. Core expresses scheduling in ticks
     * because that is the unit shop configs use.
     */
    private static final long MILLIS_PER_TICK = 50L;

    private final Path dataFolder;
    private final PlatformLogger logger;
    private final ScheduledExecutorService executor;
    private final PlatformScheduler scheduler;
    private LogUtil logUtil;

    public NeoForgeContext(Logger logger) {
        // Sits beside the world and config directories, mirroring where the
        // Bukkit plugin keeps its data under plugins/GUIShop.
        this.dataFolder = FMLPaths.GAMEDIR.get().resolve("guishop");
        this.logger = new Slf4jLogger(logger);
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "GUIShop-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler = new ExecutorScheduler(executor);
    }

    /**
     * Built after construction because {@link LogUtil} resolves its directory
     * through this context.
     */
    public void initLogUtil() {
        this.logUtil = new LogUtil();
    }

    @Override
    public Path dataFolder() {
        return dataFolder;
    }

    @Override
    public PlatformLogger logger() {
        return logger;
    }

    @Override
    public LogUtil logUtil() {
        return logUtil;
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static final class Slf4jLogger implements PlatformLogger {

        private final Logger delegate;

        private Slf4jLogger(Logger delegate) {
            this.delegate = delegate;
        }

        @Override
        public void info(String message) {
            delegate.info(message);
        }

        @Override
        public void warning(String message) {
            delegate.warn(message);
        }
    }

    /**
     * NeoForge has no off-thread repeating scheduler of its own, so core's
     * background work runs on a dedicated daemon thread.
     */
    private static final class ExecutorScheduler implements PlatformScheduler {

        private final ScheduledExecutorService executor;

        private ExecutorScheduler(ScheduledExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public CancellableTask runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
            ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                    task,
                    delayTicks * MILLIS_PER_TICK,
                    periodTicks * MILLIS_PER_TICK,
                    TimeUnit.MILLISECONDS);
            return () -> future.cancel(false);
        }
    }
}
