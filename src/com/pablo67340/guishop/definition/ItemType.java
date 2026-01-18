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
    PLAYER_HEAD;
    
    /**
     * Check if this type is a transaction GUI slot type.
     */
    public boolean isTransactionType() {
        return switch (this) {
            case ITEM_DISPLAY, BUY_1, BUY_2, BUY_3, SELL_1, SELL_2, SELL_3, BACK, PLAYER_HEAD -> true;
            default -> false;
        };
    }
}
