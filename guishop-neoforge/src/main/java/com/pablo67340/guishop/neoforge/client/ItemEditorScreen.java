package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.SaveItemPayload;
import com.pablo67340.guishop.neoforge.network.ShopEntryData;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Edits one shop item's properties.
 * <p>
 * Replaces the plugin's shift-click editing, which only worked that way
 * because a chest inventory had no room for controls. Here the fields are just
 * fields, and the item is shown large enough to see what the changes did.
 * <p>
 * Laid out in two columns so every property is visible at once. A scrolling
 * form would need fewer decisions about sizing, but hiding half an item's
 * configuration behind a scrollbar is exactly the sort of thing that makes an
 * editor annoying to use.
 * <p>
 * Everything is edited as the text the config holds, including the '&amp;'
 * colour codes, because that is what a server owner already knows how to write
 * and it saves the editor from having to model every property type.
 */
final class ItemEditorScreen extends ShopChildScreen {

    /**
     * The single-line properties, filled down the left column then the right.
     */
    private static final List<Field> SIMPLE_FIELDS = List.of(
            new Field("id", "Item ID"),
            new Field("type", "Type"),
            new Field("buy-price", "Buy price"),
            new Field("sell-price", "Sell price"),
            new Field("shop-name", "Shop name"),
            new Field("buy-name", "Bought name"),
            new Field("name", "Name"),
            new Field("enchantments", "Enchantments"),
            new Field("item-flags", "Item flags"),
            new Field("custom-model-data", "Model data"),
            new Field("mob-type", "Mob type"),
            new Field("target-shop", "Target shop"),
            new Field("nbt", "NBT"));

    /**
     * The list properties, shown side by side as one entry per line.
     * <p>
     * Potion and firework details are deliberately absent: they are structured
     * values that want a picker rather than a text box, and a half-working text
     * form would be a good way to corrupt a working item.
     */
    private static final List<Field> LIST_FIELDS = List.of(
            new Field("lore", "Lore"),
            new Field("commands", "Commands"));

    private record Field(String key, String label) {
    }

    /**
     * One form field. Held as accessors rather than as a widget because a list
     * field is a MultiLineEditBox and the rest are EditBoxes, and the two share
     * no text interface.
     */
    private record TextField(Supplier<String> read, Consumer<String> write) {
    }

    private static final int LABEL_WIDTH = 74;
    private static final int ROW_HEIGHT = 19;
    private static final int LIST_HEIGHT = 46;
    private static final int COLUMN_GAP = 10;

    /** Matches the cap the save payload enforces. */
    private static final int MAX_FIELD_LENGTH = 4096;

    private final String shopName;
    private final int index;
    private final ShopEntryData entry;

    /** The values as loaded, to tell what the editor actually changed. */
    private final Map<String, String> original;

    private final Map<String, TextField> fields = new LinkedHashMap<>();

    ItemEditorScreen(ShopBrowserScreen parent, String shopName, int index, ShopEntryData entry,
                     Map<String, String> values) {
        super(Component.literal("Edit item"), parent);
        this.shopName = shopName;
        this.index = index;
        this.entry = entry;
        this.original = new LinkedHashMap<>(values);
    }

