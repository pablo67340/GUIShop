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
 * Edited server settings, sent when an administrator saves the settings screen.
 * <p>
 * As with {@link SaveItemPayload}, values are carried as text keyed by their
 * setting name and parsed server-side, so the wire format does not have to
 * track which settings exist. Only keys the server recognises are applied.
 *
 * @param values setting name to its new text value
 */
public record SaveSettingsPayload(Map<String, String> values) implements CustomPacketPayload {

    private static final int MAX_VALUES = 128;
    private static final int MAX_VALUE_LENGTH = 1024;

    public static final CustomPacketPayload.Type<SaveSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "save_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveSettingsPayload> CODEC =
            StreamCodec.ofMember(SaveSettingsPayload::write, SaveSettingsPayload::read);

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, String>> VALUES_CODEC =
            ByteBufCodecs.map(
                    size -> new LinkedHashMap<>(Math.min(size, MAX_VALUES)),
                    ByteBufCodecs.stringUtf8(MAX_VALUE_LENGTH),
                    ByteBufCodecs.stringUtf8(MAX_VALUE_LENGTH),
                    MAX_VALUES);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        VALUES_CODEC.encode(buffer, values);
    }

    private static SaveSettingsPayload read(RegistryFriendlyByteBuf buffer) {
        return new SaveSettingsPayload(VALUES_CODEC.decode(buffer));
    }
}
