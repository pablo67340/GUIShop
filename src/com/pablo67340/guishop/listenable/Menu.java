package com.pablo67340.guishop.listenable;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.MenuItem;
import com.pablo67340.guishop.definition.MenuPage;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.SkullCreator;
import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;

public final class Menu {

    /**
     * The GUI that is projected onto the screen when a {@link Player} opens the
     * {@link Menu}.
     */
    private Gui GUI;

    private Boolean hasClicked = false;

    private MenuItem menuItem;

    private ShopPane menuPage = new ShopPane(9, 6);

    private final Player player;

    Integer pageIndex = 0;

    private PaginatedPane currentPane;

    /**
     * A {@link Map} that will store our {@link Shop}s when the server first
     * starts.
     *
     * @key The index on the {@link Menu} that this shop is located at.
     * @param player The player using this Menu.
     * @value The shop.
     */
    public Menu(Player player) {
        this.player = player;
    }

    /**
     * Load the specified shop
     */
    public void loadItems() {
        pageIndex = 0;
        if (Main.getINSTANCE().getLoadedMenu() == null) {
            Main.debugLog("Loading Menu from Config.");
            menuItem = new MenuItem();
            ConfigurationSection config = Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu.pages");
            Main.debugLog("Loading items for Menu");

            if (config == null) {
                Main.log("Check menu.yml for Menu Items. They were not found, or config is incorrectly formatted.");
            } else {
                config.getKeys(false).stream().map(str -> {
                    MenuPage page = new MenuPage();
                    ConfigurationSection shopItems = config.getConfigurationSection(str + ".items");
                    Main.debugLog("Reading Page: " + str);
                    shopItems.getKeys(false).stream().map(key -> {
                        Item item = new Item();
                        Main.debugLog("Reading item: " + key + " in page " + str);
                        ConfigurationSection section = shopItems.getConfigurationSection(key);
                        item.setSlot(Integer.parseInt(key));
                        item.setMaterial((section.contains("id") ? (String) section.get("id") : "AIR"));
                        item.setTarget_shop((section.contains("target_shop") ? (String) section.get("target_shop") : "NONE"));
                        item.setSkullUUID((section.contains("skull-uuid") ? (String) section.get("skull-uuid") : null));
                        if (item.isAnyPotion()) {
                            ConfigurationSection potionSection = section.getConfigurationSection("potion-info");
                            if (potionSection != null) {
                                item.parsePotionType(potionSection.getString("type"),
                                        potionSection.getBoolean("splash", false),
                                        potionSection.getBoolean("extended", false), potionSection.getInt("amplifier", -1));
                            }
                        }
                        item.setName((section.contains("name") ? (String) section.get("name") : null));
                        if (section.contains("enchantments")) {
                            String enchantments = section.getString("enchantments");
                            if (!enchantments.equalsIgnoreCase(" ")) {
                                item.setEnchantments(enchantments.split(" "));
                            }
                        }
                        item.setLore(
                                (section.contains("lore") ? section.getStringList("lore") : new ArrayList<>()));
                        return item;
                    }).forEachOrdered(item -> {
                        page.getItems().put(Integer.toString(item.getSlot()), item);
                    });
                    return page;
                }).forEachOrdered(page -> {
                    Main.debugLog("Adding page: " + "Page" + Integer.toString(menuItem.getPages().size()) + " to pages.");
                    menuItem.getPages().put("Page" + Integer.toString(menuItem.getPages().size()), page);
                });
                Main.debugLog("Loaded Menu Cached");
                Main.getINSTANCE().setLoadedMenu(menuItem);
                loadMenu();
            }
        } else {
            Main.debugLog("Loading Menu from Cache.");
            menuItem = (MenuItem) Main.getINSTANCE().getLoadedMenu();
            loadMenu();
        }
    }

