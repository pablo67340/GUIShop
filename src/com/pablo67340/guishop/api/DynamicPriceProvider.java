package com.pablo67340.guishop.api;

import java.math.BigDecimal;

/**
 * Used to hook into GUIShop and provide dynamic pricing. <br>
 * <br>
 * Implementations must be registered with the Bukkit registration system in
 * order for GUIShop to detect and use them.
 *
 */
public interface DynamicPriceProvider {

    /**
     * Calculates the buy price of a certain quantity of a specific item. <br>
     * The item is provided as a unique string based key. <br>
     * <br>
     * The fixed, configured buy and sell prices of the item, as defined in the
     * shops.yml, are provided in order to parameterise the price equation this
     * DynamicPriceProvider uses.
     *
     * @param item a unique string based representation of the item
     * @param quantity the amount of the item which would be bought
     * @param staticBuyPrice the configured buy price in the shops.yml
     * @param staticSellPrice the configured sell price in the shops.yml
     * @return the calculated buy price
     */
    BigDecimal calculateBuyPrice(String item, int quantity, BigDecimal staticBuyPrice, BigDecimal staticSellPrice);

    /**
     * Calculates the sell price of a certain quantity of a specific item. <br>
     * The item is provided as a qunie string based key. <br>
     * <br>
     * The fixed, configured buy and sell prices of the item, as defined in the
     * shops.yml, are provided in order to parameterise the price equation this
     * DynamicPriceProvider uses.
     *
     * @param item a unique string based representation of the item
     * @param quantity the amount of the item which would be sold
     * @param staticBuyPrice the configured buy price in the shops.yml
     * @param staticSellPrice the configured sell price in the shops.yml
     * @return the calculated buy price
     */
    BigDecimal calculateSellPrice(String item, int quantity, BigDecimal staticBuyPrice, BigDecimal staticSellPrice);

    /**
     * Indicates to this DynamicPriceProvider that a certain quantity of a
     * specific item has been bought.
     *
     * @param item a unique string based representation of the item
     * @param quantity the amount of the item which was bought
     */
    void buyItem(String item, int quantity);

    /**
     * Indicates to this DynamicPriceProvider that a certain quantity of a
     * specific item has been sold.
     *
     * @param item a unique string based representation of the item
     * @param quantity the amount of the item which was sold
     */
    void sellItem(String item, int quantity);
}
