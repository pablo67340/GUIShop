package com.pablo67340.guishop.commands;

import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.listenable.Value;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command executor for /value command.
 * Shows the buy/sell value of an item from the shop.
 */
public class ValueCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (GUIShop.isNoEconomySystem()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(sender, "no-economy-system");
            return true;
        }

        if (!(sender instanceof Player)) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("guishop.value") && !player.isOp()) {
            GUIShop.getINSTANCE().getMiscUtils().sendPrefix(player, "no-permission");
            return true;
        }

        String targetMaterial;

        if (args.length >= 1) {
            // Material name provided as argument
            String materialArg = args[0].toUpperCase();
            
            // Try to match the material
            try {
                // First try exact match
                Material mat = Material.matchMaterial(materialArg);
                if (mat != null) {
                    targetMaterial = mat.toString();
                } else {
                    // Try XMaterial for cross-version support
                    if (XMaterial.matchXMaterial(materialArg).isPresent()) {
                        targetMaterial = XMaterial.matchXMaterial(materialArg).get().parseMaterial().toString();
                    } else {
                        sendMessage(player, "value.invalid-material", 
                                "&cInvalid material: &f" + materialArg + "&c. Please enter a valid material name.",
                                materialArg);
                        return true;
                    }
                }
            } catch (Exception e) {
                sendMessage(player, "value.invalid-material", 
                        "&cInvalid material: &f" + materialArg + "&c. Please enter a valid material name.",
                        materialArg);
                return true;
            }
        } else {
            // No argument - use held item
            if (GUIShop.getINSTANCE().getMiscUtils().isMainHandNull(player)) {
                sendMessage(player, "value.usage", 
                        "&fUsage: &e/value <material> &for hold an item and type &e/value");
                return true;
            }

            ItemStack item;
            if (XMaterial.getVersion() > 18) {
                item = player.getEquipment().getItemInMainHand();
            } else {
                item = player.getItemInHand();
            }

            targetMaterial = item.getType().toString();
        }

        // Check if the material exists in the item table
        if (!GUIShop.getINSTANCE().getITEMTABLE().containsKey(targetMaterial)) {
            sendMessage(player, "value.doesnt-exist",
                    "&cThis item doesn't exist in any of the shops.");
            return true;
        }

        // Open the value GUI
        Value value = new Value(player, targetMaterial);
        value.loadItems();
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toUpperCase();
            
            // Return materials that exist in the item table
            return GUIShop.getINSTANCE().getITEMTABLE().keySet().stream()
                    .filter(mat -> mat.startsWith(partial))
                    .sorted()
                    .limit(20) // Limit suggestions to prevent lag
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Send a message to the player, using the config message if available, otherwise using fallback.
     *
     * @param player The player to send the message to
     * @param path The message path (without "messages." prefix)
     * @param fallback The fallback message if config message is missing
     * @param params Optional parameters for placeholder replacement
     */
    private void sendMessage(Player player, String path, String fallback, Object... params) {
        String message = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages." + path, params);
        
        // Check if message was found (MessageSystem returns "path cannot be null!" for missing messages)
        if (message.contains("cannot be null!")) {
            // Use fallback message
            message = fallback;
            // Simple placeholder replacement for fallback
            if (params.length > 0 && message.contains("%material%")) {
                message = message.replace("%material%", params[0].toString());
            }
        }
        
        String prefix = GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.prefix");
        if (prefix.contains("cannot be null!")) {
            prefix = "&7[&bGUIShop&7]";
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " " + message));
    }
}

