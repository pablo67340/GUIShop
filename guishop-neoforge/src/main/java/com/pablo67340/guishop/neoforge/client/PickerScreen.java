package com.pablo67340.guishop.neoforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Picks one value from a searchable list.
 * <p>
 * Used wherever a property has a knowable set of valid answers - item ids, mob
 * types, enchantments - so those fields cannot be typed wrongly. The caller
 * supplies the choices and gets back the string to store, which keeps this
 * screen ignorant of what it is choosing between.
 * <p>
 * Rows are drawn rather than built as widgets. A list of this length would mean
 * thousands of widgets, and only the dozen on screen are ever wanted.
 */
final class PickerScreen extends PanelScreen {

    /**
     * One option.
     *
     * @param value     what to store in the config if this is picked
     * @param label     how to name it to the player
     * @param detail    the registry id or similar, shown faintly as the
     *                  unambiguous version of the label
     * @param icon      a stack to draw beside it, or empty for none
     * @param searchKey label and detail lowercased, so searching does not
     *                  rebuild strings on every keystroke
     */
    record Choice(String value, Component label, String detail, ItemStack icon, String searchKey) {

        static Choice of(String value, Component label, String detail, ItemStack icon) {
            String key = (label.getString() + " " + detail).toLowerCase(Locale.ROOT);
            return new Choice(value, label, detail, icon, key);
        }
    }

    private static final int ROW_HEIGHT = 18;

    private final Screen parent;
    private final List<Choice> choices;
    private final String current;
    private final Consumer<String> onPick;

    private List<Choice> visible;
    private EditBox search;
    private int scroll;

    /**
     * @param current the value already set, highlighted and scrolled to
     * @param onPick  given the chosen value; the picker closes itself
     */
    PickerScreen(Screen parent, Component title, List<Choice> choices, String current,
                 Consumer<String> onPick) {
        super(title);
        this.parent = parent;
        this.choices = choices;
        this.current = current == null ? "" : current;
        this.onPick = onPick;
        this.visible = choices;
    }

    @Override
    protected void init() {
        layoutPanel(400, 280);

        // Kept across a rebuild so resizing the window does not clear a search
        // part-way through typing it.
        String query = search == null ? "" : search.getValue();

        search = new EditBox(font, contentLeft(), headerBottom() + 5,
                contentWidth() - 56, 16, Component.literal("Search"));
        search.setHint(Component.literal("Search..."));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(typed -> {
            scroll = 0;
            filter(typed);
        });
        addRenderableWidget(search);

        // Focused on open so the list can be narrowed by typing straight away,
        // which is the point of a picker this long.
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(panelRight() - UiTheme.PADDING - 52, headerBottom() + 5, 52, 16)
                .build());

        filter(search.getValue());
        scrollToCurrent();
    }

    private void filter(String query) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            visible = choices;
            return;
        }

        List<Choice> matches = new ArrayList<>();
        for (Choice choice : choices) {
            if (choice.searchKey().contains(needle)) {
                matches.add(choice);
            }
        }
        visible = List.copyOf(matches);
        clampScroll();
    }

    /**
     * Opens on whatever is already set, so a long list does not start at the
     * top when the answer is most likely the current value or something near
     * it.
     */
    private void scrollToCurrent() {
        if (current.isEmpty()) {
            return;
        }

        for (int index = 0; index < visible.size(); index++) {
            if (visible.get(index).value().equalsIgnoreCase(current)) {
                scroll = index * ROW_HEIGHT - listHeight() / 2 + ROW_HEIGHT;
                clampScroll();
                return;
            }
        }
    }

    // ----------------------------------------------------------------- layout

    private int listTop() {
        return headerBottom() + 26;
    }

    private int listBottom() {
        return footerTop();
    }

    private int listHeight() {
        return listBottom() - listTop();
    }

    private int maxScroll() {
        return Math.max(0, visible.size() * ROW_HEIGHT - listHeight());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int rowTop(int index) {
        return listTop() + index * ROW_HEIGHT - scroll;
    }

    private int indexAt(double mouseX, double mouseY) {
        if (mouseX < panelLeft || mouseX >= panelRight()
                || mouseY < listTop() || mouseY >= listBottom()) {
            return -1;
        }

        int index = (int) ((mouseY - listTop() + scroll) / ROW_HEIGHT);
        return index >= 0 && index < visible.size() ? index : -1;
    }

    // ----------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, getTitle(), visible.size() + " of " + choices.size());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int hovered = indexAt(mouseX, mouseY);

        graphics.enableScissor(panelLeft, listTop(), panelRight(), listBottom());
        for (int index = 0; index < visible.size(); index++) {
            int top = rowTop(index);
            if (top + ROW_HEIGHT < listTop() || top > listBottom()) {
                continue;
            }
            renderRow(graphics, visible.get(index), top, index == hovered);
        }
        graphics.disableScissor();

        renderScrollbar(graphics);
        renderFooterDivider(graphics);

        if (visible.isEmpty()) {
            graphics.drawCenteredString(font, "Nothing matches that search",
                    panelLeft + panelWidth / 2, listTop() + listHeight() / 2 - 4, UiTheme.TEXT_MUTED);
        }

        graphics.drawString(font, "Click to choose", contentLeft(), footerTop() + 9,
                UiTheme.TEXT_MUTED, false);
    }

    private void renderRow(GuiGraphics graphics, Choice choice, int top, boolean hovered) {
        boolean selected = !current.isEmpty() && choice.value().equalsIgnoreCase(current);

        if (hovered || selected) {
            graphics.fill(contentLeft() - 2, top, panelRight() - UiTheme.PADDING + 2, top + ROW_HEIGHT - 1,
                    hovered ? UiTheme.CELL_BG_HOVER : UiTheme.CELL_BG);
        }

        int textLeft = contentLeft();
        if (!choice.icon().isEmpty()) {
            graphics.renderFakeItem(choice.icon(), contentLeft(), top + 1);
            textLeft += 20;
        }

        graphics.drawString(font, choice.label(), textLeft, top + 5,
                selected ? UiTheme.BUY : UiTheme.TEXT, false);

        // Right-aligned so the labels stay in one column, and dropped entirely
        // when the label already fills the row.
        int detailWidth = font.width(choice.detail());
        int detailLeft = panelRight() - UiTheme.PADDING - UiTheme.SCROLLBAR_WIDTH - detailWidth;
        if (detailLeft > textLeft + font.width(choice.label()) + 8) {
            graphics.drawString(font, choice.detail(), detailLeft, top + 5, UiTheme.TEXT_MUTED, false);
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int range = maxScroll();
        if (range == 0) {
            return;
        }

        int trackLeft = panelRight() - UiTheme.PADDING + 2;
        int trackRight = trackLeft + UiTheme.SCROLLBAR_WIDTH;
        graphics.fill(trackLeft, listTop(), trackRight, listBottom(), UiTheme.SCROLL_TRACK);

        int thumbHeight = Math.max(16, listHeight() * listHeight() / (visible.size() * ROW_HEIGHT));
        int thumbTop = listTop() + (listHeight() - thumbHeight) * scroll / range;
        graphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, UiTheme.SCROLL_THUMB);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int index = indexAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        onPick.accept(visible.get(index).value());
        minecraft.setScreen(parent);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (maxScroll() > 0) {
            scroll -= (int) (deltaY * ROW_HEIGHT * 2);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
