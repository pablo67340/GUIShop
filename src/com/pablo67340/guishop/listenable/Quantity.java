package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.gui.SimpleGui;
import com.pablo67340.guishop.statistics.StatisticsManager;
import com.pablo67340.guishop.util.PDCUtil;
import com.pablo67340.guishop.util.SkullCreator;
import lombok.Getter;
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

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.math.BigDecimal;
import java.util.*;

import com.pablo67340.guishop.util.StringUtil;
import org.bukkit.event.inventory.ClickType;

class Quantity {

    /**
     * The item currently being targetted.
     */
    private final Item item;

    /**
     * The GUI that will be displayed.
     */
    private SimpleGui GUI;

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

    /**
     * Flag to track if player clicked back button (to prevent onClose from reopening shop)
     */
    private boolean clickedBack = false;

    Quantity(Item item, Shop shop, Player input) {
        this.item = item;
        this.currentShop = shop;
        this.player = input;
    }

    /**
     * Opens the GUI to sell the items in.
     */
    void open() {
        GUI.setCloseHandler(this::onClose);
        GUI.setTopClickHandler(this::onQuantityClick);
        GUI.setBottomClickHandler(event -> event.setCancelled(true));
        GUI.show(player);
    }

    /**
     * Preloads the inventory to display items.
     */
    public Quantity loadInventory() {
        GUI = new SimpleGui(5, Config.getTitlesConfig().getQtyTitle());
        int multiplier = 1;

        for (int x = 19; x <= 25; x++) {
            ItemStack itemStack = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();

            if (item.hasPotion()) {
                PotionInfo pi = item.getPotionInfo();

                if (pi.getSplash()) {
                    itemStack = new ItemStack(Material.SPLASH_POTION);
                }
                PotionMeta pm = (PotionMeta) itemStack.getItemMeta();

                try {
                    pm.addCustomEffect(new PotionEffect(PotionEffectType.getByName(pi.getType()), pi.getUpgraded() ? 2 : 1, pi.getExtended() ? 1 : 0), pi.getSplash());
                } catch (IllegalArgumentException ex) {
                    if (ex.getMessage().contains("upgradable")) {
                        GUIShop.getINSTANCE().getLogUtil().log("Potion: " + pi.getType() + " Is not upgradable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                        pi.setUpgraded(false);
                        pm.addCustomEffect(new PotionEffect(PotionEffectType.getByName(pi.getType()), pi.getUpgraded() ? 2 : 1, pi.getExtended() ? 1 : 0), pi.getSplash());
                    } else if (ex.getMessage().contains("extended")) {
                        GUIShop.getINSTANCE().getLogUtil().log("Potion: " + pi.getType() + " Is not extendable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                        pi.setExtended(false);
                        pm.addCustomEffect(new PotionEffect(PotionEffectType.getByName(pi.getType()), pi.getUpgraded() ? 2 : 1, pi.getExtended() ? 1 : 0), pi.getSplash());
                    }
                }
                itemStack.setItemMeta(pm);

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
                        String enchantment = StringUtil.substringBefore(enc, ":");
                        String level = StringUtil.substringAfter(enc, ":");
                        assert meta != null;
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(meta);
                    }
                } else {
                    for (String enc : item.getEnchantments()) {
                        String enchantment = StringUtil.substringBefore(enc, ":");
                        String level = StringUtil.substringAfter(enc, ":");
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
                // Store custom-nbt value in PDC for reference
                PDCUtil.setString(itemStack, PDCUtil.KEY_CUSTOM_NBT, item.getNBT());
            }

            GUI.setItem(x, itemStack);
            qty.put(x, multiplier);
            multiplier *= 2;
        }

        if (!Config.isDisableBackButton()) {
            GUI.setItem(44, Config.getButtonConfig().getBackButton().toItemStack(player, false));
        }

        return this;
    }

    /**
     * Executes when an item is clicked inside the Quantity Inventory.
     */
    private void onQuantityClick(InventoryClickEvent e) {
        e.setCancelled(true);

        // Block off-hand swap
        if (e.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            return;
        }

        if (!Config.isDisableBackButton()) {
            if (e.getSlot() == 44) {
                // Set flag to prevent onClose from also opening shop
                clickedBack = true;
                // Open shop directly - openInventory() will close current inventory
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
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "full-inventory");
            return;
        }

        buy(item, qty.get(e.getSlot()));
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        // Don't reopen shop if player clicked the back button (we handle that separately)
        if (clickedBack) {
            clickedBack = false;
            return;
        }
        
        if ((!Config.isDisableEscapeBack() || !Config.isDisableEscapeBackQuantity()) && !GUIShop.getINSTANCE().isReload) {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.scheduleSyncDelayedTask(GUIShop.getINSTANCE(), () -> currentShop.open(player), 1L);
        }
    }

    public void buy(Item item, int quantity) {
        if (!item.hasBuyPrice()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "cant-buy");
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
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "too-high-quantity", maxStackSize);
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
            dynamicPricingUpdate = () -> GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().buyItem(itemString, finalQuantity);

            priceToPay = GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().calculateBuyPrice(itemString, quantity, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
        }

        priceToPay = priceToPay.subtract(BigDecimal.valueOf(priceToReimburse));

        String currencyPrefix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix");
        String currencySuffix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
        String amount = currencyPrefix + priceToPay + currencySuffix;

        // Check if the transition was successful
        if (GUIShop.getINSTANCE().getMiscUtils().getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            // If the player has the sound enabled, play it
            if (Config.isSoundEnabled()) {
                player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
            }

            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "purchase", amount);

            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            player.getInventory().addItem(item.toBuyItemStack(quantity, player, currentShop));

            GUIShop.getINSTANCE().getLogUtil().transactionLog(
                    "Player " + player.getName() + " bought item " + item.getMaterial() + " in shop " + currentShop.getShop() + " for " + priceToPay.toPlainString() + " money! Stacksize: " + quantity);
            
            // Track purchase statistics
            StatisticsManager statsManager = StatisticsManager.getInstance();
            if (statsManager != null && statsManager.isAvailable()) {
                statsManager.recordPurchase(player, item.getMaterial(), quantity, priceToPay);
            }
        } else {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "not-enough-money", amount);
        }
    }
}
