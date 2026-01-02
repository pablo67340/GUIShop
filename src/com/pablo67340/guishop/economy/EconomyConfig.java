package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.GUIShop;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Configuration for the internal GUIShop economy system.
 */
public class EconomyConfig {
    
    private static EconomyConfig instance;
    
    private final GUIShop plugin;
    private File configFile;
    private FileConfiguration config;
    
    // Economy settings
    @Getter private boolean enabled;
    @Getter private String currencyName;
    @Getter private String currencyNamePlural;
    @Getter private String currencySymbol;
    @Getter private boolean symbolPrefix; // true = $100, false = 100$
    @Getter private int decimalPlaces;
    @Getter private boolean useThousandsSeparator;
    @Getter private String thousandsSeparator;
    @Getter private String decimalSeparator;
    
    // Balance settings
    @Getter private BigDecimal startingBalance;
    @Getter private boolean allowNegativeBalance;
    @Getter private BigDecimal minimumBalance; // Only used if negative allowed
    @Getter private BigDecimal maximumBalance;
    
    // Formatting
    @Getter private boolean abbreviateBalances;
    @Getter private String balanceFormat; // e.g., "%symbol%%amount%" or "%amount% %currency%"
    
    // Command settings
    @Getter private boolean balanceCommandEnabled;
    @Getter private boolean payCommandEnabled;
    @Getter private BigDecimal payMinimum;
    @Getter private BigDecimal payMaximum;
    @Getter private boolean payOfflinePlayers;
    
    // Messages
    @Getter private String messageBalanceSelf;
    @Getter private String messageBalanceOther;
    @Getter private String messagePaySent;
    @Getter private String messagePayReceived;
    @Getter private String messagePaySelfError;
    @Getter private String messagePayInsufficient;
    @Getter private String messagePayMinimumError;
    @Getter private String messagePayMaximumError;
    @Getter private String messagePayPlayerNotFound;
    @Getter private String messagePayInvalidAmount;
    @Getter private String messageEconomyDisabled;
    @Getter private String messageNotificationsEnabled;
    @Getter private String messageNotificationsDisabled;
    
    public EconomyConfig(GUIShop plugin) {
        this.plugin = plugin;
        instance = this;
    }
    
    public static EconomyConfig getInstance() {
        return instance;
    }
    
