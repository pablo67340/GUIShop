package com.pablo67340.guishop.listenable;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.economy.EconomyManager;
import com.pablo67340.guishop.statistics.StatisticsManager;
import com.pablo67340.guishop.util.PDCUtil;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import com.pablo67340.guishop.util.SchedulerUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class PlayerListener implements Listener {

    /**
     * An instance of a {@link PlayerListener} that will be used to handle this
     * specific object reference from other classes, even though methods here
     * will be static.
     */
    public static final PlayerListener INSTANCE = new PlayerListener();

    private final String[] commandsEntryList = {
        "reload",
        "edit",
        "eco",
        "market",
        "iteminfo",
        "toggleworth",
        "parsemob",
        "value",
        "list-shops"};

    /**
     * Opens the shop menu for a player.
     * This removes the player from creator mode to ensure normal shop behavior.
     */
    public Menu openMenu(Player player) {
        // Remove from creator mode when opening shop normally
        GUIShop.getCREATOR().remove(player.getUniqueId());
        Menu menu = new Menu(player);
        menu.open(player);
        return menu;
    }
    
    /**
     * Opens the shop menu for a player in edit mode.
     * Does NOT remove from creator mode - caller should add to CREATOR first.
     */
    public Menu openMenuForEdit(Player player) {
        Menu menu = new Menu(player);
        menu.open(player);
        return menu;
    }

    /**
     * Print the usage of the plugin to the player.
     *
     * @param sender The player the help text will be sent to
     */
    public void printUsage(CommandSender sender) {
        GUIShop.getINSTANCE().getMiscUtils().sendMessagePrefix(sender, String.join("\n", GUIShop.getINSTANCE().getConfigManager().getMessagesConfig().getStringList("messages.list"))
                .replace("%list%", Arrays.stream(commandsEntryList).map(entry -> GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages." + entry + ".entry") + "§r")
                        .collect(Collectors.joining("\n"))));
    }

    // When the player clicks a sign
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        // If the block exists
        if (block != null) {
            // If the block state is a Sign
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                String line1 = ChatColor.translateAlternateColorCodes('&', sign.getLine(0));
                // Check if the sign is a GUIShop sign
                if (line1.equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&',
                        Config.getTitlesConfig().getSignTitle()))) {
                    // If the player has Permission to use sign
                    if (GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.use") && GUIShop.getINSTANCE().getMiscUtils().getPerms().playerHas(player, "guishop.sign.use")
                            || player.isOp()) {
                        e.setCancelled(true);
                        Menu menu = new Menu(player);
                        menu.open(player);
                    } else {
                        e.setCancelled(true);
                        GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-permission");
                    }
                }
            }
        }
    }

    /**
     * Custom MobSpawner placement method.
     *
     * @param event The event type we're listening to
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (Item.isSpawnerItem(item)) {
            String mobId = PDCUtil.getString(item, PDCUtil.KEY_SPAWNER_MOB);
            if (mobId != null) {
                Block block = event.getBlockPlaced();
                SchedulerUtil.runAtLocationLater(block.getLocation(), () -> {
                    CreatureSpawner cs = (CreatureSpawner) block.getState();

                    GUIShop.getINSTANCE().getLogUtil().debugLog("Applying mob type " + mobId);

                    /*
                     * Although valueOf is almost always safe here because
                     * we used EntityType.name() when setting the PDC tag,
                     * it's possible the user might change server versions,
                     * in which case the EntityType enum may have changed.
                     */
                    try {
                        cs.setSpawnedType(EntityType.valueOf(mobId));
                        cs.update();
                    } catch (IllegalArgumentException veryRareException) {
                        GUIShop.getINSTANCE().getLogUtil().log("Detected outdated mob spawner ID: " + mobId + " placed by " + event.getPlayer());
                    }
                }, 1L);
            }
        }
    }
    
    /**
     * Load player statistics, preferences, and economy cache when they join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load statistics cache
        StatisticsManager statsManager = StatisticsManager.getInstance();
        if (statsManager != null && statsManager.isAvailable()) {
            statsManager.loadPlayerCache(event.getPlayer());
            statsManager.loadPreferencesCache(event.getPlayer().getUniqueId());
        }
        
        // Load economy cache
        EconomyManager ecoManager = EconomyManager.getInstance();
        if (ecoManager != null && ecoManager.isAvailable()) {
            ecoManager.loadPlayerCache(event.getPlayer());
        }
    }
    
    /**
     * Save and unload player statistics, preferences, and economy cache when they leave.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Unload statistics cache
        StatisticsManager statsManager = StatisticsManager.getInstance();
        if (statsManager != null && statsManager.isAvailable()) {
            statsManager.unloadPlayerCache(event.getPlayer());
            statsManager.unloadPreferencesCache(event.getPlayer().getUniqueId());
        }
        
        // Unload economy cache
        EconomyManager ecoManager = EconomyManager.getInstance();
        if (ecoManager != null && ecoManager.isAvailable()) {
            ecoManager.unloadPlayerCache(event.getPlayer());
        }
        
        // Clean up item info debug mode
        GUIShop.getITEM_INFO_DEBUG().remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * Debug inventory interactions for players with item info debug mode enabled.
     * Logs PDC/NBT data when items are moved or stacked.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Only process for players with item info debug enabled
        if (!GUIShop.getITEM_INFO_DEBUG().contains(player.getUniqueId())) return;
        
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        // Log when there's an item on cursor and an item in the slot (potential stack/swap)
        if (cursor != null && !cursor.getType().isAir() && current != null && !current.getType().isAir()) {
            GUIShop.getINSTANCE().getLogUtil().log("=== ITEM INFO DEBUG: Stack/Swap Attempt ===");
            GUIShop.getINSTANCE().getLogUtil().log("Player: " + player.getName() + " | Slot: " + event.getSlot());
            
            GUIShop.getINSTANCE().getLogUtil().log("--- CURSOR ITEM ---");
            logItemDetails(cursor);
            
            GUIShop.getINSTANCE().getLogUtil().log("--- SLOT ITEM ---");
            logItemDetails(current);
            
            // Check if they would stack
            boolean canStack = cursor.isSimilar(current);
            GUIShop.getINSTANCE().getLogUtil().log("isSimilar() = " + canStack);
            if (!canStack && cursor.getType() == current.getType()) {
                GUIShop.getINSTANCE().getLogUtil().log("Same material but NOT similar - comparing differences:");
                compareItems(cursor, current);
            }
            GUIShop.getINSTANCE().getLogUtil().log("==========================================");
        } else if (cursor != null && !cursor.getType().isAir()) {
            // Just placing an item
            GUIShop.getINSTANCE().getLogUtil().log("=== ITEM INFO DEBUG: Placing Item ===");
            GUIShop.getINSTANCE().getLogUtil().log("Player: " + player.getName() + " | Slot: " + event.getSlot());
            logItemDetails(cursor);
            GUIShop.getINSTANCE().getLogUtil().log("=====================================");
        } else if (current != null && !current.getType().isAir()) {
            // Just picking up an item
            GUIShop.getINSTANCE().getLogUtil().log("=== ITEM INFO DEBUG: Picking Up Item ===");
            GUIShop.getINSTANCE().getLogUtil().log("Player: " + player.getName() + " | Slot: " + event.getSlot());
            logItemDetails(current);
            GUIShop.getINSTANCE().getLogUtil().log("========================================");
        }
    }
    
    /**
     * Log detailed item information including PDC data.
     */
    private void logItemDetails(ItemStack item) {
        GUIShop.getINSTANCE().getLogUtil().log("  Material: " + item.getType());
        GUIShop.getINSTANCE().getLogUtil().log("  Amount: " + item.getAmount());
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                GUIShop.getINSTANCE().getLogUtil().log("  DisplayName: " + meta.getDisplayName());
            }
            if (meta.hasLore()) {
                GUIShop.getINSTANCE().getLogUtil().log("  Lore: " + meta.getLore());
            }
            if (meta.hasCustomModelData()) {
                GUIShop.getINSTANCE().getLogUtil().log("  CustomModelData: " + meta.getCustomModelData());
            }
            if (!meta.getEnchants().isEmpty()) {
                GUIShop.getINSTANCE().getLogUtil().log("  Enchants: " + meta.getEnchants());
            }
            if (!meta.getItemFlags().isEmpty()) {
                GUIShop.getINSTANCE().getLogUtil().log("  ItemFlags: " + meta.getItemFlags());
            }
            if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                if (damageable.hasDamage()) {
                    GUIShop.getINSTANCE().getLogUtil().log("  Damage: " + damageable.getDamage());
                }
            }
            
            // Log PDC data
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.isEmpty()) {
                GUIShop.getINSTANCE().getLogUtil().log("  PDC Keys:");
                for (NamespacedKey key : pdc.getKeys()) {
                    String value = getPDCValue(pdc, key);
                    GUIShop.getINSTANCE().getLogUtil().log("    " + key.toString() + " = " + value);
                }
            } else {
                GUIShop.getINSTANCE().getLogUtil().log("  PDC: (empty)");
            }
        } else {
            GUIShop.getINSTANCE().getLogUtil().log("  Meta: null");
        }
    }
    
    /**
     * Compare two items and log their differences.
     */
    private void compareItems(ItemStack item1, ItemStack item2) {
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (meta1 == null && meta2 == null) {
            GUIShop.getINSTANCE().getLogUtil().log("  Both metas are null - should stack");
            return;
        }
        if (meta1 == null || meta2 == null) {
            GUIShop.getINSTANCE().getLogUtil().log("  One meta is null, other is not");
            return;
        }
        
        // Compare display names
        String name1 = meta1.hasDisplayName() ? meta1.getDisplayName() : "(none)";
        String name2 = meta2.hasDisplayName() ? meta2.getDisplayName() : "(none)";
        if (!name1.equals(name2)) {
            GUIShop.getINSTANCE().getLogUtil().log("  DIFF DisplayName: '" + name1 + "' vs '" + name2 + "'");
        }
        
        // Compare lore
        String lore1 = meta1.hasLore() ? meta1.getLore().toString() : "(none)";
        String lore2 = meta2.hasLore() ? meta2.getLore().toString() : "(none)";
        if (!lore1.equals(lore2)) {
            GUIShop.getINSTANCE().getLogUtil().log("  DIFF Lore: '" + lore1 + "' vs '" + lore2 + "'");
        }
        
        // Compare enchants
        if (!meta1.getEnchants().equals(meta2.getEnchants())) {
            GUIShop.getINSTANCE().getLogUtil().log("  DIFF Enchants: " + meta1.getEnchants() + " vs " + meta2.getEnchants());
        }
        
        // Compare custom model data
        boolean hasCmd1 = meta1.hasCustomModelData();
        boolean hasCmd2 = meta2.hasCustomModelData();
        if (hasCmd1 != hasCmd2 || (hasCmd1 && meta1.getCustomModelData() != meta2.getCustomModelData())) {
            GUIShop.getINSTANCE().getLogUtil().log("  DIFF CustomModelData: " + 
                (hasCmd1 ? meta1.getCustomModelData() : "none") + " vs " + 
                (hasCmd2 ? meta2.getCustomModelData() : "none"));
        }
        
        // Compare PDC
        PersistentDataContainer pdc1 = meta1.getPersistentDataContainer();
        PersistentDataContainer pdc2 = meta2.getPersistentDataContainer();
        
        java.util.Set<NamespacedKey> allKeys = new java.util.HashSet<>();
        allKeys.addAll(pdc1.getKeys());
        allKeys.addAll(pdc2.getKeys());
        
        for (NamespacedKey key : allKeys) {
            boolean has1 = pdc1.has(key);
            boolean has2 = pdc2.has(key);
            
            if (has1 && has2) {
                String val1 = getPDCValue(pdc1, key);
                String val2 = getPDCValue(pdc2, key);
                if (!val1.equals(val2)) {
                    GUIShop.getINSTANCE().getLogUtil().log("  DIFF PDC[" + key + "]: '" + val1 + "' vs '" + val2 + "'");
                }
            } else if (has1) {
                GUIShop.getINSTANCE().getLogUtil().log("  DIFF PDC[" + key + "]: '" + getPDCValue(pdc1, key) + "' vs (missing)");
            } else {
                GUIShop.getINSTANCE().getLogUtil().log("  DIFF PDC[" + key + "]: (missing) vs '" + getPDCValue(pdc2, key) + "'");
            }
        }
    }
    
    /**
     * Get a PDC value as a string, trying common data types.
     */
    private String getPDCValue(PersistentDataContainer pdc, NamespacedKey key) {
        try {
            String strVal = pdc.get(key, PersistentDataType.STRING);
            if (strVal != null) return "STRING:" + strVal;
        } catch (Exception ignored) {}
        
        try {
            Integer intVal = pdc.get(key, PersistentDataType.INTEGER);
            if (intVal != null) return "INT:" + intVal;
        } catch (Exception ignored) {}
        
        try {
            Double dblVal = pdc.get(key, PersistentDataType.DOUBLE);
            if (dblVal != null) return "DOUBLE:" + dblVal;
        } catch (Exception ignored) {}
        
        try {
            Long longVal = pdc.get(key, PersistentDataType.LONG);
            if (longVal != null) return "LONG:" + longVal;
        } catch (Exception ignored) {}
        
        try {
            Byte byteVal = pdc.get(key, PersistentDataType.BYTE);
            if (byteVal != null) return "BYTE:" + byteVal;
        } catch (Exception ignored) {}
        
        try {
            byte[] byteArr = pdc.get(key, PersistentDataType.BYTE_ARRAY);
            if (byteArr != null) return "BYTE_ARRAY:" + Arrays.toString(byteArr);
        } catch (Exception ignored) {}
        
        return "(unknown type)";
    }
}
