package com.pablo67340.guishop.util;

import com.pablo67340.guishop.GUIShop;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Utility class for working with Persistent Data Containers (PDC).
 * This replaces the NBT API dependency with Bukkit's built-in PDC system (available since 1.14).
 */
public class PDCUtil {

    private static final String NAMESPACE = "guishop";

    // ============== Key Definitions ==============
    // Item pricing
    public static final NamespacedKey KEY_BUY_PRICE = new NamespacedKey(NAMESPACE, "buy_price");
    public static final NamespacedKey KEY_SELL_PRICE = new NamespacedKey(NAMESPACE, "sell_price");
    
    // Item display names
    public static final NamespacedKey KEY_SHOP_NAME = new NamespacedKey(NAMESPACE, "shop_name");
    public static final NamespacedKey KEY_BUY_NAME = new NamespacedKey(NAMESPACE, "buy_name");
    public static final NamespacedKey KEY_NAME = new NamespacedKey(NAMESPACE, "name");
    
    // Item type and target
    public static final NamespacedKey KEY_ITEM_TYPE = new NamespacedKey(NAMESPACE, "item_type");
    public static final NamespacedKey KEY_TARGET_SHOP = new NamespacedKey(NAMESPACE, "target_shop");
    
    // Spawner
    public static final NamespacedKey KEY_SPAWNER_MOB = new NamespacedKey(NAMESPACE, "spawner_mob");
    public static final NamespacedKey KEY_MOB_TYPE = new NamespacedKey(NAMESPACE, "mob_type");
    
    // Lore lines (stored as concatenated strings with :: separator)
    public static final NamespacedKey KEY_SHOP_LORE_LINES = new NamespacedKey(NAMESPACE, "shop_lore_lines");
    public static final NamespacedKey KEY_BUY_LORE_LINES = new NamespacedKey(NAMESPACE, "buy_lore_lines");
    public static final NamespacedKey KEY_LORE_LINES = new NamespacedKey(NAMESPACE, "lore_lines");
    
    // Commands
    public static final NamespacedKey KEY_COMMANDS = new NamespacedKey(NAMESPACE, "commands");
    
    // Enchantments (stored as string)
    public static final NamespacedKey KEY_ENCHANTMENTS = new NamespacedKey(NAMESPACE, "enchantments");
    
    // Custom NBT string (for user-defined NBT, though limited with PDC)
    public static final NamespacedKey KEY_CUSTOM_NBT = new NamespacedKey(NAMESPACE, "custom_nbt");
    
    // Quantity
    public static final NamespacedKey KEY_QUANTITY = new NamespacedKey(NAMESPACE, "quantity");
    
    // Skull UUID
    public static final NamespacedKey KEY_SKULL_UUID = new NamespacedKey(NAMESPACE, "skull_uuid");
    
    // Permission
    public static final NamespacedKey KEY_PERMISSION = new NamespacedKey(NAMESPACE, "permission");
    
    // Potion info
    public static final NamespacedKey KEY_POTION = new NamespacedKey(NAMESPACE, "potion");

    // ============== String Operations ==============
    
    /**
     * Set a string value in an item's PDC.
     * @param item The item to modify
     * @param key The namespaced key
     * @param value The value to set
     * @return The modified item (same reference)
     */
    public static ItemStack setString(ItemStack item, NamespacedKey key, String value) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get a string value from an item's PDC.
     * @param item The item to read from
     * @param key The namespaced key
     * @return The value, or null if not present
     */
    public static String getString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(key, PersistentDataType.STRING)) {
            return pdc.get(key, PersistentDataType.STRING);
        }
        return null;
    }
    
    /**
     * Check if an item has a string key.
     */
    public static boolean hasString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }
    
    /**
     * Remove a string key from an item's PDC.
     */
    public static ItemStack removeString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
        return item;
    }

    // ============== Double Operations ==============
    
    /**
     * Set a double value in an item's PDC.
     */
    public static ItemStack setDouble(ItemStack item, NamespacedKey key, double value) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get a double value from an item's PDC.
     */
    public static Double getDouble(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            return pdc.get(key, PersistentDataType.DOUBLE);
        }
        return null;
    }
    
    /**
     * Check if an item has a double key.
     */
    public static boolean hasDouble(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.DOUBLE);
    }
    
    /**
     * Remove a double key from an item's PDC.
     */
    public static ItemStack removeDouble(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
        return item;
    }

    // ============== Integer Operations ==============
    
    /**
     * Set an integer value in an item's PDC.
     */
    public static ItemStack setInteger(ItemStack item, NamespacedKey key, int value) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get an integer value from an item's PDC.
     */
    public static Integer getInteger(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            return pdc.get(key, PersistentDataType.INTEGER);
        }
        return null;
    }
    
    /**
     * Check if an item has an integer key.
     */
    public static boolean hasInteger(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }
    
    /**
     * Remove an integer key from an item's PDC.
     */
    public static ItemStack removeInteger(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
        return item;
    }

    // ============== Generic Operations ==============
    
    /**
     * Check if an item has any key (of any type).
     */
    public static boolean hasKey(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // Check common types
        return pdc.has(key, PersistentDataType.STRING) ||
               pdc.has(key, PersistentDataType.DOUBLE) ||
               pdc.has(key, PersistentDataType.INTEGER) ||
               pdc.has(key, PersistentDataType.BYTE) ||
               pdc.has(key, PersistentDataType.LONG);
    }
    
    /**
     * Remove a key from an item's PDC (type-agnostic).
     */
    public static ItemStack removeKey(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
        return item;
    }

    // ============== Convenience Methods ==============
    
    /**
     * Get the PDC from an ItemMeta.
     */
    public static PersistentDataContainer getPDC(ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer();
    }
    
    /**
     * Get the PDC from an ItemStack.
     */
    public static PersistentDataContainer getPDC(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer();
    }

    // ============== Legacy Key Support ==============
    // These create keys dynamically for backward compatibility with existing data patterns
    
    /**
     * Create a namespaced key with the GUIShop namespace.
     */
    public static NamespacedKey key(String name) {
        return new NamespacedKey(NAMESPACE, name.toLowerCase().replace(" ", "_"));
    }
}
