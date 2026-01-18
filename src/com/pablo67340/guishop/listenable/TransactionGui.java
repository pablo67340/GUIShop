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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.pablo67340.guishop.util.SchedulerUtil;

import java.math.BigDecimal;
import java.util.*;

import com.pablo67340.guishop.util.StringUtil;
import com.pablo67340.guishop.util.NameUtil;
import org.bukkit.event.inventory.ClickType;

/**
 * Transaction GUI for buying and selling items.
 * Shows the item in the center with sell buttons on the left and buy buttons on the right.
 */
public class TransactionGui {

    /**
     * The item currently being targeted.
     */
    private final Item item;

    /**
     * The GUI that will be displayed.
     */
    private SimpleGui GUI;

    /**
     * The instance of the {@link Shop} that spawned this TransactionGui.
     */
    private final Shop currentShop;

    @Getter
    private final Player player;

    /**
     * Flag to track if player clicked back button (to prevent onClose from reopening shop)
     */
    private boolean clickedBack = false;

    // GUI Layout constants (3-row GUI = 27 slots)
    private static final int GUI_ROWS = 3;
    
    // Sell button slots (left side, middle row - 1 is closest to item)
    private static final int SELL_64_SLOT = 10;
    private static final int SELL_32_SLOT = 11;
    private static final int SELL_1_SLOT = 12;
    
    // Item display slot (center, middle row)
    private static final int ITEM_SLOT = 13;
    
    // Buy button slots (right side, middle row - 1 is closest to item)
    private static final int BUY_1_SLOT = 14;
    private static final int BUY_32_SLOT = 15;
    private static final int BUY_64_SLOT = 16;
    
    // Player head slot (bottom left)
    private static final int PLAYER_HEAD_SLOT = 18;
    
    // Back button slot (bottom right)
    private static final int BACK_BUTTON_SLOT = 26;
    
    // Quantity amounts - loaded from config
    private int[] getQuantities() {
        return Config.getTransactionGuiConfig().getQuantities();
    }

    public TransactionGui(Item item, Shop shop, Player player) {
        this.item = item;
        this.currentShop = shop;
        this.player = player;
    }

    /**
     * Opens the GUI for the transaction.
     */
    public void open() {
        GUI.setCloseHandler(this::onClose);
        GUI.setTopClickHandler(this::onClick);
        GUI.setBottomClickHandler(event -> event.setCancelled(true));
        GUI.show(player);
    }

    /**
     * Builds the transaction GUI.
     */
    public TransactionGui loadInventory() {
        // Get the item name for the title (use title case for material names)
        String itemName = item.hasShopName() 
            ? ChatColor.translateAlternateColorCodes('&', item.getShopName())
            : NameUtil.formatMaterialName(item.getMaterial());
        
        GUI = new SimpleGui(GUI_ROWS, ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionTitle().replace("%item%", itemName)));
        
        // Create the center item display
        ItemStack displayItem = createDisplayItem();
        GUI.setItem(ITEM_SLOT, displayItem);
        
        // Create sell buttons (left side) or "not sellable" indicator
        if (item.hasSellPrice()) {
            createSellButtons();
        } else {
            createNotSellableIndicator();
        }
        
        // Create buy buttons (right side) or "not buyable" indicator
        if (item.hasBuyPrice()) {
            createBuyButtons();
        } else {
            createNotBuyableIndicator();
        }
        
        // Add player head with balance (bottom left)
        createPlayerHead();
        
        // Add back button (bottom right)
        if (!Config.isDisableBackButton()) {
            ItemStack backButton = Config.getButtonConfig().getBackButton().toItemStack(player, false);
            // Mark as GUI element to prevent worth display
            PDCUtil.setString(backButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(BACK_BUTTON_SLOT, backButton);
        }
        
        return this;
    }

