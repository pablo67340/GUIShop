package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import java.util.*;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.ShopItem;
import com.pablo67340.guishop.definition.ShopPage;
import com.pablo67340.guishop.util.ConfigUtil;
import java.io.IOException;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

public class Shop {

    /**
     * The name of this {@link Shop}.
     */
    @Getter
    @Setter
    private String shop, title;

    /**
     * The list of {@link Page}'s in this {@link Shop}.
     */
    private Gui GUI;

    private final Menu menuInstance;

    private Boolean hasClicked = false;

    private PaginatedPane currentPane;

    private ShopItem shopItem;

    private ShopPane shopPage = new ShopPane(9, 6);

    private final Player player;

    private Boolean shopMissing = false;

    private Integer pageIndex = 0;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param player Thep layer using the shop.
     * @param shop The name of the shop.
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
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCheck shops.yml for shop " + shop + ". It was not found."));
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
                                    GUIShop.log("Error adding item: " + item.getMaterial() + " to sellable list. Wrong item name, or item does not exist for this server version.");
                                }
                            }
                        }
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    GUIShop.debugLog("Adding page: " + "Page" + Integer.toString(shopItem.getPages().size()) + " to pages.");
                    shopItem.getPages().put("Page" + Integer.toString(shopItem.getPages().size()), page);
                });
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
            if (this.hasMultiplePages()) {
                this.GUI = new Gui(GUIShop.getINSTANCE(), 6,
                        ChatColor.translateAlternateColorCodes('&', ConfigUtil.getShopTitle().replace("{shopname}", title)));
            } else {
                int rows = (int) Math.ceil((double) shopItem.getPages().get("Page0").getItems().size() / 9);
                if (rows == 0) {
                    rows = 1;
                }
                this.GUI = new Gui(GUIShop.getINSTANCE(), rows,
                        ChatColor.translateAlternateColorCodes('&', ConfigUtil.getShopTitle().replace("{shopname}", title)));
            }
            PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
            Collection<ShopPage> shopPages = shopItem.getPages().values();
            for (ShopPage page : shopPages) {
                shopPage = new ShopPane(9, 6);
                for (Item item : page.getItems().values()) {

                    GuiItem gItem = new GuiItem(item.toItemStack(player, false));
                    shopPage.setItem(gItem, item.getSlot());

                }

                applyButtons(shopPage, pageIndex, shopPages.size());
                pane.addPane(pageIndex, shopPage);
                pageIndex += 1;

            }

            GUI.addPane(pane);
            this.currentPane = pane;
        }

    }

    public Boolean hasMultiplePages() {
        return this.shopItem.getPages().size() > 1;
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
        GUIShop.debugLog("Applying buttons with pageIndex: " + pageIndex + " maxPages: " + maxPages);
        if (pageIndex > 0) {
            GUIShop.debugLog("Adding Back Button");
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

            page.setItem(item, this.GUI.getInventory().getSize() - 1);
        }
    }

    /**
     * Open the player's shop
     *
     * @param input The player the shop will open for.
     */
    public void open(Player input) {
        // currentPane.setPage(0);
        if (this.isShopMissing() || shop.equalsIgnoreCase("NONE")) {
            return;
        }
        GUI.show(input);
        if (!GUIShop.getCREATOR().contains(input.getName())) {
            GUI.setOnTopClick(this::onShopClick);
            GUI.setOnBottomClick((e) -> {
                e.setCancelled(true);
            });
        } else {
            GUI.setOnBottomClick(this::creatorPlayerInventoryClick);
            GUI.setOnTopClick(this::creatorTopInventoryClick);
        }
        GUI.setOnClose(this::onClose);

    }

    private void onShopClick(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        // Forward Button
        GUIShop.debugLog("Clicked: " + e.getSlot());
        if (e.getSlot() == 51) {
            if (shopItem.getPages().size() > 1 && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {
                hasClicked = true;

                GUIShop.debugLog("Setting page " + currentPane.getPage() + " to not visible");
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                GUIShop.debugLog("Setting page to: " + (currentPane.getPage() + 1));
                currentPane.setPage(currentPane.getPage() + 1);

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUIShop.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");
                GUI.update();
            }
            return;
            // Backward Button
        } else if (e.getSlot() == 47) {
            if (currentPane.getPage() != 0 && currentPane.getPages() > 0) {
                hasClicked = true;
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                currentPane.setPage(currentPane.getPage() - 1);

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUI.update();
            }
            return;
            // Back Button
        } else if (e.getSlot() == (this.GUI.getInventory().getSize() - 1) && !ConfigUtil.isDisableBackButton()) {
            if (menuInstance != null && !GUIShop.getCREATOR().contains(player.getName())) {
                menuInstance.open(player);
            }
            return;
        }

        /*
         * If the player has enough money to purchase the item, then allow them to.
         */
        GUIShop.debugLog("Creator Status:" + GUIShop.getCREATOR().contains(player.getName()));

        Item item = shopItem.getPages().get("Page" + currentPane.getPage()).getItems().get(Integer.toString(e.getSlot()));

        if (item == null) {
            return;

        } else if (!item.hasBuyPrice()) {

            if (ConfigUtil.isAlternateSellEnabled() && item.hasSellPrice()) {
                hasClicked = true;
                new AltSell(item).open(player);
            } else {
                if (item.getItemType() == ItemType.DUMMY) {
                    return;
                } else {
                    player.sendMessage(ConfigUtil.getPrefix() + " " + ConfigUtil.getCannotBuy());
                }
            }
            return;
        }

        if (item.getItemType() == ItemType.SHOP) {
            hasClicked = true;
            if (ConfigUtil.isAlternateSellEnabled() && item.hasSellPrice() && (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                hasClicked = true;
                new AltSell(item).open(player);
            } else {
                if (item.isResolveFailed()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCannot purchase item that contains errors."));
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: " + item.getResolveReason()));
                } else {
                    if (!item.isDisableQty()) {
                        new Quantity(item, this, player).loadInventory().open();
                    } else {
                        new Quantity(item, this, player).buy(item, 1, e);
                        hasClicked = false;
                    }
                }

            }
        } else if (item.getItemType() == ItemType.COMMAND) {

            BigDecimal priceToPay;

            Runnable dynamicPricingUpdate = null;

            // sell price must be defined and nonzero for dynamic pricing to work
            if (ConfigUtil.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {

                String itemString = item.getItemString();
                dynamicPricingUpdate = () -> GUIShop.getDYNAMICPRICING().buyItem(itemString, 1);

                priceToPay = GUIShop.getDYNAMICPRICING().calculateBuyPrice(itemString, 1, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
            } else {
                priceToPay = item.getBuyPriceAsDecimal();
            }

            if (GUIShop.getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
                item.getCommands().forEach(str -> {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            GUIShop.placeholderIfy(str, player, item));
                });
                if (dynamicPricingUpdate != null) {
                    dynamicPricingUpdate.run();
                }
            } else {
                player.sendMessage(ConfigUtil.getPrefix() + ConfigUtil.getNotEnoughPre() + priceToPay
                        + ConfigUtil.getNotEnoughPost());
            }
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
                    GUIShop.debugLog("new Item: " + item.getType());
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
                    GUIShop.debugLog("new Item: " + item.getType());
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
            GUIShop.getINSTANCE().getShopConfig().save(GUIShop.getINSTANCE().getShopf());
        } catch (IOException ex) {
            GUIShop.debugLog("Error saving Shops: " + ex.getMessage());
        }
    }

    public void editShopItem(ItemStack itemStack, Integer slot) {

        Item item = Item.parse(itemStack, slot, shop);
        shopItem.getPages().get("Page" + currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);

        ConfigurationSection config = GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items") != null
                ? GUIShop.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items")
                : GUIShop.getINSTANCE().getShopConfig().createSection(shop + ".pages.Page" + currentPane.getPage() + ".items");

        config.set(slot.toString(), item.serialize());
        GUIShop.debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + slot);
        try {
            GUIShop.getINSTANCE().getShopConfig().save(GUIShop.getINSTANCE().getShopf());
        } catch (IOException ex) {
            GUIShop.debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if (!GUIShop.CREATOR.contains(player.getName())) {
            if (!ConfigUtil.isDisableEscapeBack() && !hasClicked) {
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> menuInstance.open(player), 1L);
            } else {
                hasClicked = false;
            }
        } else if (!hasClicked) {
            GUIShop.CREATOR.remove(player.getName());
        }

    }

    public Boolean isShopMissing() {
        return this.shopMissing;
    }

}
