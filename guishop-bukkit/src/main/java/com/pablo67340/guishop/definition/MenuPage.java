package com.pablo67340.guishop.definition;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Bryce
 */
@Data
public class MenuPage {

    Map<String, Item> items = new LinkedHashMap<>();

    @Setter
    @Getter
    int highestSlot = 0;
    
    /**
     * Configured rows for this specific page (1-6).
     * If 0, uses menu-level rows or auto-calculates.
     */
    @Setter
    @Getter
    int configuredRows = 0;
}
