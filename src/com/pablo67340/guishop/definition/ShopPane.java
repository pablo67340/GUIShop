package com.pablo67340.guishop.definition;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.pablo67340.guishop.GUIShop;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.*;

public class ShopPane extends Pane {

    private final Map<Vector2d, GuiItem> items;
    @Getter
    private final Map<Vector2d, ItemStack> dummies;
    public static ShopPane INSTANCE;


    private Vector2d slotToCoordinates(int slot, int length) {
        int x = slot % length;
        int y = slot / length;
        return new Vector2d(x, y);
    }

    public ShopPane(int length, int height) {
        super(length, height);
        items = new HashMap<>();
        dummies = new HashMap<>();
        INSTANCE = this;
    }

    public void addItem(GuiItem item) {
        Vector2d coordinates = slotToCoordinates(items.size() + 1, getLength());
        items.put(coordinates, item);
    }

    public void setItem(GuiItem item, int slot) {
        if (slot > 53) {
            GUIShop.getINSTANCE().getLogUtil().log("Item: " + item.getItem().getType() + " was in slot " + slot + ". The max slot is lower than that. Item ignored. Please delete this item from shops.yml!");
        } else {
            Vector2d coordinates = slotToCoordinates(slot, getLength());
            items.put(coordinates, item);
        }
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public boolean click(@NotNull Gui var1, @NotNull InventoryComponent var2, @NotNull InventoryClickEvent var3, int var4, int var5, int var6, int var7, int var8){
        return false;
    }
    
    @Override
    public void display(@NotNull InventoryComponent inventory, int paneOffsetX, int paneOffsetY, int maxLength, int maxHeight){
        for (Map.Entry<Vector2d, GuiItem> entry : items.entrySet()){
            inventory.setItem(entry.getValue().getItem(), (int) entry.getKey().x, (int) entry.getKey().y);
        }
        for (Map.Entry<Vector2d, ItemStack> entry : dummies.entrySet()){
            inventory.setItem(entry.getValue(), (int) entry.getKey().x, (int) entry.getKey().y);
        }
    }

    @NotNull
    @Override
    public Collection<GuiItem> getItems() {
        return new ArrayList<>(items.values());
    }

    @NotNull
    @Override
    public Collection<Pane> getPanes() {
        return new HashSet<>();
    }

    public Map<Vector2d, GuiItem> getItemsMap() {
        return this.items;
    }

    public ShopPane getINSTANCE() {
        return INSTANCE;
    }
}
