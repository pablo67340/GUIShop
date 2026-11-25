package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the whole catalogue to the client when it opens the shop.
 * <p>
 * One payload rather than a request per shop, so browsing between the menu and
 * the shops is instant and needs no round trip. The catalogue is small - a few
 * hundred entries at most - and it is only sent on an explicit command.
 *
 * @param menu           the main menu, empty if the menu config is missing
 * @param shops          every loaded shop
 * @param canEdit        whether this player may change shops and server
 *                       settings. Decides what the screen offers, and nothing
 *                       more: the server re-checks before acting on any
 *                       change, since a modified client could send one
 *                       regardless.
 * @param balance        the player's balance, so the trade screen can show
 *                       what they can afford without a round trip
 * @param serverSettings the editable server settings and their current values,
 *                       sent only to a player who may change them. Carried
 *                       here rather than fetched on demand because it is a
 *                       handful of short strings and saves a round trip when
 *                       the settings screen opens.
 * @param openShop       which shop to show first, or empty for the menu. Named
 *                       rather than sent in place of the menu, so /shop
 *                       &lt;name&gt; can still navigate back to the menu.
 */
public record OpenShopPayload(
        ShopView menu,
        List<ShopView> shops,
        boolean canEdit,
        double balance,
        Map<String, String> serverSettings,
        String openShop) implements CustomPacketPayload {

    private static final int MAX_SETTINGS = 128;
    private static final int MAX_SETTING_LENGTH = 1024;

    public static final CustomPacketPayload.Type<OpenShopPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "open_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC =
            StreamCodec.ofMember(OpenShopPayload::write, OpenShopPayload::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> SETTINGS_CODEC =
            ByteBufCodecs.map(
                    size -> new LinkedHashMap<>(Math.min(size, MAX_SETTINGS)),
                    ByteBufCodecs.stringUtf8(MAX_SETTING_LENGTH),
                    ByteBufCodecs.stringUtf8(MAX_SETTING_LENGTH),
                    MAX_SETTINGS);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * The same catalogue for a different player, who may have different
     * permissions, a different balance, and no business seeing the settings.
     */
    public OpenShopPayload forViewer(boolean canEdit, double balance, Map<String, String> settings) {
        return new OpenShopPayload(menu, shops, canEdit, balance, settings, openShop);
    }

    /**
     * The same catalogue opening on a particular shop.
     */
    public OpenShopPayload opening(String shopName) {
        return new OpenShopPayload(menu, shops, canEdit, balance, serverSettings,
                shopName == null ? "" : shopName);
    }

    /**
     * The view the screen should start on.
     */
    public ShopView initialView() {
        return shopOrMenu(openShop);
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
        buffer.writeBoolean(canEdit);
        buffer.writeDouble(balance);
        SETTINGS_CODEC.encode(buffer, serverSettings);
        buffer.writeUtf(openShop);
    }

    private static OpenShopPayload read(RegistryFriendlyByteBuf buffer) {
        return new OpenShopPayload(
                ShopView.CODEC.decode(buffer),
                ShopView.LIST_CODEC.decode(buffer),
                buffer.readBoolean(),
                buffer.readDouble(),
                SETTINGS_CODEC.decode(buffer),
                buffer.readUtf());
    }
}
