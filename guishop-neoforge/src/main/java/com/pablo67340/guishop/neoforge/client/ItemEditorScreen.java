package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import com.pablo67340.guishop.neoforge.network.SaveItemPayload;
import com.pablo67340.guishop.neoforge.network.ShopAdminPayload;
import com.pablo67340.guishop.neoforge.network.ShopEntryData;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Edits one shop item's properties.
 * <p>
 * Replaces the plugin's shift-click editing, which only worked that way
 * because a chest inventory had no room for controls. Here the fields are just
 * fields, and the item is shown large enough to see what the changes did.
 * <p>
 * Anything with a knowable set of answers - the item, its type, its mob, its
 * enchantments, which shop it links to - opens a picker rather than a text box,
 * so those properties cannot be misspelt. Only genuinely free text, such as a
 * display name or raw NBT, is still typed.
 */
final class ItemEditorScreen extends ShopChildScreen {

    private enum Kind {
        /** Free text. */
        TEXT,
        /** Digits and a decimal point. */
        NUMBER,
        /** Opens a picker or a sub-editor. */
        CHOOSE
    }

    private record Field(String key, String label, Kind kind) {
    }

    /**
     * The single-line properties, filled down the left column then the right.
     */
    private static final List<Field> SIMPLE_FIELDS = List.of(
            new Field("id", "Item", Kind.CHOOSE),
            new Field("type", "Type", Kind.CHOOSE),
            new Field("buy-price", "Buy price", Kind.NUMBER),
            new Field("sell-price", "Sell price", Kind.NUMBER),
            new Field("shop-name", "Shop name", Kind.TEXT),
            new Field("buy-name", "Bought name", Kind.TEXT),
            new Field("name", "Name", Kind.TEXT),
            new Field("enchantments", "Enchantments", Kind.CHOOSE),
            new Field("item-flags", "Hide in tooltip", Kind.CHOOSE),
            new Field("custom-model-data", "Model data", Kind.NUMBER),
            new Field("mob-type", "Spawner mob", Kind.CHOOSE),
            new Field("target-shop", "Links to shop", Kind.CHOOSE),
            new Field("nbt", "NBT", Kind.TEXT));

    /**
     * The list properties, shown side by side as one entry per line.
     * <p>
     * Potion and firework details are deliberately absent: they are structured
     * values that want their own pickers, and a text box for them would be a
     * good way to corrupt a working item.
     */
    private static final List<Field> LIST_FIELDS = List.of(
            new Field("lore", "Lore", Kind.TEXT),
            new Field("commands", "Commands", Kind.TEXT));

    /**
     * The types worth offering. The navigation types the chest GUI needed -
     * page arrows, filler panes, the balance icon - are left out because this
     * UI builds its own controls and drops those entries on sight.
     */
    private static final List<ItemType> TYPES = List.of(
            ItemType.SHOP, ItemType.ITEM, ItemType.COMMAND,
            ItemType.SHOP_SHORTCUT, ItemType.PLAYER_HEAD, ItemType.DUMMY);

    private static final Map<ItemType, String> TYPE_HINTS = Map.of(
            ItemType.SHOP, "a normal item, bought and sold",
            ItemType.ITEM, "the same as SHOP",
            ItemType.COMMAND, "runs commands instead of giving the item",
            ItemType.SHOP_SHORTCUT, "opens another shop",
            ItemType.PLAYER_HEAD, "a player's head",
            ItemType.DUMMY, "decoration, not purchasable");

    private static final int LABEL_WIDTH = 74;
    private static final int ROW_HEIGHT = 19;
    private static final int LIST_HEIGHT = 42;

    /** Room under the lore boxes for the character count vanilla draws there. */
    private static final int COUNTER_SPACE = 15;
    private static final int COLUMN_GAP = 10;
    private static final int PREVIEW_HEIGHT = 42;

    /** Matches the cap the save payload enforces. */
    private static final int MAX_FIELD_LENGTH = 4096;

    private final String shopName;
    private final int index;
    private final ShopEntryData entry;

    /** The values as loaded, to tell what the editor actually changed. */
    private final Map<String, String> original;

