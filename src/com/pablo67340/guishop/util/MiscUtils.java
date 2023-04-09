/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

        if (item.hasShopName()) {
            string = string.replace("%item_shop_name%", item.getShopName());
        } else {
            string = string.replace("%item_shop_name%", XMaterial.matchXMaterial(item.getMaterial()).get().name());
        }

        if (item.hasBuyName()) {
            string = string.replace("%item_buy_name%", item.getBuyName());
        } else {
            string = string.replace("%item_buy_name%", XMaterial.matchXMaterial(item.getMaterial()).get().name());
        }

        if (item.hasBuyPrice()) {
            string = string.replace("%buy_price%", item.calculateBuyPrice(1).toPlainString());
        }

        if (item.hasSellPrice()) {
            string = string.replace("%sell_price%", item.calculateSellPrice(1).toPlainString());
        }

        string = string.replace("%currency_symbol%", GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix"));
        string = string.replace("%currency_suffix%", GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix"));

        if (player != null) {
            string = string.replace("%player_name%", player.getName());
            string = string.replace("%player_uuid%", player.getUniqueId().toString());
            string = string.replace("%player_world%", player.getLocation().getWorld().getName());
            string = string.replace("%player_balance%", getECONOMY().format(getECONOMY().getBalance(player)));

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                string = PlaceholderAPI.setPlaceholders(player, string);
            }
        }

        return string;
    }

    /**
     * Sends a message to the sender with the translated path and optional
     * placeholders
     *
     * @param sender The receiver
     * @param path The path to the message
     * @param params Optional, the placeholder replacements
     */
    public void sendPrefix(CommandSender sender, String path, Object... params) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.prefix") + " " + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages." + path, params)));
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
     * Example: 2.4193 -> 2.42 <br>
     * Prevents scientific notation being displayed on items.
     *
     * @param value what to format
     * @return the formatted result
     */
    public String economyFormat(BigDecimal value) {
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
