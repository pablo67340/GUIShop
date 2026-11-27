package com.pablo67340.guishop.neoforge.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Commands that never reach the server.
 * <p>
 * The plugin's {@code /gs toggleworth} had to be a server command, because the
 * server was the one writing worth into item lore and had to remember who did
 * not want it. Here the drawing happens on this machine, so the switch lives
 * here too: it takes effect on the next tooltip, works while the server is
 * down, and follows the player between servers.
 */
@EventBusSubscriber(modid = GuiShopMod.MOD_ID, value = Dist.CLIENT)
public final class ClientCommands {

    private ClientCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> toggle = Commands.literal("toggleworth")
                .executes(context -> {
                    boolean enabled = ClientPrefs.toggleWorth();
                    context.getSource().sendSuccess(() -> Component.literal(enabled
                                    ? "Worth is now shown in item tooltips."
                                    : "Worth is no longer shown in item tooltips.")
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
                    return 1;
                });

        event.getDispatcher().register(toggle);
    }

    /**
     * Forgets the prices on the way out, so a single player world cannot show
     * the tooltips of the last server the player was on.
     */
    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        WorthTooltip.clear();
    }
}