    /**
     * The values as they stand now.
     * <p>
     * Held here rather than read off the widgets so that rebuilding the form -
     * which happens whenever a picker returns - cannot lose what has been
     * typed into the other fields.
     */
    private final Map<String, String> values;

    /** Set after the first Delete click, so a second one means it. */
    private boolean confirmingDelete;

    ItemEditorScreen(ShopBrowserScreen parent, String shopName, int index, ShopEntryData entry,
                     Map<String, String> values) {
        super(Component.literal("Edit item"), parent);
        this.shopName = shopName;
        this.index = index;
        this.entry = entry;
        this.original = new LinkedHashMap<>(values);
        this.values = new LinkedHashMap<>(values);
    }

    // ----------------------------------------------------------------- layout

    @Override
    protected void init() {
        int rows = (SIMPLE_FIELDS.size() + 1) / 2;

        // COUNTER_SPACE because MultiLineEditBox draws its character count
        // four pixels under itself; without the room it lands on the footer
        // divider.
        layoutPanel(470, UiTheme.HEADER_HEIGHT + PREVIEW_HEIGHT + rows * ROW_HEIGHT
                + 14 + LIST_HEIGHT + COUNTER_SPACE + UiTheme.FOOTER_HEIGHT);

        for (Slot slot : simpleSlots()) {
            Field field = slot.field();

            // A field the type ignores gets no widget at all. It is drawn as a
            // flat panel instead, which cannot be clicked or tabbed into and,
            // unlike a non-editable EditBox, actually looks inert.
            if (!isLive(field)) {
                continue;
            }

            if (field.kind() == Kind.CHOOSE) {
                addRenderableWidget(Button.builder(chooseLabel(field, slot.controlWidth()),
                                button -> choose(field))
                        .bounds(slot.controlLeft(), slot.top(), slot.controlWidth(), 15)
                        .build());
            } else {
                addRenderableWidget(textBox(field, slot.controlLeft(), slot.top(),
                        slot.controlWidth()));
            }
        }

        for (Slot slot : listSlots()) {
            Field field = slot.field();
            if (!isLive(field)) {
                continue;
            }

            MultiLineEditBox box = new MultiLineEditBox(font, slot.controlLeft(), slot.top(),
                    slot.controlWidth(), LIST_HEIGHT,
                    Component.literal("One per line"), Component.literal(field.label()));
            box.setCharacterLimit(MAX_FIELD_LENGTH);
            box.setValue(values.getOrDefault(field.key(), ""));
            box.setValueListener(typed -> values.put(field.key(), typed));
            addRenderableWidget(box);
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(contentLeft(), footerTop() + 4, 60, 16)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(contentLeft() + 64, footerTop() + 4, 60, 16)
                .build());

        // Two clicks, since there is no undo for removing an entry.
        addRenderableWidget(Button.builder(
                        Component.literal(confirmingDelete ? "Sure?" : "Delete"), button -> {
                            if (confirmingDelete) {
                                deleteItem();
                            } else {
                                confirmingDelete = true;
                                rebuildWidgets();
                            }
                        })
                .bounds(contentLeft() + 128, footerTop() + 4, 54, 16)
                .build());
    }

    private void deleteItem() {
        confirmingDelete = false;
        PacketDistributor.sendToServer(
                new ShopAdminPayload(ShopAdminPayload.Action.DELETE_ITEM, shopName, index, ""));
        // The server renumbers the remaining entries, so this screen's id is
        // stale the moment it succeeds.
        onClose();
    }

    private EditBox textBox(Field field, int left, int top, int width) {
        EditBox box = new EditBox(font, left, top, width, 15, Component.literal(field.label()));
        box.setMaxLength(MAX_FIELD_LENGTH);
        box.setValue(values.getOrDefault(field.key(), ""));
        box.setResponder(typed -> values.put(field.key(), typed));

        if (field.kind() == Kind.NUMBER) {
            // Rejected as it is typed rather than complained about on save,
            // which is both fewer words and impossible to get wrong.
            box.setFilter(ItemEditorScreen::isNumeric);
        }
        return box;
    }

