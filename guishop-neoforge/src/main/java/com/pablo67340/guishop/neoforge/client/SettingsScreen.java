package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.config.Settings;
import com.pablo67340.guishop.neoforge.network.SaveSettingsPayload;
import com.pablo67340.guishop.neoforge.network.ShopAdminPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUIShop's settings, in tabs.
 * <p>
 * The first tab is this player's own preferences and is always there, since how
 * someone likes their shop list sorted should not need a permission. The rest
 * only appear for someone who may change the server's configuration, and the
 * server checks that again when a change arrives, so hiding a tab is a
 * courtesy rather than the actual protection.
 * <p>
 * Rows are measured before the panel is sized: each hint is wrapped to the
 * space actually available and the row grows to fit the result. Only the rows
 * on screen get widgets, so scrolling a long tab does not leave invisible boxes
 * behind to catch clicks. Edited values are held in {@link #pending} rather
 * than read off the widgets, which is what lets a row be destroyed and rebuilt
 * without losing what was typed into it.
 */
final class SettingsScreen extends ShopChildScreen {

    private static final int PANEL_WIDTH = 450;
    private static final int CONTROL_WIDTH = 120;

    /** Between the label text and a control beside it. */
    private static final int COLUMN_GAP = 12;

    private static final int CONTROL_HEIGHT = 16;
    private static final int LINE_HEIGHT = 9;
    private static final int TAB_HEIGHT = 16;
    private static final int TAB_GAP = 3;

    /** Identifies the client-preferences tab, which has no server category. */
    private static final String OWN_TAB = "Yours";

    /** Identifies the shop management tab, which is not a settings category. */
    private static final String MANAGE_TAB = "Manage";

    private final boolean canEdit;

    /**
     * Every server setting as it currently stands, edited in place.
     * <p>
     * Holds all tabs at once so switching tabs, or scrolling a row out of view,
     * does not discard an edit that has not been saved yet.
     */
    private final Map<String, String> pending;

    private String tab = OWN_TAB;
    private int scroll;

    /** The rows of the open tab, measured and positioned. */
    private List<Row> rows = List.of();

    /** How tall the tab strip turned out, which depends on how many fit. */
    private int tabStripHeight = TAB_HEIGHT + 8;

    /** The shop awaiting a second click to confirm deletion, if any. */
    private String pendingDelete;

    private EditBox newShopName;

    /**
     * One row, already laid out.
     * <p>
     * The position is an offset from the top of the form rather than a screen
     * coordinate, because the panel is centred after the rows are measured.
     *
     * @param spec the setting this row edits, or null for a management row
     * @param hint the hint wrapped to the label column
     */
    private record Row(String label, List<FormattedCharSequence> hint, Settings.Spec spec,
                       String shopName, int offset, int height) {
    }

    /**
     * A row before it has been measured, carrying the hint as the text it is so
     * that wrapping happens once, against a known width.
     *
     * @param spec     the setting this row edits, or null for a management row
     * @param shopName the shop this row manages, or null
     */
    private record Template(String label, String hint, Settings.Spec spec, String shopName) {
    }

    SettingsScreen(ShopBrowserScreen parent, boolean canEdit, Map<String, String> serverValues) {
        super(Component.literal("GUIShop settings"), parent);
        this.canEdit = canEdit;
        this.pending = new LinkedHashMap<>(serverValues);
    }

    // ------------------------------------------------------------------- tabs

    private List<String> tabs() {
        List<String> names = new ArrayList<>();
        names.add(OWN_TAB);

        if (canEdit) {
            for (Settings.Category category : Settings.Category.values()) {
                names.add(category.label());
            }
            names.add(MANAGE_TAB);
        }
        return names;
    }

    /**
     * @return the settings category the open tab shows, or null for the
     *         client-preferences and management tabs
     */
    private Settings.Category category() {
        for (Settings.Category category : Settings.Category.values()) {
            if (category.label().equals(tab)) {
                return category;
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- layout

    @Override
    protected void init() {
        // Sized in two passes. The panel's width decides how narrow the label
        // column is, which decides how many lines each hint wraps to, which is
        // what the panel's height has to accommodate.
        layoutPanel(PANEL_WIDTH, height);

        tabStripHeight = measureTabStrip();
        rows = measureRows();

        int wanted = UiTheme.HEADER_HEIGHT + tabStripHeight + totalRowHeight()
                + UiTheme.FOOTER_HEIGHT + 8;
        layoutPanel(PANEL_WIDTH, wanted);

        addTabButtons();
        buildRows();

        if (canEdit && category() != null) {
            addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                    .bounds(contentLeft(), footerTop() + 4, 60, 16)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(panelRight() - UiTheme.PADDING - 50, footerTop() + 4, 50, 16)
                .build());
    }

    /**
     * Tabs flow left to right and wrap, so a narrow window stacks them instead
     * of running them off the panel.
     */
    private int measureTabStrip() {
        int left = 0;
        int rowsUsed = 1;

        for (String name : tabs()) {
            int tabWidth = font.width(name) + 12;
            if (left > 0 && left + tabWidth > contentWidth()) {
                left = 0;
                rowsUsed++;
            }
            left += tabWidth + TAB_GAP;
        }
        return rowsUsed * (TAB_HEIGHT + TAB_GAP) + 5;
    }

    private void addTabButtons() {
        int left = contentLeft();
        int top = headerBottom() + 4;

        for (String name : tabs()) {
            int tabWidth = font.width(name) + 12;

            if (left > contentLeft() && left + tabWidth > contentLeft() + contentWidth()) {
                left = contentLeft();
                top += TAB_HEIGHT + TAB_GAP;
            }

            Button button = Button.builder(Component.literal(name), click -> {
                        tab = name;
                        scroll = 0;
                        pendingDelete = null;
                        rebuildWidgets();
                    })
                    .bounds(left, top, tabWidth, TAB_HEIGHT)
                    .build();
            // Pressing the tab you are already on does nothing useful.
            button.active = !name.equals(tab);
            addRenderableWidget(button);

            left += tabWidth + TAB_GAP;
        }
    }

    private int formTop() {
        return headerBottom() + tabStripHeight;
    }

    private int formBottom() {
        return footerTop() - 2;
    }

    private int visibleHeight() {
        return formBottom() - formTop();
    }

    private int totalRowHeight() {
        return rows.isEmpty() ? 0 : rows.getLast().offset() + rows.getLast().height();
    }

    private int maxScroll() {
        return Math.max(0, totalRowHeight() - visibleHeight());
    }

    private int rowTop(Row row) {
        return formTop() + row.offset() - scroll;
    }

    /**
     * Where a label's text may run to before it would reach a control sitting
     * beside it. The whole content width when the control is underneath.
     */
    private int labelWidth() {
        Settings.Category category = category();
        if (category != null && category.isLongText()) {
            return contentWidth() - UiTheme.SCROLLBAR_WIDTH - 4;
        }
        return contentWidth() - CONTROL_WIDTH - COLUMN_GAP - UiTheme.SCROLLBAR_WIDTH;
    }

    private List<Row> measureRows() {
        List<Row> measured = new ArrayList<>();
        int offset = 0;

        for (Template template : rowTemplates()) {
            List<FormattedCharSequence> wrapped =
                    template.hint() == null || template.hint().isBlank()
                            ? List.of()
                            : font.split(Component.literal(template.hint()), labelWidth());

            measured.add(new Row(template.label(), wrapped, template.spec(), template.shopName(),
                    offset, heightOf(wrapped.size())));
            offset += measured.getLast().height();
        }
        return List.copyOf(measured);
    }

    private int heightOf(int hintLines) {
        Settings.Category category = category();
        boolean stacked = category != null && category.isLongText();

        int text = LINE_HEIGHT + hintLines * LINE_HEIGHT;
        return stacked
                ? text + CONTROL_HEIGHT + 7
                : Math.max(CONTROL_HEIGHT + 6, text + 5);
    }

    // ------------------------------------------------------------------- rows

    /**
     * The rows of the open tab, before measurement.
     */
    private List<Template> rowTemplates() {
        List<Template> templates = new ArrayList<>();

        if (MANAGE_TAB.equals(tab)) {
            for (String shop : parent().shopNames()) {
                templates.add(template(shop, "Delete removes it from the menu, keeping a backup file",
                        null, shop));
            }
            templates.add(template("New shop", "A name with letters, digits, dashes or underscores",
                    null, null));
            return templates;
        }

        Settings.Category category = category();
        if (category == null) {
            templates.add(template("Sort order", "How shop items are ordered", null, null));
            ClientPrefs.toggles().forEach(toggle ->
                    templates.add(template(toggle.label(), toggle.hint(), null, null)));
            return templates;
        }

        Settings.editable().stream()
                .filter(spec -> spec.category() == category)
                .forEach(spec -> templates.add(template(spec.label(), spec.hint(), spec, null)));
        return templates;
    }

    private static Template template(String label, String hint, Settings.Spec spec, String shopName) {
        return new Template(label, hint, spec, shopName);
    }

    /**
     * Builds widgets for the rows currently on screen, and only those.
     */
    private void buildRows() {
        Settings.Category category = category();
        boolean stacked = category != null && category.isLongText();

        for (int position = 0; position < rows.size(); position++) {
            Row row = rows.get(position);
            int top = rowTop(row);

            if (top + row.height() < formTop() || top > formBottom()) {
                continue;
            }

            if (MANAGE_TAB.equals(tab)) {
                buildManageRow(row, top);
            } else if (category == null) {
                buildOwnRow(position, top);
            } else if (stacked) {
                addStackedBox(row, top);
            } else {
                addBesideControl(row, top);
            }
        }
    }

    // --------------------------------------------------------- client prefs

    private void buildOwnRow(int position, int top) {
        int left = panelRight() - UiTheme.PADDING - UiTheme.SCROLLBAR_WIDTH - CONTROL_WIDTH;
        int centred = top + 3;

        if (position == 0) {
            addRenderableWidget(Button.builder(Component.literal(ClientPrefs.sort().label()), button -> {
                        ClientPrefs.setSort(ClientPrefs.sort().next());
                        button.setMessage(Component.literal(ClientPrefs.sort().label()));
                        parent().onPrefsChanged();
                    })
                    .bounds(left, centred, CONTROL_WIDTH, CONTROL_HEIGHT)
                    .build());
            return;
        }

        ClientPrefs.Toggle toggle = ClientPrefs.toggles().get(position - 1);
        addRenderableWidget(Button.builder(onOff(toggle.read().getAsBoolean()), click -> {
                    boolean next = !toggle.read().getAsBoolean();
                    toggle.write().accept(next);
                    click.setMessage(onOff(next));
                    parent().onPrefsChanged();
                })
                .bounds(left, centred, CONTROL_WIDTH, CONTROL_HEIGHT)
                .build());
    }

    // ------------------------------------------------------ server settings

    private void addBesideControl(Row row, int top) {
        Settings.Spec spec = row.spec();
        int left = panelRight() - UiTheme.PADDING - UiTheme.SCROLLBAR_WIDTH - CONTROL_WIDTH;
        int centred = top + (row.height() - CONTROL_HEIGHT) / 2;

        if (spec.kind() == Settings.Kind.FLAG) {
            addRenderableWidget(Button.builder(onOff(flagOf(spec.key())), click -> {
                        boolean next = !flagOf(spec.key());
                        pending.put(spec.key(), String.valueOf(next));
                        click.setMessage(onOff(next));
                    })
                    .bounds(left, centred, CONTROL_WIDTH, CONTROL_HEIGHT)
                    .build());
            return;
        }

        addRenderableWidget(box(spec, left, centred, CONTROL_WIDTH));
    }

    private void addStackedBox(Row row, int top) {
        Settings.Spec spec = row.spec();
        int boxTop = top + LINE_HEIGHT + row.hint().size() * LINE_HEIGHT + 2;
        int boxWidth = contentWidth() - UiTheme.SCROLLBAR_WIDTH - 4;

        addRenderableWidget(box(spec, contentLeft(), boxTop, boxWidth));
    }

    private EditBox box(Settings.Spec spec, int left, int top, int width) {
        EditBox field = new EditBox(font, left, top, width, CONTROL_HEIGHT,
                Component.literal(spec.label()));
        field.setMaxLength(512);
        field.setValue(pending.getOrDefault(spec.key(), ""));
        field.setResponder(typed -> pending.put(spec.key(), typed));

        if (spec.kind() == Settings.Kind.NUMBER) {
            field.setFilter(typed -> typed.isEmpty() || typed.matches("\\d*\\.?\\d*"));
        }
        return field;
    }

    private boolean flagOf(String key) {
        return Boolean.parseBoolean(pending.getOrDefault(key, "false"));
    }

    /**
     * Sends every server setting rather than only the edited ones: the server
     * compares each against its own current value and reports how many actually
     * differed, so it stays the authority on what changed.
     */
    private void save() {
        PacketDistributor.sendToServer(new SaveSettingsPayload(new LinkedHashMap<>(pending)));
    }

    // -------------------------------------------------------- shop management

    private void buildManageRow(Row row, int top) {
        int left = panelRight() - UiTheme.PADDING - UiTheme.SCROLLBAR_WIDTH - 60;
        int centred = top + (row.height() - CONTROL_HEIGHT) / 2;

        if (row.shopName() == null) {
            newShopName = new EditBox(font, contentLeft() + 70, centred, 140, CONTROL_HEIGHT,
                    Component.literal("Shop name"));
            newShopName.setMaxLength(32);
            newShopName.setHint(Component.literal("name"));
            addRenderableWidget(newShopName);

            addRenderableWidget(Button.builder(Component.literal("Create"), button -> createShop())
                    .bounds(left, centred, 60, CONTROL_HEIGHT)
                    .build());
            return;
        }

        String shop = row.shopName();
        boolean confirming = shop.equals(pendingDelete);

        // Two clicks, because deleting a shop is not something to do by
        // brushing against a button.
        addRenderableWidget(Button.builder(
                        Component.literal(confirming ? "Sure?" : "Delete"), button -> {
                            if (confirming) {
                                deleteShop(shop);
                            } else {
                                pendingDelete = shop;
                                rebuildWidgets();
                            }
                        })
                .bounds(left, centred, 60, CONTROL_HEIGHT)
                .build());
    }

    private void createShop() {
        String name = newShopName == null ? "" : newShopName.getValue().trim();
        if (name.isEmpty()) {
            setStatus("Type a name first.", false);
            return;
        }

        PacketDistributor.sendToServer(
                new ShopAdminPayload(ShopAdminPayload.Action.CREATE_SHOP, name, 0, ""));
        newShopName.setValue("");
    }

    private void deleteShop(String shop) {
        pendingDelete = null;
        PacketDistributor.sendToServer(
                new ShopAdminPayload(ShopAdminPayload.Action.DELETE_SHOP, shop, 0, ""));
    }

    // ---------------------------------------------------------------- render

    @Override
    protected int footerTextLeft() {
        return contentLeft() + (canEdit && category() != null ? 68 : 0);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, getTitle(), canEdit ? "Operator" : null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Labels are drawn after the widgets so they can be clipped to the form
        // without clipping the tab strip or the footer buttons, which are
        // drawn by super.
        graphics.enableScissor(panelLeft, formTop(), panelRight(), formBottom());
        renderLabels(graphics);
        graphics.disableScissor();

        renderScrollbar(graphics);
        renderFooterDivider(graphics);

        if (hasStatus()) {
            renderStatus(graphics);
            return;
        }
        graphics.drawString(font, footerHint(), footerTextLeft(), footerTop() + 9,
                UiTheme.TEXT_MUTED, false);
    }

    private String footerHint() {
        if (MANAGE_TAB.equals(tab)) {
            return "Deleted shops keep a .deleted backup file";
        }
        if (category() == null) {
            return "Your own preferences, saved on this computer";
        }
        return "Changes apply to everyone on the server";
    }

    private void renderLabels(GuiGraphics graphics) {
        for (Row row : rows) {
            int top = rowTop(row);
            if (top + row.height() < formTop() || top > formBottom()) {
                continue;
            }

            graphics.drawString(font, row.label(), contentLeft(), top + 1, UiTheme.TEXT, false);

            int line = top + 1 + LINE_HEIGHT;
            for (FormattedCharSequence hint : row.hint()) {
                graphics.drawString(font, hint, contentLeft(), line, UiTheme.TEXT_MUTED, false);
                line += LINE_HEIGHT;
            }
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int range = maxScroll();
        if (range == 0) {
            return;
        }

        int trackLeft = panelRight() - UiTheme.PADDING + 2;
        int trackRight = trackLeft + UiTheme.SCROLLBAR_WIDTH;
        graphics.fill(trackLeft, formTop(), trackRight, formBottom(), UiTheme.SCROLL_TRACK);

        int visible = visibleHeight();
        int thumbHeight = Math.max(16, visible * visible / totalRowHeight());
        int thumbTop = formTop() + (visible - thumbHeight) * scroll / range;
        graphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, UiTheme.SCROLL_THUMB);
    }

    // ----------------------------------------------------------------- input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (maxScroll() > 0 && mouseY >= formTop() && mouseY < formBottom()) {
            int previous = scroll;
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (deltaY * 18)));

            if (scroll != previous) {
                // Rows are positioned when built, and only the visible ones
                // exist, so moving means rebuilding. The typed values live in
                // pending, so nothing is lost.
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private static Component onOff(boolean on) {
        return Component.literal(on ? "On" : "Off");
    }
}
