package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.economy.DynamicPricingSettings.ItemPriceOverride;
import com.pablo67340.guishop.platform.CancellableTask;
import com.pablo67340.guishop.platform.GuiShopContext;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import com.pablo67340.guishop.util.LogUtil;
import lombok.Getter;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in dynamic pricing provider that implements supply/demand mechanics.
 * 
 * Price adjustments are based on:
 * - When items are bought: price increases (supply decreases)
 * - When items are sold: price decreases (supply increases)
 * - Prices gradually normalize back to base price over time
 * 
 * All prices are bounded between configurable min/max multipliers of the static price.
 */
public class DynamicPricingManager implements DynamicPriceProvider {
    
    @Getter
    private static DynamicPricingManager instance;
    
    private Connection connection;
    private final File databaseFile;

    /**
     * Tuning values, parsed by the platform.
     */
    @Getter
    private final DynamicPricingSettings settings;

    // Cache for item stock levels (positive = oversupply, negative = undersupply)
    private final ConcurrentHashMap<String, Integer> stockCache = new ConcurrentHashMap<>();

    // Handle to the normalization task, so shutdown can stop it
    private CancellableTask normalizationTask;

    private boolean initialized = false;

    public DynamicPricingManager(DynamicPricingSettings settings) {
        this.settings = settings != null ? settings : new DynamicPricingSettings();
        this.databaseFile = context().dataFolder().resolve("Data").resolve("dynamic_pricing.db").toFile();
        instance = this;
    }

    private static GuiShopContext context() {
        return GuiShopPlatform.get();
    }

    private static LogUtil log() {
        return GuiShopPlatform.get().logUtil();
    }

    private Map<String, ItemPriceOverride> itemOverrides() {
        return settings.getItemOverrides();
    }

    public double getPriceChangePerTransaction() {
        return settings.getPriceChangePerTransaction();
    }

    public double getMaxPriceMultiplier() {
        return settings.getMaxPriceMultiplier();
    }

    public double getMinPriceMultiplier() {
        return settings.getMinPriceMultiplier();
    }

    public double getNormalizationRate() {
        return settings.getNormalizationRate();
    }

    public int getNormalizationInterval() {
        return settings.getNormalizationInterval();
    }
    
    /**
     * Reset the singleton instance. Used during hard reload.
     */
    public static void resetInstance() {
        instance = null;
    }
    
    /**
     * Initialize the dynamic pricing database and load configuration.
     */
    public boolean initialize() {
        try {
            // Ensure Data directory exists
            File dataDir = databaseFile.getParentFile();
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // Connect to SQLite database
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Create tables
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS item_stock (" +
                    "    item_key TEXT PRIMARY KEY," +
                    "    stock_level INTEGER DEFAULT 0," +
                    "    total_bought INTEGER DEFAULT 0," +
                    "    total_sold INTEGER DEFAULT 0," +
                    "    last_update INTEGER DEFAULT 0" +
                    ")"
                );
            }
            
            // Load existing stock levels into cache
            loadStockCache();
            
            // Start normalization task (prices gradually return to normal)
            startNormalizationTask();
            
            initialized = true;
            log().log("Dynamic pricing system initialized: " + databaseFile.getPath());
            return true;
            
        } catch (SQLException e) {
            log().log("Failed to initialize dynamic pricing database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load all stock levels from database into memory cache.
     */
    private void loadStockCache() throws SQLException {
        stockCache.clear();
        String sql = "SELECT item_key, stock_level FROM item_stock";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stockCache.put(rs.getString("item_key"), rs.getInt("stock_level"));
            }
        }
        log().debugLog("Loaded " + stockCache.size() + " item stock levels into cache");
    }
    
    /**
     * Start a task that gradually normalizes prices back to base levels.
     */
    private void startNormalizationTask() {
        normalizationTask = context().scheduler().runTimerAsync(() -> {
            if (!initialized || stockCache.isEmpty()) return;
            
            int normalized = 0;
            for (var entry : stockCache.entrySet()) {
                int currentStock = entry.getValue();
                if (currentStock == 0) continue;
                
                // Move stock level toward 0 (equilibrium)
                int change = (int) Math.ceil(Math.abs(currentStock) * settings.getNormalizationRate());
                if (change < 1) change = 1;
                
                int newStock;
                if (currentStock > 0) {
                    newStock = Math.max(0, currentStock - change);
                } else {
                    newStock = Math.min(0, currentStock + change);
                }
                
                entry.setValue(newStock);
                normalized++;
            }
            
            // Periodically save to database
            if (normalized > 0) {
                saveStockCache();
            }
        }, settings.getNormalizationInterval() * 20L, settings.getNormalizationInterval() * 20L); // Convert seconds to ticks
    }
    
