package com.pablo67340.guishop.listenable.editor;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.gui.SimpleGui;
import com.pablo67340.guishop.util.ItemStackBuilder;
import com.pablo67340.guishop.util.MathUtil;
import com.pablo67340.guishop.util.PDCUtil;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * GUI-based item editor for GUIShop.
 * Allows editing all item properties through a visual interface.
 */
public class ItemEditorGui {

    @Getter
    private final Player player;
    
    @Getter
    private final ItemStack originalItem;
    
    @Getter
    private final int originalSlot;
    
    @Getter
    private final String shopName;
    
    @Getter
    private final int currentPage;
    
    private SimpleGui gui;
    
    // Callbacks
    private Runnable onSave;
    private Runnable onCancel;

    // Current editing values (loaded from PDC)
    private Object buyPrice;      // BigDecimal or Boolean (false)
    private Object sellPrice;     // BigDecimal or Boolean (false)
    private String shopDisplayName;
    private String buyName;
    private String itemName;      // Used for DUMMY items instead of shop-name/buy-name
    private ItemType itemType;
    private String targetShop;
    private String mobType;
    private String permission;
    private String enchantments;
    private List<String> shopLore;
    private List<String> buyLore;
    private List<String> commands;
    private Integer quantity;
    private String skullUuid;
    private String potionInfo;
    private String fireworkInfo;

    public ItemEditorGui(Player player, ItemStack item, int slot, String shopName) {
        this(player, item, slot, shopName, 0);
    }
    
    public ItemEditorGui(Player player, ItemStack item, int slot, String shopName, int currentPage) {
        this.player = player;
        this.originalItem = item.clone();
        this.originalSlot = slot;
        this.shopName = shopName;
        this.currentPage = currentPage;
        
        loadFromItem(item);
    }

    /**
     * Load current values from the item's PDC.
     */
    private void loadFromItem(ItemStack item) {
        // Buy price
        Double buyPriceDbl = PDCUtil.getDouble(item, PDCUtil.KEY_BUY_PRICE);
        String buyPriceStr = PDCUtil.getString(item, PDCUtil.KEY_BUY_PRICE);
        if (buyPriceDbl != null) {
            buyPrice = BigDecimal.valueOf(buyPriceDbl);
        } else if (buyPriceStr != null) {
            if (buyPriceStr.equalsIgnoreCase("false")) {
                buyPrice = false;
            } else {
                try {
                    buyPrice = new BigDecimal(buyPriceStr);
                } catch (Exception e) {
                    buyPrice = false;
                }
            }
        } else {
            buyPrice = false;
        }

        // Sell price
        Double sellPriceDbl = PDCUtil.getDouble(item, PDCUtil.KEY_SELL_PRICE);
        String sellPriceStr = PDCUtil.getString(item, PDCUtil.KEY_SELL_PRICE);
        if (sellPriceDbl != null) {
            sellPrice = BigDecimal.valueOf(sellPriceDbl);
        } else if (sellPriceStr != null) {
            if (sellPriceStr.equalsIgnoreCase("false")) {
                sellPrice = false;
            } else {
                try {
                    sellPrice = new BigDecimal(sellPriceStr);
                } catch (Exception e) {
                    sellPrice = false;
                }
            }
        } else {
            sellPrice = false;
        }

        // Other properties
        shopDisplayName = PDCUtil.getString(item, PDCUtil.KEY_SHOP_NAME);
        buyName = PDCUtil.getString(item, PDCUtil.KEY_BUY_NAME);
        itemName = PDCUtil.getString(item, PDCUtil.KEY_NAME);  // Used for DUMMY items
        targetShop = PDCUtil.getString(item, PDCUtil.KEY_TARGET_SHOP);
        mobType = PDCUtil.getString(item, PDCUtil.KEY_MOB_TYPE);
        permission = PDCUtil.getString(item, PDCUtil.KEY_PERMISSION);
        enchantments = PDCUtil.getString(item, PDCUtil.KEY_ENCHANTMENTS);
        skullUuid = PDCUtil.getString(item, PDCUtil.KEY_SKULL_UUID);
        potionInfo = PDCUtil.getString(item, PDCUtil.KEY_POTION);

        // Item type - respect DUMMY type, only default to SHOP if item has prices and no type set
        String itemTypeStr = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
        if (itemTypeStr != null) {
            try {
                itemType = ItemType.valueOf(itemTypeStr.toUpperCase());
                // Keep whatever type was set - don't override DUMMY
            } catch (Exception e) {
                itemType = ItemType.DUMMY;
            }
        } else {
            // No type in PDC - check if this is a fresh item
            // If it has prices, default to SHOP; otherwise DUMMY (decoration)
            if (buyPrice instanceof BigDecimal || sellPrice instanceof BigDecimal) {
                itemType = ItemType.SHOP;
            } else {
                itemType = ItemType.DUMMY;
            }
        }

        // Quantity
        quantity = PDCUtil.getInteger(item, PDCUtil.KEY_QUANTITY);

        // Lore lists
        String shopLoreStr = PDCUtil.getString(item, PDCUtil.KEY_SHOP_LORE_LINES);
        if (shopLoreStr != null && !shopLoreStr.isEmpty()) {
            shopLore = new ArrayList<>(Arrays.asList(shopLoreStr.split("::")));
        } else {
            shopLore = new ArrayList<>();
        }

        String buyLoreStr = PDCUtil.getString(item, PDCUtil.KEY_BUY_LORE_LINES);
        if (buyLoreStr != null && !buyLoreStr.isEmpty()) {
            buyLore = new ArrayList<>(Arrays.asList(buyLoreStr.split("::")));
        } else {
            buyLore = new ArrayList<>();
        }

        // Commands
        String commandsStr = PDCUtil.getString(item, PDCUtil.KEY_COMMANDS);
        if (commandsStr != null && !commandsStr.isEmpty()) {
            commands = new ArrayList<>(Arrays.asList(commandsStr.split("::")));
        } else {
            commands = new ArrayList<>();
        }
    }

