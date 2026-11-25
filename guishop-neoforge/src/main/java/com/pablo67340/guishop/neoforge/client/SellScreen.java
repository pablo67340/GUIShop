package com.pablo67340.guishop.neoforge.client;

import com.pablo67340.guishop.neoforge.menu.SellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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

    public SellScreen(SellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        // Where the player inventory's own label would go; drawn manually.
        this.inventoryLabelY = this.imageHeight - 94;
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
        drawSlots(graphics, left + SLOTS_LEFT, top + 103 + (SellMenu.ROWS - 4) * SLOT, 3);
        drawSlots(graphics, left + SLOTS_LEFT, top + 161 + (SellMenu.ROWS - 4) * SLOT, 1);
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
        graphics.drawString(font, title, SLOTS_LEFT, 6, UiTheme.HEADER_TEXT, false);

        // Said here rather than in chat, where it would arrive after the fact.
        graphics.drawString(font, "Close to sell. Anything unsellable comes back.",
                SLOTS_LEFT, inventoryLabelY, UiTheme.TEXT_MUTED, false);
    }
}
