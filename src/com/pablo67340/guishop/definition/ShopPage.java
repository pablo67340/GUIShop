package com.pablo67340.guishop.definition;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ShopPage {

    Map<String, Item> items = new LinkedHashMap<>();

    @Setter
    @Getter
    int highestSlot = 0;
}