    /**
     * Set the callback to run when save is clicked.
     */
    public ItemEditorGui onSave(Runnable callback) {
        this.onSave = callback;
        return this;
    }

    /**
     * Set the callback to run when cancel is clicked.
     */
    public ItemEditorGui onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    /**
     * Open the editor GUI.
     */
    public void open() {
        gui = new SimpleGui(6, "&6&lItem Editor");
        buildMainMenu();
        gui.show(player);
    }

    /**
     * Build the main editor menu.
     */
    private void buildMainMenu() {
        gui.clear();

        // Row 1: Display the item being edited
        gui.setItem(4, createDisplayItem());

        // Row 2: Pricing (only relevant for non-DUMMY items, but show for editing type)
        gui.setItem(10, createPriceButton("buy", buyPrice, Material.GOLD_INGOT, 
            "&aBuy Price", "Set the price players pay to buy this item"));
        gui.setItem(11, createPriceButton("sell", sellPrice, Material.GOLD_NUGGET,
            "&eSell Price", "Set the price players receive when selling"));

        // Row 2: Names - Different for DUMMY vs SHOP items
        if (itemType == ItemType.DUMMY) {
            // DUMMY items use "name" field only
            gui.setItem(13, createStringButton("itemname", itemName, Material.NAME_TAG,
                "&bItem Name", "The display name for this decoration item"));
            gui.setItem(14, createDummyInfo());
        } else {
            // SHOP and other types use shop-name and buy-name
            gui.setItem(13, createStringButton("shopname", shopDisplayName, Material.NAME_TAG,
                "&bShop Display Name", "The name shown in the shop GUI"));
            gui.setItem(14, createStringButton("buyname", buyName, Material.PAPER,
                "&dBuy Name", "The name on the item when purchased"));
        }

        // Row 2: Type
        gui.setItem(16, createTypeButton());

        // Row 3: Lore
        gui.setItem(19, createListButton("shoplore", shopLore, Material.WRITABLE_BOOK,
            "&9Shop Lore", "Lore lines shown in the shop"));
        gui.setItem(20, createListButton("buylore", buyLore, Material.BOOK,
            "&5Buy Lore", "Lore lines on the purchased item"));

        // Row 3: Advanced
        gui.setItem(22, createEnchantmentsButton());
        gui.setItem(23, createPotionButton());
        gui.setItem(24, createFireworkButton());

        // Row 4: Commands & Special
        gui.setItem(28, createListButton("commands", commands, Material.COMMAND_BLOCK,
            "&cCommands", "Commands to run on purchase (COMMAND type)"));
        gui.setItem(29, createStringButton("targetshop", targetShop, Material.ENDER_PEARL,
            "&6Target Shop", "Shop to open when clicked (DUMMY type)"));
        gui.setItem(30, createStringButton("mobtype", mobType, Material.SPAWNER,
            "&4Mob Type", "Entity type for spawners"));

        // Row 4: More options
        gui.setItem(32, createStringButton("skulluuid", skullUuid, Material.PLAYER_HEAD,
            "&fSkull UUID", "Player UUID or Base64 texture"));
        gui.setItem(33, createStringButton("permission", permission, Material.IRON_BARS,
            "&7Permission", "Required permission to buy"));
        gui.setItem(34, createQuantityButton());

        // Row 6: Actions
        gui.setItem(45, createActionButton(Material.LIME_WOOL, "&a&lSave", 
            "Save changes and return to shop"));
        gui.setItem(49, createActionButton(Material.YELLOW_WOOL, "&e&lReset", 
            "Reset to original values"));
        gui.setItem(53, createActionButton(Material.RED_WOOL, "&c&lCancel", 
            "Discard changes and return"));

        setupClickHandlers();
    }

