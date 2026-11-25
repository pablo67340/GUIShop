package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.definition.NeoItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The catalogue as sent to clients, together with the items behind it.
 * <p>
 * An entry is identified by its position in its shop file, which is what the
 * loader records as the item's slot. The client is sent that number and echoes
 * it back, so a request names an item in the file rather than a place in a
 * list. The list the client shows is filtered and can be re-sorted, so a list
 * position would mean different things on each side.
 *
 * @param payload the views sent to clients
 * @param items   shop name, lowercased, to its entries
 */
public record Catalogue(OpenShopPayload payload, Map<String, List<NeoItem>> items) {

    /**
     * The item an entry id refers to.
     *
     * @param entryId the item's position in its shop file
     * @return the item, or null if the shop or id is unknown, which is what a
     *         stale or forged request looks like
     */
    public NeoItem entryAt(String shop, int entryId) {
        List<NeoItem> entries = items.get(key(shop));
        if (entries == null) {
            return null;
        }

        // Searched rather than indexed because this list has had the
        // non-tradeable entries filtered out, so an item's id is not its
        // position here.
        for (NeoItem item : entries) {
            if (item.getSlot() != null && item.getSlot() == entryId) {
                return item;
            }
        }
        return null;
    }

    static String key(String shop) {
        return shop == null ? "" : shop.toLowerCase(Locale.ROOT);
    }
}
