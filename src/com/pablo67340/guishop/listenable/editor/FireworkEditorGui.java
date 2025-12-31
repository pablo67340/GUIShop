package com.pablo67340.guishop.listenable.editor;

import com.pablo67340.guishop.gui.SimpleGui;
import com.pablo67340.guishop.util.ItemStackBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI for editing firework info in GUIShop format.
 * Generates YAML-ready firework-info configuration.
 */
public class FireworkEditorGui {

    private final Player player;
    private final Consumer<String> onSave;
    private final Runnable onCancel;
    
    private SimpleGui gui;
    
    // Firework settings
    private int flight = 1;
    private final List<ExplosionData> explosions = new ArrayList<>();
    
    // Editing state
    private int editingExplosionIndex = -1;
    private boolean selectingShape = false;
    private boolean selectingColor = false;
    private boolean selectingFadeColor = false;

    // Preset colors with names
    private static final ColorPreset[] COLOR_PRESETS = {
        new ColorPreset("White", Color.WHITE),
        new ColorPreset("Silver", Color.SILVER),
        new ColorPreset("Gray", Color.GRAY),
        new ColorPreset("Black", Color.BLACK),
        new ColorPreset("Red", Color.RED),
        new ColorPreset("Maroon", Color.MAROON),
        new ColorPreset("Yellow", Color.YELLOW),
        new ColorPreset("Olive", Color.OLIVE),
        new ColorPreset("Lime", Color.LIME),
        new ColorPreset("Green", Color.GREEN),
        new ColorPreset("Aqua", Color.AQUA),
        new ColorPreset("Teal", Color.TEAL),
        new ColorPreset("Blue", Color.BLUE),
        new ColorPreset("Navy", Color.NAVY),
        new ColorPreset("Fuchsia", Color.FUCHSIA),
        new ColorPreset("Purple", Color.PURPLE),
        new ColorPreset("Orange", Color.ORANGE),
    };

    public FireworkEditorGui(Player player, String currentFireworkInfo,
                             Consumer<String> onSave, Runnable onCancel) {
        this.player = player;
        this.onSave = onSave;
        this.onCancel = onCancel;
        
        // Parse current firework info if exists (simplified format)
        // Full parsing would need the actual shops.yml structure
        if (currentFireworkInfo != null && !currentFireworkInfo.isEmpty()) {
            // For now, just start fresh - complex parsing would be needed
            // This could be enhanced to parse the serialized format
        }
    }

    public void open() {
        int rows = selectingShape || selectingColor || selectingFadeColor ? 4 : 5;
        gui = new SimpleGui(rows, "&c&lFirework Editor");
        
        if (selectingShape) {
            buildShapeSelectionGui();
        } else if (selectingColor || selectingFadeColor) {
            buildColorSelectionGui();
        } else if (editingExplosionIndex >= 0) {
            buildExplosionEditorGui();
        } else {
            buildMainGui();
        }
        gui.show(player);
    }

    private void buildMainGui() {
        gui.clear();
        
        // Flight power (slot 4)
        gui.setItem(4, new ItemStackBuilder(Material.FIREWORK_ROCKET)
            .setName(ChatColor.GOLD + "Flight Power")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + ChatColor.WHITE + flight)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Left-click to increase")
            .addLoreLine(ChatColor.YELLOW + "Right-click to decrease")
            .addLoreLine(ChatColor.GRAY + "(Range: 1-3)")
            .build());
        
        // Explosions list (slots 18-26)
        gui.setItem(13, new ItemStackBuilder(Material.FIRE_CHARGE)
            .setName(ChatColor.RED + "Explosions")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Count: " + ChatColor.WHITE + explosions.size())
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Add and configure firework effects")
            .build());
        
        for (int i = 0; i < 9; i++) {
            if (i < explosions.size()) {
                ExplosionData exp = explosions.get(i);
                gui.setItem(18 + i, createExplosionButton(i, exp));
            } else if (i == explosions.size() && i < 9) {
                gui.setItem(18 + i, new ItemStackBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .setName(ChatColor.GREEN + "+ Add Explosion")
                    .addLoreLine(ChatColor.GRAY + "Click to add a new effect")
                    .build());
            } else {
                gui.setItem(18 + i, new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setName(ChatColor.DARK_GRAY + "Empty Slot")
                    .build());
            }
        }
        
        // Preview
        gui.setItem(31, createPreviewDisplay());
        
        // Save button
        gui.setItem(36, new ItemStackBuilder(Material.LIME_WOOL)
            .setName(ChatColor.GREEN + "Save & Back")
            .build());
        
