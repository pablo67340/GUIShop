package com.pablo67340.guishop.definition;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AltSellPane extends Pane {

    private final List<GuiItem> items = new ArrayList<>();

    @Getter
    private int subjectQuantity;

    public AltSellPane(GuiItem subjectItem, GuiItem[] addRemoveItems, GuiItem indicatorItem, GuiItem confirmItem, GuiItem cancelItem) {
        super(9, 6);
        items.add(subjectItem);
        items.addAll(Arrays.asList(addRemoveItems).subList(0, 6));
        items.add(indicatorItem);
        items.add(confirmItem);
        items.add(cancelItem);
    }

    public int setSubjectQuantity(int subjectQuantity) {
        ItemStack subjectItem = items.get(0).getItem().clone();
        if (subjectQuantity > subjectItem.getMaxStackSize()) {
            subjectQuantity = subjectItem.getMaxStackSize();
        }
        subjectItem.setAmount(subjectQuantity);
        items.set(0, new GuiItem(subjectItem));
        this.subjectQuantity = subjectQuantity;
        return subjectQuantity;
    }

    public void setIndicatorName(String name) {
        items.set(7, Item.renameGuiItem(items.get(7), name));
    }

    @Override
    public void display(@NotNull InventoryComponent inventoryComponent, int i, int i1, int i2, int i3) {
        inventoryComponent.setItem(items.get(0).getItem(), 4,2);
        inventoryComponent.setItem(items.get(1).getItem(), 9, 2);
        inventoryComponent.setItem(items.get(2).getItem(), 1, 3);
        inventoryComponent.setItem(items.get(3).getItem(), 2, 3);
        inventoryComponent.setItem(items.get(4).getItem(), 3, 3);
        inventoryComponent.setItem(items.get(5).getItem(), 4, 3);
        inventoryComponent.setItem(items.get(6).getItem(), 7, 3);
        inventoryComponent.setItem(items.get(7).getItem(), 4, 4);
        inventoryComponent.setItem(items.get(8).getItem(), 7, 5);
        inventoryComponent.setItem(items.get(9).getItem(), 9, 5);
    }

    @Override
    public boolean click(@NotNull Gui gui, @NotNull InventoryComponent inventoryComponent, @NotNull InventoryClickEvent inventoryClickEvent, int i, int i1, int i2, int i3, int i4) {
        return false;
    }

    @Override
    public Collection<GuiItem> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public Collection<Pane> getPanes() {
        return new HashSet<>();
    }

    @Override
    public void clear() {
        items.clear();
    }
}
