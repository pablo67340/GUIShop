package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import com.pablo67340.guishop.neoforge.network.WorthPayload;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import com.pablo67340.guishop.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Writes what an item is worth into its tooltip.
 * <p>
 * This is the whole worth display. The plugin needed a packet library, a
 * listener on two packet types, an inventory title blacklist that had to cope
 * with hex colour codes and mojibake, code to strip the line back off equipped
 * armour, a special case for creative mode and a thread check for Folia, all
 * to put one line of text under an item. A mod is told when a tooltip is being
 * built and can simply add to it.
 * <p>
 * The prices come from the server, which sends them on login and again when
 * they change. Nothing is calculated here that a sale would disagree with.
 */
@EventBusSubscriber(modid = GuiShopMod.MOD_ID, value = Dist.CLIENT)
public final class WorthTooltip {

    private static WorthPayload payload = WorthPayload.disabled();

    /**
     * The payload's entries by item id, since a tooltip is built far more
     * often than the table is replaced.
     */
    private static Map<ResourceLocation, WorthPayload.Entry> index = Map.of();

    private WorthTooltip() {
    }

    /**
     * Takes the server's price table. Called on the client thread from the
     * payload handler.
     */
    public static void accept(WorthPayload incoming) {
        payload = incoming;

        Map<ResourceLocation, WorthPayload.Entry> built = new HashMap<>(incoming.entries().size());
        // Later entries lose rather than overwrite: the server sends the best
        // price first, and a duplicate id would otherwise demote it.
        incoming.entries().forEach(entry -> built.putIfAbsent(entry.item(), entry));
        index = Map.copyOf(built);
    }

    /** Forgets the table on disconnect, so it cannot leak into another server. */
    public static void clear() {
        payload = WorthPayload.disabled();
        index = Map.of();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!payload.enabled() || !ClientPrefs.showWorth()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        // The shop already prints a buy and a sell line under every item, so
        // a worth line there would say the same thing twice.
        if (Minecraft.getInstance().screen instanceof PanelScreen) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        if (isHidden(stack, tooltip)) {
            return;
        }

        BigDecimal unit = priceOf(stack);
        if (unit == null) {
            if (!payload.onlySellable() && !payload.unsellable().isBlank()) {
                add(tooltip, TextUtil.toComponent(payload.unsellable()));
            }
            return;
        }

        add(tooltip, TextUtil.toComponent(line(unit, stack.getCount())));
    }

    /**
     * What one of this item sells for, or null if nothing buys it.
     */
    private static BigDecimal priceOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        WorthPayload.Entry entry = id == null ? null : index.get(id);
        if (entry == null || entry.price() <= 0) {
            return null;
        }

        if (!entry.owner().isEmpty() && !ownerMatches(stack, entry.owner())) {
            // Priced for one particular player's head, and this is not it.
            return null;
        }
        return BigDecimal.valueOf(entry.price());
    }

    /**
     * Mirrors the server's head check, so a head the server would refuse to
     * buy is not advertised as having a price.
     */
    private static boolean ownerMatches(ItemStack stack, String owner) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.id().isEmpty()) {
            return false;
        }
        try {
            return UUID.fromString(owner).equals(profile.id().get());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Whether this item has been asked to be left alone, by name or by
     * something already in its tooltip.
     */
    private static boolean isHidden(ItemStack stack, List<Component> tooltip) {
        if (!payload.hiddenNames().isEmpty()) {
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            for (String pattern : payload.hiddenNames()) {
                if (!pattern.isBlank() && name.contains(pattern.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }

        if (payload.hiddenLore().isEmpty()) {
            return false;
        }
        for (Component line : tooltip) {
            String text = line.getString().toLowerCase(Locale.ROOT);
            for (String pattern : payload.hiddenLore()) {
                if (!pattern.isBlank() && text.contains(pattern.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The configured line, with the same placeholders the plugin accepted so a
     * format copied across from worth.yml reads the same.
     */
    private static String line(BigDecimal unit, int count) {
        BigDecimal total = unit.multiply(BigDecimal.valueOf(count));
        return payload.format()
                .replace("%worth%", money(total))
                .replace("%worth_single%", money(unit))
                .replace("%amount%", String.valueOf(count))
                .replace("%currency_prefix%", payload.prefix())
                .replace("%currency_suffix%", payload.suffix());
    }

    /**
     * Formatted the way the shop screens format the same number, since the two
     * sit side by side the moment a player opens a shop with their inventory
     * showing.
     */
    private static String money(BigDecimal amount) {
        String formatted = payload.abbreviate()
                ? MathUtil.formatAbbreviated(amount)
                : MathUtil.formatWithCommas(amount);
        return payload.prefix() + formatted + payload.suffix();
    }

    /**
     * Puts the line where it was asked for, with a gap before it if one was
     * asked for.
     * <p>
     * Index 1 for the top, which is directly under the item's name; the name
     * is always the first line, so this cannot land above it.
     */
    private static void add(List<Component> tooltip, Component line) {
        if (payload.top()) {
            tooltip.add(1, line);
            if (payload.blankLine()) {
                tooltip.add(2, Component.empty());
            }
            return;
        }

        if (payload.blankLine()) {
            tooltip.add(Component.empty());
        }
        tooltip.add(line);
    }
}
