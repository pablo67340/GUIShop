package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.economy.BalanceStore;
import com.pablo67340.guishop.neoforge.statistics.StatisticsStore;

/**
 * The server-side services, reachable from the static payload handlers.
 * <p>
 * Packet handlers are static by nature, so they need somewhere to find the
 * registry and the balances. Mirrors how core reaches its platform.
 */
public final class GuiShopServer {

    private static ShopRegistry registry;
    private static BalanceStore balances;
    private static StatisticsStore statistics;

    private GuiShopServer() {
    }

    public static void set(ShopRegistry shopRegistry, BalanceStore balanceStore,
                           StatisticsStore statisticsStore) {
        registry = shopRegistry;
        balances = balanceStore;
        statistics = statisticsStore;
    }

    public static void clear() {
        registry = null;
        balances = null;
        statistics = null;
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

    /**
     * @return the statistics store, or null before the server has started.
     *         Available even without a database, in which case it records
     *         nothing and answers with zeroes
     */
    public static StatisticsStore statistics() {
        return statistics;
    }
}
