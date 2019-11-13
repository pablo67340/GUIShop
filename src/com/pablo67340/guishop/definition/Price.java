package com.pablo67340.guishop.definition;

import lombok.Getter;

public final class Price {

    /**
     * Buy & Sell price of the {@link Item}
     */
    @Getter
    private final double buyPrice, sellPrice;

    /**
     * Constructor, build entire {@link Price} at once.
     */
    public Price(double buyPrice, double sellPrice) {
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

}
