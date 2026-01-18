package com.pablo67340.guishop.handler;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.listenable.Menu;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.NameUtil;
import com.pablo67340.guishop.util.PDCUtil;
import com.pablo67340.guishop.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * Unified item action handler that processes clicks based on ItemType.
 * This allows any item type to work in any GUI context (Menu, Shop, Transaction).
 * 
 * The behavior is determined by the item's ItemType, not by which GUI it's in.
 * For example:
 * - A SHOP item in the Menu will open that shop
 * - A SHOP item in a Shop will also open that shop (shop within shop)
 * - An ITEM in the Menu would open TransactionGui for buying/selling
 * - A BUY_1 button anywhere will execute a buy action
 */
public class ItemActionHandler {

    /**
     * Context information about where the click originated.
     */
    public static class ClickContext {
        private final String sourceType; // "Menu", "Shop", "Transaction"
        private final String shopName;   // Current shop name (if in a shop)
        private final int page;          // Current page number
        private final Runnable onBack;   // Action to perform when going back
        private final Item itemMeta;     // The Item definition if available
        
        public ClickContext(String sourceType, String shopName, int page, Runnable onBack, Item itemMeta) {
            this.sourceType = sourceType;
            this.shopName = shopName;
            this.page = page;
            this.onBack = onBack;
            this.itemMeta = itemMeta;
        }
        
        public String getSourceType() { return sourceType; }
        public String getShopName() { return shopName; }
        public int getPage() { return page; }
        public Runnable getOnBack() { return onBack; }
        public Item getItemMeta() { return itemMeta; }
        
        public static ClickContext menu(int page, Runnable onBack) {
            return new ClickContext("Menu", null, page, onBack, null);
        }
        
        public static ClickContext shop(String shopName, int page, Runnable onBack, Item itemMeta) {
            return new ClickContext("Shop", shopName, page, onBack, itemMeta);
        }
        
        public static ClickContext transaction(String shopName, Runnable onBack, Item itemMeta) {
            return new ClickContext("Transaction", shopName, 0, onBack, itemMeta);
        }
    }

    /**
     * Handle a click on an item. This is the main entry point.
     * 
     * @param player The player who clicked
     * @param item The ItemStack that was clicked
     * @param slot The slot that was clicked
     * @param clickType The type of click
     * @param context Context about where the click originated
     * @return true if the click was handled, false if it should be passed through
     */
    public static boolean handleClick(Player player, ItemStack item, int slot, ClickType clickType, ClickContext context) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        // Get the ItemType from PDC
        ItemType itemType = getItemType(item);
        
        GUIShop.getINSTANCE().getLogUtil().debugLog(
            "ItemActionHandler: type=" + itemType + " source=" + context.getSourceType() + 
            " shop=" + context.getShopName() + " slot=" + slot);
        
