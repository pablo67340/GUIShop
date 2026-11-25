package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.menu.SellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The sell container, drawn to match the rest of the mod rather than as a
 * chest.
 * <p>
 * A container screen rather than one of the mod's own panels, because this is
 * the one screen where items are actually picked up and put down, and that
 * wants real slots.
 */
public final class SellScreen extends AbstractContainerScreen<SellMenu> {

    private static final int SLOT = 18;

    /** Matches the slot geometry the menu lays out. */
    private static final int SLOTS_LEFT = 8;
    private static final int SLOTS_TOP = 18;

    /** Where the player's own inventory starts in a four-row container. */
    private static final int PLAYER_TOP = 103;

    /** And its hotbar. */
    private static final int HOTBAR_TOP = 161;

    /** Longest first; the first one that fits the panel is used. */
    private static final List<String> HINTS = List.of(
            "Close to sell. Anything unsellable comes back.",
            "Close to sell - the rest comes back.",
            "Close to sell");

    public SellScreen(SellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();

        // Here rather than in the constructor, where the font has not been
        // handed over yet. Centred in the gap between the two grids, derived
        // rather than guessed so it stays put if the container changes height.
        int gridBottom = SLOTS_TOP + SellMenu.ROWS * SLOT;
        int playerTop = PLAYER_TOP + (SellMenu.ROWS - 4) * SLOT;
        this.inventoryLabelY = gridBottom + (playerTop - gridBottom - font.lineHeight) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, UiTheme.PANEL_BG);
        graphics.renderOutline(left, top, imageWidth, imageHeight, UiTheme.PANEL_BORDER);

        graphics.fill(left, top, left + imageWidth, top + SLOTS_TOP - 4, UiTheme.HEADER_BG);
        graphics.fill(left, top + SLOTS_TOP - 5, left + imageWidth, top + SLOTS_TOP - 4,
                UiTheme.HEADER_BORDER);

        drawSlots(graphics, left + SLOTS_LEFT, top + SLOTS_TOP, SellMenu.ROWS);
        drawSlots(graphics, left + SLOTS_LEFT, top + PLAYER_TOP + (SellMenu.ROWS - 4) * SLOT, 3);
        drawSlots(graphics, left + SLOTS_LEFT, top + HOTBAR_TOP + (SellMenu.ROWS - 4) * SLOT, 1);
    }

    private void drawSlots(GuiGraphics graphics, int left, int top, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < SellMenu.COLUMNS; column++) {
                int x = left + column * SLOT;
                int y = top + row * SLOT;
                graphics.fill(x, y, x + 16, y + 16, UiTheme.CELL_BG);
                graphics.renderOutline(x - 1, y - 1, 18, 18, UiTheme.PANEL_BORDER);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Lifted the same way the shop header lifts a dark title, since the
        // configured title is dark grey and the header behind it is darker.
        graphics.drawString(font, Contrast.readable(title), SLOTS_LEFT, 6,
                UiTheme.HEADER_TEXT, false);

        // Said here rather than in chat, where it would arrive after the fact.
        graphics.drawString(font, hint(), SLOTS_LEFT, inventoryLabelY,
                UiTheme.TEXT_MUTED, false);
    }

    /**
     * The fullest wording that fits the panel.
     * <p>
     * The panel is as wide as nine slots and no wider, so rather than pick a
     * phrase and hope, the longest one that actually fits is measured. Holds up
     * under a resource pack with a wider font.
     */
    private String hint() {
        int available = imageWidth - SLOTS_LEFT * 2;

        for (String candidate : HINTS) {
            if (font.width(candidate) <= available) {
                return candidate;
            }
        }
        return HINTS.getLast();
    }
}
