package com.pablo67340.guishop.definition;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public record Permission(String permission) {
    public boolean doesntHavePermission(CommandSender sender) {
        if (permission == null) return false;

        if (!(sender instanceof Player player)) return false;

        if (player.isOp()) return false;

        return !player.hasPermission(permission);
    }
}
