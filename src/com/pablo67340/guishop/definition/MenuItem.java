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
public class MenuItem implements Cloneable {

    Map<String, MenuPage> pages = new LinkedHashMap<>();

    public int getHighestPageSlot(String page) {
        return pages.get(page).getHighestSlot();
    }

    public void determineHighestSlots() {
        for (Map.Entry<String, MenuPage> entry : pages.entrySet()) {
            MenuPage menuPage = entry.getValue();

            Item highestPageItem = menuPage.getItems().values().stream().max(Comparator.comparing(Item::getSlot)).get();
            menuPage.setHighestSlot(highestPageItem.getSlot());

            pages.put(entry.getKey(), menuPage);

            GUIShop.debugLog("Highest slot for Page: " + entry.getKey() + " is " + highestPageItem.getSlot());
        }
    }
}
