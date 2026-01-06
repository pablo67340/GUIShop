package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.*;
import com.pablo67340.guishop.statistics.StatisticsManager;
import com.pablo67340.guishop.util.PDCUtil;
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
     * Load the specified shop.
     * 
     * If the shop is not in cache, it will be loaded from config.
     * If preLoad is false, the GUI will be (re)created to display the items.
     *
     * @param preLoad True = only load data into cache. False = also create GUI.
     */
    public void loadItems(Boolean preLoad) {
        if (shop.equalsIgnoreCase("NONE")) {
            return;
        }

        // Always reset GUI when not preloading - ensures fresh render with current data
        if (!preLoad) {
            this.GUI = null;
        }

        if (!GUIShop.getINSTANCE().getLoadedShops().containsKey(shop)) {
            // Shop not in cache - load fresh from config file
            GUIShop.getINSTANCE().getLogUtil().debugLog("SHOP: Loading " + shop + " from config (not in cache)");
            loadShopFromConfig(preLoad);
        } else {
            // Shop in cache - use cached data
            GUIShop.getINSTANCE().getLogUtil().debugLog("SHOP: Loading " + shop + " from cache");
            shopItem = (ShopItem) GUIShop.getINSTANCE().getLoadedShops().get(shop);
            this.setTitle(GUIShop.getINSTANCE().getConfigManager().getShopConfig().getString(shop + ".title"));
            if (!preLoad) {
                loadShop();
            }
        }
    }

    /**
     * Load shop data from the configuration file with comprehensive error handling.
     */
    private void loadShopFromConfig(Boolean preLoad) {
        String shopTitle = GUIShop.getINSTANCE().getConfigManager().getShopConfig().getString(shop + ".title");
        if (shopTitle == null) {
            logShopError("Shop '" + shop + "' is missing a 'title' property. Add 'title: \"Your Shop Title\"' to the shop configuration.");
            shopMissing = true;
            return;
        }
        this.setTitle(shopTitle);
        shopItem = new ShopItem();

        ConfigurationSection pagesConfig = GUIShop.getINSTANCE().getConfigManager().getShopConfig().getConfigurationSection(shop + ".pages");
        if (pagesConfig == null) {
            logShopError("Shop '" + shop + "' has no 'pages' section. Check your shops.yml indentation and structure.");
            logShopError("Expected format:\n  " + shop + ":\n    title: 'Shop Title'\n    pages:\n      Page0:\n        items:\n          '0':\n            id: DIAMOND");
            shopMissing = true;
            return;
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Loading items for shop: " + shop);
        int pageIndex = 0;

        for (String pageKey : pagesConfig.getKeys(false)) {
            ShopPage page = new ShopPage();
            ConfigurationSection itemsSection = pagesConfig.getConfigurationSection(pageKey + ".items");

            if (itemsSection == null) {
                logShopError("Shop '" + shop + "' > " + pageKey + " is missing 'items' section.");
                logShopError("Expected format:\n  " + pageKey + ":\n    items:\n      '0':\n        id: DIAMOND");
                pageIndex++;
                continue;
            }

            for (String slotKey : itemsSection.getKeys(false)) {
                // Validate slot is a number
                int slot;
                try {
                    slot = Integer.parseInt(slotKey);
                } catch (NumberFormatException e) {
                    logShopError("Shop '" + shop + "' > " + pageKey + " > Item slot '" + slotKey + "' is not a valid number. Slot keys must be numbers (e.g., '0', '1', '2').");
                    continue;
                }

                ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                if (itemSection == null) {
                    logShopError("Shop '" + shop + "' > " + pageKey + " > Slot '" + slotKey + "' has invalid format. Check indentation.");
                    continue;
                }

                // Check for required 'id' field
                if (!itemSection.contains("id")) {
                    logShopError("Shop '" + shop + "' > " + pageKey + " > Slot '" + slotKey + "' is missing required 'id' field.");
                    continue;
                }

                Item item;
                try {
                    item = Item.deserialize(itemSection.getValues(true), slot, shop);
                } catch (Exception e) {
                    logShopError("Shop '" + shop + "' > " + pageKey + " > Slot '" + slotKey + "' failed to load: " + e.getMessage());
                    continue;
                }

                if (item == null) {
                    logShopError("Shop '" + shop + "' > " + pageKey + " > Slot '" + slotKey + "' returned null item. Check the item configuration.");
                    continue;
                }

                // Register sellable items
                if (item.hasSellPrice()) {
                    registerSellableItem(item, pageKey, slotKey);
                }

                // Add to page if appropriate
                if (!Config.isHideNonBuyable() || item.hasBuyPrice() || item.getItemType() != ItemType.SHOP) {
                    page.getItems().put(Integer.toString(item.getSlot()), item);
                }
            }

            shopItem.getPages().put("Page" + pageIndex, page);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loaded " + pageKey + " with " + page.getItems().size() + " items.");
            pageIndex++;
        }

        if (shopItem.getPages().isEmpty()) {
            logShopError("Shop '" + shop + "' loaded with 0 pages. Check your shops.yml configuration.");
            shopMissing = true;
            return;
        }

        shopItem.determineHighestSlots();
        GUIShop.getINSTANCE().getLoadedShops().put(shop, shopItem);
        GUIShop.getINSTANCE().getLogUtil().debugLog("Shop '" + shop + "' loaded successfully with " + shopItem.getPages().size() + " page(s).");

        if (!preLoad) {
            loadShop();
        }
    }

    /**
     * Register an item as sellable in the ITEMTABLE.
     */
    private void registerSellableItem(Item item, String pageKey, String slotKey) {
        try {
            String materialKey;
            ItemStack parsedItem;
            
            if (item.hasPotion() && item.getPotionInfo().getSplash()) {
                parsedItem = XMaterial.matchXMaterial("SPLASH_POTION").get().parseItem();
            } else {
                parsedItem = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();
            }
            
            // Some materials are block-only and don't have an item form (e.g., WALL_TORCH, WATER, TRIPWIRE)
            // These return null from parseItem() and cannot be registered as sellable
            if (parsedItem == null) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Skipping block-only material (no item form): " + item.getMaterial());
                return;
            }
            
            materialKey = parsedItem.getType().toString();
            
            List<Item> items = GUIShop.getINSTANCE().getITEMTABLE().get(item.getMaterial());
            if (items == null) {
                items = new ArrayList<>();
            }
            items.add(item);

                GUIShop.getINSTANCE().getLogUtil().debugLog("Registering " + item.getMaterial() + " as sellable.");
            GUIShop.getINSTANCE().getITEMTABLE().put(materialKey, items);
        } catch (NoSuchElementException e) {
            logShopError("Shop '" + shop + "' > " + pageKey + " > Slot '" + slotKey + "': Material '" + item.getMaterial() + "' is not valid for this server version.");
        } catch (Exception e) {
            logShopError("Shop '" + shop + "' > " + pageKey + " > Slot '" + slotKey + "': Failed to register sellable item - " + e.getMessage());
        }
    }

    /**
     * Log a shop configuration error with consistent formatting.
     */
    private void logShopError(String message) {
        GUIShop.getINSTANCE().getLogUtil().log("[Shop Config Error] " + message);
        if (this.player != null) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "shop-not-found", shop);
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
                GUIShop.getINSTANCE().getLogUtil().debugLog("LOADSHOP: Loading page " + entry.getKey() + " with " + entry.getValue().getItems().size() + " items");
                for (Item item : entry.getValue().getItems().values()) {
                    if (item.getItemType() == ItemType.BLANK) {
                        continue;
                    }
                    GUIShop.getINSTANCE().getLogUtil().debugLog("LOADSHOP: Item " + item.getMaterial() + " at slot " + item.getSlot() + 
                        " hasBuyPrice=" + item.hasBuyPrice() + " hasSellPrice=" + item.hasSellPrice() +
                        " type=" + item.getItemType());
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

        // Don't call closeInventory() explicitly - openInventory() handles the transition
        // This prevents mouse position reset when switching between different sized inventories
        // The close handler will still be triggered automatically by Bukkit

        // Reset click state flags when opening
        hasClicked = false;
        clickOverride = false;

        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            GUI.setTopClickHandler(this::onShopClick);
            GUI.setBottomClickHandler((e) -> e.setCancelled(true));
            // Explicitly reset allow flags to prevent item theft when GUI is reused
            GUI.setAllowTopInventoryClick(false);
            GUI.setAllowBottomInventoryClick(false);
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
                // Set hasClicked to prevent onClose from also scheduling menu.open()
                hasClicked = true;
                // Open menu directly - openInventory() will close current inventory
                menuInstance.open(player);
            }
        } else {
            handleItemClick(event);
        }
    }

    private void creatorPlayerInventoryClick(InventoryClickEvent e) {
        ItemStack clickedItem = e.getCurrentItem();
        
        // Right-click or Shift+click on an item = Open Item Editor GUI
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                                  e.getClick() == ClickType.SHIFT_LEFT || 
                                  e.getClick() == ClickType.SHIFT_RIGHT;
            
            if (isEditClick) {
                e.setCancelled(true);
                clickOverride = true;
                int currentPageForEditor = GUI.getCurrentPage();
                player.closeInventory();
                
                // Open the Item Editor GUI
                new com.pablo67340.guishop.listenable.editor.ItemEditorGui(
                    player, 
                    clickedItem, 
                    e.getSlot(), 
                    this.shop,
                    currentPageForEditor
                ).onSave(() -> {
                    // Reopen the shop after saving
                    // Cache was updated in saveToConfig(), now rebuild GUI with fresh data
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ONSAVE: Rebuilding shop GUI for " + this.shop);
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    this.loadItems(false);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ONSAVE: loadItems complete, opening shop");
                    this.open(player);
                }).onCancel(() -> {
                    // Reopen the shop on cancel
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    this.open(player);
                }).open();
                return;
            }
        }
        
        // Left-click = Allow normal item movement (don't cancel)
        // The item will be picked up/moved naturally by Minecraft
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        ItemStack clickedItem = e.getCurrentItem();
        
        // Calculate button slots - these should still function as buttons, not be stealable
        int inventorySize = GUI.getRows() * 9;
        int nextSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getForwardSlot(), inventorySize) - 1);
        int prevSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackwardSlot(), inventorySize) - 1);
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);
        
        // Handle pagination and back buttons (always cancel and process like normal)
        if (e.getSlot() == nextSlot) {
            e.setCancelled(true);
            handleForwardButton();
            return;
        } else if (e.getSlot() == prevSlot) {
            e.setCancelled(true);
            handleBackwardButton();
            return;
        } else if (e.getSlot() == backSlot && !Config.isDisableBackButton()) {
            e.setCancelled(true);
            if (menuInstance != null) {
                hasClicked = true;
                menuInstance.open(player);
            }
            return;
        }
        
        // Right-click or Shift+click on an existing item = Open Item Editor GUI
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                                  e.getClick() == ClickType.SHIFT_LEFT || 
                                  e.getClick() == ClickType.SHIFT_RIGHT;
            
            if (isEditClick) {
                e.setCancelled(true);
                clickOverride = true;
                int currentPageForEditor = GUI.getCurrentPage();
                player.closeInventory();
                
                // Open the Item Editor GUI
                new com.pablo67340.guishop.listenable.editor.ItemEditorGui(
                    player, 
                    clickedItem, 
                    e.getSlot(), 
                    this.shop,
                    currentPageForEditor
                ).onSave(() -> {
                    // Reopen the shop after saving
                    // Cache was updated in saveToConfig(), now rebuild GUI with fresh data
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ONSAVE: Rebuilding shop GUI for " + this.shop);
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    this.loadItems(false);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ONSAVE: loadItems complete, opening shop");
                    this.open(player);
                }).onCancel(() -> {
                    // Reopen the shop on cancel
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    this.open(player);
                }).open();
                return;
            }
        }
        
        // Left-click with cursor item onto empty slot = Place item (register it)
        if ((clickedItem == null || clickedItem.getType().isAir()) && e.getCursor() != null && !e.getCursor().getType().isAir()) {
            // Run after the event to get the placed item
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                ItemStack placedItem = e.getInventory().getItem(e.getSlot());
                if (placedItem != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("New item placed: " + placedItem.getType());
                    editShopItem(placedItem, e.getSlot());
                }
            }, 1L);
            return;
        }
        
        // Left-click to pick up an item = Delete from shop config
        if (clickedItem != null && !clickedItem.getType().isAir() && e.getClick() == ClickType.LEFT) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Removing item from slot: " + e.getSlot());
            deleteShopItem(e.getSlot());
            // Don't cancel - let them pick it up
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
            // Normal mode - handle escape back to menu
            if (!Config.isDisableEscapeBack() && !hasClicked && !GUIShop.getINSTANCE().isReload) {
                GUIShop.runLater(player, () -> menuInstance.open(player), 1L);
            } else if (clickOverride) {
                clickOverride = false;
            } else {
                hasClicked = false;
            }
        } else {
            // Creator mode - save and exit
            if (!clickOverride) {
                // Only save if we're not in the middle of opening the editor
                saveCreatorInventory(e.getInventory());
            }
            // ALWAYS remove from creator mode when closing
            GUIShop.getCREATOR().remove(player.getUniqueId());
            GUIShop.getINSTANCE().getLogUtil().debugLog("ONCLOSE: Removed player from creator mode");
        }
    }
    
    /**
     * Save all items in the inventory when closing in creator mode.
     * This handles items that were placed/moved without going through the Item Editor.
     */
    private void saveCreatorInventory(org.bukkit.inventory.Inventory inventory) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Starting save for shop " + shop);
        try {
            String pageKey = "Page" + GUI.getCurrentPage();
            int inventorySize = GUI.getRows() * 9;
            
            // Calculate button slots to skip
            int nextSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getForwardSlot(), inventorySize) - 1);
            int prevSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackwardSlot(), inventorySize) - 1);
            int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);
            
            org.bukkit.configuration.ConfigurationSection config = 
                GUIShop.getINSTANCE().getConfigManager().getShopConfig()
                    .getConfigurationSection(shop + ".pages." + pageKey + ".items");
            
            if (config == null) {
                config = GUIShop.getINSTANCE().getConfigManager().getShopConfig()
                    .createSection(shop + ".pages." + pageKey + ".items");
            }
            
            boolean hasChanges = false;
            
            for (int slot = 0; slot < inventorySize; slot++) {
                // Skip navigation button slots
                if (slot == nextSlot || slot == prevSlot || slot == backSlot) {
                    continue;
                }
                
                ItemStack item = inventory.getItem(slot);
                String slotKey = String.valueOf(slot);
                
                if (item == null || item.getType().isAir()) {
                    // Remove item from config if slot is now empty
                    if (config.contains(slotKey)) {
                        config.set(slotKey, null);
                        hasChanges = true;
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Removed item from slot " + slot);
                    }
                } else {
                    // Check if this is a new item (no GUIShop PDC data)
                    String existingType = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
                    
                    // If no item type set, this is a freshly placed item from player inventory
                    if (existingType == null) {
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Detected new item at slot " + slot + ": " + item.getType());
                        // Parse and save the new item
                        Item newItem = Item.parse(item, slot, shop);
                        // Default to DUMMY type for decoration items (no prices)
                        if (!newItem.hasBuyPrice() && !newItem.hasSellPrice()) {
                            newItem.setItemType(ItemType.DUMMY);
                        } else {
                            newItem.setItemType(ItemType.SHOP);
                        }
                        
                        // Save item properties individually to ensure proper YAML structure
                        java.util.Map<String, Object> serialized = newItem.serialize();
                        String itemPath = shop + ".pages." + pageKey + ".items." + slotKey;
                        org.bukkit.configuration.file.FileConfiguration shopConfig = 
                            GUIShop.getINSTANCE().getConfigManager().getShopConfig();
                        shopConfig.set(itemPath, null);
                        for (java.util.Map.Entry<String, Object> entry : serialized.entrySet()) {
                            shopConfig.set(itemPath + "." + entry.getKey(), entry.getValue());
                        }
                        
                        hasChanges = true;
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Added new item " + item.getType() + " at slot " + slot + " as " + newItem.getItemType());
                    }
                }
            }
            
            if (hasChanges) {
                GUIShop.getINSTANCE().getConfigManager().getShopConfig()
                    .save(GUIShop.getINSTANCE().getConfigManager().getShopFile());
                
                // Reload config from disk to ensure consistency
                GUIShop.getINSTANCE().getConfigManager().reloadShopConfig();
                
                // Invalidate the shop cache so next open gets fresh data
                GUIShop.getINSTANCE().getLoadedShops().remove(shop);
                
                GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Changes saved for shop " + shop);
                player.sendMessage(ChatColor.GREEN + "Shop changes saved!");
            } else {
                GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: No changes detected for shop " + shop);
            }
        } catch (Exception ex) {
            GUIShop.getINSTANCE().getLogUtil().log("Error saving creator inventory: " + ex.getMessage());
            ex.printStackTrace();
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
            boolean allCommandsSucceeded = true;
            
            for (String str : item.getCommands()) {
                boolean isSudo = str.startsWith("sudo=");
                String rawCommand = isSudo ? str.substring(5).trim() : str.trim();
                String processedCommand = GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(rawCommand, player, item);
                
                // Check for unresolved placeholders (indicates wrong format was used)
                if (hasUnresolvedPlaceholders(processedCommand)) {
                    GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Unresolved placeholders in command: " + processedCommand);
                    GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Make sure to use %placeholder% or {placeholder} format (e.g., %player_name% or {player_name})");
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "command-error");
                    allCommandsSucceeded = false;
                    continue;
                }
                
                try {
                    Bukkit.getServer().dispatchCommand(isSudo ? player : Bukkit.getConsoleSender(), processedCommand);
                } catch (Exception e) {
                    // Log the error gracefully without dumping a stack trace
                    GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Failed to execute command: " + processedCommand);
                    GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Reason: " + e.getMessage());
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "command-error");
                    allCommandsSucceeded = false;
                }
            }
            
            if (allCommandsSucceeded && Config.isSoundEnabled()) {
                try {
                    player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
                } catch (Exception ignored) {}
            }
            
            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            GUIShop.getINSTANCE().getLogUtil().transactionLog("Player " + player.getName() + " bought command " + item.getMaterial() + " in shop " + getShop() + " for " + priceToPay.toPlainString() + " money!");
            
            // Track purchase statistics
            StatisticsManager statsManager = StatisticsManager.getInstance();
            if (statsManager != null && statsManager.isAvailable()) {
                statsManager.recordPurchase(player, item.getMaterial(), 1, priceToPay);
            }
        } else {
            String currencyPrefix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix");
            String currencySuffix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
            String amount = currencyPrefix + priceToPay + currencySuffix;

            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "not-enough-money", amount);
        }
    }

    /**
     * Check if a command string has unresolved placeholders.
     * Detects patterns like %something% or {something} that weren't replaced.
     */
    private boolean hasUnresolvedPlaceholders(String command) {
        // Check for %placeholder% patterns (but ignore PlaceholderAPI's %% escape)
        if (command.matches(".*%[a-zA-Z_]+%.*")) {
            // Could be PlaceholderAPI placeholders, check for common GUIShop ones
            String lower = command.toLowerCase();
            if (lower.contains("%player_name%") || lower.contains("%player_uuid%") || 
                lower.contains("%player_world%") || lower.contains("%player_balance%") ||
                lower.contains("%buy_price%") || lower.contains("%sell_price%")) {
                return true;
            }
        }
        
        // Check for {placeholder} patterns
        if (command.matches(".*\\{[a-zA-Z_]+\\}.*")) {
            String lower = command.toLowerCase();
            if (lower.contains("{player_name}") || lower.contains("{player_uuid}") || 
                lower.contains("{player_world}") || lower.contains("{player_balance}") ||
                lower.contains("{buy_price}") || lower.contains("{sell_price}")) {
                return true;
            }
        }
        
        return false;
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
                    GUIShop.runLater(clickingPlayer, () -> this.menuInstance.openShop(clickingPlayer, shopName), 1L);

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
