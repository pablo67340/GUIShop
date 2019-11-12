package com.pablo67340.guishop.listenable;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.pablo67340.guishop.definition.Enchantments;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.handler.Item;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.ShopPane;
import com.pablo67340.guishop.util.XMaterial;

import lombok.Getter;

public class Shop {

    /**
     * The name of this {@link Shop}.
     */
    @Getter
    private final String name;

    /**
     * The shop name of this {@link Shop}.
     */
    @Getter
    private final String shop;

    /**
     * The description of this {@link Shop}.
     */
    @Getter
    private final String description;

    /**
     * The lore of this {@link Shop}.
     */
    @Getter
    private final List<String> lore;

    /**
     * The list of {@link Item}s in this {@link Shop}.
     */
    @Getter
    private List<Item> items;

    /**
     * The list of {@link Page}'s in this {@link Shop}.
     */
    private Gui GUI;

    private Menu menuInstance;

    private Boolean hasClicked = false;

    private int pageC = 0;

    private PaginatedPane currentPane;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param name        The name of the shop.
     * @param description The description of the shop.
     * @param lore        The lore of the shop.
     */
    Shop(String shop, String name, String description, List<String> lore, Menu menuInstance) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
        this.menuInstance = menuInstance;
    }

    /**
     * The constructor for a {@link Shop}.
     *
     * @param name        The name of the shop.
     * @param description The description of the shop.
     * @param lore        The lore of the shop.
     */
    Shop(String shop, String name, String description, List<String> lore, Menu menuInstance,
         List<Item> items) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
        this.menuInstance = menuInstance;
        this.items = items;
    }

    /**
     * Load the specified shop
     */
    @SuppressWarnings({"unchecked", "WrapperTypeMayBePrimitive"})
    void loadItems() {

        if (items == null) {

            items = new ArrayList<>();

            Integer index = 0;

            Item item;

            ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);

            assert config != null;
            for (String str : config.getKeys(true)) {

                item = new Item();
                index += 1;

                List<Map<?, ?>> citem = config.getMapList(str);

                item.setSlot((Integer.parseInt(str) - 1));

                for (Map<?, ?> map : citem) {

                    try {
                        if (map.containsKey("id")) {
                            String itemID = (String) map.get("id");
                            item.setMaterial(itemID);
                        } else if (map.containsKey("mobType")) {
                            item.setMobType((String) map.get("mobType"));
                        } else if (map.containsKey("name")) {
                            item.setName((String) map.get("name"));
                        } else if (map.containsKey("enchantments")) {
                            String preEnc = (String) map.get("enchantments");
                            if (!preEnc.equalsIgnoreCase("")) {
                                String[] enchants = preEnc.split(" ");
                                item.setEnchantments(enchants);
                            }
                        } else if (map.containsKey("buy-price")) {
                            item.setBuyPrice(map.get("buy-price"));
                        } else if (map.containsKey("sell-price")) {
                            item.setSellPrice(map.get("sell-price"));
                        } else if (map.containsKey("type")) {
                            ItemType type = ItemType.valueOf((String) map.get("type"));
                            item.setItemType(type);
                        } else if (map.containsKey("commands")) {
                            item.setCommands((List<String>) map.get("commands"));
                        }

                    } catch (Exception e) {
                        Main.getINSTANCE().getLogger().warning("�cError occured while reading item: " + (index - 1)
                                + " from shop: " + getShop() + " Error: " + e.getMessage());
                        Main.getINSTANCE().getLogger()
                                .warning("�cThis plugin will not function properly until error is addressed!");
                        Main.getDebugger().setHasExploded(true);
                        Main.getDebugger().setErrorMessage(
                                "�cError occured while reading item: " + (index - 1) + " from shop: " + getShop());
                    }
                }

                // Update shops.yml to add type
                if (item.getItemType() == null) {
                    item.setItemType(ItemType.ITEM);

                    List<Map<?, ?>> mapList = config.getMapList(index.toString());
                    Map<String, String> type = new HashMap<>();
                    type.put("type", item.getItemType().toString());
                    mapList.add(type);
                    config.set(index.toString(), mapList);
                    // Toggle pending for save.
                }

                items.add(item);
            }
            loadShop();
        } else {
            loadShop();
        }

    }

    private void loadShop() {
        Integer index = 0, lastIndex = 0;
        ShopPane page = new ShopPane(9, 6);

        this.GUI = new Gui(Main.getINSTANCE(), 6,
                ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());
        PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);

        for (Item item : items) {

            ItemStack itemStack = Objects.requireNonNull(XMaterial.matchXMaterial(item.getMaterial())).parseItem();

            assert itemStack != null;
            GuiItem gItem = new GuiItem(itemStack);

            ItemMeta itemMeta = gItem.getItem().getItemMeta();

            List<String> lore = new ArrayList<>();

            if (item.canBuyItem()) {
                if ((Double) item.getBuyPrice() != 0.0) {
                    lore.add(Config.getBuyLore().replace("{amount}",
                            Config.getCurrency() + item.getBuyPrice() + Config.getCurrencySuffix()));
                } else {
                    lore.add(Config.getFreeLore());
                }
            } else {
                lore.add(Config.getCannotBuy());
            }

            if (item.canSellItem()) {
                lore.add(Config.getSellLore().replace("{amount}",
                        Config.getCurrency() + item.getSellPrice() + Config.getCurrencySuffix()));
            } else {
                lore.add(Config.getCannotSell());
            }

            if (!lore.isEmpty()) {
                assert itemMeta != null;
                itemMeta.setLore(lore);
            }

            if (item.getCommands() != null) {
                assert itemMeta != null;
                List<String> currentLore = itemMeta.getLore();
                List<String> commands = item.getCommands();
                List<String> newCommands = new ArrayList<>();
                for (String cmd : commands) {
                    newCommands.add(
                            StringUtils.substringBefore(cmd, "::") + "   " + StringUtils.substringAfter(cmd, "::"));
                }
                assert currentLore != null;
                currentLore.add(" ");
                currentLore.add(Config.getAccessTo());
                currentLore.addAll(newCommands);
                itemMeta.setLore(currentLore);
            }

            if (item.getName() != null) {
                assert itemMeta != null;
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));
            } else if (item.isMobSpawner()) {
                String mobName = item.getMobType();
                mobName = mobName.toLowerCase();
                mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
                assert itemMeta != null;
                itemMeta.setDisplayName(mobName + " Spawner");
            }

            gItem.getItem().setItemMeta(itemMeta);

            if (item.getEnchantments() != null) {
                for (String enc : item.getEnchantments()) {
                    String enchantment = StringUtils.substringBefore(enc, ":");
                    String level = StringUtils.substringAfter(enc, ":");
                    gItem.getItem().addUnsafeEnchantment(Enchantments.getByName(enchantment), Integer.parseInt(level));
                }
            }

            // Create Page
            if (index == items.size() || ((index) - lastIndex) == 44) {
                page.setItem(gItem, item.getSlot());

                if (items.size() > 45) {
                    applyButtons(page);
                }
                lastIndex = index;
                pane.addPane(pageC, page);
                pageC += 1;
                page = new ShopPane(9, 6);
            } else {
                if (pageC == 0) {
                    page.setItem(gItem, item.getSlot() - lastIndex);
                } else {
                    page.setItem(gItem, item.getSlot() - lastIndex - 1);
                }
            }

            if (index + 1 == items.size()) {
                pane.addPane(pageC, page);
                applyButtons(page);
                GUI.addPane(pane);
                Main.getINSTANCE().getLoadedShops().put(name, items);
            }
            index += 1;
        }

        this.currentPane = pane;

    }

    private void applyButtons(ShopPane page) {
        if (page.getItems().size() == 45) {
            page.setItem(new GuiItem(new ItemStack(Material.ARROW)), 51);
        }
        if (pageC > 0) {
            page.setItem(new GuiItem(new ItemStack(Material.ARROW)), 47);
        }
        if (!Config.isEscapeOnly()) {

            ItemStack backButtonItem = new ItemStack(Objects.requireNonNull(XMaterial.valueOf(Config.getBackButtonItem()).parseMaterial()));

            ItemMeta backButtonMeta = backButtonItem.getItemMeta();

            assert backButtonMeta != null;
            backButtonMeta.setDisplayName(Config.getBackButtonText());

            backButtonItem.setItemMeta(backButtonMeta);

            GuiItem item = new GuiItem(backButtonItem);

            page.setItem(item, 53);
        }
    }

    /**
     * Open the player's shop
     */
    void open(Player input) {
        GUI.show(input);
        GUI.setOnClose(this::onClose);
        GUI.setOnTopClick(this::onShopClick);
        GUI.setOnBottomClick(event -> {
        	event.setCancelled(true);
        });
    }

    private void onShopClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        hasClicked = true;

        /*
         * If the player's inventory is full
         */
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Config.getFull());
            return;
        }

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        if (e.getSlot() >= 0 && e.getSlot() < GUI.getItems().size()) {
            /*
             * If the player clicks the 'back' button, then open the menu. Otherwise, If the
             * user clicks the forward button, load and open next page, Otherwise, If the
             * user clicks the backward button, load and open the previous page, Otherwise
             * Attempt to purchase the clicked item.
             */

            // Forward Button
            if (e.getSlot() == 51) {
                hasClicked = true;

                Pane[] arr = new Pane[54];
                arr = currentPane.getPanes().toArray(arr);
                arr[currentPane.getPage()].setVisible(false);
                currentPane.setPage(currentPane.getPage() + 1);

                arr[currentPane.getPage()].setVisible(true);
                GUI.update();
                return;
                // Backward Button
            } else if (e.getSlot() == 47) {
                hasClicked = true;
                Pane[] arr = new Pane[54];
                arr = currentPane.getPanes().toArray(arr);
                arr[currentPane.getPage()].setVisible(false);
                currentPane.setPage(currentPane.getPage() - 1);

                arr[currentPane.getPage()].setVisible(true);
                GUI.update();
                return;
                // Back Button
            } else if (e.getSlot() == 53 && !Config.isEscapeOnly()) {
                menuInstance.open(player);
                return;
            }

            /*
             * If the player has enough money to purchase the item, then allow them to.
             */

            Item item = getItems().get((currentPane.getPage() * 45) + e.getSlot());
            if (item.getItemType() == ItemType.COMMAND) {
                if (Main.getINSTANCE().purchaseCommands(player.getUniqueId(), item.getCommands())) {
                    if (Main.getECONOMY().withdrawPlayer(player, (Double) item.getBuyPrice()).transactionSuccess()) {
                        // If the player has the sound enabled, play
                        // it!
                        if (Config.isSoundEnabled()) {
                            try {
                                player.playSound(player.getLocation(), Sound.valueOf(Config.getSound()), 1, 1);

                            } catch (Exception ex) {
                                Main.getINSTANCE().getLogger().warning(
                                        "�cIncorrect sound specified in config. Make sure you are using sounds from the right version of your server!");
                            }
                        }
                        player.sendMessage(Config.getPrefix() + Config.getPurchased() + item.getBuyPrice()
                                + Config.getTaken() + Config.getCurrencySuffix());
                    } else {
                        player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre() + item.getBuyPrice()
                                + Config.getNotEnoughPost());
                    }
                } else {
                    player.sendMessage(Config.getCommandAlready());
                }
            } else {
                Quantity qty = new Quantity(item, this);
                qty.loadInventory();
                qty.open(player);
            }
        }
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        if (Config.isEscapeOnly() && !hasClicked) {
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> menuInstance.open(player), 1L);

        } else {
            hasClicked = false;
        }

    }

}
