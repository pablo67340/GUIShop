package com.pablo67340.guishop.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Bryce
 */
@Data
public class MenuPage {
    Map<String, Item> items = new LinkedHashMap<>();

    @Setter
    @Getter
    int highestSlot = 0;
}
