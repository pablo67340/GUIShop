/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pablo67340.guishop.definition;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Bryce
 */
public class PotionInfo {
    
    public PotionInfo(String type, Boolean splash, Boolean extended, Boolean upgraded){
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
