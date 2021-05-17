package com.pablo67340.guishop.util;

import com.pablo67340.guishop.definition.CommandsMode;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public final class ConfigUtil {

    /**
     * True/False if GUIShop should use signs only.
     */
    @Getter
    @Setter
    private static boolean signsOnly, disableBackButton, disableEscapeBack, alternateSellEnabled, soundEnabled,
            dynamicPricing, debugMode, sellSkullUUID;

    /**
     * The commands mode, determines whether to intercept commands, register
     * them, or neither
     *
     */
    @Getter
    @Setter
    private static CommandsMode commandsMode;

    /**
     * Common Language strings set in configuration.
     *
     */
    @Getter
    @Setter
    private static String added, cantSell, cantBuy, prefix, purchased, menuName, notEnoughPre, notEnoughPost, signTitle,
            sellCommand, menuTitle, shopTitle, sellTitle, altSellTitle, sold, taken, sound, full, currency,
            noPermission, qtyTitle, currencySuffix, backButtonItem, backButtonText, cannotSell, cannotBuy, buyLore,
            sellLore, freeLore, forwardPageButtonName, backwardPageButtonName, altSellAddMaterial,
            altSellRemoveMaterial, altSellIndicatorMaterial, altSellConfirmMaterial, altSellCancelMaterial,
            altSellConfirmName, altSellCancelName, altSellNotEnough, menuShopPageNumber;

    /**
     * Integers from the config
     *
     */
    @Getter
    @Setter
    private static int altSellQuantity1, altSellQuantity2, altSellQuantity3;

    @Getter
    @Setter
    private static List<String> disabledWorlds;

}
