package com.pablo67340.guishop.neoforge.menu;

import com.pablo67340.guishop.neoforge.server.SellService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The container behind {@code /sell}: six empty rows to tip items into.
 * <p>
 * The container is created per menu and belongs to nobody, so it cannot be a
 * way to store things - it is emptied when the screen closes, with the money
 * paid for whatever the shops buy and the rest handed back.
 * <p>
 * Geometry matches a six-row chest so the slots line up where a player's hands
 * already expect them.
 */
public final class SellMenu extends AbstractContainerMenu {

    public static final int ROWS = 6;
    public static final int COLUMNS = 9;
    public static final int SIZE = ROWS * COLUMNS;

    private final Container container;

    /**
     * Set once the contents have been dealt with, because
     * {@link #removed(Player)} can be called more than once for one screen and
     * paying twice for one chestful would be a generous bug.
     */
    private boolean settled;

    public SellMenu(int containerId, Inventory playerInventory) {
        super(GuiShopMenus.SELL.get(), containerId);
        this.container = new SimpleContainer(SIZE);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(container, column + row * COLUMNS,
                        8 + column * 18, 18 + row * 18));
            }
        }

        int offset = (ROWS - 4) * 18;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new Slot(playerInventory, column + row * COLUMNS + 9,
                        8 + column * 18, 103 + row * 18 + offset));
            }
        }
        for (int column = 0; column < COLUMNS; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 161 + offset));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack inSlot = slot.getItem();
        ItemStack copy = inSlot.copy();

        if (index < SIZE) {
            if (!moveItemStackTo(inSlot, SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /**
     * Sells the contents as the screen closes.
     * <p>
     * Doing it here rather than on a button means the obvious action - put
     * things in, walk away - is the one that works, which is how the Bukkit
     * plugin behaved.
     */
    @Override
    public void removed(Player player) {
        // First, so the stack on the cursor is back in the player's inventory
        // before anything is counted.
        super.removed(player);

        if (settled || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        settled = true;

        SellService.sellContents(serverPlayer, container);
    }
}
