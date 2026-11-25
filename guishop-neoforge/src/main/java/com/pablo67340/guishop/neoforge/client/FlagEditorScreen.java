package com.pablo67340.guishop.neoforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Chooses which parts of an item's tooltip to hide.
 * <p>
 * The config keeps the Bukkit item-flag names, which this version maps onto
 * per-component tooltip switches. Only the flags that actually do something
 * here are offered, so nothing on screen is a no-op.
 */
final class FlagEditorScreen extends PanelScreen {

    /**
     * @param flag the name as the config stores it
     */
    private record Toggle(String flag, String label, String hint) {
    }

    private static final List<Toggle> TOGGLES = List.of(
            new Toggle("HIDE_ENCHANTS", "Hide enchantments",
                    "Keeps the glint but drops the list of enchantments"),
            new Toggle("HIDE_ATTRIBUTES", "Hide attributes",
                    "Hides damage, speed and the rest"),
            new Toggle("HIDE_ADDITIONAL_TOOLTIP", "Hide item details",
                    "Potion effects, book contents, banner patterns"),
            new Toggle("HIDE_DYE", "Hide dye colour",
                    "For dyed leather armour"),
            new Toggle("HIDE_UNBREAKABLE", "Hide unbreakable",
                    "Drops the 'Unbreakable' line"));

    /** The older spellings the loader also accepts, folded onto one toggle. */
    private static final List<String> ADDITIONAL_ALIASES =
            List.of("HIDE_POTION_EFFECTS", "HIDE_ITEM_SPECIFICS");

    private static final int ROW_HEIGHT = 22;

    private final Screen parent;
    private final Consumer<String> onDone;
    private final Set<String> active = new LinkedHashSet<>();

    FlagEditorScreen(Screen parent, String current, Consumer<String> onDone) {
        super(Component.literal("Hide tooltip parts"));
        this.parent = parent;
        this.onDone = onDone;
        parse(current);
    }

    private void parse(String current) {
        if (current == null || current.isBlank()) {
            return;
        }

        for (String token : current.trim().split("[,\\s]+")) {
            String flag = token.trim().toUpperCase(Locale.ROOT);
            if (flag.isEmpty()) {
                continue;
            }
            // Normalised so a config written with an older name still shows its
            // toggle as on, rather than looking unset and being lost on save.
            active.add(ADDITIONAL_ALIASES.contains(flag) ? "HIDE_ADDITIONAL_TOOLTIP" : flag);
        }
    }

    @Override
    protected void init() {
        layoutPanel(380, UiTheme.HEADER_HEIGHT + TOGGLES.size() * ROW_HEIGHT
                + UiTheme.FOOTER_HEIGHT + 14);

        int row = headerBottom() + 8;
        for (Toggle toggle : TOGGLES) {
            String flag = toggle.flag();

            addRenderableWidget(Button.builder(label(active.contains(flag)), button -> {
                        if (!active.remove(flag)) {
                            active.add(flag);
                        }
                        button.setMessage(label(active.contains(flag)));
                    })
                    .bounds(panelRight() - UiTheme.PADDING - 52, row, 52, 16)
                    .build());

            row += ROW_HEIGHT;
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(panelRight() - UiTheme.PADDING - 50, footerTop() + 4, 50, 16)
                .build());
    }

    private static Component label(boolean on) {
        return Component.literal(on ? "Hidden" : "Shown");
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        renderPanel(graphics, getTitle(), active.size() + " hidden");

        int row = headerBottom() + 8;
        for (Toggle toggle : TOGGLES) {
            graphics.drawString(font, toggle.label(), contentLeft(), row + 1, UiTheme.TEXT, false);
            graphics.drawString(font, toggle.hint(), contentLeft(), row + 11, UiTheme.TEXT_MUTED, false);
            row += ROW_HEIGHT;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFooterDivider(graphics);
    }

    @Override
    public void onClose() {
        List<String> ordered = new ArrayList<>();
        // Written in the declared order rather than the order they were
        // clicked, so saving twice does not reshuffle the file.
        TOGGLES.forEach(toggle -> {
            if (active.contains(toggle.flag())) {
                ordered.add(toggle.flag());
            }
        });

        onDone.accept(String.join(" ", ordered));
        minecraft.setScreen(parent);
    }
}
