package com.pablo67340.guishop.definition;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public final class Item {

    /**
     * The name of this {@link Item} when presented on the GUI.
     */
    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private int slot;

    /**
     * The Material of this {@link Item}.
     */
    @Getter
    @Setter
    private String material;

    /**
     * The price to buy this {@link Item}.
     */
    @Getter
    @Setter
    private Object buyPrice;

    /**
     * The mob ID of this item if it's a spawner {@link Item}.
     */
    @Getter
    @Setter
    private String mobType;

    /**
     * The amount of money given when selling this {@link Item}.
     */
    @Getter
    @Setter
    private Object sellPrice;

    /**
     * The slot of this {@link Item} when presented on the GUI.
     */
    @Getter
    @Setter
    private ItemType itemType;
    
    @Getter
    @Setter
    private List<String> lore;
    
    @Getter
    @Setter
    private List<String> commands;

    /**
     * The enchantsments on this {@link Item}.
     */
    @Getter
    @Setter
    private String[] enchantments;

    public Boolean canBuyItem() {
        if (buyPrice instanceof Boolean) {
            return false;
        } else return buyPrice instanceof Double;
    }

    public Boolean canSellItem() {
        if (sellPrice instanceof Boolean) {
            return false;
        } else if (sellPrice instanceof Double) {
            return (Double) sellPrice != 0;
        }
        return false;
    }

    public Boolean isMobSpawner() {
        return material.equalsIgnoreCase("SPAWNER") || material.equalsIgnoreCase("MOB_SPAWNER");
    }


}
