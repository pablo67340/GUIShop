package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.ItemFieldsPayload;
import com.pablo67340.guishop.neoforge.network.OpenShopPayload;
import com.pablo67340.guishop.neoforge.network.RequestItemPayload;
import com.pablo67340.guishop.neoforge.network.ShopAdminPayload;
import com.pablo67340.guishop.neoforge.network.ShopEntryData;
import com.pablo67340.guishop.neoforge.network.ShopView;
import com.pablo67340.guishop.neoforge.network.TradeRequestPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import com.pablo67340.guishop.util.MathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
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
public final class ShopBrowserScreen extends PanelScreen {

    private static final int SETTINGS_WIDTH = 52;
    private static final int EDIT_WIDTH = 42;

    private OpenShopPayload catalogue;

    /** Where "back" goes, so nested navigation unwinds in order. */
    private final Deque<String> history = new ArrayDeque<>();

    private ShopView view;
    private List<Entry> visible = List.of();

    private EditBox search;

    private int cellWidth = UiTheme.CELL_WIDTH;
    private int columns = 1;
    private int scroll;

    private double balance;

    /**
     * When on, clicking an item opens the editor instead of trading. Replaces
     * the plugin's shift-click, which existed only because a chest inventory
     * had nowhere to put a button.
     */
    private boolean editing;

    /** The last thing the server said about a trade or a save. */
    private Component status = Component.empty();
    private boolean statusSuccess = true;

    /**
     * An entry and its precomputed search key.
     * <p>
     * The identity used when talking to the server comes from the entry itself
     * rather than from where it ended up on screen, since sorting and searching
     * both reorder this list.
     */
    private record Entry(ShopEntryData data, String searchKey) {

        int id() {
            return data.entryId();
        }
    }

    public ShopBrowserScreen(OpenShopPayload catalogue, ShopView view) {
        super(TextUtil.toComponent(view.title()));
        this.catalogue = catalogue;
        this.view = view;
        this.balance = catalogue.balance();
        ClientPrefs.load();
    }

    @Override
    protected void init() {
        layoutPanel(UiTheme.PANEL_MAX_WIDTH, 260);

        int innerWidth = contentWidth() - UiTheme.SCROLLBAR_WIDTH - 2;
        columns = Math.max(1, (innerWidth + UiTheme.CELL_GAP) / (UiTheme.CELL_WIDTH + UiTheme.CELL_GAP));
        // Narrow windows get one column that fits, rather than a cell drawn
        // past the panel edge.
        cellWidth = Math.max(60, Math.min(UiTheme.CELL_WIDTH,
                (innerWidth - (columns - 1) * UiTheme.CELL_GAP) / columns));

        addControls();
        rebuildEntries();
    }

    private void addControls() {
        String previousQuery = search == null ? "" : search.getValue();

        int controlsTop = headerBottom() + 4;
        int reserved = SETTINGS_WIDTH + 4 + 54
                + (catalogue.canEdit() ? EDIT_WIDTH + 4 : 0)
                + (catalogue.canEdit() && editing ? (isMenu() ? 78 : 20) : 0);

        search = new EditBox(font, contentLeft(), controlsTop,
                Math.max(40, contentWidth() - reserved), 16, Component.literal("Search"));
        search.setHint(Component.literal("Search..."));
        search.setMaxLength(48);
        search.setValue(previousQuery);
        search.setResponder(query -> {
            scroll = 0;
            rebuildEntries();
        });
        addRenderableWidget(search);

        int right = panelRight() - UiTheme.PADDING;

        Button back = Button.builder(Component.literal("Back"), button -> goBack())
                .bounds(right - 50, controlsTop, 50, 16)
                .build();
        back.active = !history.isEmpty();
        addRenderableWidget(back);
        right -= 54;

        // Spelled out rather than drawn as a gear glyph: Minecraft falls back
        // to its unicode font for those, where U+2699 comes out looking like a
        // ship's wheel.
        addRenderableWidget(Button.builder(Component.literal("Settings"), button -> openSettings())
                .bounds(right - SETTINGS_WIDTH, controlsTop, SETTINGS_WIDTH, 16)
                .build());
        right -= SETTINGS_WIDTH + 4;

        if (catalogue.canEdit()) {
            Button edit = Button.builder(
                            Component.literal(editing ? "Editing" : "Edit"),
                            button -> toggleEditing())
                    .bounds(right - EDIT_WIDTH, controlsTop, EDIT_WIDTH, 16)
                    .build();
            addRenderableWidget(edit);
            right -= EDIT_WIDTH + 4;

            if (editing) {
                // The menu can hold both: a shortcut to a shop, or an item
                // sold straight from the menu for anyone who would rather not
                // nest shops. Two buttons rather than one that asks, since the
                // choice is already made by the time it is reached.
                if (isMenu()) {
                    addRenderableWidget(Button.builder(
                                    Component.literal("+Shop"), button -> openNewShop())
                            .bounds(right - 36, controlsTop, 36, 16)
                            .build());
                    right -= 40;
                }

                addRenderableWidget(Button.builder(
                                Component.literal(isMenu() ? "+Item" : "+"), button -> addItem())
                        .bounds(right - (isMenu() ? 34 : 16), controlsTop,
                                isMenu() ? 34 : 16, 16)
                        .build());
            }
        }
    }

