package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.*;
import com.pablo67340.guishop.gui.PagedGui;
import com.pablo67340.guishop.util.NameUtil;
import com.pablo67340.guishop.util.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.pablo67340.guishop.util.SchedulerUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
     * Load the menu items.
     * 
     * If the menu is not in cache, it will be loaded from config.
     * If preLoad is false, the GUI will be (re)created to display the items.
     *
     * @param preLoad True = only load data into cache. False = also create GUI.
     */
    public void loadItems(Boolean preLoad) {
        // Always reset GUI when not preloading - ensures fresh render with current data
        if (!preLoad) {
            this.GUI = null;
        }
        
        if (GUIShop.getINSTANCE().getLoadedMenu() == null) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("MENU: Loading from config (not in cache)");
            loadMenuFromConfig(preLoad);
        } else {
            GUIShop.getINSTANCE().getLogUtil().debugLog("MENU: Loading from cache");
            menuItem = GUIShop.getINSTANCE().getLoadedMenu();
            menuItem.determineHighestSlots();
            if (!preLoad) {
                loadMenu();
            }
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
            createFallbackMenu("Missing 'Menu' section in menu.yml");
            return;
        }

        ConfigurationSection pagesConfig = menuSection.getConfigurationSection("pages");
        if (pagesConfig == null) {
            logMenuError("menu.yml is missing 'Menu.pages' section. Check your indentation.");
            logMenuError("Expected format:\n  Menu:\n    pages:\n      Page0:\n        items:");
            createFallbackMenu("Missing 'pages' section in menu.yml");
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

            // Use the actual YAML page key (Page1, Page2, etc.) for cache consistency
            menuItem.getPages().put(pageKey, page);
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loaded " + pageKey + " with " + itemsLoaded + " items.");
            totalItems += itemsLoaded;
            pageIndex++;
        }

        if (menuItem.getPages().isEmpty()) {
            logMenuError("Menu loaded with 0 pages. Your menu.yml may be empty or incorrectly formatted.");
            createFallbackMenu("Menu has 0 pages - check menu.yml");
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
     * Create a fallback menu with a BARRIER item when config loading fails.
     * This prevents the plugin from crashing and allows /gs reload to still work.
     */
    private void createFallbackMenu(String errorReason) {
        GUIShop.getINSTANCE().getLogUtil().log("[Fallback] Creating fallback menu due to config error: " + errorReason);
        
        menuItem = new MenuItem();
        MenuPage fallbackPage = new MenuPage();
        
        // Create a fallback BARRIER item explaining the error
        Item fallbackItem = new Item();
        fallbackItem.setMaterial("BARRIER");
        fallbackItem.setSlot(13); // Center of first row
        fallbackItem.setName("&c&lConfig Error");
        fallbackItem.setItemType(ItemType.DUMMY);
        
        List<String> errorLore = new ArrayList<>();
        errorLore.add("&7" + errorReason);
        errorLore.add("");
        errorLore.add("&eCheck your menu.yml file");
        errorLore.add("&eand run &f/gs reload");
        fallbackItem.setLore(errorLore);
        
        fallbackPage.getItems().put("13", fallbackItem);
        fallbackPage.setHighestSlot(13);
        menuItem.getPages().put("Page0", fallbackPage);
        
        GUIShop.getINSTANCE().setLoadedMenu(menuItem);
    }

    /**
     * Log a menu configuration error with consistent formatting.
     */
    private void logMenuError(String message) {
        GUIShop.getINSTANCE().getLogUtil().log("[Menu Config Error] " + message);
    }

    private void loadMenu() {
        if (this.GUI == null) {
            // Calculate dynamic row count based on item count (7 items per row with side padding)
            int maxItemsOnPage = 0;
            for (MenuPage page : menuItem.getPages().values()) {
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

            String title;
            if (hasMultiplePages()) {
                title = Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                        Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%", "1"));
            } else {
                title = Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "");
            }

            this.GUI = new PagedGui(initialRows, title);
            this.GUI.setDynamicRows(false);

            int pageIndex = 0;
            for (Map.Entry<String, MenuPage> entry : menuItem.getPages().entrySet()) {
                // Add a new page
                GUI.addPage();

                int rows = initialRows;
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
        int bottomRowStart = (rows - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row
        int prevSlot = centerSlot - 1; // Left of center
        int nextSlot = centerSlot + 1; // Right of center
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);
        int playerHeadSlot = bottomRowStart; // Bottom left corner

        // Add page indicator in center - always shows (configurable via buttons.page-indicator)
        Item pageIndicatorConfig = Config.getButtonConfig().getPageIndicatorButton();
        ItemStack pageIndicator = pageIndicatorConfig.toItemStack(player, true);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        String indicatorName = pageIndicatorConfig.hasShopName() ? pageIndicatorConfig.getShopName() : "&fPage %page% of %maxpage%";
        pageMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            indicatorName.replace("%page%", String.valueOf(pageIndex + 1)).replace("%maxpage%", String.valueOf(maxPages))));
        pageIndicator.setItemMeta(pageMeta);
        PDCUtil.setString(pageIndicator, PDCUtil.KEY_GUI_ELEMENT, "true");
        GUI.setItem(pageIndex, centerSlot, pageIndicator);

        // Only show navigation buttons if there are multiple pages
        if (maxPages > 1) {
            // Add forward button - only if not on last page (configurable via buttons.forward)
            if (pageIndex < (maxPages - 1)) {
                ItemStack forwardButton = Config.getButtonConfig().getForwardButton().toItemStack(player, true);
                PDCUtil.setString(forwardButton, PDCUtil.KEY_GUI_ELEMENT, "true");
                GUIShop.getINSTANCE().getLogUtil().debugLog("Adding forward button at slot " + nextSlot);
                GUI.setItem(pageIndex, nextSlot, forwardButton);
            }

            // Add backward button - only if not on first page (configurable via buttons.backward)
            if (pageIndex > 0) {
                ItemStack backwardButton = Config.getButtonConfig().getBackwardButton().toItemStack(player, true);
                PDCUtil.setString(backwardButton, PDCUtil.KEY_GUI_ELEMENT, "true");
                GUIShop.getINSTANCE().getLogUtil().debugLog("Adding backward button at slot " + prevSlot);
                GUI.setItem(pageIndex, prevSlot, backwardButton);
            }
        }

        if (!Config.isDisableBackButton()) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding back button at slot " + backSlot);
            ItemStack backButtonItem = Config.getButtonConfig().backButton.toItemStack(player, true);
            PDCUtil.setString(backButtonItem, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(pageIndex, backSlot, backButtonItem);
        }

        // Add player head with balance in bottom left
        if (player != null) {
            ItemStack playerHead = createPlayerHead();
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

        // Don't call closeInventory() explicitly - openInventory() handles the transition
        // This prevents mouse position reset when switching between different sized inventories
        // The close handler will still be triggered automatically by Bukkit

        loadItems(false);

        // Reset click state flag when opening
        hasClicked = false;

        // Menu is NOT editable - use normal click handler for both normal and creator mode
        // In creator mode, clicking a shop button will open that shop for editing
        GUI.setTopClickHandler(this::onShopClick);
        GUI.setBottomClickHandler((e) -> e.setCancelled(true));
        GUI.setAllowTopInventoryClick(false);
        GUI.setAllowBottomInventoryClick(false);
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
        int bottomRowStart = (GUI.getRows() - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row (nether star)
        int prevSlot = centerSlot - 1; // Left of center (ghast tear)
        int nextSlot = centerSlot + 1; // Right of center (ghast tear)
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);
        int playerHeadSlot = bottomRowStart; // Bottom left corner

        // Next Button (ghast tear right of center)
        GUIShop.getINSTANCE().getLogUtil().debugLog("Clicked: " + event.getSlot());
        if (event.getSlot() == nextSlot) {
            handleForwardButton(clickingPlayer);
            // Backward Button (ghast tear left of center)
        } else if (event.getSlot() == prevSlot) {
            handleBackwardButton(clickingPlayer);
            // Center slot (nether star page indicator) - do nothing
        } else if (event.getSlot() == centerSlot) {
            return;
            // Back Button
        } else if (event.getSlot() == backSlot && !Config.isDisableBackButton()) {
            clickingPlayer.closeInventory();
        } else if (event.getSlot() == playerHeadSlot) {
            // Player head click - do nothing (just displays balance info)
            return;
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
        // YAML and cache use 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
        String pageKey = "Page" + GUI.getCurrentPage();
        if (!GUIShop.getINSTANCE().getLoadedMenu().getPages().containsKey(pageKey) 
                || !GUIShop.getINSTANCE().getLoadedMenu().getPages().get(pageKey).getItems().containsKey(((Integer) event.getSlot()).toString())) {
            return;
        }
        
        Item clickedItem = GUIShop.getINSTANCE().getLoadedMenu().getPages().get(pageKey).getItems().get(((Integer) event.getSlot()).toString());
        ItemStack itemStack = event.getCurrentItem();
        
        // Use unified item action handler
        // This allows ANY item type to work in the menu (SHOP, ITEM, COMMAND, etc.)
        com.pablo67340.guishop.handler.ItemActionHandler.ClickContext context = 
            com.pablo67340.guishop.handler.ItemActionHandler.ClickContext.menu(
                GUI.getCurrentPage(), 
                () -> this.open(clickingPlayer)
            );
        
        // Check permission for target shop
        if (clickedItem.hasTargetShop()) {
            String shopName = clickedItem.getTargetShop();
            if (!GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(clickingPlayer, "guishop.shop." + shopName.toLowerCase()) 
                    && !GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(clickingPlayer, "guishop.shop.*")) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(clickingPlayer, "no-permission");
                return;
            }
            if (clickedItem.isResolveFailed()) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(clickingPlayer, "open-shop-error", clickedItem.getResolveReason());
                return;
            }
        }
        
        // Try unified handler first
        if (com.pablo67340.guishop.handler.ItemActionHandler.handleClick(
                clickingPlayer, itemStack, event.getSlot(), event.getClick(), context)) {
            hasClicked = true;
            return;
        }
        
        // Fallback: original behavior for items with target-shop
        if (clickedItem.hasTargetShop()) {
            openShop(clickingPlayer, clickedItem.getTargetShop());
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
        // YAML and cache use 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
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
        // YAML and cache use 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
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
            
            // Auto-create target shop if it doesn't exist
            if (item.hasTargetShop()) {
                String targetShopName = item.getTargetShop();
                if (!GUIShop.getINSTANCE().getConfigManager().shopExists(targetShopName)) {
                    String shopTitle = "&a" + targetShopName + " Shop";
                    if (GUIShop.getINSTANCE().getConfigManager().createShop(targetShopName, shopTitle)) {
                        if (player != null) {
                            player.sendMessage(ChatColor.GREEN + "Created new shop: " + ChatColor.YELLOW + targetShopName + ".yml");
                        }
                        GUIShop.getINSTANCE().getLogUtil().log("Auto-created new shop file: " + targetShopName + ".yml");
                    }
                }
            }
        } catch (IOException ex) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    private void creatorPlayerInventoryClick(InventoryClickEvent e) {
        // Player inventory clicks: Only allow left-click to pick up items for placing into menu
        // Right-click and shift-click are blocked to prevent accidental edits
        // Items can only be edited once placed in the menu GUI
        
        boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                              e.getClick() == ClickType.SHIFT_LEFT || 
                              e.getClick() == ClickType.SHIFT_RIGHT;
        
        if (isEditClick) {
            // Block shift-clicks to prevent moving items unexpectedly
            e.setCancelled(true);
            return;
        }
        
        // Left-click = Allow normal item pickup for placing into menu GUI
    }

    private void creatorTopInventoryClick(InventoryClickEvent e) {
        ItemStack clickedItem = e.getCurrentItem();
        
        // Calculate button slots - these should still function as buttons, not be stealable
        int inventorySize = GUI.getRows() * 9;
        int bottomRowStart = (GUI.getRows() - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row (nether star)
        int prevSlot = centerSlot - 1; // Left of center (ghast tear)
        int nextSlot = centerSlot + 1; // Right of center (ghast tear)
        int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);
        int playerHeadSlot = bottomRowStart; // Bottom left corner
        
        // Handle pagination and back buttons (always cancel and process like normal)
        if (e.getSlot() == nextSlot) {
            e.setCancelled(true);
            handleForwardButton(player);
            return;
        } else if (e.getSlot() == prevSlot) {
            e.setCancelled(true);
            handleBackwardButton(player);
            return;
        } else if (e.getSlot() == centerSlot) {
            // Center slot (nether star page indicator) - don't allow modification
            e.setCancelled(true);
            return;
        } else if (e.getSlot() == backSlot && !Config.isDisableBackButton()) {
            e.setCancelled(true);
            player.closeInventory();
            return;
        } else if (e.getSlot() == playerHeadSlot) {
            // Player head slot - don't allow modification
            e.setCancelled(true);
            return;
        }
        
        // Right-click or Shift+click on an existing item = Open Item Editor GUI
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                                  e.getClick() == ClickType.SHIFT_LEFT || 
                                  e.getClick() == ClickType.SHIFT_RIGHT;
            
            if (isEditClick) {
                e.setCancelled(true);
                hasClicked = true;
                int currentPageForEditor = GUI.getCurrentPage();
                player.closeInventory();
                
                // Open the Item Editor GUI
                new com.pablo67340.guishop.listenable.editor.ItemEditorGui(
                    player, 
                    clickedItem, 
                    e.getSlot(), 
                    "Menu",
                    currentPageForEditor
                ).onSave(() -> {
                    // Reopen the menu after saving
                    // Cache was updated in saveToConfig(), now rebuild GUI with fresh data
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ONSAVE: Rebuilding menu GUI");
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    this.loadItems(false);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ONSAVE: loadItems complete, opening menu");
                    this.open(player);
                }).onCancel(() -> {
                    // Reopen the menu on cancel
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    this.open(player);
                }).open();
                return;
            }
        }
        
        // Left-click with cursor item onto empty slot = Place item (register it)
        if ((clickedItem == null || clickedItem.getType().isAir()) && e.getCursor() != null && !e.getCursor().getType().isAir()) {
            int slot = e.getSlot();
            // Run after the event to get the placed item
            SchedulerUtil.runAtEntityLater(player, () -> {
                ItemStack placedItem = e.getInventory().getItem(slot);
                if (placedItem != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("New menu item placed: " + placedItem.getType());
                    editMenuItem(placedItem, slot);
                }
            }, 1L);
            return;
        }
        
        // Left-click on an item - check if it's a shop button to open it
        // Menu items are NOT movable - only editable via right-click
        if (clickedItem != null && !clickedItem.getType().isAir() && e.getClick() == ClickType.LEFT) {
            e.setCancelled(true); // Always cancel - menu items are not movable
            
            // YAML and cache use 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
            String pageKey = "Page" + GUI.getCurrentPage();
            GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: Looking for item at " + pageKey + " slot " + e.getSlot());
            
            // Use local menuItem cache, not global loadedMenu
            if (menuItem != null && menuItem.getPages().containsKey(pageKey)) {
                Item item = menuItem.getPages().get(pageKey).getItems().get(String.valueOf(e.getSlot()));
                GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: Item found = " + (item != null ? item.getMaterial() : "null"));
                if (item != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: hasTargetShop = " + item.hasTargetShop() + 
                        " targetShop = " + item.getTargetShop());
                }
                if (item != null && item.hasTargetShop()) {
                    // This is a shop button - open the shop in creator mode
                    String shopName = item.getTargetShop();
                    GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: Opening shop " + shopName);
                    if (GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.shop." + shopName.toLowerCase()) 
                            || GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.shop.*")) {
                        hasClicked = true;
                        openShop(player, shopName);
                    } else {
                        GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: No permission for shop " + shopName);
                    }
                    return;
                }
            } else {
                GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: menuItem is null or page not found");
                if (menuItem != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("MENU CLICK: Available pages: " + 
                        String.join(", ", menuItem.getPages().keySet()));
                }
            }
            // Not a shop button - do nothing (item is not movable, use right-click to edit)
            return;
        }
        
        // Any other click on existing items should be cancelled (no moving allowed)
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            e.setCancelled(true);
        }
    }

    private void onClose(InventoryCloseEvent event) {
        // In creator mode for menu
        if (!hasClicked) {
            // Only save if we're not in the middle of opening an editor or shop
            saveCreatorInventory(event.getInventory());
        }
        // ALWAYS remove from creator mode when closing
        GUIShop.getCREATOR().remove(event.getPlayer().getUniqueId());
        GUIShop.getINSTANCE().getLogUtil().debugLog("ONCLOSE MENU: Removed player from creator mode");
    }
    
    /**
     * Save all items in the inventory when closing in creator mode.
     * This handles items that were placed/moved without going through the Item Editor.
     */
    private void saveCreatorInventory(org.bukkit.inventory.Inventory inventory) {
        try {
            // YAML and cache use 1-indexed pages (Page1, Page2), but GUI uses 0-indexed
            String pageKey = "Page" + GUI.getCurrentPage();
            int inventorySize = GUI.getRows() * 9;
            
            // Calculate button slots to skip
            int bottomRowStart = (GUI.getRows() - 1) * 9;
            int centerSlot = bottomRowStart + 4; // Center of bottom row (nether star)
            int prevSlot = centerSlot - 1; // Left of center (ghast tear)
            int nextSlot = centerSlot + 1; // Right of center (ghast tear)
            int backSlot = Math.max(0, calculateSlot(Config.getButtonConfig().getBackSlot(), inventorySize) - 1);
            int playerHeadSlot = bottomRowStart; // Bottom left corner
            
            org.bukkit.configuration.ConfigurationSection config = 
                GUIShop.getINSTANCE().getConfigManager().getMenuConfig()
                    .getConfigurationSection("Menu.pages." + pageKey + ".items");
            
            if (config == null) {
                config = GUIShop.getINSTANCE().getConfigManager().getMenuConfig()
                    .createSection("Menu.pages." + pageKey + ".items");
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
                    MenuPage cachedPage = menuItem != null ? menuItem.getPages().get(pageKey) : null;
                    Item cachedItem = cachedPage != null ? cachedPage.getItems().get(slotKey) : null;
                    
                    if (cachedItem != null && cachedItem.getItemType() == ItemType.BLANK) {
                        // Preserve BLANK items - they appear as empty slots intentionally
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Preserving BLANK menu item at slot " + slot);
                    } else if (config.contains(slotKey)) {
                        // Only delete if it wasn't a BLANK item
                        config.set(slotKey, null);
                        hasChanges = true;
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Removed menu item from slot " + slot);
                    }
                } else {
                    // Check if this is a new item (no GUIShop PDC data) or modified
                    String existingType = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
                    
                    // If no item type set, this is a freshly placed item
                    if (existingType == null) {
                        // Parse and save the new item as DUMMY (decoration in menu)
                        Item newItem = Item.parse(item, slot, null);
                        newItem.setItemType(ItemType.DUMMY);
                        
                        // Save item properties individually to ensure proper YAML structure
                        java.util.Map<String, Object> serialized = newItem.serialize();
                        String itemPath = "Menu.pages." + pageKey + ".items." + slotKey;
                        org.bukkit.configuration.file.FileConfiguration menuConfig = 
                            GUIShop.getINSTANCE().getConfigManager().getMenuConfig();
                        menuConfig.set(itemPath, null);
                        for (java.util.Map.Entry<String, Object> entry : serialized.entrySet()) {
                            menuConfig.set(itemPath + "." + entry.getKey(), entry.getValue());
                        }
                        
                        hasChanges = true;
                        GUIShop.getINSTANCE().getLogUtil().debugLog("CREATOR SAVE: Added new menu item " + item.getType() + " at slot " + slot);
                    }
                }
            }
            
            if (hasChanges) {
                GUIShop.getINSTANCE().getConfigManager().getMenuConfig()
                    .save(GUIShop.getINSTANCE().getConfigManager().getMenuFile());
                
                // Reload config from disk to ensure consistency
                GUIShop.getINSTANCE().getConfigManager().reloadMenuConfig();
                
                // Invalidate the menu cache
                GUIShop.getINSTANCE().setLoadedMenu(null);
                
                // Check all menu items for target-shops that don't exist and auto-create them
                checkAndCreateTargetShops();
                
                if (player != null) {
                    player.sendMessage(ChatColor.GREEN + "Menu changes saved!");
                }
            }
        } catch (Exception ex) {
            GUIShop.getINSTANCE().getLogUtil().log("Error saving creator menu inventory: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Check all menu items for target-shops and auto-create any that don't exist.
     */
    private void checkAndCreateTargetShops() {
        if (menuItem == null || menuItem.getPages() == null) return;
        
        for (MenuPage page : menuItem.getPages().values()) {
            if (page.getItems() == null) continue;
            
            for (Item item : page.getItems().values()) {
                if (item.hasTargetShop()) {
                    String targetShopName = item.getTargetShop();
                    if (!GUIShop.getINSTANCE().getConfigManager().shopExists(targetShopName)) {
                        String shopTitle = "&a" + targetShopName + " Shop";
                        if (GUIShop.getINSTANCE().getConfigManager().createShop(targetShopName, shopTitle)) {
                            if (player != null) {
                                player.sendMessage(ChatColor.GREEN + "Created new shop: " + ChatColor.YELLOW + targetShopName + ".yml");
                            }
                            GUIShop.getINSTANCE().getLogUtil().log("Auto-created new shop file: " + targetShopName + ".yml");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get the current page index (for compatibility with other classes).
     */
    public int getCurrentPage() {
        return GUI != null ? GUI.getCurrentPage() : 0;
    }
}
