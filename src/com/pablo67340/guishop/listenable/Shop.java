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
import com.pablo67340.guishop.util.ConfigUtil;
import java.io.IOException;

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
    private Map<Integer, Item> items;

    private Map<Integer, Item> editedItems;

    /**
     * The list of {@link Page}'s in this {@link Shop}.
     */
    private Gui GUI;

    private final Menu menuInstance;

    private Boolean hasClicked = false;

    private int pageC = 0;

    private PaginatedPane currentPane;

    int oldPage = 0;

    private Integer lastIndex = 0;

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
    Shop(String shop, String name, String description, List<String> lore, Menu menuInstance, Map<Integer, Item> items) {
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
    public void loadItems() {

        if (items == null) {

            items = new HashMap<>();
            int pageC = 0;
            int index = 0;

            ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);

            if (config == null) {
                Main.log("Check the section for shop " + shop + " in the shops.yml. It was not found.");

            } else {
                for (String str : config.getKeys(false)) {

                    Item item = new Item();

                    ConfigurationSection section = config.getConfigurationSection(str);
                    if (section == null) {
                        Main.log("Check the config section for item " + str + " in shop " + shop + " in the shops.yml. It is not a valid section.");
                        continue;
                    }
                    int slot = Integer.parseInt(str);

                    if (Integer.parseInt(str) == 45) {
                        pageC += 1;
                    }

                    if (pageC > 0) {
                        slot = Integer.parseInt(str) - (44 * pageC);
                    }

                    item.setSlot(slot);

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

                    Main.getINSTANCE().getITEMTABLE().put(item.getItemString(), item);
                    if (section.getBoolean("show-in-gui", true)) {
                        items.put(index, item);
                    }

                    index++;
                }
                loadShop();
            }
        } else {
            System.out.println("Items were not null");
            loadShop();
        }

    }

    private void loadShop() {
        Integer index = 0;
        ShopPane page = new ShopPane(9, 6);

        this.GUI = new Gui(Main.getINSTANCE(), 6,
                ChatColor.translateAlternateColorCodes('&', ConfigUtil.getShopTitle().replace("{shopname}", getName())));
        PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);

        for (Item item : items.values()) {

            GuiItem gItem = item.parseMaterial();

            Main.debugLog("Loaded item with material: " + gItem.getItem().getType().toString());

            if (gItem == null) {
                Main.debugLog("Item " + item.getMaterial() + " could not be resolved (invalid material)");
                gItem = new GuiItem(new ItemStack(Material.AIR));
                indexCheck(index, pane, item, page, gItem);
                index += 1;
                continue;
            }

            // Checks if an item is either a shop item or command item. This also handles
            // Null items as there is a item type switch in the lines above.
            if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {

                ItemStack itemStack = gItem.getItem();
                ItemMeta itemMeta = itemStack.getItemMeta();

                if (itemMeta == null) {
                    Main.debugLog("Item + " + item.getMaterial() + " could not be resolved (null meta)");
                    gItem = new GuiItem(new ItemStack(Material.AIR));
                    indexCheck(index, pane, item, page, gItem);
                    index += 1;
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
            indexCheck(index, pane, item, page, gItem);
            index += 1;
        }

        this.currentPane = pane;

    }

    // Helper method to re-calculate the current item parsing index if an item has failed.
    private void indexCheck(Integer index, PaginatedPane pane, Item item, ShopPane page, GuiItem gItem) {
        Main.debugLog("Item Index - lastIndex: " + ((index) - lastIndex) + " index was: " + index + " Items Size: " + items.size());
        if (index + 1 == items.size() || ((index) - lastIndex) == 44) {
            if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
                page.setItem(gItem, item.getSlot());
            }

            if (items.size() > 45) {
                applyButtons(page);
            }
            lastIndex = index;
            pane.addPane(pageC, page);
            Main.debugLog("Saved Page: " + pageC + " Pages: " + pane.getPages());
            pageC += 1;
            Main.debugLog("Creating Page: " + pageC );
            page = new ShopPane(9, 6);
        } else {
            if (pageC == 0) {
                if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
                    page.setItem(gItem, item.getSlot());
                } else {
                    page.setDummy(item.getSlot(), new ItemStack(Material.AIR));
                }
            } else {

                if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
                    Main.debugLog("Adding item: "+gItem.getItem().getType()+" to slot "+item.getSlot()+" on page: "+pageC);
                    page.setItem(gItem, item.getSlot());
                } else {
                    page.setDummy(item.getSlot(), new ItemStack(Material.AIR));
                }
            }
        }

        if (index + 1 == items.size()) {
            pane.addPane(pageC, page);
            applyButtons(page);
            GUI.addPane(pane);
            Main.getINSTANCE().getLoadedShops().put(name, items);
        }
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

    private void applyButtons(ShopPane page) {
        if (page.getINSTANCE().getItemsMap().containsKey(44)) {
            page.setItem(new GuiItem(makeNamedItem(Material.ARROW, ConfigUtil.getBackwardPageButtonName())), 51);
        }
        if (pageC > 0) {
            page.setItem(new GuiItem(makeNamedItem(Material.ARROW, ConfigUtil.getForwardPageButtonName())), 47);
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

        if (Main.CREATOR.contains(input.getName())) {
            editedItems = new HashMap<>();
        }

    }

    private void onShopClick(InventoryClickEvent e) {
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
            if (items.size() > 44) {
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

                Main.debugLog("Setting page "+currentPane.getPage()+" to not visible");
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
                Main.debugLog("Setting page to: "+(currentPane.getPage() + 1));
                currentPane.setPage(currentPane.getPage() + 1);
                
                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                Main.debugLog("Setting Page: "+currentPane.getPage()+" to visible.");
                GUI.update();
            }
            return;
            // Backward Button
        } else if (e.getSlot() == 47) {
            if (items.size() > 44) {
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
            Item item = getItems().get((currentPane.getPage() * 45) + e.getSlot());

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
        editedItems.put(slot, dedItem);
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
        editedItems.put(item.getSlot(), item);

        Main.debugLog("Player Edited Item: " + item.getMaterial() + " slot: " + configSlot);

    }

    /**
     * Save the items for current shop instance
     *
     * @param player - The player who is saving the items.
     */
    public void saveItems(Player player) {

        Main.debugLog("Getting Section for: " + shop);

        ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop) != null
                ? Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop)
                : Main.getINSTANCE().getCustomConfig().createSection(shop);

        Main.debugLog("Config: " + config);

        editedItems.values().forEach(item -> {
            if (!item.getMaterial().equalsIgnoreCase("DED")) {
                config.set(item.getSlot() + "", null);
                ConfigurationSection section = config.createSection(item.getSlot() + "");
                section.set("type", item.getItemType().toString());
                section.set("id", item.getMaterial());
                if (item.hasBuyPrice()) {
                    section.set("buy-price", item.getBuyPrice());
                }
                if (item.hasSellPrice()) {
                    section.set("sell-price", item.getSellPrice());
                }
                if (item.hasShopLore()) {
                    section.set("shop-lore", item.getShopLore());
                }
                if (item.hasShopName()) {
                    section.set("shop-name", item.getShopName());
                }
                if (item.hasBuyName()) {
                    section.set("buy-name", item.getBuyName());
                }
                if (item.hasEnchantments()) {
                    String parsed = "";
                    for (String str : item.getEnchantments()) {
                        parsed += str + " ";
                    }
                    section.set("enchantments", parsed.trim());
                }
                if (item.hasShopLore()) {
                    section.set("shop-lore", item.getShopLore());
                }
                if (item.hasBuyLore()) {
                    section.set("buy-lore", item.getBuyLore());
                }
                if (item.hasCommands()) {
                    section.set("commands", item.getCommands());
                }
                if (item.hasMobType()) {
                    section.set("mobType", item.getMobType());
                }
            } else {
                Main.debugLog("Item was ded: " + item.getSlot());
                config.set(item.getSlot() + "", null);
            }
        });
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
