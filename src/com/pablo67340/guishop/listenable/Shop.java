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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.pablo67340.guishop.util.SchedulerUtil;

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
            FileConfiguration shopConfig = GUIShop.getINSTANCE().getConfigManager().getShopConfig(shop);
            if (shopConfig != null) {
                this.setTitle(shopConfig.getString("title"));
            }
            if (!preLoad) {
                loadShop();
            }
        }
    }

    /**
     * Load shop data from the configuration file with comprehensive error handling.
     */
    private void loadShopFromConfig(Boolean preLoad) {
        FileConfiguration shopConfig = GUIShop.getINSTANCE().getConfigManager().getShopConfig(shop);
        if (shopConfig == null) {
            logShopError("Shop '" + shop + "' config file not found. Create shops/" + shop + ".yml");
            shopMissing = true;
            return;
        }
        
        String shopTitle = shopConfig.getString("title");
        if (shopTitle == null) {
            logShopError("Shop '" + shop + "' is missing a 'title' property. Add 'title: \"Your Shop Title\"' to the shop configuration.");
            shopMissing = true;
            return;
        }
        this.setTitle(shopTitle);
        shopItem = new ShopItem();

        ConfigurationSection pagesConfig = shopConfig.getConfigurationSection("pages");
        if (pagesConfig == null) {
            logShopError("Shop '" + shop + "' has no 'pages' section. Check your " + shop + ".yml indentation and structure.");
            logShopError("Expected format:\n  title: 'Shop Title'\n  pages:\n    Page0:\n      items:\n        '0':\n          id: DIAMOND");
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
                
                GUIShop.getINSTANCE().getLogUtil().debugLog("LOAD: Item " + item.getMaterial() + " at slot " + slotKey + 
                    " type=" + item.getItemType() + " buyPrice=" + (item.hasBuyPrice() ? item.getBuyPriceAsDecimal() : "none"));

                // Register sellable items
                if (item.hasSellPrice()) {
                    registerSellableItem(item, pageKey, slotKey);
                }

                // Add to page if appropriate
                if (!Config.isHideNonBuyable() || item.hasBuyPrice() || item.getItemType() != ItemType.SHOP) {
                    page.getItems().put(Integer.toString(item.getSlot()), item);
                }
            }

            // Use the actual YAML page key (Page1, Page2, etc.) for cache consistency
            shopItem.getPages().put(pageKey, page);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loaded " + pageKey + " with " + page.getItems().size() + " items.");
            pageIndex++;
        }

        if (shopItem.getPages().isEmpty()) {
            logShopError("Shop '" + shop + "' loaded with 0 pages. Check your shops/" + shop + ".yml configuration.");
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
            // Calculate dynamic row count based on item count (7 items per row with side padding)
            int maxItemsOnPage = 0;
            for (ShopPage page : shopItem.getPages().values()) {
                int itemCount = 0;
                for (Item item : page.getItems().values()) {
                    if (item.getItemType() != ItemType.BLANK) {
                        itemCount++;
                    }
                }
                if (itemCount > maxItemsOnPage) {
                    maxItemsOnPage = itemCount;
                }
            }
            
            // Calculate rows: top buffer + item rows + bottom buffer + pagination
            // 1-7 items = 4 rows, 8-14 items = 5 rows, 15-21 items = 6 rows
            int itemRows = (int) Math.ceil(maxItemsOnPage / 7.0);
            int initialRows = Math.min(6, Math.max(4, itemRows + 3)); // +1 top buffer, +1 bottom buffer, +1 pagination

            String guiTitle = ChatColor.translateAlternateColorCodes('&', 
                    Config.getTitlesConfig().getShopTitle().replace("%shopname%", title));

            this.GUI = new PagedGui(initialRows, guiTitle);
            this.GUI.setDynamicRows(false);

            int pageIndex = 0;
            for (Map.Entry<String, ShopPage> entry : shopItem.getPages().entrySet()) {
                // Add a new page
                GUI.addPage();

                int rows = initialRows;
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

        int bottomRowStart = (rows - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row (slot 49 for 6-row)
        int prevSlot = centerSlot - 1; // Left of center
        int nextSlot = centerSlot + 1; // Right of center
        int backSlot = bottomRowStart + 8; // Bottom right corner
        int playerHeadSlot = bottomRowStart; // Bottom left corner
        
        boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());

        // Add page indicator in center - always shows (configurable via buttons.page-indicator)
        Item pageIndicatorConfig = Config.getButtonConfig().getPageIndicatorButton();
        ItemStack pageIndicator = pageIndicatorConfig.toItemStack(player, false);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        String indicatorName = pageIndicatorConfig.hasShopName() ? pageIndicatorConfig.getShopName() : "&fPage %page% of %maxpage%";
        pageMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            indicatorName.replace("%page%", String.valueOf(pageIndex + 1)).replace("%maxpage%", String.valueOf(maxPages))));
        if (isCreatorMode) {
            List<String> lore = pageMeta.getLore() != null ? new ArrayList<>(pageMeta.getLore()) : new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
            pageMeta.setLore(lore);
        }
        pageIndicator.setItemMeta(pageMeta);
        PDCUtil.setString(pageIndicator, PDCUtil.KEY_GUI_ELEMENT, "true");
        GUI.setItem(pageIndex, centerSlot, pageIndicator);

        // In editor mode, always show navigation buttons so they can be edited
        // In normal mode, only show if there are multiple pages and we're not at the edge
        boolean showForwardButton = isCreatorMode || (maxPages > 1 && pageIndex < (maxPages - 1));
        boolean showBackwardButton = isCreatorMode || (maxPages > 1 && pageIndex > 0);
        
        if (showForwardButton) {
            ItemStack forwardButton = Config.getButtonConfig().getForwardButton().toItemStack(player, false);
            ItemMeta forwardMeta = forwardButton.getItemMeta();
            if (isCreatorMode && forwardMeta != null) {
                List<String> lore = forwardMeta.getLore() != null ? new ArrayList<>(forwardMeta.getLore()) : new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Next Page/Create Page]");
                forwardMeta.setLore(lore);
                forwardButton.setItemMeta(forwardMeta);
            }
            PDCUtil.setString(forwardButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding forward button at slot " + nextSlot);
            GUI.setItem(pageIndex, nextSlot, forwardButton);
        }

        if (showBackwardButton) {
            ItemStack backwardButton = Config.getButtonConfig().getBackwardButton().toItemStack(player, false);
            ItemMeta backwardMeta = backwardButton.getItemMeta();
            if (isCreatorMode && backwardMeta != null) {
                List<String> lore = backwardMeta.getLore() != null ? new ArrayList<>(backwardMeta.getLore()) : new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Previous Page]");
                backwardMeta.setLore(lore);
                backwardButton.setItemMeta(backwardMeta);
            }
            PDCUtil.setString(backwardButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding backward button at slot " + prevSlot);
            GUI.setItem(pageIndex, prevSlot, backwardButton);
        }

        // Add back button (bottom right) - always show
        GUIShop.getINSTANCE().getLogUtil().debugLog("Adding back button at slot " + backSlot);
        ItemStack backButtonItem = createBackButton();
        if (isCreatorMode) {
            ItemMeta backMeta = backButtonItem.getItemMeta();
            if (backMeta != null) {
                List<String> lore = backMeta.getLore() != null ? new ArrayList<>(backMeta.getLore()) : new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Back to Menu]");
                backMeta.setLore(lore);
                backButtonItem.setItemMeta(backMeta);
            }
        }
        GUI.setItem(pageIndex, backSlot, backButtonItem);

        // Add player head with balance in bottom left
        if (player != null) {
            ItemStack playerHead = createPlayerHead();
            if (isCreatorMode) {
                ItemMeta headMeta = playerHead.getItemMeta();
                if (headMeta != null) {
                    List<String> lore = headMeta.getLore() != null ? new ArrayList<>(headMeta.getLore()) : new ArrayList<>();
                    lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                    headMeta.setLore(lore);
                    playerHead.setItemMeta(headMeta);
                }
            }
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding player head at slot " + playerHeadSlot);
            GUI.setItem(pageIndex, playerHeadSlot, playerHead);
        }
    }

    /**
     * Creates the player head showing their balance.
     */
    private ItemStack createPlayerHead() {
        ItemStack playerHead = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        skullMeta.setOwningPlayer(player);
        
        // Get player balance
        double balance = GUIShop.getINSTANCE().getMiscUtils().getECONOMY().getBalance(player);
        String formattedBalance = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
            + GUIShop.getINSTANCE().getMiscUtils().economyFormat(BigDecimal.valueOf(balance))
            + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
        
        skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionBalanceTitle().replace("%player%", player.getName())));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionBalanceLore().replace("%balance%", formattedBalance)));
        skullMeta.setLore(lore);
        
        playerHead.setItemMeta(skullMeta);
        // Mark as GUI element to prevent worth display
        PDCUtil.setString(playerHead, PDCUtil.KEY_GUI_ELEMENT, "true");
        return playerHead;
    }

    /**
     * Creates the back button (red glass pane) for returning to menu.
     */
    private ItemStack createBackButton() {
        ItemStack backButton = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lBack"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Return to menu"));
        meta.setLore(lore);
        // Set PDC to mark as BACK type
        meta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
            org.bukkit.persistence.PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
            org.bukkit.persistence.PersistentDataType.STRING, ItemType.BACK.name());
        backButton.setItemMeta(meta);
        return backButton;
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
        int bottomRowStart = (GUI.getRows() - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row
        int prevSlot = centerSlot - 1; // Left of center (ghast tear)
        int nextSlot = centerSlot + 1; // Right of center (ghast tear)
        int backSlot = bottomRowStart + 8; // Bottom right corner
        int playerHeadSlot = bottomRowStart; // Bottom left corner

        // Forward Button (ghast tear right of center)
        GUIShop.getINSTANCE().getLogUtil().debugLog("Clicked: " + event.getSlot());
        if (event.getSlot() == nextSlot) {
            handleForwardButton();
            // Backward Button (ghast tear left of center)
        } else if (event.getSlot() == prevSlot) {
            handleBackwardButton();
            // Center slot (nether star page indicator) - do nothing
        } else if (event.getSlot() == centerSlot) {
            return;
            // Back Button
        } else if (event.getSlot() == backSlot) {
            if (menuInstance != null && !GUIShop.getCREATOR().contains(player.getUniqueId())) {
                // Set hasClicked to prevent onClose from also scheduling menu.open()
                hasClicked = true;
                // Open menu directly - openInventory() will close current inventory
                menuInstance.open(player);
            }
        } else if (event.getSlot() == playerHeadSlot) {
            // Player head click - do nothing (just displays balance info)
            return;
        } else {
            handleItemClick(event);
        }
    }

    private void creatorPlayerInventoryClick(InventoryClickEvent e) {
        // Player inventory clicks: Only allow left-click to pick up items for placing into shop
        // Right-click and shift-click are blocked to prevent accidental edits
        // Items can only be edited once placed in the shop GUI
        
        boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                              e.getClick() == ClickType.SHIFT_LEFT || 
                              e.getClick() == ClickType.SHIFT_RIGHT;
        
        if (isEditClick) {
            // Block shift-clicks to prevent moving items unexpectedly
            e.setCancelled(true);
            return;
        }
        
        // Left-click = Allow normal item pickup for placing into shop GUI
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        ItemStack clickedItem = e.getCurrentItem();
        
        // Calculate button slots
        int bottomRowStart = (GUI.getRows() - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row (nether star)
        int prevSlot = centerSlot - 1; // Left of center (ghast tear)
        int nextSlot = centerSlot + 1; // Right of center (ghast tear)
        int backSlot = bottomRowStart + 8; // Bottom right corner
        int playerHeadSlot = bottomRowStart; // Bottom left corner
        
        boolean isShiftClick = e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT;
        boolean isRightClick = e.getClick() == ClickType.RIGHT;
        boolean isLeftClick = e.getClick() == ClickType.LEFT;
        
        // In editor mode, navigation buttons follow these controls:
        // - Left-click: Pick up and move (like any other item)
        // - Right-click: Open Item Editor GUI
        // - Shift+click: Navigate pages (or create new page if at end)
        
        // Handle navigation slots with editor-aware logic
        if (e.getSlot() == nextSlot || e.getSlot() == prevSlot) {
            if (isShiftClick) {
                // Shift+click = Navigate pages
                e.setCancelled(true);
                if (e.getSlot() == nextSlot) {
                    // If we're at the last page, create a new page
                    if (!GUI.hasNextPage()) {
                        GUI.addPage();
                        GUI.setPageRows(GUI.getPageCount() - 1, GUI.getRows());
                        player.sendMessage(ChatColor.GREEN + "Created new page " + GUI.getPageCount());
                    }
                    handleForwardButton();
                } else {
                    handleBackwardButton();
                }
                return;
            } else if (isRightClick) {
                // Right-click = Open Item Editor GUI (handled below with other items)
                // Don't return, fall through to the editor logic
            } else if (isLeftClick) {
                // Left-click = Allow picking up the item (don't cancel, let it fall through)
                // The item can be moved like any other
            }
        }
        
        // Center slot (page indicator) - Shift+click does nothing, Right-click opens editor, Left-click moves
        if (e.getSlot() == centerSlot) {
            if (isShiftClick) {
                e.setCancelled(true);
                return;
            }
            // Right-click and Left-click fall through to normal handling
        }
        
        // Back button - Shift+click goes back to menu, Right-click opens editor, Left-click moves
        if (e.getSlot() == backSlot) {
            if (isShiftClick) {
                e.setCancelled(true);
                if (menuInstance != null) {
                    hasClicked = true;
                    menuInstance.open(player);
                }
                return;
            }
            // Right-click and Left-click fall through to normal handling
        }
        
        // Player head slot - Shift+click does nothing, Right-click opens editor, Left-click moves
        if (e.getSlot() == playerHeadSlot) {
            if (isShiftClick) {
                e.setCancelled(true);
                return;
            }
            // Right-click and Left-click fall through to normal handling
        }
        
        // Right-click on an existing item = Open Item Editor GUI
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            if (isRightClick) {
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
            SchedulerUtil.runAtEntityLater(player, () -> {
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
        // YAML uses 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
        String pageKey = "Page" + GUI.getCurrentPage();
        shopItem.getPages().get(pageKey).getItems().remove(Integer.toString(slot));
        FileConfiguration shopConfig = GUIShop.getINSTANCE().getConfigManager().getShopConfig(shop);
        if (shopConfig == null) return;
        
        ConfigurationSection config = shopConfig.getConfigurationSection("pages." + pageKey + ".items") != null
                ? shopConfig.getConfigurationSection("pages." + pageKey + ".items")
                : shopConfig.createSection("pages." + pageKey + ".items");
        config.set(slot.toString(), null);
        GUIShop.getINSTANCE().getConfigManager().saveShopConfig(shop);
    }

    public void editShopItem(ItemStack itemStack, Integer slot) {
        // YAML uses 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
        String pageKey = "Page" + GUI.getCurrentPage();
        Item item = Item.parse(itemStack, slot, shop);
        shopItem.getPages().get(pageKey).getItems().put(Integer.toString(item.getSlot()), item);

        FileConfiguration shopConfig = GUIShop.getINSTANCE().getConfigManager().getShopConfig(shop);
        if (shopConfig == null) return;
        
        ConfigurationSection config = shopConfig.getConfigurationSection("pages." + pageKey + ".items") != null
                ? shopConfig.getConfigurationSection("pages." + pageKey + ".items")
                : shopConfig.createSection("pages." + pageKey + ".items");

        config.set(slot.toString(), item.serialize());

        GUIShop.getINSTANCE().getLogUtil().debugLog("Player edited item: " + item.getMaterial() + " slot: " + slot);
        GUIShop.getINSTANCE().getConfigManager().saveShopConfig(shop);

        hasClicked = false;
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
            // Normal mode - handle escape back to menu
            if (!Config.isDisableEscapeBack() && !hasClicked && !GUIShop.getINSTANCE().isReload) {
                SchedulerUtil.runAtEntityLater(player, () -> menuInstance.open(player), 1L);
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
            FileConfiguration shopConfig = GUIShop.getINSTANCE().getConfigManager().getShopConfig(shop);
            if (shopConfig == null) {
                GUIShop.getINSTANCE().getLogUtil().log("CREATOR SAVE: Shop config not found for " + shop);
                return;
            }
            
            // YAML uses 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
            String pageKey = "Page" + GUI.getCurrentPage();
            int inventorySize = GUI.getRows() * 9;
            
            // Calculate button slots to skip
            int bottomRowStart = (GUI.getRows() - 1) * 9;
            int centerSlot = bottomRowStart + 4; // Center of bottom row (nether star)
            int prevSlot = centerSlot - 1; // Left of center (ghast tear)
            int nextSlot = centerSlot + 1; // Right of center (ghast tear)
            int backSlot = bottomRowStart + 8; // Bottom right corner
            int playerHeadSlot = bottomRowStart; // Bottom left corner
            
            org.bukkit.configuration.ConfigurationSection config = 
                shopConfig.getConfigurationSection("pages." + pageKey + ".items");
            
            if (config == null) {
                config = shopConfig.createSection("pages." + pageKey + ".items");
            }
            
            boolean hasChanges = false;
            
            for (int slot = 0; slot < inventorySize; slot++) {
                // Skip navigation button slots, page indicator, and player head slot
                if (slot == nextSlot || slot == prevSlot || slot == centerSlot || slot == backSlot || slot == playerHeadSlot) {
                    continue;
                }
                
                ItemStack item = inventory.getItem(slot);
                String slotKey = String.valueOf(slot);
                
                if (item == null || item.getType().isAir()) {
                    // Check if this slot has a BLANK item in the cache - don't delete those
                    ShopPage cachedPage = shopItem != null ? shopItem.getPages().get(pageKey) : null;
                    Item cachedItem = cachedPage != null ? cachedPage.getItems().get(slotKey) : null;
                    
                    if (cachedItem != null && cachedItem.getItemType() == ItemType.BLANK) {
                        // Preserve BLANK items - they appear as empty slots intentionally
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Preserving BLANK item at slot " + slot);
                    } else if (config.contains(slotKey)) {
                        // Only delete if it wasn't a BLANK item
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
                        String itemPath = "pages." + pageKey + ".items." + slotKey;
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
                GUIShop.getINSTANCE().getConfigManager().saveShopConfig(shop);
                
                // Reload config from disk to ensure consistency
                GUIShop.getINSTANCE().getConfigManager().reloadShopConfig(shop);
                
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

        if (item.isResolveFailed()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "went-wrong", item.getResolveReason());
            return;
        }

        hasClicked = true;
        
        // Open the unified Transaction GUI for buying and selling
        new TransactionGui(item, this, player).loadInventory().open();
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
        com.pablo67340.guishop.api.DynamicPriceProvider dynamicProvider = 
            item.shouldUseDynamicPricing() ? GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING() : null;
        
        if (dynamicProvider != null && item.hasSellPrice()) {
            String itemString = item.getItemString();
            dynamicPricingUpdate = () -> dynamicProvider.buyItem(itemString, 1);
            priceToPay = dynamicProvider.calculateBuyPrice(itemString, 1, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal();
        }

        if (GUIShop.getINSTANCE().getMiscUtils().getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            // Execute commands - on Folia, console commands must run on the global region scheduler
            final Runnable dynamicUpdate = dynamicPricingUpdate;
            final Player commandPlayer = player;
            
            for (String str : item.getCommands()) {
                boolean isSudo = str.startsWith("sudo=");
                String rawCommand = isSudo ? str.substring(5).trim() : str.trim();
                // Remove leading slash if present
                if (rawCommand.startsWith("/")) {
                    rawCommand = rawCommand.substring(1);
                }
                String processedCommand = GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(rawCommand, player, item);
                
                // Check for unresolved placeholders (indicates wrong format was used)
                if (hasUnresolvedPlaceholders(processedCommand)) {
                    GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Unresolved placeholders in command: " + processedCommand);
                    GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Make sure to use %placeholder% or {placeholder} format (e.g., %player_name% or {player_name})");
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "command-error");
                    continue;
                }
                
                final String finalCommand = processedCommand;
                final boolean runAsPlayer = isSudo;
                
                if (runAsPlayer) {
                    // Player commands - use performCommand which handles Folia threading internally
                    try {
                        player.performCommand(finalCommand);
                    } catch (Exception e) {
                        GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Failed to execute player command: " + finalCommand);
                        GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Reason: " + e.getMessage());
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "command-error");
                    }
                } else {
                    // Console commands on Folia need to run on the global region scheduler
                    SchedulerUtil.runTask(() -> {
                        try {
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        } catch (Exception e) {
                            GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Failed to execute console command: " + finalCommand);
                            GUIShop.getINSTANCE().getLogUtil().log("[Command Error] Reason: " + e.getMessage());
                            SchedulerUtil.runAtEntity(commandPlayer, () -> {
                                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(commandPlayer, "command-error");
                            });
                        }
                    });
                }
            }
            
            if (Config.isSoundEnabled()) {
                try {
                    player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
                } catch (Exception ignored) {}
            }
            
            if (dynamicUpdate != null) {
                dynamicUpdate.run();
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
     * 
     * Note: This is intentionally conservative - we only flag common GUIShop placeholders
     * that should have been replaced. Other %% or {} patterns might be valid PlaceholderAPI
     * placeholders or command syntax.
     */
    private boolean hasUnresolvedPlaceholders(String command) {
        String lower = command.toLowerCase();
        
        // Check for GUIShop-specific placeholders that should have been replaced
        // Player placeholders (short and long forms)
        if (lower.contains("%player%") || lower.contains("{player}") ||
            lower.contains("%player_name%") || lower.contains("{player_name}") ||
            lower.contains("%player_uuid%") || lower.contains("{player_uuid}") || 
            lower.contains("%player_world%") || lower.contains("{player_world}") || 
            lower.contains("%player_balance%") || lower.contains("{player_balance}")) {
            return true;
        }
        
        // Price placeholders
        if (lower.contains("%buy_price%") || lower.contains("{buy_price}") ||
            lower.contains("%sell_price%") || lower.contains("{sell_price}")) {
            return true;
        }
        
        return false;
    }

    private void handleItemClick(InventoryClickEvent event) {
        /*
         * If the player has enough money to purchase the item, then allow them to.
         */
        // YAML uses 0-indexed pages (Page0, Page1, etc.)
        String pageKey = "Page" + GUI.getCurrentPage();
        GUIShop.getINSTANCE().getLogUtil().log("CLICK: Shop=" + shop + " Page=" + pageKey + " Slot=" + event.getSlot());
        
        // Check if page exists
        if (shopItem.getPages().get(pageKey) == null) {
            GUIShop.getINSTANCE().getLogUtil().log("CLICK: Page " + pageKey + " NOT FOUND in cache. Available: " + 
                String.join(", ", shopItem.getPages().keySet()));
            return;
        }
        
        Item item = shopItem.getPages().get(pageKey).getItems().get(Integer.toString(event.getSlot()));

        if (item == null) {
            GUIShop.getINSTANCE().getLogUtil().log("CLICK: Item NULL at slot " + event.getSlot() + 
                ". Available slots: " + String.join(", ", shopItem.getPages().get(pageKey).getItems().keySet()));
            return;
        }

        GUIShop.getINSTANCE().getLogUtil().log("CLICK: Found " + item.getMaterial() + " type=" + item.getItemType());
        
        // Create context for unified handler
        com.pablo67340.guishop.handler.ItemActionHandler.ClickContext context = 
            com.pablo67340.guishop.handler.ItemActionHandler.ClickContext.shop(
                this.shop,
                GUI.getCurrentPage(),
                () -> {
                    if (menuInstance != null) {
                        menuInstance.open(player);
                    }
                },
                item
            );
        
        // Handle item types
        if (null != item.getItemType()) {
            switch (item.getItemType()) {
                case SHOP, ITEM -> {
                    // ITEM type is treated as SHOP for backwards compatibility
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Opening Transaction GUI for " + item.getItemType() + " item");
                    shopItem(item, event);
                }
                case COMMAND -> {
                    // COMMAND items bypass TransactionGui - they execute commands directly with payment
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Processing COMMAND item directly");
                    commandItem(item);
                }
                case SHOP_SHORTCUT -> {
                    // Use unified handler for shop navigation
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Processing SHOP_SHORTCUT item via unified handler");
                    if (!com.pablo67340.guishop.handler.ItemActionHandler.handleClick(
                            player, event.getCurrentItem(), event.getSlot(), event.getClick(), context)) {
                        // Fallback to original shortcut handling
                        shopShortcut(item, event);
                    } else {
                        hasClicked = true;
                    }
                }
                case DUMMY -> {
                    // DUMMY items might have target-shop - use unified handler
                    if (com.pablo67340.guishop.handler.ItemActionHandler.handleClick(
                            player, event.getCurrentItem(), event.getSlot(), event.getClick(), context)) {
                        hasClicked = true;
                    }
                    // Otherwise do nothing (decorative)
                }
                case PAGE_LEFT -> {
                    // Navigate to previous page
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Processing PAGE_LEFT item");
                    handleBackwardButton();
                }
                case PAGE_RIGHT -> {
                    // Navigate to next page
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Processing PAGE_RIGHT item");
                    handleForwardButton();
                }
                case PAGE_STATUS, PLAYER_BALANCE -> {
                    // Display-only items - do nothing on click
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Display item " + item.getItemType() + " - no action");
                }
                default -> {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Item type " + item.getItemType() + " - no action");
                    // BLANK types - do nothing (decorative items)
                }
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
                    SchedulerUtil.runAtEntityLater(clickingPlayer, () -> this.menuInstance.openShop(clickingPlayer, shopName), 1L);

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
