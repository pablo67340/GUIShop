package com.pablo67340.guishop.listenable.editor;

import com.pablo67340.guishop.gui.SimpleGui;
import com.pablo67340.guishop.util.ItemStackBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

/**
 * GUI for editing enchantments in GUIShop format (e.g., "SHARPNESS:5 FIRE_ASPECT:2").
 */
public class EnchantmentEditorGui {

    private final Player player;
    private final Consumer<String> onSave;
    private final Runnable onCancel;
    
    private final Map<String, Integer> currentEnchants = new LinkedHashMap<>();
    private SimpleGui gui;
    private int page = 0;
    
    // All available enchantments
    private final List<Enchantment> allEnchantments;

    public EnchantmentEditorGui(Player player, String currentEnchantments, 
                                Consumer<String> onSave, Runnable onCancel) {
        this.player = player;
        this.onSave = onSave;
        this.onCancel = onCancel;
        
        // Get all enchantments
        allEnchantments = new ArrayList<>();
        for (Enchantment ench : Enchantment.values()) {
            allEnchantments.add(ench);
        }
        allEnchantments.sort(Comparator.comparing(e -> e.getKey().getKey()));
        
        // Parse current enchantments
        if (currentEnchantments != null && !currentEnchantments.isEmpty()) {
            for (String part : currentEnchantments.split(" ")) {
                String[] split = part.split(":");
                if (split.length == 2) {
                    try {
                        currentEnchants.put(split[0].toUpperCase(), Integer.parseInt(split[1]));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    public void open() {
        gui = new SimpleGui(6, "&5&lEnchantment Editor");
        buildGui();
        gui.show(player);
    }

    private void buildGui() {
        gui.clear();
        
        // Show current enchantments in top row
        gui.setItem(4, createCurrentEnchantmentsDisplay());
        
        // Enchantment list (slots 9-44)
        int startIndex = page * 36;
        for (int i = 0; i < 36 && startIndex + i < allEnchantments.size(); i++) {
            Enchantment ench = allEnchantments.get(startIndex + i);
            gui.setItem(9 + i, createEnchantmentButton(ench));
        }
        
        // Navigation
        if (page > 0) {
            gui.setItem(45, new ItemStackBuilder(Material.ARROW)
                .setName(ChatColor.YELLOW + "Previous Page")
                .build());
        }
        
        if ((page + 1) * 36 < allEnchantments.size()) {
            gui.setItem(53, new ItemStackBuilder(Material.ARROW)
                .setName(ChatColor.YELLOW + "Next Page")
                .build());
        }
        
        // Clear all button
        gui.setItem(48, new ItemStackBuilder(Material.BARRIER)
            .setName(ChatColor.RED + "Clear All")
            .addLoreLine(ChatColor.GRAY + "Remove all enchantments")
            .build());
        
        // Save button
        gui.setItem(49, new ItemStackBuilder(Material.LIME_WOOL)
            .setName(ChatColor.GREEN + "Save & Back")
            .build());
        
        // Cancel button  
        gui.setItem(50, new ItemStackBuilder(Material.RED_WOOL)
            .setName(ChatColor.RED + "Cancel")
            .build());
        
        setupClickHandlers();
    }

    private ItemStack createCurrentEnchantmentsDisplay() {
        ItemStackBuilder builder = new ItemStackBuilder(Material.ENCHANTED_BOOK)
            .setName(ChatColor.LIGHT_PURPLE + "Current Enchantments")
            .addLoreLine("");
        
        if (currentEnchants.isEmpty()) {
            builder.addLoreLine(ChatColor.RED + "None selected");
        } else {
            for (Map.Entry<String, Integer> entry : currentEnchants.entrySet()) {
                builder.addLoreLine(ChatColor.GRAY + "- " + ChatColor.WHITE + 
                    entry.getKey() + ":" + entry.getValue());
            }
            builder.addLoreLine("");
            builder.addLoreLine(ChatColor.DARK_GRAY + "Config format:");
            builder.addLoreLine(ChatColor.WHITE + buildEnchantString());
        }
        
        builder.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        return builder.build();
    }

    private ItemStack createEnchantmentButton(Enchantment enchantment) {
        String key = enchantment.getKey().getKey().toUpperCase();
        Integer currentLevel = currentEnchants.get(key);
        boolean isSelected = currentLevel != null;
        
        Material mat = isSelected ? Material.ENCHANTED_BOOK : Material.BOOK;
        
        ItemStackBuilder builder = new ItemStackBuilder(mat)
            .setName((isSelected ? ChatColor.GREEN : ChatColor.WHITE) + key);
        
        builder.addLoreLine(ChatColor.GRAY + "Max level: " + enchantment.getMaxLevel());
        
        if (isSelected) {
            builder.addLoreLine("");
            builder.addLoreLine(ChatColor.GREEN + "Selected: Level " + currentLevel);
            builder.addLoreLine("");
            builder.addLoreLine(ChatColor.YELLOW + "Left-click to increase level");
            builder.addLoreLine(ChatColor.YELLOW + "Right-click to decrease level");
            builder.addLoreLine(ChatColor.RED + "Shift-click to remove");
        } else {
            builder.addLoreLine("");
            builder.addLoreLine(ChatColor.YELLOW + "Click to add (Level 1)");
        }
        
        if (isSelected) {
            builder.addItemFlag(ItemFlag.HIDE_ENCHANTS);
        }
        
        return builder.build();
    }

    private void setupClickHandlers() {
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            // Previous page
            if (slot == 45 && page > 0) {
                page--;
                buildGui();
                gui.update();
                return;
            }
            
            // Next page
            if (slot == 53 && (page + 1) * 36 < allEnchantments.size()) {
                page++;
                buildGui();
                gui.update();
                return;
            }
            
            // Clear all
            if (slot == 48) {
                currentEnchants.clear();
                buildGui();
                gui.update();
                return;
            }
            
            // Save
            if (slot == 49) {
                player.closeInventory();
                onSave.accept(currentEnchants.isEmpty() ? null : buildEnchantString());
                return;
            }
            
            // Cancel
            if (slot == 50) {
                player.closeInventory();
                onCancel.run();
                return;
            }
            
            // Enchantment slots (9-44)
            if (slot >= 9 && slot < 45) {
                int index = (page * 36) + (slot - 9);
                if (index < allEnchantments.size()) {
                    Enchantment ench = allEnchantments.get(index);
                    String key = ench.getKey().getKey().toUpperCase();
                    Integer currentLevel = currentEnchants.get(key);
                    
                    if (event.isShiftClick()) {
                        // Remove
                        currentEnchants.remove(key);
                    } else if (currentLevel == null) {
                        // Add level 1
                        currentEnchants.put(key, 1);
                    } else if (event.isRightClick()) {
                        // Decrease level
                        if (currentLevel > 1) {
                            currentEnchants.put(key, currentLevel - 1);
                        } else {
                            currentEnchants.remove(key);
                        }
                    } else {
                        // Increase level
                        currentEnchants.put(key, currentLevel + 1);
                    }
                    
                    buildGui();
                    gui.update();
                }
            }
        });
    }

    private String buildEnchantString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : currentEnchants.entrySet()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return sb.toString();
    }
}
