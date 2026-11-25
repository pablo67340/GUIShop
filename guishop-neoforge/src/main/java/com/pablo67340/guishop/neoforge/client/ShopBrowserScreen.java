package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.OpenShopPayload;
import com.pablo67340.guishop.neoforge.network.ShopEntryData;
import com.pablo67340.guishop.neoforge.network.ShopView;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import com.pablo67340.guishop.util.MathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * The shop UI: a scrolling grid with a search box, used for both the menu and
 * the individual shops.
 * <p>
 * The whole catalogue arrives in one payload, so moving between the menu and a
 * shop is local and instant.
 */
public final class ShopBrowserScreen extends Screen {

    private final OpenShopPayload catalogue;

    /** Where "back" goes, so nested navigation unwinds in order. */
    private final Deque<ShopView> history = new ArrayDeque<>();

    private ShopView view;
    private List<ShopEntryData> visible = List.of();

    /**
     * Lowercased names for the current view, indexed alongside its entries, so
     * typing in the search box does not re-resolve every name per keystroke.
     */
    private List<String> searchKeys = List.of();

    private EditBox search;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int cellWidth = UiTheme.CELL_WIDTH;
    private int columns = 1;
    private int scroll;

    public ShopBrowserScreen(OpenShopPayload catalogue, ShopView view) {
        super(TextUtil.toComponent(view.title()));
        this.catalogue = catalogue;
        this.view = view;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(width - UiTheme.PANEL_MARGIN * 2, UiTheme.PANEL_MAX_WIDTH);
        panelHeight = Math.min(height - UiTheme.PANEL_MARGIN * 2, 260);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int innerWidth = panelWidth - UiTheme.PADDING * 2 - UiTheme.SCROLLBAR_WIDTH - 2;
        columns = Math.max(1, (innerWidth + UiTheme.CELL_GAP) / (UiTheme.CELL_WIDTH + UiTheme.CELL_GAP));
        // Narrow windows get one column that fits, rather than a cell drawn
        // past the panel edge.
        cellWidth = Math.max(60, Math.min(UiTheme.CELL_WIDTH,
                (innerWidth - (columns - 1) * UiTheme.CELL_GAP) / columns));

        String previousQuery = search == null ? "" : search.getValue();

        search = new EditBox(font, panelLeft + UiTheme.PADDING,
                panelTop + UiTheme.HEADER_HEIGHT + 4, Math.max(40, panelWidth - UiTheme.PADDING * 2 - 54), 16,
                Component.literal("Search"));
        search.setHint(Component.literal("Search..."));
        search.setMaxLength(48);
        search.setValue(previousQuery);
        search.setResponder(query -> {
            scroll = 0;
            applyFilter();
        });
        addRenderableWidget(search);

        Button back = Button.builder(Component.literal("Back"), button -> goBack())
                .bounds(panelLeft + panelWidth - UiTheme.PADDING - 50,
                        panelTop + UiTheme.HEADER_HEIGHT + 4, 50, 16)
                .build();
        back.active = !history.isEmpty();
        addRenderableWidget(back);

        indexSearchKeys();
        applyFilter();
    }

    private void indexSearchKeys() {
        List<String> keys = new ArrayList<>(view.entries().size());
        for (ShopEntryData entry : view.entries()) {
            keys.add(entry.icon().getHoverName().getString().toLowerCase(Locale.ROOT));
        }
        searchKeys = keys;
    }

