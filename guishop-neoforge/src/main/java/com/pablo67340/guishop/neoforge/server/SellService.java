package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.Messages;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.network.Catalogue;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prices and pays for the contents of the sell container.
 * <p>
 * Nothing here trusts a number from the client: the container's contents are
 * held server-side, and every price is read from the shop config.
 */
public final class SellService {

    private SellService() {
    }

    /**
     * Buys whatever the shops will take, hands back whatever they will not, and
     * empties the container either way.
     * <p>
     * Emptying it matters: the container is discarded when the screen closes,
     * so an item left behind would be destroyed. Anything unsellable goes back
     * to the player, dropped at their feet if their inventory is full.
     */
    public static void sellContents(ServerPlayer player, Container container) {
        Catalogue catalogue = catalogue(player);
        if (catalogue == null) {
            // No shops to sell to, so give everything back rather than eat it.
            returnAll(player, container);
            return;
        }

        SellIndex index = SellIndex.of(catalogue);
        BigDecimal earned = BigDecimal.ZERO;
        int sold = 0;
        int refused = 0;

        // Kept in encounter order so the log reads like the chest looked.
        Map<String, Sold> soldByItem = new LinkedHashMap<>();
        List<ItemStack> giveBack = new ArrayList<>();

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            container.setItem(slot, ItemStack.EMPTY);

            NeoItem entry = index.find(stack);
            if (entry == null) {
                refused += stack.getCount();
                giveBack.add(stack);
                continue;
            }

            int quantity = stack.getCount();
            BigDecimal value = entry.calculateSellPrice(quantity);
            earned = earned.add(value);
            sold += quantity;
            // Gathered per item rather than only as a total, because the
            // statistics record one row per item sold, not one per screenful.
            soldByItem.merge(entry.getMaterial(), new Sold(quantity, value), Sold::plus);
        }

        container.setChanged();
        giveBack.forEach(stack -> giveBack(player, stack));

        pay(player, earned, sold, soldByItem);

        if (refused > 0) {
            player.sendSystemMessage(Messages.chat("cant-sell-some", "amount", refused));
        }
    }

    private static void pay(ServerPlayer player, BigDecimal earned, int sold,
                            Map<String, Sold> soldByItem) {
        if (sold == 0) {
            return;
        }

        // Rounded once, at the end, so a hundred small items do not each lose
        // a fraction of a penny.
        BigDecimal total = earned.setScale(2, RoundingMode.HALF_UP);

        GuiShopServer.balances().deposit(player.getUUID(), total);
        soldByItem.forEach((material, entry) -> GuiShopServer.statistics()
                .recordSale(player.getUUID(), material, entry.quantity(), entry.value()));
        player.sendSystemMessage(Messages.chat("sell", "money", money(total)));

        GuiShopPlatform.get().logUtil().transactionLog(
                "Player " + player.getGameProfile().getName() + " sold " + sold + " items ("
                        + soldByItem.size() + " different) for " + total.toPlainString()
                        + " from the sell screen. Items: " + quantities(soldByItem));
    }

    /**
     * Just the counts, for the log line, which has never carried per-item
     * money and does not need to start.
     */
    private static Map<String, Integer> quantities(Map<String, Sold> soldByItem) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        soldByItem.forEach((material, entry) -> counts.put(material, entry.quantity()));
        return counts;
    }

    /**
     * How much of one item went through, and what it fetched.
     */
    private record Sold(int quantity, BigDecimal value) {

        Sold plus(Sold other) {
            return new Sold(quantity + other.quantity, value.add(other.value));
        }
    }

    /**
     * Returns everything, for when there is nothing to sell to.
     */
    private static void returnAll(ServerPlayer player, Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
                giveBack(player, stack);
            }
        }
        container.setChanged();
    }

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            // Dropped rather than discarded. getCount is read after the add,
            // which may have placed part of the stack before running out.
            player.drop(stack, false);
        }
    }

    private static Catalogue catalogue(ServerPlayer player) {
        ShopRegistry registry = GuiShopServer.registry();
        return registry == null ? null : registry.catalogue(player.server.registryAccess());
    }

    private static String money(BigDecimal amount) {
        return GuiShopPlatform.get().formatCurrency(amount);
    }
}
