package com.pablo67340.guishop.config;

import lombok.Getter;
import lombok.Setter;

public class TitlesConfig {

    @Getter
    @Setter
    public String menuName, signTitle, menuTitle, shopTitle, sellTitle, qtyTitle, menuShopPageNumber, valueTitle;
    
    // Transaction GUI configuration
    @Getter
    @Setter
    public String transactionTitle = "&8%item%";
    
    @Getter
    @Setter
    public String transactionBuyButton = "&a&lBuy %amount%";
    
    @Getter
    @Setter
    public String transactionSellButton = "&c&lSell %amount%";
    
    @Getter
    @Setter
    public String transactionBuyLore = "&7Click to buy %amount% for %price%";
    
    @Getter
    @Setter
    public String transactionSellLore = "&7Click to sell %amount% for %price%";
    
    @Getter
    @Setter
    public String transactionNotSellable = "&c&lItem Not Sellable";
    
    @Getter
    @Setter
    public String transactionNotBuyable = "&c&lItem Not Buyable";
    
    @Getter
    @Setter
    public String transactionBalanceTitle = "&6&l%player%";
    
    @Getter
    @Setter
    public String transactionBalanceLore = "&7Balance: &a%balance%";
}