    private static boolean isNumeric(String typed) {
        return typed.isEmpty() || typed.matches("\\d*\\.?\\d*");
    }

    private int formTop() {
        return headerBottom() + PREVIEW_HEIGHT;
    }

    /**
     * Where one field's label and control sit.
     * <p>
     * Worked out once and used both to place the widgets and to draw the
     * labels and the inert fields, so the two cannot drift apart.
     */
    private record Slot(Field field, int labelLeft, int controlLeft, int top, int controlWidth) {
    }

    private int columnWidth() {
        return (contentWidth() - COLUMN_GAP) / 2;
    }

    private List<Slot> simpleSlots() {
        int rows = (SIMPLE_FIELDS.size() + 1) / 2;
        int columnWidth = columnWidth();
        List<Slot> slots = new ArrayList<>(SIMPLE_FIELDS.size());

        for (int position = 0; position < SIMPLE_FIELDS.size(); position++) {
            int labelLeft = contentLeft() + (position / rows) * (columnWidth + COLUMN_GAP);
            slots.add(new Slot(SIMPLE_FIELDS.get(position), labelLeft, labelLeft + LABEL_WIDTH,
                    formTop() + (position % rows) * ROW_HEIGHT, columnWidth - LABEL_WIDTH));
        }
        return slots;
    }

    private List<Slot> listSlots() {
        int columnWidth = columnWidth();
        int top = formTop() + ((SIMPLE_FIELDS.size() + 1) / 2) * ROW_HEIGHT + 14;
        List<Slot> slots = new ArrayList<>(LIST_FIELDS.size());

        for (int position = 0; position < LIST_FIELDS.size(); position++) {
            int left = contentLeft() + position * (columnWidth + COLUMN_GAP);
            slots.add(new Slot(LIST_FIELDS.get(position), left, left, top, columnWidth));
        }
        return slots;
    }

    // ------------------------------------------------------------- relevance

    /**
     * Whether a field means anything for the type currently chosen.
     * <p>
     * A shortcut has no price and a plain item has no shop to open, so rather
     * than letting someone fill in a field that will be ignored, the Type
     * picker decides which fields are live. Switching a menu icon from
     * SHOP_SHORTCUT to SHOP is how you sell an item straight from the menu
     * instead of nesting a shop behind it.
     */
    private boolean isLive(Field field) {
        ItemType type = currentType();

        return switch (field.key()) {
            case "buy-price", "sell-price", "buy-name" -> type.isPurchasable();
            case "commands" -> type == ItemType.COMMAND;
            case "target-shop" -> type == ItemType.SHOP_SHORTCUT;
            // Only a spawner has a mob, whatever the type says.
            case "mob-type" -> "SPAWNER".equalsIgnoreCase(values.getOrDefault("id", "").trim());
            default -> true;
        };
    }

    /**
     * What the preview should show.
     * <p>
     * The server's icon carries the name, lore and glint it built, so it is
     * kept while the item is untouched. Once a different item is picked that
     * icon is of the wrong thing, and a plain stack of the new one is a truer
     * preview than a stale picture.
     */
    private ItemStack previewStack() {
        String id = values.getOrDefault("id", "");

        if (id.equals(original.getOrDefault("id", ""))) {
            return entry.icon();
        }

        ItemStack picked = Choices.stackFor(id);
        return picked.isEmpty() ? entry.icon() : picked;
    }

    // ---------------------------------------------------------------- choices

    /**
     * Opens the right picker or sub-editor for a field, each writing its result
     * straight back into {@link #values}.
     */
    private void choose(Field field) {
        switch (field.key()) {
            case "id" -> open(new PickerScreen(this, Component.literal("Choose item"),
                    Choices.items(), values.get("id"), picked -> set(field, picked)));

            case "type" -> open(new PickerScreen(this, Component.literal("Choose type"),
                    typeChoices(), currentType().name(), picked -> set(field, picked)));

            case "mob-type" -> open(new PickerScreen(this, Component.literal("Choose spawner mob"),
                    Choices.entities(), values.get("mob-type"), picked -> set(field, picked)));

            case "target-shop" -> open(new PickerScreen(this, Component.literal("Choose shop"),
                    Choices.shops(parent().shopNames()), values.get("target-shop"),
                    picked -> {
                        if (Choices.NEW_SHOP.equals(picked)) {
                            nameNewShop(field);
                        } else {
                            set(field, picked);
                        }
                    }));

            case "enchantments" -> open(new EnchantmentEditorScreen(this,
                    values.get("enchantments"), picked -> set(field, picked)));

            case "item-flags" -> open(new FlagEditorScreen(this,
                    values.get("item-flags"), picked -> set(field, picked)));

            default -> {
            }
        }
    }