        // Handle based on ItemType
        return switch (itemType) {
            case ITEM -> handleShopItem(player, item, context);
            case COMMAND -> handleCommandItem(player, item, context);
            case DUMMY -> handleDummyItem(player, item, context);
            case SHOP, SHOP_SHORTCUT -> handleShopNavigation(player, item, context);
            case BLANK -> false; // Do nothing
            
            // Transaction GUI types
            case ITEM_DISPLAY, PLAYER_HEAD -> false; // Display only
            case BUY_1 -> handleBuyAction(player, 0, context);
            case BUY_2 -> handleBuyAction(player, 1, context);
            case BUY_3 -> handleBuyAction(player, 2, context);
            case SELL_1 -> handleSellAction(player, 0, context);
            case SELL_2 -> handleSellAction(player, 1, context);
            case SELL_3 -> handleSellAction(player, 2, context);
            case BACK -> handleBackAction(player, context);
        };
    }
    
    /**
     * Get the ItemType from an item's PDC.
     */
    public static ItemType getItemType(ItemStack item) {
        String typeStr = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
        if (typeStr == null || typeStr.isEmpty()) {
            return ItemType.ITEM; // Default to ITEM for backwards compatibility
        }
        
        try {
            return ItemType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ItemType.ITEM;
        }
    }
    
    /**
     * Handle a shop ITEM click - opens TransactionGui for buy/sell.
     * Note: This is typically handled by the Shop class directly for proper TransactionGui integration.
     * This handler is mainly for items placed in unusual contexts (like Menu).
     */
    private static boolean handleShopItem(Player player, ItemStack item, ClickContext context) {
        // For now, return false to let the calling GUI handle this
        // The Shop class has the proper TransactionGui integration with Shop reference
        // Future: Could create a standalone TransactionGui that works without Shop reference
        return false;
    }
    
    /**
     * Handle a COMMAND item click - executes commands.
     */
    private static boolean handleCommandItem(Player player, ItemStack item, ClickContext context) {
        String commandsStr = PDCUtil.getString(item, PDCUtil.KEY_COMMANDS);
        if (commandsStr == null || commandsStr.isEmpty()) {
            return false;
        }
        
        // Check permission
        String permission = PDCUtil.getString(item, PDCUtil.KEY_PERMISSION);
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "Permission", permission);
            return true;
        }
        
        // Execute commands
        String[] commands = commandsStr.split("::");
        for (String command : commands) {
            String parsed = command.replace("%player%", player.getName())
                                   .replace("%uuid%", player.getUniqueId().toString());
            
            if (parsed.startsWith("[player]")) {
                // Run as player
                player.performCommand(parsed.substring(8).trim());
            } else if (parsed.startsWith("[op]")) {
                // Run as op
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(parsed.substring(4).trim());
                } finally {
                    player.setOp(wasOp);
                }
            } else if (parsed.startsWith("[console]")) {
                // Run as console
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed.substring(9).trim());
            } else {
                // Default: run as console
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
        
        return true;
    }
    
    /**
     * Handle a DUMMY item click - may open target shop if configured.
     */
    private static boolean handleDummyItem(Player player, ItemStack item, ClickContext context) {
        String targetShop = PDCUtil.getString(item, PDCUtil.KEY_TARGET_SHOP);
        
        if (targetShop != null && !targetShop.isEmpty()) {
            // DUMMY with target-shop acts like a shop navigation
            return openShop(player, targetShop, context);
        }
        
        // Regular DUMMY - do nothing
        return false;
    }
    
    /**
     * Handle SHOP or SHOP_SHORTCUT navigation.
     */
    private static boolean handleShopNavigation(Player player, ItemStack item, ClickContext context) {
        String targetShop = PDCUtil.getString(item, PDCUtil.KEY_TARGET_SHOP);
        
        if (targetShop == null || targetShop.isEmpty()) {
            // No target shop configured
            return false;
        }
        
        return openShop(player, targetShop, context);
    }
    
    /**
     * Open a shop by name.
     */
    private static boolean openShop(Player player, String shopName, ClickContext context) {
        String nearestShop = NameUtil.nearestShop(shopName);
        
        if (nearestShop == null) {
            player.sendMessage(ChatColor.RED + "Shop '" + shopName + "' does not exist.");
            return true;
        }
        
        // Create menu instance for shop navigation
        Menu menuInstance = new Menu(player);
        Shop shop = new Shop(player, nearestShop, menuInstance);
        shop.loadItems(false);
        
        player.closeInventory();
        SchedulerUtil.runAtEntityLater(player, () -> {
            shop.open(player);
        }, 1L);
        
        return true;
    }
    
    /**
     * Handle buy button actions.
     */
    private static boolean handleBuyAction(Player player, int quantityIndex, ClickContext context) {
        // This is typically called from TransactionGui
        // The actual buy logic should be delegated to TransactionGui or ItemUtil
        // For now, return false to let TransactionGui handle it
        return false;
    }
    
    /**
     * Handle sell button actions.
     */
    private static boolean handleSellAction(Player player, int quantityIndex, ClickContext context) {
        // This is typically called from TransactionGui
        // The actual sell logic should be delegated to TransactionGui or ItemUtil
        // For now, return false to let TransactionGui handle it
        return false;
    }
    
    /**
     * Handle back button action.
     */
    private static boolean handleBackAction(Player player, ClickContext context) {
        Runnable onBack = context.getOnBack();
        
        if (onBack != null) {
            player.closeInventory();
            SchedulerUtil.runAtEntityLater(player, onBack, 1L);
            return true;
        }
        
        // Default: go back to menu
        player.closeInventory();
        SchedulerUtil.runAtEntityLater(player, () -> {
            new Menu(player).open(player);
        }, 1L);
        
        return true;
    }
    
    /**
     * Check if an item has any action associated with it.
     */
    public static boolean hasAction(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        ItemType type = getItemType(item);
        
        return switch (type) {
            case BLANK, PLAYER_HEAD, ITEM_DISPLAY -> false;
            case DUMMY -> {
                // DUMMY has action only if it has target-shop
                String targetShop = PDCUtil.getString(item, PDCUtil.KEY_TARGET_SHOP);
                yield targetShop != null && !targetShop.isEmpty();
            }
            default -> true;
        };
    }
}