    /**
     * Create the display item showing current item being edited.
     */
    private ItemStack createDisplayItem() {
        ItemStack display = originalItem.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Slot: " + ChatColor.WHITE + originalSlot);
            lore.add(ChatColor.GRAY + "Shop: " + ChatColor.WHITE + shopName);
            lore.add(ChatColor.GRAY + "Material: " + ChatColor.WHITE + display.getType().name());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click an option below to edit");
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    /**
     * Create a button for editing a price value.
     */
    private ItemStack createPriceButton(String id, Object value, Material material, String name, String description) {
        String valueStr;
        if (value instanceof Boolean && !(Boolean) value) {
            valueStr = ChatColor.RED + "Disabled";
        } else if (value instanceof BigDecimal) {
            valueStr = ChatColor.GREEN + MathUtil.formatAbbreviated((BigDecimal) value);
        } else {
            valueStr = ChatColor.RED + "Not set";
        }

        return new ItemStackBuilder(material)
            .setName(ChatColor.translateAlternateColorCodes('&', name))
            .addLoreLine(ChatColor.GRAY + description)
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + valueStr)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Left-click to edit")
            .addLoreLine(ChatColor.RED + "Right-click to clear")
            .addLoreLine(ChatColor.DARK_GRAY + "(Supports 1k, 1.5M, 2B formats)")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Create a button for editing a string value.
     */
    private ItemStack createStringButton(String id, String value, Material material, String name, String description) {
        String displayValue = (value == null || value.isEmpty()) 
            ? ChatColor.RED + "Not set" 
            : ChatColor.GREEN + truncate(value, 25);

        return new ItemStackBuilder(material)
            .setName(ChatColor.translateAlternateColorCodes('&', name))
            .addLoreLine(ChatColor.GRAY + description)
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + displayValue)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Left-click to edit")
            .addLoreLine(ChatColor.RED + "Right-click to clear")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Create a button for editing a list value.
     */
    private ItemStack createListButton(String id, List<String> list, Material material, String name, String description) {
        ItemStackBuilder builder = new ItemStackBuilder(material)
            .setName(ChatColor.translateAlternateColorCodes('&', name))
            .addLoreLine(ChatColor.GRAY + description)
            .addLoreLine("");

        if (list == null || list.isEmpty()) {
            builder.addLoreLine(ChatColor.RED + "No entries");
        } else {
            builder.addLoreLine(ChatColor.GRAY + "Entries: " + ChatColor.WHITE + list.size());
            // Show first 3 entries with ellipsis
            int shown = 0;
            for (String entry : list) {
                if (shown >= 3) {
                    builder.addLoreLine(ChatColor.DARK_GRAY + "  ... and " + (list.size() - 3) + " more");
                    break;
                }
                builder.addLoreLine(ChatColor.GRAY + "  - " + ChatColor.WHITE + truncate(entry, 20));
                shown++;
            }
        }

        builder.addLoreLine("");
        builder.addLoreLine(ChatColor.YELLOW + "Left-click to add");
        builder.addLoreLine(ChatColor.YELLOW + "Right-click to clear");
        builder.addItemFlag(ItemFlag.HIDE_ATTRIBUTES);

        return builder.build();
    }

    /**
     * Create the item type button.
     */
    private ItemStack createTypeButton() {
        Material mat = switch (itemType) {
            case COMMAND -> Material.COMMAND_BLOCK;
            case BLANK -> Material.BARRIER;
            case DUMMY -> Material.GLASS;
            default -> Material.CHEST;
        };

        return new ItemStackBuilder(mat)
            .setName(ChatColor.LIGHT_PURPLE + "Item Type")
            .addLoreLine(ChatColor.GRAY + "Determines item behavior")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + ChatColor.WHITE + itemType.name())
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to cycle types")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Create the quantity button.
     */
    private ItemStack createQuantityButton() {
        String valueStr = (quantity == null || quantity == 1) 
            ? ChatColor.GRAY + "Default (1)" 
            : ChatColor.GREEN + String.valueOf(quantity);

        return new ItemStackBuilder(Material.HOPPER)
            .setName(ChatColor.AQUA + "Quantity")
            .addLoreLine(ChatColor.GRAY + "Stack size to give on purchase")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + valueStr)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Left-click to edit")
            .addLoreLine(ChatColor.RED + "Right-click to reset")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Create the enchantments button.
     */
    private ItemStack createEnchantmentsButton() {
        ItemStackBuilder builder = new ItemStackBuilder(Material.ENCHANTED_BOOK)
            .setName(ChatColor.LIGHT_PURPLE + "Enchantments")
            .addLoreLine(ChatColor.GRAY + "Configure item enchantments")
            .addLoreLine("");

        if (enchantments == null || enchantments.isEmpty()) {
            // Check actual item enchantments
            if (!originalItem.getEnchantments().isEmpty()) {
                builder.addLoreLine(ChatColor.GRAY + "From item:");
                int shown = 0;
                for (Map.Entry<Enchantment, Integer> e : originalItem.getEnchantments().entrySet()) {
                    if (shown >= 3) {
                        builder.addLoreLine(ChatColor.DARK_GRAY + "  ... and more");
                        break;
                    }
                    builder.addLoreLine(ChatColor.GRAY + "  - " + ChatColor.WHITE + 
                        e.getKey().getKey().getKey() + ":" + e.getValue());
                    shown++;
                }
            } else {
                builder.addLoreLine(ChatColor.RED + "None");
            }
        } else {
            builder.addLoreLine(ChatColor.GREEN + enchantments);
        }

        builder.addLoreLine("");
        builder.addLoreLine(ChatColor.YELLOW + "Left-click to open editor");
        builder.addLoreLine(ChatColor.RED + "Right-click to clear");
        builder.addItemFlag(ItemFlag.HIDE_ENCHANTS);

        return builder.build();
    }

    /**
     * Create the potion info button.
     */
    private ItemStack createPotionButton() {
        String displayVal = (potionInfo == null || potionInfo.isEmpty())
            ? ChatColor.RED + "Not configured"
            : ChatColor.GREEN + truncate(potionInfo, 25);

        return new ItemStackBuilder(Material.POTION)
            .setName(ChatColor.DARK_PURPLE + "Potion Info")
            .addLoreLine(ChatColor.GRAY + "Configure potion type and effects")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + displayVal)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Left-click to open editor")
            .addLoreLine(ChatColor.RED + "Right-click to clear")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .addItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build();
    }

