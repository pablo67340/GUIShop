package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.network.Catalogue;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks a stack up to the shop entry that will buy it.
 * <p>
 * The trade screen is told which entry it is selling. The sell container is
 * not: a player tips in whatever they have, so the entry has to be found from
 * the stack. Keyed by item rather than by anything finer because that is how
 * {@link ItemMatching} decides a match.
 * <p>
 * Built per sell rather than cached. It is one pass over the shop entries, next
 * to no work beside opening a screen, and a cache here would be another thing
 * to remember to invalidate when a price is edited.
 */
final class SellIndex {

    /**
     * Candidates per item, best price first.
     * <p>
     * Several shops may buy the same item at different prices, and nothing says
     * which is meant. The best price for the player wins, rather than whichever
     * shop file happened to be read last.
     */
    private final Map<Item, List<NeoItem>> byItem;

    private SellIndex(Map<Item, List<NeoItem>> byItem) {
        this.byItem = byItem;
    }

    static SellIndex of(Catalogue catalogue) {
        Map<Item, List<NeoItem>> byItem = new HashMap<>();

        for (List<NeoItem> entries : catalogue.items().values()) {
            for (NeoItem entry : entries) {
                if (!entry.hasSellPrice()) {
                    continue;
                }
                Item resolved = entry.resolveItem();
                if (resolved != null) {
                    byItem.computeIfAbsent(resolved, key -> new ArrayList<>()).add(entry);
                }
            }
        }

        byItem.values().forEach(candidates -> candidates.sort(
                Comparator.comparing(NeoItem::getSellPriceAsDecimal).reversed()));

        return new SellIndex(byItem);
    }

    /**
     * @return the entry that will buy this stack, or null if nothing will
     */
    NeoItem find(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        List<NeoItem> candidates = byItem.get(stack.getItem());
        if (candidates == null) {
            return null;
        }

        for (NeoItem candidate : candidates) {
            // Only the head rule is left to check; the item itself is the key.
            if (ItemMatching.headOwnerMatches(stack, candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
