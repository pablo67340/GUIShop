package com.pablo67340.guishop.definition;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Bryce
 */
public class PotionInfo {

    public PotionInfo(String type, boolean splash, boolean extended, boolean upgraded) {
        this.type = type;
        this.upgraded = upgraded;
        this.extended = extended;
        this.splash = splash;
    }

    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private Boolean extended, splash, upgraded;
}
