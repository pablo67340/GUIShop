package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.Settings;
import com.pablo67340.guishop.neoforge.network.SaveSettingsPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.neoforge.permission.GuiShopPermissions;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies server settings changed from the settings screen.
 * <p>
 * Like {@link EditService}, the permission is re-checked here rather than
 * trusted from the request.
 */
public final class SettingsService {

    private SettingsService() {
    }

    public static void handle(ServerPlayer player, SaveSettingsPayload request) {
        if (!GuiShopPermissions.canEdit(player)) {
            GuiShopPlatform.get().logger().warning(player.getGameProfile().getName()
                    + " tried to change server settings without permission.");
            reply(player, "You do not have permission to change server settings.", false);
            return;
        }

        int applied = Settings.apply(request.values());
        if (applied == 0) {
            reply(player, "Nothing was changed.", false);
            return;
        }

        // Price wording and the menu title both come from the settings, so the
        // menu is re-parsed and the catalogue rebuilt for the change to show.
        if (GuiShopServer.registry() != null) {
            GuiShopServer.registry().refresh("menu");
            GuiShopServer.registry().invalidateCatalogue();
        }
        GuiShopNetworkSender.broadcastCatalogue(player);

        reply(player, "Saved " + applied + (applied == 1 ? " setting." : " settings."), true);
    }

    private static void reply(ServerPlayer player, String message, boolean success) {
        GuiShopNetworkSender.send(player, new TradeResultPayload(
                Component.literal(message).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED),
                success,
                GuiShopServer.balances().get(player.getUUID()).doubleValue()));
    }
}
