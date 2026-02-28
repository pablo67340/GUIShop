package com.pablo67340.guishop.definition;

public enum ItemType {
    // Standard shop item types
    ITEM,
    COMMAND,
    DUMMY,
    SHOP,
    BLANK,
    SHOP_SHORTCUT,
    
    // Transaction GUI slot types
    ITEM_DISPLAY,
    BUY_1,
    BUY_2,
    BUY_3,
    SELL_1,
    SELL_2,
    SELL_3,
    BACK,
    PLAYER_HEAD,
    
    // Navigation/UI slot types (usable in shops and menus)
    PAGE_LEFT,
    PAGE_RIGHT,
    PAGE_STATUS,
    PLAYER_BALANCE;
    
    /**
     * Check if this type is a transaction GUI slot type.
     */
    public boolean isTransactionType() {
        return switch (this) {
            case ITEM_DISPLAY, BUY_1, BUY_2, BUY_3, SELL_1, SELL_2, SELL_3, BACK, PLAYER_HEAD -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this type is a navigation/pagination type.
     */
    public boolean isNavigationType() {
        return switch (this) {
            case PAGE_LEFT, PAGE_RIGHT, PAGE_STATUS, PLAYER_BALANCE, BACK -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this type is a purchasable item that should show buy/sell lore.
     * ITEM, SHOP, and COMMAND types are purchasable items.
     * SHOP_SHORTCUT links to other shops (not purchasable).
     * DUMMY, BLANK, and navigation types are not purchasable.
     */
    public boolean isPurchasable() {
        return switch (this) {
            case ITEM, SHOP, COMMAND -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this type is a shop/menu item type (not transaction-specific).
     */
    public boolean isShopMenuType() {
        return switch (this) {
            case ITEM, COMMAND, DUMMY, SHOP, BLANK, SHOP_SHORTCUT, PAGE_LEFT, PAGE_RIGHT, PAGE_STATUS, PLAYER_BALANCE, BACK -> true;
            default -> false;
        };
    }
}