    private void applyFilter() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            visible = view.entries();
        } else {
            List<ShopEntryData> matches = new ArrayList<>();
            for (int index = 0; index < view.entries().size(); index++) {
                if (searchKeys.get(index).contains(query)) {
                    matches.add(view.entries().get(index));
                }
            }
            visible = List.copyOf(matches);
        }
        clampScroll();
    }

    // ---------------------------------------------------------------- layout

    private int gridTop() {
        return panelTop + UiTheme.HEADER_HEIGHT + 24;
    }

    private int gridBottom() {
        return panelTop + panelHeight - UiTheme.FOOTER_HEIGHT;
    }

    private int gridHeight() {
        return gridBottom() - gridTop();
    }

    private int contentHeight() {
        int rows = (visible.size() + columns - 1) / columns;
        return rows == 0 ? 0 : rows * (UiTheme.CELL_HEIGHT + UiTheme.CELL_GAP) - UiTheme.CELL_GAP;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - gridHeight());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int cellLeft(int column) {
        return panelLeft + UiTheme.PADDING + column * (cellWidth + UiTheme.CELL_GAP);
    }

    private int cellTop(int row) {
        return gridTop() + row * (UiTheme.CELL_HEIGHT + UiTheme.CELL_GAP) - scroll;
    }

    /**
     * @return the index under the cursor, or -1
     */
    private int indexAt(double mouseX, double mouseY) {
        if (mouseY < gridTop() || mouseY >= gridBottom()) {
            return -1;
        }

        for (int index = 0; index < visible.size(); index++) {
            int left = cellLeft(index % columns);
            int top = cellTop(index / columns);
            if (mouseX >= left && mouseX < left + cellWidth
                    && mouseY >= top && mouseY < top + UiTheme.CELL_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    // --------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + panelWidth + 1, panelTop + panelHeight + 1,
                UiTheme.PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, UiTheme.PANEL_BG);

        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + UiTheme.HEADER_HEIGHT,
                UiTheme.HEADER_BG);
        graphics.fill(panelLeft, panelTop + UiTheme.HEADER_HEIGHT - 1,
                panelLeft + panelWidth, panelTop + UiTheme.HEADER_HEIGHT, UiTheme.HEADER_BORDER);

        // No shadow: these titles are dark greys on a pale bar, as authored.
        graphics.drawString(font, getTitle(), panelLeft + UiTheme.PADDING, panelTop + 12,
                UiTheme.HEADER_TEXT, false);

        String counter = visible.size() + (visible.size() == 1 ? " entry" : " entries");
        graphics.drawString(font, counter,
                panelLeft + panelWidth - UiTheme.PADDING - font.width(counter),
                panelTop + 13, UiTheme.HEADER_MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (visible.isEmpty()) {
            String message = view.isEmpty() ? "This shop has no items." : "Nothing matches that search.";
            graphics.drawCenteredString(font, message, panelLeft + panelWidth / 2,
                    gridTop() + gridHeight() / 2 - 4, UiTheme.TEXT_MUTED);
            renderFooter(graphics);
            return;
        }

        int hovered = indexAt(mouseX, mouseY);

        // Clipped so partially scrolled rows are cut at the grid edge rather
        // than bleeding over the header and footer.
        graphics.enableScissor(panelLeft, gridTop(), panelLeft + panelWidth, gridBottom());
        for (int index = 0; index < visible.size(); index++) {
            int top = cellTop(index / columns);
            if (top + UiTheme.CELL_HEIGHT < gridTop() || top > gridBottom()) {
                continue;
            }
            renderCell(graphics, visible.get(index), cellLeft(index % columns), top, index == hovered);
        }
        graphics.disableScissor();

        renderScrollbar(graphics);
        renderFooter(graphics);

        if (hovered >= 0) {
            renderCellTooltip(graphics, visible.get(hovered), mouseX, mouseY);
        }
    }

    private void renderCell(GuiGraphics graphics, ShopEntryData entry, int left, int top, boolean hovered) {
        graphics.fill(left, top, left + cellWidth, top + UiTheme.CELL_HEIGHT,
                hovered ? UiTheme.CELL_BG_HOVER : UiTheme.CELL_BG);
        if (hovered) {
            graphics.renderOutline(left, top, cellWidth, UiTheme.CELL_HEIGHT, UiTheme.CELL_BORDER_HOVER);
        }

        graphics.renderFakeItem(entry.icon(), left + 6, top + 8);

        int textLeft = left + 28;
        int textWidth = Math.max(8, cellWidth - 34);

        // Drawn from the stack's own name so the configured colours and
        // formatting survive, falling back to the vanilla item name.
        graphics.drawString(font, trimToWidth(Contrast.readable(entry.icon().getHoverName()), textWidth),
                textLeft, top + 6, UiTheme.TEXT, false);

        renderCellPrices(graphics, entry, textLeft, top + 18);
    }

    /**
     * Keeps the name's styling while clipping it to the cell. The styled form
     * is used when it fits, which is the common case.
     */
    private FormattedCharSequence trimToWidth(Component name, int maxWidth) {
        if (font.width(name) <= maxWidth) {
            return name.getVisualOrderText();
        }
        return Language.getInstance().getVisualOrder(
                FormattedText.composite(
                        font.substrByWidth(name, maxWidth - font.width("...")),
                        FormattedText.of("...")));
    }

    /**
     * Draws the single detail line under the item name: either its prices, or
     * what the entry does instead of trading.
     */
    private void renderCellPrices(GuiGraphics graphics, ShopEntryData entry, int left, int top) {
        if (entry.isShortcut()) {
            graphics.drawString(font, "Browse shop", left, top, UiTheme.TEXT_MUTED, false);
            return;
        }

        if (!entry.canBuy() && !entry.canSell()) {
            graphics.drawString(font, "Not tradeable", left, top, UiTheme.TEXT_MUTED, false);
            return;
        }

        int cursor = left;
        if (entry.canBuy()) {
            String buy = "B " + MathUtil.formatWithCommas(entry.buyPrice());
            graphics.drawString(font, buy, cursor, top, UiTheme.BUY, false);
            cursor += font.width(buy) + 6;
        }
        if (entry.canSell()) {
            graphics.drawString(font, "S " + MathUtil.formatWithCommas(entry.sellPrice()),
                    cursor, top, UiTheme.SELL, false);
        }
    }

    /**
     * Builds the tooltip from Minecraft's own item tooltip, then appends the
     * shop lines.
     * <p>
     * Deriving it from the stack rather than hand-assembling name and lore means
     * every configured component - custom name, lore, enchantments, potion
     * effects, attribute modifiers, trim and the rest - shows up exactly as it
     * would anywhere else in the game, with no per-feature work here.
     */
    private void renderCellTooltip(GuiGraphics graphics, ShopEntryData entry, int mouseX, int mouseY) {
        ItemStack stack = entry.icon();

        List<Component> lines = new ArrayList<>(stack.getTooltipLines(
                Item.TooltipContext.of(minecraft.level),
                minecraft.player,
                minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));

        if (entry.isShortcut()) {
            lines.add(Component.empty());
            lines.add(muted("Click to open " + entry.targetShop()));
        } else if (entry.canBuy() || entry.canSell()) {
            lines.add(Component.empty());
            if (entry.canBuy()) {
                lines.add(coloured("Buy: " + MathUtil.formatWithCommas(entry.buyPrice()), UiTheme.BUY));
            }
            if (entry.canSell()) {
                lines.add(coloured("Sell: " + MathUtil.formatWithCommas(entry.sellPrice()), UiTheme.SELL));
            }
        } else {
            lines.add(Component.empty());
            lines.add(coloured("Not tradeable", UiTheme.WARN));
        }

        graphics.renderComponentTooltip(font, lines, mouseX, mouseY, stack);
    }

    private static Component muted(String text) {
        return coloured(text, UiTheme.TEXT_MUTED);
    }

    /**
     * Theme colours carry an alpha byte for the fill calls; text colours must
     * not, so it is masked off here.
     */
    private static Component coloured(String text, int argb) {
        return Component.literal(text).withStyle(style -> style.withColor(argb & 0xFFFFFF));
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int range = maxScroll();
        if (range == 0) {
            return;
        }

        int trackLeft = panelLeft + panelWidth - UiTheme.PADDING + 2;
        int trackRight = trackLeft + UiTheme.SCROLLBAR_WIDTH;
        graphics.fill(trackLeft, gridTop(), trackRight, gridBottom(), UiTheme.SCROLL_TRACK);

        int thumbHeight = Math.max(16, gridHeight() * gridHeight() / contentHeight());
        int thumbTop = gridTop() + (gridHeight() - thumbHeight) * scroll / range;
        graphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, UiTheme.SCROLL_THUMB);
    }

    private void renderFooter(GuiGraphics graphics) {
        int footerTop = panelTop + panelHeight - UiTheme.FOOTER_HEIGHT;
        graphics.fill(panelLeft, footerTop, panelLeft + panelWidth, footerTop + 1, UiTheme.DIVIDER);

        String hint = history.isEmpty()
                ? catalogue.shops().size() + " shops loaded"
                : "Backspace to go back";
        graphics.drawString(font, hint, panelLeft + UiTheme.PADDING, footerTop + 9,
                UiTheme.TEXT_MUTED, false);
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int index = indexAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        ShopEntryData entry = visible.get(index);
        if (entry.isShortcut()) {
            openShop(entry.targetShop());
        }

        // Buying and selling land here next; the data is already present.
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (maxScroll() > 0) {
            scroll -= (int) (deltaY * (UiTheme.CELL_HEIGHT + UiTheme.CELL_GAP));
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Only when the search box does not own the key, so backspace can still
        // edit the query.
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isFocused() && !history.isEmpty()) {
            goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openShop(String name) {
        ShopView target = catalogue.shopOrMenu(name);
        if (target == view) {
            return;
        }

        history.push(view);
        switchTo(target);
    }

    private void goBack() {
        if (history.isEmpty()) {
            return;
        }
        switchTo(history.pop());
    }

    /**
     * Rebuilds the screen for a new view, clearing the search so the incoming
     * shop is not silently filtered by the previous query.
     */
    private void switchTo(ShopView target) {
        view = target;
        scroll = 0;
        search.setValue("");
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public Component getTitle() {
        return TextUtil.toComponent(view.title());
    }
}
