package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static Catalogue build(ShopRegistry registry, HolderLookup.Provider registries) {
        NeoShop menu = registry.getMenu();
        Map<String, List<NeoItem>> items = new LinkedHashMap<>();

        List<ShopView> shops = new ArrayList<>();
        for (NeoShop shop : registry.all()) {
            shops.add(toView(shop, registries, items));
        }

        ShopView menuView = menu == null
                ? ShopView.empty("Menu")
                : toView(menu, registries, items);

        // No viewer yet: permission, balance and settings are filled in per
        // player when the catalogue is sent, which is what forViewer is for.
        OpenShopPayload payload = new OpenShopPayload(
                menuView, List.copyOf(shops), false, 0.0D, Map.of(), "");
        return new Catalogue(payload, Map.copyOf(items));
    }

    private static ShopView toView(NeoShop shop, HolderLookup.Provider registries,
                                   Map<String, List<NeoItem>> index) {
        List<NeoItem> shown = new ArrayList<>();

        for (var page : shop.getPages().values()) {
            // Config order within a page is slot order, which the map does not
            // guarantee, so sort explicitly.
            page.getItems().values().stream()
                    .sorted(Comparator.comparing(item -> item.getSlot() == null ? 0 : item.getSlot()))
                    .filter(ShopViewBuilder::isWorthShowing)
                    .forEach(shown::add);
        }

        List<ShopEntryData> entries = new ArrayList<>(shown.size());
        for (NeoItem item : shown) {
            entries.add(toEntry(item, registries));
        }

        index.put(Catalogue.key(shop.getName()), List.copyOf(shown));
        return new ShopView(shop.getName(), shop.getTitle(), List.copyOf(entries));
    }

    /**
     * Counts how many entries carry each kind of configured component.
     * <p>
     * Written to the debug log on startup so a config whose metadata silently
     * failed to apply is visible without opening the game.
     */
    public static String summarise(Catalogue catalogue) {
        OpenShopPayload payload = catalogue.payload();
        List<ShopEntryData> all = new ArrayList<>(payload.menu().entries());
        payload.shops().forEach(shop -> all.addAll(shop.entries()));

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
                + count(all, stack -> stack.has(DataComponents.CUSTOM_DATA)) + " with nbt, "
                + all.stream().filter(entry -> !entry.priceLines().isEmpty()).count() + " with price lines";
    }

    /**
     * One rendered price line, so the configured wording and currency format
     * can be confirmed from the log rather than by opening the game.
     *
     * @return the sample, or empty if nothing is priced
     */
    public static String samplePriceLine(Catalogue catalogue) {
        return catalogue.payload().shops().stream()
                .flatMap(shop -> shop.entries().stream())
                .filter(entry -> !entry.priceLines().isEmpty())
                .findFirst()
                .map(entry -> entry.icon().getHoverName().getString() + " -> "
                        + entry.priceLines().stream().map(Component::getString).toList())
                .orElse("");
    }

    private static long count(List<ShopEntryData> entries, Predicate<ItemStack> predicate) {
        return entries.stream().map(ShopEntryData::icon).filter(predicate).count();
    }

    private static boolean isWorthShowing(NeoItem item) {
        if (item.getItemType().isNavigationType()) {
            return false;
        }
        // Shortcuts carry the menu; everything else must actually be tradeable.
        return item.getItemType() == ItemType.SHOP_SHORTCUT || item.getItemType().isPurchasable();
    }

    private static ShopEntryData toEntry(NeoItem item, HolderLookup.Provider registries) {
        return new ShopEntryData(
                // The loader sets the slot to the entry's position in the file,
                // which is the identity both sides agree on.
                item.getSlot() == null ? 0 : item.getSlot(),
                item.toItemStack(registries),
                item.hasBuyPrice() ? item.getBuyPriceAsDecimal().doubleValue() : ShopEntryData.NO_PRICE,
                item.hasSellPrice() ? item.getSellPriceAsDecimal().doubleValue() : ShopEntryData.NO_PRICE,
                item.getTargetShop() == null ? "" : item.getTargetShop(),
                item.getItemType() == ItemType.SHOP_SHORTCUT,
                priceLines(item));
    }

    /**
     * The configured buy and sell lines for a single unit.
     * <p>
     * Built here rather than on the client because the templates, the currency
     * format and any live pricing are all server-side. A shortcut gets none:
     * it opens a shop rather than trading.
     * <p>
     * The "cannot buy" and "cannot sell" lines are deliberately omitted when
     * the item has the other price, since a sell-only item reading "Cannot buy"
     * on every row is noise rather than information.
     */
    private static List<Component> priceLines(NeoItem item) {
        if (item.getItemType() == ItemType.SHOP_SHORTCUT) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>(2);
        if (item.hasBuyPrice()) {
            lines.add(TextUtil.toComponent(item.getBuyLore(1)));
        }
        if (item.hasSellPrice()) {
            lines.add(TextUtil.toComponent(item.getSellLore(1)));
        }

        if (lines.isEmpty()) {
            // Nothing is priced, so say so once rather than twice.
            lines.add(TextUtil.toComponent(item.getBuyLore(1)));
        }
        return List.copyOf(lines);
    }
}
