package com.pablo67340.guishop.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pablo67340.guishop.economy.DynamicPricingSettings;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * The {@code /shop} command.
 * <p>
 * Until the screens exist this reports what the shared core module can see,
 * which is what proves the cross-module wiring works at runtime rather than
 * only at compile time.
 */
public final class ShopCommand {

    private ShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    DynamicPricingSettings pricing = new DynamicPricingSettings();

                    source.sendSuccess(() -> Component.literal("GUIShop is loaded."), false);
                    source.sendSuccess(() -> Component.literal(
                            "Data folder: " + GuiShopPlatform.get().dataFolder()), false);
                    source.sendSuccess(() -> Component.literal(
                            "Price bounds from core: "
                                    + pricing.getMinPriceMultiplier() + "x - "
                                    + pricing.getMaxPriceMultiplier() + "x"), false);
                    source.sendSuccess(() -> Component.literal(
                            "Shop screens are not implemented yet."), false);

                    return 1;
                }));
    }
}
