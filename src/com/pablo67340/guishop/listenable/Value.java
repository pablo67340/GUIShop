package com.pablo67340.guishop.listenable;

import java.util.*;

import org.bukkit.*;
import org.bukkit.entity.Player;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.ShopItem;
import com.pablo67340.guishop.definition.ShopPage;

import lombok.Getter;
import lombok.Setter;

public class Value {

    /**
     * The name of this {@link Shop}.
     */
    @Getter
    @Setter
    private String targetMaterial;

    /**
     * The list of {@link ShopPage}'s in this {@link Shop}.
     */
    private Gui GUI;

    private ShopItem shopItem;

    private ShopPane shopPage = new ShopPane(9, 6);

    private final Player player;

    private Integer pageIndex = 0;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param player The player using the shop.
     * @param targetMaterial The item that is being valued.
     */
    public Value(Player player, String targetMaterial) {
        this.player = player;
        this.targetMaterial = targetMaterial;
    }

    /**
     * Load the specified shop
     *
     */
    public void loadItems() {
        shopItem = new ShopItem();
        ShopPage page = new ShopPage();
        int index = 0;
        if (!GUIShop.getINSTANCE().getITEMTABLE().containsKey(targetMaterial)) {
            GUIShop.sendPrefix(player, "value.doesnt-exist");
            return;
        }

        for (Item item : GUIShop.getINSTANCE().getITEMTABLE().get(targetMaterial)) {
            GUIShop.debugLog("Reading item value: " + item.getMaterial());
            page.getItems().put(Integer.toString(index), item);
            index += 1;
        }
        GUIShop.debugLog("Adding page: " + "Page" + shopItem.getPages().size() + " to pages.");
        shopItem.getPages().put("Page" + shopItem.getPages().size(), page);
        loadShop();
    }

    private void loadShop() {
        if (this.GUI == null || this.GUI.getItems().isEmpty()) {
            if (this.hasMultiplePages()) {
                this.GUI = new Gui(GUIShop.getINSTANCE(), 6,
                        ChatColor.translateAlternateColorCodes('&', "&2Item values"));
            } else {
                int rows = (int) Math.ceil((double) shopItem.getPages().get("Page0").getItems().size() / 9);
                if (rows == 0) {
                    rows = 1;
                }
                this.GUI = new Gui(GUIShop.getINSTANCE(), rows,
                        ChatColor.translateAlternateColorCodes('&', "&2Item values"));
            }
            PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
            Collection<ShopPage> shopPages = shopItem.getPages().values();
            for (ShopPage page : shopPages) {
                shopPage = new ShopPane(9, 6);
                for (Item item : page.getItems().values()) {
                    GuiItem gItem = new GuiItem(item.toItemStack(player, false));
                    shopPage.addItem(gItem);
                }
                pane.addPane(pageIndex, shopPage);
                pageIndex += 1;
            }

            GUI.addPane(pane);
            open();
        }
    }

    public boolean hasMultiplePages() {
        return this.shopItem.getPages().size() > 1;
    }

    /**
     * Open the player's shop
     *
     */
    public void open() {
        GUI.show(player);
        GUI.setOnTopClick((e) -> e.setCancelled(true));
        GUI.setOnBottomClick((e) -> e.setCancelled(true));
    }
}
