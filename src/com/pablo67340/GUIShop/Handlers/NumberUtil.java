package com.pablo67340.GUIShop.Handlers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.pablo67340.GUIShop.Main.Main;

public class NumberUtil
{
  Main plugin;
  
  public NumberUtil(Main instance)
  {
    plugin = instance;
  }
  
  static DecimalFormat twoDPlaces = new DecimalFormat("#,###.##");
  static DecimalFormat currencyFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));
  
  public static boolean isInt(String sInt)
  {
    try
    {
      Integer.parseInt(sInt);
    }
    catch (NumberFormatException e)
    {
      return false;
    }
    return true;
  }
}
