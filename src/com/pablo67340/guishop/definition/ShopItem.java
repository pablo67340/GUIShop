package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.Main;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
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
        Main.debugLog("Highest Slot for Page: "+page+" is "+highestPageItem.getSlot());
        return highestPageItem.getSlot();
    }
}
