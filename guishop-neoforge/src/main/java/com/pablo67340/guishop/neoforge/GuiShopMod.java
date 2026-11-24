package com.pablo67340.guishop.neoforge;

import com.pablo67340.guishop.neoforge.command.ShopCommand;
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

    private final NeoForgeContext context;

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

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);

        LOGGER.info("GUIShop {} loaded; data folder {}",
                modContainer.getModInfo().getVersion(), context.dataFolder());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ShopCommand.register(event.getDispatcher());
    }

    private void onServerStarting(ServerStartingEvent event) {
        context.logUtil().log("GUIShop starting up on NeoForge.");
    }

    private void onServerStopped(ServerStoppedEvent event) {
        context.logUtil().flushLogs();
        context.shutdown();
        GuiShopPlatform.clear();
    }
}
