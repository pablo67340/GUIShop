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
 * Edited item properties, sent when an editor saves.
 * <p>
 * Fields are carried as the text the editor showed, keyed by their config name,
 * and the server parses them. That keeps the wire format indifferent to which
 * properties exist: adding one to the editor needs no change here.
 * <p>
 * An empty value means "remove this property", which is how a field is cleared.
 *
 * @param shop   the shop the entry belongs to
 * @param index  the entry's position in that shop, or -1 to add a new item
 * @param fields config key to its new text value
 */
public record SaveItemPayload(
        String shop,
        int index,
        Map<String, String> fields) implements CustomPacketPayload {

    /** A guard against a client sending an unreasonable number of fields. */
    private static final int MAX_FIELDS = 64;

    /** Long enough for a lore block or a command list, short enough to bound. */
    private static final int MAX_VALUE_LENGTH = 4096;

    public static final CustomPacketPayload.Type<SaveItemPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "save_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveItemPayload> CODEC =
            StreamCodec.ofMember(SaveItemPayload::write, SaveItemPayload::read);

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

    private static SaveItemPayload read(RegistryFriendlyByteBuf buffer) {
        return new SaveItemPayload(
                buffer.readUtf(),
                buffer.readVarInt(),
                FIELDS_CODEC.decode(buffer));
    }
}
