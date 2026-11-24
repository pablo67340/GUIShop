package com.pablo67340.guishop.statistics;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.economy.EconomyConfig;
import com.pablo67340.guishop.economy.EconomyManager;
import com.pablo67340.guishop.util.MathUtil;
import com.pablo67340.guishop.util.NameUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for GUIShop statistics and economy.
 * 
 * Available placeholders:
 * 
 * Balance placeholders (requires internal economy enabled):
 * - %guishop_balance% - Player balance (formatted with currency symbol)
 * - %guishop_balance_raw% - Player balance (raw number)
 * - %guishop_balance_formatted% - Player balance (abbreviated: $1.5K, $2M)
 * - %guishop_balance_commas% - Player balance (with commas: $1,500)
 * 
 * Statistics placeholders:
 * - %guishop_total_spent% - Total money spent (with commas)
 * - %guishop_total_spent_formatted% - Total money spent (abbreviated: 1.5K, 2M)
 * - %guishop_total_earned% - Total money earned (with commas)
 * - %guishop_total_earned_formatted% - Total money earned (abbreviated)
 * 
 * Item count placeholders:
 * - %guishop_items_bought% - Total items bought (with commas)
 * - %guishop_items_bought_formatted% - Total items bought (abbreviated)
 * - %guishop_items_sold% - Total items sold (with commas)
 * - %guishop_items_sold_formatted% - Total items sold (abbreviated)
 * 
 * Top items placeholders:
 * - %guishop_top_bought_1% - #1 most bought item (Material: Quantity)
 * - %guishop_top_bought_2% - #2 most bought item
 * - %guishop_top_bought_3% - #3 most bought item
 * - %guishop_top_bought_1_item% - #1 most bought item (Material name only)
 * - %guishop_top_bought_1_qty% - #1 most bought item (Quantity only)
 * - %guishop_top_sold_1% - #1 most sold item (Material: Quantity)
 * - %guishop_top_sold_2% - #2 most sold item
 * - %guishop_top_sold_3% - #3 most sold item
 * - %guishop_top_sold_1_item% - #1 most sold item (Material name only)
 * - %guishop_top_sold_1_qty% - #1 most sold item (Quantity only)
 */
public class GUIShopPlaceholderExpansion extends PlaceholderExpansion {
    
    private final GUIShop plugin;
    
    public GUIShopPlaceholderExpansion(GUIShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "guishop";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "pablo67340";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Balance placeholders (uses singleton pattern - survives reloads)
        String lowerParams = params.toLowerCase();
        if (lowerParams.startsWith("balance")) {
            EconomyManager ecoManager = EconomyManager.getInstance();
            EconomyConfig ecoConfig = EconomyConfig.getInstance();
            
            if (ecoManager == null) {
                plugin.getLogUtil().debugLog("Placeholder balance: EconomyManager is null");
                return "$0";
            }
            if (!ecoManager.isAvailable()) {
                plugin.getLogUtil().debugLog("Placeholder balance: EconomyManager not available");
                return "$0";
            }
            if (ecoConfig == null) {
                plugin.getLogUtil().debugLog("Placeholder balance: EconomyConfig is null");
                return "$0";
            }
            
            BigDecimal balance = ecoManager.getBalance(uuid);
            plugin.getLogUtil().debugLog("Placeholder balance for " + player.getName() + ": " + balance);
            
            switch (lowerParams) {
                case "balance":
                    return ecoConfig.formatBalance(balance);
                case "balance_raw":
                    return balance.toPlainString();
                case "balance_formatted":
                    return ecoConfig.getCurrencySymbol() + MathUtil.formatAbbreviated(balance);
                case "balance_commas":
                    return ecoConfig.getCurrencySymbol() + MathUtil.formatWithCommas(balance);
                default:
                    return ecoConfig.formatBalance(balance);
            }
        }
        
        StatisticsManager statsManager = StatisticsManager.getInstance();
        if (statsManager == null || !statsManager.isAvailable()) {
            return "N/A";
        }
        
        PlayerStats stats = statsManager.getStats(uuid);
        
        // Money placeholders
        switch (lowerParams) {
            case "total_spent":
                return MathUtil.formatWithCommas(stats.getTotalSpent());
            case "total_spent_formatted":
                return MathUtil.formatAbbreviated(stats.getTotalSpent());
            case "total_spent_raw":
                return stats.getTotalSpent().toPlainString();
                
            case "total_earned":
                return MathUtil.formatWithCommas(stats.getTotalEarned());
            case "total_earned_formatted":
                return MathUtil.formatAbbreviated(stats.getTotalEarned());
            case "total_earned_raw":
                return stats.getTotalEarned().toPlainString();
                
            // Item count placeholders
            case "items_bought":
                return String.format("%,d", stats.getItemsBought());
            case "items_bought_formatted":
                return MathUtil.formatAbbreviated(BigDecimal.valueOf(stats.getItemsBought()));
            case "items_bought_raw":
                return String.valueOf(stats.getItemsBought());
                
            case "items_sold":
                return String.format("%,d", stats.getItemsSold());
            case "items_sold_formatted":
                return MathUtil.formatAbbreviated(BigDecimal.valueOf(stats.getItemsSold()));
            case "items_sold_raw":
                return String.valueOf(stats.getItemsSold());
        }
        
        // Top bought items
        if (lowerParams.startsWith("top_bought_")) {
            return handleTopItem(params.substring(11), stats, true);
        }
        
        // Top sold items
        if (lowerParams.startsWith("top_sold_")) {
            return handleTopItem(params.substring(9), stats, false);
        }
        
        return null;
    }
    
    private String handleTopItem(String suffix, PlayerStats stats, boolean bought) {
        // Parse rank (1, 2, or 3)
        int rank;
        String type = "";
        
        if (suffix.contains("_")) {
            String[] parts = suffix.split("_", 2);
            try {
                rank = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return null;
            }
            type = parts[1].toLowerCase();
        } else {
            try {
                rank = Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        if (rank < 1 || rank > 3) {
            return null;
        }
        
        // Get the data
        String material = stats.getTopItemMaterial(rank, bought);
        Integer quantity = stats.getTopItemQuantity(rank, bought);
        
        if (material == null || quantity == null) {
            return "None";
        }
        
        // Format material name nicely
        String formattedMaterial = NameUtil.formatMaterialName(material);
        
        switch (type) {
            case "item":
            case "material":
                return formattedMaterial;
            case "qty":
            case "quantity":
                return String.format("%,d", quantity);
            case "qty_formatted":
            case "quantity_formatted":
                return MathUtil.formatAbbreviated(BigDecimal.valueOf(quantity));
            default:
                // Full format: "Diamond Sword: 150"
                return formattedMaterial + ": " + String.format("%,d", quantity);
        }
    }
}
