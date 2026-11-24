package com.pablo67340.guishop.config;

import com.pablo67340.guishop.definition.CommandsMode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The settings that mean the same thing on every platform.
 * <p>
 * Core owns the shape; each platform parses its own file format and populates
 * these via the setters, which is why there is no loading code here. Settings
 * that only make sense for a chest-inventory GUI stay in the platform's own
 * config class.
 */
public class BaseConfig {

    /**
     * Not instantiable; subclassed only so platforms can add their own static
     * settings while inheriting these.
     */
    protected BaseConfig() {
    }

    /**
     * Some configuration booleans to enable/disable things
     */
    @Getter
    @Setter
    private static boolean signsOnly, disableEscapeBack, soundEnabled,
            dynamicPricing, debugMode, sellSkullUUID, disableEscapeBackQuantity, transactionLog, hideNonBuyable,
            abbreviatePrices, showMarketInfo;

    /**
     * The commands mode, determines whether to intercept commands, register
     * them, or neither
     */
    @Getter
    @Setter
    private static CommandsMode commandsMode;

    /**
     * Common Language strings set in configuration
     */
    @Getter
    @Setter
    private static String sound;

    @Getter
    @Setter
    private static AltSellConfig altSellConfig = new AltSellConfig();

    @Getter
    @Setter
    private static TitlesConfig titlesConfig = new TitlesConfig();

    @Getter
    @Setter
    private static LoreConfig loreConfig = new LoreConfig();

    /**
     * The list of disabled worlds for GUIShop
     */
    @Getter
    @Setter
    private static List<String> disabledWorlds;
}
