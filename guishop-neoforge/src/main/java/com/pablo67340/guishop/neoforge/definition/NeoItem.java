package com.pablo67340.guishop.neoforge.definition;

import com.pablo67340.guishop.definition.ItemDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The NeoForge half of a shop item: everything that needs the Minecraft
 * registries or an {@link ItemStack}.
 * <p>
 * Mirrors {@code Item extends ItemDefinition} in the Bukkit module.
 */
public final class NeoItem extends ItemDefinition {

    private net.minecraft.world.item.Item resolved;
    private ItemStack cachedStack;

    /**
     * Resolves this item's configured material to a Minecraft item.
     * <p>
     * Shop configs name materials the way Bukkit does, e.g. {@code OAK_LOG},
     * which maps onto {@code minecraft:oak_log}. A namespaced id is also
     * accepted so modded items can be used.
     */
    public net.minecraft.world.item.Item resolveItem() {
        if (resolved != null) {
            return resolved;
        }

        ResourceLocation key = ItemStackFactory.namespaced(getMaterial());
        if (key == null) {
            setResolveFailed("'" + getMaterial() + "' is not a valid item id");
            resolved = Items.AIR;
        } else if (!BuiltInRegistries.ITEM.containsKey(key)) {
            setResolveFailed("no item registered as '" + key + "'");
            resolved = Items.AIR;
        } else {
            resolved = BuiltInRegistries.ITEM.get(key);
        }

        return resolved;
    }

    /**
     * The stack shown in a shop, with every configured component applied.
     * <p>
     * Built once and reused: the result only depends on the config, so it is
     * rebuilt when the config reloads, which replaces these items wholesale.
     * Callers must treat it as read-only and {@link ItemStack#copy() copy} it
     * before handing it to a player.
     */
    public ItemStack toItemStack(HolderLookup.Provider registries) {
        if (cachedStack == null) {
            cachedStack = ItemStackFactory.create(this, registries);
        }
        return cachedStack;
    }
}
