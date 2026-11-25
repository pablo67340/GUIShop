package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.server.EditService;
import com.pablo67340.guishop.neoforge.server.SettingsService;
import com.pablo67340.guishop.neoforge.server.ShopAdminService;
import com.pablo67340.guishop.neoforge.server.TradeService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers GUIShop's packets.
 * <p>
 * Everything the client sends is a request, never an instruction: the server
 * decides what it means and whether it is allowed.
 */
public final class GuiShopNetwork {

    /**
     * Bumped whenever a payload's wire format changes, so a mismatched client
     * is rejected at handshake rather than misreading a buffer.
     */
    private static final String PROTOCOL_VERSION = "2";

    private GuiShopNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                OpenShopPayload.TYPE,
                OpenShopPayload.CODEC,
                GuiShopNetwork::handleOpenShop);

        registrar.playToClient(
                TradeResultPayload.TYPE,
                TradeResultPayload.CODEC,
                GuiShopNetwork::handleTradeResult);

        registrar.playToServer(
                TradeRequestPayload.TYPE,
                TradeRequestPayload.CODEC,
                (payload, context) -> onServer(context, player -> TradeService.handle(player, payload)));

        registrar.playToServer(
                SaveItemPayload.TYPE,
                SaveItemPayload.CODEC,
                (payload, context) -> onServer(context, player -> EditService.handle(player, payload)));

        registrar.playToServer(
                SaveSettingsPayload.TYPE,
                SaveSettingsPayload.CODEC,
                (payload, context) -> onServer(context, player -> SettingsService.handle(player, payload)));

        registrar.playToServer(
                ShopAdminPayload.TYPE,
                ShopAdminPayload.CODEC,
                (payload, context) -> onServer(context, player -> ShopAdminService.handle(player, payload)));

        registrar.playToServer(
                RequestItemPayload.TYPE,
                RequestItemPayload.CODEC,
                (payload, context) -> onServer(context, player -> EditService.handle(player, payload)));

        registrar.playToClient(
                ItemFieldsPayload.TYPE,
                ItemFieldsPayload.CODEC,
                GuiShopNetwork::handleItemFields);
    }

    /**
     * Runs the handler on the server thread with the sending player.
     * <p>
     * Decoding happens on the network thread, so anything that touches the
     * world or a player's inventory has to be handed over first.
     */
    private static void onServer(IPayloadContext context, java.util.function.Consumer<ServerPlayer> handler) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handler.accept(player);
            }
        });
    }

    /**
     * Kept as indirections so the client-only screen classes are not resolved
     * on a dedicated server, which never receives these payloads.
     */
    private static void handleOpenShop(OpenShopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.pablo67340.guishop.neoforge.client.ClientShopUi.open(payload));
    }

    private static void handleTradeResult(TradeResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.pablo67340.guishop.neoforge.client.ClientShopUi.showResult(payload));
    }

    private static void handleItemFields(ItemFieldsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> com.pablo67340.guishop.neoforge.client.ClientShopUi.openEditor(payload));
    }
}
