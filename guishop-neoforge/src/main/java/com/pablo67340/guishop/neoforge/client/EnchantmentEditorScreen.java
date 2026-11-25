package com.pablo67340.guishop.neoforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds an item's enchantment list.
 * <p>
 * The config stores this as {@code SHARPNESS:5 UNBREAKING:3}, which is easy to
 * get wrong by hand: a misspelt name silently does nothing and a level above
 * the enchantment's maximum is quietly clamped. Here the names come from the
 * registry and the levels are bounded, so the string it writes is always one
 * that works.
 */
final class EnchantmentEditorScreen extends PanelScreen {

    private static final int ROW_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<String> onDone;

    /** Enchantment name as the config spells it, mapped to its level. */
    private final Map<String, Integer> chosen = new LinkedHashMap<>();

    EnchantmentEditorScreen(Screen parent, String current, Consumer<String> onDone) {
        super(Component.literal("Enchantments"));
        this.parent = parent;
        this.onDone = onDone;
        parse(current);
    }

    /**
     * Reads the config's form, tolerating either separator and a missing level.
     */
    private void parse(String current) {
        if (current == null || current.isBlank()) {
            return;
        }

        for (String token : current.trim().split("[,\\s]+")) {
            if (token.isBlank()) {
                continue;
            }

            String[] parts = token.split(":");
            // A modded name is namespace:path:level, so the level is whatever
            // trails the last colon and the name is everything before it.
            String name = parts.length <= 1 ? token : token.substring(0, token.lastIndexOf(':'));
            int level = 1;

            if (parts.length > 1) {
                try {
                    level = Integer.parseInt(parts[parts.length - 1].trim());
                } catch (NumberFormatException exception) {
                    // Not a level, so the whole token was the name.
                    name = token;
                }
            }

            chosen.put(name.trim().toUpperCase(Locale.ROOT), Math.max(1, level));
        }
    }

    @Override
    protected void init() {
        int rows = Math.max(1, chosen.size());
        layoutPanel(400, UiTheme.HEADER_HEIGHT + rows * ROW_HEIGHT + UiTheme.FOOTER_HEIGHT + 32);

        int row = headerBottom() + 8;
        for (Map.Entry<String, Integer> entry : new ArrayList<>(chosen.entrySet())) {
            String name = entry.getKey();
            int max = Choices.maxLevel(name);

            int right = panelRight() - UiTheme.PADDING;

            Button remove = Button.builder(Component.literal("Remove"), button -> {
                        chosen.remove(name);
                        rebuildWidgets();
                    })
                    .bounds(right - 54, row, 54, 16)
                    .build();
            addRenderableWidget(remove);

            Button up = Button.builder(Component.literal("+"), button -> {
                        chosen.merge(name, 1, (old, add) -> Math.min(max, old + add));
                        rebuildWidgets();
                    })
                    .bounds(right - 76, row, 18, 16)
                    .build();
            // Stopped at the enchantment's own maximum rather than allowing a
            // level the game would clamp anyway.
            up.active = entry.getValue() < max;
            addRenderableWidget(up);

            Button down = Button.builder(Component.literal("-"), button -> {
                        chosen.merge(name, -1, (old, sub) -> Math.max(1, old + sub));
                        rebuildWidgets();
                    })
                    .bounds(right - 118, row, 18, 16)
                    .build();
            down.active = entry.getValue() > 1;
            addRenderableWidget(down);

            row += ROW_HEIGHT;
        }

        addRenderableWidget(Button.builder(Component.literal("Add enchantment"), button -> add())
                .bounds(contentLeft(), footerTop() - 22, 110, 16)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(panelRight() - UiTheme.PADDING - 50, footerTop() + 4, 50, 16)
                .build());
    }

    private void add() {
        minecraft.setScreen(new PickerScreen(this, Component.literal("Add enchantment"),
                Choices.enchantments(), "", picked -> {
            if (!picked.isEmpty()) {
                chosen.putIfAbsent(picked.toUpperCase(Locale.ROOT), 1);
            }
            // The picker returns to this screen, which must resize for the new
            // row.
            rebuildWidgets();
        }));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, getTitle(), chosen.size() + " applied");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int row = headerBottom() + 8;
        if (chosen.isEmpty()) {
            graphics.drawString(font, "None. Add one below.", contentLeft(), row + 5,
                    UiTheme.TEXT_MUTED, false);
        }

        for (Map.Entry<String, Integer> entry : chosen.entrySet()) {
            int max = Choices.maxLevel(entry.getKey());

            graphics.drawString(font, readable(entry.getKey()), contentLeft(), row + 5,
                    UiTheme.TEXT, false);
            graphics.drawString(font, entry.getValue() + " / " + max,
                    panelRight() - UiTheme.PADDING - 96, row + 5, UiTheme.TEXT_MUTED, false);

            row += ROW_HEIGHT;
        }

        renderFooterDivider(graphics);
        graphics.drawString(font, "Levels stop at each enchantment's maximum",
                contentLeft(), footerTop() + 9, UiTheme.TEXT_MUTED, false);
    }

    /**
     * Turns the stored spelling into something readable, without pretending to
     * look the translation up: this is the name the config will hold, and
     * showing it as stored makes the file and the screen agree.
     */
    private static String readable(String stored) {
        return stored.replace('_', ' ');
    }

    @Override
    public void onClose() {
        onDone.accept(toConfigString());
        minecraft.setScreen(parent);
    }

    private String toConfigString() {
        List<String> parts = new ArrayList<>(chosen.size());
        chosen.forEach((name, level) -> parts.add(name + ":" + level));
        return String.join(" ", parts);
    }
}
