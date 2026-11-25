package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import com.pablo67340.guishop.neoforge.menu.GuiShopMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Tells the client which screen opens for the sell container.
 * <p>
 * A client-only subscriber rather than a listener added in the mod
 * constructor: the event itself is a client class, so a dedicated server would
 * fail loading a listener for it. FML only loads this class on the client, and
 * routes it to the mod bus on its own because the event says which bus it
 * belongs to.
 */
@EventBusSubscriber(modid = GuiShopMod.MOD_ID, value = Dist.CLIENT)
public final class ClientMenus {

    private ClientMenus() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(GuiShopMenus.SELL.get(), SellScreen::new);
    }
}
