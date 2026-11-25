package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A screen opened on top of the shop browser: the trade panel, the item editor
 * or the settings.
 * <p>
 * Each knows the browser it came from, so closing returns there instead of to
 * the game, and so a catalogue update can be applied to the screen underneath
 * without disturbing the one in front.
 */
abstract class ShopChildScreen extends PanelScreen {

    private final ShopBrowserScreen parent;

    /** The last thing the server said, shown in the footer. */
    private Component status = Component.empty();
    private boolean statusSuccess = true;

    protected ShopChildScreen(Component title, ShopBrowserScreen parent) {
        super(title);
        this.parent = parent;
    }

    ShopBrowserScreen parent() {
        return parent;
    }

    /**
     * Called when the server reports the outcome of something this screen did.
     * The balance is taken from the same message, so the screen can never show
     * a balance the server disagrees with.
     */
    void onResult(TradeResultPayload result) {
        status = result.message();
        statusSuccess = result.success();
        parent.setBalance(result.balance());
        onServerResult(result);
    }

    /**
     * For subclasses that need to react beyond showing the message, such as
     * closing once a save has gone through.
     */
    protected void onServerResult(TradeResultPayload result) {
    }

    protected boolean hasStatus() {
        return !status.getString().isEmpty();
    }

    /**
     * Where footer text may start without landing on a footer button.
     * <p>
     * Overridden by screens that put buttons in the bottom-left, since the
     * status line would otherwise be drawn straight over them.
     */
    protected int footerTextLeft() {
        return contentLeft();
    }

    protected void renderStatus(GuiGraphics graphics) {
        if (!hasStatus()) {
            return;
        }
        int left = footerTextLeft();
        graphics.drawString(font, trimToWidth(status, panelRight() - UiTheme.PADDING - left),
                left, footerTop() + 9, statusSuccess ? UiTheme.BUY : UiTheme.WARN, false);
    }

    protected void setStatus(String message, boolean success) {
        status = Component.literal(message);
        statusSuccess = success;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