    /**
     * Save all cached stock levels to database.
     */
    private void saveStockCache() {
        if (connection == null) return;
        
        String sql = "INSERT OR REPLACE INTO item_stock (item_key, stock_level, last_update) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            long now = System.currentTimeMillis() / 1000;
            for (var entry : stockCache.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setInt(2, entry.getValue());
                pstmt.setLong(3, now);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            log().debugLog("Failed to save stock cache: " + e.getMessage());
        }
    }
    
    /**
     * Calculate price multiplier based on stock level.
     * Positive stock = oversupply = lower buy price, higher sell price
     * Negative stock = undersupply = higher buy price, lower sell price
     * 
     * @param item the item key (material name)
     * @param stockLevel current stock level
     * @param isBuyPrice true for buy price calculation, false for sell price
     */
    private double calculateMultiplier(String item, int stockLevel, boolean isBuyPrice) {
        // Get per-item settings or use global defaults
        double priceChange = settings.getPriceChangePerTransaction();
        double maxMult = settings.getMaxPriceMultiplier();
        double minMult = settings.getMinPriceMultiplier();
        
        ItemPriceOverride override = itemOverrides().get(item.toUpperCase());
        if (override != null) {
            priceChange = override.getPriceChangePerItem();
            maxMult = override.getMaxMultiplier();
            minMult = override.getMinMultiplier();
            log().debugLog("Using override for " + item + ": priceChange=" + priceChange);
        }
        
        // For buy prices, undersupply (negative stock) increases price
        // For sell prices, oversupply (positive stock) decreases price  
        double multiplier;
        if (isBuyPrice) {
            // Buying: negative stock = high demand = price goes UP
            multiplier = 1.0 - (stockLevel * priceChange);
        } else {
            // Selling: positive stock = oversupply = price goes DOWN
            multiplier = 1.0 - (stockLevel * priceChange);
        }
        
        // Clamp to min/max bounds
        multiplier = Math.max(minMult, Math.min(maxMult, multiplier));
        
        return multiplier;
    }
    
