package com.pablo67340.guishop.neoforge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoPage;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.neoforge.network.OpenShopPayload;
import com.pablo67340.guishop.neoforge.network.ShopView;
import com.pablo67340.guishop.neoforge.network.ShopViewBuilder;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The {@code /shop} command.
 * <p>
 * {@code /shop} and {@code /shop <name>} open the screen; {@code /shop info}
 * prints the same data as text, which is what console and diagnostics use.
 */
public final class ShopCommand {

    private ShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ShopRegistry registry) {
        SuggestionProvider<CommandSourceStack> shopNames = (context, builder) -> {
            List<String> targets = new ArrayList<>(registry.names());
            targets.add("menu");
            return SharedSuggestionProvider.suggest(targets, builder);
        };

        dispatcher.register(Commands.literal("shop")
                .executes(context -> openShop(context.getSource(), registry, ""))
                // Literals win over the argument, so a shop named "info" or
                // "reload" would not be reachable here.
                .then(Commands.literal("info")
                        .executes(context -> listShops(context.getSource(), registry))
                        .then(Commands.argument("shop", StringArgumentType.word())
                                .suggests(shopNames)
                                .executes(context -> describeShop(
                                        context.getSource(), registry,
                                        StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource(), registry)))
                .then(Commands.argument("shop", StringArgumentType.word())
                        .suggests(shopNames)
                        .executes(context -> openShop(
                                context.getSource(), registry,
                                StringArgumentType.getString(context, "shop")))));
    }

    // ------------------------------------------------------------------- UI

    private static int openShop(CommandSourceStack source, ShopRegistry registry, String shopName) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.literal("Only a player can open the shop. Try /shop info."));
            return 0;
        }

        if (registry.names().isEmpty()) {
            source.sendFailure(Component.literal(
                    "No shops are loaded. Place shop YAML files in the guishop/shops directory and run /shop reload."));
            return 0;
        }

        if (!shopName.isEmpty() && !"menu".equalsIgnoreCase(shopName) && registry.get(shopName) == null) {
            source.sendFailure(Component.literal("No shop named '" + shopName + "'."));
            return 0;
        }

        OpenShopPayload catalogue = ShopViewBuilder.build(registry);
        ShopView opening = catalogue.shopOrMenu("menu".equalsIgnoreCase(shopName) ? "" : shopName);

        PacketDistributor.sendToPlayer(player, new OpenShopPayload(opening, catalogue.shops()));
        return 1;
    }

    // ---------------------------------------------------------- diagnostics

    private static int listShops(CommandSourceStack source, ShopRegistry registry) {
        if (registry.names().isEmpty()) {
            source.sendFailure(Component.literal(
                    "No shops are loaded. Place shop YAML files in the guishop/shops directory and run /shop reload."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("GUIShop: " + registry.names().size()
                + " shop(s), " + registry.totalItems() + " item(s)").withStyle(ChatFormatting.GREEN), false);

        for (NeoShop shop : registry.all()) {
            source.sendSuccess(() -> Component.empty()
                    .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(TextUtil.toComponent(shop.getTitle()))
                    .append(Component.literal(" (" + shop.getName() + ") ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(ShopRegistry.countItems(shop) + " items")
                            .withStyle(ChatFormatting.DARK_GRAY)), false);
        }

        NeoShop menu = registry.getMenu();
        if (menu == null) {
            source.sendSuccess(() -> Component.literal(" menu.yml not loaded")
                    .withStyle(ChatFormatting.RED), false);
        } else {
            source.sendSuccess(() -> Component.literal(" menu: "
                            + ShopRegistry.countItems(menu) + " icon(s) across "
                            + menu.getPages().size() + " page(s)")
                    .withStyle(ChatFormatting.GRAY), false);

            List<String> broken = registry.brokenMenuTargets();
            if (!broken.isEmpty()) {
                source.sendSuccess(() -> Component.literal(" menu icons point at missing shops: "
                        + String.join(", ", broken)).withStyle(ChatFormatting.RED), false);
            }
        }

        return registry.names().size();
    }

    private static int describeShop(CommandSourceStack source, ShopRegistry registry, String name) {
        // The menu is not in the shop map, but inspecting it the same way is useful.
        NeoShop shop = "menu".equalsIgnoreCase(name) ? registry.getMenu() : registry.get(name);
        if (shop == null) {
            source.sendFailure(Component.literal("No shop named '" + name + "'."));
            return 0;
        }

        source.sendSuccess(() -> Component.empty()
                .append(TextUtil.toComponent(shop.getTitle()))
                .append(Component.literal(" - " + shop.getPages().size() + " page(s)")
                        .withStyle(ChatFormatting.GRAY)), false);

        for (Map.Entry<String, NeoPage> pageEntry : shop.getPages().entrySet()) {
            NeoPage page = pageEntry.getValue();
            source.sendSuccess(() -> Component.literal("  " + pageEntry.getKey()
                            + ": " + page.getItems().size() + " items, highest slot " + page.getHighestSlot())
                    .withStyle(ChatFormatting.GRAY), false);
        }

        // Show a couple of real entries so prices and resolution are visible.
        shop.getPages().values().stream()
                .flatMap(page -> page.getItems().values().stream())
                .limit(5)
                .forEach(item -> source.sendSuccess(() -> describeItem(item), false));

        return 1;
    }

    private static Component describeItem(NeoItem item) {
        StringBuilder detail = new StringBuilder();
        detail.append("    slot ").append(item.getSlot()).append(": ").append(item.getMaterial());

        if (item.hasBuyPrice()) {
            detail.append(" buy ").append(item.getBuyPriceAsDecimal());
        }
        if (item.hasSellPrice()) {
            detail.append(" sell ").append(item.getSellPriceAsDecimal());
        }
        if (item.getItemType() != ItemType.SHOP) {
            detail.append(" [").append(item.getItemType()).append("]");
        }
        if (item.getTargetShop() != null) {
            detail.append(" -> ").append(item.getTargetShop());
        }

        // resolveItem flags unknown ids, which is the common config mistake.
        item.resolveItem();
        if (item.isResolveFailed()) {
            return Component.literal(detail + " - " + item.getResolveReason()).withStyle(ChatFormatting.RED);
        }

        return Component.literal(detail.toString()).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static int reload(CommandSourceStack source, ShopRegistry registry) {
        int loaded = registry.loadAll();
        source.sendSuccess(() -> Component.literal("GUIShop reloaded: " + loaded + " shop(s), "
                + registry.totalItems() + " item(s).").withStyle(ChatFormatting.GREEN), true);
        return loaded;
    }
}
