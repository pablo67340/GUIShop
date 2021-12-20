package com.pablo67340.guishop.definition;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Permission {

    @Getter
    public String permission;

    public Permission(String permission) {
        this.permission = permission;
    }

    public boolean doesntHavePermission(CommandSender sender) {
        if (permission == null) {
            return false;
        }

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (player.isOp()) {
            return false;
        }

        return !player.hasPermission(permission);
    }
}