    private void open(net.minecraft.client.gui.screens.Screen screen) {
        minecraft.setScreen(screen);
    }

    /**
     * Names a shop that does not exist yet. The name is only recorded here; the
     * server creates the shop when this item is saved.
     */
    private void nameNewShop(Field field) {
        // Queued rather than opened from inside the picker's callback, which is
        // running while the picker is closing itself back to this screen.
        minecraft.tell(() -> minecraft.setScreen(new ShopNameScreen(
                this, parent().shopNames(), named -> set(field, named))));
    }

    private void set(Field field, String value) {
        values.put(field.key(), value == null ? "" : value);
        // The button's label is its value, so it has to be rebuilt to show it.
        rebuildWidgets();
    }

    private List<PickerScreen.Choice> typeChoices() {
        List<PickerScreen.Choice> choices = new ArrayList<>(TYPES.size());
        for (ItemType type : TYPES) {
            choices.add(PickerScreen.Choice.of(type.name(), Component.literal(type.name()),
                    TYPE_HINTS.getOrDefault(type, ""), ItemStack.EMPTY));
        }
        return choices;
    }

    /**
     * The same rule the loader applies: an entry with no declared type is a
     * plain shop item, unless it names a shop, which makes it a shortcut.
     * <p>
     * Mirrored rather than assumed to be SHOP because menu icons are written
     * with only a {@code target-shop}, and treating one as a shop item would
     * grey out the very field that makes it work.
     */
    private ItemType currentType() {
        return typeOf(values);
    }