    @Override
    protected void init() {
        int rows = (SIMPLE_FIELDS.size() + 1) / 2;
        int needed = UiTheme.HEADER_HEIGHT + previewHeight() + rows * ROW_HEIGHT
                + 14 + LIST_HEIGHT + UiTheme.FOOTER_HEIGHT + 8;

        layoutPanel(460, needed);
        fields.clear();

        int columnWidth = (contentWidth() - COLUMN_GAP) / 2;
        int boxWidth = columnWidth - LABEL_WIDTH;

        for (int position = 0; position < SIMPLE_FIELDS.size(); position++) {
            Field field = SIMPLE_FIELDS.get(position);
            int column = position / rows;
            int row = position % rows;

            int left = contentLeft() + column * (columnWidth + COLUMN_GAP);
            int top = formTop() + row * ROW_HEIGHT;

            EditBox box = new EditBox(font, left + LABEL_WIDTH, top, boxWidth, 15,
                    Component.literal(field.label()));
            box.setMaxLength(MAX_FIELD_LENGTH);
            box.setValue(original.getOrDefault(field.key(), ""));
            addRenderableWidget(box);
            fields.put(field.key(), new TextField(box::getValue, box::setValue));
        }

        int listTop = formTop() + rows * ROW_HEIGHT + 14;
        for (int position = 0; position < LIST_FIELDS.size(); position++) {
            Field field = LIST_FIELDS.get(position);
            int left = contentLeft() + position * (columnWidth + COLUMN_GAP);

            MultiLineEditBox box = new MultiLineEditBox(font, left, listTop, columnWidth, LIST_HEIGHT,
                    Component.literal("One per line"), Component.literal(field.label()));
            box.setCharacterLimit(MAX_FIELD_LENGTH);
            box.setValue(original.getOrDefault(field.key(), ""));
            addRenderableWidget(box);
            fields.put(field.key(), new TextField(box::getValue, box::setValue));
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(contentLeft(), footerTop() + 4, 60, 16)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(contentLeft() + 64, footerTop() + 4, 60, 16)
                .build());
    }

    private static int previewHeight() {
        return 42;
    }

    private int formTop() {
        return headerBottom() + previewHeight();
    }

    // ------------------------------------------------------------------ save

    /**
     * Sends only what changed, so a field the editor never touched is not
     * rewritten and cannot be reformatted by a round trip through the screen.
     */
    private void save() {
        Map<String, String> changed = new LinkedHashMap<>();

        fields.forEach((key, field) -> {
            String value = field.read().get();
            if (!value.equals(original.getOrDefault(key, ""))) {
                changed.put(key, value);
            }
        });

        if (changed.isEmpty()) {
            setStatus("Nothing changed.", false);
            return;
        }

        PacketDistributor.sendToServer(new SaveItemPayload(shopName, index, changed));
    }

    @Override
    protected void onServerResult(TradeResultPayload result) {
        if (result.success()) {
            // The browser already has the new catalogue by now, so going back
            // shows the edit rather than the old values.
            onClose();
        }
    }

    // ---------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        renderPanel(graphics, getTitle(), shopName + " #" + (index + 1));
        renderPreview(graphics);
        renderLabels(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        renderFooterDivider(graphics);
        if (hasStatus()) {
            renderStatus(graphics);
        } else {
            graphics.drawString(font, "Blank a field to remove it",
                    contentLeft() + 128, footerTop() + 9, UiTheme.TEXT_MUTED, false);
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        int top = headerBottom() + 6;

        // Scaling the pose rather than the sprite keeps the model, the
        // enchantment glint and any overlays.
        graphics.pose().pushPose();
        graphics.pose().translate(contentLeft() + 2, top, 0.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderFakeItem(entry.icon(), 0, 0);
        graphics.pose().popPose();

        graphics.drawString(font, Contrast.readable(entry.icon().getHoverName()),
                contentLeft() + 42, top + 4, UiTheme.TEXT, false);

        for (Component line : entry.priceLines()) {
            graphics.drawString(font, Contrast.readable(line),
                    contentLeft() + 42, top + 16, UiTheme.TEXT_MUTED, false);
            // Only the first line: the preview is a reminder, not the tooltip.
            break;
        }
    }

    private void renderLabels(GuiGraphics graphics) {
        int rows = (SIMPLE_FIELDS.size() + 1) / 2;
        int columnWidth = (contentWidth() - COLUMN_GAP) / 2;

        for (int position = 0; position < SIMPLE_FIELDS.size(); position++) {
            int left = contentLeft() + (position / rows) * (columnWidth + COLUMN_GAP);
            int top = formTop() + (position % rows) * ROW_HEIGHT;

            graphics.drawString(font, SIMPLE_FIELDS.get(position).label(), left, top + 4,
                    UiTheme.TEXT_MUTED, false);
        }

        int listLabelTop = formTop() + rows * ROW_HEIGHT + 4;
        for (int position = 0; position < LIST_FIELDS.size(); position++) {
            graphics.drawString(font, LIST_FIELDS.get(position).label(),
                    contentLeft() + position * (columnWidth + COLUMN_GAP), listLabelTop,
                    UiTheme.TEXT_MUTED, false);
        }
    }
}
