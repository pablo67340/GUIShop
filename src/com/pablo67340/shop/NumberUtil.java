package com.pablo67340.shop;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberUtil
{
  GUIShop plugin;
  
  public NumberUtil(GUIShop instance)
  {
    this.plugin = instance;
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
