package com.pablo67340.guishop.statistics;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.util.MathUtil;
import com.pablo67340.guishop.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player shop statistics using SQLite storage.
 */
public class StatisticsManager {
    
    private static StatisticsManager instance;
    
    private final GUIShop plugin;
    private Connection connection;
    private final File databaseFile;
    
    // Cache for online players
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    
    // Cache for player preferences (payment notifications enabled)
    private final Map<UUID, Boolean> payNotificationsCache = new ConcurrentHashMap<>();
    
    public StatisticsManager(GUIShop plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "Data/player_statistics.db");
        instance = this;
    }
    
    public static StatisticsManager getInstance() {
        return instance;
    }
    
    /**
     * Reset the singleton instance. Used during hard reload.
     */
    public static void resetInstance() {
        instance = null;
    }
    
    /**
     * Initialize the database connection and create tables.
     */
    public void initialize() {
        try {
            // Ensure Data folder exists
            File dataFolder = databaseFile.getParentFile();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            // Connect to SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            
            createTables();
            
            plugin.getLogUtil().log("Statistics database initialized: " + databaseFile.getPath());
            
        } catch (ClassNotFoundException e) {
            plugin.getLogUtil().log("SQLite JDBC driver not found. Statistics will be disabled.");
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to initialize statistics database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create database tables if they don't exist.
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Player stats table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid TEXT PRIMARY KEY,
                    total_spent REAL DEFAULT 0,
                    total_earned REAL DEFAULT 0,
                    items_bought INTEGER DEFAULT 0,
                    items_sold INTEGER DEFAULT 0
                )
            """);
            
            // Item transactions table for tracking top items
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS item_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    material TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price REAL NOT NULL,
                    type TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """);
            
            // Create indexes for faster queries
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON item_transactions(uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_type ON item_transactions(uuid, type)");
            
            // Player preferences table for settings like payment notifications
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_preferences (
                    uuid TEXT PRIMARY KEY,
                    pay_notifications INTEGER DEFAULT 1
                )
            """);
        }
    }
    
    /**
     * Close the database connection.
     */
    public void shutdown() {
        // Save all cached stats
        for (PlayerStats stats : cache.values()) {
            saveStats(stats);
        }
        cache.clear();
        
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogUtil().log("Statistics database closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Check if statistics tracking is available.
     */
    public boolean isAvailable() {
        return connection != null;
    }
    
    // ==================== Stats Recording ====================
    
    /**
     * Record a purchase transaction.
     * @param player The player who bought
     * @param material The material purchased
     * @param quantity The quantity purchased
     * @param price The total price paid
     */
    public void recordPurchase(Player player, String material, int quantity, BigDecimal price) {
        if (!isAvailable()) return;
        
        SchedulerUtil.runTaskAsync(() -> {
            try {
                UUID uuid = player.getUniqueId();
                
                // Update stats
                PlayerStats stats = getStats(uuid);
                stats.addSpent(price);
                stats.addItemsBought(quantity);
                saveStats(stats);
                
                // Record transaction
                recordTransaction(uuid, material, quantity, price, "BUY");
                
            } catch (Exception e) {
                plugin.getLogUtil().log("Failed to record purchase: " + e.getMessage());
            }
        });
    }
    
    /**
     * Record a sale transaction.
     * @param player The player who sold
     * @param material The material sold
     * @param quantity The quantity sold
     * @param price The total price received
     */
    public void recordSale(Player player, String material, int quantity, BigDecimal price) {
        if (!isAvailable()) return;
        
        SchedulerUtil.runTaskAsync(() -> {
            try {
                UUID uuid = player.getUniqueId();
                
                // Update stats
                PlayerStats stats = getStats(uuid);
                stats.addEarned(price);
                stats.addItemsSold(quantity);
                saveStats(stats);
                
                // Record transaction
                recordTransaction(uuid, material, quantity, price, "SELL");
                
            } catch (Exception e) {
                plugin.getLogUtil().log("Failed to record sale: " + e.getMessage());
            }
        });
    }
    
    private void recordTransaction(UUID uuid, String material, int quantity, BigDecimal price, String type) {
        String sql = "INSERT INTO item_transactions (uuid, material, quantity, price, type, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, material);
            pstmt.setInt(3, quantity);
            pstmt.setDouble(4, price.doubleValue());
            pstmt.setString(5, type);
            pstmt.setLong(6, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to record transaction: " + e.getMessage());
        }
    }
    
    // ==================== Stats Retrieval ====================
    
    /**
     * Get stats for a player (cached for online players).
     */
    public PlayerStats getStats(UUID uuid) {
        // Check cache first
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }
        
        // Load from database
        PlayerStats stats = loadStats(uuid);
        
        // Cache if player is online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            cache.put(uuid, stats);
        }
        
        return stats;
    }
    
    /**
     * Get stats for an online player.
     */
    public PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }
    
    private PlayerStats loadStats(UUID uuid) {
        PlayerStats stats = new PlayerStats(uuid);
        
        // Load basic stats
        String sql = "SELECT total_spent, total_earned, items_bought, items_sold FROM player_stats WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                stats.setTotalSpent(BigDecimal.valueOf(rs.getDouble("total_spent")));
                stats.setTotalEarned(BigDecimal.valueOf(rs.getDouble("total_earned")));
                stats.setItemsBought(rs.getInt("items_bought"));
                stats.setItemsSold(rs.getInt("items_sold"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to load player stats: " + e.getMessage());
        }
        
        // Load top items
        stats.setTopBoughtItems(loadTopItems(uuid, "BUY"));
        stats.setTopSoldItems(loadTopItems(uuid, "SELL"));
        
        return stats;
    }
    
    private Map<String, Integer> loadTopItems(UUID uuid, String type) {
        Map<String, Integer> items = new LinkedHashMap<>();
        
        String sql = """
            SELECT material, SUM(quantity) as total_qty 
            FROM item_transactions 
            WHERE uuid = ? AND type = ? 
            GROUP BY material 
            ORDER BY total_qty DESC 
            LIMIT 3
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, type);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                items.put(rs.getString("material"), rs.getInt("total_qty"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to load top items: " + e.getMessage());
        }
        
        return items;
    }
    
    private void saveStats(PlayerStats stats) {
        String sql = """
            INSERT INTO player_stats (uuid, total_spent, total_earned, items_bought, items_sold)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                total_spent = excluded.total_spent,
                total_earned = excluded.total_earned,
                items_bought = excluded.items_bought,
                items_sold = excluded.items_sold
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, stats.getUuid().toString());
            pstmt.setDouble(2, stats.getTotalSpent().doubleValue());
            pstmt.setDouble(3, stats.getTotalEarned().doubleValue());
            pstmt.setInt(4, stats.getItemsBought());
            pstmt.setInt(5, stats.getItemsSold());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to save player stats: " + e.getMessage());
        }
    }
    
    // ==================== Cache Management ====================
    
    /**
     * Load player stats into cache when they join.
     */
    public void loadPlayerCache(Player player) {
        if (!isAvailable()) return;
        
        SchedulerUtil.runTaskAsync(() -> {
            PlayerStats stats = loadStats(player.getUniqueId());
            cache.put(player.getUniqueId(), stats);
        });
    }
    
    /**
     * Save and remove player stats from cache when they leave.
     */
    public void unloadPlayerCache(Player player) {
        if (!isAvailable()) return;
        
        PlayerStats stats = cache.remove(player.getUniqueId());
        if (stats != null) {
            SchedulerUtil.runTaskAsync(() -> saveStats(stats));
        }
    }
    
    // ==================== Formatted Getters ====================
    
    /**
     * Get total spent as formatted string.
     */
    public String getTotalSpentFormatted(UUID uuid, boolean abbreviated) {
        PlayerStats stats = getStats(uuid);
        if (abbreviated) {
            return MathUtil.formatAbbreviated(stats.getTotalSpent());
        }
        return MathUtil.formatWithCommas(stats.getTotalSpent());
    }
    
    /**
     * Get total earned as formatted string.
     */
    public String getTotalEarnedFormatted(UUID uuid, boolean abbreviated) {
        PlayerStats stats = getStats(uuid);
        if (abbreviated) {
            return MathUtil.formatAbbreviated(stats.getTotalEarned());
        }
        return MathUtil.formatWithCommas(stats.getTotalEarned());
    }
    
    /**
     * Get items bought as formatted string.
     */
    public String getItemsBoughtFormatted(UUID uuid, boolean abbreviated) {
        PlayerStats stats = getStats(uuid);
        if (abbreviated) {
            return MathUtil.formatAbbreviated(BigDecimal.valueOf(stats.getItemsBought()));
        }
        return String.format("%,d", stats.getItemsBought());
    }
    
    /**
     * Get items sold as formatted string.
     */
    public String getItemsSoldFormatted(UUID uuid, boolean abbreviated) {
        PlayerStats stats = getStats(uuid);
        if (abbreviated) {
            return MathUtil.formatAbbreviated(BigDecimal.valueOf(stats.getItemsSold()));
        }
        return String.format("%,d", stats.getItemsSold());
    }
    
    // ==================== Leaderboard Queries ====================
    
    /**
     * Get top players by total spent.
     */
    public List<Map.Entry<UUID, BigDecimal>> getTopSpenders(int limit) {
        List<Map.Entry<UUID, BigDecimal>> result = new ArrayList<>();
        
        String sql = "SELECT uuid, total_spent FROM player_stats ORDER BY total_spent DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                BigDecimal spent = BigDecimal.valueOf(rs.getDouble("total_spent"));
                result.add(new AbstractMap.SimpleEntry<>(uuid, spent));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get top spenders: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get top players by total earned.
     */
    public List<Map.Entry<UUID, BigDecimal>> getTopEarners(int limit) {
        List<Map.Entry<UUID, BigDecimal>> result = new ArrayList<>();
        
        String sql = "SELECT uuid, total_earned FROM player_stats ORDER BY total_earned DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                BigDecimal earned = BigDecimal.valueOf(rs.getDouble("total_earned"));
                result.add(new AbstractMap.SimpleEntry<>(uuid, earned));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get top earners: " + e.getMessage());
        }
        
        return result;
    }
    
    // ==================== Top Items (Server-wide) ====================
    
    /**
     * Get top sold items server-wide (all players combined).
     * @param limit max number of items to return
     * @return LinkedHashMap of material name to total quantity sold, ordered by quantity desc
     */
    public Map<String, Long> getServerTopSoldItems(int limit) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (!isAvailable()) return result;
        
        String sql = """
            SELECT material, SUM(quantity) as total_qty 
            FROM item_transactions 
            WHERE type = 'SELL' 
            GROUP BY material 
            ORDER BY total_qty DESC 
            LIMIT ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                result.put(rs.getString("material"), rs.getLong("total_qty"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get server top sold items: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get top bought items server-wide (all players combined).
     * @param limit max number of items to return
     * @return LinkedHashMap of material name to total quantity bought, ordered by quantity desc
     */
    public Map<String, Long> getServerTopBoughtItems(int limit) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (!isAvailable()) return result;
        
        String sql = """
            SELECT material, SUM(quantity) as total_qty 
            FROM item_transactions 
            WHERE type = 'BUY' 
            GROUP BY material 
            ORDER BY total_qty DESC 
            LIMIT ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                result.put(rs.getString("material"), rs.getLong("total_qty"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get server top bought items: " + e.getMessage());
        }
        
        return result;
    }
    
    // ==================== Top Items (Per-player) ====================
    
    /**
     * Get a player's top sold items.
     * @param uuid the player's UUID
     * @param limit max number of items to return
     * @return LinkedHashMap of material name to total quantity sold, ordered by quantity desc
     */
    public Map<String, Long> getPlayerTopSoldItems(UUID uuid, int limit) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (!isAvailable()) return result;
        
        String sql = """
            SELECT material, SUM(quantity) as total_qty 
            FROM item_transactions 
            WHERE uuid = ? AND type = 'SELL' 
            GROUP BY material 
            ORDER BY total_qty DESC 
            LIMIT ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                result.put(rs.getString("material"), rs.getLong("total_qty"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get player top sold items: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get a player's top bought items.
     * @param uuid the player's UUID
     * @param limit max number of items to return
     * @return LinkedHashMap of material name to total quantity bought, ordered by quantity desc
     */
    public Map<String, Long> getPlayerTopBoughtItems(UUID uuid, int limit) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (!isAvailable()) return result;
        
        String sql = """
            SELECT material, SUM(quantity) as total_qty 
            FROM item_transactions 
            WHERE uuid = ? AND type = 'BUY' 
            GROUP BY material 
            ORDER BY total_qty DESC 
            LIMIT ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                result.put(rs.getString("material"), rs.getLong("total_qty"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get player top bought items: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Reset a player's statistics.
     */
    public void resetStats(UUID uuid) {
        if (!isAvailable()) return;
        
        SchedulerUtil.runTaskAsync(() -> {
            try {
                // Delete from both tables
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM player_stats WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM item_transactions WHERE uuid = ?")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                }
                
                // Clear from cache
                cache.remove(uuid);
                
            } catch (SQLException e) {
                plugin.getLogUtil().log("Failed to reset stats: " + e.getMessage());
            }
        });
    }
    
    // ==================== Payment Notification Preferences ====================
    
    /**
     * Check if a player has payment notifications enabled.
     * @param uuid the player's UUID
     * @return true if notifications are enabled (default true)
     */
    public boolean isPayNotificationsEnabled(UUID uuid) {
        // Check cache first
        if (payNotificationsCache.containsKey(uuid)) {
            return payNotificationsCache.get(uuid);
        }
        
        // Load from database
        if (!isAvailable()) return true;
        
        String sql = "SELECT pay_notifications FROM player_preferences WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                boolean enabled = rs.getInt("pay_notifications") == 1;
                payNotificationsCache.put(uuid, enabled);
                return enabled;
            }
        } catch (SQLException e) {
            plugin.getLogUtil().debugLog("Failed to load pay notifications: " + e.getMessage());
        }
        
        // Default: enabled
        return true;
    }
    
    /**
     * Set whether a player has payment notifications enabled.
     * @param uuid the player's UUID
     * @param enabled true to enable, false to disable
     */
    public void setPayNotificationsEnabled(UUID uuid, boolean enabled) {
        // Update cache
        payNotificationsCache.put(uuid, enabled);
        
        if (!isAvailable()) return;
        
        // Save to database asynchronously
        SchedulerUtil.runTaskAsync(() -> {
            String sql = """
                INSERT INTO player_preferences (uuid, pay_notifications) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET pay_notifications = ?
            """;
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setInt(2, enabled ? 1 : 0);
                pstmt.setInt(3, enabled ? 1 : 0);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogUtil().log("Failed to save pay notifications: " + e.getMessage());
            }
        });
    }
    
    /**
     * Toggle payment notifications for a player.
     * @param uuid the player's UUID
     * @return the new state (true = enabled)
     */
    public boolean togglePayNotifications(UUID uuid) {
        boolean current = isPayNotificationsEnabled(uuid);
        boolean newState = !current;
        setPayNotificationsEnabled(uuid, newState);
        return newState;
    }
    
    /**
     * Load preferences into cache when player joins.
     */
    public void loadPreferencesCache(UUID uuid) {
        if (!isAvailable()) return;
        
        String sql = "SELECT pay_notifications FROM player_preferences WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                payNotificationsCache.put(uuid, rs.getInt("pay_notifications") == 1);
            } else {
                payNotificationsCache.put(uuid, true); // Default enabled
            }
        } catch (SQLException e) {
            plugin.getLogUtil().debugLog("Failed to load preferences: " + e.getMessage());
        }
    }
    
    /**
     * Unload preferences from cache when player leaves.
     */
    public void unloadPreferencesCache(UUID uuid) {
        payNotificationsCache.remove(uuid);
    }
}
