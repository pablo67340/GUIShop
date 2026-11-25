package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An item's configured properties, in answer to a {@link RequestItemPayload}.
 * <p>
 * Values are the config's own text, colour codes included, which is what the
 * editor shows and what the server expects back.
 *
 * @param shop   the shop the entry belongs to
 * @param index  the entry's position in that shop
 * @param fields config key to its current value, absent if unset
 */
public record ItemFieldsPayload(
        String shop,
        int index,
        Map<String, String> fields) implements CustomPacketPayload {

    private static final int MAX_FIELDS = 64;
    private static final int MAX_VALUE_LENGTH = 4096;

    public static final CustomPacketPayload.Type<ItemFieldsPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "item_fields"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemFieldsPayload> CODEC =
            StreamCodec.ofMember(ItemFieldsPayload::write, ItemFieldsPayload::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> FIELDS_CODEC =
            ByteBufCodecs.map(
                    size -> new LinkedHashMap<>(Math.min(size, MAX_FIELDS)),
                    ByteBufCodecs.stringUtf8(MAX_VALUE_LENGTH),
                    ByteBufCodecs.stringUtf8(MAX_VALUE_LENGTH),
                    MAX_FIELDS);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(shop);
        buffer.writeVarInt(index);
        FIELDS_CODEC.encode(buffer, fields);
    }

    private static ItemFieldsPayload read(RegistryFriendlyByteBuf buffer) {
        return new ItemFieldsPayload(
                buffer.readUtf(),
                buffer.readVarInt(),
                FIELDS_CODEC.decode(buffer));
    }
}
