package com.pablo67340.guishop.listenable;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Sell {

    private Gui GUI;

    /**
     * Open the {@link Sell} GUI.
     *
     * @param player The player the GUI will display to
     */
    public void open(Player player) {
        GUI = new Gui(GUIShop.getINSTANCE(), 6, Config.getTitlesConfig().getSellTitle());
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

        ConcurrentHashMap<Material, Integer> itemMap = new ConcurrentHashMap<>();

        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }

            Item shopItem = null;

            GUIShop.debugLog("Checking if " + item.getType() + " is sellable");

            List<Item> itemList = GUIShop.getINSTANCE().getITEMTABLE().get(item.getType().toString());

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

            if (itemMap.containsKey(item.getType())) {
                int oldAmount = itemMap.get(item.getType());

                itemMap.put(item.getType(), oldAmount + item.getAmount());
            } else {
                itemMap.put(item.getType(), item.getAmount());
            }

            int quantity = item.getAmount();

            // buy price must be defined for dynamic pricing to work
            if (Config.isDynamicPricing() && shopItem.isUseDynamicPricing() && shopItem.hasBuyPrice()) {
                moneyToGive = moneyToGive.add(GUIShop.getDYNAMICPRICING().calculateSellPrice(item.getType().toString(), quantity,
                        shopItem.getBuyPriceAsDecimal(), shopItem.getSellPriceAsDecimal()));
                GUIShop.getDYNAMICPRICING().sellItem(item.getType().toString(), quantity);
            } else {
                moneyToGive = moneyToGive.add(shopItem.getSellPriceAsDecimal().multiply(BigDecimal.valueOf(quantity)));
            }
        }

        if (couldntSell) {
            GUIShop.sendPrefix(player, "cant-sell", countSell);
            return;
        }
        roundAndGiveMoney(player, moneyToGive);

        String materialsString = Arrays.stream(items).map(item -> item.getType().toString()).collect(Collectors.joining(", "));

        int itemAmount = 0;

        for (Map.Entry<Material, Integer> entry : itemMap.entrySet()) {
            itemAmount += entry.getValue();
        }

        GUIShop.transactionLog(
                "Player " + player.getName() + " sold " + itemAmount + " items (" + itemMap.size() + " different) for " + moneyToGive.toPlainString() + ". Items: \n" + materialsString);
    }

    /**
     * Rounds the amount and deposits it on behalf of the player.
     *
     * @param player the player
     * @param moneyToGive the amount to give
     */
    public static void roundAndGiveMoney(Player player, BigDecimal moneyToGive) {
        if (moneyToGive.compareTo(BigDecimal.ZERO) > 0) {
            GUIShop.getECONOMY().depositPlayer(player, moneyToGive.doubleValue());

            String amount = GUIShop.getINSTANCE().messageSystem.translate("currency-prefix") +
                    moneyToGive.toPlainString() + GUIShop.getINSTANCE().messageSystem.translate("currency-suffix");

            GUIShop.sendPrefix(player, "sell", amount);
        }
    }

    private void onSellClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        sell(player);
    }
}