    /**
     * The menu is a shop internally but is not one to add items to.
     */
    private boolean isMenu() {
        // Blank when the menu config is missing, in which case nothing is the
        // menu rather than everything unnamed being it.
        String menuName = catalogue.menu().name();
        return !menuName.isBlank() && view.name().equalsIgnoreCase(menuName);
    }

    private void addItem() {
        PacketDistributor.sendToServer(
                new ShopAdminPayload(ShopAdminPayload.Action.ADD_ITEM, view.name(), 0, ""));
    }

    private void openNewShop() {
        minecraft.setScreen(new NewShopScreen(this, shopNames()));
    }

    private void toggleEditing() {
        editing = !editing;
        setStatus(editing
                        ? "Edit mode on. Click an item to edit it."
                        : "Edit mode off.",
                true);
        rebuildWidgets();
    }

    private void openSettings() {
        minecraft.setScreen(new SettingsScreen(this, catalogue.canEdit(), catalogue.serverSettings()));
    }

    // ------------------------------------------------------------- catalogue

    /**
     * Takes a freshly sent catalogue without disturbing where the player is.
     * <p>
     * Called after an edit, when the server resends to everyone. Reopening
     * would lose the current shop, the search and the scroll position in the
     * middle of someone working.
     */
    void refresh(OpenShopPayload updated) {
        catalogue = updated;
        balance = updated.balance();

        ShopView sameView = updated.shopOrMenu(view.name());
        view = sameView == null ? updated.menu() : sameView;

        rebuildEntries();
    }

    double balance() {
        return balance;
    }

    /**
     * The shops an item could link to, for the editor's shortcut picker. Taken
     * from the catalogue so the list is exactly what the server loaded.
     */
    List<String> shopNames() {
        return catalogue.shops().stream().map(ShopView::name).toList();
    }

    void setBalance(double value) {
        balance = value;
    }

    void onResult(TradeResultPayload result) {
        status = result.message();
        statusSuccess = result.success();
        balance = result.balance();
    }

    private void setStatus(String message, boolean success) {
        status = Component.literal(message);
        statusSuccess = success;
    }

    /**
     * Called when a preference changes, since sorting and the affordability
     * filter both come from them.
     */
    void onPrefsChanged() {
        rebuildEntries();
    }

    /**
     * Opens the editor for an entry, once the server has sent its properties.
     */
    void openEditor(ItemFieldsPayload fields) {
        Entry entry = visible.stream()
                .filter(candidate -> candidate.id() == fields.index())
                .findFirst()
                .orElse(null);

        if (entry == null || !fields.shop().equalsIgnoreCase(view.name())) {
            // The player navigated away while the request was in flight.
            return;
        }

        minecraft.setScreen(new ItemEditorScreen(
                this, view.name(), fields.index(), entry.data(), fields.fields()));
    }

    // ---------------------------------------------------------------- entries

