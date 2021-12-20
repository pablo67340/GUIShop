package com.pablo67340.guishop.config;

import com.pablo67340.guishop.definition.CommandsMode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public final class Config {

    /**
     * Some configuration booleans to enable/disable things
     */
    @Getter
    @Setter
    private static boolean signsOnly, disableBackButton, disableEscapeBack, alternateSellEnabled, soundEnabled,
            dynamicPricing, debugMode, sellSkullUUID, disableEscapeBackQuantity, transactionLog;

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
    private static ButtonConfig buttonConfig = new ButtonConfig();

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
