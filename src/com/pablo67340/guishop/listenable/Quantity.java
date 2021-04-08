package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.SkullCreator;

import lombok.Getter;

class Quantity {

    /**
     * The item currently being targetted.
     */
    private final Item item;

    /**
     * The GUI that will be displayed.
     */
    private Gui GUI;

    /**
     * The map containing the sell increments.
     */
    private final Map<Integer, Integer> qty = new HashMap<>();

    /**
     * The instance of the {@link Shop} that spawned this Quantity.
     */
    private final Shop currentShop;

    @Getter
    private final Player player;

    Quantity(Item item, Shop shop, Player input) {
        this.item = item;
        this.currentShop = shop;
        this.player = input;
    }

    /**
     * Opens the GUI to sell the items in.
     */
    void open() {
        GUI.setOnClose(this::onClose);
        GUI.setOnTopClick(this::onQuantityClick);
        GUI.setOnBottomClick(event -> {
            event.setCancelled(true);
        });
        GUI.show(player);
    }

    /**
     * Preloads the inventory to display items.
     */
    public Quantity loadInventory() {
        GUI = new Gui(Main.getINSTANCE(), 6, ConfigUtil.getQtyTitle());
        int multiplier = 1;
        ShopPane page = new ShopPane(9, 6);
        for (int x = 19; x <= 25; x++) {

            GuiItem gItem = item.parseMaterial();

            ItemStack itemStack = gItem.getItem();

            itemStack.setAmount(multiplier);
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = new ArrayList<>();

            lore.add(item.getBuyLore(multiplier));

            item.getShopLore().forEach(str -> {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            });

            assert itemMeta != null;
            itemMeta.setLore(lore);

            String type = itemStack.getType().toString();

            boolean isInList = ConfigUtil.getDisabledQty().stream().anyMatch(t -> spellCheck(type, t));

            if (isInList && x >= 20) {
                break;
            }

            if (item.hasShopName()) {
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getShopName()));
            } else if (Item.isSpawnerItem(itemStack)) {
                String mobName = item.getMobType();
                mobName = mobName.toLowerCase();
                mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
                itemMeta.setDisplayName(mobName + " Spawner");
            }

