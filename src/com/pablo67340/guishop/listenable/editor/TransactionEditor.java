package com.pablo67340.guishop.listenable.editor;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.gui.SimpleGui;
import com.pablo67340.guishop.util.PDCUtil;
import com.pablo67340.guishop.util.SchedulerUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor for the Transaction GUI layout.
 * Works exactly like Shop/Menu editors:
 * - Left-click: Move items around
 * - Right-click or Shift+click: Open ItemEditorGui
 * - Close: Save changes
 */
public class TransactionEditor {

    private final Player player;
    private SimpleGui gui;
    private boolean clickOverride = false;
    
    // PDC key for storing transaction slot type
    public static final org.bukkit.NamespacedKey KEY_SLOT_TYPE = 
        new org.bukkit.NamespacedKey("guishop", "transaction_slot_type");
    
    public TransactionEditor(Player player) {
        this.player = player;
    }

    public void open() {
        FileConfiguration config = GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        
        if (config == null) {
            player.sendMessage(ChatColor.RED + "Error: transaction.yml not loaded! Run /gs reload first.");
            GUIShop.getCREATOR().remove(player.getUniqueId());
            return;
        }
        
        int rows = config.getInt("rows", 3);
        String title = ChatColor.translateAlternateColorCodes('&', 
            config.getString("title", "&8Transaction Editor"));
        
        gui = new SimpleGui(rows, ChatColor.DARK_PURPLE + "[Editor] " + title);
        
        loadLayout();
        
        // Set up handlers EXACTLY like Shop.java
        gui.setTopClickHandler(this::creatorTopInventoryClick);
        gui.setBottomClickHandler(this::creatorPlayerInventoryClick);
        gui.setCloseHandler(this::onClose);
        
        // Allow item movement in creator mode
        gui.setAllowTopInventoryClick(true);
        gui.setAllowBottomInventoryClick(true);
        
        // Show editor instructions
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "=== Transaction GUI Editor ===");
        player.sendMessage(ChatColor.YELLOW + "Left-click" + ChatColor.GRAY + " - Move items");
        player.sendMessage(ChatColor.YELLOW + "Right-click/Shift-click" + ChatColor.GRAY + " - Open item settings");
        player.sendMessage(ChatColor.GRAY + "Close inventory to save changes.");
        player.sendMessage("");
        
