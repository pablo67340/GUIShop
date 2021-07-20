package com.pablo67340.guishop.config;

import lombok.Getter;
import lombok.Setter;

public class AltSellConfig {
    @Getter
    @Setter
    public int Quantity1, Quantity2, Quantity3;
    
    @Getter
    @Setter
    public String addMaterial, removeMaterial, indicatorMaterial, confirmMaterial, cancelMaterial,
            confirmName, cancelName, notEnough, increaseTitle, decreaseTitle, title;
}
