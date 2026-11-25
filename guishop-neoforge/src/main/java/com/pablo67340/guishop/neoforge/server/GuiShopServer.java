package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.economy.BalanceStore;

/**
 * The server-side services, reachable from the static payload handlers.
 * <p>
 * Packet handlers are static by nature, so they need somewhere to find the
 * registry and the balances. Mirrors how core reaches its platform.
 */
public final class GuiShopServer {

    private static ShopRegistry registry;
    private static BalanceStore balances;

    private GuiShopServer() {
    }

    public static void set(ShopRegistry shopRegistry, BalanceStore balanceStore) {
        registry = shopRegistry;
        balances = balanceStore;
    }

    public static void clear() {
        registry = null;
        balances = null;
    }

    /**
     * @return the shop registry, or null before the server has started
     */
    public static ShopRegistry registry() {
        return registry;
    }

    /**
     * @return the balance store, or null before the server has started
     */
    public static BalanceStore balances() {
        return balances;
    }
}
