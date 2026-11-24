package com.pablo67340.guishop.definition;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents firework configuration for shop items.
 */
public class FireworkInfo {

    public FireworkInfo(int flight, List<ExplosionInfo> explosions) {
        this.flight = flight;
        this.explosions = explosions != null ? explosions : new ArrayList<>();
    }

    @Getter
    @Setter
    private int flight;

    @Getter
    @Setter
    private List<ExplosionInfo> explosions;

    /**
     * Represents a single firework explosion effect.
     */
    public static class ExplosionInfo {
        
        public ExplosionInfo(String shape, List<Integer> colors, List<Integer> fadeColors, boolean hasFlicker, boolean hasTrail) {
            this.shape = shape;
            this.colors = colors != null ? colors : new ArrayList<>();
            this.fadeColors = fadeColors != null ? fadeColors : new ArrayList<>();
            this.hasFlicker = hasFlicker;
            this.hasTrail = hasTrail;
        }

        @Getter
        @Setter
        private String shape; // small_ball, large_ball, star, creeper, burst

        @Getter
        @Setter
        private List<Integer> colors;

        @Getter
        @Setter
        private List<Integer> fadeColors;

        @Getter
        @Setter
        private boolean hasFlicker;

        @Getter
        @Setter
        private boolean hasTrail;
    }
}

