package com.pablo67340.guishop.listenable;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.*;
import com.pablo67340.guishop.gui.PagedGui;
import com.pablo67340.guishop.util.NameUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.util.Map;

public final class Menu {

    /**
     * The GUI that is projected onto the screen when a {@link Player} opens the
     * {@link Menu}.
     */
    public PagedGui GUI;

    private Boolean hasClicked = false;

    private MenuItem menuItem;

    private final Player player;

    /**
     * A {@link Map} that will store our {@link Shop}s when the server first
     * starts.
     *
     * @param player The player using this Menu.
     * @key The index on the {@link Menu} that this shop is located at.
     * @value The shop.
     */
    public Menu(Player player) {
        this.player = player;
    }

    public Menu() {
        this.player = null;
    }

    /**
     * Load the specified shop
     *
     * @param preLoad true/false if the items are preloading, or in production.
     */
    public void loadItems(Boolean preLoad) {
        if (GUIShop.getINSTANCE().getLoadedMenu() == null) {
            loadMenuFromConfig(preLoad);
        } else {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loading Menu from cache.");
            menuItem = GUIShop.getINSTANCE().getLoadedMenu();
            menuItem.determineHighestSlots();
            loadMenu();
        }
    }

    /**
     * Load menu data from the configuration file with comprehensive error handling.
     */
    private void loadMenuFromConfig(Boolean preLoad) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("Loading Menu from config.");
        menuItem = new MenuItem();

        // Check if menu.yml has the Menu section
        ConfigurationSection menuSection = GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu");
        if (menuSection == null) {
            logMenuError("menu.yml is missing the root 'Menu' section. Check your file structure and indentation.");
            logMenuError("Expected format:\n  Menu:\n    pages:\n      Page0:\n        items:\n          '0':\n            id: DIAMOND\n            target-shop: Blocks");
            return;
        }