        // Clear button
        gui.setItem(40, new ItemStackBuilder(Material.BARRIER)
            .setName(ChatColor.RED + "Clear All")
            .addLoreLine(ChatColor.GRAY + "Remove all explosions")
            .build());
        
        // Cancel button
        gui.setItem(44, new ItemStackBuilder(Material.RED_WOOL)
            .setName(ChatColor.RED + "Cancel")
            .build());
        
        setupMainClickHandlers();
    }

    private void buildExplosionEditorGui() {
        gui.clear();
        ExplosionData exp = explosions.get(editingExplosionIndex);
        
        // Back button
        gui.setItem(0, new ItemStackBuilder(Material.ARROW)
            .setName(ChatColor.YELLOW + "Back")
            .build());
        
        // Title
        gui.setItem(4, new ItemStackBuilder(Material.FIREWORK_STAR)
            .setName(ChatColor.GOLD + "Editing Explosion #" + (editingExplosionIndex + 1))
            .build());
        
        // Shape
        gui.setItem(10, new ItemStackBuilder(Material.FIRE_CHARGE)
            .setName(ChatColor.AQUA + "Shape")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + ChatColor.WHITE + exp.shape)
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to change")
            .build());
        
        // Flicker
        gui.setItem(12, new ItemStackBuilder(exp.flicker ? Material.GLOWSTONE : Material.COAL)
            .setName(ChatColor.YELLOW + "Flicker")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + (exp.flicker ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to toggle")
            .build());
        
        // Trail
        gui.setItem(14, new ItemStackBuilder(exp.trail ? Material.BLAZE_POWDER : Material.GUNPOWDER)
            .setName(ChatColor.GOLD + "Trail")
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Current: " + (exp.trail ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to toggle")
            .build());
        
        // Colors
        gui.setItem(16, createColorsButton("Colors", exp.colors, false));
        
        // Fade colors
        gui.setItem(28, createColorsButton("Fade Colors", exp.fadeColors, true));
        
        // Delete explosion
        gui.setItem(34, new ItemStackBuilder(Material.BARRIER)
            .setName(ChatColor.RED + "Delete Explosion")
            .addLoreLine(ChatColor.GRAY + "Remove this effect")
            .build());
        
        // Done button
        gui.setItem(31, new ItemStackBuilder(Material.LIME_WOOL)
            .setName(ChatColor.GREEN + "Done")
            .build());
        
        setupExplosionEditorClickHandlers();
    }

    private void buildShapeSelectionGui() {
        gui.clear();
        
        // Back button
        gui.setItem(0, new ItemStackBuilder(Material.ARROW)
            .setName(ChatColor.YELLOW + "Back")
            .build());
        
        // Shape options
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        for (int i = 0; i < types.length && i < 9; i++) {
            FireworkEffect.Type type = types[i];
            String shapeName = type.name().toLowerCase();
            ExplosionData exp = explosions.get(editingExplosionIndex);
            boolean isSelected = exp.shape.equalsIgnoreCase(shapeName);
            
            gui.setItem(10 + i, new ItemStackBuilder(isSelected ? Material.FIREWORK_STAR : Material.FIRE_CHARGE)
                .setName((isSelected ? ChatColor.GREEN : ChatColor.WHITE) + shapeName)
                .addLoreLine(isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.YELLOW + "Click to select")
                .build());
        }
        
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 0) {
                selectingShape = false;
                open();
                return;
            }
            
            if (slot >= 10 && slot < 19) {
                int index = slot - 10;
                FireworkEffect.Type[] shapes = FireworkEffect.Type.values();
                if (index < shapes.length) {
                    explosions.get(editingExplosionIndex).shape = shapes[index].name().toLowerCase();
                    selectingShape = false;
                    open();
                }
            }
        });
    }

    private void buildColorSelectionGui() {
        gui.clear();
        
        // Back button
        gui.setItem(0, new ItemStackBuilder(Material.ARROW)
            .setName(ChatColor.YELLOW + "Back")
            .build());
        
        // Title
        String title = selectingFadeColor ? "Select Fade Color" : "Select Color";
        gui.setItem(4, new ItemStackBuilder(Material.LEATHER_CHESTPLATE)
            .setName(ChatColor.AQUA + title)
            .addLoreLine(ChatColor.GRAY + "Click a color to add it")
            .build());
        
        // Color presets
        for (int i = 0; i < COLOR_PRESETS.length && i < 27; i++) {
            ColorPreset preset = COLOR_PRESETS[i];
            Material woolMat = getWoolForColor(preset.color);
            
            gui.setItem(9 + i, new ItemStackBuilder(woolMat)
                .setName(ChatColor.WHITE + preset.name)
                .addLoreLine(ChatColor.GRAY + "RGB: " + preset.color.asRGB())
                .addLoreLine("")
                .addLoreLine(ChatColor.YELLOW + "Click to add")
                .build());
        }
        
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 0) {
                selectingColor = false;
                selectingFadeColor = false;
                open();
                return;
            }
            
            if (slot >= 9 && slot < 36) {
                int index = slot - 9;
                if (index < COLOR_PRESETS.length) {
                    ExplosionData exp = explosions.get(editingExplosionIndex);
                    int rgb = COLOR_PRESETS[index].color.asRGB();
                    
                    if (selectingFadeColor) {
                        if (!exp.fadeColors.contains(rgb)) {
                            exp.fadeColors.add(rgb);
                        }
                    } else {
                        if (!exp.colors.contains(rgb)) {
                            exp.colors.add(rgb);
                        }
                    }
                    
                    selectingColor = false;
                    selectingFadeColor = false;
                    open();
                }
            }
        });
    }

    private ItemStack createExplosionButton(int index, ExplosionData exp) {
        ItemStackBuilder builder = new ItemStackBuilder(Material.FIREWORK_STAR)
            .setName(ChatColor.GOLD + "Explosion #" + (index + 1))
            .addLoreLine("")
            .addLoreLine(ChatColor.GRAY + "Shape: " + ChatColor.WHITE + exp.shape)
            .addLoreLine(ChatColor.GRAY + "Flicker: " + (exp.flicker ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine(ChatColor.GRAY + "Trail: " + (exp.trail ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"))
            .addLoreLine(ChatColor.GRAY + "Colors: " + ChatColor.WHITE + exp.colors.size())
            .addLoreLine(ChatColor.GRAY + "Fade Colors: " + ChatColor.WHITE + exp.fadeColors.size())
            .addLoreLine("")
            .addLoreLine(ChatColor.YELLOW + "Click to edit");
        
        return builder.build();
    }

    private ItemStack createColorsButton(String name, List<Integer> colors, boolean isFade) {
        ItemStackBuilder builder = new ItemStackBuilder(isFade ? Material.GRAY_DYE : Material.RED_DYE)
            .setName(ChatColor.LIGHT_PURPLE + name)
            .addLoreLine("");
        
        if (colors.isEmpty()) {
            builder.addLoreLine(ChatColor.RED + "None");
        } else {
            for (int i = 0; i < Math.min(colors.size(), 5); i++) {
                builder.addLoreLine(ChatColor.GRAY + "- " + ChatColor.WHITE + colors.get(i));
            }
            if (colors.size() > 5) {
                builder.addLoreLine(ChatColor.DARK_GRAY + "... and " + (colors.size() - 5) + " more");
            }
        }
        
        builder.addLoreLine("");
        builder.addLoreLine(ChatColor.YELLOW + "Left-click to add color");
        builder.addLoreLine(ChatColor.RED + "Right-click to clear");
        
        return builder.build();
    }

    private ItemStack createPreviewDisplay() {
        ItemStackBuilder builder = new ItemStackBuilder(Material.FIREWORK_ROCKET)
            .setName(ChatColor.AQUA + "Configuration Preview")
            .addLoreLine("")
            .addLoreLine(ChatColor.WHITE + "firework-info:")
            .addLoreLine(ChatColor.WHITE + "  flight: " + flight);
        
        if (explosions.isEmpty()) {
            builder.addLoreLine(ChatColor.WHITE + "  explosions: []");
        } else {
            builder.addLoreLine(ChatColor.WHITE + "  explosions:");
            for (int i = 0; i < Math.min(explosions.size(), 2); i++) {
                ExplosionData exp = explosions.get(i);
                builder.addLoreLine(ChatColor.WHITE + "    - shape: " + exp.shape);
                builder.addLoreLine(ChatColor.WHITE + "      flicker: " + exp.flicker);
            }
            if (explosions.size() > 2) {
                builder.addLoreLine(ChatColor.DARK_GRAY + "      ... and more");
            }
        }
        
        return builder.build();
    }

    private void setupMainClickHandlers() {
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            // Flight power
            if (slot == 4) {
                if (event.isRightClick()) {
                    flight = Math.max(1, flight - 1);
                } else {
                    flight = Math.min(3, flight + 1);
                }
                buildMainGui();
                gui.update();
                return;
            }
            
            // Explosion slots (18-26)
            if (slot >= 18 && slot <= 26) {
                int index = slot - 18;
                if (index < explosions.size()) {
                    // Edit existing explosion
                    editingExplosionIndex = index;
                    open();
                } else if (index == explosions.size() && explosions.size() < 9) {
                    // Add new explosion
                    explosions.add(new ExplosionData());
                    editingExplosionIndex = explosions.size() - 1;
                    open();
                }
                return;
            }
            
            // Save
            if (slot == 36) {
                player.closeInventory();
                onSave.accept(buildFireworkString());
                return;
            }
            
            // Clear
            if (slot == 40) {
                explosions.clear();
                flight = 1;
                buildMainGui();
                gui.update();
                return;
            }
            
            // Cancel
            if (slot == 44) {
                player.closeInventory();
                onCancel.run();
            }
        });
    }

    private void setupExplosionEditorClickHandlers() {
        gui.setTopClickHandler(event -> {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            ExplosionData exp = explosions.get(editingExplosionIndex);
            
            switch (slot) {
                case 0, 31 -> { // Back / Done
                    editingExplosionIndex = -1;
                    open();
                }
                case 10 -> { // Shape
                    selectingShape = true;
                    open();
                }
                case 12 -> { // Flicker
                    exp.flicker = !exp.flicker;
                    buildExplosionEditorGui();
                    gui.update();
                }
                case 14 -> { // Trail
                    exp.trail = !exp.trail;
                    buildExplosionEditorGui();
                    gui.update();
                }
                case 16 -> { // Colors
                    if (event.isRightClick()) {
                        exp.colors.clear();
                        buildExplosionEditorGui();
                        gui.update();
                    } else {
                        selectingColor = true;
                        open();
                    }
                }
                case 28 -> { // Fade colors
                    if (event.isRightClick()) {
                        exp.fadeColors.clear();
                        buildExplosionEditorGui();
                        gui.update();
                    } else {
                        selectingFadeColor = true;
                        open();
                    }
                }
                case 34 -> { // Delete
                    explosions.remove(editingExplosionIndex);
                    editingExplosionIndex = -1;
                    open();
                }
            }
        });
    }

    private String buildFireworkString() {
        if (explosions.isEmpty()) {
            return null;
        }
        
        // Build a serialized format that can be parsed back
        StringBuilder sb = new StringBuilder();
        sb.append(flight).append(";");
        
        for (int i = 0; i < explosions.size(); i++) {
            if (i > 0) sb.append("|");
            ExplosionData exp = explosions.get(i);
            sb.append(exp.shape).append(",");
            sb.append(exp.flicker).append(",");
            sb.append(exp.trail).append(",");
            sb.append(joinInts(exp.colors)).append(",");
            sb.append(joinInts(exp.fadeColors));
        }
        
        return sb.toString();
    }

    private String joinInts(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(":");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private Material getWoolForColor(Color color) {
        // Approximate color to wool
        if (color.equals(Color.WHITE)) return Material.WHITE_WOOL;
        if (color.equals(Color.RED) || color.equals(Color.MAROON)) return Material.RED_WOOL;
        if (color.equals(Color.ORANGE)) return Material.ORANGE_WOOL;
        if (color.equals(Color.YELLOW) || color.equals(Color.OLIVE)) return Material.YELLOW_WOOL;
        if (color.equals(Color.LIME) || color.equals(Color.GREEN)) return Material.LIME_WOOL;
        if (color.equals(Color.AQUA) || color.equals(Color.TEAL)) return Material.CYAN_WOOL;
        if (color.equals(Color.BLUE) || color.equals(Color.NAVY)) return Material.BLUE_WOOL;
        if (color.equals(Color.PURPLE) || color.equals(Color.FUCHSIA)) return Material.PURPLE_WOOL;
        if (color.equals(Color.GRAY) || color.equals(Color.SILVER)) return Material.GRAY_WOOL;
        if (color.equals(Color.BLACK)) return Material.BLACK_WOOL;
        return Material.WHITE_WOOL;
    }

    /**
     * Data class for explosion configuration.
     */
    private static class ExplosionData {
        String shape = "ball";
        boolean flicker = false;
        boolean trail = false;
        List<Integer> colors = new ArrayList<>();
        List<Integer> fadeColors = new ArrayList<>();
    }

    /**
     * Preset color with name.
     */
    private record ColorPreset(String name, Color color) {}
}
