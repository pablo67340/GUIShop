package com.pablo67340.guishop.economy;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tuning values for {@link DynamicPricingManager}.
 * <p>
 * A plain settings object with no loading code: each platform reads its own
 * file format and populates this, which keeps the pricing maths independent of
 * whether the values came from YAML, TOML or anywhere else. Defaults here are
 * the ones used when a key is absent.
 */
public class DynamicPricingSettings {

    /**
     * Fraction the price moves per item of stock imbalance. 0.01 is 1%.
     */
    @Getter
    @Setter
    private double priceChangePerTransaction = 0.01;

    /**
     * Upper bound on the price multiplier, e.g. 2.0 caps at 200% of base.
     */
    @Getter
    @Setter
    private double maxPriceMultiplier = 2.0;

    /**
     * Lower bound on the price multiplier, e.g. 0.5 floors at 50% of base.
     */
    @Getter
    @Setter
    private double minPriceMultiplier = 0.5;

    /**
     * Fraction of the remaining imbalance removed on each normalization tick.
     */
    @Getter
    @Setter
    private double normalizationRate = 0.001;

    /**
     * Seconds between normalization ticks.
     */
    @Getter
    @Setter
    private int normalizationInterval = 300;

    /**
     * Per-item overrides, keyed by upper-case item key.
     */
    @Getter
    private final Map<String, ItemPriceOverride> itemOverrides = new ConcurrentHashMap<>();

    /**
     * Per-item settings that replace the global defaults, and the knock-on
     * effects this item has on others.
     */
    public static class ItemPriceOverride {

        @Getter
        private final double priceChangePerItem;

        @Getter
        private final double maxMultiplier;

        @Getter
        private final double minMultiplier;

        /**
         * Items whose stock also moves when this one is traded, keyed by
         * upper-case item key. The value scales the quantity, so a block that
         * contains nine of something uses 9.0.
         */
        @Getter
        private final Map<String, Double> affects;

        public ItemPriceOverride(double priceChangePerItem, double maxMultiplier, double minMultiplier,
                                 Map<String, Double> affects) {
            this.priceChangePerItem = priceChangePerItem;
            this.maxMultiplier = maxMultiplier;
            this.minMultiplier = minMultiplier;
            this.affects = affects != null ? affects : new HashMap<>();
        }
    }
}
