package com.pablo67340.guishop.definition;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record Permission(String permission) {
    public boolean hasPermission(CommandSender sender) {
        if (permission == null) return true;

        if (!(sender instanceof Player player)) return true;

        if (player.isOp()) return true;

        return player.hasPermission(permission);
    }
}
