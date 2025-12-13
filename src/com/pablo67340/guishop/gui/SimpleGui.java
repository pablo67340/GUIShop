package com.pablo67340.guishop.gui;

import com.pablo67340.guishop.GUIShop;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * A simple GUI wrapper around Bukkit's inventory system.
 */
public class SimpleGui implements GUIHolder {

    @Getter
    private Inventory inventory;

    @Getter
    private String title;

    @Getter
    private int rows;

    @Getter
    private Player viewer;

    @Setter
    private Consumer<InventoryClickEvent> topClickHandler;

    @Setter
    private Consumer<InventoryClickEvent> bottomClickHandler;

    @Setter
    private Consumer<InventoryClickEvent> globalClickHandler;

    @Setter
    private Consumer<InventoryCloseEvent> closeHandler;

    @Setter
    private boolean allowBottomInventoryClick = false;

    @Setter
    private boolean allowTopInventoryClick = false;

    // Flag to prevent close handler from running during internal refresh
    @Getter
    @Setter
    private boolean refreshing = false;

    /**
     * Create a new SimpleGui.
     *
     * @param rows  Number of rows (1-6)
     * @param title The title of the inventory
     */
    public SimpleGui(int rows, String title) {
        this.rows = Math.max(1, Math.min(6, rows));
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        createInventory();
    }

    private void createInventory() {
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
    }

    /**
     * Set the number of rows. This recreates the inventory.
     * Items are preserved if the new size can accommodate them.
     * Note: Changing rows requires close/reopen since Minecraft can't resize open inventories.
     */
    public void setRows(int newRows) {
        newRows = Math.max(1, Math.min(6, newRows));
        if (newRows == this.rows) {
            return;
        }

        ItemStack[] oldContents = inventory.getContents();
        
        // Unregister old inventory
        GuiListener.getInstance().unregister(inventory);
        
        this.rows = newRows;
        createInventory();

        // Copy over items that fit in the new size
        int maxSlot = Math.min(oldContents.length, rows * 9);
        for (int i = 0; i < maxSlot; i++) {
            inventory.setItem(i, oldContents[i]);
        }
        
        // Re-register new inventory
        GuiListener.getInstance().register(inventory, this);
    }

    /**
     * Set the title. If the inventory is currently open, uses Paper's API to update
     * the title without recreating the inventory. Otherwise recreates the inventory.
     */
    public void setTitle(String newTitle) {
        this.title = ChatColor.translateAlternateColorCodes('&', newTitle);
        
        // If a player has this inventory open, try to update the title in place (Paper API)
        if (viewer != null && viewer.isOnline() && viewer.getOpenInventory().getTopInventory().equals(inventory)) {
            try {
                // Paper API: Update title without closing inventory
                viewer.getOpenInventory().setTitle(this.title);
                return;
            } catch (Exception e) {
                // Not on Paper or API not available, fall through to recreate
                GUIShop.getINSTANCE().getLogUtil().debugLog("Could not update title in-place, will recreate inventory");
            }
        }
        
        // Not open or Paper API failed - recreate the inventory
        ItemStack[] oldContents = inventory.getContents();
        
        // Unregister old inventory
        GuiListener.getInstance().unregister(inventory);
        
        createInventory();
        inventory.setContents(oldContents);
        
        // Re-register new inventory
        GuiListener.getInstance().register(inventory, this);
    }

    /**
     * Set an item at a specific slot.
     */
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /**
     * Get an item at a specific slot.
     */
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < inventory.getSize()) {
            return inventory.getItem(slot);
        }
        return null;
    }

    /**
     * Clear all items from the inventory.
     */
    public void clear() {
        inventory.clear();
    }

    /**
     * Show this GUI to a player.
     */
    public void show(Player player) {
        this.viewer = player;
        
        // Unregister old inventory if it exists
        GuiListener.getInstance().unregister(inventory);
        
        // Register the new inventory
        GuiListener.getInstance().register(inventory, this);
        
        player.openInventory(inventory);
    }

    /**
     * Update the viewer's inventory view.
     * Call this after making changes while the GUI is open.
     */
    public void update() {
        if (viewer != null && viewer.isOnline()) {
            // Check if viewer still has this inventory open
            if (viewer.getOpenInventory().getTopInventory().equals(inventory)) {
                viewer.updateInventory();
            }
        }
    }

    /**
     * Refresh the GUI by closing and reopening.
     * Use this when the inventory size changes.
     */
    public void refresh(Player player) {
        refreshing = true;
        
        // Close and reopen
        Bukkit.getScheduler().scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
            if (player.isOnline()) {
                player.closeInventory();
                Bukkit.getScheduler().scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                    refreshing = false;
                    if (player.isOnline()) {
                        show(player);
                    }
                }, 1L);
            } else {
                refreshing = false;
            }
        }, 1L);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Block off-hand swap
        if (event.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            event.setCancelled(true);
            return;
        }

        // Global click handler
        if (globalClickHandler != null) {
            globalClickHandler.accept(event);
        }

        // Determine which inventory was clicked
        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
            // Bottom inventory (player inventory)
            if (!allowBottomInventoryClick) {
                event.setCancelled(true);
            }
            if (bottomClickHandler != null) {
                bottomClickHandler.accept(event);
            }
        } else {
            // Top inventory (GUI)
            if (!allowTopInventoryClick) {
                event.setCancelled(true);
            }
            if (topClickHandler != null) {
                topClickHandler.accept(event);
            }
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        // Cancel drags that affect the top inventory, unless allowed
        if (!allowTopInventoryClick) {
            for (int slot : event.getRawSlots()) {
                if (slot < inventory.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Don't trigger close handler during internal refresh
        if (!refreshing && closeHandler != null) {
            closeHandler.accept(event);
        }
        // Unregister when closed (but not during refresh - we'll re-register)
        if (!refreshing) {
            GuiListener.getInstance().unregister(inventory);
        }
    }
}

