package com.pablo67340.guishop.gui;

import com.pablo67340.guishop.GUIShop;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A GUI with pagination support.
 * Each page is stored as a map of slot -> ItemStack.
 */
public class PagedGui extends SimpleGui {

    @Getter
    private final List<Map<Integer, ItemStack>> pages = new ArrayList<>();

    @Getter
    @Setter
    private int currentPage = 0;

    @Getter
    @Setter
    private boolean dynamicRows = true;

    // Store the rows for each page
    private final List<Integer> pageRows = new ArrayList<>();

    public PagedGui(int rows, String title) {
        super(rows, title);
    }

    /**
     * Add a new empty page.
     *
     * @return The index of the new page
     */
    public int addPage() {
        pages.add(new HashMap<>());
        pageRows.add(getRows());
        return pages.size() - 1;
    }

    /**
     * Add a new page with content.
     *
     * @param content Map of slot -> ItemStack
     * @return The index of the new page
     */
    public int addPage(Map<Integer, ItemStack> content) {
        pages.add(new HashMap<>(content));
        pageRows.add(getRows());
        return pages.size() - 1;
    }

    /**
     * Set the number of rows for a specific page.
     */
    public void setPageRows(int pageIndex, int rows) {
        while (pageRows.size() <= pageIndex) {
            pageRows.add(6);
        }
        pageRows.set(pageIndex, Math.max(1, Math.min(6, rows)));
    }

    /**
     * Get the number of rows for a specific page.
     */
    public int getPageRows(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pageRows.size()) {
            return pageRows.get(pageIndex);
        }
        return getRows();
    }

    /**
     * Set an item on a specific page and slot.
     */
    public void setItem(int pageIndex, int slot, ItemStack item) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            if (slot >= 0 && slot < 54) {
                pages.get(pageIndex).put(slot, item);
                // If this is the current page, update the inventory
                if (pageIndex == currentPage) {
                    super.setItem(slot, item);
                }
            } else {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Warning: Attempted to set item at invalid slot " + slot + " on page " + pageIndex);
            }
        }
    }

    /**
     * Get an item from a specific page and slot.
     */
    public ItemStack getItem(int pageIndex, int slot) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            return pages.get(pageIndex).get(slot);
        }
        return null;
    }

    /**
     * Get the total number of pages.
     */
    public int getPageCount() {
        return pages.size();
    }

    /**
     * Check if there are multiple pages.
     */
    public boolean hasMultiplePages() {
        return pages.size() > 1;
    }

    /**
     * Check if there's a next page.
     */
    public boolean hasNextPage() {
        return currentPage < pages.size() - 1;
    }

    /**
     * Check if there's a previous page.
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Go to the next page.
     *
     * @return true if successful, false if already on last page
     */
    public boolean nextPage() {
        if (hasNextPage()) {
            currentPage++;
            loadCurrentPage();
            return true;
        }
        return false;
    }

    /**
     * Go to the previous page.
     *
     * @return true if successful, false if already on first page
     */
    public boolean previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            loadCurrentPage();
            return true;
        }
        return false;
    }

    /**
     * Go to a specific page.
     *
     * @param pageIndex The page index (0-based)
     * @return true if successful, false if page doesn't exist
     */
    public boolean goToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            currentPage = pageIndex;
            loadCurrentPage();
            return true;
        }
        return false;
    }

    /**
     * Load the current page's content into the inventory.
     */
    public void loadCurrentPage() {
        if (currentPage < 0 || currentPage >= pages.size()) {
            return;
        }

        Player player = getViewer();
        if (player == null || !player.isOnline()) {
            return;
        }

        // Check if rows need to change
        int targetRows = getPageRows(currentPage);
        boolean rowsChanged = targetRows != getRows();

        if (rowsChanged && dynamicRows) {
            // Rows changed - we MUST close and reopen
            setRows(targetRows);
            
            // Build the new contents
            ItemStack[] contents = new ItemStack[getInventory().getSize()];
            Map<Integer, ItemStack> pageContent = pages.get(currentPage);
            for (Map.Entry<Integer, ItemStack> entry : pageContent.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < contents.length) {
                    contents[slot] = entry.getValue();
                }
            }
            getInventory().setContents(contents);
            
            // Close and reopen since size changed
            setRefreshing(true);
            Bukkit.getScheduler().scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                if (player.isOnline()) {
                    player.closeInventory();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                        setRefreshing(false);
                        if (player.isOnline()) {
                            show(player);
                        }
                    }, 1L);
                } else {
                    setRefreshing(false);
                }
            }, 1L);
        } else {
            // Rows didn't change - update in place without closing
            GUIShop.getINSTANCE().getLogUtil().debugLog("Updating page in-place. Page " + currentPage + " has " + pages.get(currentPage).size() + " items");
            
            // Clear the inventory first
            getInventory().clear();
            
            // Set each item individually
            Map<Integer, ItemStack> pageContent = pages.get(currentPage);
            for (Map.Entry<Integer, ItemStack> entry : pageContent.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < getInventory().getSize()) {
                    getInventory().setItem(slot, entry.getValue());
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Set item at slot " + slot + ": " + (entry.getValue() != null ? entry.getValue().getType() : "null"));
                }
            }
            
            // Schedule the update for next tick to ensure it applies after event processing
            final Player finalPlayer = player;
            Bukkit.getScheduler().runTask(GUIShop.getINSTANCE(), () -> {
                if (finalPlayer.isOnline()) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Running scheduled inventory update");
                    finalPlayer.updateInventory();
                }
            });
        }
    }

    /**
     * Show this GUI to a player, loading the current page.
     */
    @Override
    public void show(Player player) {
        // Load current page content before showing
        if (!pages.isEmpty()) {
            // Set rows for current page
            int targetRows = getPageRows(currentPage);
            if (targetRows != getRows() && dynamicRows) {
                setRows(targetRows);
            }

            // Clear and load
            clear();
            Map<Integer, ItemStack> pageContent = pages.get(currentPage);
            for (Map.Entry<Integer, ItemStack> entry : pageContent.entrySet()) {
                int slot = entry.getKey();
                if (slot < getInventory().getSize()) {
                    super.setItem(slot, entry.getValue());
                }
            }
        }

        super.show(player);
    }

    /**
     * Clear a specific page's content.
     */
    public void clearPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            pages.get(pageIndex).clear();
            if (pageIndex == currentPage) {
                clear();
            }
        }
    }
}

