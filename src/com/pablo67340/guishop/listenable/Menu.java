package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
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
     * List of out-of-bounds item warnings to display when menu opens.
     */
    private final java.util.List<String> outOfBoundsWarnings = new java.util.ArrayList<>();

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
        
        // Read menu-level rows (1-6, default 0 = auto-calculate)
        int menuConfiguredRows = menuSection.getInt("rows", 0);
        if (menuConfiguredRows > 0) {
            if (menuConfiguredRows < 1 || menuConfiguredRows > 6) {
                logMenuError("Menu has invalid 'rows: " + menuConfiguredRows + "'. Must be 1-6. Using auto-calculation.");
                menuConfiguredRows = 0;
            } else {
                menuItem.setConfiguredRows(menuConfiguredRows);
                GUIShop.getINSTANCE().getLogUtil().debugLog("Menu configured with " + menuConfiguredRows + " rows");
            }
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Loading items for Menu");
        int pageIndex = 0;
        int totalItems = 0;

        for (String pageKey : pagesConfig.getKeys(false)) {
            MenuPage page = new MenuPage();
            
            // Read per-page rows (overrides menu-level rows)
            int pageRows = pagesConfig.getInt(pageKey + ".rows", 0);
            if (pageRows > 0) {
                if (pageRows < 1 || pageRows > 6) {
                    logMenuError("Menu > " + pageKey + " has invalid 'rows: " + pageRows + "'. Must be 1-6.");
                    pageRows = 0;
                } else {
                    page.setConfiguredRows(pageRows);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Menu > " + pageKey + " configured with " + pageRows + " rows");
                }
            }
            
            // Determine effective rows for this page (page-level > menu-level > default 6)
            int effectiveRows = page.getConfiguredRows() > 0 ? page.getConfiguredRows() 
                : (menuItem.getConfiguredRows() > 0 ? menuItem.getConfiguredRows() : 6);
            
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

                // Check for required 'id' field - unless it's a navigation type that auto-generates its appearance
                String typeStr = itemSection.getString("type");
                boolean isNavigationType = typeStr != null && 
                    (typeStr.equalsIgnoreCase("PLAYER_BALANCE") || 
                     typeStr.equalsIgnoreCase("PAGE_LEFT") ||
                     typeStr.equalsIgnoreCase("PAGE_RIGHT") ||
                     typeStr.equalsIgnoreCase("PAGE_STATUS") ||
                     typeStr.equalsIgnoreCase("BACK"));
                
                if (!itemSection.contains("id") && !isNavigationType) {
                    logMenuError("Menu > " + pageKey + " > Slot '" + slotKey + "' is missing required 'id' field. Every item needs an 'id' property (except navigation types like PLAYER_BALANCE, PAGE_LEFT, etc.).");
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
                
                // Validate slot is within configured row bounds
                int maxSlot = effectiveRows * 9 - 1;
                if (slot > maxSlot) {
                    String warning = pageKey + " > Slot " + slot + " (" + item.getMaterial() + ") is out of bounds! Max slot for " + 
                        effectiveRows + " rows is " + maxSlot + ".";
                    logMenuError("Menu > " + warning);
                    outOfBoundsWarnings.add(warning);
                    continue; // Skip this item - don't add to menu
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
            int initialRows;
            
            // Use configured rows if specified, otherwise auto-calculate
            if (menuItem.getConfiguredRows() > 0) {
                initialRows = menuItem.getConfiguredRows();
                GUIShop.getINSTANCE().getLogUtil().debugLog("LOADMENU: Using configured rows: " + initialRows);
            } else {
                // Calculate dynamic row count based on HIGHEST SLOT NUMBER used
                // This ensures navigation items at high slots (like 45-53) are visible
                int highestSlot = 0;
                for (MenuPage page : menuItem.getPages().values()) {
                    for (Item item : page.getItems().values()) {
                        if (item.getItemType() != ItemType.BLANK && item.getSlot() > highestSlot) {
                            highestSlot = item.getSlot();
                        }
                    }
                }
                
                // Calculate rows needed to fit the highest slot (add 1 because slots are 0-indexed)
                // Minimum 4 rows, maximum 6 rows
                int rowsNeeded = (int) Math.ceil((highestSlot + 1) / 9.0);
                initialRows = Math.min(6, Math.max(4, rowsNeeded));
                GUIShop.getINSTANCE().getLogUtil().debugLog("LOADMENU: Auto-calculated rows: " + initialRows);
            }

            String title;
            if (hasMultiplePages()) {
                title = Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                        Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%", "1"));
            } else {
                title = Config.getTitlesConfig().getMenuTitle().replace("%page-number%", "");
            }
            
            // Add [Editor] prefix if in creator mode
            if (GUIShop.getCREATOR().contains(player.getUniqueId())) {
                title = ChatColor.DARK_PURPLE + "[Editor] " + ChatColor.RESET + title;
            }

            this.GUI = new PagedGui(initialRows, title);
            this.GUI.setDynamicRows(false);

            int pageIndex = 0;
            int maxPages = menuItem.getPages().size();
            for (Map.Entry<String, MenuPage> entry : menuItem.getPages().entrySet()) {
                // Add a new page
                GUI.addPage();

                // Use page-level rows if configured, otherwise menu-level, otherwise auto-calculated
                MenuPage menuPage = entry.getValue();
                int rows = menuPage.getConfiguredRows() > 0 ? menuPage.getConfiguredRows() 
                    : (menuItem.getConfiguredRows() > 0 ? menuItem.getConfiguredRows() : initialRows);
                GUI.setPageRows(pageIndex, rows);

                // Add items to the page
                for (Item item : entry.getValue().getItems().values()) {
                    if (item.getItemType() == ItemType.BLANK) {
                        continue;
                    }
                    
                    ItemStack itemStack = createItemForType(item, pageIndex, maxPages);
                    GUI.setItem(pageIndex, item.getSlot(), itemStack);
                }

                // Apply navigation buttons (legacy support - only adds if not already present via item types)
                applyButtons(pageIndex, maxPages, rows);
                pageIndex++;
            }
        }
    }

    private void applyButtons(int pageIndex, int maxPages, int rows) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("Applying buttons with page index: " + pageIndex + " max pages: " + maxPages);

        int bottomRowStart = (rows - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row
        int prevSlot = centerSlot - 1; // Left of center
        int nextSlot = centerSlot + 1; // Right of center
        int playerHeadSlot = bottomRowStart; // Bottom left corner
        
        // Get the page key for checking config items
        String pageKey = "Page" + pageIndex;
        
        // Only add hardcoded buttons if the user hasn't configured their own items of that type
        // This allows users to fully customize navigation via the editor by placing items with
        // the appropriate ItemType anywhere they want

        // Add page indicator in center - only if no PAGE_STATUS item exists on this page
        if (!hasConfigItemOfType(pageKey, ItemType.PAGE_STATUS)) {
            Item pageIndicatorConfig = Config.getButtonConfig().getPageIndicatorButton();
            ItemStack pageIndicator = pageIndicatorConfig.toItemStack(player, true);
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            String indicatorName = pageIndicatorConfig.hasShopName() ? pageIndicatorConfig.getShopName() : "&fPage %page% of %maxpage%";
            pageMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                indicatorName.replace("%page%", String.valueOf(pageIndex + 1)).replace("%maxpage%", String.valueOf(maxPages))));
            pageIndicator.setItemMeta(pageMeta);
            PDCUtil.setString(pageIndicator, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(pageIndex, centerSlot, pageIndicator);
        }

        // Only show navigation buttons if there are multiple pages
        if (maxPages > 1) {
            // Add forward button - only if not on last page and no PAGE_RIGHT item exists
            if (pageIndex < (maxPages - 1) && !hasConfigItemOfType(pageKey, ItemType.PAGE_RIGHT)) {
                ItemStack forwardButton = Config.getButtonConfig().getForwardButton().toItemStack(player, true);
                PDCUtil.setString(forwardButton, PDCUtil.KEY_GUI_ELEMENT, "true");
                GUIShop.getINSTANCE().getLogUtil().debugLog("Adding forward button at slot " + nextSlot);
                GUI.setItem(pageIndex, nextSlot, forwardButton);
            }

            // Add backward button - only if not on first page and no PAGE_LEFT item exists
            if (pageIndex > 0 && !hasConfigItemOfType(pageKey, ItemType.PAGE_LEFT)) {
                ItemStack backwardButton = Config.getButtonConfig().getBackwardButton().toItemStack(player, true);
                PDCUtil.setString(backwardButton, PDCUtil.KEY_GUI_ELEMENT, "true");
                GUIShop.getINSTANCE().getLogUtil().debugLog("Adding backward button at slot " + prevSlot);
                GUI.setItem(pageIndex, prevSlot, backwardButton);
            }
        }

        // Back button is now configured via BACK item type in menu.yml - no legacy fallback

        // Add player head with balance - only if no PLAYER_BALANCE item exists
        if (player != null && !hasConfigItemOfType(pageKey, ItemType.PLAYER_BALANCE)) {
            ItemStack playerHead = createPlayerHead();
            GUIShop.getINSTANCE().getLogUtil().debugLog("Adding player head at slot " + playerHeadSlot);
            GUI.setItem(pageIndex, playerHeadSlot, playerHead);
        }
    }
    
    /**
     * Check if there's a configured item at the given slot for the given page.
     */
    private boolean hasConfigItem(String pageKey, int slot) {
        if (menuItem == null || !menuItem.getPages().containsKey(pageKey)) {
            return false;
        }
        MenuPage page = menuItem.getPages().get(pageKey);
        Item item = page.getItems().get(String.valueOf(slot));
        return item != null && item.getItemType() != ItemType.BLANK;
    }
    
    /**
     * Check if there's ANY configured item of the given type on the page.
     * This is used to prevent adding hardcoded buttons when the user has
     * configured their own navigation items (at any slot).
     */
    private boolean hasConfigItemOfType(String pageKey, ItemType targetType) {
        if (menuItem == null || !menuItem.getPages().containsKey(pageKey)) {
            return false;
        }
        MenuPage page = menuItem.getPages().get(pageKey);
        for (Item item : page.getItems().values()) {
            if (item.getItemType() == targetType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an ItemStack based on the item's type, handling special navigation types.
     * 
     * @param item The item definition
     * @param pageIndex Current page index (0-indexed)
     * @param maxPages Total number of pages
     * @return The created ItemStack with appropriate dynamic content
     */
    private ItemStack createItemForType(Item item, int pageIndex, int maxPages) {
        if (item.getItemType() == null) {
            return item.toItemStack(player, true);
        }
        
        switch (item.getItemType()) {
            case PAGE_STATUS -> {
                // Page status indicator - show dynamic page info
                ItemStack itemStack = item.toItemStack(player, true);
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    String name = item.hasShopName() ? item.getShopName() : "&fPage %page% of %maxpage%";
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        name.replace("%page%", String.valueOf(pageIndex + 1))
                            .replace("%maxpage%", String.valueOf(maxPages))));
                    // Add editor hints in creator mode
                    boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());
                    if (isCreatorMode) {
                        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                        meta.setLore(lore);
                    }
                    // Set PDC on the meta BEFORE applying it back
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
                        org.bukkit.persistence.PersistentDataType.STRING, "true");
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
                        org.bukkit.persistence.PersistentDataType.STRING, ItemType.PAGE_STATUS.name());
                    itemStack.setItemMeta(meta);
                }
                return itemStack;
            }
            case PAGE_LEFT -> {
                // In editor mode, always show the button so it can be edited
                // In normal mode, hide if on first page
                boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());
                if (!isCreatorMode && pageIndex <= 0) {
                    // First page - don't show left navigation (return empty)
                    ItemStack empty = XMaterial.AIR.parseItem();
                    return empty;
                }
                ItemStack itemStack = item.toItemStack(player, true);
                // Set PDC after toItemStack to ensure it's not overwritten
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    // Add editor hints in creator mode
                    if (isCreatorMode) {
                        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                        lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Previous Page]");
                        meta.setLore(lore);
                    }
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
                        org.bukkit.persistence.PersistentDataType.STRING, "true");
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
                        org.bukkit.persistence.PersistentDataType.STRING, ItemType.PAGE_LEFT.name());
                    itemStack.setItemMeta(meta);
                }
                return itemStack;
            }
            case PAGE_RIGHT -> {
                // In editor mode, always show the button so it can be edited
                // In normal mode, hide if on last page
                boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());
                if (!isCreatorMode && pageIndex >= (maxPages - 1)) {
                    // Last page - don't show right navigation
                    ItemStack empty = XMaterial.AIR.parseItem();
                    return empty;
                }
                ItemStack itemStack = item.toItemStack(player, true);
                // Set PDC after toItemStack to ensure it's not overwritten
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    // Add editor hints in creator mode
                    if (isCreatorMode) {
                        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                        lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Next Page/Create]");
                        meta.setLore(lore);
                    }
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
                        org.bukkit.persistence.PersistentDataType.STRING, "true");
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
                        org.bukkit.persistence.PersistentDataType.STRING, ItemType.PAGE_RIGHT.name());
                    itemStack.setItemMeta(meta);
                }
                return itemStack;
            }
            case PLAYER_BALANCE -> {
                // Player head with balance - always use the player's head texture
                return createPlayerBalanceItem(item);
            }
            case BACK -> {
                // Back button - closes menu or goes back to parent
                ItemStack itemStack = item.toItemStack(player, true);
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    // Add editor hints in creator mode
                    boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());
                    if (isCreatorMode) {
                        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                        lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Close Menu]");
                        meta.setLore(lore);
                    }
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
                        org.bukkit.persistence.PersistentDataType.STRING, "true");
                    meta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
                        org.bukkit.persistence.PersistentDataType.STRING, ItemType.BACK.name());
                    itemStack.setItemMeta(meta);
                }
                return itemStack;
            }
            default -> {
                ItemStack itemStack = item.toItemStack(player, true);
                // Add editor hints for shop buttons in creator mode
                boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());
                if (isCreatorMode && item.hasTargetShop()) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
                        lore.add(ChatColor.DARK_GRAY + "[Shift+Click=Open Shop Editor]");
                        meta.setLore(lore);
                        itemStack.setItemMeta(meta);
                    }
                }
                return itemStack;
            }
        }
    }
    
    /**
     * Creates a player balance item (player head with balance info).
     * Uses the item's configured name/lore but always shows as player's head.
     */
    private ItemStack createPlayerBalanceItem(Item item) {
        ItemStack playerHead = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        skullMeta.setOwningPlayer(player);
        
        // Get player balance
        double balance = GUIShop.getINSTANCE().getMiscUtils().getECONOMY().getBalance(player);
        String formattedBalance = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
            + GUIShop.getINSTANCE().getMiscUtils().economyFormat(BigDecimal.valueOf(balance))
            + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
        
        // Use item's configured name from YAML (check 'name' field first, then 'shop-name', then default)
        String name;
        if (item.hasName()) {
            name = item.getName();
        } else if (item.hasShopName()) {
            name = item.getShopName();
        } else {
            name = Config.getTitlesConfig().getTransactionBalanceTitle();
        }
        skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            name.replace("%player%", player.getName()).replace("%balance%", formattedBalance)));
        
        // Use item's configured lore from YAML (check 'lore' field first, then 'shop-lore', then default)
        List<String> lore = new ArrayList<>();
        List<String> sourceLore;
        if (item.getLore() != null && !item.getLore().isEmpty()) {
            sourceLore = item.getLore();
        } else if (item.getShopLore() != null && !item.getShopLore().isEmpty()) {
            sourceLore = item.getShopLore();
        } else {
            sourceLore = null;
        }
        
        if (sourceLore != null) {
            for (String line : sourceLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    line.replace("%player%", player.getName()).replace("%balance%", formattedBalance)));
            }
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                Config.getTitlesConfig().getTransactionBalanceLore().replace("%balance%", formattedBalance)));
        }
        
        // Add editor hints in creator mode
        boolean isCreatorMode = GUIShop.getCREATOR().contains(player.getUniqueId());
        if (isCreatorMode) {
            lore.add(ChatColor.DARK_GRAY + "[Editor: Left=Move, Right=Edit]");
        }
        skullMeta.setLore(lore);
        
        // Set PDC on the meta before applying
        skullMeta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
            org.bukkit.persistence.PersistentDataType.STRING, "true");
        skullMeta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
            org.bukkit.persistence.PersistentDataType.STRING, ItemType.PLAYER_BALANCE.name());
        
        playerHead.setItemMeta(skullMeta);
        return playerHead;
    }

    /**
     * Creates the player head showing their balance (legacy method for applyButtons).
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
     * Inherits navigation buttons (PAGE_LEFT, PAGE_RIGHT, PAGE_STATUS, PLAYER_BALANCE, BACK)
     * from the source page to the target page. This is used when creating new pages in editor mode.
     */
    private void inheritNavigationButtons(int sourcePageIndex, int targetPageIndex) {
        String sourcePageKey = "Page" + sourcePageIndex;
        String targetPageKey = "Page" + targetPageIndex;
        
        MenuItem menuItem = GUIShop.getINSTANCE().getLoadedMenu();
        if (menuItem == null || !menuItem.getPages().containsKey(sourcePageKey)) {
            return;
        }
        
        MenuPage sourcePage = menuItem.getPages().get(sourcePageKey);
        
        // Create the target page if it doesn't exist
        if (!menuItem.getPages().containsKey(targetPageKey)) {
            menuItem.getPages().put(targetPageKey, new MenuPage());
        }
        MenuPage targetPage = menuItem.getPages().get(targetPageKey);
        
        // Copy navigation items from source to target
        for (Map.Entry<String, Item> entry : sourcePage.getItems().entrySet()) {
            Item item = entry.getValue();
            if (item.getItemType() != null && item.getItemType().isNavigationType()) {
                // Copy the item to the new page
                String slotKey = entry.getKey();
                int slotNum = Integer.parseInt(slotKey);
                
                Item copiedItem = new Item();
                copiedItem.setSlot(slotNum); // Important: Set the slot!
                copiedItem.setMaterial(item.getMaterial());
                copiedItem.setItemType(item.getItemType());
                copiedItem.setShopName(item.getShopName());
                copiedItem.setShopLore(item.getShopLore() != null ? new ArrayList<>(item.getShopLore()) : null);
                copiedItem.setLore(item.getLore() != null ? new ArrayList<>(item.getLore()) : null);
                targetPage.getItems().put(slotKey, copiedItem);
                
                // Also set the item in the GUI
                ItemStack itemStack = createItemForType(copiedItem, targetPageIndex, GUI.getPageCount());
                if (itemStack != null && !itemStack.getType().isAir()) {
                    GUI.setItem(targetPageIndex, slotNum, itemStack);
                }
            }
        }
        
        GUIShop.getINSTANCE().getLogUtil().debugLog("Inherited navigation buttons from " + sourcePageKey + " to " + targetPageKey);
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

        // Check if in creator mode
        if (GUIShop.getCREATOR().contains(player.getUniqueId())) {
            // Creator mode - allow editing items including navigation buttons
            GUI.setTopClickHandler(this::creatorTopInventoryClick);
            GUI.setBottomClickHandler(this::creatorPlayerInventoryClick);
            GUI.setAllowTopInventoryClick(true);
            GUI.setAllowBottomInventoryClick(true);
        } else {
            // Normal mode - use standard click handler
            GUI.setTopClickHandler(this::onShopClick);
            GUI.setBottomClickHandler((e) -> e.setCancelled(true));
            GUI.setAllowTopInventoryClick(false);
            GUI.setAllowBottomInventoryClick(false);
        }
        
        GUI.setCloseHandler(this::onClose);
        GUI.show(player);
        
        // Display out-of-bounds warnings to admins
        if (!outOfBoundsWarnings.isEmpty() && player.hasPermission("guishop.admin")) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "[GUIShop] " + ChatColor.YELLOW + 
                outOfBoundsWarnings.size() + " item(s) out of bounds in menu.yml:");
            for (String warning : outOfBoundsWarnings) {
                player.sendMessage(ChatColor.RED + "  - " + warning);
            }
            player.sendMessage(ChatColor.GRAY + "These items are not displayed. Increase 'rows' or reduce slot numbers.");
        }
    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop,
     * if so, run logic.
     */
    private void onShopClick(InventoryClickEvent event) {
        Player clickingPlayer = (Player) event.getWhoClicked();

        event.setCancelled(true);

        int bottomRowStart = (GUI.getRows() - 1) * 9;
        int centerSlot = bottomRowStart + 4; // Center of bottom row (nether star)
        int prevSlot = centerSlot - 1; // Left of center (ghast tear)
        int nextSlot = centerSlot + 1; // Right of center (ghast tear)
        int backSlot = bottomRowStart + 8; // Bottom right corner
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
        } else if (event.getSlot() == backSlot) {
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
                String newTitle = ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                                Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%",
                                        Integer.toString(newPage))));
                // Add [Editor] prefix if in creator mode
                if (GUIShop.getCREATOR().contains(player.getUniqueId())) {
                    newTitle = ChatColor.DARK_PURPLE + "[Editor] " + ChatColor.RESET + newTitle;
                }
                GUI.setTitle(newTitle);
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
                String newTitle = ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getMenuTitle().replace("%page-number%",
                                Config.getTitlesConfig().getMenuShopPageNumber().replace("%number%",
                                        Integer.toString(newPage))));
                // Add [Editor] prefix if in creator mode
                if (GUIShop.getCREATOR().contains(player.getUniqueId())) {
                    newTitle = ChatColor.DARK_PURPLE + "[Editor] " + ChatColor.RESET + newTitle;
                }
                GUI.setTitle(newTitle);
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
        
        // Handle navigation types directly (they need access to the GUI instance)
        if (clickedItem.getItemType() != null) {
            switch (clickedItem.getItemType()) {
                case PAGE_LEFT -> {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: PAGE_LEFT item");
                    handleBackwardButton(clickingPlayer);
                    return;
                }
                case PAGE_RIGHT -> {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: PAGE_RIGHT item");
                    handleForwardButton(clickingPlayer);
                    return;
                }
                case PAGE_STATUS, PLAYER_BALANCE -> {
                    // Display-only items - do nothing
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Display item " + clickedItem.getItemType());
                    return;
                }
                case BACK -> {
                    // Back button behavior
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: BACK item");
                    clickingPlayer.closeInventory();
                    return;
                }
                case COMMAND -> {
                    // COMMAND items execute directly with payment - bypass unified handler
                    GUIShop.getINSTANCE().getLogUtil().debugLog("CLICK: Processing COMMAND item directly from menu");
                    executeCommandItem(clickingPlayer, clickedItem);
                    return;
                }
                default -> {
                    // Continue to unified handler
                }
            }
        }
        
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
    
    /**
     * Execute a COMMAND type item with proper payment handling.
     * This method handles command items placed in the menu.
     */
    private void executeCommandItem(Player player, Item item) {
        // Check permission
        if (item.hasPermission()) {
            com.pablo67340.guishop.definition.Permission permission = item.getPermission();
            if (permission.doesntHavePermission(player)) {
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-item-permission");
                return;
            }
        }

        BigDecimal priceToPay;

        Runnable dynamicPricingUpdate = null;

        // Dynamic pricing support
        com.pablo67340.guishop.api.DynamicPriceProvider dynamicProvider = 
            item.shouldUseDynamicPricing() ? GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING() : null;
        
        if (dynamicProvider != null && item.hasSellPrice()) {
            String itemString = item.getItemString();
            dynamicPricingUpdate = () -> dynamicProvider.buyItem(itemString, 1);
            priceToPay = dynamicProvider.calculateBuyPrice(itemString, 1, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal();
        }

        // Process payment and execute commands
        if (GUIShop.getINSTANCE().getMiscUtils().getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            // Execute commands - on Folia, console commands must run on the global region scheduler
            final Runnable dynamicUpdate = dynamicPricingUpdate;
            final Player commandPlayer = player;
            
            for (String str : item.getCommands()) {
                String rawCommand = str.trim();
                // Remove leading slash if present
                if (rawCommand.startsWith("/")) {
                    rawCommand = rawCommand.substring(1);
                }
                String processedCommand = GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(rawCommand, player, item);
                
                final String finalCommand = processedCommand;
                
                if (item.isSudo()) {
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
                            org.bukkit.Bukkit.getServer().dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), finalCommand);
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

            GUIShop.getINSTANCE().getLogUtil().transactionLog("Player " + player.getName() + " bought command " + item.getMaterial() + " from menu for " + priceToPay.toPlainString() + " money!");
            
            // Track purchase statistics
            com.pablo67340.guishop.statistics.StatisticsManager statsManager = com.pablo67340.guishop.statistics.StatisticsManager.getInstance();
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
        
        // Check if the clicked item is a pagination button or has a target shop
        String itemTypeStr = clickedItem != null ? PDCUtil.getString(clickedItem, PDCUtil.KEY_ITEM_TYPE) : null;
        String targetShop = clickedItem != null ? PDCUtil.getString(clickedItem, PDCUtil.KEY_TARGET_SHOP) : null;
        
        boolean isPageButton = itemTypeStr != null && 
            (itemTypeStr.equals(ItemType.PAGE_LEFT.name()) || itemTypeStr.equals(ItemType.PAGE_RIGHT.name()));
        boolean isShopButton = targetShop != null && !targetShop.isEmpty();
        
        // Shift+click on PAGE_LEFT/PAGE_RIGHT = Actually navigate pages (so you can edit other pages)
        if (isPageButton && (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
            e.setCancelled(true);
            if (itemTypeStr != null && itemTypeStr.equals(ItemType.PAGE_LEFT.name())) {
                handleBackwardButton(player);
            } else {
                // PAGE_RIGHT - either navigate to next page or CREATE a new page
                if (!GUI.hasNextPage()) {
                    // Create a new page
                    int newPageNumber = GUI.getPageCount();
                    GUI.addPage();
                    GUI.setPageRows(newPageNumber, GUI.getRows());
                    
                    // Inherit navigation buttons from current page
                    int currentPage = GUI.getCurrentPage();
                    inheritNavigationButtons(currentPage, newPageNumber);
                    
                    player.sendMessage(ChatColor.GREEN + "Created new page " + (newPageNumber + 1));
                }
                handleForwardButton(player);
            }
            return;
        }
        
        // Shift+click on a shop button = Open that shop in editor mode
        if (isShopButton && (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
            e.setCancelled(true);
            hasClicked = true;
            
            // Open the target shop in editor mode
            Shop openShop = new Shop(player, targetShop, this);
            openShop.loadItems(false);
            if (!openShop.open(player)) {
                player.sendMessage(ChatColor.RED + "Could not open shop: " + targetShop);
            }
            return;
        }
        
        // Shift+click on BACK button = Close menu (execute the back action)
        boolean isBackButton = itemTypeStr != null && itemTypeStr.equals(ItemType.BACK.name());
        if (isBackButton && (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)) {
            e.setCancelled(true);
            hasClicked = true;
            player.closeInventory();
            return;
        }
        
        // Right-click on an existing item = Open Item Editor GUI
        boolean isEditClick = e.getClick() == ClickType.RIGHT;
        
        // Right-click on an existing item = Open Item Editor GUI
        if (clickedItem != null && !clickedItem.getType().isAir() && isEditClick) {
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
        
        // In creator mode, ALL items can be moved with left-click
        // Navigation buttons are no longer blocked - they're just regular items that can be edited/moved
        
        // Left-click = Allow normal item pickup/placement for rearranging menu layout
        // Items are saved when the inventory is closed
        // No need to cancel - let the vanilla inventory interaction happen
        
        // Track slot changes for saving later
        if (e.getClick() == ClickType.LEFT && clickedItem != null && !clickedItem.getType().isAir()) {
            int slot = e.getSlot();
            // Run after the event to check if item was moved
            SchedulerUtil.runAtEntityLater(player, () -> {
                ItemStack itemAfter = e.getInventory().getItem(slot);
                if (itemAfter == null || itemAfter.getType().isAir()) {
                    // Item was picked up from this slot
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Menu item picked up from slot: " + slot);
                }
            }, 1L);
        }
        
        // Left-click with cursor item onto empty slot = Place item
        if ((clickedItem == null || clickedItem.getType().isAir()) && e.getCursor() != null && !e.getCursor().getType().isAir()) {
            int slot = e.getSlot();
            // Run after the event to get the placed item
            SchedulerUtil.runAtEntityLater(player, () -> {
                ItemStack placedItem = e.getInventory().getItem(slot);
                if (placedItem != null) {
                    GUIShop.getINSTANCE().getLogUtil().debugLog("Menu item placed at slot: " + slot);
                }
            }, 1L);
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
            int backSlot = bottomRowStart + 8; // Bottom right corner
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
