package com.pablo67340.guishop.economy;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.statistics.StatisticsManager;
import com.pablo67340.guishop.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handles /bal, /balance, and /pay commands for the internal economy.
 */
public class EconomyCommands implements CommandExecutor {
    
    private final GUIShop plugin;
    
    public EconomyCommands(GUIShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        EconomyManager ecoManager = EconomyManager.getInstance();
        EconomyConfig ecoConfig = EconomyConfig.getInstance();
        
        // Check if economy is enabled
        if (ecoManager == null || !ecoManager.isAvailable()) {
            sender.sendMessage(colorize(ecoConfig != null ? ecoConfig.getMessageEconomyDisabled() : "&cEconomy system is not enabled."));
            return true;
        }
        
        String cmdName = command.getName().toLowerCase();
        
        switch (cmdName) {
            case "bal", "balance", "money" -> handleBalance(sender, args);
            case "pay", "send" -> handlePay(sender, args);
            case "togglepay", "paytoggle" -> handleTogglePay(sender);
        }
        
        return true;
    }
    
    /**
     * Handle /togglepay command.
     */
    private void handleTogglePay(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }
        
        EconomyConfig ecoConfig = EconomyConfig.getInstance();
        StatisticsManager statsManager = StatisticsManager.getInstance();
        
        if (statsManager == null || !statsManager.isAvailable()) {
            sender.sendMessage(ChatColor.RED + "This feature is not available.");
            return;
        }
        
        boolean newState = statsManager.togglePayNotifications(player.getUniqueId());
        
