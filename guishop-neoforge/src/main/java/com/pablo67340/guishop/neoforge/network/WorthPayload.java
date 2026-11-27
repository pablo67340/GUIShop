package com.pablo67340.guishop.neoforge.network;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * What every item is worth, so the client can say so in a tooltip.
 * <p>
 * The plugin had to rewrite each item's lore inside the packets leaving the
 * server, because a plugin cannot reach the tooltip. That brought with it an
 * inventory blacklist, special handling for armour slots and creative mode,
 * and a dependency on PacketEvents. None of it is needed here: the client is
 * ours, so it is told the prices once and decides for itself what to draw.
 * <p>
 * Sent on login and again whenever prices change. A few hundred entries of an
 * id and a double, which is smaller than a single chunk.
 *
 * @param enabled      whether to show worth at all. Sent rather than assumed
 *                     so turning it off in game takes effect without a
 *                     reconnect
 * @param format       the line to draw, with %worth%, %worth_single% and
 *                     %amount%
 * @param unsellable   the line for an item no shop buys, when those are shown
 * @param onlySellable whether to say nothing at all about an unsellable item
 * @param blankLine    whether to separate the worth line from the tooltip
 * @param top          whether the line goes under the item's name rather than
 *                     at the end
 * @param prefix       currency marker before the amount
 * @param suffix       currency marker after the amount
 * @param abbreviate   whether to write 1.5K rather than 1,500, matching how
 *                     the shop screens show the same number
 * @param hiddenNames  items whose name contains one of these get no worth
 *                     line, for the custom items a shop sells by name
 * @param hiddenLore   items whose tooltip already contains one of these are
 *                     left alone
 * @param entries      what each item sells for, one unit at a time
 */
public record WorthPayload(
        boolean enabled,
        String format,
        String unsellable,
        boolean onlySellable,
        boolean blankLine,
        boolean top,
        String prefix,
        String suffix,
        boolean abbreviate,
        List<String> hiddenNames,
        List<String> hiddenLore,
        List<Entry> entries) implements CustomPacketPayload {

    /** Longer than any shop, and a bound on what a hostile server can send. */
    private static final int MAX_ENTRIES = 20_000;
    private static final int MAX_PATTERNS = 256;

    public static final CustomPacketPayload.Type<WorthPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "worth"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorthPayload> CODEC =
            StreamCodec.ofMember(WorthPayload::write, WorthPayload::read);

    /** Nothing to show, for a server with the feature switched off. */
    public static WorthPayload disabled() {
        return new WorthPayload(false, "", "", true, false, false, "", "", true,
                List.of(), List.of(), List.of());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * What one of an item fetches.
     *
     * @param item  the item's registry id, sent by name rather than by numeric
     *              id so a mod loading in a different order cannot shift every
     *              price by one
     * @param price what a single one sells for
     * @param owner the head owner this price is tied to, or empty. Only ever
     *              set when the server is matching heads by owner, in which
     *              case any other head is worth nothing
     */
    public record Entry(ResourceLocation item, double price, String owner) {
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(enabled);
        if (!enabled) {
            // Everything else is only read when it is on, so an off server
            // sends one byte rather than a table nobody will draw.
            return;
        }

        buffer.writeUtf(format);
        buffer.writeUtf(unsellable);
        buffer.writeBoolean(onlySellable);
        buffer.writeBoolean(blankLine);
        buffer.writeBoolean(top);
        buffer.writeUtf(prefix);
        buffer.writeUtf(suffix);
        buffer.writeBoolean(abbreviate);

        writeStrings(buffer, hiddenNames);
        writeStrings(buffer, hiddenLore);

        buffer.writeVarInt(Math.min(entries.size(), MAX_ENTRIES));
        int written = 0;
        for (Entry entry : entries) {
            if (written++ >= MAX_ENTRIES) {
                break;
            }
            buffer.writeResourceLocation(entry.item());
            buffer.writeDouble(entry.price());
            buffer.writeUtf(entry.owner());
        }
    }

    private static WorthPayload read(RegistryFriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return disabled();
        }

        String format = buffer.readUtf();
        String unsellable = buffer.readUtf();
        boolean onlySellable = buffer.readBoolean();
        boolean blankLine = buffer.readBoolean();
        boolean top = buffer.readBoolean();
        String prefix = buffer.readUtf();
        String suffix = buffer.readUtf();
        boolean abbreviate = buffer.readBoolean();

        List<String> hiddenNames = readStrings(buffer);
        List<String> hiddenLore = readStrings(buffer);

        int count = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(
                    buffer.readResourceLocation(),
                    buffer.readDouble(),
                    buffer.readUtf()));
        }

        return new WorthPayload(true, format, unsellable, onlySellable, blankLine, top,
                prefix, suffix, abbreviate, hiddenNames, hiddenLore, List.copyOf(entries));
    }

    private static void writeStrings(RegistryFriendlyByteBuf buffer, List<String> values) {
        int count = Math.min(values.size(), MAX_PATTERNS);
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            buffer.writeUtf(values.get(index));
        }
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_PATTERNS);
        List<String> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            values.add(buffer.readUtf());
        }
        return List.copyOf(values);
    }
}
