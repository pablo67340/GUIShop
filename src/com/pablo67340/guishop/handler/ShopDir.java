package com.pablo67340.guishop.handler;

import java.util.List;

import com.pablo67340.guishop.definition.ItemType;

import lombok.Getter;

public class ShopDir {

    @Getter
    public String name, shop, description, itemID;

    @Getter
    public List<String> lore;
    
    @Getter
    public ItemType itemType;

    public ShopDir(String shop, String name, String description, List<String> lore, ItemType itemType, String itemID) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
        this.itemType = itemType;
        this.itemID = itemID;
    }


}
