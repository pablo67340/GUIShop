package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.GUIShop;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Vault Economy implementation for GUIShop's internal economy.
 * 
 * NOTE: This class does NOT cache EconomyManager or EconomyConfig references.
 * It always fetches them dynamically via getInstance() to support hot-reloading.
 */
public class GUIShopEconomy implements Economy {
    
    private final GUIShop plugin;
    
    public GUIShopEconomy(GUIShop plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get the current EconomyManager instance (supports hot-reload).
     */
    private EconomyManager getManager() {
        return EconomyManager.getInstance();
    }
    
    /**
     * Get the current EconomyConfig instance (supports hot-reload).
     */
    private EconomyConfig getConfig() {
        return EconomyConfig.getInstance();
    }
    
    @Override
    public boolean isEnabled() {
        EconomyConfig config = getConfig();
        EconomyManager manager = getManager();
        return config != null && config.isEnabled() && manager != null && manager.isAvailable();
    }
    
    @Override
    public String getName() {
        return "GUIShop Economy";
    }
    
    @Override
    public boolean hasBankSupport() {
        return false; // No bank support
    }
    
    @Override
    public int fractionalDigits() {
        return getConfig().getDecimalPlaces();
    }
    
    @Override
    public String format(double amount) {
        return getConfig().formatBalance(BigDecimal.valueOf(amount));
    }
    
    @Override
    public String currencyNamePlural() {
        return getConfig().getCurrencyNamePlural();
    }
    
    @Override
    public String currencyNameSingular() {
        return getConfig().getCurrencyName();
    }
    
    // ==================== Account Methods ====================
    
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return getManager().hasAccount(player.getUniqueId());
    }
    
    @Override
    public boolean hasAccount(String playerName) {
        EconomyManager manager = getManager();
        UUID uuid = manager.getUUIDByUsername(playerName);
        return uuid != null && manager.hasAccount(uuid);
    }
    
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player); // World-specific not supported
    }
    
    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }
    
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        String name = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        return getManager().createAccount(player.getUniqueId(), name);
    }
    
    @Override
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return createPlayerAccount(player);
    }
    
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
    
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }
    
    // ==================== Balance Methods ====================
    
    @Override
    public double getBalance(OfflinePlayer player) {
        return getManager().getBalance(player.getUniqueId()).doubleValue();
    }
    
    @Override
    public double getBalance(String playerName) {
        EconomyManager manager = getManager();
        UUID uuid = manager.getUUIDByUsername(playerName);
        if (uuid == null) return 0;
        return manager.getBalance(uuid).doubleValue();
    }
    
    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }
    
    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }
    
    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getManager().has(player.getUniqueId(), BigDecimal.valueOf(amount));
    }
    
    @Override
    public boolean has(String playerName, double amount) {
        EconomyManager manager = getManager();
        UUID uuid = manager.getUUIDByUsername(playerName);
        if (uuid == null) return false;
        return manager.has(uuid, BigDecimal.valueOf(amount));
    }
    
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
    
    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }
    
    // ==================== Transaction Methods ====================
    
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), 
                EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount");
        }
        
        if (!hasAccount(player)) {
            return new EconomyResponse(0, 0, 
                EconomyResponse.ResponseType.FAILURE, "Player does not have an account");
        }
        
        EconomyManager manager = getManager();
        EconomyConfig config = getConfig();
        BigDecimal amountBD = BigDecimal.valueOf(amount);
        
        if (!manager.has(player.getUniqueId(), amountBD) && !config.isAllowNegativeBalance()) {
            return new EconomyResponse(0, getBalance(player), 
                EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        }
        
        boolean success = manager.withdraw(player.getUniqueId(), amountBD);
        
        if (success) {
            return new EconomyResponse(amount, getBalance(player), 
                EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, getBalance(player), 
                EconomyResponse.ResponseType.FAILURE, "Transaction failed");
        }
    }
    
    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return withdrawPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), 
                EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount");
        }
        
        if (!hasAccount(player)) {
            createPlayerAccount(player);
        }
        
        boolean success = getManager().deposit(player.getUniqueId(), BigDecimal.valueOf(amount));
        
        if (success) {
            return new EconomyResponse(amount, getBalance(player), 
                EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, getBalance(player), 
                EconomyResponse.ResponseType.FAILURE, "Transaction failed");
        }
    }
    
    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return depositPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }
    
    // ==================== Bank Methods (Not Supported) ====================
    
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }
    
    @Override
    public List<String> getBanks() {
        return List.of();
    }
}
