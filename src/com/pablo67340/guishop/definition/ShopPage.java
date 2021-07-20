package com.pablo67340.guishop.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.Getter;

/**
 *
 * @author Bryce
 */
@Data
public class ShopPage {

    Map<String, Item> items = new LinkedHashMap<>();

    @Getter
    int highestSlot = 0;

    public void setHighestSlot(Integer slot) {
        this.highestSlot = slot;
    }

}
