package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.gui.SimpleGui;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.NoSuchElementException;
import org.bukkit.event.inventory.ClickType;

public class AltSell {

    private final Item subjectItem;
    private final SimpleGui gui;

    private final Item indicatorItem;
    private final Item addItem;
    private final Item removeItem;
    private final Item confirmItem;
    private final Item cancelItem;

    private boolean hasClicked = false;

    private int subjectQuantity = 1;

    // Slots for the alt sell GUI
    private static final int SUBJECT_SLOT = 13;
    private static final int INDICATOR_SLOT = 22;
    private static final int[] ADD_SLOTS = {18, 19, 20};
    private static final int[] REMOVE_SLOTS = {24, 25, 26};
    private static final int CONFIRM_SLOT = 48;
    private static final int CANCEL_SLOT = 50;

    @Getter
    private final Shop shop;

    public AltSell(Item subjectItem, Shop shop) {
        this.subjectItem = subjectItem;
        gui = new SimpleGui(6, ChatColor.translateAlternateColorCodes('&', Config.getAltSellConfig().getTitle()));
        indicatorItem = new Item();
        indicatorItem.setMaterial(Config.getAltSellConfig().getIndicatorMaterial());
        addItem = new Item();
        addItem.setMaterial(Config.getAltSellConfig().getAddMaterial());
        removeItem = new Item();
        removeItem.setMaterial(Config.getAltSellConfig().getRemoveMaterial());
        confirmItem = new Item();
        confirmItem.setMaterial(Config.getAltSellConfig().getConfirmMaterial());
        cancelItem = new Item();
        cancelItem.setMaterial(Config.getAltSellConfig().getCancelMaterial());

        this.shop = shop;
    }

    private ItemStack setQuantityAndGet(ItemStack item, int quantity, boolean isDecrease) {
        ItemStack clone = item.clone();
        clone.setAmount(quantity);

        ItemMeta im = clone.getItemMeta();

        if (isDecrease) {
            im.setDisplayName(Config.getAltSellConfig().getDecreaseTitle().replace("%amount%", Integer.toString(quantity)));
        } else {
            im.setDisplayName(Config.getAltSellConfig().getIncreaseTitle().replace("%amount%", Integer.toString(quantity)));
        }

        clone.setItemMeta(im);

        return clone;
    }

    public void open(Player player) {
        if (!GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.sell")) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-permission");
            return;
        }

        ItemStack subjectStack;
        ItemStack indicatorStack;
        ItemStack addStack;
        ItemStack removeStack;
        ItemStack confirmStack;
        ItemStack cancelStack;

        try {
            subjectStack = XMaterial.matchXMaterial(subjectItem.getMaterial()).get().parseItem();
            indicatorStack = XMaterial.matchXMaterial(indicatorItem.getMaterial()).get().parseItem();
            addStack = XMaterial.matchXMaterial(addItem.getMaterial()).get().parseItem();
            removeStack = XMaterial.matchXMaterial(removeItem.getMaterial()).get().parseItem();
            confirmStack = XMaterial.matchXMaterial(confirmItem.getMaterial()).get().parseItem();
            cancelStack = XMaterial.matchXMaterial(cancelItem.getMaterial()).get().parseItem();
        } catch (NoSuchElementException | NullPointerException exception) {
            GUIShop.getINSTANCE().getLogUtil().log("One or more of the materials you defined in the alt sell GUI are not valid.");
            return;
        }

