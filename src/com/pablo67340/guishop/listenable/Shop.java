package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import java.util.*;
import java.util.logging.Level;

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
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.ShopItem;
import com.pablo67340.guishop.definition.ShopPage;
import com.pablo67340.guishop.util.ConfigUtil;
import java.io.IOException;
import java.util.Map.Entry;

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
     * The list of {@link Page}'s in this {@link Shop}.
     */
    private Gui GUI;

    private final Menu menuInstance;

    private Boolean hasClicked = false;

    private PaginatedPane currentPane;

    private ShopItem shopItem;

    private ShopPane shopPage = new ShopPane(9, 6);

    private final List<Integer> blacklistedSlots = new ArrayList<>(Arrays.asList(53, 52, 50, 49, 48, 46, 45));

    /**
     * The constructor for a {@link Shop}.
     *
     * @param name The name of the shop.
     * @param description The description of the shop.
     * @param lore The lore of the shop.
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
     * @param shop The Shop ID.
     * @param name The name of the shop.
     * @param description The description of the shop.
     * @param lore The lore of the shop.
     */
    public Shop(String shop, String name, String description, List<String> lore) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
        this.menuInstance = null;
    }

    /**
     * The constructor for a {@link Shop}.
     *
     * @param name The name of the shop.
     * @param description The description of the shop.
     * @param lore The lore of the shop.
     */
    Shop(String shop, String name, String description, List<String> lore, Menu menuInstance, ShopItem shopItem) {
        this.name = name;
        this.shop = shop;
        this.description = description;
        this.lore = lore;
        this.menuInstance = menuInstance;
        this.shopItem = shopItem;
    }

    /**
     * Load the specified shop
     */
    public void loadItems() {
        if (!Main.getINSTANCE().getLoadedShops().containsKey(shop)) {
            shopItem = new ShopItem();
            ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop+".pages");
            Main.debugLog("Loading items for shop: " + shop);

            if (config == null) {
                Main.log("Check shops.yml for shop " + shop + ". It was not found.");
            } else {
                config.getKeys(false).stream().map(str -> {
                    ShopPage page = new ShopPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str+".items");
                    Main.debugLog("Reading Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        Item item = new Item();
                        Main.debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        item.setSlot(Integer.parseInt(key));
                        item.setMaterial((section.contains("id") ? (String) section.get("id") : "AIR"));
                        if (item.isAnyPotion()) {
                            ConfigurationSection potionSection = section.getConfigurationSection("potion-info");
                            if (potionSection != null) {
                                item.parsePotionType(potionSection.getString("type"),
                                        potionSection.getBoolean("splash", false),
                                        potionSection.getBoolean("extended", false), potionSection.getInt("amplifier", -1));
                            }
                        }
                        item.setMobType((section.contains("mobType") ? (String) section.get("mobType") : null));
                        item.setShopName((section.contains("shop-name") ? (String) section.get("shop-name") : null));
                        item.setBuyName((section.contains("buy-name") ? (String) section.get("buy-name") : null));
                        if (section.contains("enchantments")) {
                            String enchantments = section.getString("enchantments");
                            if (!enchantments.equalsIgnoreCase(" ")) {
                                item.setEnchantments(enchantments.split(" "));
                            }
                        }
                        item.setBuyPrice(section.get("buy-price"));
                        item.setSellPrice(section.get("sell-price"));
                        item.setItemType(
                                section.contains("type") ? ItemType.valueOf((String) section.get("type")) : ItemType.SHOP);
                        item.setUseDynamicPricing(section.getBoolean("use-dynamic-price", true));
                        item.setShopLore(
                                (section.contains("shop-lore") ? section.getStringList("shop-lore") : new ArrayList<>()));
                        item.setBuyLore(
                                (section.contains("buy-lore") ? section.getStringList("buy-lore") : new ArrayList<>()));
                        item.setCommands(
                                (section.contains("commands") ? section.getStringList("commands") : new ArrayList<>()));
                        return item;
                    }).map(item -> {
                        Main.getINSTANCE().getITEMTABLE().put(item.getItemString(), item);
                        return item;
                    }).forEachOrdered(item -> {
                        String mapSlot = Integer.toString(page.getItems().values().size());
                        page.getItems().put(mapSlot, item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    String pageTitle = "Page"+(shopItem.getPages().size());
                    Main.debugLog("Added page to: "+pageTitle);
                    shopItem.getPages().put(pageTitle, page);
                });

                Main.getINSTANCE().getLoadedShops().put(shop, shopItem);
            }
        } else {
            shopItem = Main.getINSTANCE().getLoadedShops().get(shop);
            loadShop();
        }
    }

    private void loadShop() {
        this.GUI = new Gui(Main.getINSTANCE(), 6,
                ChatColor.translateAlternateColorCodes('&', ConfigUtil.getShopTitle().replace("{shopname}", getName())));
        PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
        int pageIndex = 0;
        for (ShopPage page : shopItem.getPages().values()) {
            shopPage = new ShopPane(9, 6);
            Collection<Item> shopPages = page.getItems().values();
            for (Item item : shopPages) {
                GuiItem gItem = item.parseMaterial();
                Main.debugLog("Adding item to slot: " + item.getSlot());
                if (gItem == null) {
                    Main.debugLog("Item " + item.getMaterial() + " could not be resolved (invalid material)");
                    shopPage.addBlankItem();
                    continue;
                }

                // Checks if an item is either a shop item or command item. This also handles
                // Null items as there is a item type switch in the lines above.
                if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {

                    ItemStack itemStack = gItem.getItem();
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        Main.debugLog("Item + " + item.getMaterial() + " could not be resolved (null meta)");
                        shopPage.addBlankItem();
                        continue;
                    }

                    List<String> itemLore = new ArrayList<>();

                    itemLore.add(item.getBuyLore(1));

                    itemLore.add(item.getSellLore(1));

                    if (item.hasShopLore()) {
                        item.getShopLore().forEach(str -> {
                            if (!itemLore.contains(str) && !itemLore.contains(ConfigUtil.getBuyLore().replace("{AMOUNT}", Double.toString(item.calculateBuyPrice(1))))) {
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                            }
                        });
                    }

                    if (!itemLore.isEmpty()) {
                        assert itemMeta != null;
                        itemMeta.setLore(itemLore);
                    }

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

                    if (item.getEnchantments() != null) {
                        if (item.getEnchantments().length > 1) {
                            for (String enc : item.getEnchantments()) {
                                String enchantment = StringUtils.substringBefore(enc, ":");
                                String level = StringUtils.substringAfter(enc, ":");
                                itemStack.addUnsafeEnchantment(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level));
                            }
                        }
                    }

                    itemStack.setItemMeta(itemMeta);

                    if (item.hasPotion()) {
                        item.getPotion().apply(itemStack);
                    }

                }

                // Create Page
                Main.debugLog("Setting item to slot: " + item.getSlot());
                shopPage.setItem(gItem, item.getSlot());

            }

            applyButtons(shopPage, pageIndex, shopItem.getPages().size());
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
        if (!ConfigUtil.isEscapeOnly()) {

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
        GUI.show(input);
        if (!Main.getCREATOR().contains(input.getName())) {
            GUI.setOnBottomClick(event -> {
                event.setCancelled(true);
            });
        }
        GUI.setOnClose(this::onClose);
        GUI.setOnGlobalClick(this::onShopClick);

    }

    private void onShopClick(InventoryClickEvent e) {
        if (blacklistedSlots.contains(e.getSlot())) {
            e.setCancelled(true);
            return;
        }
        Player player = (Player) e.getWhoClicked();
        Main.debugLog("Click");
        if (!Main.getCREATOR().contains(player.getName())) {
            e.setCancelled(true);
            hasClicked = true;
        }

        if (e.getClickedInventory() == null) {
            return;
        }

        // Forward Button
        Main.debugLog("Clicked: " + e.getSlot());
        if (e.getSlot() == 51) {
            hasClicked = true;
            if (shopItem.getPages().size() > 0) {
                if (Main.getCREATOR().contains(player.getName())) {
                    ItemStack[] shopItems = GUI.getInventory().getContents();

                    int slot = 0;
                    for (ItemStack item : shopItems) {
                        ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setItem(new GuiItem(item),
                                slot);
                        slot += 1;
                    }

                    saveItems(player);
                }

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
            if (currentPane.getPage() != 0) {
                hasClicked = true;

                if (Main.getCREATOR().contains(player.getName())) {
                    ItemStack[] shopItems = GUI.getInventory().getContents();

                    int slot = 0;
                    for (ItemStack item : shopItems) {
                        ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setItem(new GuiItem(item),
                                slot);
                        slot += 1;
                    }
                    saveItems(player);
                }
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                currentPane.setPage(currentPane.getPage() - 1);

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUI.update();
            }
            return;
            // Back Button
        } else if (e.getSlot() == 53 && !ConfigUtil.isEscapeOnly()) {
            if (menuInstance != null && !Main.getCREATOR().contains(player.getName())) {
                menuInstance.open(player);
            }
            return;
        }

        /*
		 * If the player has enough money to purchase the item, then allow them to.
         */
        Main.debugLog("Creator Status:" + Main.getCREATOR().contains(player.getName()));
        if (!Main.getCREATOR().contains(player.getName())) {
            Item item = shopItem.getPages().get("Page"+currentPane.getPage()).getItems().get(e.getSlot());

            if (item == null) {
                return;

            } else if (!item.hasBuyPrice()) {

                if (ConfigUtil.isAlternateSellEnabled() && item.hasSellPrice() && (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                    new AltSell(item).open(player);
                } else {
                    player.sendMessage(ConfigUtil.getPrefix() + " " + ConfigUtil.getCannotBuy());
                }
                return;
            }

            if (item.getItemType() == ItemType.SHOP) {
                if (ConfigUtil.isAlternateSellEnabled() && item.hasSellPrice() && (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT)) {
                    new AltSell(item).open(player);
                } else {
                    new Quantity(item, this, player).loadInventory().open();
                }
            } else if (item.getItemType() == ItemType.COMMAND) {

                double priceToPay;

                Runnable dynamicPricingUpdate = null;

                // sell price must be defined and nonzero for dynamic pricing to work
                if (ConfigUtil.isDynamicPricing() && item.isUseDynamicPricing() && item.hasSellPrice()) {

                    String itemString = item.getItemString();
                    dynamicPricingUpdate = () -> Main.getDYNAMICPRICING().buyItem(itemString, 1);

                    priceToPay = Main.getDYNAMICPRICING().calculateBuyPrice(itemString, 1, item.getBuyPriceAsDouble(), item.getSellPriceAsDouble());
                } else {
                    priceToPay = item.getBuyPriceAsDouble();
                }

                if (Main.getECONOMY().withdrawPlayer(player, priceToPay).transactionSuccess()) {
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
        } else {
            // When players remove an item from the shop
            if (e.getClickedInventory().getType() != InventoryType.PLAYER) {

                // If an item was removed from the shop, this is null
                if (e.getCurrentItem() != null) {
                    Main.debugLog("Cursor: " + e.getCursor());
                    if (e.getInventory().getItem(e.getSlot()) != null) {
                        deleteShopItem(e.getSlot());
                    }

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
            } else {
                // When the player moves an item from their inventory to the shop via shift
                // click
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
        }
    }

    private void deleteShopItem(Integer slot) {
        Main.debugLog("Deleting Item: " + slot);
        Main.getINSTANCE().setCreatorRefresh(true);
        Item dedItem = new Item();
        dedItem.setMaterial("DED");
        dedItem.setSlot(slot);
        shopItem.getPages().get("Page"+currentPane.getPage()).getItems().put(Integer.toString(slot), dedItem);
    }

    /**
     * The inventory closeEvent handling for the Menu.
     */
    private void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        if (!Main.CREATOR.contains(player.getName())) {
            if (ConfigUtil.isEscapeOnly() && !hasClicked) {
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> menuInstance.open(player), 1L);

            } else {
                hasClicked = false;
            }
        } else {
            saveItems(player);
        }

    }

    public void editShopItem(ItemStack itemStack, int slot) {
        Item item = new Item();
        Integer mult = (44 * currentPane.getPage());
        Integer configSlot = slot + mult;
        Main.getINSTANCE().setCreatorRefresh(true);
        if (itemStack != null) {
            NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
            ItemMeta im = itemStack.getItemMeta();
            item.setItemType(ItemType.SHOP);
            item.setMaterial(itemStack.getType().toString());
            item.setSlot(configSlot);
            if (comp.hasKey("buyPrice")) {

                Object buyPrice = getBuyPrice(itemStack);
                Main.debugLog("had buyPrice comp: " + buyPrice);
                item.setBuyPrice(buyPrice);
            }
            if (comp.hasKey("sellPrice")) {

                Object sellPrice = getSellPrice(itemStack);
                item.setSellPrice(sellPrice);
            }

            if (im.hasDisplayName()) {
                item.setShopName(im.getDisplayName());
            }

            if (comp.hasKey("buyName")) {
                item.setBuyName(comp.getString("buyName"));
            }

            if (comp.hasKey("enchantments")) {
                item.setEnchantments(comp.getString("enchantments").split(" "));
            }

            if (comp.hasKey("itemType")) {
                item.setItemType(ItemType.valueOf(comp.getString("itemType")));
            }

            if (comp.hasKey("commands")) {
                item.setItemType(ItemType.COMMAND);
                item.setCommands(Arrays.asList(comp.getString("commands").split("::")));
            }

            if (comp.hasKey("mobType")) {
                item.setMobType(comp.getString("mobType"));
            }

            if (im.hasLore()) {
                List<String> itemLore = im.getLore();
                List<String> cleaned = new ArrayList<>();
                itemLore.stream().filter(str -> (!(ChatColor.stripColor(str)
                        .contains(ChatColor.stripColor(ConfigUtil.getBuyLore().replace("{amount}", ""))))
                        && !(ChatColor.stripColor(str)
                                .contains(ChatColor.stripColor(ConfigUtil.getSellLore().replace("{amount}", ""))))
                        && !(ChatColor.stripColor(str).contains(ChatColor.stripColor(ConfigUtil.getCannotBuy())))
                        && !(ChatColor.stripColor(str).contains(ChatColor.stripColor(ConfigUtil.getCannotSell()))))).forEachOrdered(str -> {
                    cleaned.add(str);
                });
                item.setShopLore(cleaned);
            }

            if (comp.hasKey("loreLines")) {
                String line = comp.getString("loreLines");
                String[] parsedLore = line.split("::");
                item.setBuyLore(Arrays.asList(parsedLore));
            }
        }
        shopItem.getPages().get("Page"+currentPane.getPage()).getItems().put(Integer.toString(item.getSlot()), item);

        Main.debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + configSlot);

    }

    /**
     * Save the items for current shop instance
     *
     * @param player - The player who is saving the items.
     */
    public void saveItems(Player player) {

        Main.debugLog("Saving Shop: " + shop);

        for (Entry<String, ShopPage> page : shopItem.getPages().entrySet()){
            for (Entry<String, Item> items : page.getValue().getItems().entrySet()){
                Main.getINSTANCE().getCustomConfig().set(shop+".pages."+page.getKey()+".items."+items.getKey(), items.getValue());
            }
        }

        try {
            Main.getINSTANCE().getCustomConfig().save(Main.getINSTANCE().getSpecialf());
        } catch (IOException ex) {
            Main.getINSTANCE().getLogger().log(Level.WARNING, ex.getMessage());
        }

        if (!hasClicked) {
            Main.debugLog("Removed from creator");
            Main.getCREATOR().remove(player.getName());
        }
        Main.sendMessage(player, "&aShop Saved!");
    }

    /**
     * Gets the buyPrice of the item using NBT, or <code>null</code> if not
     * defined
     *
     */
    private Object getBuyPrice(ItemStack item) {
        NBTTagCompound comp = ItemNBTUtil.getTag(item);

        if (comp.hasKey("buyPrice")) {
            Double vl = comp.getDouble("buyPrice");
            return vl;
        }
        return null;
    }

    /**
     * Gets the sellPrice of an item using NBT, or <code>null</code> if not
     * defined
     *
     */
    private Object getSellPrice(ItemStack item) {
        NBTTagCompound comp = ItemNBTUtil.getTag(item);

        if (comp.hasKey("sellPrice")) {
            Double vl = comp.getDouble("sellPrice");
            return vl;
        }
        return null;
    }

}
