package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Sends the whole catalogue to the client when it opens the shop.
 * <p>
 * One payload rather than a request per shop, so browsing between the menu and
 * the shops is instant and needs no round trip. The catalogue is small - a few
 * hundred entries at most - and it is only sent on an explicit command.
 *
 * @param menu  the main menu, empty if menu.yml is missing
 * @param shops every loaded shop
 */
public record OpenShopPayload(ShopView menu, List<ShopView> shops) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenShopPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "open_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC =
            StreamCodec.ofMember(OpenShopPayload::write, OpenShopPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * @param shopName the shop to show first, or empty to show the menu
     */
    public ShopView shopOrMenu(String shopName) {
        if (shopName == null || shopName.isEmpty()) {
            return menu;
        }
        return shops.stream()
                .filter(shop -> shop.name().equalsIgnoreCase(shopName))
                .findFirst()
                .orElse(menu);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ShopView.CODEC.encode(buffer, menu);
        ShopView.LIST_CODEC.encode(buffer, shops);
    }

    private static OpenShopPayload read(RegistryFriendlyByteBuf buffer) {
        return new OpenShopPayload(
                ShopView.CODEC.decode(buffer),
                ShopView.LIST_CODEC.decode(buffer));
    }
}
