package com.pablo67340.guishop.util;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;

import java.util.List;

public class ItemBuilder {
    private final Item item = new Item();

    public ItemBuilder setItemType(ItemType itemType) {
        this.item.setItemType(itemType);
        return this;
    }

    public ItemBuilder setMaterial(String material) {
        this.item.setMaterial(material);
        return this;
    }

    public ItemBuilder setName(String name) {
        this.item.setName(name);
        return this;
    }

    public ItemBuilder setShopName(String shopName) {
        this.item.setShopName(shopName);
        return this;
    }

    public ItemBuilder setBuyName(String buyName) {
        this.item.setBuyName(buyName);
        return this;
    }

    public ItemBuilder setNames(String names) {
        this.setName(names);
        this.setShopName(names);
        this.setBuyName(names);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        this.item.setLore(lore);
        return this;
    }

    public ItemBuilder setShopLore(List<String> shopLore) {
        this.item.setShopLore(shopLore);
        return this;
    }

    public ItemBuilder setBuyLore(List<String> buyLore) {
        this.item.setBuyLore(buyLore);
        return this;
    }

    public ItemBuilder setLores(List<String> lores) {
        this.setLore(lores);
        this.setShopLore(lores);
        this.setBuyLore(lores);
        return this;
    }

    public Item build() {
        return this.item;
    }
}
