package com.pablo67340.guishop.listenable.editor;

import com.pablo67340.guishop.gui.SimpleGui;
import com.pablo67340.guishop.util.ItemStackBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI for editing potion info in GUIShop format.
 * Generates YAML-ready potion-info configuration.
 */
public class PotionEditorGui {

    private final Player player;
    private final Consumer<String> onSave;
    private final Runnable onCancel;
    
    private SimpleGui gui;
    private int page = 0;
    
    // Potion settings
    private String potionType = "SPEED";
    private boolean splash = false;
    private boolean extended = false;
    private boolean upgraded = false;
    
    private final List<PotionType> allPotionTypes;
    private boolean selectingType = false;

    public PotionEditorGui(Player player, String currentPotionInfo,
                           Consumer<String> onSave, Runnable onCancel) {
        this.player = player;
        this.onSave = onSave;
        this.onCancel = onCancel;
        
        // Get all potion types
        allPotionTypes = new ArrayList<>();
        for (PotionType type : PotionType.values()) {
            if (type != PotionType.WATER && type != PotionType.MUNDANE 
                && type != PotionType.THICK && type != PotionType.AWKWARD) {
                allPotionTypes.add(type);
            }
        }
        allPotionTypes.sort(Comparator.comparing(Enum::name));
        
        // Parse current potion info if exists
        if (currentPotionInfo != null && !currentPotionInfo.isEmpty()) {
            String[] parts = currentPotionInfo.split(" ");
            if (parts.length >= 1) potionType = parts[0];
            if (parts.length >= 2) splash = Boolean.parseBoolean(parts[1]);
            if (parts.length >= 3) extended = Boolean.parseBoolean(parts[2]);
            if (parts.length >= 4) upgraded = Boolean.parseBoolean(parts[3]);
        }
    }

    public void open() {
        gui = new SimpleGui(selectingType ? 6 : 3, "&d&lPotion Editor");
        if (selectingType) {
            buildTypeSelectionGui();
        } else {
            buildMainGui();
        }
        gui.show(player);
    }

