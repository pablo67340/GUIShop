package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Asks the server to create or remove a shop, or an item within one.
 * <p>
 * One payload for all four because they share a permission check and a
 * response; four near-identical packets would only be four places to forget it.
 * <p>
 * Like every other request from a client this names what to act on and nothing
 * more. The server decides whether the asker may, what the file is called and
 * what a new item starts out as.
 *
 * @param action  what to do
 * @param shop    the shop to act on, or the name to create
 * @param entryId the item's position in its shop file, for the item actions
 * @param value   spare field for a future action that needs one
 */
public record ShopAdminPayload(Action action, String shop, int entryId, String value)
        implements CustomPacketPayload {

    public enum Action {
        CREATE_SHOP,
        DELETE_SHOP,
        ADD_ITEM,
        DELETE_ITEM
    }

    /** Long enough for any sensible shop name, short enough to bound the packet. */
    private static final int MAX_LENGTH = 64;

    public static final Type<ShopAdminPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "shop_admin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopAdminPayload> CODEC =
            StreamCodec.ofMember(ShopAdminPayload::write, ShopAdminPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeUtf(shop, MAX_LENGTH);
        buffer.writeVarInt(entryId);
        buffer.writeUtf(value, MAX_LENGTH);
    }

    private static ShopAdminPayload read(RegistryFriendlyByteBuf buffer) {
        return new ShopAdminPayload(
                buffer.readEnum(Action.class),
                buffer.readUtf(MAX_LENGTH),
                buffer.readVarInt(),
                buffer.readUtf(MAX_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