        gui.show(player);
    }

    private void loadLayout() {
        FileConfiguration config = GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        ConfigurationSection layout = config.getConfigurationSection("layout");
        
        if (layout == null) {
            player.sendMessage(ChatColor.RED + "No layout section found in transaction.yml!");
            return;
        }
        
        for (String slotKey : layout.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(slotKey);
            } catch (NumberFormatException e) {
                continue;
            }
            
            ConfigurationSection slotConfig = layout.getConfigurationSection(slotKey);
            if (slotConfig == null) continue;
            
            String type = slotConfig.getString("type", "DUMMY");
            ItemStack item = createSlotItem(type, slotConfig);
            
            if (item != null) {
                // Store the slot type in both PDC keys for proper editor integration
                PDCUtil.setString(item, KEY_SLOT_TYPE, type);
                PDCUtil.setString(item, PDCUtil.KEY_ITEM_TYPE, type);
                gui.setItem(slot, item);
            }
        }
    }

    private ItemStack createSlotItem(String type, ConfigurationSection slotConfig) {
        ItemStack item;
        ItemMeta meta;
        List<String> lore = new ArrayList<>();
        
        switch (type.toUpperCase()) {
            case "ITEM_DISPLAY":
                item = new ItemStack(Material.NETHER_STAR);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Item Display");
                lore.add(ChatColor.GRAY + "The item being transacted");
                lore.add(ChatColor.GRAY + "will be shown here.");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "ITEM_DISPLAY");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "BUY_1":
                item = Config.getTransactionGuiConfig().getBuyMaterial().parseItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Buy x" + Config.getTransactionGuiConfig().getQuantities()[0]);
                lore.add(ChatColor.GRAY + "Buy button for quantity 1");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "BUY_1");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "BUY_2":
                item = Config.getTransactionGuiConfig().getBuyMaterial().parseItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Buy x" + Config.getTransactionGuiConfig().getQuantities()[1]);
                lore.add(ChatColor.GRAY + "Buy button for quantity 2");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "BUY_2");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "BUY_3":
                item = Config.getTransactionGuiConfig().getBuyMaterial().parseItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Buy x" + Config.getTransactionGuiConfig().getQuantities()[2]);
                lore.add(ChatColor.GRAY + "Buy button for quantity 3");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "BUY_3");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "SELL_1":
                item = Config.getTransactionGuiConfig().getSellMaterial().parseItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Sell x" + Config.getTransactionGuiConfig().getQuantities()[0]);
                lore.add(ChatColor.GRAY + "Sell button for quantity 1");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "SELL_1");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "SELL_2":
                item = Config.getTransactionGuiConfig().getSellMaterial().parseItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Sell x" + Config.getTransactionGuiConfig().getQuantities()[1]);
                lore.add(ChatColor.GRAY + "Sell button for quantity 2");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "SELL_2");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "SELL_3":
                item = Config.getTransactionGuiConfig().getSellMaterial().parseItem();
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Sell x" + Config.getTransactionGuiConfig().getQuantities()[2]);
                lore.add(ChatColor.GRAY + "Sell button for quantity 3");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "SELL_3");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "BACK":
                item = new ItemStack(Material.BARRIER);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Back Button");
                lore.add(ChatColor.GRAY + "Returns player to the shop.");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "BACK");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "PLAYER_HEAD":
                item = new ItemStack(Material.PLAYER_HEAD);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Player Head");
                lore.add(ChatColor.GRAY + "Shows player's head with");
                lore.add(ChatColor.GRAY + "balance information.");
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "PLAYER_HEAD");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
                
            case "DUMMY":
            default:
                // Load custom material from config
                String materialStr = slotConfig.getString("id", "BLACK_STAINED_GLASS_PANE");
                XMaterial xMat = XMaterial.matchXMaterial(materialStr).orElse(XMaterial.BLACK_STAINED_GLASS_PANE);
                item = xMat.parseItem();
                meta = item.getItemMeta();
                
                String name = slotConfig.getString("name", " ");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                
                lore.add("");
                lore.add(ChatColor.DARK_PURPLE + "Slot Type: " + ChatColor.WHITE + "DUMMY");
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
                return item;
        }
    }

    /**
     * Handle clicks in the player inventory.
     * Same as Shop.creatorPlayerInventoryClick.
     */
    private void creatorPlayerInventoryClick(InventoryClickEvent e) {
        boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                              e.getClick() == ClickType.SHIFT_LEFT || 
                              e.getClick() == ClickType.SHIFT_RIGHT;
        
        if (isEditClick) {
            e.setCancelled(true);
            return;
        }
        // Left-click = Allow normal item pickup for placing into editor
    }

    /**
     * Handle clicks in the transaction GUI being edited.
     * Same pattern as Shop.creatorTopInventoryClick.
     */
    private void creatorTopInventoryClick(InventoryClickEvent e) {
        ItemStack clickedItem = e.getCurrentItem();
        
        // Right-click or Shift+click on an existing item = Open Item Editor GUI
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            boolean isEditClick = e.getClick() == ClickType.RIGHT || 
                                  e.getClick() == ClickType.SHIFT_LEFT || 
                                  e.getClick() == ClickType.SHIFT_RIGHT;
            
            if (isEditClick) {
                e.setCancelled(true);
                clickOverride = true;
                player.closeInventory();
                
                // Open the Item Editor GUI - it will handle transaction slot types
                new ItemEditorGui(
                    player, 
                    clickedItem, 
                    e.getSlot(), 
                    "Transaction",
                    0
                ).onSave(() -> {
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    open();
                }).onCancel(() -> {
                    GUIShop.getCREATOR().add(player.getUniqueId());
                    open();
                }).open();
                return;
            }
        }
        
        // Left-click with cursor item onto empty slot = Place item
        if ((clickedItem == null || clickedItem.getType().isAir()) && 
            e.getCursor() != null && !e.getCursor().getType().isAir()) {
            // Run after the event to get the placed item
            SchedulerUtil.runAtEntityLater(player, () -> {
                ItemStack placedItem = e.getInventory().getItem(e.getSlot());
                if (placedItem != null) {
                    // Mark new items as DUMMY type (both keys for editor integration)
                    PDCUtil.setString(placedItem, KEY_SLOT_TYPE, ItemType.DUMMY.name());
                    PDCUtil.setString(placedItem, PDCUtil.KEY_ITEM_TYPE, ItemType.DUMMY.name());
                    saveSlot(e.getSlot(), placedItem);
                }
            }, 1L);
            return;
        }
        
        // Left-click to pick up an item = Delete from config
        if (clickedItem != null && !clickedItem.getType().isAir() && e.getClick() == ClickType.LEFT) {
            deleteSlot(e.getSlot());
            // Don't cancel - let them pick it up
        }
    }

    /**
     * Handle inventory close - save all changes.
     */
    private void onClose(InventoryCloseEvent event) {
        if (!clickOverride) {
            saveAllSlots();
            GUIShop.getCREATOR().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Transaction GUI layout saved!");
            GUIShop.getINSTANCE().getConfigManager().reloadTransactionConfig();
        }
        clickOverride = false;
    }

    /**
     * Save a single slot to config.
     */
    private void saveSlot(int slot, ItemStack item) {
        FileConfiguration config = GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        String path = "layout." + slot;
        
        // Check KEY_ITEM_TYPE first (set by ItemEditorGui), then fall back to KEY_SLOT_TYPE
        String slotType = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
        if (slotType == null) {
            slotType = PDCUtil.getString(item, KEY_SLOT_TYPE);
        }
        if (slotType == null) slotType = ItemType.DUMMY.name();
        
        config.set(path + ".type", slotType);
        
        if (ItemType.DUMMY.name().equalsIgnoreCase(slotType)) {
            config.set(path + ".id", item.getType().name());
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                config.set(path + ".name", item.getItemMeta().getDisplayName());
            } else {
                config.set(path + ".name", " ");
            }
        }
        
        try {
            config.save(GUIShop.getINSTANCE().getConfigManager().getTransactionFile());
        } catch (IOException e) {
            GUIShop.getINSTANCE().getLogUtil().log("Error saving transaction slot: " + e.getMessage());
        }
    }

    /**
     * Delete a slot from config.
     */
    private void deleteSlot(int slot) {
        FileConfiguration config = GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        config.set("layout." + slot, null);
        
        try {
            config.save(GUIShop.getINSTANCE().getConfigManager().getTransactionFile());
        } catch (IOException e) {
            GUIShop.getINSTANCE().getLogUtil().log("Error deleting transaction slot: " + e.getMessage());
        }
    }

    /**
     * Save all slots from the current GUI state.
     */
    private void saveAllSlots() {
        FileConfiguration config = GUIShop.getINSTANCE().getConfigManager().getTransactionConfig();
        int totalSlots = gui.getRows() * 9;
        
        for (int slot = 0; slot < totalSlots; slot++) {
            ItemStack item = gui.getInventory().getItem(slot);
            
            if (item == null || item.getType().isAir()) {
                config.set("layout." + slot, null);
                continue;
            }
            
            // Check KEY_ITEM_TYPE first (set by ItemEditorGui), then fall back to KEY_SLOT_TYPE
            String slotType = PDCUtil.getString(item, PDCUtil.KEY_ITEM_TYPE);
            if (slotType == null) {
                slotType = PDCUtil.getString(item, KEY_SLOT_TYPE);
            }
            if (slotType == null) slotType = ItemType.DUMMY.name();
            
            String path = "layout." + slot;
            config.set(path + ".type", slotType);
            
            if (ItemType.DUMMY.name().equalsIgnoreCase(slotType)) {
                config.set(path + ".id", item.getType().name());
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String name = item.getItemMeta().getDisplayName();
                    config.set(path + ".name", name);
                } else {
                    config.set(path + ".name", " ");
                }
            } else {
                // Functional slots don't need material/name - clear them
                config.set(path + ".id", null);
                config.set(path + ".name", null);
            }
        }
        
        try {
            config.save(GUIShop.getINSTANCE().getConfigManager().getTransactionFile());
        } catch (IOException e) {
            GUIShop.getINSTANCE().getLogUtil().log("Error saving transaction layout: " + e.getMessage());
        }
    }
}
