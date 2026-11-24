package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.platform.GuiShopPlatform;
import lombok.Data;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of pages, keyed by page name. Shared by shops and menus, which
 * differ only in the item and page types they hold.
 *
 * @param <T> the platform's item type
 * @param <P> the page type holding those items
 */
@Data
public class PagedDefinition<T extends ItemDefinition, P extends PageDefinition<T>> implements Cloneable {

    private Map<String, P> pages = new LinkedHashMap<>();

    /**
     * Configured number of rows for this inventory (1-6).
     * If not set (0), rows are calculated dynamically based on item count.
     */
    private int configuredRows = 0;

    public int getHighestPageSlot(String page) {
        return pages.get(page).getHighestSlot();
    }

    /**
     * Recalculates each page's highest occupied slot, which drives how many rows
     * the page is rendered with.
     */
    public void determineHighestSlots() {
        for (Map.Entry<String, P> entry : pages.entrySet()) {
            P page = entry.getValue();

            // Empty pages are legitimate, e.g. worth-only shops whose materials
            // are all block-only.
            if (page.getItems().isEmpty()) {
                page.setHighestSlot(0);
                GuiShopPlatform.get().logUtil()
                        .debugLog("Page " + entry.getKey() + " is empty, setting highest slot to 0");
                continue;
            }

            // Slots are nullable, and comparing them would throw.
            T highest = page.getItems().values().stream()
                    .filter(item -> item.getSlot() != null)
                    .max(Comparator.comparing(ItemDefinition::getSlot))
                    .orElse(null);

            if (highest != null) {
                page.setHighestSlot(highest.getSlot());
                GuiShopPlatform.get().logUtil()
                        .debugLog("Highest slot for Page: " + entry.getKey() + " is " + highest.getSlot());
            } else {
                page.setHighestSlot(0);
                GuiShopPlatform.get().logUtil()
                        .debugLog("Page " + entry.getKey() + " has no items, setting highest slot to 0");
            }
        }
    }
}