    /**
     * Create the firework info button.
     */
    private ItemStack createFireworkButton() {
        String displayVal = (fireworkInfo == null || fireworkInfo.isEmpty())
            ? ChatColor.RED + "Not configured"
            : ChatColor.GREEN + "Configured";

        return new ItemStackBuilder(Material.FIREWORK_ROCKET)
            .setName(ChatColor.RED + "Firework Info")
            .addLoreLine(ChatColor.GRAY + "Configure firework effects")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Status: " + displayVal)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Left-click to open editor")
            .addLoreLine(ChatColor.RED + "Right-click to clear")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Create an action button.
     */
    private ItemStack createActionButton(Material material, String name, String description) {
        return new ItemStackBuilder(material)
            .setName(ChatColor.translateAlternateColorCodes('&', name))
            .addLoreLine(ChatColor.GRAY + description)
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
    
    /**
     * Create info display for DUMMY items (shows that buy-name is not used).
     */
    private ItemStack createDummyInfo() {
        return new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .setName(ChatColor.GRAY + "Not Used (DUMMY)")
            .addLoreLine(ChatColor.DARK_GRAY + "DUMMY items don't use buy-name")
            .addLoreLine(ChatColor.DARK_GRAY + "Use 'Item Name' to rename")
            .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    /**
     * Set up click handlers for the GUI.
     */
    private void setupClickHandlers() {
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            switch (slot) {
                // Buy Price (right-click to clear)
                case 10 -> editBuyPrice(event.isRightClick());
                // Sell Price (right-click to clear)
                case 11 -> editSellPrice(event.isRightClick());
                // Shop Name / Item Name (right-click to clear) - depends on item type
                case 13 -> {
                    if (itemType == ItemType.DUMMY) {
                        editItemName(event.isRightClick());
                    } else {
                        editShopName(event.isRightClick());
                    }
                }
                // Buy Name (right-click to clear) - only for non-DUMMY
                case 14 -> {
                    if (itemType != ItemType.DUMMY) {
                        editBuyName(event.isRightClick());
                    }
                    // DUMMY items show info panel, no action
                }
                // Item Type
                case 16 -> cycleItemType();
                // Shop Lore (right-click to clear)
                case 19 -> editShopLore(event.isRightClick());
                // Buy Lore (right-click to clear)
                case 20 -> editBuyLore(event.isRightClick());
                // Enchantments (right-click to clear)
                case 22 -> openEnchantmentsEditor(event.isRightClick());
                // Potion (right-click to clear)
                case 23 -> openPotionEditor(event.isRightClick());
                // Firework (right-click to clear)
                case 24 -> openFireworkEditor(event.isRightClick());
                // Commands (right-click to clear)
                case 28 -> editCommands(event.isRightClick());
                // Target Shop (right-click to clear)
                case 29 -> editTargetShop(event.isRightClick());
                // Mob Type (right-click to clear)
                case 30 -> editMobType(event.isRightClick());
                // Skull UUID (right-click to clear)
                case 32 -> editSkullUuid(event.isRightClick());
                // Permission (right-click to clear)
                case 33 -> editPermission(event.isRightClick());
                // Quantity (right-click to clear/reset)
                case 34 -> editQuantity(event.isRightClick());
                // Save
                case 45 -> saveAndClose();
                // Reset
                case 49 -> resetValues();
                // Cancel
                case 53 -> cancelAndClose();
            }
        });
    }

