package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;

/**
 * Decides whether a stack a player is holding counts as a shop's item.
 * <p>
 * Shared by the trade screen, which knows which entry it is selling, and the
 * sell container, which has to work that out from the stack. One
 * implementation, because a stack that one path will buy and the other will not
 * would be a confusing thing to explain.
 */
final class ItemMatching {

    private ItemMatching() {
    }

    /**
     * Matched on the item alone, not its components, which is what the Bukkit
     * plugin does: its sell table is keyed by material. So a named or enchanted
     * sword sells as a plain one. Matching components instead would be stricter
     * but would make most items unsellable, since a shop entry usually carries
     * a custom name that a looted item does not.
     */
    static boolean matches(ItemStack held, ItemStack template, NeoItem item) {
        if (held.isEmpty() || held.getItem() != template.getItem()) {
            return false;
        }
        return headOwnerMatches(held, item);
    }

    /**
     * The one exception to matching on the item alone, and only when
     * {@code sell-skull-uuid} asks for it: a shop selling a specific player's
     * head would otherwise buy back any head at all.
     */
    static boolean headOwnerMatches(ItemStack held, NeoItem item) {
        if (!BaseConfig.isSellSkullUUID() || item.getSkullUUID() == null
                || item.getSkullUUID().isBlank()) {
            return true;
        }

        ResolvableProfile profile = held.get(DataComponents.PROFILE);
        if (profile == null || profile.id().isEmpty()) {
            return false;
        }

        // Compared as a UUID rather than as text, so the two spellings of a
        // uuid - with hyphens and without - are treated as the same owner.
        UUID wanted = parseUuid(item.getSkullUUID());
        return wanted != null && wanted.equals(profile.id().get());
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
