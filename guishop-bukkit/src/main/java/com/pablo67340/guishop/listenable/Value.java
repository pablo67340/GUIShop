package com.pablo67340.guishop.listenable;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ShopItem;
import com.pablo67340.guishop.definition.ShopPage;
import com.pablo67340.guishop.gui.SimpleGui;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class Value {

    /**
     * The name of this {@link Shop}.
     */
    @Getter
    @Setter
    private String targetMaterial;

    /**
     * The GUI for displaying values.
     */
    private SimpleGui GUI;

    private ShopItem shopItem;

    private final Player player;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param player         The player using the shop.
     * @param targetMaterial The item that is being valued.
     */
    public Value(Player player, String targetMaterial) {
        this.player = player;
        this.targetMaterial = targetMaterial;
    }

    /**
     * Load the specified shop
     */
    public void loadItems() {
        shopItem = new ShopItem();
        ShopPage page = new ShopPage();
        int index = 0;
        if (!GUIShop.getINSTANCE().getITEMTABLE().containsKey(targetMaterial)) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "value.doesnt-exist");
            return;
        }

        for (Item item : GUIShop.getINSTANCE().getITEMTABLE().get(targetMaterial)) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Reading item value: " + item.getMaterial());
            page.getItems().put(Integer.toString(index), item);
            index += 1;
        }
        GUIShop.getINSTANCE().getLogUtil().debugLog("Adding page: " + "Page" + shopItem.getPages().size() + " to pages.");
        shopItem.getPages().put("Page" + shopItem.getPages().size(), page);
        loadShop();
    }

    private void loadShop() {
        if (this.GUI == null) {
            int rows = (int) Math.ceil((double) shopItem.getPages().get("Page0").getItems().size() / 9);
            if (rows == 0) {
                rows = 1;
            }
            if (rows > 6) {
                rows = 6;
            }
            this.GUI = new SimpleGui(rows, Config.getTitlesConfig().getValueTitle());

            ShopPage page = shopItem.getPages().get("Page0");
            int slot = 0;
            for (Item item : page.getItems().values()) {
                if (slot < GUI.getRows() * 9) {
                    GUI.setItem(slot, item.toItemStack(player, false));
                    slot++;
                }
            }

            open();
        }
    }

    public boolean hasMultiplePages() {
        return this.shopItem.getPages().size() > 1;
    }

    /**
     * Open the player's shop
     */
    public void open() {
        GUI.setTopClickHandler(this::onTopClick);
        GUI.setBottomClickHandler((e) -> e.setCancelled(true));
        GUI.show(player);
    }

    private void onTopClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            return;
        }
    }
}
