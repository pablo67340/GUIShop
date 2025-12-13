package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.*;
import com.pablo67340.guishop.gui.PagedGui;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Shop {

    /**
     * The name of this {@link Shop}.
     */
    @Getter
    @Setter
    private String shop, title;

    /**
     * The GUI for this shop.
     */
    public PagedGui GUI;

    private final Menu menuInstance;

    private boolean hasClicked = false, clickOverride = false;

    private ShopItem shopItem;

    private final Player player;

    private Boolean shopMissing = false;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param player      The player using the shop.
     * @param shop        The name of the shop.
     * @param menuInstance The instance of the menu that opened this shop.
     */
    public Shop(Player player, String shop, Menu menuInstance) {
        this.shop = shop;
        this.menuInstance = menuInstance;
        this.player = player;
    }

    public Shop(String shop) {
        this.shop = shop;
        this.menuInstance = null;
        this.player = null;
    }

    /**
     * Load the specified shop
     *
     * @param preLoad True/False if the GUI should load after items are loaded.
     */
    public void loadItems(Boolean preLoad) {
        if (shop.equalsIgnoreCase("NONE")) {
            return;
        }

        if (!GUIShop.getINSTANCE().getLoadedShops().containsKey(shop)) {
            this.setTitle(GUIShop.getINSTANCE().getConfigManager().getShopConfig().getString(shop + ".title"));
            shopItem = new ShopItem();
            ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getShopConfig().getConfigurationSection(shop + ".pages");
            if (config == null) {
                GUIShop.getINSTANCE().getLogUtil().log("Check shops.yml for shop " + shop + ". It was not found.");
                if (this.player != null) {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "shop-not-found", shop);
                }
                shopMissing = true;
            } else {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Loading items for shop: " + shop);
                config.getKeys(false).stream().map(str -> {
                    ShopPage page = new ShopPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Reading Shop Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        GUIShop.getINSTANCE().getLogUtil().debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        return Item.deserialize(section.getValues(true), Integer.parseInt(key), shop);
                    }).forEachOrdered(item -> {
                        if (item.hasSellPrice()) {
                            List<Item> items = GUIShop.getINSTANCE().getITEMTABLE().get(item.getMaterial());
                            if (items == null) {
                                items = new ArrayList<>();
                            }
                            items.add(item);

                            if (item.hasPotion() && item.getPotionInfo().getSplash()) {
                                GUIShop.getINSTANCE().getLogUtil().debugLog("Making item: SPLASH_POTION sellable.");
                                GUIShop.getINSTANCE().getITEMTABLE().put(XMaterial.matchXMaterial("SPLASH_POTION").get().parseItem().getType().toString(), items);
                            } else {
                                try {
                                    GUIShop.getINSTANCE().getLogUtil().debugLog("Making item: " + item.getMaterial() + " sellable.");
                                    GUIShop.getINSTANCE().getITEMTABLE().put(XMaterial.matchXMaterial(item.getMaterial()).get().parseItem().getType().toString(), items);
                                } catch (Exception ex) {
                                    GUIShop.getINSTANCE().getLogUtil().log("Error adding item: " + item.getMaterial() + " to sellable list. Wrong item name or item does not exist for this server version.");
                                }
                            }
                        }
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Adding page: " + "Page" + shopItem.getPages().size() + " to pages.");
                    shopItem.getPages().put("Page" + shopItem.getPages().size(), page);
                });

                shopItem.determineHighestSlots();

                GUIShop.getINSTANCE().getLogUtil().debugLog("Shop items added to loaded shops");
                GUIShop.getINSTANCE().getLoadedShops().put(shop, shopItem);
                if (!preLoad) {
                    loadShop();
                }
            }
        } else {
            shopItem = (ShopItem) GUIShop.getINSTANCE().getLoadedShops().get(shop);
            this.setTitle(GUIShop.getINSTANCE().getConfigManager().getShopConfig().getString(shop + ".title"));
            //Re-Check for preload here in case they have multiple item's leading to one shop.
            if (!preLoad) {
                loadShop();
            }
        }
    }

    private void loadShop() {
        if (this.GUI == null) {
            // Get initial rows from first page
            int initialRows = 6;
            if (!shopItem.getPages().isEmpty()) {
                ShopPage firstPage = shopItem.getPages().values().iterator().next();
                initialRows = GUIShop.rowChart.getRowsFromHighestSlot(firstPage.getHighestSlot());
                if (hasMultiplePages() && initialRows != 6) {
                    initialRows += 1; // Add row for navigation buttons
                }
            }

            String guiTitle = ChatColor.translateAlternateColorCodes('&', 
                    Config.getTitlesConfig().getShopTitle().replace("%shopname%", title));

            this.GUI = new PagedGui(initialRows, guiTitle);
            this.GUI.setDynamicRows(true);

            int pageIndex = 0;
            for (Map.Entry<String, ShopPage> entry : shopItem.getPages().entrySet()) {
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
                    ItemStack itemStack = item.toItemStack(player, false);
                    GUI.setItem(pageIndex, item.getSlot(), itemStack);
                }

                // Apply navigation buttons
                applyButtons(pageIndex, shopItem.getPages().size(), rows);
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
            GUI.setItem(pageIndex, nextSlot, Config.getButtonConfig().forwardButton.toItemStack(player, false));
        }

        if (pageIndex > 0) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding backward button at slot " + prevSlot);
            GUI.setItem(pageIndex, prevSlot, Config.getButtonConfig().backwardButton.toItemStack(player, false));
        }

        if (!Config.isDisableBackButton()) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding back button at slot " + backSlot);
            ItemStack backButtonItem = Config.getButtonConfig().backButton.toItemStack(player, false);
            GUI.setItem(pageIndex, backSlot, backButtonItem);
        }
    }

    private int calculateSlot(int setSlot, int inventorySize) {
        if (setSlot > inventorySize) {
            return calculateSlot(setSlot - 9, inventorySize);
        } else {
            return setSlot;
        }
    }

    public boolean hasMultiplePages() {
        return this.shopItem.getPages().size() > 1;
    }

    /**
     * Open the player's shop
     *
     * @param player The player the shop will open for.
     * @return If the shop opened successfully
     */
    public boolean open(Player player) {
        if (this.isShopMissing() || shop.equalsIgnoreCase("NONE")) {
            return false;
        }

        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            GUI.setTopClickHandler(this::onShopClick);
            GUI.setBottomClickHandler((e) -> e.setCancelled(true));
        } else {
            GUI.setBottomClickHandler(this::creatorPlayerInventoryClick);
            GUI.setTopClickHandler(this::creatorTopInventoryClick);
            GUI.setAllowTopInventoryClick(true);
            GUI.setAllowBottomInventoryClick(true);
        }
        GUI.setCloseHandler(this::onClose);
        GUI.show(player);
        return true;
    }

    private void onShopClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        int inventorySize = GUI.getRows() * 9;
        int nextSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getForwardSlot(), inventorySize) - 1);
        int prevSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackwardSlot(), inventorySize) - 1);
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);

        // Forward Button
        GUIShop.getINSTANCE().getLogUtil().debugLog("Clicked: " + event.getSlot());
        if (event.getSlot() == nextSlot) {
            handleForwardButton();
            // Backward Button
        } else if (event.getSlot() == prevSlot) {
            handleBackwardButton();
            // Back Button
        } else if (event.getSlot() == backSlot && !Config.isDisableBackButton()) {
            if (menuInstance != null && !GUIShop.getCREATOR().contains(player.getUniqueId())) {
                menuInstance.open(player);
            }
        } else {
            handleItemClick(event);
        }
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
                    GUIShop.getINSTANCE().getLogUtil().debugLog("New item: " + item.getType());
                    editShopItem(item, slot);
                }
            }, 5L);
        }
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() != null || e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Cursor: " + e.getCursor());
            deleteShopItem(e.getSlot());

            // When an item is dropped into the slot, it's not null. This is a new item.
        } else {
            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(e.getSlot());
                if (item != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("New item: " + item.getType());
                    editShopItem(item, e.getSlot());
                }
            }, 5L);
        }
    }

    private void deleteShopItem(Integer slot) {
        String pageKey = "Page" + GUI.getCurrentPage();
        shopItem.getPages().get(pageKey).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getShopConfig().getConfigurationSection(shop + ".pages." + pageKey + ".items") != null
                ? GUIShop.getINSTANCE().getConfigManager().getShopConfig().getConfigurationSection(shop + ".pages." + pageKey + ".items")
                : GUIShop.getINSTANCE().getConfigManager().getShopConfig().createSection(shop + ".pages." + pageKey + ".items");
        config.set(slot.toString(), null);
        try {
            GUIShop.getINSTANCE().getConfigManager().getShopConfig().save(GUIShop.getINSTANCE().getConfigManager().getShopFile());
        } catch (IOException ex) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Error saving shops: " + ex.getMessage());
        }
    }

    public void editShopItem(ItemStack itemStack, Integer slot) {
        String pageKey = "Page" + GUI.getCurrentPage();
        Item item = Item.parse(itemStack, slot, shop);
        shopItem.getPages().get(pageKey).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = GUIShop.getINSTANCE().getConfigManager().getShopConfig().getConfigurationSection(shop + ".pages." + pageKey + ".items") != null
                ? GUIShop.getINSTANCE().getConfigManager().getShopConfig().getConfigurationSection(shop + ".pages." + pageKey + ".items")
                : GUIShop.getINSTANCE().getConfigManager().getShopConfig().createSection(shop + ".pages." + pageKey + ".items");

        config.set(slot.toString(), item.serialize());

        GUIShop.getINSTANCE().getLogUtil().debugLog("Player edited item: " + item.getMaterial() + " slot: " + slot);
        try {
            GUIShop.getINSTANCE().getConfigManager().getShopConfig().save(GUIShop.getINSTANCE().getConfigManager().getShopFile());
        } catch (IOException ex) {
            GUIShop.getINSTANCE().getLogUtil().log("Error saving shops: " + ex.getMessage());
        }

        hasClicked = false;
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            if (!Config.isDisableEscapeBack() && !hasClicked && !GUIShop.getINSTANCE().isReload) {
                BukkitScheduler scheduler = Bukkit.getScheduler();
                scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> menuInstance.open(player), 1L);
            } else if (clickOverride) {
                clickOverride = false;
            } else {
                hasClicked = false;
            }
        } else if (!hasClicked && !clickOverride) {
            GUIShop.getCREATOR().remove(player.getUniqueId());
        }
    }

    private void shopItem(Item item, InventoryClickEvent event) {
        if (item.hasPermission()) {
            Permission permission = item.getPermission();
            if (permission.doesntHavePermission(player)) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-item-permission");
                return;
            }
        }

        hasClicked = true;
        if (Config.isAlternateSellEnabled() && item.hasSellPrice() && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            hasClicked = true;
            new AltSell(item, this).open(player);
        } else {
            if (item.isResolveFailed()) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "went-wrong", item.getResolveReason());
            } else {
                boolean quantityDisabled = item.getQuantityValue() == null || item.getQuantityValue().isDisabled();

                int maxStackSize = 64;

                try {
                    maxStackSize = XMaterial.matchXMaterial(item.getMaterial()).get().parseMaterial().getMaxStackSize();
                } catch (NoSuchElementException | NullPointerException ignored) {
                }

                int quantityCount;

                if (item.getQuantityValue() == null) {
                    quantityCount = maxStackSize;
                } else {
                    quantityCount = item.getQuantityValue().getQuantity();
                }

                if (quantityDisabled && maxStackSize > 1) {
                    new Quantity(item, this, player).loadInventory().open();
                } else {
                    new Quantity(item, this, player).buy(item, quantityCount);
                    hasClicked = false;
                }
            }
        }
    }

    private void commandItem(Item item) {
        if (item.hasPermission()) {
            Permission permission = item.getPermission();
            if (permission.doesntHavePermission(player)) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-item-permission");
                return;
            }
        }

        BigDecimal priceToPay;

        Runnable dynamicPricingUpdate = null;

        // sell price must be defined and nonzero for dynamic pricing to work
        if (Config.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {
            String itemString = item.getItemString();
            dynamicPricingUpdate = () -> GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().buyItem(itemString, 1);

            priceToPay = GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().calculateBuyPrice(itemString, 1, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal();
        }

        if (GUIShop.getINSTANCE().getMiscUtils().getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            item.getCommands().forEach(str -> {
                Bukkit.getServer().dispatchCommand(str.startsWith("sudo=") ? player : Bukkit.getConsoleSender(),
                        GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(str.startsWith("sudo=") ? str.substring(5).trim() : str.trim(), player, item));
            });
            if (Config.isSoundEnabled()) {
                player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
            }
            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            GUIShop.getINSTANCE().getLogUtil().transactionLog("Player " + player.getName() + " bought command " + item.getMaterial() + " in shop " + getShop() + " for " + priceToPay.toPlainString() + " money!");
        } else {
            String currencyPrefix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix");
            String currencySuffix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
            String amount = currencyPrefix + priceToPay + currencySuffix;

            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "not-enough-money", amount);
        }
    }

    private void handleItemClick(InventoryClickEvent event) {
        /*
         * If the player has enough money to purchase the item, then allow them to.
         */
        GUIShop.getINSTANCE().getLogUtil().debugLog("Creator status: " + GUIShop.getCREATOR().contains(player.getUniqueId()));

        String pageKey = "Page" + GUI.getCurrentPage();
        Item item = shopItem.getPages().get(pageKey).getItems().get(Integer.toString(event.getSlot()));

        if (item == null) {
            return;
        } else if (!item.hasBuyPrice() && item.getItemType() == ItemType.SHOP) {
            if (Config.isAlternateSellEnabled() && item.hasSellPrice() && item.getItemType() == ItemType.SHOP) {
                hasClicked = true;
                new AltSell(item, this).open(player);
            }
            return;
        }

        if (null != item.getItemType()) {
            switch (item.getItemType()) {
                case SHOP ->
                    shopItem(item, event);
                case COMMAND ->
                    commandItem(item);
                case SHOP_SHORTCUT ->
                    shopShortcut(item, event);
            }
        }
    }

    public void shopShortcut(Item item, InventoryClickEvent event) {
        Player clickingPlayer = (Player) event.getWhoClicked();
        if (item.hasTargetShop()) {
            String shopName = item.getTargetShop();
            if (GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(clickingPlayer, "guishop.shop." + shopName.toLowerCase()) || GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(clickingPlayer, "guishop.shop.*")) {
                if (!item.isResolveFailed()) {
                    hasClicked = true;
                    BukkitScheduler scheduler = Bukkit.getScheduler();
                    scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> this.menuInstance.openShop(clickingPlayer, shopName), 1L);

                } else {
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(clickingPlayer, "open-shop-error", item.getResolveReason());
                }
            } else {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(clickingPlayer, "no-permission");
            }
        }
    }

    private void handleBackwardButton() {
        if (GUI.hasPreviousPage()) {
            hasClicked = true;
            clickOverride = true;

            GUIShop.getINSTANCE().getLogUtil().debugLog("Going to previous page from " + GUI.getCurrentPage());
            GUI.previousPage();
            GUIShop.getINSTANCE().getLogUtil().debugLog("Now on page " + GUI.getCurrentPage());
        }
    }

    private void handleForwardButton() {
        if (GUI.hasNextPage()) {
            hasClicked = true;
            clickOverride = true;

            GUIShop.getINSTANCE().getLogUtil().debugLog("Going to next page from " + GUI.getCurrentPage());
            GUI.nextPage();
            GUIShop.getINSTANCE().getLogUtil().debugLog("Now on page " + GUI.getCurrentPage());
        }
    }

    public boolean isShopMissing() {
        return this.shopMissing;
    }

    /**
     * Get the current page index (for compatibility with other classes).
     */
    public int getCurrentPage() {
        return GUI != null ? GUI.getCurrentPage() : 0;
    }
}
