package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.GUIShop;
import lombok.Data;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Bryce
 */
@Data
public class ShopItem implements Cloneable {

    Map<String, ShopPage> pages = new LinkedHashMap<>();
    
    /**
     * Configured number of rows for this shop's inventory (1-6).
     * If not set (0), rows are calculated dynamically based on item count.
     */
    private int configuredRows = 0;

    public int getHighestPageSlot(String page) {
        return pages.get(page).getHighestSlot();
    }

    public void determineHighestSlots() {
        for (Map.Entry<String, ShopPage> entry : pages.entrySet()) {
            ShopPage shopPage = entry.getValue();

            // Handle empty pages gracefully (can happen with worth-only shops that have block-only materials)
            if (shopPage.getItems().isEmpty()) {
                shopPage.setHighestSlot(0);
                pages.put(entry.getKey(), shopPage);
                GUIShop.getINSTANCE().getLogUtil().debugLog("Page " + entry.getKey() + " is empty, setting highest slot to 0");
                continue;
            }

            Item highestPageItem = shopPage.getItems().values().stream().max(Comparator.comparing(Item::getSlot)).orElse(null);
            if (highestPageItem != null) {
            shopPage.setHighestSlot(highestPageItem.getSlot());
                GUIShop.getINSTANCE().getLogUtil().debugLog("Highest slot for Page: " + entry.getKey() + " is " + highestPageItem.getSlot());
            } else {
                shopPage.setHighestSlot(0);
                GUIShop.getINSTANCE().getLogUtil().debugLog("Page " + entry.getKey() + " has no items, setting highest slot to 0");
            }

            pages.put(entry.getKey(), shopPage);
        }
    }
}
