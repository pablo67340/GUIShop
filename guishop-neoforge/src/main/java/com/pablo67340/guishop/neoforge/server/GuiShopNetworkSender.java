package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.neoforge.config.Settings;
import com.pablo67340.guishop.neoforge.permission.GuiShopPermissions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;

/**
 * Sends payloads to players, in one place so the per-viewer details are not
 * repeated at every call site.
 */
public final class GuiShopNetworkSender {

    private GuiShopNetworkSender() {
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    /**
     * Sends the catalogue as this player should see it, with their own
     * permission and balance folded in.
     * <p>
     * The server settings go only to a player who may change them, so an
     * ordinary client is never sent configuration it has no use for.
     */
    public static void sendCatalogue(ServerPlayer player) {
        sendCatalogue(player, "");
    }

    /**
     * @param openShop which shop the screen should start on, or empty for the
     *                 menu
     */
    /**
     * Whether this player's current world allows the shop to be opened.
     * <p>
     * Checked by dimension id, which is the only stable name a world has here;
     * Bukkit matched on the world folder's name, which has no equivalent.
     */
    public static boolean isWorldAllowed(ServerPlayer player) {
        List<String> disabled = BaseConfig.getDisabledWorlds();
        if (disabled == null || disabled.isEmpty()) {
            return true;
        }

        String dimension = player.level().dimension().location().toString();
        String path = player.level().dimension().location().getPath();

        // Either spelling is accepted, so "the_nether" works as well as
        // "minecraft:the_nether".
        return disabled.stream().noneMatch(entry ->
                entry.equalsIgnoreCase(dimension) || entry.equalsIgnoreCase(path));
    }

    public static void sendCatalogue(ServerPlayer player, String openShop) {
        boolean canEdit = GuiShopPermissions.canEdit(player);

        ShopRegistryAccess.catalogue(player).ifPresent(catalogue ->
                send(player, catalogue.payload()
                        .forViewer(
                                canEdit,
                                GuiShopServer.balances().get(player.getUUID()).doubleValue(),
                                canEdit ? Settings.currentValues() : Map.of())
                        .opening(openShop)));
    }

    /**
     * Resends the catalogue to everyone who could be looking at it, so an edit
     * shows up without each player reopening the shop.
     */
    public static void broadcastCatalogue(ServerPlayer editor) {
        for (ServerPlayer player : editor.server.getPlayerList().getPlayers()) {
            sendCatalogue(player);
        }
    }
}
