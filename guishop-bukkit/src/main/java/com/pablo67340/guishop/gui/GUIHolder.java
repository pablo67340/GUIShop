package com.pablo67340.guishop.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Interface for classes that can handle GUI events.
 * Extends InventoryHolder so GUIShop inventories can be properly identified.
 */
public interface GUIHolder extends InventoryHolder {

    /**
     * Handle a click event in this GUI.
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Handle a drag event in this GUI.
     */
    default void handleDrag(InventoryDragEvent event) {
        // Default: cancel all drags in managed GUIs
        event.setCancelled(true);
    }

    /**
     * Handle the GUI being closed.
     */
    default void handleClose(InventoryCloseEvent event) {
        // Default: do nothing
    }
}