        ConfigurationSection pagesConfig = menuSection.getConfigurationSection("pages");
        if (pagesConfig == null) {
            logMenuError("menu.yml is missing 'Menu.pages' section. Check your indentation.");
            logMenuError("Expected format:\n  Menu:\n    pages:\n      Page0:\n        items:");
            return;
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Loading items for Menu");
        int pageIndex = 0;
        int totalItems = 0;

        for (String pageKey : pagesConfig.getKeys(false)) {
            MenuPage page = new MenuPage();
            ConfigurationSection itemsSection = pagesConfig.getConfigurationSection(pageKey + ".items");

            if (itemsSection == null) {
                logMenuError("Menu > " + pageKey + " is missing 'items' section. Check your indentation.");
                logMenuError("Expected format:\n  " + pageKey + ":\n    items:\n      '0':\n        id: DIAMOND");
                pageIndex++;
                continue;
            }

            int itemsLoaded = 0;
            for (String slotKey : itemsSection.getKeys(false)) {
                // Validate slot is a number
                int slot;
                try {
                    slot = Integer.parseInt(slotKey);
                } catch (NumberFormatException e) {
                    logMenuError("Menu > " + pageKey + " > Item slot '" + slotKey + "' is not a valid number. Slot keys must be numbers (e.g., '0', '1', '2').");
                    continue;
                }

                ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                if (itemSection == null) {
                    logMenuError("Menu > " + pageKey + " > Slot '" + slotKey + "' has invalid format. Check your indentation - each slot needs proper YAML structure.");
                    continue;
                }

                // Check for required 'id' field
                if (!itemSection.contains("id")) {
                    logMenuError("Menu > " + pageKey + " > Slot '" + slotKey + "' is missing required 'id' field. Every item needs an 'id' property.");
                    continue;
                }

                Item item;
                try {
                    item = Item.deserialize(itemSection.getValues(true), slot, null);
                } catch (Exception e) {
                    logMenuError("Menu > " + pageKey + " > Slot '" + slotKey + "' failed to load: " + e.getMessage());
                    continue;
                }

                if (item == null) {
                    logMenuError("Menu > " + pageKey + " > Slot '" + slotKey + "' returned null item. Check the item configuration.");
                    continue;
                }

                page.getItems().put(Integer.toString(item.getSlot()), item);
                itemsLoaded++;
            }

            menuItem.getPages().put("Page" + pageIndex, page);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loaded " + pageKey + " with " + itemsLoaded + " items.");
            totalItems += itemsLoaded;
            pageIndex++;
        }

        if (menuItem.getPages().isEmpty()) {
            logMenuError("Menu loaded with 0 pages. Your menu.yml may be empty or incorrectly formatted.");
            return;
        }

        if (totalItems == 0) {
            logMenuError("Menu loaded with 0 items across all pages. Check your menu.yml configuration.");
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Menu loaded successfully with " + menuItem.getPages().size() + " page(s) and " + totalItems + " item(s).");
        GUIShop.getINSTANCE().setLoadedMenu(menuItem);

        if (!preLoad) {
            loadMenu();
        }
    }

    /**
     * Log a menu configuration error with consistent formatting.
     */
    private void logMenuError(String message) {
        GUIShop.getINSTANCE().getLogUtil().log("[Menu Config Error] " + message);
    }

    private void loadMenu() {
        if (this.GUI == null) {
            // Get initial rows from first page
            int initialRows = 6;
            if (!menuItem.getPages().isEmpty()) {
                MenuPage firstPage = menuItem.getPages().values().iterator().next();
                initialRows = GUIShop.rowChart.getRowsFromHighestSlot(firstPage.getHighestSlot());
                if (hasMultiplePages() && initialRows != 6) {
                    initialRows += 1; // Add row for navigation buttons
                }
            }

            String title;
            if (hasMultiplePages()) {
                title = Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                        Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%", "1"));
            } else {
                title = Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "");
            }

            this.GUI = new PagedGui(initialRows, title);
            this.GUI.setDynamicRows(true);

            int pageIndex = 0;
            for (Map.Entry<String, MenuPage> entry : menuItem.getPages().entrySet()) {
                // Add a new page
                GUI.addPage();

                // Calculate rows for this page
                int rows = GUIShop.rowChart.getRowsFromHighestSlot(entry.getValue().getHighestSlot());
                if (hasMultiplePages() && rows != 6) {
                    rows += 1; // Add row for navigation buttons
                }
                GUI.setPageRows(pageIndex, rows);

                // Add items to the page
                for (Item item : entry.getValue().getItems().values()) {
                    if (item.getItemType() == ItemType.BLANK) {
                        continue;
                    }
                    ItemStack itemStack = item.toItemStack(player, true);
                    GUI.setItem(pageIndex, item.getSlot(), itemStack);
                }

                // Apply navigation buttons
                applyButtons(pageIndex, menuItem.getPages().size(), rows);
                pageIndex++;
            }
        }
    }

    private void applyButtons(int pageIndex, int maxPages, int rows) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("Applying buttons with page index: " + pageIndex + " max pages: " + maxPages);