            if (item.hasEnchantments()) {
                if (item.getEnchantments().length > 1) {
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemStack.addUnsafeEnchantment(
                                XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(),
                                Integer.parseInt(level));
                    }
                }
            }

            itemStack.setItemMeta(itemMeta);

            if (itemStack.getType() == Material.PLAYER_HEAD && item.hasSkullUUID()) {
                itemStack.setItemMeta(SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(item.getSkullUUID())));
            }

            if (item.hasPotion()) {
                item.getPotion().apply(itemStack);
            }

            page.setItem(gItem, x);
            qty.put(x, multiplier);
            multiplier *= 2;
        }

        if (!ConfigUtil.isEscapeOnly()) {

            ItemStack backButtonItem = XMaterial.matchXMaterial(ConfigUtil.getBackButtonItem()).get().parseItem();

            ItemMeta backButtonMeta = backButtonItem.getItemMeta();

            assert backButtonMeta != null;
            backButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(Main.INSTANCE.getConfig().getString("back"))));

            backButtonItem.setItemMeta(backButtonMeta);

            GuiItem gItem = new GuiItem(backButtonItem, this::onQuantityClick);
            page.setItem(gItem, 53);

        }
        GUI.addPane(page);

        return this;
    }

    private Boolean spellCheck(String type, String t) {
        return type.contains(t);
    }

    /**
     * Executes when an item is clicked inside the Quantity Inventory.
     */
    private void onQuantityClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!ConfigUtil.isEscapeOnly()) {
            if (e.getSlot() == 53) {
                currentShop.open(player);
                return;
            }
        }

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ConfigUtil.getFull());
            return;
        }

        if (!item.hasBuyPrice()) {
            player.sendMessage(ConfigUtil.getCantBuy());
            return;
        }

        // Does the quantity work out?
        int quantity = qty.get(e.getSlot());

        // If the quantity is 0
        if (quantity == 0) {
            player.sendMessage(ConfigUtil.getPrefix() + " " + ConfigUtil.getNotEnoughPre() + item.calculateBuyPrice(1)
                    + ConfigUtil.getNotEnoughPost());
            player.setItemOnCursor(new ItemStack(Material.AIR));
            return;
        }

        ItemStack itemStack = e.getCurrentItem().clone();

        // remove IF's IF-uuid NBT tag
        NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
        comp.remove("IF-uuid");
        itemStack = ItemNBTUtil.setNBTTag(comp, itemStack);

        // If the item is not a mob spawner
        if (!item.isMobSpawner()) {
            // If the item has enchantments
            if (item.hasEnchantments()) {
                if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        assert meta != null;
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(),
                                Integer.parseInt(level), true);

                    }
                } else {
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemStack.addUnsafeEnchantment(
                                XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(),
                                Integer.parseInt(level));
                    }

                }
            }
        }

        List<String> lore = new ArrayList<>();

        if (item.hasBuyLore()) {
            item.getBuyLore().forEach(str -> {
                lore.add(ChatColor.translateAlternateColorCodes('&', Main.placeholderIfy(str, player, item)));
            });
        }

        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setLore(lore);

        if (item.hasBuyName()) {
            assert itemMeta != null;
            itemMeta.setDisplayName(
                    ChatColor.translateAlternateColorCodes('&', Main.placeholderIfy(item.getBuyName(), player, item)));
        } else if (Item.isSpawnerItem(itemStack)) {
            String mobName = item.getMobType();
            mobName = mobName.toLowerCase();
            mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
            assert itemMeta != null;
            itemMeta.setDisplayName(mobName + " Spawner");
        }

        itemStack.setItemMeta(itemMeta);

        double priceToPay;

        /*
	* If the map is empty, then the items purchased don't overflow the player's
	* inventory. Otherwise, we need to reimburse the player (subtract it from
	* priceToPay).
         */
        double priceToReimburse = 0D;

        // if the item is not a shift click
        int amount = itemStack.getAmount();

        Runnable dynamicPricingUpdate = null;

        // sell price must be defined and nonzero for dynamic pricing to work
        if (ConfigUtil.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {

            String itemString = item.getItemString();
            dynamicPricingUpdate = () -> Main.getDYNAMICPRICING().buyItem(itemString, amount);

            priceToPay = Main.getDYNAMICPRICING().calculateBuyPrice(itemString, amount, item.getBuyPriceAsDouble(), item.getSellPriceAsDouble());
        } else {
            priceToPay = item.getBuyPriceAsDouble() * amount;
        }

        priceToPay -= priceToReimburse;

        // Check if the transition was successful
        if (Main.getECONOMY().withdrawPlayer(player, priceToPay).transactionSuccess()) {
            // If the player has the sound enabled, play
            // it!
            if (ConfigUtil.isSoundEnabled()) {

                player.playSound(player.getLocation(), XSound.matchXSound(ConfigUtil.getSound()).get().parseSound(), 1, 1);

            }
            player.sendMessage(ConfigUtil.getPrefix() + ConfigUtil.getPurchased() + priceToPay + ConfigUtil.getTaken()
                    + ConfigUtil.getCurrencySuffix());

            if (item.isMobSpawner()) {

                EntityType type = item.parseMobSpawnerType();
                if (type == null) {
                    Main.log("Invalid EntityType in shops.yml: " + item.getMobType());

                } else {
                    String entityValue = type.name();
                    Main.debugLog("Attaching " + entityValue + " to purchased spawner");

                    NBTTagCompound tag = ItemNBTUtil.getTag(itemStack);
                    tag.setString("GUIShopSpawner", entityValue);
                    itemStack = ItemNBTUtil.setNBTTag(tag, itemStack);
                }
            }

            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            if (itemStack.getType() == Material.PLAYER_HEAD && item.hasSkullUUID()) {
                itemStack.setItemMeta(SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(item.getSkullUUID())));
            }

            player.getInventory().addItem(itemStack);

        } else {
            player.sendMessage(ConfigUtil.getPrefix() + ConfigUtil.getNotEnoughPre() + priceToPay + ConfigUtil.getNotEnoughPost());
        }

    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if (ConfigUtil.isEscapeOnly()) {
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> currentShop.open(player), 1L);

        }
    }
}
