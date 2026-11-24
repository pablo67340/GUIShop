package com.pablo67340.guishop.neoforge.definition;

import com.pablo67340.guishop.definition.ItemDefinition;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The NeoForge half of a shop item: everything that needs the Minecraft
 * registries or an {@link ItemStack}.
 * <p>
 * Mirrors {@code Item extends ItemDefinition} in the Bukkit module.
 */
public final class NeoItem extends ItemDefinition {

    /**
     * Resolves this item's configured material to a Minecraft item.
     * <p>
     * Shop configs name materials the way Bukkit does, e.g. {@code OAK_LOG},
     * which maps onto {@code minecraft:oak_log}. A namespaced id is also
     * accepted so modded items can be used.
     */
    public net.minecraft.world.item.Item resolveItem() {
        String id = getMaterial();
        if (id == null || id.isBlank()) {
            return Items.AIR;
        }

        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        ResourceLocation key = ResourceLocation.tryParse(normalized);
        if (key == null) {
            setResolveFailed("'" + id + "' is not a valid item id");
            return Items.AIR;
        }

        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            setResolveFailed("no item registered as '" + normalized + "'");
            return Items.AIR;
        }

        return BuiltInRegistries.ITEM.get(key);
    }

    /**
     * Builds the stack shown in a shop, with the configured display name and
     * lore applied.
     */
    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(resolveItem());

        String displayName = hasShopName() ? getShopName() : getName();
        if (displayName != null && !displayName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, TextUtil.toComponent(displayName));
        }

        List<Component> lore = new ArrayList<>();
        if (hasShopLore()) {
            getShopLore().forEach(line -> lore.add(TextUtil.toComponent(line)));
        } else if (hasLore()) {
            getLore().forEach(line -> lore.add(TextUtil.toComponent(line)));
        }
        if (!lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }

        return stack;
    }
}