    private void buildMainGui() {
        gui.clear();
        
        // Current potion display
        gui.setItem(4, createCurrentPotionDisplay());
        
        // Potion type selector - show actual potion color
        gui.setItem(10, createPotionTypeSelectorButton());
        
        // Splash toggle
        gui.setItem(12, new ItemStackBuilder(splash ? Material.SPLASH_POTION : Material.POTION)
            .setName(ChatColor.GOLD + "Splash")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + (splash ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to toggle")
            .addItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build());
        
        // Extended toggle
        gui.setItem(14, new ItemStackBuilder(Material.CLOCK)
            .setName(ChatColor.BLUE + "Extended Duration")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + (extended ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Cannot be combined with Upgraded")
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to toggle")
            .build());
        
        // Upgraded toggle
        gui.setItem(16, new ItemStackBuilder(Material.GLOWSTONE_DUST)
            .setName(ChatColor.LIGHT_PURPLE + "Upgraded (Level II)")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + (upgraded ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Cannot be combined with Extended")
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to toggle")
            .build());
        
        // Save button
        gui.setItem(22, new ItemStackBuilder(Material.LIME_WOOL)
            .setName(ChatColor.GREEN + "Save & Back")
            .addLoreLine("")
            .addLoreLine(ChatColor.DARK_GRAY + "Config output:")
            .addLoreLine(ChatColor.WHITE + buildPotionString())
            .build());
        
        // Clear button
        gui.setItem(20, new ItemStackBuilder(Material.BARRIER)
            .setName(ChatColor.RED + "Clear")
            .addLoreLine(ChatColor.GRAY + "Remove potion configuration")
            .build());
        
        // Cancel button
        gui.setItem(24, new ItemStackBuilder(Material.RED_WOOL)
            .setName(ChatColor.RED + "Cancel")
            .build());
        
        setupMainClickHandlers();
    }

    private void buildTypeSelectionGui() {
        gui.clear();
        
        // Back button
        gui.setItem(0, new ItemStackBuilder(Material.ARROW)
            .setName(ChatColor.YELLOW + "Back")
            .build());
        
        // Title
        gui.setItem(4, new ItemStackBuilder(Material.POTION)
            .setName(ChatColor.AQUA + "Select Potion Type")
            .addItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            .build());
        
        // Potion types (slots 9-44)
        int startIndex = page * 36;
        for (int i = 0; i < 36 && startIndex + i < allPotionTypes.size(); i++) {
            PotionType type = allPotionTypes.get(startIndex + i);
            boolean isSelected = type.name().equalsIgnoreCase(potionType);
            
            gui.setItem(9 + i, createPotionItem(type, isSelected));
        }
        
        // Pagination
        if (page > 0) {
            gui.setItem(45, new ItemStackBuilder(Material.ARROW)
                .setName(ChatColor.YELLOW + "Previous Page")
                .build());
        }
        
        if ((page + 1) * 36 < allPotionTypes.size()) {
            gui.setItem(53, new ItemStackBuilder(Material.ARROW)
                .setName(ChatColor.YELLOW + "Next Page")
                .build());
        }
        
        setupTypeSelectionClickHandlers();
    }

    private ItemStack createCurrentPotionDisplay() {
        Material material = splash ? Material.SPLASH_POTION : Material.POTION;
        ItemStack potion = new ItemStack(material);
        
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            // Apply the actual potion type
            try {
                PotionType type = PotionType.valueOf(potionType.toUpperCase());
                meta.setBasePotionType(type);
            } catch (Exception e) {
                // Fallback for older versions or invalid type
                try {
                    PotionType type = PotionType.valueOf(potionType.toUpperCase());
                    meta.getClass().getMethod("setBasePotionData", 
                        Class.forName("org.bukkit.potion.PotionData"))
                        .invoke(meta, Class.forName("org.bukkit.potion.PotionData")
                            .getConstructor(PotionType.class)
                            .newInstance(type));
                } catch (Exception ignored) {}
            }
            
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Current Configuration");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + potionType);
            lore.add(ChatColor.GRAY + "Splash: " + (splash ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            lore.add(ChatColor.GRAY + "Extended: " + (extended ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            lore.add(ChatColor.GRAY + "Upgraded: " + (upgraded ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "YAML output:");
            lore.add(ChatColor.WHITE + "potion-info:");
            lore.add(ChatColor.WHITE + "  type: " + potionType);
            lore.add(ChatColor.WHITE + "  splash: " + splash);
            lore.add(ChatColor.WHITE + "  extended: " + extended);
            lore.add(ChatColor.WHITE + "  upgraded: " + upgraded);
            meta.setLore(lore);
            
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            potion.setItemMeta(meta);
        }
        
        return potion;
    }

    private void setupMainClickHandlers() {
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            switch (slot) {
                case 10 -> { // Type selector
                    selectingType = true;
                    page = 0;
                    open();
                }
                case 12 -> { // Splash toggle
                    splash = !splash;
                    buildMainGui();
                    gui.update();
                }
                case 14 -> { // Extended toggle
                    extended = !extended;
                    if (extended) upgraded = false; // Mutually exclusive
                    buildMainGui();
                    gui.update();
                }
                case 16 -> { // Upgraded toggle
                    upgraded = !upgraded;
                    if (upgraded) extended = false; // Mutually exclusive
                    buildMainGui();
                    gui.update();
                }
                case 20 -> { // Clear
                    player.closeInventory();
                    onSave.accept(null);
                }
                case 22 -> { // Save
                    player.closeInventory();
                    onSave.accept(buildPotionString());
                }
                case 24 -> { // Cancel
                    player.closeInventory();
                    onCancel.run();
                }
            }
        });
    }

    private void setupTypeSelectionClickHandlers() {
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            // Back button
            if (slot == 0) {
                selectingType = false;
                open();
                return;
            }
            
            // Pagination
            if (slot == 45 && page > 0) {
                page--;
                buildTypeSelectionGui();
                gui.update();
                return;
            }
            
            if (slot == 53 && (page + 1) * 36 < allPotionTypes.size()) {
                page++;
                buildTypeSelectionGui();
                gui.update();
                return;
            }
            
            // Type selection (slots 9-44)
            if (slot >= 9 && slot < 45) {
                int index = (page * 36) + (slot - 9);
                if (index < allPotionTypes.size()) {
                    potionType = allPotionTypes.get(index).name();
                    selectingType = false;
                    open();
                }
            }
        });
    }

    private String buildPotionString() {
        return potionType + " " + splash + " " + extended + " " + upgraded;
    }
    
    /**
     * Create the potion type selector button with actual potion color.
     */
    private ItemStack createPotionTypeSelectorButton() {
        ItemStack potion = new ItemStack(Material.POTION);
        
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            try {
                PotionType type = PotionType.valueOf(potionType.toUpperCase());
                meta.setBasePotionType(type);
            } catch (Exception e) {
                try {
                    PotionType type = PotionType.valueOf(potionType.toUpperCase());
                    meta.getClass().getMethod("setBasePotionData", 
                        Class.forName("org.bukkit.potion.PotionData"))
                        .invoke(meta, Class.forName("org.bukkit.potion.PotionData")
                            .getConstructor(PotionType.class)
                            .newInstance(type));
                } catch (Exception ignored) {}
            }
            
            meta.setDisplayName(ChatColor.AQUA + "Potion Type");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + potionType);
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to select");
            meta.setLore(lore);
            
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            potion.setItemMeta(meta);
        }
        
        return potion;
    }

    /**
     * Create a potion item with the actual potion type applied.
     */
    private ItemStack createPotionItem(PotionType type, boolean isSelected) {
        Material material = isSelected ? Material.LINGERING_POTION : Material.POTION;
        ItemStack potion = new ItemStack(material);
        
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            // Apply the actual potion type so it shows the correct color
            try {
                meta.setBasePotionType(type);
            } catch (Exception e) {
                // Fallback for older versions - try deprecated method
                try {
                    // For older Bukkit versions
                    meta.getClass().getMethod("setBasePotionData", 
                        Class.forName("org.bukkit.potion.PotionData"))
                        .invoke(meta, Class.forName("org.bukkit.potion.PotionData")
                            .getConstructor(PotionType.class)
                            .newInstance(type));
                } catch (Exception ignored) {
                    // Just use the raw potion if all else fails
                }
            }
            
            meta.setDisplayName((isSelected ? ChatColor.GREEN : ChatColor.WHITE) + type.name());
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Click to select");
            meta.setLore(lore);
            
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            potion.setItemMeta(meta);
        }
        
        return potion;
    }
}
