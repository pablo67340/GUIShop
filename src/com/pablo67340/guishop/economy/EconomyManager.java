package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player balances using SQLite storage.
 */
public class EconomyManager {
    
    private static EconomyManager instance;
    
    private final GUIShop plugin;
    private Connection connection;
    private final File databaseFile;
    
    // Cache for online players
    private final ConcurrentHashMap<UUID, BigDecimal> balanceCache = new ConcurrentHashMap<>();
    
    public EconomyManager(GUIShop plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "Data/player_balances.db");
        instance = this;
    }
    
    public static EconomyManager getInstance() {
        return instance;
    }
    
    /**
     * Initialize the database connection and create tables.
     */
    public boolean initialize() {
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
            
            plugin.getLogUtil().log("Economy database initialized: " + databaseFile.getPath());
            return true;
            
        } catch (ClassNotFoundException e) {
            plugin.getLogUtil().log("SQLite JDBC driver not found. Internal economy will be disabled.");
            return false;
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to initialize economy database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_balances (
                    uuid TEXT PRIMARY KEY,
                    username TEXT,
                    balance REAL DEFAULT 0,
                    last_seen INTEGER
                )
            """);
            
            // Create index for faster username lookups
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_username ON player_balances(username)");
        }
    }
    
    /**
     * Shutdown and close database connection.
     */
    public void shutdown() {
        // Save all cached balances
        for (var entry : balanceCache.entrySet()) {
            saveBalance(entry.getKey(), entry.getValue());
        }
        balanceCache.clear();
        
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogUtil().log("Economy database closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Check if the economy system is available.
     */
    public boolean isAvailable() {
        return connection != null && EconomyConfig.getInstance().isEnabled();
    }
    
    // ==================== Account Management ====================
    
    /**
     * Check if a player has an account.
     */
    public boolean hasAccount(UUID uuid) {
        if (balanceCache.containsKey(uuid)) {
            return true;
        }
        
        String sql = "SELECT 1 FROM player_balances WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to check account: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a new account with the starting balance.
     */
    public boolean createAccount(UUID uuid, String username) {
        if (hasAccount(uuid)) {
            return false;
        }
        
        BigDecimal startingBalance = EconomyConfig.getInstance().getStartingBalance();
        
        String sql = "INSERT INTO player_balances (uuid, username, balance, last_seen) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, username);
            pstmt.setDouble(3, startingBalance.doubleValue());
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
            
            balanceCache.put(uuid, startingBalance);
            return true;
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to create account: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== Balance Operations ====================
    
    /**
     * Get a player's balance.
     */
    public BigDecimal getBalance(UUID uuid) {
        // Check cache first
        if (balanceCache.containsKey(uuid)) {
            return balanceCache.get(uuid);
        }
        
        // Load from database
        String sql = "SELECT balance FROM player_balances WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal balance = BigDecimal.valueOf(rs.getDouble("balance"));
                
                // Cache if player is online
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    balanceCache.put(uuid, balance);
                }
                
                return balance;
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to get balance: " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Set a player's balance.
     */
    public boolean setBalance(UUID uuid, BigDecimal amount) {
        EconomyConfig config = EconomyConfig.getInstance();
        
        // Enforce balance limits
        if (amount.compareTo(config.getMaximumBalance()) > 0) {
            amount = config.getMaximumBalance();
        }
        
        if (!config.isAllowNegativeBalance() && amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        } else if (config.isAllowNegativeBalance() && amount.compareTo(config.getMinimumBalance()) < 0) {
            amount = config.getMinimumBalance();
        }
        
        // Update cache
        balanceCache.put(uuid, amount);
        
        // Save to database
        return saveBalance(uuid, amount);
    }
    
    private boolean saveBalance(UUID uuid, BigDecimal amount) {
        String sql = "UPDATE player_balances SET balance = ?, last_seen = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, amount.doubleValue());
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, uuid.toString());
            int updated = pstmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to save balance: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a player has at least the specified amount.
     */
    public boolean has(UUID uuid, BigDecimal amount) {
        BigDecimal balance = getBalance(uuid);
        return balance.compareTo(amount) >= 0;
    }
    
    /**
     * Withdraw (remove) money from a player's account.
     * @return true if successful, false if insufficient funds or not allowed
     */
    public boolean withdraw(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false; // Can't withdraw negative
        }
        
        BigDecimal balance = getBalance(uuid);
        BigDecimal newBalance = balance.subtract(amount);
        
        EconomyConfig config = EconomyConfig.getInstance();
        
        // Check if withdrawal would put below minimum
        if (!config.isAllowNegativeBalance() && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (config.isAllowNegativeBalance() && newBalance.compareTo(config.getMinimumBalance()) < 0) {
            return false;
        }
        
        return setBalance(uuid, newBalance);
    }
    
    /**
     * Deposit (add) money to a player's account.
     * @return true if successful
     */
    public boolean deposit(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return false; // Can't deposit negative
        }
        
        BigDecimal balance = getBalance(uuid);
        BigDecimal newBalance = balance.add(amount);
        
        return setBalance(uuid, newBalance);
    }
    
    // ==================== Cache Management ====================
    
    /**
     * Load player balance into cache when they join.
     */
    public void loadPlayerCache(Player player) {
        if (!isAvailable()) return;
        
        UUID uuid = player.getUniqueId();
        
        // Create account if doesn't exist
        if (!hasAccount(uuid)) {
            createAccount(uuid, player.getName());
        } else {
            // Update username in case it changed
            updateUsername(uuid, player.getName());
            
            // Load balance into cache
            BigDecimal balance = getBalance(uuid);
            balanceCache.put(uuid, balance);
        }
    }
    
    /**
     * Save and remove player balance from cache when they leave.
     */
    public void unloadPlayerCache(Player player) {
        if (!isAvailable()) return;
        
        UUID uuid = player.getUniqueId();
        BigDecimal balance = balanceCache.remove(uuid);
        
        if (balance != null) {
            saveBalance(uuid, balance);
        }
    }
    
    private void updateUsername(UUID uuid, String username) {
        String sql = "UPDATE player_balances SET username = ?, last_seen = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Get UUID by username (for offline player lookups).
     */
    public UUID getUUIDByUsername(String username) {
        // First check online players
        Player online = Bukkit.getPlayerExact(username);
        if (online != null) {
            return online.getUniqueId();
        }
        
        // Check database
        String sql = "SELECT uuid FROM player_balances WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return UUID.fromString(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogUtil().log("Failed to lookup UUID: " + e.getMessage());
        }
        
        // Fall back to Bukkit offline player
        OfflinePlayer offline = Bukkit.getOfflinePlayer(username);
        if (offline.hasPlayedBefore()) {
            return offline.getUniqueId();
        }
        
        return null;
    }
    
    /**
     * Format a balance using economy config settings.
     */
    public String format(BigDecimal amount) {
        return EconomyConfig.getInstance().formatBalance(amount);
    }
    
    /**
     * Parse an amount string (supports abbreviations like 1k, 1.5M).
     */
    public BigDecimal parseAmount(String input) {
        try {
            return MathUtil.parseAbbreviatedNumber(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
