package com.pablo67340.guishop.neoforge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The panel chrome every shop screen sits in: a centred rounded-off box with a
 * header and a footer.
 * <p>
 * Shared so the browser, the trade panel, the item editor and the settings all
 * agree on where their edges are, rather than each recomputing a layout that
 * has to look identical.
 */
abstract class PanelScreen extends Screen {

    protected int panelLeft;
    protected int panelTop;
    protected int panelWidth;
    protected int panelHeight;

    protected PanelScreen(Component title) {
        super(title);
    }

    /**
     * Centres a panel of at most the given size, shrinking it to fit a small
     * window rather than letting it run off the edges.
     */
    protected void layoutPanel(int maxWidth, int maxHeight) {
        panelWidth = Math.min(width - UiTheme.PANEL_MARGIN * 2, maxWidth);
        panelHeight = Math.min(height - UiTheme.PANEL_MARGIN * 2, maxHeight);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
    }

    protected int panelRight() {
        return panelLeft + panelWidth;
    }

    protected int panelBottom() {
        return panelTop + panelHeight;
    }

    protected int contentLeft() {
        return panelLeft + UiTheme.PADDING;
    }

    protected int contentWidth() {
        return panelWidth - UiTheme.PADDING * 2;
    }

    protected int headerBottom() {
        return panelTop + UiTheme.HEADER_HEIGHT;
    }

    protected int footerTop() {
        return panelBottom() - UiTheme.FOOTER_HEIGHT;
    }

    /**
     * Draws the panel and its header.
     *
     * @param heading   the title, drawn with configured colours lifted to stay
     *                  readable on the dark header
     * @param rightText a short status drawn right-aligned, or null for none
     */
    protected void renderPanel(GuiGraphics graphics, Component heading, String rightText) {
        graphics.fill(panelLeft - 1, panelTop - 1, panelRight() + 1, panelBottom() + 1, UiTheme.PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelRight(), panelBottom(), UiTheme.PANEL_BG);

        graphics.fill(panelLeft, panelTop, panelRight(), headerBottom(), UiTheme.HEADER_BG);
        graphics.fill(panelLeft, headerBottom() - 1, panelRight(), headerBottom(), UiTheme.HEADER_BORDER);

        graphics.drawString(font,
                Contrast.readable(heading, UiTheme.HEADER_MIN_LUMINANCE, UiTheme.HEADER_TARGET_LUMINANCE),
                contentLeft(), panelTop + 12, UiTheme.HEADER_TEXT, false);

        if (rightText != null && !rightText.isEmpty()) {
            graphics.drawString(font, rightText,
                    panelRight() - UiTheme.PADDING - font.width(rightText),
                    panelTop + 13, UiTheme.HEADER_MUTED, false);
        }
    }

    protected void renderFooterDivider(GuiGraphics graphics) {
        graphics.fill(panelLeft, footerTop(), panelRight(), footerTop() + 1, UiTheme.DIVIDER);
    }

    /**
     * Theme colours carry an alpha byte for the fill calls; text colours must
     * not, so it is masked off here.
     */
    protected static Component coloured(String text, int argb) {
        return Component.literal(text).withStyle(style -> style.withColor(argb & 0xFFFFFF));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
