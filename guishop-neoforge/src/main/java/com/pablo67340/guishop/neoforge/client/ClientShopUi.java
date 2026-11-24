package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.OpenShopPayload;
import net.minecraft.client.Minecraft;

/**
 * Client-side entry point for opening the shop.
 * <p>
 * Isolated in its own class so the dedicated server, which never handles the
 * open payload, does not resolve the screen classes.
 */
public final class ClientShopUi {

    private ClientShopUi() {
    }

    public static void open(OpenShopPayload payload) {
        Minecraft.getInstance().setScreen(
                new ShopBrowserScreen(payload, payload.menu()));
    }
}
