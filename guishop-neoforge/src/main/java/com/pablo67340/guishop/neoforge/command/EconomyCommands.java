package com.pablo67340.guishop.neoforge.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pablo67340.guishop.neoforge.config.Messages;
import com.pablo67340.guishop.neoforge.economy.BalanceStore;
import com.pablo67340.guishop.neoforge.permission.GuiShopPermissions;
import com.pablo67340.guishop.neoforge.server.GuiShopServer;
import com.pablo67340.guishop.neoforge.statistics.StatisticsStore;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import com.pablo67340.guishop.util.MathUtil;
import com.pablo67340.guishop.util.NameUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Money and statistics commands.
 * <p>
 * The plugin registers these through Vault, which is how any other plugin on a
 * Bukkit server finds an economy. NeoForge has no such interface, so these are
 * the economy: they read and write the mod's own balances directly.
 * <p>
 * Every amount accepts the abbreviations the plugin accepted, so {@code 1.5k}
 * and {@code 100B} mean what someone used to them expects.
 */
public final class EconomyCommands {

    /** Guards against a typo adding thirteen zeroes to someone's balance. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

    private EconomyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> balance = Commands.literal("bal")
                .executes(context -> ownBalance(context.getSource()))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(knownPlayers())
                        .requires(EconomyCommands::maySeeOthers)
                        .executes(context -> otherBalance(context.getSource(),
                                StringArgumentType.getString(context, "player"))));

        dispatcher.register(balance);
        // Registered separately rather than redirected, because a redirect
        // shows up in the client's command tree as "balance -> bal" and the
        // usage text then names a command the player did not type.
        dispatcher.register(Commands.literal("balance")
                .executes(context -> ownBalance(context.getSource()))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(knownPlayers())
                        .requires(EconomyCommands::maySeeOthers)
                        .executes(context -> otherBalance(context.getSource(),
                                StringArgumentType.getString(context, "player")))));
        dispatcher.register(Commands.literal("money")
                .executes(context -> ownBalance(context.getSource())));

        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(knownPlayers())
                        .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(context -> pay(context.getSource(),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "amount"))))));

        dispatcher.register(Commands.literal("togglepay")
                .executes(context -> togglePay(context.getSource())));
    }

    /**
     * The admin side, hung off whatever root command the caller provides, so
     * the same subcommands appear under both {@code /shop} and {@code /gs}.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> adminTree() {
        return Commands.literal("eco")
                .requires(source -> source.hasPermission(2))
                .then(amountCommand("give", Action.GIVE))
                .then(amountCommand("take", Action.TAKE))
                .then(amountCommand("set", Action.SET))
                .then(Commands.literal("balance")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(knownPlayers())
                                .executes(context -> otherBalance(context.getSource(),
                                        StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(knownPlayers())
                                .executes(context -> reset(context.getSource(),
                                        StringArgumentType.getString(context, "player")))));
    }

    /**
     * The statistics side, likewise hung off either root.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> statsTree() {
        return Commands.literal("stats")
                .executes(context -> ownStats(context.getSource()))
                .then(Commands.literal("top")
                        .executes(context -> serverTop(context.getSource())))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(knownPlayers())
                        .requires(EconomyCommands::maySeeOthers)
                        .executes(context -> otherStats(context.getSource(),
                                StringArgumentType.getString(context, "player"))));
    }

    private enum Action { GIVE, TAKE, SET }

    private static LiteralArgumentBuilder<CommandSourceStack> amountCommand(String name, Action action) {
        return Commands.literal(name)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(knownPlayers())
                        .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(context -> adjust(context.getSource(), action,
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "amount")))));
    }

    // ---------------------------------------------------------------- balance

    private static int ownBalance(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            source.sendFailure(Messages.get("only-player"));
            return 0;
        }

        BigDecimal balance = balances().get(player.getUUID());
        source.sendSuccess(() -> Component.literal("Balance: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(money(balance)).withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    private static int otherBalance(CommandSourceStack source, String name) {
        Target target = resolve(source, name);
        if (target == null) {
            return 0;
        }

        source.sendSuccess(() -> Component.literal(target.name() + "'s balance: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(money(balances().get(target.uuid())))
                        .withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    // -------------------------------------------------------------------- pay

    private static int pay(CommandSourceStack source, String name, String rawAmount) {
        ServerPlayer sender = playerOrNull(source);
        if (sender == null) {
            source.sendFailure(Messages.get("only-player"));
            return 0;
        }

        Target target = resolve(source, name);
        if (target == null) {
            return 0;
        }
        if (target.uuid().equals(sender.getUUID())) {
            source.sendFailure(Component.literal("You cannot pay yourself.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        BigDecimal amount = amount(source, rawAmount);
        if (amount == null) {
            return 0;
        }
        if (amount.signum() <= 0) {
            source.sendFailure(Component.literal("Pay a positive amount.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // Taken before it is given, so a failure cannot create money.
        if (!balances().withdraw(sender.getUUID(), amount)) {
            source.sendFailure(Messages.get("not-enough-money", "needed", money(amount)));
            return 0;
        }
        balances().deposit(target.uuid(), amount);

        source.sendSuccess(() -> Component.literal("Paid " + money(amount) + " to " + target.name() + ".")
                .withStyle(ChatFormatting.GREEN), false);

        ServerPlayer recipient = source.getServer().getPlayerList().getPlayer(target.uuid());
        if (recipient != null && statistics().payNotifications(target.uuid())) {
            recipient.sendSystemMessage(Component.literal(
                            sender.getGameProfile().getName() + " paid you " + money(amount) + ".")
                    .withStyle(ChatFormatting.GREEN));
        }

        GuiShopPlatform.get().logUtil().transactionLog(
                sender.getGameProfile().getName() + " paid " + target.name() + " "
                        + amount.toPlainString() + ".");
        return 1;
    }

    private static int togglePay(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            source.sendFailure(Messages.get("only-player"));
            return 0;
        }

        boolean enabled = !statistics().payNotifications(player.getUUID());
        statistics().setPayNotifications(player.getUUID(), enabled);

        source.sendSuccess(() -> Component.literal(enabled
                        ? "You will be told when someone pays you."
                        : "You will no longer be told when someone pays you.")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return 1;
    }

    // ------------------------------------------------------------------ admin

    private static int adjust(CommandSourceStack source, Action action, String name, String rawAmount) {
        Target target = resolve(source, name);
        if (target == null) {
            return 0;
        }

        BigDecimal amount = amount(source, rawAmount);
        if (amount == null) {
            return 0;
        }

        BalanceStore balances = balances();
        BigDecimal updated = switch (action) {
            case GIVE -> balances.get(target.uuid()).add(amount);
            case TAKE -> balances.get(target.uuid()).subtract(amount);
            case SET -> amount;
        };

        if (updated.signum() < 0) {
            // Clamped rather than refused: taking more than someone has is a
            // reasonable way to say "take it all".
            updated = BigDecimal.ZERO;
        }

        balances.set(target.uuid(), updated);

        BigDecimal shown = updated;
        source.sendSuccess(() -> Component.literal(
                        target.name() + "'s balance is now " + money(shown) + ".")
                .withStyle(ChatFormatting.GREEN), true);

        ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.uuid());
        if (online != null) {
            online.sendSystemMessage(Component.literal("Your balance is now " + money(shown) + ".")
                    .withStyle(ChatFormatting.GREEN));
        }
        return 1;
    }

    private static int reset(CommandSourceStack source, String name) {
        Target target = resolve(source, name);
        if (target == null) {
            return 0;
        }

        balances().reset(target.uuid());
        statistics().reset(target.uuid());

        source.sendSuccess(() -> Component.literal(
                        "Reset " + target.name() + "'s balance and statistics.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ------------------------------------------------------------ statistics

    private static int ownStats(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            source.sendFailure(Messages.get("only-player"));
            return 0;
        }
        return showStats(source, player.getUUID(), player.getGameProfile().getName());
    }

    private static int otherStats(CommandSourceStack source, String name) {
        Target target = resolve(source, name);
        return target == null ? 0 : showStats(source, target.uuid(), target.name());
    }

    private static int showStats(CommandSourceStack source, UUID uuid, String name) {
        StatisticsStore statistics = statistics();
        if (!statistics.isAvailable()) {
            source.sendFailure(Component.literal(
                            "Statistics are not being recorded: the database could not be opened.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        var stats = statistics.stats(uuid);
        source.sendSuccess(() -> Component.literal(name).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" has spent ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(money(stats.getTotalSpent())).withStyle(ChatFormatting.RED))
                .append(Component.literal(" and earned ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(money(stats.getTotalEarned())).withStyle(ChatFormatting.GREEN)),
                false);
        source.sendSuccess(() -> Component.literal("  " + stats.getItemsBought() + " bought, "
                + stats.getItemsSold() + " sold").withStyle(ChatFormatting.GRAY), false);

        sendTop(source, "  Most bought: ", statistics.topItems(uuid, true, 3));
        sendTop(source, "  Most sold:   ", statistics.topItems(uuid, false, 3));
        return 1;
    }

    private static int serverTop(CommandSourceStack source) {
        StatisticsStore statistics = statistics();
        if (!statistics.isAvailable()) {
            source.sendFailure(Component.literal(
                            "Statistics are not being recorded: the database could not be opened.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Across the server")
                .withStyle(ChatFormatting.WHITE), false);
        sendTop(source, "  Most bought: ", statistics.serverTopItems(true, 5));
        sendTop(source, "  Most sold:   ", statistics.serverTopItems(false, 5));
        return 1;
    }

    private static void sendTop(CommandSourceStack source, String label, Map<String, Integer> items) {
        String summary = items.isEmpty()
                ? "nothing yet"
                : String.join(", ", items.entrySet().stream()
                        .map(entry -> NameUtil.formatMaterialName(entry.getKey())
                                + " x" + entry.getValue())
                        .toList());
        source.sendSuccess(() -> Component.literal(label + summary)
                .withStyle(ChatFormatting.DARK_GRAY), false);
    }

    // ----------------------------------------------------------------- lookup

    /**
     * A player named on the command line, online or not.
     *
     * @return the resolved player, or null having already said why not
     */
    private static Target resolve(CommandSourceStack source, String name) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            return new Target(online.getUUID(), online.getGameProfile().getName());
        }

        // The profile cache is what the server itself uses for whitelist and
        // ban entries, so anyone who has ever joined is still findable.
        Optional<GameProfile> profile = source.getServer().getProfileCache() == null
                ? Optional.empty()
                : source.getServer().getProfileCache().get(name);

        if (profile.isEmpty()) {
            source.sendFailure(Component.literal("No player called '" + name + "' has been seen here.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        return new Target(profile.get().getId(), profile.get().getName());
    }

    /**
     * Names worth suggesting: everyone online, plus everyone with an account.
     * <p>
     * Offline names come from the balance store rather than the profile cache,
     * which holds every name the server has ever looked up including ones that
     * never joined.
     */
    private static SuggestionProvider<CommandSourceStack> knownPlayers() {
        return (context, builder) -> {
            List<String> names = new ArrayList<>(
                    List.of(context.getSource().getServer().getPlayerNames()));
            BalanceStore balances = GuiShopServer.balances();
            if (balances != null) {
                balances.knownNames().forEach(known -> {
                    if (!known.isBlank() && !names.contains(known)) {
                        names.add(known);
                    }
                });
            }
            return SharedSuggestionProvider.suggest(names, builder);
        };
    }

    /**
     * @return the parsed amount, or null having already said why not
     */
    private static BigDecimal amount(CommandSourceStack source, String raw) {
        BigDecimal parsed = MathUtil.tryParseAbbreviated(raw);
        if (parsed == null) {
            source.sendFailure(Component.literal(
                            "'" + raw + "' is not an amount. Try 100, 1.5k or 2M.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        if (parsed.abs().compareTo(MAX_AMOUNT) > 0) {
            source.sendFailure(Component.literal(
                            "That is more money than this server deals in.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        return parsed;
    }

    /**
     * Whether this source may look at someone else's money or statistics.
     * Operators and anyone with the edit permission, matching who may already
     * see every price on the server.
     */
    private static boolean maySeeOthers(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        ServerPlayer player = playerOrNull(source);
        return player != null && GuiShopPermissions.canEdit(player);
    }

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            return null;
        }
    }

    private static BalanceStore balances() {
        return GuiShopServer.balances();
    }

    private static StatisticsStore statistics() {
        return GuiShopServer.statistics();
    }

    private static String money(BigDecimal amount) {
        return GuiShopPlatform.get().formatCurrency(amount);
    }

    /**
     * A player a command is about.
     */
    private record Target(UUID uuid, String name) {
    }
}
