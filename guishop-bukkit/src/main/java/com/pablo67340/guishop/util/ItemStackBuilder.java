package com.pablo67340.guishop.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating ItemStacks with custom properties.
 */
public class ItemStackBuilder {

    private final ItemStack itemStack;
    private ItemMeta itemMeta;
    private List<String> lore;

    /**
     * Create a builder from a material.
     */
    public ItemStackBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
        this.lore = new ArrayList<>();
    }

    /**
     * Create a builder from an existing ItemStack.
     */
    public ItemStackBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasLore()) {
            this.lore = new ArrayList<>(itemMeta.getLore());
        } else {
            this.lore = new ArrayList<>();
        }
    }

    /**
     * Set the display name.
     */
    public ItemStackBuilder setName(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }
        return this;
    }

    /**
     * Set the amount.
     */
    public ItemStackBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * Add a line to the lore.
     */
    public ItemStackBuilder addLoreLine(String line) {
        lore.add(ChatColor.translateAlternateColorCodes('&', line));
        return this;
    }

    /**
     * Add multiple lines to the lore.
     */
    public ItemStackBuilder addLoreLines(List<String> lines) {
        for (String line : lines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return this;
    }

    /**
     * Set the entire lore.
     */
    public ItemStackBuilder setLore(List<String> lines) {
        lore.clear();
        for (String line : lines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return this;
    }

    /**
     * Clear the lore.
     */
    public ItemStackBuilder clearLore() {
        lore.clear();
        return this;
    }

    /**
     * Add an item flag.
     */
    public ItemStackBuilder addItemFlag(ItemFlag flag) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flag);
        }
        return this;
    }

    /**
     * Add multiple item flags.
     */
    public ItemStackBuilder addItemFlags(ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    /**
     * Add an enchantment.
     */
    public ItemStackBuilder addEnchantment(Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    /**
     * Set unbreakable.
     */
    public ItemStackBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }

    /**
     * Hide all flags (enchants, attributes, etc.)
     */
    public ItemStackBuilder hideAllFlags() {
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    /**
     * Build the final ItemStack.
     */
    public ItemStack build() {
        if (itemMeta != null) {
            if (!lore.isEmpty()) {
                itemMeta.setLore(lore);
            }
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
