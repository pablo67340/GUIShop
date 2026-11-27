package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.ItemFieldsPayload;
import com.pablo67340.guishop.neoforge.network.OpenShopPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.neoforge.network.WorthPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client-side entry point for the shop screens.
 * <p>
 * Isolated in its own class so the dedicated server, which never handles these
 * payloads, does not resolve the screen classes.
 */
public final class ClientShopUi {

    private ClientShopUi() {
    }

    /**
     * Opens the shop, or refreshes it in place if it is already open.
     * <p>
     * Refreshing matters because the server resends the catalogue after an
     * edit: reopening would throw away where the player had scrolled to and
     * which shop they were looking at, in the middle of them working.
     */
    public static void open(OpenShopPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen current = minecraft.screen;

        if (current instanceof ShopBrowserScreen browser) {
            browser.refresh(payload);
            return;
        }
        if (current instanceof ShopChildScreen child) {
            // An editor or trade panel is open over the browser; update what it
            // will return to rather than closing it under the player.
            child.parent().refresh(payload);
            return;
        }

        minecraft.setScreen(new ShopBrowserScreen(payload, payload.initialView()));
    }

    /**
     * Opens the item editor, now that the server has sent the item's
     * properties. Ignored unless the browser is still what the player is
     * looking at, since they may have navigated away while it was in flight.
     */
    public static void openEditor(ItemFieldsPayload fields) {
        if (Minecraft.getInstance().screen instanceof ShopBrowserScreen browser) {
            browser.openEditor(fields);
        }
    }

    /**
     * Takes the server's table of sell prices, for the worth tooltips.
     */
    public static void acceptWorth(WorthPayload payload) {
        WorthTooltip.accept(payload);
    }

    /**
     * Shows the outcome of a trade or a save on whichever shop screen is open.
     */
    public static void showResult(TradeResultPayload result) {
        Screen current = Minecraft.getInstance().screen;

        if (current instanceof ShopChildScreen child) {
            child.onResult(result);
        } else if (current instanceof ShopBrowserScreen browser) {
            browser.onResult(result);
        }
    }
}
