package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Asks for an item's configured properties, so the editor can be filled in.
 * <p>
 * Fetched on demand rather than sent with the catalogue: the raw config is only
 * any use to someone who may edit it, and sending it to every client with
 * every shop would be both wasteful and more than they need to know.
 *
 * @param shop  the shop the entry belongs to
 * @param index the entry's position in that shop
 */
public record RequestItemPayload(String shop, int index) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestItemPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "request_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemPayload> CODEC =
            StreamCodec.ofMember(RequestItemPayload::write, RequestItemPayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(shop);
        buffer.writeVarInt(index);
    }

    private static RequestItemPayload read(RegistryFriendlyByteBuf buffer) {
        return new RequestItemPayload(buffer.readUtf(), buffer.readVarInt());
    }
}
