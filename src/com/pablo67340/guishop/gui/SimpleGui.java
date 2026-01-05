package com.pablo67340.guishop.gui;

import com.pablo67340.guishop.GUIShop;
import lombok.Getter;
import lombok.Setter;
import com.pablo67340.guishop.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
     * This preserves the cursor item during the update (InventoryFramework approach).
     */
    public void update() {
        if (viewer == null || !viewer.isOnline()) return;
        if (!viewer.getOpenInventory().getTopInventory().equals(inventory)) return;
        
        // Save cursor, update, restore cursor - this prevents cursor reset!
        ItemStack cursor = viewer.getItemOnCursor();
        viewer.setItemOnCursor(new ItemStack(Material.AIR));
        
        viewer.updateInventory();
        
        viewer.setItemOnCursor(cursor);
    }
    
    /**
     * Seamlessly update the GUI without resetting cursor position.
     * Works even when inventory size changes (reopens inventory but preserves cursor).
     * Based on InventoryFramework's approach.
     */
    public void updateSeamlessly() {
        if (viewer == null || !viewer.isOnline()) return;
        
        // Save cursor item before any changes
        ItemStack cursor = viewer.getItemOnCursor();
        viewer.setItemOnCursor(new ItemStack(Material.AIR));
        
        // Set refreshing flag to prevent close handler from running
        refreshing = true;
        
        // Close and reopen (necessary for size changes)
        viewer.closeInventory();
        
        // Re-register and show
        GuiListener.getInstance().register(inventory, this);
        viewer.openInventory(inventory);
        
        // Restore cursor on next tick (after inventory is fully open)
        SchedulerUtil.runAtEntity(viewer, () -> {
            refreshing = false;
            if (viewer != null && viewer.isOnline()) {
                viewer.setItemOnCursor(cursor);
            }
        });
    }
    
    /**
     * Update all slots via SET_SLOT packets if PacketEvents is available.
     * Falls back to updateSeamlessly() if PacketEvents is not present.
     * Does NOT reset cursor position.
     */
    public void updateViaPackets() {
        if (viewer == null || !viewer.isOnline()) return;
        if (!viewer.getOpenInventory().getTopInventory().equals(inventory)) return;
        
        // Check if PacketEvents is available
        if (!isPacketEventsAvailable()) {
            // Fallback to seamless update (save/restore cursor approach)
            update();
            return;
        }
        
        try {
            int windowId = getWindowId(viewer);
            GUIShop.getINSTANCE().getLogUtil().debugLog("GUI: Using window ID " + windowId + " for packet updates");
            
            if (windowId < 0) {
                update();
                return;
            }
            
            // Use PacketEvents to send SET_SLOT packets
            Object packetEvents = Class.forName("com.github.retrooper.packetevents.PacketEvents")
                .getMethod("getAPI").invoke(null);
            Object playerManager = packetEvents.getClass().getMethod("getPlayerManager").invoke(packetEvents);
            
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                
                // Convert to PacketEvents ItemStack
                Object packetItem = Class.forName("io.github.retrooper.packetevents.util.SpigotConversionUtil")
                    .getMethod("fromBukkitItemStack", ItemStack.class)
                    .invoke(null, item);
                
                // Create SET_SLOT wrapper
                Object setSlotPacket = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot")
                    .getConstructor(int.class, int.class, int.class, 
                        Class.forName("com.github.retrooper.packetevents.protocol.item.ItemStack"))
                    .newInstance(windowId, 0, slot, packetItem);
                
                // Send packet
                playerManager.getClass().getMethod("sendPacket", Object.class, Object.class)
                    .invoke(playerManager, viewer, setSlotPacket);
            }
            
            GUIShop.getINSTANCE().getLogUtil().debugLog("GUI: Updated " + inventory.getSize() + " slots via packets (windowId=" + windowId + ")");
            
        } catch (Exception e) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Packet update failed, using fallback: " + e.getMessage());
            update();
        }
    }
    
    /**
     * Check if PacketEvents is available on the server.
     */
    private boolean isPacketEventsAvailable() {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Get the window ID for the player's currently open inventory.
     * Uses reflection to access the internal container ID.
     */
    private int getWindowId(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            
            // Try different field names for different MC versions
            Object container = null;
            
            // 1.17+ uses containerMenu
            try {
                container = handle.getClass().getField("containerMenu").get(handle);
            } catch (NoSuchFieldException e) {
                // 1.16 and below use activeContainer
                try {
                    container = handle.getClass().getField("activeContainer").get(handle);
                } catch (NoSuchFieldException e2) {
                    // Try bV for some versions
                    container = handle.getClass().getField("bV").get(handle);
                }
            }
            
            if (container != null) {
                // Try containerId field
                try {
                    return (int) container.getClass().getField("containerId").get(container);
                } catch (NoSuchFieldException e) {
                    // Try windowId for older versions
                    try {
                        return (int) container.getClass().getField("windowId").get(container);
                    } catch (NoSuchFieldException e2) {
                        // Try j for some mapped versions
                        try {
                            return (int) container.getClass().getField("j").get(container);
                        } catch (NoSuchFieldException e3) {
                            // Last resort: look for a public int field
                            for (java.lang.reflect.Field f : container.getClass().getFields()) {
                                if (f.getType() == int.class && f.getName().length() <= 2) {
                                    int val = (int) f.get(container);
                                    if (val > 0 && val < 256) {
                                        return val;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Could not get window ID via reflection: " + e.getMessage());
        }
        
        // Fallback: For custom GUIs opened after player inventory, it's typically 1
        // Window 0 is always the player's inventory
        return 1;
    }

    /**
     * Refresh the GUI by closing and reopening.
     * Use this when the inventory size changes.
     */
    public void refresh(Player player) {
        refreshing = true;
        
        // Close and reopen
        SchedulerUtil.runAtEntityLater(player, () -> {
            if (player.isOnline()) {
                player.closeInventory();
                SchedulerUtil.runAtEntityLater(player, () -> {
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