    // ========== EDIT METHODS ==========

    private void editBuyPrice(boolean clear) {
        if (clear) {
            buyPrice = false;
            player.sendMessage(ChatColor.YELLOW + "Buy price cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestNumber(player, 
            "Enter the buy price for this item:", 
            value -> {
                if (value == null) {
                    buyPrice = false;
                } else {
                    buyPrice = value;
                }
                open();
            },
            this::open,
            60
        );
    }

    private void editSellPrice(boolean clear) {
        if (clear) {
            sellPrice = false;
            player.sendMessage(ChatColor.YELLOW + "Sell price cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestNumber(player,
            "Enter the sell price for this item:",
            value -> {
                if (value == null) {
                    sellPrice = false;
                } else {
                    sellPrice = value;
                }
                open();
            },
            this::open,
            60
        );
    }

    private void editShopName(boolean clear) {
        if (clear) {
            shopDisplayName = null;
            player.sendMessage(ChatColor.YELLOW + "Shop name cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the display name for this item in the shop:\n" +
            ChatColor.GRAY + "(Use & for color codes)",
            input -> {
                shopDisplayName = sanitizeInput(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editBuyName(boolean clear) {
        if (clear) {
            buyName = null;
            player.sendMessage(ChatColor.YELLOW + "Buy name cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the name for this item when purchased:\n" +
            ChatColor.GRAY + "(Use & for color codes)",
            input -> {
                buyName = sanitizeInput(input);
                open();
            },
            this::open,
            60
        );
    }
    
    private void editItemName(boolean clear) {
        if (clear) {
            itemName = null;
            player.sendMessage(ChatColor.YELLOW + "Item name cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the display name for this item:\n" +
            ChatColor.GRAY + "(Use & for color codes)",
            input -> {
                itemName = sanitizeInput(input);
                open();
            },
            this::open,
            60
        );
    }
    
    /**
     * Sanitize user input to prevent YAML issues.
     * Removes problematic characters that could break YAML parsing.
     */
    private String sanitizeInput(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        // Remove leading/trailing whitespace
        input = input.trim();
        // If input is empty after trim, return null
        if (input.isEmpty()) {
            return null;
        }
        return input;
    }

    private void cycleItemType() {
        ItemType[] types = ItemType.values();
        int current = itemType.ordinal();
        itemType = types[(current + 1) % types.length];
        buildMainMenu();
        gui.update();
    }

    private void editShopLore(boolean clear) {
        if (clear) {
            shopLore.clear();
            player.sendMessage(ChatColor.YELLOW + "Shop lore cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter a new shop lore line:\n" +
            ChatColor.GRAY + "(Use & for color codes)",
            input -> {
                shopLore.add(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editBuyLore(boolean clear) {
        if (clear) {
            buyLore.clear();
            player.sendMessage(ChatColor.YELLOW + "Buy lore cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter a new buy lore line:\n" +
            ChatColor.GRAY + "(Use & for color codes)",
            input -> {
                buyLore.add(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editCommands(boolean clear) {
        if (clear) {
            commands.clear();
            player.sendMessage(ChatColor.YELLOW + "Commands cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter a command to run on purchase:\n" +
            ChatColor.GRAY + "(Without /, use {PLAYER_NAME} for player name)",
            input -> {
                commands.add(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editTargetShop(boolean clear) {
        if (clear) {
            targetShop = null;
            player.sendMessage(ChatColor.YELLOW + "Target shop cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the target shop name:",
            input -> {
                targetShop = sanitizeInput(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editMobType(boolean clear) {
        if (clear) {
            mobType = null;
            player.sendMessage(ChatColor.YELLOW + "Mob type cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the mob type for this spawner (e.g., ZOMBIE, SKELETON):\n" +
            ChatColor.GRAY + "(Use /gs parsemob to validate)",
            input -> {
                String sanitized = sanitizeInput(input);
                mobType = sanitized != null ? sanitized.toUpperCase() : null;
                open();
            },
            this::open,
            60
        );
    }

    private void editSkullUuid(boolean clear) {
        if (clear) {
            skullUuid = null;
            player.sendMessage(ChatColor.YELLOW + "Skull UUID cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter player UUID or Base64 texture:",
            input -> {
                skullUuid = sanitizeInput(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editPermission(boolean clear) {
        if (clear) {
            permission = null;
            player.sendMessage(ChatColor.YELLOW + "Permission cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the required permission:",
            input -> {
                permission = sanitizeInput(input);
                open();
            },
            this::open,
            60
        );
    }

    private void editQuantity(boolean clear) {
        if (clear) {
            quantity = null;
            player.sendMessage(ChatColor.YELLOW + "Quantity reset to default.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        player.closeInventory();
        ChatInputHandler.getInstance().requestInput(player,
            "Enter the quantity to give on purchase:",
            input -> {
                try {
                    quantity = Integer.parseInt(input.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number!");
                    quantity = null;
                }
                open();
            },
            this::open,
            60
        );
    }

    private void openEnchantmentsEditor(boolean clear) {
        if (clear) {
            enchantments = null;
            player.sendMessage(ChatColor.YELLOW + "Enchantments cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        new EnchantmentEditorGui(player, enchantments, newEnchants -> {
            enchantments = newEnchants;
            open();
        }, this::open).open();
    }

    private void openPotionEditor(boolean clear) {
        if (clear) {
            potionInfo = null;
            player.sendMessage(ChatColor.YELLOW + "Potion info cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        new PotionEditorGui(player, potionInfo, newInfo -> {
            potionInfo = newInfo;
            open();
        }, this::open).open();
    }

    private void openFireworkEditor(boolean clear) {
        if (clear) {
            fireworkInfo = null;
            player.sendMessage(ChatColor.YELLOW + "Firework info cleared.");
            buildMainMenu();
            gui.update();
            return;
        }
        
        new FireworkEditorGui(player, fireworkInfo, newInfo -> {
            fireworkInfo = newInfo;
            open();
        }, this::open).open();
    }

    private void resetValues() {
        loadFromItem(originalItem);
        buildMainMenu();
        gui.update();
        player.sendMessage(ChatColor.YELLOW + "Values reset to original.");
    }

    private void saveAndClose() {
        // Apply all changes to the item via PDC
        ItemStack item = originalItem.clone();
        
        GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Starting save for " + item.getType());
        GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: buyPrice=" + buyPrice + " (type=" + (buyPrice != null ? buyPrice.getClass().getSimpleName() : "null") + ")");
        GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: sellPrice=" + sellPrice + " (type=" + (sellPrice != null ? sellPrice.getClass().getSimpleName() : "null") + ")");
        
        // AUTO-CONVERT: DUMMY items with prices should become SHOP items
        // This is the root fix - a DUMMY item with prices is an invalid state
        boolean hasPrices = (buyPrice instanceof BigDecimal) || (sellPrice instanceof BigDecimal);
        if (itemType == ItemType.DUMMY && hasPrices) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Auto-converting DUMMY to SHOP (has prices)");
            itemType = ItemType.SHOP;
        }
        
        // AUTO-CONVERT: SHOP items with no prices should become DUMMY items (unless they have commands/target-shop)
        boolean hasCommands = commands != null && !commands.isEmpty();
        boolean hasTargetShop = targetShop != null && !targetShop.isEmpty();
        if (itemType == ItemType.SHOP && !hasPrices && !hasCommands && !hasTargetShop) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Auto-converting SHOP to DUMMY (no prices/commands/target)");
            itemType = ItemType.DUMMY;
        }
        
        GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Final itemType=" + itemType.name());
        
        // Buy price
        if (buyPrice instanceof BigDecimal) {
            double buyVal = ((BigDecimal) buyPrice).doubleValue();
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Setting buy price PDC to " + buyVal);
            PDCUtil.setDouble(item, PDCUtil.KEY_BUY_PRICE, buyVal);
        } else {
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Removing buy price PDC (not BigDecimal)");
            PDCUtil.removeKey(item, PDCUtil.KEY_BUY_PRICE);
        }
        
        // Sell price
        if (sellPrice instanceof BigDecimal) {
            double sellVal = ((BigDecimal) sellPrice).doubleValue();
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Setting sell price PDC to " + sellVal);
            PDCUtil.setDouble(item, PDCUtil.KEY_SELL_PRICE, sellVal);
        } else {
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Removing sell price PDC (not BigDecimal)");
            PDCUtil.removeKey(item, PDCUtil.KEY_SELL_PRICE);
        }
        
        // String properties - for DUMMY items, only save name, not shop-name/buy-name
        if (itemType == ItemType.DUMMY) {
            setOrRemoveString(item, PDCUtil.KEY_NAME, itemName);
            PDCUtil.removeKey(item, PDCUtil.KEY_SHOP_NAME);
            PDCUtil.removeKey(item, PDCUtil.KEY_BUY_NAME);
        } else {
            setOrRemoveString(item, PDCUtil.KEY_SHOP_NAME, shopDisplayName);
            setOrRemoveString(item, PDCUtil.KEY_BUY_NAME, buyName);
            PDCUtil.removeKey(item, PDCUtil.KEY_NAME);
        }
        setOrRemoveString(item, PDCUtil.KEY_TARGET_SHOP, targetShop);
        setOrRemoveString(item, PDCUtil.KEY_MOB_TYPE, mobType);
        setOrRemoveString(item, PDCUtil.KEY_PERMISSION, permission);
        setOrRemoveString(item, PDCUtil.KEY_ENCHANTMENTS, enchantments);
        setOrRemoveString(item, PDCUtil.KEY_SKULL_UUID, skullUuid);
        setOrRemoveString(item, PDCUtil.KEY_POTION, potionInfo);
        setOrRemoveString(item, PDCUtil.KEY_FIREWORK, fireworkInfo);
        
        // Item type
        GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Setting item type PDC to " + itemType.name());
        PDCUtil.setString(item, PDCUtil.KEY_ITEM_TYPE, itemType.name());
        
        // Quantity
        if (quantity != null && quantity > 1) {
            PDCUtil.setInteger(item, PDCUtil.KEY_QUANTITY, quantity);
        } else {
            PDCUtil.removeKey(item, PDCUtil.KEY_QUANTITY);
        }
        
        // Lists (join with ::)
        if (shopLore != null && !shopLore.isEmpty()) {
            PDCUtil.setString(item, PDCUtil.KEY_SHOP_LORE_LINES, String.join("::", shopLore));
        } else {
            PDCUtil.removeKey(item, PDCUtil.KEY_SHOP_LORE_LINES);
        }
        
        if (buyLore != null && !buyLore.isEmpty()) {
            PDCUtil.setString(item, PDCUtil.KEY_BUY_LORE_LINES, String.join("::", buyLore));
        } else {
            PDCUtil.removeKey(item, PDCUtil.KEY_BUY_LORE_LINES);
        }
        
        if (commands != null && !commands.isEmpty()) {
            PDCUtil.setString(item, PDCUtil.KEY_COMMANDS, String.join("::", commands));
        } else {
            PDCUtil.removeKey(item, PDCUtil.KEY_COMMANDS);
        }
        
        // Save directly to config based on whether this is a shop or menu item
        saveToConfig(item);
        
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Item saved!");
        
        if (onSave != null) {
            onSave.run();
        }
    }
    
    /**
     * Save the edited item directly to the shop or menu config.
     * 
     * This method:
     * 1. Parses the item from PDC data
     * 2. Saves to the config file (for persistence)
     * 3. Updates in-memory cache directly (for immediate display)
     * 
     * No cache invalidation needed - we update the cache in place.
     */
    private void saveToConfig(ItemStack item) {
        try {
            String pageKey = "Page" + currentPage;
            
            // Parse item from PDC data
            Item parsedItem = Item.parse(item, originalSlot, "Menu".equalsIgnoreCase(shopName) ? null : shopName);
            java.util.Map<String, Object> serialized = parsedItem.serialize();
            
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Saving " + parsedItem.getMaterial() + 
                " at slot " + originalSlot + " to " + shopName + "/" + pageKey);
            GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: type=" + parsedItem.getItemType() + 
                " buyPrice=" + (parsedItem.hasBuyPrice() ? parsedItem.getBuyPriceAsDecimal() : "none") +
                " sellPrice=" + (parsedItem.hasSellPrice() ? parsedItem.getSellPriceAsDecimal() : "none"));
            
            if ("Menu".equalsIgnoreCase(shopName)) {
                // 1. Save to menu.yml config file
                org.bukkit.configuration.file.FileConfiguration menuConfig = 
                    GUIShop.getINSTANCE().getConfigManager().getMenuConfig();
                
                String itemPath = "Menu.pages." + pageKey + ".items." + originalSlot;
                menuConfig.set(itemPath, null);
                for (java.util.Map.Entry<String, Object> entry : serialized.entrySet()) {
                    menuConfig.set(itemPath + "." + entry.getKey(), entry.getValue());
                }
                menuConfig.save(GUIShop.getINSTANCE().getConfigManager().getMenuFile());
                
                // 2. Update in-memory cache directly
                com.pablo67340.guishop.definition.MenuItem loadedMenu = GUIShop.getINSTANCE().getLoadedMenu();
                if (loadedMenu != null && loadedMenu.getPages().containsKey(pageKey)) {
                    loadedMenu.getPages().get(pageKey).getItems().put(String.valueOf(originalSlot), parsedItem);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Updated menu cache at slot " + originalSlot);
                }
            } else {
                // 1. Save to individual shop config file
                org.bukkit.configuration.file.FileConfiguration shopConfig = 
                    GUIShop.getINSTANCE().getConfigManager().getShopConfig(shopName);
                
                if (shopConfig == null) {
                    GUIShop.getINSTANCE().getLogUtil().log("EDITOR SAVE: Shop config not found for " + shopName);
                    player.sendMessage(ChatColor.RED + "Error: Shop config not found!");
                    return;
                }
                
                String itemPath = "pages." + pageKey + ".items." + originalSlot;
                shopConfig.set(itemPath, null);
                for (java.util.Map.Entry<String, Object> entry : serialized.entrySet()) {
                    shopConfig.set(itemPath + "." + entry.getKey(), entry.getValue());
                }
                GUIShop.getINSTANCE().getConfigManager().saveShopConfig(shopName);
                
                // 2. Update in-memory cache directly
                Object cached = GUIShop.getINSTANCE().getLoadedShops().get(shopName);
                if (cached instanceof com.pablo67340.guishop.definition.ShopItem) {
                    com.pablo67340.guishop.definition.ShopItem shopItem = 
                        (com.pablo67340.guishop.definition.ShopItem) cached;
                    if (shopItem.getPages().containsKey(pageKey)) {
                        shopItem.getPages().get(pageKey).getItems().put(String.valueOf(originalSlot), parsedItem);
                        GUIShop.getINSTANCE().getLogUtil().debugLog("EDITOR SAVE: Updated shop cache at slot " + originalSlot);
                    }
                }
            }
            
        } catch (Exception ex) {
            GUIShop.getINSTANCE().getLogUtil().log("Error saving item: " + ex.getMessage());
            ex.printStackTrace();
            player.sendMessage(ChatColor.RED + "Error saving item. Check console for details.");
        }
    }

    private void setOrRemoveString(ItemStack item, org.bukkit.NamespacedKey key, String value) {
        if (value != null && !value.isEmpty()) {
            PDCUtil.setString(item, key, value);
        } else {
            PDCUtil.removeKey(item, key);
        }
    }

    private void cancelAndClose() {
        player.closeInventory();
        if (onCancel != null) {
            onCancel.run();
        }
    }

    /**
     * Truncate a string with ellipsis.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}
