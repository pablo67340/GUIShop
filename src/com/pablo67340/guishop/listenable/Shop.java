package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.*;
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
     * The list of {@link ShopPage}'s in this {@link Shop}.
     */
    public Gui GUI;

    private final Menu menuInstance;

    private boolean hasClicked = false, clickOverride = false;

    public PaginatedPane currentPane;

    private ShopItem shopItem;

    private ShopPane shopPage = new ShopPane(9, 6);

    private final Player player;

    private Boolean shopMissing = false;

    private Integer pageIndex = 0;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param player       Thep layer using the shop.
     * @param shop         The name of the shop.
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
            this.setTitle(GUIShop.getINSTANCE().getShopConfig().getString(shop + ".title"));
            shopItem = new ShopItem();
            ConfigurationSection config = GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages");
            if (config == null) {
                GUIShop.log("Check shops.yml for shop " + shop + ". It was not found.");
                if (this.player != null) {
                    GUIShop.sendPrefix(player, "shop-not-found", shop);
                }
                shopMissing = true;
            } else {
                GUIShop.debugLog("Loading items for shop: " + shop);
                config.getKeys(false).stream().map(str -> {
                    ShopPage page = new ShopPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    GUIShop.debugLog("Reading Shop Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        GUIShop.debugLog("Reading item: " + key + " in page " + str);
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
                                GUIShop.debugLog("Making item: SPLASH_POTION sellable.");
                                GUIShop.getINSTANCE().getITEMTABLE().put(XMaterial.matchXMaterial("SPLASH_POTION").get().parseItem().getType().toString(), items);
                            } else {
                                try {
                                    GUIShop.debugLog("Making item: " + item.getMaterial() + " sellable.");
                                    GUIShop.getINSTANCE().getITEMTABLE().put(XMaterial.matchXMaterial(item.getMaterial()).get().parseItem().getType().toString(), items);
                                } catch (Exception ex) {
                                    GUIShop.log("Error adding item: " + item.getMaterial() + " to sellable list. Wrong item name or item does not exist for this server version.");
                                }
                            }
                        }
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    GUIShop.debugLog("Adding page: " + "Page" + shopItem.getPages().size() + " to pages.");
                    shopItem.getPages().put("Page" + shopItem.getPages().size(), page);
                });

                shopItem.determineHighestSlots();

                GUIShop.debugLog("Shop items added to loaded shops");
                GUIShop.getINSTANCE().getLoadedShops().put(shop, shopItem);
                if (!preLoad) {
                    loadShop();
                }
            }
        } else {
            shopItem = (ShopItem) GUIShop.getINSTANCE().getLoadedShops().get(shop);
            this.setTitle(GUIShop.getINSTANCE().getShopConfig().getString(shop + ".title"));
            //Re-Check for preload here in case they have multiple item's leading to one shop.
            if (!preLoad) {
                loadShop();
            }
        }
    }

    private void loadShop() {
        if (this.GUI == null || this.GUI.getItems().isEmpty()) {
            PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
            for (Map.Entry<String, ShopPage> entry : shopItem.getPages().entrySet()) {
                if (this.GUI == null) {
                    this.GUI = new Gui(GUIShop.getINSTANCE(), GUIShop.rowChart.getRowsFromHighestSlot(shopItem.getHighestPageSlot(entry.getKey())),
                            ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getShopTitle().replace("%shopname%", title)));
                    int rows = GUIShop.rowChart.getRowsFromHighestSlot(entry.getValue().getHighestSlot());
                    if (rows != 6 && this.hasMultiplePages()) {
                        this.GUI = new Gui(GUIShop.getINSTANCE(), rows + 1,
                                ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getShopTitle().replace("%shopname%", title)));
                    } else {
                        this.GUI = new Gui(GUIShop.getINSTANCE(), rows,
                                ChatColor.translateAlternateColorCodes('&', Config.getTitlesConfig().getShopTitle().replace("%shopname%", title)));
                    }
                }

                shopPage = new ShopPane(9, 6);

                for (Item item : entry.getValue().getItems().values()) {
                    if (item.getItemType() == ItemType.BLANK) {
                        continue;
                    }
                    GuiItem gItem = new GuiItem(item.toItemStack(player, false));
                    shopPage.setItem(gItem, item.getSlot());
                }

                applyButtons(shopPage, pageIndex, shopItem.getPages().size());
                pane.addPane(pageIndex, shopPage);
                pageIndex += 1;
            }

            GUI.addPane(pane);
            this.currentPane = pane;
        }
    }

    private void applyButtons(ShopPane page, int pageIndex, int maxPages) {
        GUIShop.debugLog("Applying buttons with page index: " + pageIndex + " max pages: " + maxPages);

        int nextSlot = calculateSlot(Config.getButtonConfig().getForwardSlot(), GUI.getRows() * 9) - 1;
        int prevSlot = calculateSlot(Config.getButtonConfig().getBackwardSlot(), GUI.getRows() * 9) - 1;
        int backSlot = calculateSlot(Config.getButtonConfig().getBackSlot(), GUI.getRows() * 9) - 1;

        if (pageIndex < (maxPages - 1)) {
            GUIShop.debugLog("Adding forward button");
            page.setItem(new GuiItem(Config.getButtonConfig().forwardButton.toItemStack(player, false)), nextSlot);
        }

        if (pageIndex > 0) {
            GUIShop.debugLog("Adding backward button");
            page.setItem(new GuiItem(Config.getButtonConfig().backwardButton.toItemStack(player, false)), prevSlot);
        }

        if (!Config.isDisableBackButton()) {
            GUIShop.debugLog("Adding back button");
            ItemStack backButtonItem = Config.getButtonConfig().backButton.toItemStack(player, false);

            GuiItem item = new GuiItem(backButtonItem);

            page.setItem(item, backSlot);
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

        GUI.show(player);

        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            GUI.setOnTopClick(this::onShopClick);
            GUI.setOnBottomClick((e) -> e.setCancelled(true));
        } else {
            GUI.setOnBottomClick(this::creatorPlayerInventoryClick);
            GUI.setOnTopClick(this::creatorTopInventoryClick);
        }
        GUI.setOnClose(this::onClose);
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

        // Forward Button
        GUIShop.debugLog("Clicked: " + event.getSlot());
        if (event.getSlot() == (GUI.getRows() * 9) - 3) {
            handleForwardButton(event);
            // Backward Button
        } else if (event.getSlot() == (GUI.getRows() * 9) - 7) {
            handleBackwardButton(event);
            // Back Button
        } else if (event.getSlot() == (shopItem.getHighestPageSlot("Page" + currentPane.getPage()) - 1) && !Config.isDisableBackButton()) {
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
                    GUIShop.debugLog("New item: " + item.getType());
                    editShopItem(item, slot);
                }
            }, 5L);
        }
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() != null || e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            GUIShop.debugLog("Cursor: " + e.getCursor());
            deleteShopItem(e.getSlot());

            // When an item is dropped into the slot, it's not null. This is a new item.
        } else {
            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(e.getSlot());
                if (item != null) {
                    GUIShop.debugLog("New item: " + item.getType());
                    editShopItem(item, e.getSlot());
                }
            }, 5L);
        }
    }

    private void deleteShopItem(Integer slot) {
        shopItem.getPages().get("Page" + currentPane.getPage()).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getShopConfig().createSection(shop + ".pages.Page" + currentPane.getPage() + ".items");
        config.set(slot.toString(), null);
        try {
            GUIShop.getINSTANCE().getShopConfig().save(GUIShop.getINSTANCE().getShopFile());
        } catch (IOException ex) {
            GUIShop.debugLog("Error saving shops: " + ex.getMessage());
        }
    }

    public void editShopItem(ItemStack itemStack, Integer slot) {
        Item item = Item.parse(itemStack, slot, shop);
        shopItem.getPages().get("Page" + currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getShopConfig().createSection(shop + ".pages.Page" + currentPane.getPage() + ".items");

        config.set(slot.toString(), item.serialize());

        GUIShop.debugLog("Player edited item: " + item.getMaterial() + " slot: " + slot);
        try {
            GUIShop.getINSTANCE().getShopConfig().save(GUIShop.getINSTANCE().getShopFile());
        } catch (IOException ex) {
            GUIShop.log("Error saving shops: " + ex.getMessage());
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
                GUIShop.sendPrefix(player, "no-item-permission");
                return;
            }
        }

        hasClicked = true;
        if (Config.isAlternateSellEnabled() && item.hasSellPrice() && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            hasClicked = true;
            new AltSell(item, this).open(player);
        } else {
            if (item.isResolveFailed()) {
                GUIShop.sendPrefix(player, "went-wrong", item.getResolveReason());
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
                GUIShop.sendPrefix(player, "no-item-permission");
                return;
            }
        }

        BigDecimal priceToPay;

        Runnable dynamicPricingUpdate = null;

        // sell price must be defined and nonzero for dynamic pricing to work
        if (Config.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {
            String itemString = item.getItemString();
            dynamicPricingUpdate = () -> GUIShop.getDYNAMICPRICING().buyItem(itemString, 1);

            priceToPay = GUIShop.getDYNAMICPRICING().calculateBuyPrice(itemString, 1, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal();
        }

        if (GUIShop.getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            item.getCommands().forEach(str -> {
                Bukkit.getServer().dispatchCommand(str.startsWith("sudo=") ? player : Bukkit.getConsoleSender(),
                        GUIShop.placeholderIfy(str.startsWith("sudo=") ? str.substring(5).trim() : str.trim(), player, item));
            });
            if (Config.isSoundEnabled()) {
                player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
            }
            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            GUIShop.transactionLog("Player " + player.getName() + " bought command " + item.getMaterial() + " in shop " + getShop() + " for " + priceToPay.toPlainString() + " money!");
        } else {
            String currencyPrefix = GUIShop.getINSTANCE().messageSystem.translate("messages.currency-prefix");
            String currencySuffix = GUIShop.getINSTANCE().messageSystem.translate("messages.currency-suffix");
            String amount = currencyPrefix + priceToPay + currencySuffix;

            GUIShop.sendPrefix(player, "not-enough-money", amount);
        }
    }

    private void handleItemClick(InventoryClickEvent event) {
        /*
         * If the player has enough money to purchase the item, then allow them to.
         */
        GUIShop.debugLog("Creator status: " + GUIShop.getCREATOR().contains(player.getUniqueId()));

        Item item = shopItem.getPages().get("Page" + currentPane.getPage()).getItems().get(Integer.toString(event.getSlot()));

        if (item == null) {
            return;
        } else if (!item.hasBuyPrice()) {
            if (Config.isAlternateSellEnabled() && item.hasSellPrice() && item.getItemType() == ItemType.SHOP) {
                hasClicked = true;
                new AltSell(item, this).open(player);
            }
            return;
        }

        if (item.getItemType() == ItemType.SHOP) {
            shopItem(item, event);
        } else if (item.getItemType() == ItemType.COMMAND) {
            commandItem(item);
        }
    }

    private void handleBackwardButton(InventoryClickEvent event) {
        if (currentPane.getPage() != 0 && currentPane.getPages() > 0) {
            hasClicked = true;
            clickOverride = true;

            GUIShop.debugLog("Setting page " + currentPane.getPage() + " to not visible");
            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);

            GUIShop.debugLog("Setting page to: " + (currentPane.getPage() - 1));
            currentPane.setPage(currentPane.getPage() - 1);

            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
            GUIShop.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");

            int rows = GUIShop.rowChart.getRowsFromHighestSlot(shopItem.getHighestPageSlot("Page" + (currentPane.getPage())));
            if (rows != 6) {
                rows += 1;
            }
            GUI.setRows(rows);
            GUIShop.debugLog("Resizing GUI to the next pane.");

            GUI.update();
        } else {
            handleItemClick(event);
        }
    }

    private void handleForwardButton(InventoryClickEvent event) {
        if (shopItem.getPages().size() > 1 && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {
            hasClicked = true;
            clickOverride = true;

            GUIShop.debugLog("Setting page " + currentPane.getPage() + " to not visible");
            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);

            GUIShop.debugLog("Setting page to: " + (currentPane.getPage() + 1));
            currentPane.setPage(currentPane.getPage() + 1);

            ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
            GUIShop.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");

            int rows = GUIShop.rowChart.getRowsFromHighestSlot(shopItem.getHighestPageSlot("Page" + (currentPane.getPage())));
            if (rows != 6) {
                rows += 1;
            }
            GUI.setRows(rows);
            GUIShop.debugLog("Resizing GUI to the next pane.");

            GUI.update();
        } else {
            handleItemClick(event);
        }
    }

    public boolean isShopMissing() {
        return this.shopMissing;
    }
}