    /**
     * Rebuilds what is on screen from the current view, search and preferences.
     */
    private void rebuildEntries() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);

        List<Entry> entries = new ArrayList<>(view.entries().size());
        for (ShopEntryData data : view.entries()) {
            // Built once per rebuild rather than per keystroke.
            String key = data.icon().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !key.contains(query)) {
                continue;
            }
            if (ClientPrefs.hideUnaffordable() && isUnaffordable(data)) {
                continue;
            }
            entries.add(new Entry(data, key));
        }

        sort(entries);
        visible = List.copyOf(entries);
        clampScroll();
    }

    /**
     * A shortcut is never filtered out by affordability: it opens a shop rather
     * than costing anything.
     */
    private boolean isUnaffordable(ShopEntryData data) {
        return !data.isShortcut() && data.canBuy() && data.buyPrice() > balance;
    }

    private void sort(List<Entry> entries) {
        switch (ClientPrefs.sort()) {
            case NAME -> entries.sort(Comparator.comparing(Entry::searchKey));
            case PRICE_LOW -> entries.sort(Comparator.comparingDouble(entry -> sortPrice(entry, true)));
            case PRICE_HIGH -> entries.sort(Comparator.comparingDouble(
                    (Entry entry) -> sortPrice(entry, false)).reversed());
            // Config order is the order the server sent, which is already it.
            case CONFIG -> {
            }
        }
    }

    /**
     * What to sort an unpriced entry by: pushed to whichever end is furthest
     * from where prices start, so shortcuts and oddities do not sit in the
     * middle of a price-ordered list.
     */
    private static double sortPrice(Entry entry, boolean ascending) {
        if (!entry.data().canBuy()) {
            return ascending ? Double.MAX_VALUE : -Double.MAX_VALUE;
        }
        return entry.data().buyPrice();
    }

    // ----------------------------------------------------------------- layout

    private int gridTop() {
        return headerBottom() + 24;
    }

    private int gridBottom() {
        return footerTop();
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
        return contentLeft() + column * (cellWidth + UiTheme.CELL_GAP);
    }

    private int cellTop(int row) {
        return gridTop() + row * (UiTheme.CELL_HEIGHT + UiTheme.CELL_GAP) - scroll;
    }

    /**
     * @return the index into {@link #visible} under the cursor, or -1
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

    // ----------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, getTitle(), "$" + MathUtil.formatWithCommas(balance));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (editing) {
            // A clear reminder, because a click means something different now.
            graphics.fill(panelLeft, headerBottom(), panelRight(), headerBottom() + 2, UiTheme.SELL);
        }

        if (visible.isEmpty()) {
            graphics.drawCenteredString(font, emptyMessage(), panelLeft + panelWidth / 2,
                    gridTop() + gridHeight() / 2 - 4, UiTheme.TEXT_MUTED);
            renderFooter(graphics);
            return;
        }

        int hovered = indexAt(mouseX, mouseY);

        // Clipped so partially scrolled rows are cut at the grid edge rather
        // than bleeding over the header and footer.
        graphics.enableScissor(panelLeft, gridTop(), panelRight(), gridBottom());
        for (int index = 0; index < visible.size(); index++) {
            int top = cellTop(index / columns);
            if (top + UiTheme.CELL_HEIGHT < gridTop() || top > gridBottom()) {
                continue;
            }
            renderCell(graphics, visible.get(index).data(),
                    cellLeft(index % columns), top, index == hovered);
        }
        graphics.disableScissor();

        renderScrollbar(graphics);
        renderFooter(graphics);

        if (hovered >= 0) {
            renderCellTooltip(graphics, visible.get(hovered).data(), mouseX, mouseY);
        }
    }

    private String emptyMessage() {
        if (view.isEmpty()) {
            return "This shop has no items.";
        }
        if (ClientPrefs.hideUnaffordable()) {
            return "Nothing here you can afford, or nothing matches that search.";
        }
        return "Nothing matches that search.";
    }

    private void renderCell(GuiGraphics graphics, ShopEntryData entry, int left, int top, boolean hovered) {
        graphics.fill(left, top, left + cellWidth, top + UiTheme.CELL_HEIGHT,
                hovered ? UiTheme.CELL_BG_HOVER : UiTheme.CELL_BG);
        if (hovered) {
            graphics.renderOutline(left, top, cellWidth, UiTheme.CELL_HEIGHT,
                    editing ? UiTheme.SELL : UiTheme.CELL_BORDER_HOVER);
        }

        graphics.renderFakeItem(entry.icon(), left + 6, top + 8);

        int textLeft = left + 28;
        int textWidth = Math.max(8, cellWidth - 34);

        // Drawn from the stack's own name so the configured colours and
        // formatting survive, falling back to the vanilla item name.
        graphics.drawString(font, trimToWidth(Contrast.readable(entry.icon().getHoverName()), textWidth),
                textLeft, top + 6, UiTheme.TEXT, false);

        renderCellPrices(graphics, entry, textLeft, top + 18, textWidth);
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
     * Draws the detail line under the item name: the server-rendered price
     * lines as the config words them, or what the entry does instead of
     * trading.
     * <p>
     * Lines that will not fit are left out rather than clipped, since the
     * tooltip shows all of them anyway.
     */
    private void renderCellPrices(GuiGraphics graphics, ShopEntryData entry, int left, int top, int maxWidth) {
        if (entry.isShortcut()) {
            graphics.drawString(font, "Browse shop", left, top, UiTheme.TEXT_MUTED, false);
            return;
        }

        int cursor = left;
        for (Component line : entry.priceLines()) {
            Component readable = Contrast.readable(line);
            int lineWidth = font.width(readable);
            if (cursor > left && cursor + lineWidth > left + maxWidth) {
                break;
            }
            graphics.drawString(font, readable, cursor, top, UiTheme.TEXT, false);
            cursor += lineWidth + 6;
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
        } else if (!entry.priceLines().isEmpty()) {
            lines.add(Component.empty());
            entry.priceLines().forEach(line -> lines.add(Contrast.readable(line)));
        }

        if (editing) {
            lines.add(Component.empty());
            lines.add(coloured("Click to edit", UiTheme.SELL));
        }

        graphics.renderComponentTooltip(font, lines, mouseX, mouseY, stack);
    }

    private static Component muted(String text) {
        return coloured(text, UiTheme.TEXT_MUTED);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int range = maxScroll();
        if (range == 0) {
            return;
        }

        int trackLeft = panelRight() - UiTheme.PADDING + 2;
        int trackRight = trackLeft + UiTheme.SCROLLBAR_WIDTH;
        graphics.fill(trackLeft, gridTop(), trackRight, gridBottom(), UiTheme.SCROLL_TRACK);

        int thumbHeight = Math.max(16, gridHeight() * gridHeight() / contentHeight());
        int thumbTop = gridTop() + (gridHeight() - thumbHeight) * scroll / range;
        graphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, UiTheme.SCROLL_THUMB);
    }

    private void renderFooter(GuiGraphics graphics) {
        renderFooterDivider(graphics);

        if (!status.getString().isEmpty()) {
            graphics.drawString(font, status, contentLeft(), footerTop() + 9,
                    statusSuccess ? UiTheme.BUY : UiTheme.WARN, false);
            return;
        }

        String hint = history.isEmpty()
                ? visible.size() + (visible.size() == 1 ? " entry" : " entries")
                + " of " + view.entries().size()
                : "Backspace to go back";
        graphics.drawString(font, hint, contentLeft(), footerTop() + 9, UiTheme.TEXT_MUTED, false);
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int position = indexAt(mouseX, mouseY);
        if (position < 0) {
            return false;
        }

        Entry entry = visible.get(position);

        if (editing) {
            // The properties live on the server, so the editor cannot open
            // until they arrive.
            PacketDistributor.sendToServer(new RequestItemPayload(view.name(), entry.id()));
            return true;
        }

        if (entry.data().isShortcut()) {
            openShop(entry.data().targetShop());
            return true;
        }

        if (ClientPrefs.confirmPurchases()) {
            minecraft.setScreen(new TradeScreen(this, view.name(), entry.id(), entry.data()));
        } else if (entry.data().canBuy()) {
            PacketDistributor.sendToServer(new TradeRequestPayload(view.name(), entry.id(), 1, false));
        } else {
            setStatus("That item cannot be bought.", false);
        }
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

        history.push(view.name());
        switchTo(target);
    }

    private void goBack() {
        if (history.isEmpty()) {
            return;
        }
        switchTo(catalogue.shopOrMenu(history.pop()));
    }

    /**
     * Rebuilds the screen for a new view, clearing the search so the incoming
     * shop is not silently filtered by the previous query.
     */
    private void switchTo(ShopView target) {
        view = target;
        scroll = 0;
        status = Component.empty();
        search.setValue("");
        rebuildWidgets();
    }

    @Override
    public Component getTitle() {
        return TextUtil.toComponent(view.title());
    }
}
