package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Turns the loaded core model into the flat views the client renders.
 * <p>
 * Filler panes, page arrows and balance icons are dropped: they only exist to
 * pad a fixed 54-slot chest grid, and a screen that reflows its contents has no
 * use for them.
 */
public final class ShopViewBuilder {

    private ShopViewBuilder() {
    }

    public static OpenShopPayload build(ShopRegistry registry, HolderLookup.Provider registries) {
        NeoShop menu = registry.getMenu();

        List<ShopView> shops = new ArrayList<>();
        for (NeoShop shop : registry.all()) {
            shops.add(toView(shop, registries));
        }

        return new OpenShopPayload(
                menu == null ? ShopView.empty("Menu") : toView(menu, registries),
                List.copyOf(shops));
    }

    private static ShopView toView(NeoShop shop, HolderLookup.Provider registries) {
        List<ShopEntryData> entries = new ArrayList<>();

        for (var page : shop.getPages().values()) {
            // Config order within a page is slot order, which the map does not
            // guarantee, so sort explicitly.
            page.getItems().values().stream()
                    .sorted(Comparator.comparing(item -> item.getSlot() == null ? 0 : item.getSlot()))
                    .filter(ShopViewBuilder::isWorthShowing)
                    .map(item -> toEntry(item, registries))
                    .forEach(entries::add);
        }

        return new ShopView(shop.getName(), shop.getTitle(), List.copyOf(entries));
    }

    /**
     * Counts how many entries carry each kind of configured component.
     * <p>
     * Written to the debug log on startup so a config whose metadata silently
     * failed to apply is visible without opening the game.
     */
    public static String summarise(OpenShopPayload catalogue) {
        List<ShopEntryData> all = new ArrayList<>(catalogue.menu().entries());
        catalogue.shops().forEach(shop -> all.addAll(shop.entries()));

        return all.size() + " entries: "
                + count(all, stack -> stack.has(DataComponents.CUSTOM_NAME)) + " named, "
                // Enchantments and lore have empty defaults on every item, so
                // presence alone would count all of them.
                + count(all, stack -> !stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
                        .lines().isEmpty()) + " with lore, "
                + count(all, stack -> !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .isEmpty()) + " enchanted, "
                + count(all, stack -> stack.has(DataComponents.POTION_CONTENTS)) + " potions, "
                + count(all, stack -> stack.has(DataComponents.BLOCK_ENTITY_DATA)) + " spawners, "
                + count(all, stack -> stack.has(DataComponents.FIREWORKS)) + " fireworks, "
                + count(all, stack -> stack.has(DataComponents.PROFILE)) + " heads, "
                + count(all, stack -> stack.has(DataComponents.CUSTOM_MODEL_DATA)) + " custom models, "
                + count(all, stack -> stack.has(DataComponents.CUSTOM_DATA)) + " with nbt";
    }

    private static long count(List<ShopEntryData> entries, Predicate<ItemStack> predicate) {
        return entries.stream().map(ShopEntryData::icon).filter(predicate).count();
    }

    private static boolean isWorthShowing(NeoItem item) {
        if (item.getItemType().isNavigationType()) {
            return false;
        }
        // Shortcuts carry the menu; everything else must actually be tradeable.
        return item.getItemType() == com.pablo67340.guishop.definition.ItemType.SHOP_SHORTCUT
                || item.getItemType().isPurchasable();
    }

    private static ShopEntryData toEntry(NeoItem item, HolderLookup.Provider registries) {
        return new ShopEntryData(
                item.toItemStack(registries),
                item.hasBuyPrice() ? item.getBuyPriceAsDecimal().doubleValue() : ShopEntryData.NO_PRICE,
                item.hasSellPrice() ? item.getSellPriceAsDecimal().doubleValue() : ShopEntryData.NO_PRICE,
                item.getTargetShop() == null ? "" : item.getTargetShop());
    }
}
