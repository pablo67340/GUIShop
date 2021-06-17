package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.INBTBase;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NbtParser;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.SkullCreator;
import java.math.BigDecimal;
import java.util.Map.Entry;

import lombok.Getter;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

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
        GUI.setOnBottomClick(event -> event.setCancelled(true));
        GUI.show(player);
    }

    /**
     * Preloads the inventory to display items.
     */
    public Quantity loadInventory() {
        GUI = new Gui(GUIShop.getINSTANCE(), 5, Config.getQtyTitle());
        int multiplier = 1;
        ShopPane page = new ShopPane(9, 5);
        for (int x = 19; x <= 25; x++) {

            ItemStack itemStack = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();

            if (item.hasPotion()) {
                PotionInfo pi = item.getPotionInfo();
                if (XMaterial.isNewVersion()) {

                    if (pi.getSplash()) {
                        itemStack = new ItemStack(Material.SPLASH_POTION);
                    }
                    PotionMeta pm = (PotionMeta) itemStack.getItemMeta();

                    PotionData pd;
                    try {
                        pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                        pm.setBasePotionData(pd);
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().contains("upgradable")) {
                            GUIShop.log("Potion: " + pi.getType() + " Is not upgradable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                            pi.setUpgraded(false);
                            pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                            pm.setBasePotionData(pd);
                        } else if (ex.getMessage().contains("extended")) {
                            GUIShop.log("Potion: " + pi.getType() + " Is not extendable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                            pi.setExtended(false);
                            pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                            pm.setBasePotionData(pd);
                        }
                    }
                    itemStack.setItemMeta(pm);
                } else {
                    Potion potion = new Potion(PotionType.valueOf(pi.getType()), pi.getUpgraded() ? 2 : 1, pi.getSplash(), pi.getExtended());
                    potion.apply(itemStack);
                }
            }

            itemStack.setAmount(multiplier);
            ItemMeta itemMeta = itemStack.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (item.hasShopLore()) {
                item.getShopLore().forEach(str -> {
                    lore.add(ChatColor.translateAlternateColorCodes('&', str));
                });
            }

            lore.add(item.getBuyLore(multiplier));
            lore.add(item.getSellLore(multiplier));

            assert itemMeta != null;
            itemMeta.setLore(lore);

            if (item.isDisableQty() && x >= 20) {
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

            if (item.hasCustomModelID()) {
                itemMeta.setCustomModelData(item.getCustomModelData());
            }

            if (item.hasItemFlags()) {
                itemMeta.addItemFlags((ItemFlag[]) item.getItemFlags().toArray());
            }

            if (item.hasEnchantments()) {
                if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        assert meta != null;
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(meta);
                    }
                } else {
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(itemMeta);
                    }
                }
            } else {
                itemStack.setItemMeta(itemMeta);
            }

            if (itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial() && item.hasSkullUUID()) {
                itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(item.getSkullUUID()), item.getSkullUUID());
            }

            if (item.hasNBT()) {
                try {
                    NBTTagCompound oldComp = ItemNBTUtil.getTag(itemStack);
                    NBTTagCompound newComp = NbtParser.parse(item.getNBT());
                    for (Entry<String, INBTBase> entry : oldComp.getAllEntries().entrySet()) {
                        if (!newComp.hasKey(entry.getKey())) {
                            newComp.set(entry.getKey(), entry.getValue());
                        }
                    }
                    itemStack = ItemNBTUtil.setNBTTag(newComp, itemStack);

                } catch (NbtParser.NbtParseException ex) {
                    GUIShop.log("Error Parsing Custom NBT for Item: " + item.getMaterial() + " in Shop: " + currentShop.getShop() + ". Please fix or remove custom-nbt value.");
                }
            }

            GuiItem gItem = new GuiItem(itemStack);
            page.setItem(gItem, x);
            qty.put(x, multiplier);
            multiplier *= 2;
        }

        if (!Config.isDisableBackButton()) {

            ItemStack backButtonItem = XMaterial.matchXMaterial(Config.getBackButtonItem()).get().parseItem();

            ItemMeta backButtonMeta = backButtonItem.getItemMeta();

            assert backButtonMeta != null;
            backButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    Objects.requireNonNull(GUIShop.INSTANCE.getConfig().getString("back"))));

            backButtonItem.setItemMeta(backButtonMeta);

            GuiItem gItem = new GuiItem(backButtonItem, this::onQuantityClick);
            page.setItem(gItem, 44);

        }
        GUI.addPane(page);

        return this;
    }

    /**
     * Executes when an item is clicked inside the Quantity Inventory.
     */
    private void onQuantityClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!Config.isDisableBackButton()) {
            if (e.getSlot() == 44) {
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
            player.sendMessage(Config.getPrefix() + " " + Config.getFull());
            return;
        }

        buy(item, qty.get(e.getSlot()));
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if (!Config.isDisableEscapeBack() || !Config.isDisableEscapeBackQuantity()) {
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> currentShop.open(player), 1L);
        }
    }

    public void buy(Item item, int quantity) {
        if (!item.hasBuyPrice()) {
            player.sendMessage(Config.getPrefix() + " " + Config.getCantBuy());
            return;
        }

        // If the quantity is 0
        if (quantity == 0) {
            quantity = 1;
        }

        if (Material.getMaterial(item.getMaterial()).getMaxStackSize() < quantity) {
            GUIShop.sendMessage(player, Config.getTooHighBuyQuantity());
            return;
        }

        BigDecimal priceToPay;

        /*
         * If the map is empty, then the items purchased don't overflow the player's
         * inventory. Otherwise, we need to reimburse the player (subtract it from
         * priceToPay).
         */
        double priceToReimburse = 0D;

        Runnable dynamicPricingUpdate = null;

        // sell price must be defined and nonzero for dynamic pricing to work
        if (Config.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {
            String itemString = item.getItemString();
            int finalQuantity = quantity;
            dynamicPricingUpdate = () -> GUIShop.getDYNAMICPRICING().buyItem(itemString, finalQuantity);

            priceToPay = GUIShop.getDYNAMICPRICING().calculateBuyPrice(itemString, quantity, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
        }

        priceToPay = priceToPay.subtract(BigDecimal.valueOf(priceToReimburse));

        // Check if the transition was successful
        if (GUIShop.getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            // If the player has the sound enabled, play it
            if (Config.isSoundEnabled()) {
                player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
            }

            player.sendMessage(Config.getPrefix() + " " + Config.getPurchased() +
                    Config.getCurrency() + priceToPay + Config.getCurrencySuffix() + Config.getTaken());

            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            player.getInventory().addItem(item.toBuyItemStack(quantity, player, currentShop));
        } else {
            player.sendMessage(Config.getPrefix() + " " + Config.getNotEnoughPre() +
                    Config.getCurrency() + priceToPay + Config.getCurrencySuffix() + Config.getNotEnoughPost());
        }
    }
}
