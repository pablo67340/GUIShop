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

            // Handle empty pages gracefully
            if (menuPage.getItems().isEmpty()) {
                menuPage.setHighestSlot(0);
                pages.put(entry.getKey(), menuPage);
                GUIShop.getINSTANCE().getLogUtil().debugLog("Page " + entry.getKey() + " is empty, setting highest slot to 0");
                continue;
            }

            Item highestPageItem = menuPage.getItems().values().stream().max(Comparator.comparing(Item::getSlot)).orElse(null);
            if (highestPageItem != null) {
            menuPage.setHighestSlot(highestPageItem.getSlot());
                GUIShop.getINSTANCE().getLogUtil().debugLog("Highest slot for Page: " + entry.getKey() + " is " + highestPageItem.getSlot());
            } else {
                menuPage.setHighestSlot(0);
                GUIShop.getINSTANCE().getLogUtil().debugLog("Page " + entry.getKey() + " has no items, setting highest slot to 0");
            }

            pages.put(entry.getKey(), menuPage);
        }
    }
}
