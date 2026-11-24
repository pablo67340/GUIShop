package com.pablo67340.guishop.neoforge;

import com.pablo67340.guishop.neoforge.command.ShopCommand;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.network.GuiShopNetwork;
import com.pablo67340.guishop.neoforge.network.ShopViewBuilder;
import com.pablo67340.guishop.platform.CancellableTask;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Mod entry point. Installs the platform context that the shared core module
 * reads its services from, then registers commands.
 */
@Mod(GuiShopMod.MOD_ID)
public final class GuiShopMod {

    public static final String MOD_ID = "guishop";

    private static final Logger LOGGER = LoggerFactory.getLogger("GUIShop");

    /** 30 seconds, matching the Bukkit module's flush interval. */
    private static final long LOG_FLUSH_INTERVAL_TICKS = 600L;

    private final NeoForgeContext context;
    private final ShopRegistry shopRegistry = new ShopRegistry();
    private CancellableTask logFlushTask;

    public GuiShopMod(IEventBus modEventBus, ModContainer modContainer) {
        this.context = new NeoForgeContext(LOGGER);

        // Must precede initLogUtil, which resolves its directory through the context.
        GuiShopPlatform.set(context);

        try {
            Files.createDirectories(context.dataFolder());
        } catch (IOException exception) {
            LOGGER.warn("Could not create the GUIShop data folder ({}): {}",
                    context.dataFolder(), exception.getMessage());
        }

        context.initLogUtil();

        modEventBus.addListener(GuiShopNetwork::register);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);

        LOGGER.info("GUIShop {} loaded; data folder {}",
                modContainer.getModInfo().getVersion(), context.dataFolder());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ShopCommand.register(event.getDispatcher(), shopRegistry);
    }

    private void onServerStarting(ServerStartingEvent event) {
        int loaded = shopRegistry.loadAll();
        context.logUtil().log("GUIShop loaded " + loaded + " shop(s) with "
                + shopRegistry.totalItems() + " item(s), "
                + shopRegistry.tradeableItems() + " priced, menu "
                + (shopRegistry.getMenu() != null ? "loaded" : "missing") + ".");

        shopRegistry.brokenMenuTargets().forEach(target ->
                context.logger().warning("Menu icon points at missing shop '" + target + "'."));

        // Built now rather than on the first /shop, so no player pays for
        // assembling the catalogue.
        context.logUtil().debugLog("Catalogue warmed, "
                + ShopViewBuilder.summarise(shopRegistry.catalogue(event.getServer().registryAccess())));

        // Without this, anything cached since the last flush is lost if the
        // process is killed rather than shut down.
        logFlushTask = context.scheduler().runTimerAsync(
                () -> context.logUtil().flushLogs(),
                LOG_FLUSH_INTERVAL_TICKS,
                LOG_FLUSH_INTERVAL_TICKS);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        if (logFlushTask != null) {
            logFlushTask.cancel();
            logFlushTask = null;
        }
        context.logUtil().flushLogs();
        context.shutdown();
        GuiShopPlatform.clear();
    }
}
