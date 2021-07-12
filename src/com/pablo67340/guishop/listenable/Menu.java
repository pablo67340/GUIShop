package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.MenuItem;
import com.pablo67340.guishop.definition.MenuPage;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.config.Config;
import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitScheduler;

public final class Menu {

    /**
     * The GUI that is projected onto the screen when a {@link Player} opens the
     * {@link Menu}.
     */
    public Gui GUI;

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
     * @key The index on the {@link Menu} that this shop is located at.
     * @param player The player using this Menu.
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
            GUIShop.debugLog("Loading Menu from config.");
            menuItem = new MenuItem();
            ConfigurationSection config = GUIShop.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages");
            GUIShop.debugLog("Loading items for Menu");

            if (config == null) {
                GUIShop.log("Check menu.yml for Menu items. They were not found, or the menu.yml is incorrectly formatted.");
            } else {
                config.getKeys(false).stream().map(str -> {
                    MenuPage page = new MenuPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    GUIShop.debugLog("Reading Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        GUIShop.debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        return Item.deserialize(section.getValues(true), Integer.parseInt(key), null);
                    }).forEachOrdered(item -> {
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    GUIShop.debugLog("Adding page: " + "Page" + menuItem.getPages().size() + " to pages.");
                    menuItem.getPages().put("Page" + menuItem.getPages().size(), page);
                });
                GUIShop.debugLog("Loaded Menu cached");
                GUIShop.getINSTANCE().setLoadedMenu(menuItem);
                if (!preLoad) {
                    loadMenu();
                }
            }
        } else {
            GUIShop.debugLog("Loading Menu from cache.");
            menuItem = GUIShop.getINSTANCE().getLoadedMenu();
            loadMenu();
        }
    }

    private void loadMenu() {
        if (this.GUI == null || this.GUI.getItems().isEmpty()) {
            if (this.hasMultiplePages()) {
                this.GUI = new Gui(GUIShop.getINSTANCE(), 6,
                        ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getMenuTitle().replace("%page-number%", Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%", "1"))));
            } else {
                int rows = (int) Math.ceil((double) menuItem.getPages().get("Page0").getItems().size() / 9);
                if (rows == 0) {
                    rows = 1;
                }
                this.GUI = new Gui(GUIShop.getINSTANCE(), rows,
                        ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "")));
            }

            PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
            Collection<MenuPage> menuPages = menuItem.getPages().values();
            for (MenuPage page : menuPages) {
                menuPage = new ShopPane(9, 6);
                for (Item item : page.getItems().values()) {
                    GuiItem gItem = new GuiItem(item.toItemStack(player, true));
                    menuPage.setItem(gItem, item.getSlot());
                }

                applyButtons(menuPage, pageIndex, menuPages.size());
                pane.addPane(pageIndex, menuPage);
                pageIndex++;
            }

            if (menuPages.isEmpty()) {
                menuPage = new ShopPane(9, 6);
                pane.addPane(pageIndex, menuPage);
            }

            GUI.addPane(pane);
            this.currentPane = pane;
        }

    }

    private void applyButtons(ShopPane page, int pageIndex, int maxPages) {
        if (pageIndex < (maxPages - 1)) {
            page.setItem(new GuiItem(Config.getButtonConfig().forwardButton.toItemStack(player, true)), this.GUI.getInventory().getSize() - 3);
        }
        GUIShop.debugLog("Applying buttons with pageIndex: " + pageIndex + " maxPages: " + maxPages);
        if (pageIndex > 0) {
            GUIShop.debugLog("Adding back button");
            page.setItem(new GuiItem(Config.getButtonConfig().backwardButton.toItemStack(player, true)), this.GUI.getInventory().getSize() - 7);
        }
        if (!Config.isDisableBackButton()) {
            ItemStack backButtonItem = Config.getButtonConfig().backButton.toItemStack(player, true);

            GuiItem item = new GuiItem(backButtonItem);

            page.setItem(item, (this.GUI.getInventory().getSize() - 1));
        }
    }

    /**
     * Opens the GUI in this {@link Menu}.
     *
     * @param player The player the GUI will display to
     */
    public void open(Player player) {
        if (!player.hasPermission("guishop.use") && !player.isOp()) {
            GUIShop.sendPrefix(player, "no-permission");
            return;
        }

        if (GUIShop.getINSTANCE().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
            GUIShop.sendPrefix(player, "disabled-world");
            return;
        }

        loadItems(false);

        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            GUI.setOnTopClick(this::onShopClick);
            GUI.setOnBottomClick((e) -> {
                e.setCancelled(true);
            });
        } else {
            GUI.setOnBottomClick(this::creatorPlayerInventoryClick);
            GUI.setOnTopClick(this::creatorTopInventoryClick);
            GUI.setOnClose(this::onClose);
        }
        GUI.show(player);
    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop,
     * if so, run logic.
     */
    private void onShopClick(InventoryClickEvent e) {
        Player pl = (Player) e.getWhoClicked();

        e.setCancelled(true);

        // Next Button
        if (e.getSlot() == this.GUI.getInventory().getSize() - 3) {
            hasClicked = true;
            if (hasMultiplePages() && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {

                GUIShop.debugLog("Setting page " + currentPane.getPage() + " to not visible");
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                GUIShop.debugLog("Setting page to: " + (currentPane.getPage() + 1));
                currentPane.setPage(currentPane.getPage() + 1);

                int currentPage = currentPane.getPage() + 1;
                GUI.setTitle(ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getMenuTitle().replace("{page-number}", Config.getTitlesConfig().getMenuShopPageNumber().replace("{number}", Integer.toString(currentPage)))));

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUIShop.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");
                GUI.update();
            }
            // Backward Button
        } else if (e.getSlot() == this.GUI.getInventory().getSize() - 7) {
            if (currentPane.getPage() != 0) {
                hasClicked = true;

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                currentPane.setPage(currentPane.getPage() - 1);

                if (hasMultiplePages()) {
                    int currentPage = currentPane.getPage() + 1;
                    GUI.setTitle(ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getMenuTitle().replace("{page-number}", Config.getTitlesConfig().getMenuShopPageNumber().replace("{number}", Integer.toString(currentPage)))));
                }

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUI.update();
            }
            // Back Button
        } else if (e.getSlot() == (this.GUI.getInventory().getSize() - 1) && !Config.isDisableBackButton()) {
            pl.closeInventory();
        } else {
            // Everything else
            if (GUIShop.getINSTANCE().getLoadedMenu().getPages().containsKey("Page" + currentPane.getPage()) && GUIShop.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().containsKey(((Integer) e.getSlot()).toString())) {
                Item clickedItem = GUIShop.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().get(((Integer) e.getSlot()).toString());

                if (clickedItem.hasTargetShop()) {
                    String shopName = clickedItem.getTargetShop();
                    if (pl.hasPermission("guishop.shop." + shopName)) {
                        if (!clickedItem.isResolveFailed()) {
                            openShop(pl, shopName);
                        } else {
                            GUIShop.sendPrefix(pl, "open-shop-error", clickedItem.getResolveReason());
                         }
                    } else {
                        GUIShop.sendPrefix(pl, "no-permission");
                    }
                }
            }
        }
    }

    public Boolean hasMultiplePages() {
        return this.menuItem.getPages().size() > 1;
    }

    public void openShop(Player player, String shop) {
        /*
         * The currently open shop associated with this Menu instance.
         */
        if (shop != null) {
            Shop openShop = new Shop(player, shop, this);
            openShop.loadItems(false);
            if (!openShop.open(player)) {
                GUIShop.log("Error: Target shop of clicked item not existent. Please edit target-shop to the item in menu.yml to fix this.");
            }
        } else {
            GUIShop.log("Error: Target shop of clicked item not specified. Please add target-shop to the item in menu.yml to fix this.");
        }
    }

    private void deleteMenuItem(Integer slot) {
        menuItem.getPages().get("Page" + currentPane.getPage()).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = GUIShop.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getMenuConfig().createSection("Menu.pages.Page" + currentPane.getPage() + ".items");
        config.set(slot.toString(), null);
        try {
            GUIShop.getINSTANCE().getMenuConfig().save(GUIShop.getINSTANCE().getMenuf());
        } catch (IOException ex) {
            GUIShop.debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    public void editMenuItem(ItemStack itemStack, Integer slot) {
        Item item = Item.parse(itemStack, slot, null);
        menuItem.getPages().get("Page" + currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = GUIShop.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getMenuConfig().createSection("Menu.pages.Page" + currentPane.getPage() + ".items");

        config.set(slot.toString(), item.serialize());
        GUIShop.debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + slot);
        try {
            GUIShop.getINSTANCE().getMenuConfig().save(GUIShop.getINSTANCE().getMenuf());
        } catch (IOException ex) {
            GUIShop.debugLog("Error saving Shops: " + ex.getMessage());
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
                    GUIShop.debugLog("new Item: " + item.getType());
                    editMenuItem(item, slot);
                }
            }, 5L);
        }
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() != null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            GUIShop.debugLog("Cursor: " + e.getCursor());
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
                    GUIShop.debugLog("New item: " + item.getType());
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
