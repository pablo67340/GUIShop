package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoShop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    public static OpenShopPayload build(ShopRegistry registry) {
        NeoShop menu = registry.getMenu();

        List<ShopView> shops = new ArrayList<>();
        for (NeoShop shop : registry.all()) {
            shops.add(toView(shop));
        }

        return new OpenShopPayload(
                menu == null ? ShopView.empty("Menu") : toView(menu),
                List.copyOf(shops));
    }

    private static ShopView toView(NeoShop shop) {
        List<ShopEntryData> entries = new ArrayList<>();

        for (var page : shop.getPages().values()) {
            // Config order within a page is slot order, which the map does not
            // guarantee, so sort explicitly.
            page.getItems().values().stream()
                    .sorted(Comparator.comparing(item -> item.getSlot() == null ? 0 : item.getSlot()))
                    .filter(ShopViewBuilder::isWorthShowing)
                    .map(ShopViewBuilder::toEntry)
                    .forEach(entries::add);
        }

        return new ShopView(shop.getName(), shop.getTitle(), List.copyOf(entries));
    }

    private static boolean isWorthShowing(NeoItem item) {
        if (item.getItemType().isNavigationType()) {
            return false;
        }
        // Shortcuts carry the menu; everything else must actually be tradeable.
        return item.getItemType() == com.pablo67340.guishop.definition.ItemType.SHOP_SHORTCUT
                || item.getItemType().isPurchasable();
    }

    private static ShopEntryData toEntry(NeoItem item) {
        return new ShopEntryData(
                item.toItemStack(),
                item.hasBuyPrice() ? item.getBuyPriceAsDecimal().doubleValue() : ShopEntryData.NO_PRICE,
                item.hasSellPrice() ? item.getSellPriceAsDecimal().doubleValue() : ShopEntryData.NO_PRICE,
                item.getTargetShop() == null ? "" : item.getTargetShop());
    }
}
