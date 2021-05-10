package com.pablo67340.guishop.listenable;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.ConfigUtil;
import java.math.BigDecimal;
import java.util.List;

public final class Sell {

    private Gui GUI;

    /**
     * Open the {@link Sell} GUI.
     *
     * @param player - The player the GUI will display to
     */
    public void open(Player player) {
        GUI = new Gui(Main.getINSTANCE(), 6, ConfigUtil.getSellTitle());
        GUI.setOnClose(this::onSellClose);
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        GUI.addPane(pane);
        GUI.show(player);
    }

    /**
     * Sell items inside the {@link Sell} GUI.
     *
     * @param player - The player selling items
     */
    public void sell(Player player) {
        sellItems(player, GUI.getInventory().getContents());
        GUI.getInventory().clear();
    }

    /**
     * Sells the specified items on the behalf of a player
     *
     * @param player the player
     * @param items the items
     */
    public static void sellItems(Player player, ItemStack[] items) {
        BigDecimal moneyToGive = BigDecimal.valueOf(0);
        boolean couldntSell = false;
        int countSell = 0;
        for (ItemStack item : items) {

            if (item == null) {
                continue;
            }

            Item shopItem = null;

            Main.debugLog("Checking if " + item.getType().toString() + " is sellable");

            List<Item> itemList = Main.getINSTANCE().getITEMTABLE().get(item.getType().toString());

            if (itemList != null) {
                for (Item itm : itemList) {
                    if (itm.isItemFromItemStack(item)) {
                        shopItem = itm;
                    }
                }
            }

            if (shopItem == null || !shopItem.hasSellPrice() || !player.hasPermission("guishop.shop." + shopItem.getShop())) {
                countSell += 1;
                couldntSell = true;
                player.getInventory().addItem(item);
                continue;
            }

            int quantity = item.getAmount();

            // buy price must be defined for dynamic pricing to work
            if (ConfigUtil.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()) {
                moneyToGive = moneyToGive.add(Main.getDYNAMICPRICING().calculateSellPrice(item.getType().toString(), quantity,
                        shopItem.getBuyPriceAsDecimal(), shopItem.getSellPriceAsDecimal()));
                Main.getDYNAMICPRICING().sellItem(item.getType().toString(), quantity);
            } else {
                moneyToGive = moneyToGive.add(shopItem.getSellPriceAsDecimal().multiply(BigDecimal.valueOf(quantity)));
            }

        }

        if (couldntSell) {
            player.sendMessage(ConfigUtil.getPrefix() + " " + ConfigUtil.getCantSell().replace("{count}", countSell + ""));
        }
        roundAndGiveMoney(player, moneyToGive);
    }

    /**
     * Rounds the amount and deposits it on behalf of the player.
     *
     * @param player the player
     * @param moneyToGive the amount to give
     */
    public static void roundAndGiveMoney(Player player, BigDecimal moneyToGive) {

        if (moneyToGive.compareTo(BigDecimal.ZERO) > 0) {
            Main.getECONOMY().depositPlayer(player, moneyToGive.doubleValue());

            player.sendMessage(ConfigUtil.getSold() + moneyToGive.toPlainString() + ConfigUtil.getAdded());
        }
    }

    private void onSellClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        sell(player);
    }

}
