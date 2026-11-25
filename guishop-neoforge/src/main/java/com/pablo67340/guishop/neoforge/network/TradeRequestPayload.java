package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A request to buy or sell, sent when the player confirms a trade.
 * <p>
 * Carries only what identifies the entry, never a price. The server looks the
 * price up for itself, so a modified client cannot name its own.
 *
 * @param shop     the shop the entry belongs to
 * @param index    the entry's position in that shop, as the client received it
 * @param quantity how many to trade
 * @param selling  true to sell, false to buy
 */
public record TradeRequestPayload(
        String shop,
        int index,
        int quantity,
        boolean selling) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TradeRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "trade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeRequestPayload> CODEC =
            StreamCodec.ofMember(TradeRequestPayload::write, TradeRequestPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(shop);
        buffer.writeVarInt(index);
        buffer.writeVarInt(quantity);
        buffer.writeBoolean(selling);
    }

    private static TradeRequestPayload read(RegistryFriendlyByteBuf buffer) {
        return new TradeRequestPayload(
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean());
    }
}
