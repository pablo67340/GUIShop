package com.pablo67340.GUIShop.Handlers;

import com.pablo67340.GUIShop.Main.Main;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberUtil {
	Main plugin;
	static DecimalFormat twoDPlaces = new DecimalFormat("#,###.##");
	static DecimalFormat currencyFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));

	public NumberUtil(Main instance) {
		this.plugin = instance;
	}

	public static boolean isInt(String sInt) {
		try {
			Integer.parseInt(sInt);
		}
		catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}

