package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.network.Catalogue;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Guards access to the catalogue, which needs both the registry and the
 * server's registry access to build.
 */
final class ShopRegistryAccess {

    private ShopRegistryAccess() {
    }

    static Optional<Catalogue> catalogue(ServerPlayer player) {
        ShopRegistry registry = GuiShopServer.registry();
        if (registry == null || GuiShopServer.balances() == null) {
            return Optional.empty();
        }
        return Optional.of(registry.catalogue(player.server.registryAccess()));
    }
}
