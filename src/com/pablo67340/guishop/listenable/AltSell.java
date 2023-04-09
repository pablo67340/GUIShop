package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.AltSellPane;
import com.pablo67340.guishop.definition.Item;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.NoSuchElementException;
import org.bukkit.event.inventory.ClickType;

public class AltSell {

    private final Item subjectItem;
    private final Gui gui;

    private AltSellPane pane;

    private final Item indicatorItem;
    private final Item addItem;
    private final Item removeItem;
    private final Item confirmItem;
    private final Item cancelItem;

    private boolean hasClicked = false;

    @Getter
    private final Shop shop;

    public AltSell(Item subjectItem, Shop shop) {
        this.subjectItem = subjectItem;
        gui = new Gui(GUIShop.getINSTANCE(), 6, ChatColor.translateAlternateColorCodes('&', Config.getAltSellConfig().getTitle()));
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

    private GuiItem setQuantityAndGet(ItemStack item, int quantity, boolean isDecrease) {
        item.setAmount(quantity);

        ItemMeta im = item.getItemMeta();

        if (isDecrease) {
            im.setDisplayName(Config.getAltSellConfig().getDecreaseTitle().replace("%amount%", Integer.toString(quantity)));
        } else {
            im.setDisplayName(Config.getAltSellConfig().getIncreaseTitle().replace("%amount%", Integer.toString(quantity)));
        }

        item.setItemMeta(im);

        return new GuiItem(item);
    }

    public void open(Player player) {
        if (!GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.sell")) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-permission");
            return;
        }

        GuiItem gItem;
        GuiItem gIndicator;
        GuiItem gAddItem;
        GuiItem gRemoveItem;
        GuiItem gConfirmItem;
        GuiItem gCancelItem;

        try {
            gItem = new GuiItem(XMaterial.matchXMaterial(subjectItem.getMaterial()).get().parseItem());
            gIndicator = new GuiItem(XMaterial.matchXMaterial(indicatorItem.getMaterial()).get().parseItem());
            gAddItem = new GuiItem(XMaterial.matchXMaterial(addItem.getMaterial()).get().parseItem());
            gRemoveItem = new GuiItem(XMaterial.matchXMaterial(removeItem.getMaterial()).get().parseItem());
            gConfirmItem = new GuiItem(XMaterial.matchXMaterial(confirmItem.getMaterial()).get().parseItem());
            gCancelItem = new GuiItem(XMaterial.matchXMaterial(cancelItem.getMaterial()).get().parseItem());
        } catch (NoSuchElementException | NullPointerException exception) {
            GUIShop.getINSTANCE().getLogUtil().log("One or more of the materials you defined in the alt sell GUI are not valid.");
            return;
        }

        if (gItem != null && gIndicator != null && gAddItem != null && gRemoveItem != null && gConfirmItem != null && gCancelItem != null) {
            GuiItem[] addRemoveItems = new GuiItem[6];
            ItemStack addItemstack = gAddItem.getItem();
            addRemoveItems[0] = setQuantityAndGet(addItemstack.clone(), Config.getAltSellConfig().getQuantity1(), false);
            addRemoveItems[1] = setQuantityAndGet(addItemstack.clone(), Config.getAltSellConfig().getQuantity2(), false);
            addRemoveItems[2] = setQuantityAndGet(addItemstack.clone(), Config.getAltSellConfig().getQuantity3(), false);
            ItemStack removeItemstack = gRemoveItem.getItem();
            addRemoveItems[3] = setQuantityAndGet(removeItemstack.clone(), Config.getAltSellConfig().getQuantity1(), true);
            addRemoveItems[4] = setQuantityAndGet(removeItemstack.clone(), Config.getAltSellConfig().getQuantity2(), true);
            addRemoveItems[5] = setQuantityAndGet(removeItemstack.clone(), Config.getAltSellConfig().getQuantity3(), true);
            pane = new AltSellPane(gItem, addRemoveItems, gIndicator,
                    Item.renameGuiItem(gConfirmItem, Config.getAltSellConfig().getConfirmName()), Item.renameGuiItem(gCancelItem, Config.getAltSellConfig().getCancelName()));
            pane.setSubjectQuantity(1);
            pane.setIndicatorName(subjectItem.getSellLore(1));
            gui.addPane(pane);
            gui.setOnTopClick(this::onClick);
            gui.setOnBottomClick(event -> event.setCancelled(true));
            gui.setOnClose(this::onClose);
            gui.setOnGlobalClick(this::onGlobalClick);
            gui.show(player);
        } else {
            GUIShop.getINSTANCE().getLogUtil().log("One or more of the materials you defined in the alt sell GUI are not valid.");
        }
    }
    
    private void onGlobalClick(InventoryClickEvent event){
        if (event.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            event.setCancelled(true);
        }
    }

    private void changeQuantity(int delta) {
        hasClicked = true;
        int previous = pane.getSubjectQuantity();
        int update = previous + delta;
        if (update < 1) {
            update = 1;
        }
        update = pane.setSubjectQuantity(update);
        if (update != previous) {
            pane.setIndicatorName(subjectItem.getSellLore(pane.getSubjectQuantity()));
            gui.update();
        }
    }

    private void sell(Player player, ItemStack itemStack) {
        NBTItem comp = new NBTItem(itemStack);
        comp.removeKey("IF-uuid");
        itemStack = comp.getItem();

        GUIShop.getINSTANCE().getLogUtil().log(itemStack.toString());

        int amount = itemStack.getAmount();
        Map<Integer, ItemStack> result = player.getInventory().removeItem(itemStack);
        if (result.isEmpty()) {
            Sell.roundAndGiveMoney(player, subjectItem.calculateSellPrice(amount));
            // buy price must be defined for dynamic pricing to work
            if (subjectItem.hasBuyPrice() && Config.isDynamicPricing()) {
                GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().sellItem(subjectItem.getItemString(), amount);
            }

            GUIShop.getINSTANCE().getLogUtil().transactionLog(
                    "Player " + player.getName() + " sold " + amount + " items (1 different) for " + subjectItem.calculateSellPrice(amount) + ". Item: \n" + itemStack.getType());
        } else {
            ItemStack addBack = result.get(0).clone();
            addBack.setAmount(amount - addBack.getAmount());
            if (addBack.getAmount() > 0) {
                player.getInventory().addItem(addBack);
            }
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "alt-sell-not-enough", amount);
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

        switch (event.getSlot()) {
            case 18:
            case 19:
            case 20:
                changeQuantity(event.getCurrentItem().getAmount());
                break;
            case 24:
            case 25:
            case 26:
                changeQuantity(-event.getCurrentItem().getAmount());
                break;
            case 48:
                sell((Player) event.getWhoClicked(), event.getInventory().getItem(13));
                break;
            case 50:
                hasClicked = true;
                if (GUIShop.getINSTANCE().isReload) {
                    event.getWhoClicked().closeInventory();
                    break;
                }

                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                if (this.shop != null) {
                    scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> shop.open((Player) event.getWhoClicked()), 1L);
                } else {
                    scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> PlayerListener.INSTANCE.openMenu((Player) event.getWhoClicked()), 1L);
                }
                break;
            default:
                break;
        }
    }
}
