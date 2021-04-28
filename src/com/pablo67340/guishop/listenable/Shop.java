package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.INBTBase;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NbtParser;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.definition.ShopItem;
import com.pablo67340.guishop.definition.ShopPage;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.SkullCreator;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class Shop {

    /**
     * The name of this {@link Shop}.
     */
    @Getter
    @Setter
    private String shop, title;

    /**
     * The list of {@link Page}'s in this {@link Shop}.
     */
    private Gui GUI;
    
    private final Menu menuInstance;
    
    private Boolean hasClicked = false;
    
    private PaginatedPane currentPane;
    
    private ShopItem shopItem;
    
    private ShopPane shopPage = new ShopPane(9, 6);
    
    private final List<Integer> blacklistedSlots = new ArrayList<>(Arrays.asList(53, 52, 50, 49, 48, 46, 45));
    
    private final Player player;
    
    private Boolean shopMissing = false;
    
    private Integer pageIndex = 0;

    /**
     * The constructor for a {@link Shop}.
     *
     * @param player Thep layer using the shop.
     * @param shop The name of the shop.
     * @param menuInstance The instance of the menu that opened this shop.
     */
    public Shop(Player player, String shop, Menu menuInstance) {
        this.shop = shop;
        this.menuInstance = menuInstance;
        this.player = player;
    }
    
    public Shop(String shop) {
        this.shop = shop;
        this.menuInstance = null;
        this.player = null;
    }

    /**
     * Load the specified shop
     *
     * @param preLoad True/False if the GUI should load after items are loaded.
     */
    public void loadItems(Boolean preLoad) {
        if (shop.equalsIgnoreCase("NONE")) {
            return;
        }
        if (!Main.getINSTANCE().getLoadedShops().containsKey(shop)) {
            this.setTitle(Main.getINSTANCE().getShopConfig().getString(shop + ".title"));
            shopItem = new ShopItem();
            ConfigurationSection config = Main.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages");
            Main.debugLog("Loading items for shop: " + shop);
            
            if (config == null) {
                Main.log("Check shops.yml for shop " + shop + ". It was not found.");
                if (this.player != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCheck shops.yml for shop " + shop + ". It was not found."));
                }
                shopMissing = true;
            } else {
                config.getKeys(false).stream().map(str -> {
                    ShopPage page = new ShopPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    Main.debugLog("Reading Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        Main.debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        Item item = Item.deserialize(section.getValues(true), Integer.parseInt(key), shop);
                        return item;
                    }).forEachOrdered(item -> {
                        if (!Main.getINSTANCE().getITEMTABLE().containsKey(item.getMaterial())) {
                            Main.getINSTANCE().getITEMTABLE().put(item.getMaterial(), item);
                        }
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    Main.debugLog("Adding page: " + "Page" + Integer.toString(shopItem.getPages().size()) + " to pages.");
                    shopItem.getPages().put("Page" + Integer.toString(shopItem.getPages().size()), page);
                });
                Main.debugLog("Shop items added to loaded shops");
                Main.getINSTANCE().getLoadedShops().put(shop, shopItem);
                if (!preLoad) {
                    loadShop();
                }
            }
        } else {
            shopItem = (ShopItem) Main.getINSTANCE().getLoadedShops().get(shop);
            this.setTitle(Main.getINSTANCE().getShopConfig().getString(shop + ".title"));
            //Re-Check for preload here in case they have multiple item's leading to one shop.
            if (!preLoad) {
                loadShop();
            }
        }
    }
    
    private ItemStack itemStack;
    
    private void loadShop() {
        this.GUI = new Gui(Main.getINSTANCE(), 6,
                ChatColor.translateAlternateColorCodes('&', ConfigUtil.getShopTitle().replace("{shopname}", title)));
        PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
        Collection<ShopPage> shopPages = shopItem.getPages().values();
        for (ShopPage page : shopPages) {
            shopPage = new ShopPane(9, 6);
            for (Item item : page.getItems().values()) {
                try {
                    itemStack = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();
                } catch (NoSuchElementException ex) {
                    item.setResolveFailed(true);
                }
                
                Main.debugLog("Adding item to slot: " + item.getSlot());
                if (itemStack == null || itemStack.getType() == null || item.isResolveFailed()) {
                    Main.log("Item: " + item.getMaterial() + " could not be resolved (invalid material). Are you using an old server version?");
                    shopPage.addBrokenItem("&cItem Material Not Found", item.getSlot());
                    item.setResolveFailed(true);
                    continue;
                }

                // Checks if an item is either a shop item or command item. This also handles
                // Null items as there is a item type switch in the lines above.
                if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
                    
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    
                    if (itemMeta == null) {
                        Main.log("Item: " + item.getMaterial() + " could not be resolved (null meta).");
                        shopPage.addBrokenItem("&cItem Material Not Found", item.getSlot());
                        continue;
                    }
                    
                    List<String> itemLore = new ArrayList<>();
                    
                    itemLore.add(item.getBuyLore(1));
                    
                    itemLore.add(item.getSellLore(1));
                    
                    if (player != null) {
                        if (!Main.getCREATOR().contains(player.getName())) {
                            if (item.hasShopName()) {
                                assert itemMeta != null;
                                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getShopName()));
                            } else if (item.isMobSpawner()) {
                                String mobName = item.getMobType();
                                mobName = mobName.toLowerCase();
                                mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
                                assert itemMeta != null;
                                itemMeta.setDisplayName(mobName + " Spawner");
                            }
                            if (item.hasShopLore()) {
                                item.getShopLore().forEach(str -> {
                                    if (!itemLore.contains(str) && !itemLore.contains(ConfigUtil.getBuyLore().replace("{AMOUNT}", item.calculateBuyPrice(1).toPlainString()))) {
                                        itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                                    }
                                });
                            }
                        } else {
                            itemLore.add(" ");
                            itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fItem Type: &r" + item.getItemType().toString()));
                            if (item.hasShopName()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fShop Name: &r" + item.getShopName()));
                            }
                            if (item.hasMobType()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fMob Type: &r" + item.getMobType()));
                            }
                            if (item.hasBuyName()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fBuy Name: &r" + item.getBuyName()));
                            }
                            if (item.hasBuyLore()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fBuy Lore: &r"));
                                item.getBuyLore().forEach(str -> {
                                    itemLore.add(str);
                                });
                            }
                            if (item.hasShopLore()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fShop Lore: &r"));
                                item.getShopLore().forEach(str -> {
                                    itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                                });
                            }
                            if (item.hasCommands()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fCommands: "));
                                item.getCommands().forEach(str -> {
                                    if (str.length() > 20) {
                                        String s = ChatColor.translateAlternateColorCodes('&', "/" + str);
                                        s = s.substring(0, Math.min(s.length(), 20));
                                        itemLore.add(s + "...");
                                    } else {
                                        itemLore.add("/" + str);
                                    }
                                });
                            }
                            if (item.hasEnchantments()) {
                                String enchantments = "";
                                for (String str : item.getEnchantments()) {
                                    enchantments += str + " ";
                                }
                                itemLore.add("Enchantments: " + enchantments.trim());
                            }
                        }
                    }
                    
                    if (!itemLore.isEmpty()) {
                        assert itemMeta != null;
                        itemMeta.setLore(itemLore);
                    }
                    
                    if (item.hasItemFlags()) {
                        itemMeta.addItemFlags((ItemFlag[]) item.getItemFlags().toArray());
                    }
                    if (item.hasCustomModelID()) {
                        itemMeta.setCustomModelData(item.getCustomModelData());
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
                            if (itemStack == null) {
                                Main.log("Error Parsing Custom NBT for Item: " + item.getMaterial() + " in Shop: " + shop + ". Please fix or remove custom-nbt value.");
                                shopPage.addBrokenItem("&cInvalid or Unsupported NBT", item.getSlot());
                                item.setResolveFailed(true);
                                continue;
                            }
                            
                        } catch (NbtParser.NbtParseException ex) {
                            Main.log("Error Parsing Custom NBT for Item: " + item.getMaterial() + " in Shop: " + shop + ". Please fix or remove custom-nbt value.");
                            shopPage.addBrokenItem("&cInvalid or Unsupported NBT", item.getSlot());
                            item.setResolveFailed(true);
                            continue;
                        }
                    }
                    
                    if (player != null) {
                        if (Main.getCREATOR().contains(player.getName())) {
                            NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
                            Main.debugLog("USER IN CREATOR.Setting item Buy Price");
                            if (item.hasBuyPrice()) {
                                comp.setDouble("buyPrice", item.getBuyPriceAsDecimal().doubleValue());
                            }
                            if (item.hasSellPrice()) {
                                comp.setDouble("sellPrice", item.getSellPriceAsDecimal().doubleValue());
                            }
                            if (item.hasBuyName()) {
                                comp.setString("buyName", item.getBuyName());
                            }
                            if (item.hasShopName()) {
                                comp.setString("shopName", item.getShopName());
                            }
                            if (item.hasMobType()) {
                                comp.setString("mob-type", item.getMobType());
                            }
                            if (item.hasEnchantments()) {
                                String enchantments = "";
                                for (String str : item.getEnchantments()) {
                                    enchantments += str + ",";
                                }
                                comp.setString("enchantments", enchantments);
                            }
                            if (item.hasShopLore()) {
                                String lor = "";
                                int index = 0;
                                for (String str : item.getShopLore()) {
                                    if (index != (item.getShopLore().size() - 1)) {
                                        lor += str + "::";
                                    } else {
                                        lor += str;
                                    }
                                    index += 1;
                                }
                                comp.setString("shopLoreLines", lor);
                            }
                            if (item.hasBuyLore()) {
                                String lor = "";
                                int index = 0;
                                for (String str : item.getBuyLore()) {
                                    if (index != (item.getBuyLore().size() - 1)) {
                                        lor += str + "::";
                                    } else {
                                        lor += str;
                                    }
                                    index += 1;
                                }
                                comp.setString("loreLines", lor);
                            }
                            if (item.hasCommands()) {
                                String lor = "";
                                int index = 0;
                                for (String str : item.getCommands()) {
                                    if (index != (item.getCommands().size() - 1)) {
                                        lor += str + "::";
                                    } else {
                                        lor += str;
                                    }
                                    index += 1;
                                }
                                comp.setString("commands", lor);
                            }
                            comp.setString("itemType", item.getItemType().toString());
                            itemStack = ItemNBTUtil.setNBTTag(comp, itemStack);
                        }
                    }
                    
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
                                    Main.log("Potion: " + pi.getType() + " Is not upgradable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                                    pi.setUpgraded(false);
                                    pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                                    pm.setBasePotionData(pd);
                                } else if (ex.getMessage().contains("extended")) {
                                    Main.log("Potion: " + pi.getType() + " Is not extendable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                                    pi.setExtended(false);
                                    pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                                    pm.setBasePotionData(pd);
                                }
                            }
                            itemStack.setItemMeta(pm);
                        } else {
                            Potion potion = new Potion(PotionType.valueOf(pi.getType()), pi.getUpgraded() == true ? 2 : 1, pi.getSplash(), pi.getExtended());
                            potion.apply(itemStack);
                        }
                    }
                } else if (item.getItemType() == ItemType.DUMMY) {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (item.hasShopName()) {
                        assert itemMeta != null;
                        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getShopName()));
                    }
                    itemStack.setItemMeta(itemMeta);
                }

                // Create Page
                Main.debugLog("Setting item to slot: " + item.getSlot());
                if (itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial() && item.hasSkullUUID()) {
                    itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(item.getSkullUUID()));
                }
                
                GuiItem gItem = new GuiItem(itemStack);
                shopPage.setItem(gItem, item.getSlot());
                
            }
            
            applyButtons(shopPage, pageIndex, shopPages.size());
            pane.addPane(pageIndex, shopPage);
            pageIndex += 1;
            
        }
        
        GUI.addPane(pane);
        this.currentPane = pane;
        
    }

    /**
     * Creates a named itemstack from a material and name.
     *
     * @param material the material
     * @param name the name with colour codes already applied
     * @return a named item
     */
    private ItemStack makeNamedItem(Material material, String name) {
        ItemStack is = new ItemStack(material);
        if (!name.isEmpty()) {
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName(name);
            is.setItemMeta(meta);
        }
        return is;
    }
    
    private void applyButtons(ShopPane page, int pageIndex, int maxPages) {
        if (pageIndex < (maxPages - 1)) {
            page.setItem(new GuiItem(makeNamedItem(Material.ARROW, ConfigUtil.getForwardPageButtonName())), 51);
        }
        Main.debugLog("Applying buttons with pageIndex: " + pageIndex + " maxPages: " + maxPages);
        if (pageIndex > 0) {
            Main.debugLog("Adding Back Button");
            page.setItem(new GuiItem(makeNamedItem(Material.ARROW, ConfigUtil.getBackwardPageButtonName())), 47);
        }
        if (!ConfigUtil.isDisableBackButton()) {
            
            ItemStack backButtonItem = new ItemStack(
                    Objects.requireNonNull(XMaterial.matchXMaterial(ConfigUtil.getBackButtonItem()).get().parseMaterial()));
            
            ItemMeta backButtonMeta = backButtonItem.getItemMeta();
            
            assert backButtonMeta != null;
            backButtonMeta.setDisplayName(ConfigUtil.getBackButtonText());
            
            backButtonItem.setItemMeta(backButtonMeta);
            
            GuiItem item = new GuiItem(backButtonItem);
            
            page.setItem(item, 53);
        }
    }

    /**
     * Open the player's shop
     *
     * @param input The player the shop will open for.
     */
    public void open(Player input) {
        // currentPane.setPage(0);
        if (this.isShopMissing() || shop.equalsIgnoreCase("NONE")) {
            return;
        }
        GUI.show(input);
        if (!Main.getCREATOR().contains(input.getName())) {
            GUI.setOnTopClick(this::onShopClick);
            GUI.setOnBottomClick((e) -> {
                e.setCancelled(true);
            });
        } else {
            GUI.setOnBottomClick(this::creatorPlayerInventoryClick);
            GUI.setOnTopClick(this::creatorTopInventoryClick);
        }
        GUI.setOnClose(this::onClose);
        
    }
    
    private void onShopClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (blacklistedSlots.contains(e.getSlot())) {
            return;
        }
        
        if (e.getClickedInventory() == null) {
            return;
        }
        
        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        // Forward Button
        Main.debugLog("Clicked: " + e.getSlot());
        if (e.getSlot() == 51) {
            if (shopItem.getPages().size() > 1 && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {
                hasClicked = true;
                
                Main.debugLog("Setting page " + currentPane.getPage() + " to not visible");
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                Main.debugLog("Setting page to: " + (currentPane.getPage() + 1));
                currentPane.setPage(currentPane.getPage() + 1);
                
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                Main.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");
                GUI.update();
            }
            return;
            // Backward Button
        } else if (e.getSlot() == 47) {
            if (currentPane.getPage() != 0 && currentPane.getPages() > 0) {
                hasClicked = true;
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                currentPane.setPage(currentPane.getPage() - 1);
                
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUI.update();
            }
            return;
            // Back Button
        } else if (e.getSlot() == 53 && !ConfigUtil.isDisableBackButton()) {
            if (menuInstance != null && !Main.getCREATOR().contains(player.getName())) {
                menuInstance.open(player);
            }
            return;
        }

        /*
         * If the player has enough money to purchase the item, then allow them to.
         */
        Main.debugLog("Creator Status:" + Main.getCREATOR().contains(player.getName()));
        
        Item item = shopItem.getPages().get("Page" + currentPane.getPage()).getItems().get(Integer.toString(e.getSlot()));
        
        if (item == null) {
            return;
            
        } else if (!item.hasBuyPrice()) {
            
            if (ConfigUtil.isAlternateSellEnabled() && item.hasSellPrice() && (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                new AltSell(item).open(player);
            } else {
                if (item.getItemType() == ItemType.DUMMY) {
                    return;
                } else {
                    player.sendMessage(ConfigUtil.getPrefix() + " " + ConfigUtil.getCannotBuy());
                }
            }
            return;
        }
        
        if (item.getItemType() == ItemType.SHOP) {
            hasClicked = true;
            if (ConfigUtil.isAlternateSellEnabled() && item.hasSellPrice() && (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                new AltSell(item).open(player);
            } else {
                if (item.isResolveFailed()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCannot purchase item that contains errors."));
                } else {
                    if (!item.isDisableQty()) {
                        new Quantity(item, this, player).loadInventory().open();
                    } else {
                        new Quantity(item, this, player).buy(item, 1, e);
                        hasClicked = false;
                    }
                }
                
            }
        } else if (item.getItemType() == ItemType.COMMAND) {
            
            BigDecimal priceToPay;
            
            Runnable dynamicPricingUpdate = null;

            // sell price must be defined and nonzero for dynamic pricing to work
            if (ConfigUtil.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {
                
                String itemString = item.getItemString();
                dynamicPricingUpdate = () -> Main.getDYNAMICPRICING().buyItem(itemString, 1);
                
                priceToPay = Main.getDYNAMICPRICING().calculateBuyPrice(itemString, 1, item.getBuyPriceAsDecimal(), item.getSellPriceAsDecimal());
            } else {
                priceToPay = item.getBuyPriceAsDecimal();
            }
            
            if (Main.getECONOMY().withdrawPlayer(player, priceToPay.doubleValue()).transactionSuccess()) {
                item.getCommands().forEach(str -> {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                            Main.placeholderIfy(str, player, item));
                });
                if (dynamicPricingUpdate != null) {
                    dynamicPricingUpdate.run();
                }
            } else {
                player.sendMessage(ConfigUtil.getPrefix() + ConfigUtil.getNotEnoughPre() + priceToPay
                        + ConfigUtil.getNotEnoughPost());
            }
        }
        
    }
    
    private void creatorPlayerInventoryClick(InventoryClickEvent e) {
        if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {

            // Since shift clicking moves items to the first available slot, we can assume
            // the item
            // will end up in this slot.
            int slot = e.getInventory().firstEmpty();

            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(slot);
                if (item != null) {
                    Main.debugLog("new Item: " + item.getType());
                    editShopItem(item, slot);
                }
            }, 5L);
        }
    }
    
    private void creatorTopInventoryClick(InventoryClickEvent e) {
        if (e.getCurrentItem() != null || e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.SHIFT_LEFT) {
            Main.debugLog("Cursor: " + e.getCursor());
            deleteShopItem(e.getSlot());

            // When an item is dropped into the slot, it's not null. This is a new item.
        } else {
            // Run the scheduler after this event is complete. This will ensure the
            // possible new item is in the slot in time.
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> {
                ItemStack item = e.getInventory().getItem(e.getSlot());
                if (item != null) {
                    Main.debugLog("new Item: " + item.getType());
                    editShopItem(item, e.getSlot());
                }
            }, 5L);
        }
    }
    
    private void deleteShopItem(Integer slot) {
        shopItem.getPages().get("Page" + currentPane.getPage()).getItems().remove(Integer.toString(slot));
        ConfigurationSection config = Main.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items") != null
                ? Main.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items")
                : Main.getINSTANCE().getShopConfig().createSection(shop + ".pages.Page" + currentPane.getPage() + ".items");
        config.set(slot.toString(), null);
        try {
            Main.getINSTANCE().getShopConfig().save(Main.getINSTANCE().getShopf());
        } catch (IOException ex) {
            Main.debugLog("Error saving Shops: " + ex.getMessage());
        }
    }
    
    public void editShopItem(ItemStack itemStack, Integer slot) {
        
        Item item = Item.parse(itemStack, slot, shop);
        shopItem.getPages().get("Page" + currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);
        
        ConfigurationSection config = Main.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items") != null
                ? Main.getINSTANCE().getShopConfig().getConfigurationSection(shop + ".pages.Page" + currentPane.getPage() + ".items")
                : Main.getINSTANCE().getShopConfig().createSection(shop + ".pages.Page" + currentPane.getPage() + ".items");
        
        config.set(slot.toString(), item.serialize());
        Main.debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + slot);
        try {
            Main.getINSTANCE().getShopConfig().save(Main.getINSTANCE().getShopf());
        } catch (IOException ex) {
            Main.debugLog("Error saving Shops: " + ex.getMessage());
        }
        hasClicked = false;
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        if (!Main.CREATOR.contains(player.getName())) {
            if (!ConfigUtil.isDisableEscapeBack() && !hasClicked) {
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> menuInstance.open(player), 1L);
            } else {
                hasClicked = false;
            }
        } else if (!hasClicked) {
            Main.CREATOR.remove(player.getName());
        }
        
    }
    
    public Boolean isShopMissing() {
        return this.shopMissing == true;
    }
    
}