    @Override
    public BigDecimal calculateBuyPrice(String item, int quantity, BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        if (!initialized || staticBuyPrice == null) {
            return staticBuyPrice != null ? staticBuyPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        }
        
        int stockLevel = stockCache.getOrDefault(item, 0);
        double multiplier = calculateMultiplier(item, stockLevel, true);
        
        BigDecimal adjustedPrice = staticBuyPrice.multiply(BigDecimal.valueOf(multiplier));
        BigDecimal totalPrice = adjustedPrice.multiply(BigDecimal.valueOf(quantity));
        
        log().debugLog("Dynamic buy price for " + item + " (qty=" + quantity + 
            ", stock=" + stockLevel + ", mult=" + String.format("%.4f", multiplier) + 
            "): " + staticBuyPrice + " -> " + adjustedPrice + " (total: " + totalPrice + ")");
        
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateSellPrice(String item, int quantity, BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        if (!initialized || staticSellPrice == null) {
            return staticSellPrice != null ? staticSellPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        }
        
        int stockLevel = stockCache.getOrDefault(item, 0);
        double multiplier = calculateMultiplier(item, stockLevel, false);
        
        BigDecimal adjustedPrice = staticSellPrice.multiply(BigDecimal.valueOf(multiplier));
        BigDecimal totalPrice = adjustedPrice.multiply(BigDecimal.valueOf(quantity));
        
        log().debugLog("Dynamic sell price for " + item + " (qty=" + quantity + 
            ", stock=" + stockLevel + ", mult=" + String.format("%.4f", multiplier) + 
            "): " + staticSellPrice + " -> " + adjustedPrice + " (total: " + totalPrice + ")");
        
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public void buyItem(String item, int quantity) {
        if (!initialized) return;
        
        // Buying removes from supply -> stock level decreases (goes negative = undersupply)
        int currentStock = stockCache.getOrDefault(item, 0);
        int newStock = currentStock - quantity;
        stockCache.put(item, newStock);
        
        // Update totals in database
        updateItemStats(item, quantity, 0);
        
        log().debugLog("Item bought: " + item + " x" + quantity + 
            " (stock: " + currentStock + " -> " + newStock + ")");
        
        // Apply linked price effects to related items
        applyLinkedPriceEffects(item, quantity, true);
    }
    
    @Override
    public void sellItem(String item, int quantity) {
        if (!initialized) return;
        
        // Selling adds to supply -> stock level increases (goes positive = oversupply)
        int currentStock = stockCache.getOrDefault(item, 0);
        int newStock = currentStock + quantity;
        stockCache.put(item, newStock);
        
        // Update totals in database
        updateItemStats(item, 0, quantity);
        
        log().debugLog("Item sold: " + item + " x" + quantity + 
            " (stock: " + currentStock + " -> " + newStock + ")");
        
        // Apply linked price effects to related items
        applyLinkedPriceEffects(item, quantity, false);
    }
    
    /**
     * Apply price effects to linked items when an item is bought or sold.
     * For example, buying DIAMOND_BLOCK can affect DIAMOND prices.
     * 
     * @param item the item that was bought/sold
     * @param quantity the quantity bought/sold
     * @param isBuy true if bought, false if sold
     */
    private void applyLinkedPriceEffects(String item, int quantity, boolean isBuy) {
        ItemPriceOverride override = itemOverrides().get(item.toUpperCase());
        if (override == null || override.getAffects().isEmpty()) {
            return;
        }
        
        for (var entry : override.getAffects().entrySet()) {
            String affectedItem = entry.getKey();
            double multiplier = entry.getValue();
            
            // Calculate the effective quantity change for the affected item
            int effectiveQuantity = (int) Math.round(quantity * multiplier);
            if (effectiveQuantity == 0) continue;
            
            int currentStock = stockCache.getOrDefault(affectedItem, 0);
            int newStock;
            
            if (isBuy) {
                // Buying the source item decreases supply of affected item
                newStock = currentStock - effectiveQuantity;
            } else {
                // Selling the source item increases supply of affected item
                newStock = currentStock + effectiveQuantity;
            }
            
            stockCache.put(affectedItem, newStock);
            
            log().debugLog("Linked price effect: " + item + " " + (isBuy ? "buy" : "sell") + 
                " affected " + affectedItem + " (multiplier=" + multiplier + 
                ", effectiveQty=" + effectiveQuantity + ", stock: " + currentStock + " -> " + newStock + ")");
        }
    }
    
    /**
     * Update item statistics in database.
     */
    private void updateItemStats(String item, int bought, int sold) {
        if (connection == null) return;
        
        String sql = "INSERT INTO item_stock (item_key, stock_level, total_bought, total_sold, last_update) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(item_key) DO UPDATE SET " +
                     "stock_level = ?, total_bought = total_bought + ?, total_sold = total_sold + ?, last_update = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int stockLevel = stockCache.getOrDefault(item, 0);
            long now = System.currentTimeMillis() / 1000;
            
            pstmt.setString(1, item);
            pstmt.setInt(2, stockLevel);
            pstmt.setInt(3, bought);
            pstmt.setInt(4, sold);
            pstmt.setLong(5, now);
            pstmt.setInt(6, stockLevel);
            pstmt.setInt(7, bought);
            pstmt.setInt(8, sold);
            pstmt.setLong(9, now);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log().debugLog("Failed to update item stats: " + e.getMessage());
        }
    }
    
    /**
     * Get the current stock level for an item.
     * Positive = oversupply, Negative = undersupply, 0 = equilibrium
     */
    public int getStockLevel(String item) {
        return stockCache.getOrDefault(item, 0);
    }
    
    /**
     * Get the current price multiplier for an item.
     */
    public double getBuyMultiplier(String item) {
        return calculateMultiplier(item, stockCache.getOrDefault(item, 0), true);
    }
    
    /**
     * Get the current sell price multiplier for an item.
     */
    public double getSellMultiplier(String item) {
        return calculateMultiplier(item, stockCache.getOrDefault(item, 0), false);
    }
    
    /**
     * Reset stock level for an item (admin command).
     */
    public void resetItem(String item) {
        stockCache.put(item, 0);
        
        String sql = "UPDATE item_stock SET stock_level = 0 WHERE item_key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, item);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log().debugLog("Failed to reset item: " + e.getMessage());
        }
    }
    
    /**
     * Reset all stock levels (admin command).
     */
    public void resetAll() {
        stockCache.clear();
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("UPDATE item_stock SET stock_level = 0");
        } catch (SQLException e) {
            log().debugLog("Failed to reset all items: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown and save data.
     */
    public void shutdown() {
        if (!initialized) return;
        
        // Cancel normalization task
        if (normalizationTask != null) {
            normalizationTask.cancel();
            normalizationTask = null;
        }
        
        saveStockCache();
        
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log().debugLog("Error closing dynamic pricing database: " + e.getMessage());
        }
        
        initialized = false;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
