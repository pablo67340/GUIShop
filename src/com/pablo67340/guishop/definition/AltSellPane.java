package com.pablo67340.guishop.definition;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
    public void display(Gui gui, Inventory inventory, PlayerInventory playerInventory, int paneOffsetX, int paneOffsetY,
                        int maxLength, int maxHeight) {
        inventory.setItem(13, items.get(0).getItem());
        inventory.setItem(18, items.get(1).getItem());
        inventory.setItem(19, items.get(2).getItem());
        inventory.setItem(20, items.get(3).getItem());
        inventory.setItem(24, items.get(4).getItem());
        inventory.setItem(25, items.get(5).getItem());
        inventory.setItem(26, items.get(6).getItem());
        inventory.setItem(31, items.get(7).getItem());
        inventory.setItem(48, items.get(8).getItem());
        inventory.setItem(50, items.get(9).getItem());
    }

    @Override
    public boolean click(Gui gui, InventoryClickEvent event, int paneOffsetX, int paneOffsetY, int maxLength,
                         int maxHeight) {
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
