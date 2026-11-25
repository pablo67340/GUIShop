package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.config.Settings;
import com.pablo67340.guishop.neoforge.network.SaveSettingsPayload;
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
 * GUIShop's settings, in two tabs.
 * <p>
 * The display tab is this player's own preferences and is always there, since
 * how someone likes their shop list sorted should not need a permission. The
 * server tab only appears for someone who may change the server's
 * configuration, and the server checks that again when a change arrives, so
 * hiding the tab is a courtesy rather than the actual protection.
 * <p>
 * Each row is measured before the panel is sized: the hint text is wrapped to
 * whatever space is left beside the control, and the row grows to fit the
 * wrapped result. A fixed row height would overlap the controls as soon as a
 * hint was long or the GUI scale was large.
 */
final class SettingsScreen extends ShopChildScreen {

    private enum Tab {
        DISPLAY("Display"),
        SERVER("Server");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int PANEL_WIDTH = 440;
    private static final int CONTROL_WIDTH = 120;

    /** Between the label text and the control, so the two never touch. */
    private static final int COLUMN_GAP = 12;

    private static final int TABS_HEIGHT = 22;
    private static final int CONTROL_HEIGHT = 16;
    private static final int LINE_HEIGHT = 9;

    private final boolean canEdit;

    /** The server's current values, as sent with the catalogue. */
    private final Map<String, String> serverValues;

    /** Text-valued server settings by key, present only on the server tab. */
    private final Map<String, EditBox> serverBoxes = new LinkedHashMap<>();

    /** On/off server settings, which are buttons rather than boxes. */
    private final Map<String, Boolean> serverFlags = new LinkedHashMap<>();

    private Tab tab = Tab.DISPLAY;

    /** The rows of the open tab, measured and positioned. */
    private List<Row> rows = List.of();

    /**
     * One settings row, already laid out.
     * <p>
     * The position is an offset from the top of the form rather than a screen
     * coordinate, because the panel is centred after the rows are measured and
     * a screen coordinate taken beforehand would be stale.
     *
     * @param hint   the hint wrapped to the label column
     * @param offset where the row starts, measured from the form's top
     */
    private record Row(String label, List<FormattedCharSequence> hint, int offset, int height) {
    }

    SettingsScreen(ShopBrowserScreen parent, boolean canEdit, Map<String, String> serverValues) {
        super(Component.literal("GUIShop settings"), parent);
        this.canEdit = canEdit;
        this.serverValues = new LinkedHashMap<>(serverValues);
    }

    @Override
    protected void init() {
        // Sized in two passes. The panel's width decides how narrow the label
        // column is, which decides how many lines each hint wraps to, which is
        // what the panel's height has to accommodate.
        layoutPanel(PANEL_WIDTH, height);
        rows = measureRows();

        int needed = UiTheme.HEADER_HEIGHT + TABS_HEIGHT + totalRowHeight()
                + UiTheme.FOOTER_HEIGHT + 8;
        layoutPanel(PANEL_WIDTH, needed);

        serverBoxes.clear();
        serverFlags.clear();

        addTabButtons();

        if (tab == Tab.DISPLAY) {
            buildDisplayTab();
        } else {
            buildServerTab();
        }

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(panelRight() - UiTheme.PADDING - 50, footerTop() + 4, 50, 16)
                .build());
    }

    private void addTabButtons() {
        int left = contentLeft();
        for (Tab option : Tab.values()) {
            if (option == Tab.SERVER && !canEdit) {
                continue;
            }

            Button button = Button.builder(Component.literal(option.label), click -> {
                        tab = option;
                        rebuildWidgets();
                    })
                    .bounds(left, headerBottom() + 4, 70, 16)
                    .build();
            // Pressing the tab you are already on does nothing useful.
            button.active = option != tab;
            addRenderableWidget(button);
            left += 74;
        }
    }

    // ----------------------------------------------------------------- layout

    private int labelWidth() {
        return contentWidth() - CONTROL_WIDTH - COLUMN_GAP;
    }

    private int controlLeft() {
        return panelRight() - UiTheme.PADDING - CONTROL_WIDTH;
    }

    private int formTop() {
        return headerBottom() + TABS_HEIGHT + 4;
    }

    private int totalRowHeight() {
        return rows.isEmpty() ? 0 : rows.getLast().offset() + rows.getLast().height();
    }

    private int rowTop(Row row) {
        return formTop() + row.offset();
    }

    /**
     * Wraps each hint to the label column and stacks the rows, so drawing and
     * widget placement both read the same measurements.
     */
    private List<Row> measureRows() {
        List<Row> measured = new ArrayList<>();
        int offset = 0;

        for (Hint hint : hints()) {
            List<FormattedCharSequence> wrapped =
                    font.split(Component.literal(hint.hint()), labelWidth());

            int height = Math.max(CONTROL_HEIGHT + 6,
                    LINE_HEIGHT + wrapped.size() * LINE_HEIGHT + 5);

            measured.add(new Row(hint.label(), wrapped, offset, height));
            offset += height;
        }
        return List.copyOf(measured);
    }

    private record Hint(String label, String hint) {
    }

