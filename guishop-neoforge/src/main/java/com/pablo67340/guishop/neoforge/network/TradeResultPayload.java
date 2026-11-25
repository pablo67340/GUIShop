package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The outcome of a trade, shown on the shop screen rather than in chat so the
 * player sees it where they are looking.
 * <p>
 * The balance travels with it so the screen never has to guess what a trade
 * cost, which would drift from the server's view the moment anything else
 * changed the balance.
 *
 * @param message a line describing what happened, already worded by the server
 * @param success whether the trade went through, which decides the colouring
 * @param balance the balance after the trade
 */
public record TradeResultPayload(
        Component message,
        boolean success,
        double balance) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TradeResultPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "trade_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TradeResultPayload> CODEC =
            StreamCodec.ofMember(TradeResultPayload::write, TradeResultPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        ComponentSerialization.STREAM_CODEC.encode(buffer, message);
        buffer.writeBoolean(success);
        buffer.writeDouble(balance);
    }

    private static TradeResultPayload read(RegistryFriendlyByteBuf buffer) {
        return new TradeResultPayload(
                ComponentSerialization.STREAM_CODEC.decode(buffer),
                buffer.readBoolean(),
                buffer.readDouble());
    }
}