    /**
     * Load or create the economy.yml configuration.
     */
    public void load() {
        configFile = new File(plugin.getDataFolder(), "economy.yml");
        
        if (!configFile.exists()) {
            // Copy the default config from resources (preserves comments)
            plugin.saveResource("economy.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }
    
    private void loadValues() {
        enabled = config.getBoolean("enabled", false);
        
        // Currency
        currencyName = config.getString("currency.name", "Dollar");
        currencyNamePlural = config.getString("currency.name-plural", "Dollars");
        currencySymbol = config.getString("currency.symbol", "$");
        symbolPrefix = config.getBoolean("currency.symbol-prefix", true);
        
        // Formatting
        decimalPlaces = config.getInt("formatting.decimal-places", 2);
        useThousandsSeparator = config.getBoolean("formatting.use-thousands-separator", true);
        thousandsSeparator = config.getString("formatting.thousands-separator", ",");
        decimalSeparator = config.getString("formatting.decimal-separator", ".");
        abbreviateBalances = config.getBoolean("formatting.abbreviate-large-numbers", true);
        balanceFormat = config.getString("formatting.balance-format", "%symbol%%amount%");
        
        // Balance
        startingBalance = BigDecimal.valueOf(config.getDouble("balance.starting-balance", 1000.0));
        allowNegativeBalance = config.getBoolean("balance.allow-negative", false);
        minimumBalance = BigDecimal.valueOf(config.getDouble("balance.minimum-balance", -10000.0));
        maximumBalance = BigDecimal.valueOf(config.getDouble("balance.maximum-balance", 1000000000000.0));
        
        // Command settings
        balanceCommandEnabled = config.getBoolean("commands.balance-enabled", true);
        payCommandEnabled = config.getBoolean("commands.pay-enabled", true);
        payMinimum = BigDecimal.valueOf(config.getDouble("commands.pay-minimum", 1.0));
        payMaximum = BigDecimal.valueOf(config.getDouble("commands.pay-maximum", 0));
        payOfflinePlayers = config.getBoolean("commands.pay-offline-players", false);
        
        // Messages
        messageBalanceSelf = config.getString("messages.balance-self", "&7Your balance: &a%balance%");
        messageBalanceOther = config.getString("messages.balance-other", "&7%player%'s balance: &a%balance%");
        messagePaySent = config.getString("messages.pay-sent", "&aYou sent %amount% to %player%.");
        messagePayReceived = config.getString("messages.pay-received", "&aYou received %amount% from %player%.");
        messagePaySelfError = config.getString("messages.pay-self-error", "&cYou cannot pay yourself!");
        messagePayInsufficient = config.getString("messages.pay-insufficient", "&cInsufficient funds. You have %balance%.");
        messagePayMinimumError = config.getString("messages.pay-minimum-error", "&cMinimum payment amount is %minimum%.");
        messagePayMaximumError = config.getString("messages.pay-maximum-error", "&cMaximum payment amount is %maximum%.");
        messagePayPlayerNotFound = config.getString("messages.pay-player-not-found", "&cPlayer '%player%' not found.");
        messagePayInvalidAmount = config.getString("messages.pay-invalid-amount", "&cInvalid amount: %amount%");
        messageEconomyDisabled = config.getString("messages.economy-disabled", "&cEconomy system is not enabled.");
        messageNotificationsEnabled = config.getString("messages.notifications-enabled", "&aPayment notifications enabled.");
        messageNotificationsDisabled = config.getString("messages.notifications-disabled", "&cPayment notifications disabled.");
    }
    
    /**
     * Reload the configuration.
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }
    
    /**
     * Format a balance amount according to config settings.
     */
    public String formatBalance(BigDecimal amount) {
        String formatted;
        
        if (abbreviateBalances && amount.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) {
            formatted = formatAbbreviated(amount);
        } else {
            formatted = formatNumber(amount);
        }
        
        // Apply balance format
        String result = balanceFormat
            .replace("%symbol%", currencySymbol)
            .replace("%amount%", formatted)
            .replace("%currency%", amount.abs().compareTo(BigDecimal.ONE) == 0 ? currencyName : currencyNamePlural)
            .replace("%currency_plural%", currencyNamePlural);
        
        return result;
    }
    
    /**
     * Format a number with proper decimal places and separators.
     */
    public String formatNumber(BigDecimal amount) {
        // Round to decimal places
        amount = amount.setScale(decimalPlaces, java.math.RoundingMode.HALF_UP);
        
        String[] parts = amount.toPlainString().split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "";
        
        // Pad decimal part if needed
        while (decimalPart.length() < decimalPlaces) {
            decimalPart += "0";
        }
        
        // Add thousands separators
        if (useThousandsSeparator) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            boolean negative = integerPart.startsWith("-");
            String digits = negative ? integerPart.substring(1) : integerPart;
            
            for (int i = digits.length() - 1; i >= 0; i--) {
                if (count > 0 && count % 3 == 0) {
                    sb.insert(0, thousandsSeparator);
                }
                sb.insert(0, digits.charAt(i));
                count++;
            }
            
            integerPart = (negative ? "-" : "") + sb.toString();
        }
        
        if (decimalPlaces > 0) {
            return integerPart + decimalSeparator + decimalPart;
        } else {
            return integerPart;
        }
    }
    
    /**
     * Format a number in abbreviated form (1K, 1.5M, etc.)
     */
    public String formatAbbreviated(BigDecimal amount) {
        double value = amount.doubleValue();
        boolean negative = value < 0;
        value = Math.abs(value);
        
        String suffix = "";
        if (value >= 1_000_000_000_000.0) {
            value /= 1_000_000_000_000.0;
            suffix = "T";
        } else if (value >= 1_000_000_000.0) {
            value /= 1_000_000_000.0;
            suffix = "B";
        } else if (value >= 1_000_000.0) {
            value /= 1_000_000.0;
            suffix = "M";
        } else if (value >= 1_000.0) {
            value /= 1_000.0;
            suffix = "K";
        }
        
        String formatted;
        if (value == Math.floor(value) && value < 1000) {
            formatted = String.format("%.0f", value);
        } else {
            formatted = String.format("%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        
        return (negative ? "-" : "") + formatted + suffix;
    }
}
