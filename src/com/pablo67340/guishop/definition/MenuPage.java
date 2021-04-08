/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pablo67340.guishop.definition;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 *
 * @author Bryce
 */
@Data
public class MenuPage {

    Map<String, Item> items = new LinkedHashMap<>();
    
}