    private void loadMenu() {
        if (this.GUI == null || this.GUI.getItems().isEmpty()) {

            if (this.hasMultiplePages()) {
                this.GUI = new Gui(Main.getINSTANCE(), 6,
                        ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", ConfigUtil.getMenuShopPageNumber().replace("{number}", "1"))));
            } else {
                int rows = (int) Math.ceil((double) menuItem.getPages().get("Page0").getItems().size() / 9);
                this.GUI = new Gui(Main.getINSTANCE(), rows,
                        ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", "")));
            }

            PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);
            Collection<MenuPage> menuPages = menuItem.getPages().values();
            for (MenuPage page : menuPages) {
                menuPage = new ShopPane(9, 6);
                for (Item item : page.getItems().values()) {
                    ItemStack itemStack = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();
                    Main.debugLog("Adding item to slot: " + item.getSlot());
                    if (itemStack == null) {
                        Main.debugLog("Item " + item.getMaterial() + " could not be resolved (invalid material)");
                        menuPage.addBlankItem();
                        continue;
                    }

                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta == null) {
                        Main.debugLog("Item + " + item.getMaterial() + " could not be resolved (null meta)");
                        menuPage.addBlankItem();
                        continue;
                    }

                    List<String> itemLore = new ArrayList<>();

                    if (player != null) {
                        if (!Main.getCREATOR().contains(player.getName())) {
                            if (item.hasName()) {
                                assert itemMeta != null;
                                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getName()));
                            }
                            if (item.hasEnchantments()) {
                                for (String str : item.getEnchantments()) {
                                    String enchant = StringUtils.substringBefore(str, ":");
                                    String level = StringUtils.substringAfter(str, ":");
                                    itemLore.add(enchant + " " + level);
                                }
                            }
                            if (item.hasLore()) {
                                item.getLore().forEach(str -> {
                                    if (!itemLore.contains(str)) {
                                        itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                                    }
                                });
                            }
                        } else {
                            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            itemLore.add(" ");
                            itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fItem Type: &r" + item.getItemType().toString()));
                            if (item.hasName()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fName: &r" + item.getName()));
                            }
                            if (item.hasLore()) {
                                itemLore.add(" ");
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fLore: &r"));
                                item.getBuyLore().forEach(str -> {
                                    itemLore.add(str);
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

                    itemStack.setItemMeta(itemMeta);

                    if (player != null) {
                        if (Main.getCREATOR().contains(player.getName())) {
                            NBTWrappers.NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
                            Main.debugLog("USER IN CREATOR.Setting item Buy Price");
                            if (item.hasName()) {
                                comp.setString("itemName", item.getName());
                            }
                            if (item.hasEnchantments()) {
                                String enchantments = "";
                                for (String str : item.getEnchantments()) {
                                    enchantments += str + ",";
                                }
                                comp.setString("enchantments", enchantments);
                            }
                            if (item.hasLore()) {
                                String lor = "";
                                int index = 0;
                                for (String str : item.getLore()) {
                                    if (index != (item.getLore().size() - 1)) {
                                        lor += str + "::";
                                    } else {
                                        lor += str;
                                    }
                                    index += 1;
                                }
                                comp.setString("loreLines", lor);
                            }
                            comp.setString("itemType", item.getItemType().toString());
                            itemStack = ItemNBTUtil.setNBTTag(comp, itemStack);
                        }
                    }

                    if (item.hasEnchantments()) {
                        for (String enc : item.getEnchantments()) {
                            String enchantment = StringUtils.substringBefore(enc, ":");
                            String level = StringUtils.substringAfter(enc, ":");
                            itemStack.addUnsafeEnchantment(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level));
                        }
                    }

                    if (item.hasPotion()) {
                        item.getPotion().apply(itemStack);
                    }

                    // Create Page
                    Main.debugLog("Setting item to slot: " + item.getSlot());
                    if (itemStack.getType() == Material.PLAYER_HEAD && item.hasSkullUUID()) {
                        itemStack.setItemMeta(SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(item.getSkullUUID())));
                    }
                    GuiItem gItem = new GuiItem(itemStack);
                    menuPage.setItem(gItem, item.getSlot());

                }

                applyButtons(menuPage, pageIndex, menuPages.size());
                pane.addPane(pageIndex, menuPage);
                pageIndex += 1;

            }

            GUI.addPane(pane);
            this.currentPane = pane;
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

    /**
     * Save the items for current shop instance
     *
     * @param player - The player who is saving the items.
     */
    public void saveItems(Player player) {

        Main.debugLog("Getting Section for Menu");

        ConfigurationSection config = Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu") != null
                ? Main.getINSTANCE().getMenuConfig().getConfigurationSection("Menu")
                : Main.getINSTANCE().getMenuConfig().createSection("Menu");

        Main.debugLog("Config: " + config);
        pageIndex = 0;
        Main.getINSTANCE().setLoadedMenu(menuItem);
        menuItem.getPages().values().forEach(page -> {
            String pageNumber = "Page" + pageIndex;
            page.getItems().values().forEach(item -> {

                if (!item.getMaterial().equalsIgnoreCase("DED")) {
                    config.set("pages." + pageNumber + ".items." + item.getSlot() + "", null);
                    ConfigurationSection section = config.createSection("pages." + pageNumber + ".items." + item.getSlot() + "");
                    section.set("id", item.getMaterial());
                    if (item.hasLore()) {
                        section.set("lore", item.getLore());
                    }
                    if (item.hasShopName()) {
                        section.set("name", item.getName());
                    }
                    if (item.hasEnchantments()) {
                        String parsed = "";
                        for (String str : item.getEnchantments()) {
                            parsed += str + " ";
                        }
                        section.set("enchantments", parsed.trim());
                    }

                } else {
                    Main.debugLog("Item was ded: " + item.getSlot());
                    config.set("pages." + pageNumber + ".items." + item.getSlot() + "", null);
                }
            });
            pageIndex += 1;
        });
        // Set to re-cache the object since the config was changed
        Main.getINSTANCE().setLoadedMenu(null);
        try {
            Main.getINSTANCE().getMenuConfig().save(Main.getINSTANCE().getMenuf());
        } catch (IOException ex) {
            Main.getINSTANCE().getLogger().log(Level.WARNING, ex.getMessage());
        }

        if (!hasClicked) {
            Main.debugLog("Removed from creator");
            Main.getCREATOR().remove(player.getName());
        }
        Main.sendMessage(player, "&aMenu Saved!");
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
     * Opens the GUI in this {@link Menu}.
     *
     * @param player - The player the GUI will display to
     */
    public void open(Player player) {

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

        loadItems();

        GUI.setOnTopClick(this::onShopClick);
        GUI.setOnBottomClick(event -> {
            event.setCancelled(true);
        });
        if (Main.getCREATOR().contains(player.getName())) {
            GUI.setOnClose(event -> onClose(event));
        }
        GUI.show(player);

    }

    /**
     * Handle global inventory click events, check if inventory is for GUIShop,
     * if so, run logic.
     */
    private void onShopClick(InventoryClickEvent e) {
        Player pl = (Player) e.getWhoClicked();
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        hasClicked = true;

        if (e.getSlot() == Main.getINSTANCE().getMenuConfig().getInt("Menu.nextButtonSlot")) {
            hasClicked = true;
            if (hasMultiplePages() && this.currentPane.getPage() != (this.currentPane.getPages() - 1)) {
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

                Integer currentPage = currentPane.getPage() + 1;
                GUI.setTitle(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", ConfigUtil.getMenuShopPageNumber().replace("{number}", currentPage.toString()))));

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                Main.debugLog("Setting Page: " + currentPane.getPage() + " to visible.");
                GUI.update();
            }
            // Backward Button
        } else if (e.getSlot() == Main.getINSTANCE().getMenuConfig().getInt("Menu.backButtonSlot")) {
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

                if (hasMultiplePages()) {
                    Integer currentPage = currentPane.getPage() + 1;
                    GUI.setTitle(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getMenuTitle().replace("{page-number}", ConfigUtil.getMenuShopPageNumber().replace("{number}", currentPage.toString()))));
                }

                ((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
                GUI.update();
            }
            // Back Button
        } else if (e.getSlot() == 53 && !ConfigUtil.isEscapeOnly()) {
            player.closeInventory();
        } else {
            openShop(pl, Main.getINSTANCE().getLoadedMenu().getPages().get("Page" + currentPane.getPage()).getItems().get(((Integer) e.getSlot()).toString()).getTarget_shop());
        }

    }

    public Boolean hasMultiplePages() {
        return this.menuItem.getPages().size() > 1;
    }

    public Shop openShop(Player player, String shop) {
        /*
         * The currently open shop associated with this Menu instance.
         */
        Shop openShop = new Shop(player, shop, this);

        openShop.loadItems();
        openShop.open(player);
        return openShop;

    }

    private void onClose(InventoryCloseEvent e) {
        if (!hasClicked) {
            Player p = (Player) e.getPlayer();
            if (Main.getCREATOR().contains(p.getName())) {
                Main.getCREATOR().remove(p.getName());
            }
        }
    }

}
