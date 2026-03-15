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
        this.lingering = false;
    }
    
    public PotionInfo(String type, boolean splash, boolean lingering, boolean extended, boolean upgraded) {
        this.type = type;
        this.upgraded = upgraded;
        this.extended = extended;
        this.splash = splash;
        this.lingering = lingering;
    }

    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private Boolean extended, splash, upgraded, lingering;
}