    /**
     * The rows of whichever tab is open, in order.
     */
    private List<Hint> hints() {
        List<Hint> labels = new ArrayList<>();

        if (tab == Tab.DISPLAY) {
            labels.add(new Hint("Sort order", "How shop items are ordered"));
            ClientPrefs.toggles().forEach(toggle ->
                    labels.add(new Hint(toggle.label(), toggle.hint())));
        } else {
            Settings.editable().forEach(spec ->
                    labels.add(new Hint(spec.label(), spec.hint())));
        }
        return labels;
    }

    /**
     * Controls sit vertically centred against their row, so a row that grew to
     * fit two lines of hint does not leave its control stuck at the top.
     */
    private int controlTop(int position) {
        Row row = rows.get(position);
        return rowTop(row) + (row.height() - CONTROL_HEIGHT) / 2;
    }

    // ----------------------------------------------------------- display tab

    private void buildDisplayTab() {
        addRenderableWidget(Button.builder(sortLabel(), button -> {
                    ClientPrefs.setSort(ClientPrefs.sort().next());
                    button.setMessage(sortLabel());
                    parent().onPrefsChanged();
                })
                .bounds(controlLeft(), controlTop(0), CONTROL_WIDTH, CONTROL_HEIGHT)
                .build());

        List<ClientPrefs.Toggle> toggles = ClientPrefs.toggles();
        for (int position = 0; position < toggles.size(); position++) {
            ClientPrefs.Toggle toggle = toggles.get(position);

            addRenderableWidget(Button.builder(onOff(toggle.read().getAsBoolean()), click -> {
                        boolean next = !toggle.read().getAsBoolean();
                        toggle.write().accept(next);
                        click.setMessage(onOff(next));
                        parent().onPrefsChanged();
                    })
                    .bounds(controlLeft(), controlTop(position + 1), CONTROL_WIDTH, CONTROL_HEIGHT)
                    .build());
        }
    }

    private static Component sortLabel() {
        return Component.literal(ClientPrefs.sort().label());
    }

    // ------------------------------------------------------------ server tab

    private void buildServerTab() {
        List<Settings.Spec> specs = Settings.editable();

        for (int position = 0; position < specs.size(); position++) {
            Settings.Spec spec = specs.get(position);
            String value = serverValues.getOrDefault(spec.key(), "");
            int top = controlTop(position);

            if (spec.kind() == Settings.Kind.FLAG) {
                String key = spec.key();
                boolean on = Boolean.parseBoolean(value);
                serverFlags.put(key, on);

                addRenderableWidget(Button.builder(onOff(on), click -> {
                            boolean next = !serverFlags.get(key);
                            serverFlags.put(key, next);
                            click.setMessage(onOff(next));
                        })
                        .bounds(controlLeft(), top, CONTROL_WIDTH, CONTROL_HEIGHT)
                        .build());
            } else {
                EditBox box = new EditBox(font, controlLeft(), top, CONTROL_WIDTH, CONTROL_HEIGHT,
                        Component.literal(spec.label()));
                box.setMaxLength(512);
                box.setValue(value);

                if (spec.kind() == Settings.Kind.NUMBER) {
                    box.setFilter(typed -> typed.isEmpty() || typed.matches("\\d*\\.?\\d*"));
                }

                addRenderableWidget(box);
                serverBoxes.put(spec.key(), box);
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveServerSettings())
                .bounds(contentLeft(), footerTop() + 4, 60, 16)
                .build());
    }

    /**
     * Sends every server setting the screen shows rather than only the edited
     * ones: the server compares each against its own current value and reports
     * how many actually differed, so it stays the authority on what changed.
     */
    private void saveServerSettings() {
        Map<String, String> values = new LinkedHashMap<>();
        serverFlags.forEach((key, on) -> values.put(key, String.valueOf(on)));
        serverBoxes.forEach((key, box) -> values.put(key, box.getValue()));

        PacketDistributor.sendToServer(new SaveSettingsPayload(values));
    }

    // ---------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        renderPanel(graphics, getTitle(), canEdit ? "Operator" : null);
        renderLabels(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        renderFooterDivider(graphics);
        if (hasStatus()) {
            renderStatus(graphics);
            return;
        }

        String hint = tab == Tab.DISPLAY
                ? "Your own preferences, saved on this computer"
                : "Changes apply to everyone on the server";
        graphics.drawString(font, hint, contentLeft() + 68, footerTop() + 9,
                UiTheme.TEXT_MUTED, false);
    }

    private void renderLabels(GuiGraphics graphics) {
        for (Row row : rows) {
            graphics.drawString(font, row.label(), contentLeft(), rowTop(row) + 1,
                    UiTheme.TEXT, false);

            int line = rowTop(row) + 1 + LINE_HEIGHT;
            for (FormattedCharSequence hint : row.hint()) {
                graphics.drawString(font, hint, contentLeft(), line, UiTheme.TEXT_MUTED, false);
                line += LINE_HEIGHT;
            }
        }
    }

    private static Component onOff(boolean on) {
        return Component.literal(on ? "On" : "Off");
    }
}
