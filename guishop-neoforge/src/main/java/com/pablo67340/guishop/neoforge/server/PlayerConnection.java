package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.economy.BalanceStore;
import com.pablo67340.guishop.neoforge.statistics.StatisticsStore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps a player's money and statistics ready while they are online, and sends
 * them the prices their tooltips need.
 */
public final class PlayerConnection {

    private PlayerConnection() {
    }

    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BalanceStore balances = GuiShopServer.balances();
        if (balances != null) {
            // Reading the balance is what opens the account, so a player who
            // has never shopped still has their starting money the first time
            // anything asks. The name is recorded so the admin commands can
            // find them again once they are offline.
            balances.get(player.getUUID());
            balances.rememberName(player.getUUID(), player.getGameProfile().getName());
        }

        StatisticsStore statistics = GuiShopServer.statistics();
        if (statistics != null) {
            statistics.loadPlayer(player.getUUID());
        }

        // Before they have opened anything: worth lines appear in the
        // inventory, and the first thing most players do is press E.
        GuiShopNetworkSender.sendWorth(player);
    }

    public static void onQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Written now rather than waiting for the next flush, so a crash
        // between here and then cannot lose what they earned this session.
        BalanceStore balances = GuiShopServer.balances();
        if (balances != null) {
            balances.save();
        }

        StatisticsStore statistics = GuiShopServer.statistics();
        if (statistics != null) {
            statistics.unloadPlayer(player.getUUID());
        }
    }
}
