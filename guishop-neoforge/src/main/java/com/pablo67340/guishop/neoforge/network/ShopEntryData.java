package com.pablo67340.guishop.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
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
 * @param entryId    identifies this entry to the server. The entry's position in
 *                   its shop file, which the client treats as opaque and echoes
 *                   back. Not a list position: the list is filtered and may be
 *                   sorted, so the two would stop agreeing and a trade or an
 *                   edit would resolve a different item.
 * @param icon       the stack to render, carrying its configured name and lore
 * @param buyPrice   the buy price, or {@link #NO_PRICE} if it cannot be bought
 * @param sellPrice  the sell price, or {@link #NO_PRICE} if it cannot be sold
 * @param targetShop the shop this opens, or empty if no shop has been chosen
 * @param shortcut   whether this entry opens a shop rather than trading. Sent
 *                   rather than inferred from {@code targetShop}: an entry whose
 *                   shop has not been chosen yet is still a shortcut, and
 *                   guessing from an empty string made it look like an item the
 *                   client would then offer to sell
 * @param priceLines the price lines as configured, already rendered. Built on
 *                   the server because only it has the lore templates, the
 *                   currency format and the live prices; the client would
 *                   otherwise have to invent its own wording.
 */
public record ShopEntryData(
        int entryId,
        ItemStack icon,
        double buyPrice,
        double sellPrice,
        String targetShop,
        boolean shortcut,
        List<Component> priceLines) {

    /** Prices are never negative in config, so this cannot collide with a real one. */
    public static final double NO_PRICE = -1.0D;

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopEntryData> CODEC =
            StreamCodec.ofMember(ShopEntryData::write, ShopEntryData::read);

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ShopEntryData>> LIST_CODEC =
            CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> LINES_CODEC =
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list());

    public boolean canBuy() {
        return buyPrice > NO_PRICE;
    }

    public boolean canSell() {
        return sellPrice > NO_PRICE;
    }

    public boolean isShortcut() {
        return shortcut;
    }

    /** Whether this shortcut actually has a shop to open yet. */
    public boolean hasTarget() {
        return shortcut && !targetShop.isBlank();
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entryId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, icon);
        buffer.writeDouble(buyPrice);
        buffer.writeDouble(sellPrice);
        buffer.writeUtf(targetShop);
        buffer.writeBoolean(shortcut);
        LINES_CODEC.encode(buffer, priceLines);
    }

    private static ShopEntryData read(RegistryFriendlyByteBuf buffer) {
        return new ShopEntryData(
                buffer.readVarInt(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readUtf(),
                buffer.readBoolean(),
                LINES_CODEC.decode(buffer));
    }
}
