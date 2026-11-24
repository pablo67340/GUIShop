package com.pablo67340.guishop.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Worth Display feature.
 * This feature shows item sell values in item lore using ProtocolLib packet interception.
 */
public final class WorthConfig {

    @Getter
    @Setter
    private static boolean enabled = true;

    @Getter
    @Setter
    private static String format = "&7Worth: &a%worth%";

    @Getter
    @Setter
    private static String position = "BOTTOM";

    @Getter
    @Setter
    private static boolean addBlankLine = true;

    @Getter
    @Setter
    private static boolean onlyShowSellable = true;

    @Getter
    @Setter
    private static String notSellableFormat = "&7Worth: &cNot sellable";

    @Getter
    @Setter
    private static List<String> ignoreLoreContaining = new ArrayList<>();

    @Getter
    @Setter
    private static List<String> blacklistedInventories = new ArrayList<>();

    @Getter
    @Setter
    private static List<String> blacklistedItemNames = new ArrayList<>();

    @Getter
    @Setter
    private static boolean playerInventoryOnly = false;

    @Getter
    @Setter
    private static boolean hideArmorSlots = true;

    @Getter
    @Setter
    private static boolean debug = false;

    /**
     * Check if the worth line should be added at the top of the lore
     */
    public static boolean isPositionTop() {
        return "TOP".equalsIgnoreCase(position);
    }

    /**
     * Check if the worth line should be added at the bottom of the lore
     */
    public static boolean isPositionBottom() {
        return "BOTTOM".equalsIgnoreCase(position);
    }
}

