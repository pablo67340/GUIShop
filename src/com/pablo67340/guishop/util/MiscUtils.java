package com.pablo67340.guishop.util;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.definition.Item;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 *
 * @author bryce.wilkinson
 */
public class MiscUtils {

    /**
     * the instance of the dynamic price provider, if dynamic pricing is used
     */
    @Getter
    @lombok.Setter
    private DynamicPriceProvider DYNAMICPRICING;
    
    /**
     * An instance Vault's Economy.
     */
    @Getter
    private Economy ECONOMY;
    
    @Getter
    private Permission perms;

    public String placeholderIfy(String input, Player player, Item item) {
        String string = ChatColor.translateAlternateColorCodes('&', input);

        // Item name fallback
        String itemName;
        try {
            itemName = XMaterial.matchXMaterial(item.getMaterial()).get().name();
        } catch (Exception e) {
            itemName = item.getMaterial();
        }

        // Item placeholders
        if (item.hasShopName()) {
            string = replaceCaseInsensitive(string, "%item_shop_name%", item.getShopName());
        } else {
            string = replaceCaseInsensitive(string, "%item_shop_name%", itemName);
        }

        if (item.hasBuyName()) {
            string = replaceCaseInsensitive(string, "%item_buy_name%", item.getBuyName());
        } else {
            string = replaceCaseInsensitive(string, "%item_buy_name%", itemName);
        }

        if (item.hasBuyPrice()) {
            string = replaceCaseInsensitive(string, "%buy_price%", item.calculateBuyPrice(1).toPlainString());
        }

        if (item.hasSellPrice()) {
            string = replaceCaseInsensitive(string, "%sell_price%", item.calculateSellPrice(1).toPlainString());
        }

        string = replaceCaseInsensitive(string, "%currency_symbol%", GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix"));
        string = replaceCaseInsensitive(string, "%currency_suffix%", GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix"));

        // Player placeholders
        if (player != null) {
            String playerName = player.getName();
            String playerUuid = player.getUniqueId().toString();
            String playerWorld = player.getLocation().getWorld() != null ? player.getLocation().getWorld().getName() : "unknown";
            String playerBalance = getECONOMY().format(getECONOMY().getBalance(player));

            // Common shorthand placeholders (many plugins use these)
            string = replaceCaseInsensitive(string, "%player%", playerName);
            string = replaceCaseInsensitive(string, "{PLAYER}", playerName);
            string = replaceCaseInsensitive(string, "{PLAYER_NAME}", playerName);
            
            // Standard GUIShop placeholders
            string = replaceCaseInsensitive(string, "%player_name%", playerName);
            string = replaceCaseInsensitive(string, "%player_uuid%", playerUuid);
            string = replaceCaseInsensitive(string, "%player_world%", playerWorld);
            string = replaceCaseInsensitive(string, "%player_balance%", playerBalance);

            // PlaceholderAPI support (handles its own placeholders)
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                string = PlaceholderAPI.setPlaceholders(player, string);
            }
        }

        return string;
    }

    /**
     * Case-insensitive string replacement that supports both %placeholder% and {placeholder} formats.
     * 
     * @param source The source string
     * @param target The target to find in %placeholder% format (case-insensitive)
     * @param replacement The replacement value
     * @return The string with all case-insensitive matches replaced
     */
    private String replaceCaseInsensitive(String source, String target, String replacement) {
        if (source == null || target == null || replacement == null) {
            return source;
        }
        
        // Replace %placeholder% format (case-insensitive)
        source = source.replaceAll("(?i)" + java.util.regex.Pattern.quote(target), 
                java.util.regex.Matcher.quoteReplacement(replacement));
        
        // Also replace {placeholder} format (convert %name% to {name} pattern)
        if (target.startsWith("%") && target.endsWith("%")) {
            String bracketTarget = "{" + target.substring(1, target.length() - 1) + "}";
            source = source.replaceAll("(?i)" + java.util.regex.Pattern.quote(bracketTarget), 
                    java.util.regex.Matcher.quoteReplacement(replacement));
        }
        
        return source;
    }

    /**
     * Sends a message to the sender with the translated path and optional
     * placeholders. Supports multi-line messages (lists in messages.yml).
     *
     * @param sender The receiver
     * @param path The path to the message
     * @param params Optional, the placeholder replacements
     */
    public void sendPrefix(CommandSender sender, String path, Object... params) {
        String prefix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.prefix");
        String message = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages." + path, params);
        
        // Split on newlines to support list messages
        String[] lines = message.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Only add prefix to the first line
            if (i == 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " " + line));
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
    }

