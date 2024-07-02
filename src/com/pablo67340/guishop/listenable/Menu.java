package com.pablo67340.guishop.listenable;


import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.*;
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
    public ChestGui GUI;

    private Boolean hasClicked = false;

    private MenuItem menuItem;

    private ShopPane menuPage = new ShopPane(9, 6);

    private final Player player;

    int pageIndex = 0;

    public PaginatedPane currentPane;

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
        pageIndex = 0;
        if (GUIShop.getINSTANCE().getLoadedMenu() == null) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loading Menu from config.");
            menuItem = new MenuItem();
            ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages");
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loading items for Menu");

            if (config == null) {
                GUIShop.getINSTANCE().getLogUtil().log("Check menu.yml for Menu items. They were not found, or the menu.yml is incorrectly formatted.");
            } else {
                config.getKeys(false).stream().map(str -> {
                    MenuPage page = new MenuPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Reading Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        GUIShop.getINSTANCE().getLogUtil().debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        return Item.deserialize(section.getValues(true), Integer.parseInt(key), null);
                    }).forEachOrdered(item -> page.getItems().put(Integer.toString(item.getSlot()), item));
                    return page;
                }).forEachOrdered(page -> {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Adding page: " + "Page" + menuItem.getPages().size() + " to pages.");
                    menuItem.getPages().put("Page" + menuItem.getPages().size(), page);
                });
                GUIShop.getINSTANCE().getLogUtil().debugLog("Loaded Menu cached");
                GUIShop.getINSTANCE().setLoadedMenu(menuItem);
                if (!preLoad) {
                    loadMenu();
                }
            }
        } else {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loading Menu from cache.");
            menuItem = GUIShop.getINSTANCE().getLoadedMenu();
            menuItem.determineHighestSlots();
            loadMenu();
        }
    }

    private void loadMenu() {
        if (this.GUI == null || this.GUI.getItems().isEmpty()) {
            PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
            for (Map.Entry<String, MenuPage> entry : menuItem.getPages().entrySet()) {
                if (this.GUI == null) {
                    int rows = GUIShop.rowChart.getRowsFromHighestSlot(entry.getValue().getHighestSlot());

                    if (this.hasMultiplePages()) {
                        this.GUI = new ChestGui(rows,
                                ChatColor.translateAlternateColorCodes('&',
                                        Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                                                Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%", "1"))));
                    } else {
                        this.GUI = new ChestGui(rows,
                                ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "")));
                    }
                }

                menuPage = new ShopPane(9, 6);

                for (Item item : entry.getValue().getItems().values()) {
                    if (item.getItemType() == ItemType.BLANK) {
                        continue;
                    }
                    ItemStack itemStack = item.toItemStack(player, true);
                    GuiItem gItem = new GuiItem(itemStack);
                    menuPage.setItem(gItem, item.getSlot());
                }

                applyButtons(menuPage, pageIndex, menuItem.getPages().size());
                pane.addPane(pageIndex, menuPage);
                pageIndex += 1;
            }

            GUI.addPane(pane);
            this.currentPane = pane;
        }
    }

    private void applyButtons(ShopPane page, int pageIndex, int maxPages) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("Applying buttons with page index: " + pageIndex + " max pages: " + maxPages);

        int nextSlot = calculateSlot(Config.getButtonConfig().getForwardSlot(), GUI.getRows() * 9) - 1;
        int prevSlot = calculateSlot(Config.getButtonConfig().getBackwardSlot(), GUI.getRows() * 9) - 1;
        int backSlot = calculateSlot(Config.getButtonConfig().getBackSlot(), GUI.getRows() * 9) - 1;

        if (pageIndex < (maxPages - 1)) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding forward button");
            page.setItem(new GuiItem(Config.getButtonConfig().forwardButton.toItemStack(player, true)), nextSlot);
        }

        if (pageIndex > 0) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding backward button");
            page.setItem(new GuiItem(Config.getButtonConfig().backwardButton.toItemStack(player, true)), prevSlot);
        }

        if (!Config.isDisableBackButton()) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding back button");
            ItemStack backButtonItem = Config.getButtonConfig().backButton.toItemStack(player, true);

            GuiItem item = new GuiItem(backButtonItem);

            page.setItem(item, backSlot);
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
            GUI.setOnTopClick(this::onShopClick);
            GUI.setOnBottomClick((e) -> e.setCancelled(true));
        } else {
            GUI.setOnBottomClick(this::creatorPlayerInventoryClick);
            GUI.setOnTopClick(this::creatorTopInventoryClick);
            GUI.setOnClose(this::onClose);
        }
        GUI.setOnGlobalClick(this::onGlobalClick);
        GUI.show(player);
    }
    
    private void onGlobalClick(InventoryClickEvent event){
        if (event.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop,
     * if so, run logic.
     */
    private void onShopClick(InventoryClickEvent event) {
        Player clickingPlayer = (Player) event.getWhoClicked();

        event.setCancelled(true);

        // Next Button
        GUIShop.getINSTANCE().getLogUtil().debugLog("Clicked: " + event.getSlot());
        if (event.getSlot() == (calculateSlot(Config.getButtonConfig().getForwardSlot(), GUI.getRows() * 9) - 1)) {
            handleForwardButton(clickingPlayer, event);
            // Backward Button
        } else if (event.getSlot() == (calculateSlot(Config.getButtonConfig().getBackwardSlot(), GUI.getRows() * 9) - 1)) {
            handleBackwardButton(player, event);
            // Back Button
        } else if (event.getSlot() == calculateSlot(Config.getButtonConfig().getBackSlot(), GUI.getRows() * 9) - 1 && !Config.isDisableBackButton()) {
            clickingPlayer.closeInventory();
        } else {
            handleItemClick(clickingPlayer, event);
        }
    }

    private void handleBackwardButton(Player player, InventoryClickEvent event) {
        if (currentPane.getPage() != 0) {
            hasClicked = true;

            if (hasMultiplePages()) {
                int currentPage = currentPane.getPage();
                GUI.setTitle(ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                                Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%",
                                        Integer.toString(currentPage)))));
            }

            GUIShop.getINSTANCE().getLogUtil().debugLog("Setting page " + currentPane.getPage() + " to not visible");
            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);

            GUIShop.getINSTANCE().getLogUtil().debugLog("Setting page to: " + (currentPane.getPage() - 1));
            currentPane.setPage(currentPane.getPage() - 1);

            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Setting Page: " + currentPane.getPage() + " to visible.");

            int rows = GUIShop.rowChart.getRowsFromHighestSlot(menuItem.getHighestPageSlot("Page" + (currentPane.getPage())));
            if (rows != 6) {
                rows += 1;
            }
            GUI.setRows(rows);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Resizing GUI to the next pane.");

            GUI.update();
        } else {
            handleItemClick(player, event);
        }
    }

    private void handleForwardButton(Player player, InventoryClickEvent event) {
        hasClicked = true;
        if (hasMultiplePages() && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {
            int currentPage = currentPane.getPage() + 2;
            GUI.setTitle(ChatColor.translateAlternateColorCodes('&',
                    Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                            Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%",
                                    Integer.toString(currentPage)))));

            GUIShop.getINSTANCE().getLogUtil().debugLog("Setting page " + currentPane.getPage() + " to not visible");
            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);

            GUIShop.getINSTANCE().getLogUtil().debugLog("Setting page to: " + (currentPane.getPage() + 1));
            currentPane.setPage(currentPane.getPage() + 1);

            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Setting Page: " + currentPane.getPage() + " to visible.");

            int rows = GUIShop.rowChart.getRowsFromHighestSlot(menuItem.getHighestPageSlot("Page" + (currentPane.getPage())));
            if (rows != 6) {
                rows += 1;
            }
            GUI.setRows(rows);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Resizing GUI to the next pane.");

            GUI.update();
        } else {
            handleItemClick(player, event);
        }
    }

    private void handleItemClick(Player clickingPlayer, InventoryClickEvent event) {
        // Everything else
        if (GUIShop.getINSTANCE().getLoadedMenu().getPages().containsKey("Page" + currentPane.getPage()) && GUIShop.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().containsKey(((Integer) event.getSlot()).toString())) {
            Item clickedItem = GUIShop.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().get(((Integer) event.getSlot()).toString());

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
        menuItem.getPages().get("Page" + currentPane.getPage()).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getConfigManager().getMenuConfig().createSection("Menu.pages.Page" + currentPane.getPage() + ".items");

        config.set(slot.toString(), null);

        try {
            GUIShop.getINSTANCE().getConfigManager().getMenuConfig().save(GUIShop.getINSTANCE().getConfigManager().getMenuFile());
        } catch (IOException ex) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    public void editMenuItem(ItemStack itemStack, Integer slot) {
        Item item = Item.parse(itemStack, slot, null);
        menuItem.getPages().get("Page" + currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getConfigManager().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getConfigManager().getMenuConfig().createSection("Menu.pages.Page" + currentPane.getPage() + ".items");

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
        if (e.getCurrentItem() != null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Cursor: " + e.getCursor());
            deleteMenuItem(e.getSlot());

            // When an item is dropped into the slot, it's not null. This is a new item.
        } else if (e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            e.setCancelled(true);
            String shopName = GUIShop.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().get(((Integer) e.getSlot()).toString()).getTargetShop();
            openShop((Player) e.getWhoClicked(), shopName);

        } else if (e.getCurrentItem() == null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            int slot = e.getInventory().firstEmpty();

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
}
