package com.pablo67340.guishop.neoforge.permission;

import com.mojang.authlib.GameProfile;
import com.pablo67340.guishop.neoforge.GuiShopMod;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Who is allowed to change the shops and the server settings.
 * <p>
 * Registered as a NeoForge permission node so permission mods such as
 * LuckPerms can grant it by name, with an operator check as the default. The
 * node is the only thing a server admin needs to know about:
 * {@code guishop.edit}.
 */
public final class GuiShopPermissions {

    /** Level 2 is the vanilla threshold for commands that change the world. */
    private static final int OPERATOR_LEVEL = 2;

    /**
     * Mirrors the Bukkit plugin's {@code guishop.edit} permission, so the same
     * grant works on either platform.
     */
    public static final PermissionNode<Boolean> EDIT = new PermissionNode<>(
            ResourceLocation.fromNamespaceAndPath(GuiShopMod.MOD_ID, "edit"),
            PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && player.hasPermissions(OPERATOR_LEVEL))
            .setInformation(
                    Component.literal("Edit GUIShop"),
                    Component.literal("Change shop items and server settings from the shop screen."));

    private GuiShopPermissions() {
    }

    /**
     * Fired on the game bus during server startup, before anything may query a
     * node.
     */
    public static void register(PermissionGatherEvent.Nodes event) {
        event.addNodes(EDIT);
        GuiShopPlatform.get().logger().info("Registered permission node " + EDIT.getNodeName() + ".");
    }

    /**
     * Whether this player may change shops and server settings.
     * <p>
     * Must be consulted on the server for every change, not just when deciding
     * what to show: a modified client can send any packet it likes, so the
     * screen hiding a control is a convenience rather than a protection.
     *
     * @param player the player to check, or null for a non-player source
     */
    public static boolean canEdit(ServerPlayer player) {
        if (player == null) {
            return false;
        }

        // Whoever opened a singleplayer world owns it, whether or not cheats
        // are on, and would otherwise be locked out of their own shops.
        // Compared through the profile rather than with isSingleplayerOwner,
        // which lives on the client-only IntegratedServer and would not resolve
        // on a dedicated server.
        MinecraftServer server = player.getServer();
        if (server != null) {
            GameProfile owner = server.getSingleplayerProfile();
            if (owner != null && owner.getName().equalsIgnoreCase(player.getGameProfile().getName())) {
                return true;
            }
        }

        try {
            return PermissionAPI.getPermission(player, EDIT);
        } catch (RuntimeException exception) {
            // The node is unregistered if the permission API never initialised.
            // Falling back keeps operators in control rather than locking
            // everyone out.
            return player.hasPermissions(OPERATOR_LEVEL);
        }
    }
}