        if (subjectStack != null && indicatorStack != null && addStack != null && removeStack != null && confirmStack != null && cancelStack != null) {
            // Set up subject item
            subjectStack.setAmount(subjectQuantity);
            gui.setItem(SUBJECT_SLOT, subjectStack);

            // Set up indicator
            ItemMeta indicatorMeta = indicatorStack.getItemMeta();
            indicatorMeta.setDisplayName(subjectItem.getSellLore(subjectQuantity));
            indicatorStack.setItemMeta(indicatorMeta);
            gui.setItem(INDICATOR_SLOT, indicatorStack);

            // Set up add buttons
            int[] quantities = {Config.getAltSellConfig().getQuantity1(), Config.getAltSellConfig().getQuantity2(), Config.getAltSellConfig().getQuantity3()};
            for (int i = 0; i < ADD_SLOTS.length; i++) {
                gui.setItem(ADD_SLOTS[i], setQuantityAndGet(addStack, quantities[i], false));
            }

            // Set up remove buttons
            for (int i = 0; i < REMOVE_SLOTS.length; i++) {
                gui.setItem(REMOVE_SLOTS[i], setQuantityAndGet(removeStack, quantities[i], true));
            }

            // Set up confirm button
            ItemMeta confirmMeta = confirmStack.getItemMeta();
            confirmMeta.setDisplayName(Config.getAltSellConfig().getConfirmName());
            confirmStack.setItemMeta(confirmMeta);
            gui.setItem(CONFIRM_SLOT, confirmStack);

            // Set up cancel button
            ItemMeta cancelMeta = cancelStack.getItemMeta();
            cancelMeta.setDisplayName(Config.getAltSellConfig().getCancelName());
            cancelStack.setItemMeta(cancelMeta);
            gui.setItem(CANCEL_SLOT, cancelStack);

            gui.setTopClickHandler(this::onClick);
            gui.setBottomClickHandler(event -> event.setCancelled(true));
            gui.setCloseHandler(this::onClose);
            gui.show(player);
        } else {
            GUIShop.getINSTANCE().getLogUtil().log("One or more of the materials you defined in the alt sell GUI are not valid.");
        }
    }

    private void changeQuantity(int delta) {
        hasClicked = true;
        int previous = subjectQuantity;
        int update = previous + delta;
        if (update < 1) {
            update = 1;
        }
        // Limit to max stack size
        int maxStack = 64;
        try {
            maxStack = XMaterial.matchXMaterial(subjectItem.getMaterial()).get().parseMaterial().getMaxStackSize();
        } catch (Exception ignored) {}
        if (update > maxStack) {
            update = maxStack;
        }
        subjectQuantity = update;

        if (update != previous) {
            // Update the subject item amount
            ItemStack subjectStack = gui.getItem(SUBJECT_SLOT);
            if (subjectStack != null) {
                subjectStack.setAmount(subjectQuantity);
            }

            // Update the indicator
            ItemStack indicatorStack = gui.getItem(INDICATOR_SLOT);
            if (indicatorStack != null) {
                ItemMeta indicatorMeta = indicatorStack.getItemMeta();
                indicatorMeta.setDisplayName(subjectItem.getSellLore(subjectQuantity));
                indicatorStack.setItemMeta(indicatorMeta);
            }

            gui.update();
        }
    }

    private void sell(Player player, ItemStack itemStack) {
        // Note: We need to use isItemFromItemStack() for matching, not removeItem(),
        // because items may have enchantments, potions, NBT, etc. that need smart matching.

        GUIShop.getINSTANCE().getLogUtil().debugLog("AltSell: Attempting to sell " + itemStack.toString());

        int amountToSell = itemStack.getAmount();
        int amountRemoved = 0;
        
        // Iterate through player inventory and find matching items using the same logic as Sell.java
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && amountRemoved < amountToSell; i++) {
            ItemStack invItem = contents[i];
            if (invItem == null || invItem.getType().isAir()) {
                continue;
            }
            
            // Use isItemFromItemStack for smart matching (handles enchantments, potions, NBT, etc.)
            if (subjectItem.isItemFromItemStack(invItem)) {
                int canTake = Math.min(invItem.getAmount(), amountToSell - amountRemoved);
                amountRemoved += canTake;
                
                if (canTake >= invItem.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    invItem.setAmount(invItem.getAmount() - canTake);
                }
            }
        }
        
        if (amountRemoved >= amountToSell) {
            Sell.roundAndGiveMoney(player, subjectItem.calculateSellPrice(amountToSell));
            // buy price must be defined for dynamic pricing to work
            if (subjectItem.hasBuyPrice() && Config.isDynamicPricing()) {
                GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().sellItem(subjectItem.getItemString(), amountToSell);
            }

            GUIShop.getINSTANCE().getLogUtil().transactionLog(
                    "Player " + player.getName() + " sold " + amountToSell + " items (1 different) for " + subjectItem.calculateSellPrice(amountToSell) + ". Item: \n" + itemStack.getType());
        } else if (amountRemoved > 0) {
            // Partial sell - sold what we could
            Sell.roundAndGiveMoney(player, subjectItem.calculateSellPrice(amountRemoved));
            if (subjectItem.hasBuyPrice() && Config.isDynamicPricing()) {
                GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().sellItem(subjectItem.getItemString(), amountRemoved);
            }
            GUIShop.getINSTANCE().getLogUtil().transactionLog(
                    "Player " + player.getName() + " sold " + amountRemoved + " items (partial, wanted " + amountToSell + ") for " + subjectItem.calculateSellPrice(amountRemoved) + ". Item: \n" + itemStack.getType());
        } else {
            // Couldn't find any matching items
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "alt-sell-not-enough", amountToSell);
        }
    }

    private void onClose(InventoryCloseEvent event) {
        if (!hasClicked && !GUIShop.getINSTANCE().isReload) {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> {
                if (this.shop != null) {
                    shop.open((Player) event.getPlayer());
                } else {
                    PlayerListener.INSTANCE.openMenu((Player) event.getPlayer());
                }
            }, 1L);
        }
    }

    private void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        // Block off-hand swap
        if (event.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            return;
        }

        int slot = event.getSlot();

        // Check add slots
        for (int i = 0; i < ADD_SLOTS.length; i++) {
            if (slot == ADD_SLOTS[i]) {
                ItemStack item = event.getCurrentItem();
                if (item != null) {
                    changeQuantity(item.getAmount());
                }
                return;
            }
        }

        // Check remove slots
        for (int i = 0; i < REMOVE_SLOTS.length; i++) {
            if (slot == REMOVE_SLOTS[i]) {
                ItemStack item = event.getCurrentItem();
                if (item != null) {
                    changeQuantity(-item.getAmount());
                }
                return;
            }
        }

        // Check confirm
        if (slot == CONFIRM_SLOT) {
            sell((Player) event.getWhoClicked(), gui.getItem(SUBJECT_SLOT));
            return;
        }

        // Check cancel
        if (slot == CANCEL_SLOT) {
            hasClicked = true;
            if (GUIShop.getINSTANCE().isReload) {
                event.getWhoClicked().closeInventory();
                return;
            }

            // Open directly - openInventory() will close current inventory
            if (this.shop != null) {
                shop.open((Player) event.getWhoClicked());
            } else {
                PlayerListener.INSTANCE.openMenu((Player) event.getWhoClicked());
            }
        }
    }
}
