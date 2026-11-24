package com.pablo67340.guishop.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One purchasable entry as the client needs it.
 * <p>
 * Deliberately not slot-based: a real screen reflows its contents, so the
 * chest GUI's fixed slot index is meaningless here and ordering is carried by
 * the list instead.
 * <p>
 * The name and lore are not sent separately. They travel inside the stack's own
 * components, along with everything else configured on the item, so the client
 * can render it exactly as Minecraft would anywhere else.
 *
 * @param icon       the stack to render, carrying its configured name and lore
 * @param buyPrice   the buy price, or {@link #NO_PRICE} if it cannot be bought
 * @param sellPrice  the sell price, or {@link #NO_PRICE} if it cannot be sold
 * @param targetShop the shop a menu icon opens, or empty for a normal item
 */
public record ShopEntryData(
        ItemStack icon,
        double buyPrice,
        double sellPrice,
        String targetShop) {

    /** Prices are never negative in config, so this cannot collide with a real one. */
    public static final double NO_PRICE = -1.0D;

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopEntryData> CODEC =
            StreamCodec.ofMember(ShopEntryData::write, ShopEntryData::read);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ShopEntryData>> LIST_CODEC =
            CODEC.apply(ByteBufCodecs.list());

    public boolean canBuy() {
        return buyPrice > NO_PRICE;
    }

    public boolean canSell() {
        return sellPrice > NO_PRICE;
    }

    public boolean isShortcut() {
        return !targetShop.isEmpty();
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, icon);
        buffer.writeDouble(buyPrice);
        buffer.writeDouble(sellPrice);
        buffer.writeUtf(targetShop);
    }

    private static ShopEntryData read(RegistryFriendlyByteBuf buffer) {
        return new ShopEntryData(
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf());
    }
}
