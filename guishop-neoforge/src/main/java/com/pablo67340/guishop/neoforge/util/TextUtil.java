package com.pablo67340.guishop.neoforge.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Converts the '&' colour codes used throughout GUIShop's configs into
 * Minecraft components.
 */
public final class TextUtil {

    private static final char ALT_CODE = '&';

    private TextUtil() {
    }

    /**
     * Parses legacy '&' codes into a styled component.
     * <p>
     * Built as explicit sibling components with styles rather than by swapping
     * '&' for the section sign, because item names and lore are not run through
     * the legacy formatter the chat renderer uses.
     */
    public static Component toComponent(String input) {
        if (input == null) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        StringBuilder pending = new StringBuilder();
        Style style = Style.EMPTY;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            boolean isCode = (current == ALT_CODE || current == ChatFormatting.PREFIX_CODE)
                    && i + 1 < input.length();

            if (!isCode) {
                pending.append(current);
                continue;
            }

            ChatFormatting formatting = ChatFormatting.getByCode(input.charAt(i + 1));
            if (formatting == null) {
                pending.append(current);
                continue;
            }

            if (pending.length() > 0) {
                result.append(Component.literal(pending.toString()).withStyle(style));
                pending.setLength(0);
            }

            // Colours reset any active decoration, matching the legacy behaviour.
            style = formatting == ChatFormatting.RESET ? Style.EMPTY
                    : formatting.isColor() ? Style.EMPTY.applyFormat(formatting)
                    : style.applyFormat(formatting);

            i++;
        }

        if (pending.length() > 0) {
            result.append(Component.literal(pending.toString()).withStyle(style));
        }

        return result;
    }

    /**
     * Removes '&' colour codes, for log lines and plain comparisons.
     */
    public static String strip(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if ((current == ALT_CODE || current == ChatFormatting.PREFIX_CODE)
                    && i + 1 < input.length()
                    && ChatFormatting.getByCode(input.charAt(i + 1)) != null) {
                i++;
                continue;
            }
            out.append(current);
        }
        return out.toString();
    }
}
