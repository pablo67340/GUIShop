package com.pablo67340.guishop.neoforge.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

/**
 * Keeps configured text legible against the surface it is drawn on.
 * <p>
 * Shop colours were authored against a vanilla chest's pale background, so a
 * config can legitimately contain near-black names that would vanish on dark
 * chrome. An unreadable item is an unsellable one, so rather than overriding
 * the author's choice outright this lifts only colours that are too dark to
 * read, keeping their hue.
 */
final class Contrast {

    /**
     * Relative luminance below which text is lifted, and the level it is lifted
     * to. Both are on the 0-1 scale.
     */
    private static final double MINIMUM_LUMINANCE = 0.22D;
    private static final double TARGET_LUMINANCE = 0.55D;

    private Contrast() {
    }

    /**
     * Rebuilds the component with any too-dark colours lightened.
     * <p>
     * Text with no explicit colour is left alone, since it inherits the
     * surface's own readable default.
     */
    static Component readable(Component source) {
        MutableComponent result = Component.empty();

        source.visit((style, text) -> {
            result.append(Component.literal(text).withStyle(lift(style)));
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    private static Style lift(Style style) {
        TextColor colour = style.getColor();
        if (colour == null) {
            return style;
        }

        int rgb = colour.getValue() & 0xFFFFFF;
        double luminance = luminance(rgb);
        if (luminance >= MINIMUM_LUMINANCE) {
            return style;
        }

        return style.withColor(TextColor.fromRgb(blendToWhite(rgb, luminance)));
    }

    /**
     * How far to blend towards white to reach the target, derived from the gap
     * rather than stepped, so a very dark colour is lifted further than a
     * borderline one.
     */
    private static int blendToWhite(int rgb, double luminance) {
        double amount = Math.min(1.0D, (TARGET_LUMINANCE - luminance) / (1.0D - luminance));

        int red = channel(rgb >> 16, amount);
        int green = channel(rgb >> 8, amount);
        int blue = channel(rgb, amount);

        return (red << 16) | (green << 8) | blue;
    }

    private static int channel(int shifted, double amount) {
        int value = shifted & 0xFF;
        return (int) Math.round(value + (255 - value) * amount);
    }

    /**
     * Perceived brightness, weighted per channel because the eye is far more
     * sensitive to green than to blue.
     */
    private static double luminance(int rgb) {
        double red = ((rgb >> 16) & 0xFF) / 255.0D;
        double green = ((rgb >> 8) & 0xFF) / 255.0D;
        double blue = (rgb & 0xFF) / 255.0D;
        return 0.2126D * red + 0.7152D * green + 0.0722D * blue;
    }
}
