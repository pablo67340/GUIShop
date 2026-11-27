package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.config.WorthConfig;
import com.pablo67340.guishop.neoforge.config.Settings;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.network.WorthPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the table of sell prices the client draws worth tooltips from.
 * <p>
 * Derived from {@link SellIndex}, which is also what prices an actual sale, so
 * the figure in the tooltip is the figure the player is paid. Building it from
 * the shop config separately would be two answers to one question, and the one
 * in the tooltip would be the one people noticed was wrong.
 */
public final class WorthTable {

    /**
     * Rebuilt only when the catalogue changes, which is rare, and sent to
     * every player who logs in, which is not.
     */
    private static WorthPayload cached;

    private WorthTable() {
    }

    /**
     * The current table, building it if the cache is cold.
     */
    public static WorthPayload current(MinecraftServer server) {
        if (cached == null) {
            cached = build(server);
        }
        return cached;
    }

    /**
     * Throws the cache away, so the next send rebuilds it. Called after a
     * price is edited or the settings change.
     */
    public static void invalidate() {
        cached = null;
    }

    private static WorthPayload build(MinecraftServer server) {
        ShopRegistry registry = GuiShopServer.registry();
        if (!WorthConfig.isEnabled() || registry == null) {
            return WorthPayload.disabled();
        }

        Map<net.minecraft.world.item.Item, NeoItem> best =
                SellIndex.of(registry.catalogue(server.registryAccess())).best();

        List<WorthPayload.Entry> entries = new ArrayList<>(best.size());
        best.forEach((item, entry) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                return;
            }
            // The price of one, which the client multiplies by the stack size.
            // Linear because nothing here prices in bulk; if dynamic pricing
            // ever lands on this platform, the tooltip becomes an estimate and
            // should say so.
            entries.add(new WorthPayload.Entry(id,
                    entry.calculateSellPrice(1).doubleValue(),
                    ownerOf(entry)));
        });

        return new WorthPayload(
                true,
                WorthConfig.getFormat(),
                WorthConfig.getNotSellableFormat(),
                WorthConfig.isOnlyShowSellable(),
                WorthConfig.isAddBlankLine(),
                WorthConfig.isPositionTop(),
                Settings.getCurrencyPrefix(),
                Settings.getCurrencySuffix(),
                BaseConfig.isAbbreviatePrices(),
                List.copyOf(WorthConfig.getBlacklistedItemNames()),
                List.copyOf(WorthConfig.getIgnoreLoreContaining()),
                List.copyOf(entries));
    }

    /**
     * The head owner a price is tied to, or empty when any head will do.
     * <p>
     * Without this a shop buying one particular player's head would have the
     * client offer that price on every head in the game, and the sale would
     * then be refused.
     */
    private static String ownerOf(NeoItem entry) {
        if (!BaseConfig.isSellSkullUUID() || entry.getSkullUUID() == null) {
            return "";
        }
        return entry.getSkullUUID().trim();
    }
}
