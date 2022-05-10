package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTContainer;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.util.SkullCreator;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitScheduler;

import java.math.BigDecimal;
import java.util.*;
import org.bukkit.event.inventory.ClickType;

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
        GUI.setOnGlobalClick(this::onGlobalClick);
        GUI.setOnBottomClick(event -> event.setCancelled(true));
        GUI.show(player);
    }

    /**
     * Preloads the inventory to display items.
     */
    public Quantity loadInventory() {
        GUI = new Gui(GUIShop.getINSTANCE(), 5, Config.getTitlesConfig().getQtyTitle());
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

            lore.add(item.getBuyLore(multiplier));
            lore.add(item.getSellLore(multiplier));

            if (item.hasShopLore()) {
                item.getShopLore().forEach(str -> {
                    lore.add(ChatColor.translateAlternateColorCodes('&', str));
                });
            }

            itemMeta.setLore(lore);

            if ((item.getQuantityValue() != null && item.getQuantityValue().getQuantity() > -1) && x >= 20) {
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
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(meta);
                    }
                } else {
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
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
                ItemStack tempItem = itemStack.clone();
                NBTContainer container = new NBTContainer(item.getNBT());
                NBTItem nbti = new NBTItem(tempItem);
                nbti.mergeCompound(container);
                tempItem = nbti.getItem();

                if (tempItem == null) {
                    GUIShop.log("Error parsing custom NBT for item: " + item.getMaterial() + " in shop: " + currentShop.getShop() + ". Please fix or remove custom-nbt value.");
                } else {
                    itemStack = nbti.getItem();
                }
            }

            GuiItem gItem = new GuiItem(itemStack);
            page.setItem(gItem, x);
            qty.put(x, multiplier);
            multiplier *= 2;
        }

        if (!Config.isDisableBackButton()) {
            GuiItem gItem = new GuiItem(Config.getButtonConfig().getBackButton().toItemStack(player, false), this::onQuantityClick);
            page.setItem(gItem, 44);
        }

        GUI.addPane(page);
        return this;
    }
    
    private void onGlobalClick(InventoryClickEvent event){
        if (event.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            event.setCancelled(true);
        }
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
            GUIShop.sendPrefix(player, "full-inventory");
            return;
        }

        buy(item, qty.get(e.getSlot()));
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if ((!Config.isDisableEscapeBack() || !Config.isDisableEscapeBackQuantity()) && !GUIShop.getINSTANCE().isReload) {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> currentShop.open(player), 1L);
        }
    }

    public void buy(Item item, int quantity) {
        if (!item.hasBuyPrice()) {
            GUIShop.sendPrefix(player, "cant-buy");
            return;
        }

        // If the quantity is 0
        if (quantity == 0) {
            quantity = 1;
        }

        boolean tooHighQuantity = false;
        int maxStackSize = 64;

        try {
            Optional<XMaterial> material = XMaterial.matchXMaterial(item.getMaterial());

            if (material.isPresent() && material.get().parseMaterial().getMaxStackSize() < quantity) {
                tooHighQuantity = true;
                maxStackSize = material.get().parseMaterial().getMaxStackSize();
            }
        } catch (NoSuchElementException | NullPointerException ignored) {
        }

        if (tooHighQuantity) {
            GUIShop.sendPrefix(player, "too-high-quantity", maxStackSize);
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

        String currencyPrefix = GUIShop.getINSTANCE().messageSystem.translate("messages.currency-prefix");
        String currencySuffix = GUIShop.getINSTANCE().messageSystem.translate("messages.currency-suffix");
        String amount = currencyPrefix + priceToPay + currencySuffix;

        // Check if the transition was successful
        if (GUIShop.getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            // If the player has the sound enabled, play it
            if (Config.isSoundEnabled()) {
                player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
            }

            GUIShop.sendPrefix(player, "purchase", amount);

            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            player.getInventory().addItem(item.toBuyItemStack(quantity, player, currentShop));

            GUIShop.transactionLog(
                    "Player " + player.getName() + " bought item " + item.getMaterial() + " in shop " + currentShop.getShop() + " for " + priceToPay.toPlainString() + " money! Stacksize: " + quantity);
        } else {
            GUIShop.sendPrefix(player, "not-enough-money", amount);
        }
    }
}