        int inventorySize = rows * 9;
        int nextSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getForwardSlot(), inventorySize) - 1);
        int prevSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackwardSlot(), inventorySize) - 1);
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);

        if (pageIndex < (maxPages - 1)) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding forward button at slot " + nextSlot);
            GUI.setItem(pageIndex, nextSlot, Config.getButtonConfig().forwardButton.toItemStack(player, true));
        }

        if (pageIndex > 0) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding backward button at slot " + prevSlot);
            GUI.setItem(pageIndex, prevSlot, Config.getButtonConfig().backwardButton.toItemStack(player, true));
        }

        if (!Config.isDisableBackButton()) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding back button at slot " + backSlot);
            ItemStack backButtonItem = Config.getButtonConfig().backButton.toItemStack(player, true);
            GUI.setItem(pageIndex, backSlot, backButtonItem);
        }
    }

    private int calculateSlot(int setSlot, int inventorySize) {
        if (setSlot > inventorySize) {
            if (setSlot - 9 < 1) {
                return setSlot;
            }
            return calculateSlot(setSlot - 9, inventorySize);
        } else {
            return setSlot;
        }
    }

    /**
     * Opens the GUI in this {@link Menu}.
     *
     * @param player The player the GUI will display to
     */
    public void open(Player player) {
        if (!GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.use") && !player.isOp()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-permission");
            return;
        }

        if (GUIShop.getINSTANCE().getConfigManager().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "disabled-world");
            return;
        }

        loadItems(false);

        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            GUI.setTopClickHandler(this::onShopClick);
            GUI.setBottomClickHandler((e) -> e.setCancelled(true));
            // Explicitly reset allow flags to prevent item theft when GUI is reused
            GUI.setAllowTopInventoryClick(false);
            GUI.setAllowBottomInventoryClick(false);
        } else {
            GUI.setBottomClickHandler(this::creatorPlayerInventoryClick);
            GUI.setTopClickHandler(this::creatorTopInventoryClick);
            GUI.setCloseHandler(this::onClose);
            GUI.setAllowTopInventoryClick(true);
            GUI.setAllowBottomInventoryClick(true);
        }
        GUI.show(player);
    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop,
     * if so, run logic.
     */
    private void onShopClick(InventoryClickEvent event) {
        Player clickingPlayer = (Player) event.getWhoClicked();

        event.setCancelled(true);

        int inventorySize = GUI.getRows() * 9;
        int nextSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getForwardSlot(), inventorySize) - 1);
        int prevSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackwardSlot(), inventorySize) - 1);
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);

        // Next Button
        GUIShop.getINSTANCE().getLogUtil().debugLog("Clicked: " + event.getSlot());
        if (event.getSlot() == nextSlot) {
            handleForwardButton(clickingPlayer);
            // Backward Button
        } else if (event.getSlot() == prevSlot) {
            handleBackwardButton(clickingPlayer);
            // Back Button
        } else if (event.getSlot() == backSlot && !Config.isDisableBackButton()) {
            clickingPlayer.closeInventory();
        } else {
            handleItemClick(clickingPlayer, event);
        }
    }

    private void handleBackwardButton(Player player) {
        if (GUI.hasPreviousPage()) {
            hasClicked = true;

            GUIShop.getINSTANCE().getLogUtil().debugLog("Going to previous page from " + GUI.getCurrentPage());

            // Update title before page change
            if (hasMultiplePages()) {
                int newPage = GUI.getCurrentPage(); // Will be the page number after going back (0-indexed becomes display number)
                GUI.setTitle(ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                                Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%",
                                        Integer.toString(newPage)))));
            }

            GUI.previousPage();
            GUIShop.getINSTANCE().getLogUtil().debugLog("Now on page " + GUI.getCurrentPage());
        }
    }

    private void handleForwardButton(Player player) {
        if (GUI.hasNextPage()) {
            hasClicked = true;

            GUIShop.getINSTANCE().getLogUtil().debugLog("Going to next page from " + GUI.getCurrentPage());

            // Update title before page change
            if (hasMultiplePages()) {
                int newPage = GUI.getCurrentPage() + 2; // Current is 0-indexed, going forward, so +2 for display
                GUI.setTitle(ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                                Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%",
                                        Integer.toString(newPage)))));
            }

            GUI.nextPage();
            GUIShop.getINSTANCE().getLogUtil().debugLog("Now on page " + GUI.getCurrentPage());
        }
    }

    private void handleItemClick(Player clickingPlayer, InventoryClickEvent event) {
        // Everything else
        String pageKey = "Page" + GUI.getCurrentPage();
        if (GUIShop.getINSTANCE().getLoadedMenu().getPages().containsKey(pageKey) 
                && GUIShop.getINSTANCE().getLoadedMenu().getPages().get(pageKey).getItems().containsKey(((Integer) event.getSlot()).toString())) {
            Item clickedItem = GUIShop.getINSTANCE().getLoadedMenu().getPages().get(pageKey).getItems().get(((Integer) event.getSlot()).toString());

            if (clickedItem.hasTargetShop()) {
                String shopName = clickedItem.getTargetShop();
                if (GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(clickingPlayer, "guishop.shop." + shopName.toLowerCase()) || GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(clickingPlayer, "guishop.shop.*")) {
                    if (!clickedItem.isResolveFailed()) {
                        openShop(clickingPlayer, shopName);
                    } else {
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(clickingPlayer, "open-shop-error", clickedItem.getResolveReason());
                    }
                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(clickingPlayer, "no-permission");
                }
            }
        }
    }

    public boolean hasMultiplePages() {
        return this.menuItem.getPages().size() > 1;
    }

    public void openShop(Player player, String shop) {
        /*
         * The currently open shop associated with this Menu instance.
         */

        String nearestShop = NameUtil.nearestShop(shop);

        if (nearestShop != null) {
            // Set hasClicked to prevent any close handlers from interfering
            hasClicked = true;
            
            Shop openShop = new Shop(player, nearestShop, this);
            openShop.loadItems(false);

            if (!openShop.open(player)) {
                GUIShop.getINSTANCE().getLogUtil().log("Error: Target shop of clicked item not existent. Please edit target-shop to the item in menu.yml to fix this.");
            }
        } else {
            GUIShop.getINSTANCE().getLogUtil().log("Error: Target shop of clicked item not specified. Please add target-shop to the item in menu.yml to fix this.");
        }
    }

    private void deleteMenuItem(Integer slot) {
        String pageKey = "Page" + GUI.getCurrentPage();
        menuItem.getPages().get(pageKey).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages." + pageKey + ".items") != null
                ? GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages." + pageKey + ".items")
                : GUIShop.getINSTANCE().getConfigManager().getMenuConfig().createSection("Menu.pages." + pageKey + ".items");

        config.set(slot.toString(), null);

        try {
            GUIShop.getINSTANCE().getConfigManager().getMenuConfig().save(GUIShop.getINSTANCE().getConfigManager().getMenuFile());
        } catch (IOException ex) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    public void editMenuItem(ItemStack itemStack, Integer slot) {
        String pageKey = "Page" + GUI.getCurrentPage();
        Item item = Item.parse(itemStack, slot, null);
        menuItem.getPages().get(pageKey).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages." + pageKey + ".items") != null
                ? GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages." + pageKey + ".items")
                : GUIShop.getINSTANCE().getConfigManager().getMenuConfig().createSection("Menu.pages." + pageKey + ".items");

        config.set(slot.toString(), item.serialize());

        GUIShop.getINSTANCE().getLogUtil().debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + slot);
        try {
            GUIShop.getINSTANCE().getConfigManager().getMenuConfig().save(GUIShop.getINSTANCE().getConfigManager().getMenuFile());
        } catch (IOException ex) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    private void creatorPlayerInventoryClick(InventoryClickEvent e) {
        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {

            // Since shift clicking moves items to the first available slot, we can assume
            // the item
            // will end up in this slot.
            int slot = e.getInventory().firstEmpty();

            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(slot);
                if (item != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("new Item: " + item.getType());
                    editMenuItem(item, slot);
                }
            }, 5L);
        }
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        String pageKey = "Page" + GUI.getCurrentPage();
        if (e.getCurrentItem() != null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Cursor: " + e.getCursor());
            deleteMenuItem(e.getSlot());

            // When an item is dropped into the slot, it's not null. This is a new item.
        } else if (e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            e.setCancelled(true);
            String shopName = GUIShop.getINSTANCE().getLoadedMenu().getPages().get(pageKey).getItems().get(((Integer) e.getSlot()).toString()).getTargetShop();
            openShop((Player) e.getWhoClicked(), shopName);

        } else if (e.getCurrentItem() == null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            int slot = e.getSlot();

            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(slot);
                if (item != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("New item: " + item.getType());
                    editMenuItem(item, slot);
                }
            }, 5L);
        }
    }

    private void onClose(InventoryCloseEvent event) {
        if (!hasClicked) {
            GUIShop.getCREATOR().remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Get the current page index (for compatibility with other classes).
     */
    public int getCurrentPage() {
        return GUI != null ? GUI.getCurrentPage() : 0;
    }
}
