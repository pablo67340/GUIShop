package com.pablo67340.guishop.util;

import java.util.ArrayList;
import java.util.List;

import com.pablo67340.guishop.listenable.Menu;

import lombok.Getter;
import lombok.Setter;

public final class Config {

	/**
	 * True/False if GUIShop should use signs only.
	 */
	@Getter
	@Setter
	private static boolean registerCommands, signsOnly, escapeOnly, soundEnabled, enableCreator, dynamicPricing, debugMode;
	
	@Getter
	private static List<String> disabledQty = new ArrayList<>();

	/**
	 * Common Language strings set in configuration.
	 * 
	 */
	@Getter
	@Setter
	private static String added, cantSell, cantBuy, prefix, purchased, menuName, notEnoughPre, notEnoughPost, signTitle,
			sellCommand, menuTitle, shopTitle, sellTitle, sold, taken, sound, full, currency, noPermission, qtyTitle,
			currencySuffix, backButtonItem, backButtonText, cannotSell, cannotBuy, buyLore, sellLore, freeLore,
			forwardPageButtonName, backwardPageButtonName;

	/**
	 * Number of rows for the {@link Menu} GUI.
	 * 
	 */
	@Getter
	@Setter
	private static Integer menuRows;


}
