package com.pablo67340.guishop.listenable;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.AltSellPane;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.util.ConfigUtil;

public class AltSell {

    private final Item subjectItem;
    private final Gui gui;

    private AltSellPane pane;

    private final Item indicatorItem;
    private final Item addItem;
    private final Item removeItem;
    private final Item confirmItem;
    private final Item cancelItem;

    public AltSell(Item subjectItem) {
        this.subjectItem = subjectItem;
        gui = new Gui(Main.getINSTANCE(), 6, ChatColor.translateAlternateColorCodes('&', ConfigUtil.getAltSellTitle()));
        indicatorItem = new Item();
        indicatorItem.setMaterial(ConfigUtil.getAltSellIndicatorMaterial());
        addItem = new Item();
        addItem.setMaterial(ConfigUtil.getAltSellAddMaterial());
        removeItem = new Item();
        removeItem.setMaterial(ConfigUtil.getAltSellRemoveMaterial());
        confirmItem = new Item();
        confirmItem.setMaterial(ConfigUtil.getAltSellConfirmMaterial());
        cancelItem = new Item();
        cancelItem.setMaterial(ConfigUtil.getAltSellCancelMaterial());
    }

    private GuiItem setQuantityAndGet(ItemStack item, int quantity) {
        item.setAmount(quantity);
        return new GuiItem(item);
    }

    public void open(Player player) {
        if (!player.hasPermission("guishop.sell")) {
            player.sendMessage(ConfigUtil.getNoPermission());
            return;
        }
        GuiItem gItem = subjectItem.parseMaterial();
        GuiItem gIndicator = indicatorItem.parseMaterial();
        GuiItem gAddItem = addItem.parseMaterial();
        GuiItem gRemoveItem = removeItem.parseMaterial();
        GuiItem gConfirmItem = confirmItem.parseMaterial();
        GuiItem gCancelItem = cancelItem.parseMaterial();
        if (gItem != null && gIndicator != null && gAddItem != null && gRemoveItem != null && gConfirmItem != null
                && gCancelItem != null) {
            GuiItem[] addRemoveItems = new GuiItem[6];
            ItemStack addItemstack = gAddItem.getItem();
            addRemoveItems[0] = setQuantityAndGet(addItemstack.clone(), ConfigUtil.getAltSellQuantity1());
            addRemoveItems[1] = setQuantityAndGet(addItemstack.clone(), ConfigUtil.getAltSellQuantity2());
            addRemoveItems[2] = setQuantityAndGet(addItemstack.clone(), ConfigUtil.getAltSellQuantity3());
            ItemStack removeItemstack = gRemoveItem.getItem();
            addRemoveItems[3] = setQuantityAndGet(removeItemstack.clone(), ConfigUtil.getAltSellQuantity1());
            addRemoveItems[4] = setQuantityAndGet(removeItemstack.clone(), ConfigUtil.getAltSellQuantity2());
            addRemoveItems[5] = setQuantityAndGet(removeItemstack.clone(), ConfigUtil.getAltSellQuantity3());
            pane = new AltSellPane(gItem, addRemoveItems, gIndicator,
                    Item.renameGuiItem(gConfirmItem, ConfigUtil.getAltSellConfirmName()), Item.renameGuiItem(gCancelItem, ConfigUtil.getAltSellCancelName()));
            pane.setSubjectQuantity(1);
            pane.setIndicatorName(subjectItem.getSellLore(1));
            gui.addPane(pane);
            gui.setOnTopClick(this::onClick);
            gui.setOnBottomClick(event -> event.setCancelled(true));
            gui.show(player);
        } else {
            Main.log("One or more of the materials you defined in the alt sell GUI are not valid.");
        }
    }

    private void changeQuantity(int delta) {
        int previous = pane.getSubjectQuantity();
        int update = previous + delta;
        if (update < 1) {
            update = 1;
        }
        update = pane.setSubjectQuantity(update);
        if (update != previous) {
            pane.setIndicatorName(subjectItem.getSellLore(update));
            gui.update();
        }
    }

    private void sell(Player player, ItemStack itemStack) {
        // remove IF's IF-uuid NBT tag
        NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
        comp.remove("IF-uuid");
        itemStack = ItemNBTUtil.setNBTTag(comp, itemStack);

        Main.debugLog(itemStack.toString());

        int amount = itemStack.getAmount();
        Map<Integer, ItemStack> result = player.getInventory().removeItem(itemStack);
        if (result.isEmpty()) {

            Sell.roundAndGiveMoney(player, subjectItem.calculateSellPrice(amount));
            // buy price must be defined for dynamic pricing to work
            if (subjectItem.hasBuyPrice() && ConfigUtil.isDynamicPricing()) {
                Main.getDYNAMICPRICING().sellItem(subjectItem.getItemString(), amount);
            }
        } else {
            ItemStack addBack = result.get(0).clone();
            addBack.setAmount(amount - addBack.getAmount());
            if (addBack.getAmount() > 0) {
                player.getInventory().addItem(addBack);
            }
            player.sendMessage(ConfigUtil.getAltSellNotEnough().replace("{amount}", Integer.toString(amount)));
        }
    }

    private void onClick(InventoryClickEvent evt) {
        evt.setCancelled(true);
        switch (evt.getSlot()) {
            case 18:
            case 19:
            case 20:
                changeQuantity(evt.getCurrentItem().getAmount());
                break;
            case 24:
            case 25:
            case 26:
                changeQuantity(-evt.getCurrentItem().getAmount());
                break;
            case 48:
                sell((Player) evt.getWhoClicked(), evt.getInventory().getItem(13));
                break;
            case 50:
                evt.getWhoClicked().closeInventory();
                break;
            default:
                break;
        }
    }

}
