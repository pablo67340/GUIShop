package com.pablo67340.guishop.neoforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Asks for the name of a shop that does not exist yet.
 * <p>
 * Reached from the shop picker's "New shop..." entry, and like the pickers it
 * returns to whichever screen opened it rather than to the browser. It only
 * hands the name back; nothing is written until the item is saved, so naming a
 * shop and then changing your mind leaves no file behind.
 */
final class ShopNameScreen extends PanelScreen {

    /** Must agree with ShopAdminService, which has the final say. */
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final Screen parent;
    private final List<String> existing;
    private final Consumer<String> onNamed;

    private EditBox name;
    private Button confirm;

    ShopNameScreen(Screen parent, List<String> existing, Consumer<String> onNamed) {
        super(Component.literal("New shop"));
        this.parent = parent;
        this.existing = existing;
        this.onNamed = onNamed;
    }

    @Override
    protected void init() {
        layoutPanel(280, UiTheme.HEADER_HEIGHT + 62 + UiTheme.FOOTER_HEIGHT);

        String typed = name == null ? "" : name.getValue();

        name = new EditBox(font, contentLeft(), headerBottom() + 22, contentWidth(), 18,
                Component.literal("Shop name"));
        name.setHint(Component.literal("Shop name"));
        name.setMaxLength(32);
        // Filtered as it is typed, so a space or a slash simply cannot be put
        // into something that becomes a file name.
        name.setFilter(value -> value.isEmpty() || VALID_NAME.matcher(value).matches());
        name.setValue(typed);
        name.setResponder(value -> refresh());
        addRenderableWidget(name);

        confirm = Button.builder(Component.literal("Use this name"), button -> submit())
                .bounds(contentLeft(), footerTop() + 4, 94, 16)
                .build();
        addRenderableWidget(confirm);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(contentLeft() + 98, footerTop() + 4, 60, 16)
                .build());

        setInitialFocus(name);
        refresh();
    }

    private void refresh() {
        confirm.active = !name.getValue().trim().isEmpty() && problem() == null;
    }

    /**
     * @return why the typed name cannot be used, or null if it can
     */
    private String problem() {
        String typed = name.getValue().trim();
        if (typed.isEmpty()) {
            return null;
        }
        return existing.stream().anyMatch(shop -> shop.equalsIgnoreCase(typed))
                ? "That shop already exists - pick it from the list instead."
                : null;
    }

    private void submit() {
        String typed = name.getValue().trim();
        if (typed.isEmpty() || problem() != null) {
            return;
        }
        onNamed.accept(typed);
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER && confirm.active) {
            submit();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, getTitle(), null);
        renderFooterDivider(graphics);

        graphics.drawString(font, "Name", contentLeft(), headerBottom() + 10,
                UiTheme.TEXT, false);

        String issue = problem();
        String note = issue != null
                ? issue
                : "Created empty when you save the item. Letters, digits, dashes, underscores.";

        graphics.drawString(font, note, contentLeft(), headerBottom() + 46,
                issue != null ? UiTheme.WARN : UiTheme.TEXT_MUTED, false);
    }
}
