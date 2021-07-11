package com.pablo67340.guishop.definition;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.pablo67340.guishop.GUIShop;
import lombok.Data;

/**
 *
 * @author Bryce
 */
@Data
public class ShopItem implements Cloneable {
    Map<String, ShopPage> pages = new LinkedHashMap<>();

    public int getHighestPageSlot(String page){
        ShopPage shopPage = pages.get(page);
        Item highestPageItem = shopPage.getItems().values().stream().max(Comparator.comparing(Item::getSlot)).get();
        GUIShop.debugLog("Highest slot for page: " + page + " is " + highestPageItem.getSlot());
        return highestPageItem.getSlot();
    }
}
