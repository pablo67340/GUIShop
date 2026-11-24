package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
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
    
    /**
     * The page index the player was on when opening this TransactionGui.
     * Used to return to the same page when going back.
     */
    private final int returnPage;

    // Dynamic slot positions loaded from config
    private int guiRows = 3;
    private int sellSlot1 = -1, sellSlot2 = -1, sellSlot3 = -1;
    private int buySlot1 = -1, buySlot2 = -1, buySlot3 = -1;
    private int itemDisplaySlot = -1;
    private int playerHeadSlot = -1;
    private int backButtonSlot = -1;
    
    // Quantity amounts - intelligently scaled based on item's max stack size
    private int[] getQuantities() {
        int maxStackSize = getItemMaxStackSize();
        
        if (maxStackSize >= 64) {
            // Standard stackable items: 1, 32, 64
            return Config.getTransactionGuiConfig().getQuantities();
        } else if (maxStackSize == 1) {
            // Unstackable items (tools, armor): 1, 1, 1
            return new int[]{1, 1, 1};
        } else {
            // Items with custom max stack sizes (ender pearls=16, eggs=16, etc.)
            // Scale: 1, half, max
            int half = Math.max(1, maxStackSize / 2);
            return new int[]{1, half, maxStackSize};
        }
    }
    
    /**
     * Gets the max stack size for the current item being transacted.
     */
    private int getItemMaxStackSize() {
        if (item == null || item.getMaterial() == null) {
            return 64;
        }
        try {
            Optional<XMaterial> material = XMaterial.matchXMaterial(item.getMaterial());
            if (material.isPresent() && material.get().parseMaterial() != null) {
                return material.get().parseMaterial().getMaxStackSize();
            }
        } catch (Exception e) {
            // Fall back to default
        }
        return 64;
    }
    
    /**
     * Loads slot positions from transaction.yml layout configuration.
     */
    private void loadSlotPositions() {
        org.bukkit.configuration.file.FileConfiguration config = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        
        guiRows = config.getInt("rows", 3);
        
        org.bukkit.configuration.ConfigurationSection layout = config.getConfigurationSection("layout");
        if (layout == null) {
            // Use defaults if no layout configured
            sellSlot3 = 10; sellSlot2 = 11; sellSlot1 = 12;
            itemDisplaySlot = 13;
            buySlot1 = 14; buySlot2 = 15; buySlot3 = 16;
            playerHeadSlot = 18;
            backButtonSlot = 26;
            return;
        }
        
        for (String slotKey : layout.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(slotKey);
            } catch (NumberFormatException e) {
                continue;
            }
            
            String type = layout.getString(slotKey + ".type", "DUMMY");
            switch (type.toUpperCase()) {
                case "ITEM_DISPLAY" -> itemDisplaySlot = slot;
                case "BUY_1" -> buySlot1 = slot;
                case "BUY_2" -> buySlot2 = slot;
                case "BUY_3" -> buySlot3 = slot;
                case "SELL_1" -> sellSlot1 = slot;
                case "SELL_2" -> sellSlot2 = slot;
                case "SELL_3" -> sellSlot3 = slot;
                case "BACK" -> backButtonSlot = slot;
                case "PLAYER_HEAD" -> playerHeadSlot = slot;
            }
        }
    }

    public TransactionGui(Item item, Shop shop, Player player) {
        this.item = item;
        this.currentShop = shop;
        this.player = player;
        this.returnPage = shop.getCurrentPage();
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
        // Load slot positions from config
        loadSlotPositions();
        
        // Get the item name for the title (use title case for material names)
        String itemName = item.hasShopName() 
            ? ChatColor.translateAlternateColorCodes('&', item.getShopName())
            : NameUtil.formatMaterialName(item.getMaterial());
        
        // Get title from transaction.yml or fall back to titles config
        org.bukkit.configuration.file.FileConfiguration transConfig = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        String title = transConfig.getString("title", Config.getTitlesConfig().getTransactionTitle());
        title = title.replace("%item%", itemName);
        
        GUI = new SimpleGui(guiRows, ChatColor.translateAlternateColorCodes('&', title));
        
        // Load DUMMY decorations from layout
        loadDummyItems();
        
        // Create the center item display
        if (itemDisplaySlot >= 0) {
            ItemStack displayItem = createDisplayItem();
            GUI.setItem(itemDisplaySlot, displayItem);
        }
        
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
        
        // Add player head with balance
        if (playerHeadSlot >= 0) {
            createPlayerHead();
        }
        
        // Add back button
        if (backButtonSlot >= 0) {
            ItemStack backButton = createBackButton();
            GUI.setItem(backButtonSlot, backButton);
        }
        
        return this;
    }
    
    /**
     * Loads DUMMY decoration items from the layout configuration.
     */
    private void loadDummyItems() {
        org.bukkit.configuration.file.FileConfiguration config = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        org.bukkit.configuration.ConfigurationSection layout = config.getConfigurationSection("layout");
        
        if (layout == null) return;
        
        for (String slotKey : layout.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(slotKey);
            } catch (NumberFormatException e) {
                continue;
            }
            
            String type = layout.getString(slotKey + ".type", "DUMMY");
            if (!"DUMMY".equalsIgnoreCase(type)) continue;
            
            String materialStr = layout.getString(slotKey + ".id", "BLACK_STAINED_GLASS_PANE");
            String name = layout.getString(slotKey + ".name", " ");
            List<String> lore = layout.getStringList(slotKey + ".lore");
            
            XMaterial xMat = XMaterial.matchXMaterial(materialStr).orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
            ItemStack dummyItem = xMat.parseItem();
            ItemMeta meta = dummyItem.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            dummyItem.setItemMeta(meta);
            
            // Mark as GUI element to prevent worth display
            PDCUtil.setString(dummyItem, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(slot, dummyItem);
        }
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
        // Load button display config from transaction.yml
        org.bukkit.configuration.file.FileConfiguration transConfig = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        String buttonName = transConfig.getString("buttons.sell.name", 
            Config.getTitlesConfig().getTransactionSellButton());
        List<String> buttonLoreTemplate = transConfig.getStringList("buttons.sell.lore");
        if (buttonLoreTemplate.isEmpty()) {
            buttonLoreTemplate = List.of(Config.getTitlesConfig().getTransactionSellLore());
        }
        
        int[] sellSlots = {sellSlot1, sellSlot2, sellSlot3};
        int[] quantities = getQuantities();
        
        for (int i = 0; i < quantities.length; i++) {
            int slot = sellSlots[i];
            if (slot < 0) continue; // Skip if slot not configured
            
            int quantity = quantities[i];
            ItemStack sellButton = Config.getTransactionGuiConfig().getSellMaterial().parseItem();
            sellButton.setAmount(Math.min(quantity, 64)); // Cap visual amount at 64
            
            ItemMeta meta = sellButton.getItemMeta();
            String sellPrice = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
                + GUIShop.getINSTANCE().getMiscUtils().economyFormat(item.calculateSellPrice(quantity))
                + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
            
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                buttonName.replace("%amount%", String.valueOf(quantity))));
            
            List<String> lore = new ArrayList<>();
            for (String line : buttonLoreTemplate) {
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    line.replace("%amount%", String.valueOf(quantity))
                        .replace("%price%", sellPrice)));
            }
            meta.setLore(lore);
            
            sellButton.setItemMeta(meta);
            // Mark as GUI element to prevent worth display
            PDCUtil.setString(sellButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(slot, sellButton);
        }
    }

    /**
     * Creates the buy buttons using configured material.
     */
    private void createBuyButtons() {
        // Load button display config from transaction.yml
        org.bukkit.configuration.file.FileConfiguration transConfig = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        String buttonName = transConfig.getString("buttons.buy.name", 
            Config.getTitlesConfig().getTransactionBuyButton());
        List<String> buttonLoreTemplate = transConfig.getStringList("buttons.buy.lore");
        if (buttonLoreTemplate.isEmpty()) {
            buttonLoreTemplate = List.of(Config.getTitlesConfig().getTransactionBuyLore());
        }
        
        int[] buySlots = {buySlot1, buySlot2, buySlot3};
        int[] quantities = getQuantities();
        
        for (int i = 0; i < quantities.length; i++) {
            int slot = buySlots[i];
            if (slot < 0) continue; // Skip if slot not configured
            
            int quantity = quantities[i];
            ItemStack buyButton = Config.getTransactionGuiConfig().getBuyMaterial().parseItem();
            buyButton.setAmount(Math.min(quantity, 64)); // Cap visual amount at 64
            
            ItemMeta meta = buyButton.getItemMeta();
            String buyPrice = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
                + GUIShop.getINSTANCE().getMiscUtils().economyFormat(item.calculateBuyPrice(quantity))
                + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix");
            
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                buttonName.replace("%amount%", String.valueOf(quantity))));
            
            List<String> lore = new ArrayList<>();
            for (String line : buttonLoreTemplate) {
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    line.replace("%amount%", String.valueOf(quantity))
                        .replace("%price%", buyPrice)));
            }
            meta.setLore(lore);
            
            buyButton.setItemMeta(meta);
            // Mark as GUI element to prevent worth display
            PDCUtil.setString(buyButton, PDCUtil.KEY_GUI_ELEMENT, "true");
            GUI.setItem(slot, buyButton);
        }
    }

    /**
     * Creates an indicator showing the item is not sellable.
     */
    private void createNotSellableIndicator() {
        // Load display config from transaction.yml
        org.bukkit.configuration.file.FileConfiguration transConfig = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        String indicatorName = transConfig.getString("buttons.not-sellable.name", 
            Config.getTitlesConfig().getTransactionNotSellable());
        List<String> indicatorLore = transConfig.getStringList("buttons.not-sellable.lore");
        
        ItemStack notSellable = Config.getTransactionGuiConfig().getNotSellableMaterial().parseItem();
        ItemMeta meta = notSellable.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', indicatorName));
        if (!indicatorLore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : indicatorLore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }
        notSellable.setItemMeta(meta);
        // Mark as GUI element to prevent worth display
        PDCUtil.setString(notSellable, PDCUtil.KEY_GUI_ELEMENT, "true");
        
        // Place in configured sell slots (use slot 2 as center preference)
        if (sellSlot2 >= 0) GUI.setItem(sellSlot2, notSellable);
        else if (sellSlot1 >= 0) GUI.setItem(sellSlot1, notSellable);
        else if (sellSlot3 >= 0) GUI.setItem(sellSlot3, notSellable);
    }

    /**
     * Creates an indicator showing the item is not buyable.
     */
    private void createNotBuyableIndicator() {
        // Load display config from transaction.yml
        org.bukkit.configuration.file.FileConfiguration transConfig = 
            GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        String indicatorName = transConfig.getString("buttons.not-buyable.name", 
            Config.getTitlesConfig().getTransactionNotBuyable());
        List<String> indicatorLore = transConfig.getStringList("buttons.not-buyable.lore");
        
        ItemStack notBuyable = Config.getTransactionGuiConfig().getNotBuyableMaterial().parseItem();
        ItemMeta meta = notBuyable.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', indicatorName));
        if (!indicatorLore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : indicatorLore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }
        notBuyable.setItemMeta(meta);
        // Mark as GUI element to prevent worth display
        PDCUtil.setString(notBuyable, PDCUtil.KEY_GUI_ELEMENT, "true");
        
        // Place in configured buy slots (use slot 2 as center preference)
        if (buySlot2 >= 0) GUI.setItem(buySlot2, notBuyable);
        else if (buySlot1 >= 0) GUI.setItem(buySlot1, notBuyable);
        else if (buySlot3 >= 0) GUI.setItem(buySlot3, notBuyable);
    }

    /**
     * Creates the player head showing their balance.
     */
    private void createPlayerHead() {
        if (playerHeadSlot < 0) return;
        
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
        GUI.setItem(playerHeadSlot, playerHead);
    }

    /**
     * Creates the back button (red glass pane) for returning to shop.
     */
    private ItemStack createBackButton() {
        ItemStack backButton = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lBack"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Return to shop"));
        meta.setLore(lore);
        // Set PDC to mark as BACK type
        meta.getPersistentDataContainer().set(PDCUtil.KEY_GUI_ELEMENT, 
            org.bukkit.persistence.PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(PDCUtil.KEY_ITEM_TYPE, 
            org.bukkit.persistence.PersistentDataType.STRING, ItemType.BACK.name());
        backButton.setItemMeta(meta);
        return backButton;
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
        int[] quantities = getQuantities();

        // Back button
        if (slot == backButtonSlot && backButtonSlot >= 0) {
            clickedBack = true;
            // Efficiently refresh only dynamic pricing lore without full reload
            currentShop.refreshDynamicPrices();
            currentShop.openAtPage(player, returnPage);
            return;
        }

        // Sell buttons
        if (item.hasSellPrice()) {
            if (slot == sellSlot1) {
                sell(quantities[0]);
                return;
            } else if (slot == sellSlot2) {
                sell(quantities[1]);
                return;
            } else if (slot == sellSlot3) {
                sell(quantities[2]);
                return;
            }
        }

        // Buy buttons
        if (item.hasBuyPrice()) {
            if (slot == buySlot1) {
                buy(quantities[0]);
                return;
            } else if (slot == buySlot2) {
                buy(quantities[1]);
                return;
            } else if (slot == buySlot3) {
                buy(quantities[2]);
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
            SchedulerUtil.runAtEntityLater(player, () -> {
                // Efficiently refresh only dynamic pricing lore without full reload
                currentShop.refreshDynamicPrices();
                currentShop.openAtPage(player, returnPage);
            }, 1L);
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
            if (item.hasBuyPrice() && item.shouldUseDynamicPricing()) {
                com.pablo67340.guishop.api.DynamicPriceProvider dynamicProvider = 
                    GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING();
                if (dynamicProvider != null) {
                    dynamicProvider.sellItem(item.getItemString(), amountRemoved);
                }
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
            
            // Refresh the GUI to update balance and prices (dynamic pricing)
            refreshGui();
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
        com.pablo67340.guishop.api.DynamicPriceProvider dynamicProvider = 
            item.shouldUseDynamicPricing() ? GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING() : null;
        
        if (dynamicProvider != null && item.hasSellPrice()) {
            String itemString = item.getItemString();
            int finalQuantity = quantity;
            dynamicPricingUpdate = () -> dynamicProvider.buyItem(itemString, finalQuantity);
            priceToPay = dynamicProvider.calculateBuyPrice(
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
            
            // Refresh the GUI to update balance and prices (dynamic pricing)
            refreshGui();
        } else {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "not-enough-money", amount);
        }
    }

    /**
     * Refreshes the player head to show updated balance.
     */
    private void refreshPlayerHead() {
        createPlayerHead();
    }
    
    /**
     * Refreshes the buy/sell buttons to show updated dynamic prices.
     * Called after each transaction when dynamic pricing is enabled.
     */
    private void refreshPriceButtons() {
        // Refresh sell buttons with new prices
        if (item.hasSellPrice()) {
            createSellButtons();
        }
        
        // Refresh buy buttons with new prices
        if (item.hasBuyPrice()) {
            createBuyButtons();
        }
        
        // Also refresh the center display item as it shows buy/sell prices
        if (itemDisplaySlot >= 0) {
            ItemStack displayItem = createDisplayItem();
            GUI.setItem(itemDisplaySlot, displayItem);
        }
    }
    
    /**
     * Refreshes the entire transaction GUI after a transaction.
     * Updates balance, prices, and display item.
     */
    private void refreshGui() {
        refreshPlayerHead();
        refreshPriceButtons();
        GUI.update();
    }
}

