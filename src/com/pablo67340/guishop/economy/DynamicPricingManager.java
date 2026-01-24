package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.api.DynamicPriceProvider;
import lombok.Getter;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
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
    
    private final GUIShop plugin;
    private Connection connection;
    private final File databaseFile;
    
    // Cache for item stock levels (positive = oversupply, negative = undersupply)
    private final ConcurrentHashMap<String, Integer> stockCache = new ConcurrentHashMap<>();
    
    // Task holder for normalization task
    private final com.pablo67340.guishop.util.SchedulerUtil.TaskHolder normalizationTask = 
        new com.pablo67340.guishop.util.SchedulerUtil.TaskHolder();
    
    // Configuration
    @Getter private double priceChangePerTransaction = 0.01; // 1% change per item
    @Getter private double maxPriceMultiplier = 2.0; // Max 200% of base price
    @Getter private double minPriceMultiplier = 0.5; // Min 50% of base price
    @Getter private double normalizationRate = 0.001; // Rate prices return to normal
    @Getter private int normalizationInterval = 300; // Seconds between normalization ticks
    
    private boolean initialized = false;
    
    public DynamicPricingManager(GUIShop plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "Data/dynamic_pricing.db");
        instance = this;
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
            
            // Load configuration from config.yml
            loadConfiguration();
            
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
            plugin.getLogUtil().log("Dynamic pricing system initialized: " + databaseFile.getPath());
            return true;
            
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to initialize dynamic pricing database: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load configuration from dynamicpricing.yml
     */
    private void loadConfiguration() {
        File configFile = new File(plugin.getDataFolder(), "dynamicpricing.yml");
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("dynamicpricing.yml", false);
        }
        
        org.bukkit.configuration.file.FileConfiguration config = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        
        priceChangePerTransaction = config.getDouble("price-change-per-item", 0.01);
        maxPriceMultiplier = config.getDouble("max-price-multiplier", 2.0);
        minPriceMultiplier = config.getDouble("min-price-multiplier", 0.5);
        normalizationRate = config.getDouble("normalization-rate", 0.001);
        normalizationInterval = config.getInt("normalization-interval", 300);
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
        plugin.getLogUtil().debugLog("Loaded " + stockCache.size() + " item stock levels into cache");
    }
    
    /**
     * Start a task that gradually normalizes prices back to base levels.
     */
    private void startNormalizationTask() {
        com.pablo67340.guishop.util.SchedulerUtil.runTaskTimerAsync(normalizationTask, () -> {
            if (!initialized || stockCache.isEmpty()) return;
            
            int normalized = 0;
            for (var entry : stockCache.entrySet()) {
                int currentStock = entry.getValue();
                if (currentStock == 0) continue;
                
                // Move stock level toward 0 (equilibrium)
                int change = (int) Math.ceil(Math.abs(currentStock) * normalizationRate);
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
        }, normalizationInterval * 20L, normalizationInterval * 20L); // Convert seconds to ticks
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
            plugin.getLogUtil().debugLog("Failed to save stock cache: " + e.getMessage());
        }
    }
    
    /**
     * Calculate price multiplier based on stock level.
     * Positive stock = oversupply = lower buy price, higher sell price
     * Negative stock = undersupply = higher buy price, lower sell price
     */
    private double calculateMultiplier(int stockLevel, boolean isBuyPrice) {
        // For buy prices, undersupply (negative stock) increases price
        // For sell prices, oversupply (positive stock) decreases price  
        double multiplier;
        if (isBuyPrice) {
            // Buying: negative stock = high demand = price goes UP
            multiplier = 1.0 - (stockLevel * priceChangePerTransaction);
        } else {
            // Selling: positive stock = oversupply = price goes DOWN
            multiplier = 1.0 - (stockLevel * priceChangePerTransaction);
        }
        
        // Clamp to min/max bounds
        multiplier = Math.max(minPriceMultiplier, Math.min(maxPriceMultiplier, multiplier));
        
        return multiplier;
    }
    
    @Override
    public BigDecimal calculateBuyPrice(String item, int quantity, BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        if (!initialized || staticBuyPrice == null) {
            return staticBuyPrice != null ? staticBuyPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        }
        
        int stockLevel = stockCache.getOrDefault(item, 0);
        double multiplier = calculateMultiplier(stockLevel, true);
        
        BigDecimal adjustedPrice = staticBuyPrice.multiply(BigDecimal.valueOf(multiplier));
        BigDecimal totalPrice = adjustedPrice.multiply(BigDecimal.valueOf(quantity));
        
        plugin.getLogUtil().debugLog("Dynamic buy price for " + item + " (qty=" + quantity + 
            ", stock=" + stockLevel + ", mult=" + String.format("%.2f", multiplier) + 
            "): " + staticBuyPrice + " -> " + adjustedPrice + " (total: " + totalPrice + ")");
        
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public BigDecimal calculateSellPrice(String item, int quantity, BigDecimal staticBuyPrice, BigDecimal staticSellPrice) {
        if (!initialized || staticSellPrice == null) {
            return staticSellPrice != null ? staticSellPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        }
        
        int stockLevel = stockCache.getOrDefault(item, 0);
        double multiplier = calculateMultiplier(stockLevel, false);
        
        BigDecimal adjustedPrice = staticSellPrice.multiply(BigDecimal.valueOf(multiplier));
        BigDecimal totalPrice = adjustedPrice.multiply(BigDecimal.valueOf(quantity));
        
        plugin.getLogUtil().debugLog("Dynamic sell price for " + item + " (qty=" + quantity + 
            ", stock=" + stockLevel + ", mult=" + String.format("%.2f", multiplier) + 
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
        
        plugin.getLogUtil().debugLog("Item bought: " + item + " x" + quantity + 
            " (stock: " + currentStock + " -> " + newStock + ")");
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
        
        plugin.getLogUtil().debugLog("Item sold: " + item + " x" + quantity + 
            " (stock: " + currentStock + " -> " + newStock + ")");
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
            plugin.getLogUtil().debugLog("Failed to update item stats: " + e.getMessage());
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
        return calculateMultiplier(stockCache.getOrDefault(item, 0), true);
    }
    
    /**
     * Get the current sell price multiplier for an item.
     */
    public double getSellMultiplier(String item) {
        return calculateMultiplier(stockCache.getOrDefault(item, 0), false);
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
            plugin.getLogUtil().debugLog("Failed to reset item: " + e.getMessage());
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
            plugin.getLogUtil().debugLog("Failed to reset all items: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown and save data.
     */
    public void shutdown() {
        if (!initialized) return;
        
        // Cancel normalization task
        com.pablo67340.guishop.util.SchedulerUtil.cancelTask(normalizationTask);
        
        saveStockCache();
        
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogUtil().debugLog("Error closing dynamic pricing database: " + e.getMessage());
        }
        
        initialized = false;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