        if (newState) {
            sender.sendMessage(colorize(ecoConfig.getMessageNotificationsEnabled()));
        } else {
            sender.sendMessage(colorize(ecoConfig.getMessageNotificationsDisabled()));
        }
    }
    
    /**
     * Handle /bal and /balance commands.
     */
    private void handleBalance(CommandSender sender, String[] args) {
        EconomyManager ecoManager = EconomyManager.getInstance();
        EconomyConfig ecoConfig = EconomyConfig.getInstance();
        
        if (!ecoConfig.isBalanceCommandEnabled()) {
            sender.sendMessage(ChatColor.RED + "Balance command is disabled.");
            return;
        }
        
        // /bal - show own balance
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /bal <player>");
                return;
            }
            
            // Ensure account exists
            if (!ecoManager.hasAccount(player.getUniqueId())) {
                ecoManager.createAccount(player.getUniqueId(), player.getName());
            }
            
            BigDecimal balance = ecoManager.getBalance(player.getUniqueId());
            String formatted = ecoManager.format(balance);
            
            String message = ecoConfig.getMessageBalanceSelf()
                .replace("%balance%", formatted);
            sender.sendMessage(colorize(message));
            return;
        }
        
        // /bal <player> - show other's balance (requires permission)
        if (!sender.hasPermission("guishop.economy.balance.others") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to check other players' balances.");
            return;
        }
        
        String targetName = args[0];
        UUID targetUUID = ecoManager.getUUIDByUsername(targetName);
        
        if (targetUUID == null || !ecoManager.hasAccount(targetUUID)) {
            String message = ecoConfig.getMessagePayPlayerNotFound()
                .replace("%player%", targetName);
            sender.sendMessage(colorize(message));
            return;
        }
        
        BigDecimal balance = ecoManager.getBalance(targetUUID);
        String formatted = ecoManager.format(balance);
        
        // Get actual player name
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        String displayName = target.getName() != null ? target.getName() : targetName;
        
        String message = ecoConfig.getMessageBalanceOther()
            .replace("%player%", displayName)
            .replace("%balance%", formatted);
        sender.sendMessage(colorize(message));
    }
    
    /**
     * Handle /pay command.
     */
    private void handlePay(CommandSender sender, String[] args) {
        EconomyManager ecoManager = EconomyManager.getInstance();
        EconomyConfig ecoConfig = EconomyConfig.getInstance();
        
        if (!ecoConfig.isPayCommandEnabled()) {
            sender.sendMessage(ChatColor.RED + "Pay command is disabled.");
            return;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use /pay.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
            sender.sendMessage(ChatColor.GRAY + "Supports abbreviations: 1k, 1.5M, 100B");
            return;
        }
        
        String targetName = args[0];
        String amountStr = args[1];
        
        // Parse amount
        BigDecimal amount;
        try {
            amount = MathUtil.parseAbbreviatedNumber(amountStr);
        } catch (NumberFormatException e) {
            String message = ecoConfig.getMessagePayInvalidAmount()
                .replace("%amount%", amountStr);
            sender.sendMessage(colorize(message));
            return;
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            String message = ecoConfig.getMessagePayInvalidAmount()
                .replace("%amount%", amountStr);
            sender.sendMessage(colorize(message));
            return;
        }
        
        // Check minimum
        if (amount.compareTo(ecoConfig.getPayMinimum()) < 0) {
            String message = ecoConfig.getMessagePayMinimumError()
                .replace("%minimum%", ecoManager.format(ecoConfig.getPayMinimum()));
            sender.sendMessage(colorize(message));
            return;
        }
        
        // Check maximum (if set)
        if (ecoConfig.getPayMaximum().compareTo(BigDecimal.ZERO) > 0 && 
            amount.compareTo(ecoConfig.getPayMaximum()) > 0) {
            String message = ecoConfig.getMessagePayMaximumError()
                .replace("%maximum%", ecoManager.format(ecoConfig.getPayMaximum()));
            sender.sendMessage(colorize(message));
            return;
        }
        
        // Find target player
        UUID targetUUID = ecoManager.getUUIDByUsername(targetName);
        Player targetOnline = Bukkit.getPlayerExact(targetName);
        
        if (targetUUID == null) {
            String message = ecoConfig.getMessagePayPlayerNotFound()
                .replace("%player%", targetName);
            sender.sendMessage(colorize(message));
            return;
        }
        
        // Check if paying self
        if (targetUUID.equals(player.getUniqueId())) {
            sender.sendMessage(colorize(ecoConfig.getMessagePaySelfError()));
            return;
        }
        
        // Check if target is offline and that's not allowed
        if (targetOnline == null && !ecoConfig.isPayOfflinePlayers()) {
            String message = ecoConfig.getMessagePayPlayerNotFound()
                .replace("%player%", targetName);
            sender.sendMessage(colorize(message));
            return;
        }
        
        // Ensure both accounts exist
        if (!ecoManager.hasAccount(player.getUniqueId())) {
            ecoManager.createAccount(player.getUniqueId(), player.getName());
        }
        if (!ecoManager.hasAccount(targetUUID)) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            ecoManager.createAccount(targetUUID, target.getName() != null ? target.getName() : targetName);
        }
        
        // Check sender has enough
        BigDecimal senderBalance = ecoManager.getBalance(player.getUniqueId());
        if (senderBalance.compareTo(amount) < 0) {
            String message = ecoConfig.getMessagePayInsufficient()
                .replace("%balance%", ecoManager.format(senderBalance));
            sender.sendMessage(colorize(message));
            return;
        }
        
        // Perform the transfer
        boolean withdrawSuccess = ecoManager.withdraw(player.getUniqueId(), amount);
        if (!withdrawSuccess) {
            String message = ecoConfig.getMessagePayInsufficient()
                .replace("%balance%", ecoManager.format(senderBalance));
            sender.sendMessage(colorize(message));
            return;
        }
        
        boolean depositSuccess = ecoManager.deposit(targetUUID, amount);
        if (!depositSuccess) {
            // Refund if deposit fails
            ecoManager.deposit(player.getUniqueId(), amount);
            sender.sendMessage(ChatColor.RED + "Payment failed. Please try again.");
            return;
        }
        
        // Get target display name
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        String targetDisplayName = target.getName() != null ? target.getName() : targetName;
        String formattedAmount = ecoManager.format(amount);
        
        // Send success messages
        String sentMessage = ecoConfig.getMessagePaySent()
            .replace("%amount%", formattedAmount)
            .replace("%player%", targetDisplayName);
        sender.sendMessage(colorize(sentMessage));
        
        // Notify recipient if online and has notifications enabled
        if (targetOnline != null) {
            StatisticsManager statsManager = StatisticsManager.getInstance();
            boolean notificationsEnabled = statsManager == null || 
                !statsManager.isAvailable() || 
                statsManager.isPayNotificationsEnabled(targetUUID);
            
            if (notificationsEnabled) {
                String receivedMessage = ecoConfig.getMessagePayReceived()
                    .replace("%amount%", formattedAmount)
                    .replace("%player%", player.getName());
                targetOnline.sendMessage(colorize(receivedMessage));
            }
        }
    }
    
    /**
     * Colorize a message using & codes.
     */
    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
