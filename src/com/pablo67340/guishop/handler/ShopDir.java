package com.pablo67340.guishop.handler;

import java.util.List;

import lombok.Getter;

public class ShopDir {

    @Getter
    public String name, shop, description;

    @Getter
    public List<String> lore;

    public ShopDir(String shop, String name, String description, List<String> lore) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
    }


}
