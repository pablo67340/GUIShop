package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.definition.NeoItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The catalogue as sent to clients, together with the items behind it.
 * <p>
 * The client identifies an entry by its position in a shop's list, which is all
 * it has. The server needs to turn that back into the real item to price a
 * trade or to edit it, so the same ordering is kept here. Deriving both from
 * one build is what guarantees they agree; computing the order twice would
 * eventually let them drift.
 *
 * @param payload the views sent to clients
 * @param items   shop name, lowercased, to its entries in the order the client
 *                sees them
 */
public record Catalogue(OpenShopPayload payload, Map<String, List<NeoItem>> items) {

    /**
     * The item an entry index refers to.
     *
     * @return the item, or null if the shop or index is unknown, which is what
     *         a stale or forged request looks like
     */
    public NeoItem entryAt(String shop, int index) {
        List<NeoItem> entries = items.get(key(shop));
        if (entries == null || index < 0 || index >= entries.size()) {
            return null;
        }
        return entries.get(index);
    }

    static String key(String shop) {
        return shop == null ? "" : shop.toLowerCase(Locale.ROOT);
    }
}
