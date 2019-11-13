package com.pablo67340.guishop.definition;

import java.util.List;

import lombok.Getter;

public class ShopDef {

    @Getter
    public String name, shop, description, itemID;

    @Getter
    public List<String> lore;
    
    @Getter
    public ItemType itemType;

    public ShopDef(String shop, String name, String description, List<String> lore, ItemType itemType, String itemID) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
        this.itemType = itemType;
        this.itemID = itemID;
    }


}
