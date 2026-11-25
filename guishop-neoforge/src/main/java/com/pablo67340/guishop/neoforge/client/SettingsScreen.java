package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.config.Settings;
import com.pablo67340.guishop.neoforge.network.SaveSettingsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
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

    private static final int LABEL_WIDTH = 170;
    private static final int ROW_HEIGHT = 22;
    private static final int CONTROL_WIDTH = 140;
    private static final int TABS_HEIGHT = 22;

    private final boolean canEdit;

    /** The server's current values, as sent with the catalogue. */
    private final Map<String, String> serverValues;

    /** Text-valued server settings by key, present only on the server tab. */
    private final Map<String, EditBox> serverBoxes = new LinkedHashMap<>();

    /** On/off server settings, which are buttons rather than boxes. */
    private final Map<String, Boolean> serverFlags = new LinkedHashMap<>();

    private Tab tab = Tab.DISPLAY;

    SettingsScreen(ShopBrowserScreen parent, boolean canEdit, Map<String, String> serverValues) {
        super(Component.literal("GUIShop settings"), parent);
        this.canEdit = canEdit;
        this.serverValues = new LinkedHashMap<>(serverValues);
    }

    @Override
    protected void init() {
        int rows = rowLabels().size();
        layoutPanel(430, UiTheme.HEADER_HEIGHT + TABS_HEIGHT + rows * ROW_HEIGHT
                + UiTheme.FOOTER_HEIGHT + 10);

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

    private int formTop() {
        return headerBottom() + TABS_HEIGHT + 4;
    }

    private int controlLeft() {
        return contentLeft() + LABEL_WIDTH;
    }

    // ----------------------------------------------------------- display tab

    private void buildDisplayTab() {
        int row = formTop();

        addRenderableWidget(Button.builder(sortLabel(), button -> {
                    ClientPrefs.setSort(ClientPrefs.sort().next());
                    button.setMessage(sortLabel());
                    parent().onPrefsChanged();
                })
                .bounds(controlLeft(), row, CONTROL_WIDTH, 16)
                .build());
        row += ROW_HEIGHT;

        for (ClientPrefs.Toggle toggle : ClientPrefs.toggles()) {
            addRenderableWidget(Button.builder(onOff(toggle.read().getAsBoolean()), click -> {
                        boolean next = !toggle.read().getAsBoolean();
                        toggle.write().accept(next);
                        click.setMessage(onOff(next));
                        parent().onPrefsChanged();
                    })
                    .bounds(controlLeft(), row, CONTROL_WIDTH, 16)
                    .build());
            row += ROW_HEIGHT;
        }
    }

    private static Component sortLabel() {
        return Component.literal(ClientPrefs.sort().label());
    }

    // ------------------------------------------------------------ server tab

    private void buildServerTab() {
        int row = formTop();

        for (Settings.Spec spec : Settings.editable()) {
            String value = serverValues.getOrDefault(spec.key(), "");

            if (spec.kind() == Settings.Kind.FLAG) {
                String key = spec.key();
                boolean on = Boolean.parseBoolean(value);
                serverFlags.put(key, on);

                addRenderableWidget(Button.builder(onOff(on), click -> {
                            boolean next = !serverFlags.get(key);
                            serverFlags.put(key, next);
                            click.setMessage(onOff(next));
                        })
                        .bounds(controlLeft(), row, CONTROL_WIDTH, 16)
                        .build());
            } else {
                EditBox box = new EditBox(font, controlLeft(), row, CONTROL_WIDTH, 16,
                        Component.literal(spec.label()));
                box.setMaxLength(512);
                box.setValue(value);
                addRenderableWidget(box);
                serverBoxes.put(spec.key(), box);
            }

            row += ROW_HEIGHT;
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
        int row = formTop();
        for (Label label : rowLabels()) {
            graphics.drawString(font, label.text(), contentLeft(), row, UiTheme.TEXT, false);
            graphics.drawString(font, label.hint(), contentLeft(), row + 10,
                    UiTheme.TEXT_MUTED, false);
            row += ROW_HEIGHT;
        }
    }

    private record Label(String text, String hint) {
    }

    /**
     * The rows of whichever tab is open, used both for drawing the labels and
     * for sizing the panel, so the two cannot disagree.
     */
    private List<Label> rowLabels() {
        List<Label> labels = new ArrayList<>();

        if (tab == Tab.DISPLAY) {
            labels.add(new Label("Sort order", "How shop items are ordered"));
            ClientPrefs.toggles().forEach(toggle ->
                    labels.add(new Label(toggle.label(), toggle.hint())));
        } else {
            Settings.editable().forEach(spec ->
                    labels.add(new Label(spec.label(), spec.hint())));
        }
        return labels;
    }

    private static Component onOff(boolean on) {
        return Component.literal(on ? "On" : "Off");
    }
}
