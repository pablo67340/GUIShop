package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.economy.BalanceStore;
import com.pablo67340.guishop.neoforge.network.Catalogue;
import com.pablo67340.guishop.neoforge.network.TradeRequestPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carries out a buy or a sell.
 * <p>
 * Everything is decided here from the server's own copy of the config: the
 * request names an entry and a quantity and nothing else, and the price, the
 * stock and the balance are all looked up rather than taken on trust. A client
 * that sends an impossible request gets a refusal, not a bargain.
 */
public final class TradeService {

    /**
     * A full inventory's worth, which is the most a single trade could ever
     * usefully move, and a bound on what a request can ask for.
     */
    private static final int MAX_QUANTITY = 36 * 64;

    private TradeService() {
    }

    public static void handle(ServerPlayer player, TradeRequestPayload request) {
        ShopRegistry registry = GuiShopServer.registry();
        BalanceStore balances = GuiShopServer.balances();
        if (registry == null || balances == null) {
            return;
        }

        int quantity = Math.max(1, Math.min(MAX_QUANTITY, request.quantity()));

        Catalogue catalogue = registry.catalogue(player.server.registryAccess());
        NeoItem item = catalogue.entryAt(request.shop(), request.index());
        if (item == null) {
            // Stale screen after a reload, or a forged index.
            refuse(player, balances, "That item is no longer available. Reopen the shop.");
            return;
        }

        if (request.selling()) {
            sell(player, balances, item, quantity);
        } else {
            buy(player, balances, item, quantity);
        }
    }

    // -------------------------------------------------------------------- buy

    private static void buy(ServerPlayer player, BalanceStore balances, NeoItem item, int quantity) {
        if (!item.hasBuyPrice()) {
            refuse(player, balances, "That item cannot be bought.");
            return;
        }

        BigDecimal total = item.calculateBuyPrice(quantity);

        // Checked before anything is given, so a failure cannot leave the
        // player holding goods they did not pay for.
        if (!balances.withdraw(player.getUUID(), total)) {
            refuse(player, balances, "You cannot afford " + money(total) + ".");
            return;
        }

        if (item.getItemType() == ItemType.COMMAND) {
            runCommands(player, item, quantity);
            succeed(player, balances, "Bought " + quantity + "x "
                    + name(item, player) + " for " + money(total) + ".");
            return;
        }

        ItemStack template = item.toItemStack(player.server.registryAccess());
        if (template.isEmpty()) {
            // Refunded rather than silently charged for nothing.
            balances.deposit(player.getUUID(), total);
            refuse(player, balances, "That item is misconfigured and cannot be given.");
            return;
        }

        give(player, template, quantity);
        succeed(player, balances, "Bought " + quantity + "x "
                + name(item, player) + " for " + money(total) + ".");
    }

    /**
     * Hands over the goods, in stack-sized pieces, dropping anything that will
     * not fit rather than destroying it.
     */
    private static void give(ServerPlayer player, ItemStack template, int quantity) {
        int remaining = quantity;
        while (remaining > 0) {
            int size = Math.min(remaining, template.getMaxStackSize());
            ItemStack stack = template.copyWithCount(size);

            if (!player.getInventory().add(stack)) {
                // Keeps the item in the world where the player can pick it up.
                player.drop(stack, false);
            }
            remaining -= size;
        }
    }

    // ------------------------------------------------------------------- sell

    private static void sell(ServerPlayer player, BalanceStore balances, NeoItem item, int quantity) {
        if (!item.hasSellPrice()) {
            refuse(player, balances, "That item cannot be sold.");
            return;
        }

        ItemStack template = item.toItemStack(player.server.registryAccess());
        if (template.isEmpty()) {
            refuse(player, balances, "That item is misconfigured and cannot be sold.");
            return;
        }

        int held = count(player, template);
        if (held < quantity) {
            refuse(player, balances, "You only have " + held + " of those.");
            return;
        }

        remove(player, template, quantity);

        BigDecimal total = item.calculateSellPrice(quantity);
        balances.deposit(player.getUUID(), total);
        succeed(player, balances, "Sold " + quantity + "x "
                + name(item, player) + " for " + money(total) + ".");
    }

    /**
     * Whether a held stack counts as the shop's item.
     * <p>
     * Matched on the item alone, not its components, which is what the Bukkit
     * plugin does: its sell table is keyed by material. So a named or enchanted
     * sword sells as a plain one. Matching components instead would be stricter
     * but would make most items unsellable, since a shop entry usually carries
     * a custom name that a looted item does not.
     */
    private static boolean matches(ItemStack held, ItemStack template) {
        return !held.isEmpty() && held.getItem() == template.getItem();
    }

    private static int count(ServerPlayer player, ItemStack template) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, template)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void remove(ServerPlayer player, ItemStack template, int quantity) {
        int remaining = quantity;
        List<ItemStack> contents = player.getInventory().items;

        for (int slot = 0; slot < contents.size() && remaining > 0; slot++) {
            ItemStack stack = contents.get(slot);
            if (!matches(stack, template)) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;

            if (stack.isEmpty()) {
                contents.set(slot, ItemStack.EMPTY);
            }
        }
    }

    // --------------------------------------------------------------- commands

    /**
     * Runs a command item's commands, once per unit bought.
     * <p>
     * As console by default, or as the player when the item sets {@code sudo},
     * matching the plugin. Running as the player keeps permission-based
     * commands honest, so it is not the default.
     */
    private static void runCommands(ServerPlayer player, NeoItem item, int quantity) {
        if (item.getCommands() == null || item.getCommands().isEmpty()) {
            return;
        }

        MinecraftServer server = player.server;
        CommandSourceStack source = item.isSudo()
                ? player.createCommandSourceStack()
                : server.createCommandSourceStack();

        for (int run = 0; run < quantity; run++) {
            for (String command : item.getCommands()) {
                server.getCommands().performPrefixedCommand(source, substitute(command, player));
            }
        }
    }

    private static String substitute(String command, ServerPlayer player) {
        return command
                .replace("{PLAYER_NAME}", player.getGameProfile().getName())
                .replace("{PLAYER_UUID}", player.getUUID().toString())
                .replace("{PLAYER_WORLD}", player.level().dimension().location().toString())
                .replace("{PLAYER_BALANCE}",
                        GuiShopServer.balances().get(player.getUUID()).toPlainString());
    }

    // --------------------------------------------------------------- feedback

    private static void succeed(ServerPlayer player, BalanceStore balances, String message) {
        send(player, balances, Component.literal(message).withStyle(ChatFormatting.GREEN), true);
    }

    private static void refuse(ServerPlayer player, BalanceStore balances, String message) {
        send(player, balances, Component.literal(message).withStyle(ChatFormatting.RED), false);
    }

    private static void send(ServerPlayer player, BalanceStore balances, Component message, boolean success) {
        GuiShopNetworkSender.send(player, new TradeResultPayload(
                message, success, balances.get(player.getUUID()).doubleValue()));
    }

    private static String money(BigDecimal amount) {
        return GuiShopPlatform.get().formatCurrency(amount);
    }

    /**
     * What to call the item in a message: its configured shop name if it has
     * one, so the wording matches what the player clicked.
     */
    private static String name(NeoItem item, ServerPlayer player) {
        if (item.getShopName() != null && !item.getShopName().isBlank()) {
            return com.pablo67340.guishop.neoforge.util.TextUtil.strip(item.getShopName());
        }
        return item.toItemStack(player.server.registryAccess()).getHoverName().getString();
    }
}
