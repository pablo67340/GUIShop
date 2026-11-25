package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.ShopEntryData;
import com.pablo67340.guishop.neoforge.network.TradeRequestPayload;
import com.pablo67340.guishop.util.MathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Buying and selling one item.
 * <p>
 * The prices shown are the server's own, taken from the catalogue, and the
 * totals here are only a preview: the server recalculates everything when the
 * request arrives, so a stale or tampered preview cannot turn into a cheap
 * purchase.
 */
final class TradeScreen extends ShopChildScreen {

    /** The usual amounts, so most trades need no typing. */
    private static final int[] PRESETS = {1, 8, 16, 32, 64};

    private static final int MAX_QUANTITY = 36 * 64;

    /** The icon is drawn at triple size, so 48 pixels plus breathing room. */
    private static final int PREVIEW_HEIGHT = 58;

    private final String shopName;
    private final int index;
    private final ShopEntryData entry;

    private int quantity = 1;
    private EditBox quantityBox;

    /** Set while the box is being filled in, so the responder does not loop. */
    private boolean syncingQuantity;

    TradeScreen(ShopBrowserScreen parent, String shopName, int index, ShopEntryData entry) {
        super(entry.icon().getHoverName(), parent);
        this.shopName = shopName;
        this.index = index;
        this.entry = entry;
    }

    @Override
    protected void init() {
        layoutPanel(300, UiTheme.HEADER_HEIGHT + PREVIEW_HEIGHT + 42 + 46 + UiTheme.FOOTER_HEIGHT);

        int row = presetsTop();

        int presetWidth = (contentWidth() - (PRESETS.length - 1) * 4) / PRESETS.length;
        for (int slot = 0; slot < PRESETS.length; slot++) {
            int amount = PRESETS[slot];
            addRenderableWidget(Button.builder(
                            Component.literal(String.valueOf(amount)),
                            button -> setQuantity(amount))
                    .bounds(contentLeft() + slot * (presetWidth + 4), row, presetWidth, 16)
                    .build());
        }

        quantityBox = new EditBox(font, contentLeft(), row + 22, 60, 16,
                Component.literal("Quantity"));
        quantityBox.setValue(String.valueOf(quantity));
        quantityBox.setMaxLength(5);
        quantityBox.setResponder(this::onQuantityTyped);
        addRenderableWidget(quantityBox);

        int actionWidth = (contentWidth() - 68 - 4) / 2;
        int actionLeft = contentLeft() + 68;

        Button buy = Button.builder(Component.literal("Buy"), button -> send(false))
                .bounds(actionLeft, row + 22, actionWidth, 16)
                .build();
        buy.active = entry.canBuy();
        addRenderableWidget(buy);

        Button sell = Button.builder(Component.literal("Sell"), button -> send(true))
                .bounds(actionLeft + actionWidth + 4, row + 22, actionWidth, 16)
                .build();
        sell.active = entry.canSell();
        addRenderableWidget(sell);

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(panelRight() - UiTheme.PADDING - 50, footerTop() + 4, 50, 16)
                .build());
    }

    private int previewTop() {
        return headerBottom() + 8;
    }

    /**
     * Below the preview rather than a fixed distance from the header, so the
     * large icon cannot reach down over the buttons.
     */
    private int presetsTop() {
        return previewTop() + PREVIEW_HEIGHT;
    }

    // ------------------------------------------------------------- quantity

    private void setQuantity(int amount) {
        quantity = Math.max(1, Math.min(MAX_QUANTITY, amount));

        syncingQuantity = true;
        quantityBox.setValue(String.valueOf(quantity));
        syncingQuantity = false;
    }

    private void onQuantityTyped(String typed) {
        if (syncingQuantity) {
            return;
        }
        if (typed.isBlank()) {
            // Treated as one rather than rejected, so clearing the box to type
            // a new number does not flash an error.
            quantity = 1;
            return;
        }
        try {
            quantity = Math.max(1, Math.min(MAX_QUANTITY, Integer.parseInt(typed.trim())));
        } catch (NumberFormatException exception) {
            setStatus("'" + typed + "' is not a number.", false);
        }
    }

    private void send(boolean selling) {
        PacketDistributor.sendToServer(new TradeRequestPayload(shopName, index, quantity, selling));
    }

    // --------------------------------------------------------------- render

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderPanel(graphics, getTitle(), balanceText());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int top = previewTop();

        // Drawn at triple size, which is the "bigger preview" the small grid
        // cell cannot give.
        renderLargeIcon(graphics, contentLeft() + 2, top + 2);

        int textLeft = contentLeft() + 60;
        graphics.drawString(font, Contrast.readable(entry.icon().getHoverName()),
                textLeft, top, UiTheme.TEXT, false);

        int line = top + 12;
        for (Component priceLine : entry.priceLines()) {
            graphics.drawString(font, Contrast.readable(priceLine), textLeft, line, UiTheme.TEXT, false);
            line += 10;
        }

        renderTotals(graphics, contentLeft(), presetsTop() + 42);

        renderFooterDivider(graphics);
        renderStatus(graphics);

        if (isOverIcon(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, tooltip(), mouseX, mouseY, entry.icon());
        }
    }

    /**
     * Draws the item three times its normal size.
     * <p>
     * Scaling the whole pose rather than the item means the model, enchantment
     * glint and overlays all come along, which drawing a larger sprite would
     * not.
     */
    private void renderLargeIcon(GuiGraphics graphics, int left, int top) {
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.renderFakeItem(entry.icon(), 0, 0);
        graphics.pose().popPose();
    }

    private boolean isOverIcon(int mouseX, int mouseY) {
        int left = contentLeft() + 2;
        int top = previewTop() + 2;
        return mouseX >= left && mouseX < left + 48 && mouseY >= top && mouseY < top + 48;
    }

    private List<Component> tooltip() {
        ItemStack stack = entry.icon();
        List<Component> lines = new ArrayList<>(stack.getTooltipLines(
                Item.TooltipContext.of(minecraft.level),
                minecraft.player,
                minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
        return lines;
    }

    /**
     * The running totals for the chosen quantity.
     * <p>
     * Unit prices are multiplied here only to show the player what to expect.
     * With dynamic pricing the server's figure can differ, which is why the
     * total is labelled as an estimate rather than a promise.
     */
    private void renderTotals(GuiGraphics graphics, int left, int top) {
        graphics.drawString(font, "Quantity " + quantity, left, top, UiTheme.TEXT_MUTED, false);

        int line = top + 12;
        if (entry.canBuy()) {
            graphics.drawString(font,
                    "Buy " + quantity + " for " + money(entry.buyPrice() * quantity),
                    left, line, UiTheme.BUY, false);
            line += 10;
        }
        if (entry.canSell()) {
            graphics.drawString(font,
                    "Sell " + quantity + " for " + money(entry.sellPrice() * quantity),
                    left, line, UiTheme.SELL, false);
        }
    }

    private String balanceText() {
        return "Balance " + money(parent().balance());
    }

    private static String money(double amount) {
        return MathUtil.formatWithCommas(amount);
    }
}
