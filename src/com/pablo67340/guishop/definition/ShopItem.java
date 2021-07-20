package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.Main;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;

/**
 *
 * @author Bryce
 */
@Data
public class ShopItem implements Cloneable {

    Map<String, ShopPage> pages = new LinkedHashMap<>();

    public int getHighestPageSlot(String page) {
        return pages.get(page).getHighestSlot();
    }

    public void determineHighestSlots() {
        for (Entry<String, ShopPage> entry : pages.entrySet()) {
            ShopPage shopPage = entry.getValue();
            Item highestPageItem = shopPage.getItems().values().stream().max(Comparator.comparing(Item::getSlot)).get();
            shopPage.setHighestSlot(highestPageItem.getSlot());
            pages.put(entry.getKey(), shopPage);
            Main.debugLog("Highest Slot for Page: " + entry.getKey() + " is " + highestPageItem.getSlot());
        }
    }
}
