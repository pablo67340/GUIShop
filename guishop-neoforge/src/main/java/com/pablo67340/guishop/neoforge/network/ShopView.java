package com.pablo67340.guishop.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * One shop, or the menu, as the client needs it.
 *
 * @param name    the shop's config name, which menu icons target
 * @param title   the display title, still in '&' colour code form
 * @param entries the shop's contents, in config order
 */
public record ShopView(String name, String title, List<ShopEntryData> entries) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopView> CODEC =
            StreamCodec.ofMember(ShopView::write, ShopView::read);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ShopView>> LIST_CODEC =
            CODEC.apply(ByteBufCodecs.list());

    /** An empty stand-in, so the client never has to handle a null menu. */
    public static ShopView empty(String name) {
        return new ShopView(name, name, List.of());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(name);
        buffer.writeUtf(title);
        ShopEntryData.LIST_CODEC.encode(buffer, entries);
    }

    private static ShopView read(RegistryFriendlyByteBuf buffer) {
        return new ShopView(
                buffer.readUtf(),
                buffer.readUtf(),
                ShopEntryData.LIST_CODEC.decode(buffer));
    }
}