    private static ItemType typeOf(Map<String, String> fields) {
        String stored = fields.getOrDefault("type", "");

        if (stored.isBlank()) {
            return fields.getOrDefault("target-shop", "").isBlank()
                    ? ItemType.SHOP
                    : ItemType.SHOP_SHORTCUT;
        }
        try {
            return ItemType.valueOf(stored.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ItemType.SHOP;
        }
    }

    /**
     * What a chooser button reads: its current value, or a prompt when unset,
     * shortened to fit rather than overflowing the button.
     */
    private Component chooseLabel(Field field, int width) {
        String value = values.getOrDefault(field.key(), "");

        String text = switch (field.key()) {
            case "type" -> currentType().name();
            case "enchantments" -> value.isBlank() ? "None" : countOf(value) + " enchantment(s)";
            case "item-flags" -> value.isBlank() ? "Nothing hidden" : countOf(value) + " hidden";
            default -> value.isBlank() ? "Choose..." : value;
        };

        if (font.width(text) > width - 8) {
            text = font.plainSubstrByWidth(text, width - 8 - font.width("...")) + "...";
        }
        return Component.literal(text);
    }

    private static int countOf(String spaceSeparated) {
        return spaceSeparated.trim().split("[,\\s]+").length;
    }

    // ------------------------------------------------------------------ save

    /**
     * Sends only what changed, so a field the editor never touched is not
     * rewritten and cannot be reformatted by a round trip through the screen.
     */
    private void save() {
        Map<String, String> changed = new LinkedHashMap<>();

        values.forEach((key, value) -> {
            if (!value.equals(original.getOrDefault(key, ""))) {
                changed.put(key, value);
            }
        });

        dropStrandedFields(changed);

        if (changed.isEmpty()) {
            setStatus("Nothing changed.", false);
            return;
        }

        PacketDistributor.sendToServer(new SaveItemPayload(shopName, index, changed));
    }

    /**
     * Clears fields the new type has no use for, by blanking them, which is how
     * this editor removes a value.
     * <p>
     * Only when the type was actually changed here. Turning a menu shortcut
     * into a sellable item would otherwise leave its {@code target-shop}
     * behind, and the entry would both open a shop and sell something. Left
     * alone when the type was not touched, so opening an item and saving it
     * cannot quietly discard something that was already in the file.
     */
    private void dropStrandedFields(Map<String, String> changed) {
        if (currentType() == originalType()) {
            return;
        }

        for (Field field : SIMPLE_FIELDS) {
            if (!field.key().equals("type") && !isLive(field)
                    && !values.getOrDefault(field.key(), "").isBlank()) {
                changed.put(field.key(), "");
            }
        }
        for (Field field : LIST_FIELDS) {
            if (!isLive(field) && !values.getOrDefault(field.key(), "").isBlank()) {
                changed.put(field.key(), "");
            }
        }
    }

    private ItemType originalType() {
        return typeOf(original);
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

    /**
     * Clear of the Save and Cancel buttons in the bottom-left.
     */
    @Override
    protected int footerTextLeft() {
        return contentLeft() + 188;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        renderFooterDivider(graphics);
        if (hasStatus()) {
            renderStatus(graphics);
        } else {
            graphics.drawString(font, "Blank a field to remove it",
                    footerTextLeft(), footerTop() + 9, UiTheme.TEXT_MUTED, false);
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        int top = headerBottom() + 6;
        ItemStack preview = previewStack();

        // Scaling the pose rather than the sprite keeps the model, the
        // enchantment glint and any overlays.
        graphics.pose().pushPose();
        graphics.pose().translate(contentLeft() + 2, top, 0.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderFakeItem(preview, 0, 0);
        graphics.pose().popPose();

        // The configured shop name wins over the item's own, since that is
        // what a player will see on the shelf.
        String configured = values.getOrDefault("shop-name", "");
        if (configured.isBlank()) {
            configured = values.getOrDefault("name", "");
        }
        Component name = configured.isBlank()
                ? Contrast.readable(preview.getHoverName())
                : Contrast.readable(TextUtil.toComponent(configured));

        graphics.drawString(font, name, contentLeft() + 42, top + 4, UiTheme.TEXT, false);

        // Only the first line: the preview is a reminder of which item this is,
        // not a second copy of the tooltip.
        for (Component line : entry.priceLines()) {
            graphics.drawString(font, Contrast.readable(line),
                    contentLeft() + 42, top + 16, UiTheme.TEXT_MUTED, false);
            break;
        }
    }

    private void renderLabels(GuiGraphics graphics) {
        for (Slot slot : simpleSlots()) {
            boolean live = isLive(slot.field());

            // Dimmed alongside its disabled control, so the row reads as one
            // thing that is off rather than a live label with a dead box.
            graphics.drawString(font, slot.field().label(), slot.labelLeft(), slot.top() + 4,
                    live ? UiTheme.TEXT_MUTED : UiTheme.LABEL_OFF, false);

            if (!live) {
                renderInertField(graphics, slot, 15, "");
            }
        }

        for (Slot slot : listSlots()) {
            boolean live = isLive(slot.field());

            graphics.drawString(font, slot.field().label(), slot.labelLeft(), slot.top() - 10,
                    live ? UiTheme.TEXT_MUTED : UiTheme.LABEL_OFF, false);

            if (!live) {
                renderInertField(graphics, slot, LIST_HEIGHT, "Not used by this type");
            }
        }
    }

    /**
     * Stands in for a field the chosen type ignores.
     */
    private void renderInertField(GuiGraphics graphics, Slot slot, int height, String note) {
        int left = slot.controlLeft();
        int top = slot.top();
        int width = slot.controlWidth();

        graphics.fill(left, top, left + width, top + height, UiTheme.FIELD_OFF_BG);
        graphics.renderOutline(left, top, width, height, UiTheme.FIELD_OFF_BORDER);

        if (!note.isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(note, width - 8),
                    left + 4, top + 4, UiTheme.LABEL_OFF, false);
        }
    }
}
