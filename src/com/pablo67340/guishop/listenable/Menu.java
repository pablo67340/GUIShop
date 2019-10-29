package com.pablo67340.guishop.listenable;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.handler.ShopDir;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.ShopPane;
import com.pablo67340.guishop.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@SuppressWarnings({"JavaDoc", "SpellCheckingInspection"})
public final class Menu {

    /**
     * The GUI that is projected onto the screen when a {@link Player} opens the
     * {@link Menu}.
     */
    private Gui GUI;

    /**
     * The loaded shops read from the config.
     */
    private Map<Integer, ShopDir> shops;

    /**
     * A {@link Map} that will store our {@link Shop}s when the server first starts.
     *
     * @key The index on the {@link Menu} that this shop is located at.
     * @value The shop.
     */

    public Menu() {
        this.GUI = new Gui(Main.getINSTANCE(), Config.getMenuRows(), "Menu");
        this.shops = new HashMap<>();
    }

    /**
     * Preloads the configs into their corresponding objects.
     */
    public void preLoad(Player player) {

        ShopPane page = new ShopPane(9, 1);

        ConfigurationSection menuItems = Main.getINSTANCE().getConfig().getConfigurationSection("menu-items");

        assert menuItems != null;
        for (String key : menuItems.getKeys(false)) {

            if (!Main.getINSTANCE().getMainConfig().getBoolean("menu-items." + key + ".Enabled")) {
                continue;
            }

            String shop = ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("menu-items." + key + ".Shop")));

            String name = ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("menu-items." + key + ".Name")));

            String description = ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("menu-items." + key + ".Desc")));

            List<String> lore = new ArrayList<>();

            if (description.length() > 0) {
                lore.add(description);
            }

            shops.put(Integer.parseInt(key), new ShopDir(shop, name, description, lore));

            if (player.hasPermission("guishop.slot." + key) || player.isOp()
                    || player.hasPermission("guishop.slot.*")) {
                String itemID = Main.getINSTANCE().getMainConfig().getString("menu-items." + key + ".Item");

                ItemStack itemStack = XMaterial.valueOf(itemID).parseItem();

                GuiItem gItem = new GuiItem(itemStack);
                setName(gItem, name, lore);

                // SetItem no longer works with self created inventory object. Prefill with air?
                page.addItem(gItem);
            }
        }

        GUI.addPane(page);

    }

    /**
     * Opens the GUI in this {@link Menu}.
     */
    void open(Player player) {

        if (!player.hasPermission("guishop.use") && !player.isOp()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("no-permission"))));
            return;
        }

        if (Main.getINSTANCE().getMainConfig().getStringList("disabled-worlds").contains(player.getWorld().getName())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.getINSTANCE().getMainConfig().getString("disabled-world"))));
            return;
        }
        preLoad(player);
        GUI.setOnTopClick(this::onShopClick);
        GUI.show(player);
    }

    /**
     * Sets the item's display name.
     */
    private void setName(GuiItem item, String name, List<String> lore) {
        ItemMeta IM = item.getItem().getItemMeta();

        if (name != null) {
            assert IM != null;
            IM.setDisplayName(name);
        }

        if (lore != null && !lore.isEmpty()) {
            assert IM != null;
            IM.setLore(lore);
        }

        item.getItem().setItemMeta(IM);

    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop, if
     * so, run logic.
     */
    private void onShopClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ShopDir shopDef = shops.get(e.getSlot());
        if (!shopDef.getShop().equalsIgnoreCase("")) {
            /*
             * The currently open shop associated with this Menu instance.
             */
            Shop openShop;
            if (!Main.getINSTANCE().getLoadedShops().containsKey(e.getSlot())) {
                openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(),
                        shopDef.getLore(), e.getSlot(), this);
            } else {
                openShop = new Shop(shopDef.getShop(), shopDef.getName(), shopDef.getDescription(),
                        shopDef.getLore(), e.getSlot(), this, Main.getINSTANCE().getLoadedShops().get(e.getSlot()));
            }
            openShop.loadItems();
            openShop.open(player);
        }

    }

}