    /**
     * Sends a message to the sender with the prefix from the config
     *
     * @param sender The sender the message should be sent to
     * @param message The message the sender should receive
     */
    public void sendMessagePrefix(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.prefix") + " " + ChatColor.translateAlternateColorCodes('&', message)));
    }

    /**
     * A little helper to check if a players main hand is AIR
     *
     * @param player The player that the check should be ran on
     * @return If the main hand is null
     */
    public boolean isMainHandNull(Player player) {
        if (XMaterial.supports(0)) {
            if (player.getEquipment() != null) {
                return player.getEquipment().getItemInMainHand().getType() == Material.AIR;
            }
        } else {
            return player.getItemInHand().getType() == Material.AIR;
        }
        return true;
    }

    public void transactionLog(String input) {
        if (Config.isTransactionLog()) {
            GUIShop.getINSTANCE().getLogger().log(Level.INFO, "TRANSACTION: {0}", input);
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(GUIShop.getINSTANCE().getLogUtil().DATE_FORMAT_NOW);

        GUIShop.getINSTANCE().getLogUtil().getTransactionLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] TRANSACTION: " + input);
    }

    /**
     * Find the dynamic price provider if present
     */
    public boolean setupDynamicPricing() {
        RegisteredServiceProvider<DynamicPriceProvider> rsp = Bukkit.getServer().getServicesManager().getRegistration(DynamicPriceProvider.class);

        if (rsp == null) {
            return false;
        } else {
            rsp.getProvider();
        }
        DYNAMICPRICING = rsp.getProvider();

        return true;
    }
    
    /**
     * Formats money using the economy plugin's significant digits. <br>
     * <i>Does not add currency prefixes or suffixes. </i> <br>
     * <br>
     * If abbreviate-prices is enabled in config, formats as 1.5k, 2.3M, etc.
     * Otherwise uses economy plugin's fractional digits (e.g., 2.4193 -> 2.42).
     *
     * @param value what to format
     * @return the formatted result
     */
    public String economyFormat(BigDecimal value) {
        // Check if abbreviation is enabled
        if (Config.isAbbreviatePrices()) {
            return MathUtil.formatAbbreviated(value);
        }
        
        int digits = ECONOMY.fractionalDigits();
        return (digits == -1) ? value.toPlainString() : String.format("%." + digits + "f", value);
    }
    
    /**
     * Formats money with explicit control over abbreviation.
     *
     * @param value what to format
     * @param abbreviate true to use abbreviated format (1.5k), false for full format
     * @return the formatted result
     */
    public String economyFormat(BigDecimal value, boolean abbreviate) {
        if (abbreviate) {
            return MathUtil.formatAbbreviated(value);
        }
        
        int digits = ECONOMY.fractionalDigits();
        return (digits == -1) ? value.toPlainString() : String.format("%." + digits + "f", value);
    }
    
    /**
     * Check if Vault is present, check if an Economy plugin is present, if so,
     * hook.
     * 
     * @return True/False if economy hook successful.
     */
    public boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        RegisteredServiceProvider<Permission> rsp2 = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

        if (rsp == null || rsp2 == null) {
            return false;
        }

        ECONOMY = rsp.getProvider();
        perms = rsp2.getProvider();

        return true;
    }

}
