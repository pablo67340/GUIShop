package com.pablo67340.guishop.neoforge.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers GUIShop's packets.
 */
public final class GuiShopNetwork {

    /**
     * Bumped whenever a payload's wire format changes, so a mismatched client
     * is rejected at handshake rather than misreading a buffer.
     */
    private static final String PROTOCOL_VERSION = "1";

    private GuiShopNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                OpenShopPayload.TYPE,
                OpenShopPayload.CODEC,
                GuiShopNetwork::handleOpenShop);
    }

    /**
     * Kept as an indirection so the client-only screen classes are not resolved
     * on a dedicated server, which never receives this payload.
     */
    private static void handleOpenShop(OpenShopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.pablo67340.guishop.neoforge.client.ClientShopUi.open(payload));
    }
}
