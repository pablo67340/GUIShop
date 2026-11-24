package com.pablo67340.guishop.definition;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One page of a shop or menu: the items it holds, plus the row geometry needed
 * to render it.
 * <p>
 * Generic over the item type so each platform can keep its own
 * {@link ItemDefinition} subclass in the map without casting. On Bukkit that is
 * {@code ShopPage extends PageDefinition<Item>}.
 *
 * @param <T> the platform's item type
 */
@Data
public class PageDefinition<T extends ItemDefinition> {

    private Map<String, T> items = new LinkedHashMap<>();

    private int highestSlot = 0;

    /**
     * Configured rows for this specific page (1-6).
     * If 0, uses the shop/menu level rows or auto-calculates.
     */
    private int configuredRows = 0;
}
