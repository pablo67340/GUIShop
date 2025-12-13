package com.pablo67340.guishop.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central listener that routes inventory events to the appropriate GUI instance.
 */
public class GuiListener implements Listener {

    private static GuiListener instance;
    private final Map<Inventory, GUIHolder> activeGuis = new ConcurrentHashMap<>();

    private GuiListener() {}

    public static GuiListener getInstance() {
        if (instance == null) {
            instance = new GuiListener();
        }
        return instance;
    }

    /**
     * Register a GUI to receive events.
     */
    public void register(Inventory inventory, GUIHolder holder) {
        activeGuis.put(inventory, holder);
    }

    /**
     * Unregister a GUI from receiving events.
     */
    public void unregister(Inventory inventory) {
        activeGuis.remove(inventory);
    }

    /**
     * Check if an inventory is managed by this listener.
     */
    public boolean isManaged(Inventory inventory) {
        return activeGuis.containsKey(inventory);
    }

    /**
     * Get the GUI holder for an inventory.
     */
    public GUIHolder getHolder(Inventory inventory) {
        return activeGuis.get(inventory);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        GUIHolder holder = activeGuis.get(topInventory);
        
        if (holder != null) {
            holder.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        GUIHolder holder = activeGuis.get(topInventory);
        
        if (holder != null) {
            holder.handleDrag(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        GUIHolder holder = activeGuis.get(topInventory);
        
        if (holder != null) {
            holder.handleClose(event);
        }
    }

    /**
     * Clear all registered GUIs. Called on plugin disable/reload.
     */
    public void clearAll() {
        activeGuis.clear();
    }
}