    /**
     * Creates the center display item with buy/sell price lore.
     */
    private ItemStack createDisplayItem() {
        ItemStack itemStack = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();

        if (item.hasPotion()) {
            PotionInfo pi = item.getPotionInfo();
            if (pi.getSplash()) {
                itemStack = new ItemStack(Material.SPLASH_POTION);
            }
            PotionMeta pm = (PotionMeta) itemStack.getItemMeta();
            try {
                pm.addCustomEffect(new PotionEffect(PotionEffectType.getByName(pi.getType()), 
                    pi.getUpgraded() ? 2 : 1, pi.getExtended() ? 1 : 0), pi.getSplash());
            } catch (IllegalArgumentException ignored) {}
            itemStack.setItemMeta(pm);
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = new ArrayList<>();
        
        // Add buy price lore
        lore.add(item.getBuyLore(1));
        // Add sell price lore
        lore.add(item.getSellLore(1));
        
        // Add custom shop lore if exists
        if (item.hasShopLore()) {
            item.getShopLore().forEach(str -> {
                lore.add(ChatColor.translateAlternateColorCodes('&', str));
            });
        }

        itemMeta.setLore(lore);

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
                    try {
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), 
                            Integer.parseInt(level), true);
                    } catch (Exception ignored) {}
                }
                itemStack.setItemMeta(meta);
            } else {
                for (String enc : item.getEnchantments()) {
                    String enchantment = StringUtil.substringBefore(enc, ":");
                    String level = StringUtil.substringAfter(enc, ":");
                    try {
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), 
                            Integer.parseInt(level), true);
                    } catch (Exception ignored) {}
                }
                itemStack.setItemMeta(itemMeta);
            }
        } else {
            itemStack.setItemMeta(itemMeta);
        }

        if (itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial() && item.hasSkullUUID()) {
            itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(item.getSkullUUID()), item.getSkullUUID());
        }

        if (item.hasNBT()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_CUSTOM_NBT, item.getNBT());
        }

        return itemStack;
    }

    /**
     * Creates the sell buttons using configured material.
     */
    private void createSellButtons() {
        // Order: quantity 1 goes to SELL_1_SLOT (closest to item), 32 to middle, 64 to left
        int[] sellSlots = {SELL_1_SLOT, SELL_32_SLOT, SELL_64_SLOT};
        int[] quantities = getQuantities();
        
        for (int i = 0; i < quantities.length; i++) {
            int quantity = quantities[i];
            ItemStack sellButton = Config.getTransactionGuiConfig().getSellMaterial().parseItem();
            sellButton.setAmount(quantity);
            
            ItemMeta meta = sellButton.getItemMeta();
            String sellPrice = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
                + GUIShop.getINSTANCE().getMiscUtils().economyFormat(item.calculateSellPrice(quantity))
                + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
            
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                Config.getTitlesConfig().getTransactionSellButton().replace("%amount%", String.valueOf(quantity))));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                Config.getTitlesConfig().getTransactionSellLore()
                    .replace("%amount%", String.valueOf(quantity))
                    .replace("%price%", sellPrice)));
            meta.setLore(lore);
            
            sellButton.setItemMeta(meta);
            // Mark as GUI element to prevent worth display
            PDCUtil.setString(sellButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(sellSlots[i], sellButton);
        }
    }

    /**
     * Creates the buy buttons using configured material.
     */
    private void createBuyButtons() {
        int[] buySlots = {BUY_1_SLOT, BUY_32_SLOT, BUY_64_SLOT};
        int[] quantities = getQuantities();
        
        for (int i = 0; i < quantities.length; i++) {
            int quantity = quantities[i];
            ItemStack buyButton = Config.getTransactionGuiConfig().getBuyMaterial().parseItem();
            buyButton.setAmount(quantity);
            
            ItemMeta meta = buyButton.getItemMeta();
            String buyPrice = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
                + GUIShop.getINSTANCE().getMiscUtils().economyFormat(item.calculateBuyPrice(quantity))
                + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
            
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                Config.getTitlesConfig().getTransactionBuyButton().replace("%amount%", String.valueOf(quantity))));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                Config.getTitlesConfig().getTransactionBuyLore()
                    .replace("%amount%", String.valueOf(quantity))
                    .replace("%price%", buyPrice)));
            meta.setLore(lore);
            
            buyButton.setItemMeta(meta);
            // Mark as GUI element to prevent worth display
            PDCUtil.setString(buyButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(buySlots[i], buyButton);
        }
    }

    /**
     * Creates an indicator showing the item is not sellable.
     */
    private void createNotSellableIndicator() {
        ItemStack notSellable = Config.getTransactionGuiConfig().getNotSellableMaterial().parseItem();
        ItemMeta meta = notSellable.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionNotSellable()));
        notSellable.setItemMeta(meta);
        // Mark as GUI element to prevent worth display
        PDCUtil.setString(notSellable, PDCUtil.KEY_GUI_ELEMENT, "true");
        
        // Place in the center of the sell area
        GUI.setItem(SELL_32_SLOT, notSellable);
    }

    /**
     * Creates an indicator showing the item is not buyable.
     */
    private void createNotBuyableIndicator() {
        ItemStack notBuyable = Config.getTransactionGuiConfig().getNotBuyableMaterial().parseItem();
        ItemMeta meta = notBuyable.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionNotBuyable()));
        notBuyable.setItemMeta(meta);
        // Mark as GUI element to prevent worth display
        PDCUtil.setString(notBuyable, PDCUtil.KEY_GUI_ELEMENT, "true");
        
        // Place in the center of the buy area
        GUI.setItem(BUY_32_SLOT, notBuyable);
    }

    /**
     * Creates the player head showing their balance.
     */
    private void createPlayerHead() {
        ItemStack playerHead = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        skullMeta.setOwningPlayer(player);
        
        // Get player balance
        double balance = GUIShop.getINSTANCE().getMiscUtils().getECONOMY().getBalance(player);
        String formattedBalance = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
            + GUIShop.getINSTANCE().getMiscUtils().economyFormat(BigDecimal.valueOf(balance))
            + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
        
        skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionBalanceTitle().replace("%player%", player.getName())));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', 
            Config.getTitlesConfig().getTransactionBalanceLore().replace("%balance%", formattedBalance)));
        skullMeta.setLore(lore);
        
        playerHead.setItemMeta(skullMeta);
        // Mark as GUI element to prevent worth display
        PDCUtil.setString(playerHead, PDCUtil.KEY_GUI_ELEMENT, "true");
        GUI.setItem(PLAYER_HEAD_SLOT, playerHead);
    }

    /**
     * Handles click events in the transaction GUI.
     */
    private void onClick(InventoryClickEvent e) {
        e.setCancelled(true);

        // Block off-hand swap
        if (e.getClick() == ClickType.valueOf("SWAP_OFFHAND")) {
            return;
        }

        int slot = e.getSlot();

        // Back button
        if (!Config.isDisableBackButton() && slot == BACK_BUTTON_SLOT) {
            clickedBack = true;
            currentShop.open(player);
            return;
        }

        // Sell buttons
        if (item.hasSellPrice()) {
            if (slot == SELL_1_SLOT) {
                sell(1);
                return;
            } else if (slot == SELL_32_SLOT) {
                sell(32);
                return;
            } else if (slot == SELL_64_SLOT) {
                sell(64);
                return;
            }
        }

        // Buy buttons
        if (item.hasBuyPrice()) {
            if (slot == BUY_1_SLOT) {
                buy(1);
                return;
            } else if (slot == BUY_32_SLOT) {
                buy(32);
                return;
            } else if (slot == BUY_64_SLOT) {
                buy(64);
                return;
            }
        }
    }

    /**
     * Handles inventory close.
     */
    private void onClose(InventoryCloseEvent e) {
        if (clickedBack) {
            clickedBack = false;
            return;
        }
        
        if ((!Config.isDisableEscapeBack() || !Config.isDisableEscapeBackQuantity()) && !GUIShop.getINSTANCE().isReload) {
            SchedulerUtil.runAtEntityLater(player, () -> currentShop.open(player), 1L);
        }
    }

    /**
     * Sells the specified quantity of items.
     */
    private void sell(int quantity) {
        if (!GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.sell")) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-permission");
            return;
        }

        int amountRemoved = 0;
        
        // Find and remove matching items from player inventory
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && amountRemoved < quantity; i++) {
            ItemStack invItem = contents[i];
            if (invItem == null || invItem.getType().isAir()) {
                continue;
            }
            
            if (item.isItemFromItemStack(invItem)) {
                int canTake = Math.min(invItem.getAmount(), quantity - amountRemoved);
                amountRemoved += canTake;
                
                if (canTake >= invItem.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    invItem.setAmount(invItem.getAmount() - canTake);
                }
            }
        }
        
        if (amountRemoved > 0) {
            BigDecimal moneyToGive = item.calculateSellPrice(amountRemoved);
            Sell.roundAndGiveMoney(player, moneyToGive);
            
            // Update dynamic pricing
            if (item.hasBuyPrice() && Config.isDynamicPricing() && item.isUseDynamicPricing()) {
                GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().sellItem(item.getItemString(), amountRemoved);
            }
            
            // Track statistics
            StatisticsManager statsManager = StatisticsManager.getInstance();
            if (statsManager != null && statsManager.isAvailable()) {
                statsManager.recordSale(player, item.getMaterial(), amountRemoved, moneyToGive);
            }
            
            GUIShop.getINSTANCE().getLogUtil().transactionLog(
                "Player " + player.getName() + " sold " + amountRemoved + " " + item.getMaterial() + 
                " for " + moneyToGive.toPlainString());
            
            // Play sound
            if (Config.isSoundEnabled()) {
                try {
                    player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
                } catch (Exception ignored) {}
            }
            
            // Refresh the GUI to update player balance display
            refreshPlayerHead();
        } else {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "alt-sell-not-enough", quantity);
        }
    }

    /**
     * Buys the specified quantity of items.
     */
    private void buy(int quantity) {
        if (!item.hasBuyPrice()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "cant-buy");
            return;
        }
        
        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "full-inventory");
            return;
        }

        // Check max stack size
        int maxStackSize = 64;
        try {
            Optional<XMaterial> material = XMaterial.matchXMaterial(item.getMaterial());
            if (material.isPresent() && material.get().parseMaterial().getMaxStackSize() < quantity) {
                maxStackSize = material.get().parseMaterial().getMaxStackSize();
                GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "too-high-quantity", maxStackSize);
                return;
            }
        } catch (NoSuchElementException | NullPointerException ignored) {}

        BigDecimal priceToPay;
        Runnable dynamicPricingUpdate = null;

        // Calculate price with dynamic pricing if enabled
        if (Config.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {
            String itemString = item.getItemString();
            int finalQuantity = quantity;
            dynamicPricingUpdate = () -> GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().buyItem(itemString, finalQuantity);
            priceToPay = GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().calculateBuyPrice(
                itemString, quantity, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
        } else {
            priceToPay = item.getBuyPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
        }

        String currencyPrefix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix");
        String currencySuffix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
        String amount = currencyPrefix + priceToPay + currencySuffix;

        // Process transaction
        if (GUIShop.getINSTANCE().getMiscUtils().getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
            if (Config.isSoundEnabled()) {
                try {
                    player.playSound(player.getLocation(), XSound.matchXSound(Config.getSound()).get().parseSound(), 1, 1);
                } catch (Exception ignored) {}
            }

            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "purchase", amount);

            if (dynamicPricingUpdate != null) {
                dynamicPricingUpdate.run();
            }

            ItemStack purchasedItem = item.toBuyItemStack(quantity, player, currentShop);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(purchasedItem);
            
            // Handle overflow
            if (!overflow.isEmpty()) {
                int itemsNotAdded = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
                if (itemsNotAdded > 0 && itemsNotAdded < quantity) {
                    BigDecimal refundPerItem = item.getBuyPriceAsDecimal();
                    BigDecimal refundAmount = refundPerItem.multiply(BigDecimal.valueOf(itemsNotAdded));
                    GUIShop.getINSTANCE().getMiscUtils().getECONOMY().depositPlayer(player, refundAmount.doubleValue());
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "partial-purchase", quantity - itemsNotAdded);
                } else if (itemsNotAdded == quantity) {
                    GUIShop.getINSTANCE().getMiscUtils().getECONOMY().depositPlayer(player, priceToPay.doubleValue());
                    GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "full-inventory");
                    return;
                }
                for (ItemStack overflowItem : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                }
            }

            GUIShop.getINSTANCE().getLogUtil().transactionLog(
                "Player " + player.getName() + " bought " + quantity + " " + item.getMaterial() + 
                " in shop " + currentShop.getShop() + " for " + priceToPay.toPlainString());
            
            // Track statistics
            StatisticsManager statsManager = StatisticsManager.getInstance();
            if (statsManager != null && statsManager.isAvailable()) {
                statsManager.recordPurchase(player, item.getMaterial(), quantity, priceToPay);
            }
            
            // Refresh the GUI to update player balance display
            refreshPlayerHead();
        } else {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "not-enough-money", amount);
        }
    }

    /**
     * Refreshes the player head to show updated balance.
     */
    private void refreshPlayerHead() {
        createPlayerHead();
        GUI.update();
    }
}

