package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.MenuItem;
import com.pablo67340.guishop.definition.MenuPage;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.util.ConfigUtil;
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
    private Gui GUI;

    private Boolean hasClicked = false;

    private MenuItem menuItem;

    private ShopPane menuPage = new ShopPane(9, 6);

    private final Player player;

    Integer pageIndex = 0;

    private PaginatedPane currentPane;

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
        if (Main.getINSTANCE().getLoadedMenu() == null) {
            Main.debugLog("Loading Menu from Config.");
            menuItem = new MenuItem();
            ConfigurationSection config = Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages");
            Main.debugLog("Loading items for Menu");

            if (config == null) {
                Main.log("Check menu.yml for Menu Items. They were not found, or config is incorrectly formatted.");
            } else {
                config.getKeys(false).stream().map(str -> {
                    MenuPage page = new MenuPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    Main.debugLog("Reading Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        Main.debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        Item item = Item.deserialize(section.getValues(true), Integer.parseInt(key), null);
                        return item;
                    }).forEachOrdered(item -> {
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    Main.debugLog("Adding page: " + "Page" + Integer.toString(menuItem.getPages().size()) + " to pages.");
                    menuItem.getPages().put("Page" + Integer.toString(menuItem.getPages().size()), page);
                });
                Main.debugLog("Loaded Menu Cached");
                Main.getINSTANCE().setLoadedMenu(menuItem);
                if (!preLoad) {
                    loadMenu();
                }
            }
        } else {
            Main.debugLog("Loading Menu from Cache.");
            menuItem = (MenuItem) Main.getINSTANCE().getLoadedMenu();
            loadMenu();
        }
    }

    private void loadMenu() {
        if (this.GUI == null || this.GUI.getItems().isEmpty()) {
            if (this.hasMultiplePages()) {
                this.GUI = new Gui(Main.getINSTANCE(), 6,
                        ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", ConfigUtil.getMenuShopPageNumber().replace("{number}", "1"))));
            } else {
                int rows = (int) Math.ceil((double) menuItem.getPages().get("Page0").getItems().size() / 9);
                if (rows == 0) {
                    rows = 1;
                }
                this.GUI = new Gui(Main.getINSTANCE(), rows,
                        ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", "")));
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
                pageIndex += 1;

            }

            if (menuPages.isEmpty()) {
                menuPage = new ShopPane(9, 6);
                pane.addPane(pageIndex, menuPage);
            }

            GUI.addPane(pane);
            this.currentPane = pane;
        }

    }

    /**
     * Creates a named itemstack from a material and name.
     *
     * @param material the material
     * @param name the name with colour codes already applied
     * @return a named item
     */
    private ItemStack makeNamedItem(Material material, String name) {
        ItemStack is = new ItemStack(material);
        if (!name.isEmpty()) {
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName(name);
            is.setItemMeta(meta);
        }
        return is;
    }

    private void applyButtons(ShopPane page, int pageIndex, int maxPages) {
        if (pageIndex < (maxPages - 1)) {
            page.setItem(new GuiItem(makeNamedItem(Material.ARROW, ConfigUtil.getForwardPageButtonName())), 51);
        }
        Main.debugLog("Applying buttons with pageIndex: " + pageIndex + " maxPages: " + maxPages);
        if (pageIndex > 0) {
            Main.debugLog("Adding Back Button");
            page.setItem(new GuiItem(makeNamedItem(Material.ARROW, ConfigUtil.getBackwardPageButtonName())), 47);
        }
        if (!ConfigUtil.isDisableBackButton()) {

            ItemStack backButtonItem = new ItemStack(
                    Objects.requireNonNull(XMaterial.matchXMaterial(ConfigUtil.getBackButtonItem()).get().parseMaterial()));

            ItemMeta backButtonMeta = backButtonItem.getItemMeta();

            assert backButtonMeta != null;
            backButtonMeta.setDisplayName(ConfigUtil.getBackButtonText());

            backButtonItem.setItemMeta(backButtonMeta);

            GuiItem item = new GuiItem(backButtonItem);

            page.setItem(item, (this.GUI.getInventory().getSize()-1));
        }
    }

    /**
     * Opens the GUI in this {@link Menu}.
     *
     * @param player - The player the GUI will display to
     */
    public void open(Player player) {

        if (!player.hasPermission("guishop.use") && !player.isOp()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("no-permission"))));
            return;
        }

        if (Main.getINSTANCE().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("disabled-world"))));
            return;
        }

        loadItems(false);

        if (!Main.getCREATOR().contains(player.getName())) {
            GUI.setOnTopClick(this::onShopClick);
            GUI.setOnBottomClick((e) -> {
                e.setCancelled(true);
            });
        } else {
            GUI.setOnBottomClick(this::creatorPlayerInventoryClick);
            GUI.setOnTopClick(this::creatorTopInventoryClick);
            GUI.setOnClose(event -> onClose(event));
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

        if (e.getSlot() == Main.getINSTANCE().getMenuConfig().getInt("Menu.nextButtonSlot")) {
            hasClicked = true;
            if (hasMultiplePages() && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {

                Main.debugLog("Setting page " + currentPane.getPage() + " to not visible");
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                Main.debugLog("Setting page to: " + (currentPane.getPage() + 1));
                currentPane.setPage(currentPane.getPage() + 1);

                Integer currentPage = currentPane.getPage() + 1;
                GUI.setTitle(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", ConfigUtil.getMenuShopPageNumber().replace("{number}", currentPage.toString()))));

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                Main.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");
                GUI.update();
            }
            // Backward Button
        } else if (e.getSlot() == Main.getINSTANCE().getMenuConfig().getInt("Menu.backButtonSlot")) {
            if (currentPane.getPage() != 0) {
                hasClicked = true;

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                currentPane.setPage(currentPane.getPage() - 1);

                if (hasMultiplePages()) {
                    Integer currentPage = currentPane.getPage() + 1;
                    GUI.setTitle(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", ConfigUtil.getMenuShopPageNumber().replace("{number}", currentPage.toString()))));
                }

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUI.update();
            }
            // Back Button
        } else if (e.getSlot() == (this.GUI.getInventory().getSize()-1) && !ConfigUtil.isDisableBackButton()) {
            pl.closeInventory();
        } else {
            if (Main.getINSTANCE().getLoadedMenu().getPages().containsKey("Page" + currentPane.getPage()) && Main.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().containsKey(((Integer) e.getSlot()).toString())) {
                String shopName = Main.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().get(((Integer) e.getSlot()).toString()).getTargetShop();
                if (pl.hasPermission("guishop.shop." + shopName)) {
                    openShop(pl, shopName);
                } else {
                    pl.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou do not have permission to use this shop."));
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
            openShop.open(player);
        } else {
            Main.log("Error: Target shop of clicked item not specified. Please add target-shop to specific item in menu.yml to fix this.");
        }
    }

    private void deleteMenuItem(Integer slot) {
        menuItem.getPages().get("Page" + currentPane.getPage()).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items") != null
                ? Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items")
                : Main.getINSTANCE().getMenuConfig().createSection("Menu.pages.Page" + currentPane.getPage() + ".items");
        config.set(slot.toString(), null);
        try {
            Main.getINSTANCE().getMenuConfig().save(Main.getINSTANCE().getMenuf());
        } catch (IOException ex) {
            Main.debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    public void editMenuItem(ItemStack itemStack, Integer slot) {
        Item item = Item.parse(itemStack, slot, null);
        menuItem.getPages().get("Page" + currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items") != null
                ? Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages.Page" + currentPane.getPage() + ".items")
                : Main.getINSTANCE().getMenuConfig().createSection("Menu.pages.Page" + currentPane.getPage() + ".items");

        config.set(slot.toString(), item.serialize());
        Main.debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + slot);
        try {
            Main.getINSTANCE().getMenuConfig().save(Main.getINSTANCE().getMenuf());
        } catch (IOException ex) {
            Main.debugLog("Error saving Shops: " + ex.getMessage());
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
            scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(slot);
                if (item != null) {
                    Main.debugLog("new Item: " + item.getType());
                    editMenuItem(item, slot);
                }
            }, 5L);
        }
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() != null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            Main.debugLog("Cursor: " + e.getCursor());
            deleteMenuItem(e.getSlot());

            // When an item is dropped into the slot, it's not null. This is a new item.
        } else if (e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            e.setCancelled(true);
            String shopName = Main.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().get(((Integer) e.getSlot()).toString()).getTargetShop();
            openShop((Player) e.getWhoClicked(), shopName);

        } else if (e.getCurrentItem() == null && e.getClick() != ClickType.SHIFT_RIGHT && e.getClick() != ClickType.SHIFT_LEFT) {
            int slot = e.getInventory().firstEmpty();

            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(slot);
                if (item != null) {
                    Main.debugLog("new Item: " + item.getType());
                    editMenuItem(item, slot);
                }
            }, 5L);
        }
    }

    private void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (Main.getCREATOR().contains(p.getName()) && !hasClicked) {
            Main.getCREATOR().remove(p.getName());
        }
    }
}
