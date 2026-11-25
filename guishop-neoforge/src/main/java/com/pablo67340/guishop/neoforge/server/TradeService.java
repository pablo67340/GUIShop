package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.neoforge.config.Messages;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.economy.BalanceStore;
import com.pablo67340.guishop.neoforge.network.Catalogue;
import com.pablo67340.guishop.neoforge.network.TradeRequestPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

        if (request.quantity() > MAX_QUANTITY) {
            // Told rather than quietly clamped, so a player who typed a large
            // number knows why they got fewer.
            refuse(player, balances, Messages.get("too-high-quantity", "max", MAX_QUANTITY));
            return;
        }

        int quantity = Math.max(1, request.quantity());

        Catalogue catalogue = registry.catalogue(player.server.registryAccess());
        NeoItem item = catalogue.entryAt(request.shop(), request.index());
        if (item == null) {
            // Stale screen after a reload, or a forged index.
            refuse(player, balances, Messages.get("item-gone"));
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
            refuse(player, balances, Messages.get("cant-buy"));
            return;
        }

        BigDecimal total = item.calculateBuyPrice(quantity);

        // Checked before anything is given, so a failure cannot leave the
        // player holding goods they did not pay for.
        if (!balances.withdraw(player.getUUID(), total)) {
            refuse(player, balances, Messages.get("not-enough-money", "needed", money(total)));
            return;
        }

        if (item.getItemType() == ItemType.COMMAND) {
            runCommands(player, item, quantity);
            succeed(player, balances, Messages.get("purchase", "taken", money(total)));
            return;
        }

        ItemStack template = item.toItemStack(player.server.registryAccess());
        if (template.isEmpty()) {
            // Refunded rather than silently charged for nothing.
            balances.deposit(player.getUUID(), total);
            refuse(player, balances, Messages.get("something-wrong"));
            return;
        }

        int dropped = give(player, template, quantity);
        succeed(player, balances, Messages.get("purchase", "taken", money(total)));

        if (dropped > 0) {
            // A second line rather than a replacement: the purchase did happen,
            // and the player still needs telling that some of it is on the
            // ground.
            player.sendSystemMessage(Messages.chat("partial-purchase",
                    "amount", quantity - dropped));
        }
    }

    /**
     * Hands over the goods, in stack-sized pieces, dropping anything that will
     * not fit rather than destroying it.
     *
     * @return how many items would not fit and were dropped
     */
    private static int give(ServerPlayer player, ItemStack template, int quantity) {
        int remaining = quantity;
        int dropped = 0;

        while (remaining > 0) {
            int size = Math.min(remaining, template.getMaxStackSize());
            ItemStack stack = template.copyWithCount(size);

            if (!player.getInventory().add(stack)) {
                // Keeps the item in the world where the player can pick it up.
                // getCount is read after the add, which may have placed part of
                // the stack before running out of room.
                dropped += stack.getCount();
                player.drop(stack, false);
            }
            remaining -= size;
        }
        return dropped;
    }

    // ------------------------------------------------------------------- sell

    private static void sell(ServerPlayer player, BalanceStore balances, NeoItem item, int quantity) {
        if (!item.hasSellPrice()) {
            refuse(player, balances, Messages.get("cant-sell"));
            return;
        }

        ItemStack template = item.toItemStack(player.server.registryAccess());
        if (template.isEmpty()) {
            refuse(player, balances, Messages.get("something-wrong"));
            return;
        }

        int held = count(player, template, item);
        if (held < quantity) {
            refuse(player, balances, Messages.get("not-enough-items", "amount", quantity));
            return;
        }

        remove(player, template, item, quantity);

        BigDecimal total = item.calculateSellPrice(quantity);
        balances.deposit(player.getUUID(), total);
        succeed(player, balances, Messages.get("sell", "money", money(total)));
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
    private static boolean matches(ItemStack held, ItemStack template, NeoItem item) {
        if (held.isEmpty() || held.getItem() != template.getItem()) {
            return false;
        }
        return headOwnerMatches(held, item);
    }

    /**
     * The one exception to matching on the item alone, and only when
     * {@code sell-skull-uuid} asks for it: a shop selling a specific player's
     * head would otherwise buy back any head at all.
     */
    private static boolean headOwnerMatches(ItemStack held, NeoItem item) {
        if (!BaseConfig.isSellSkullUUID() || item.getSkullUUID() == null
                || item.getSkullUUID().isBlank()) {
            return true;
        }

        ResolvableProfile profile = held.get(DataComponents.PROFILE);
        if (profile == null || profile.id().isEmpty()) {
            return false;
        }

        // Compared as a UUID rather than as text, so the two spellings of a
        // uuid - with hyphens and without - are treated as the same owner.
        UUID wanted = parseUuid(item.getSkullUUID());
        return wanted != null && wanted.equals(profile.id().get());
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static int count(ServerPlayer player, ItemStack template, NeoItem item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, template, item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void remove(ServerPlayer player, ItemStack template, NeoItem item, int quantity) {
        int remaining = quantity;
        List<ItemStack> contents = player.getInventory().items;

        for (int slot = 0; slot < contents.size() && remaining > 0; slot++) {
            ItemStack stack = contents.get(slot);
            if (!matches(stack, template, item)) {
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

    private static void succeed(ServerPlayer player, BalanceStore balances, Component message) {
        playTradeSound(player);
        send(player, balances, message, true);
    }

    private static void refuse(ServerPlayer player, BalanceStore balances, Component message) {
        send(player, balances, message, false);
    }

    private static void send(ServerPlayer player, BalanceStore balances, Component message, boolean success) {
        GuiShopNetworkSender.send(player, new TradeResultPayload(
                message, success, balances.get(player.getUUID()).doubleValue()));
    }

    /**
     * The configured confirmation sound, played to the one player.
     * <p>
     * Looked up by id each time rather than resolved once at load, so a sound
     * changed in the settings screen takes effect without a restart. An
     * unknown id is ignored: a mistyped sound should not stop a trade that has
     * already gone through.
     */
    private static void playTradeSound(ServerPlayer player) {
        if (!BaseConfig.isSoundEnabled()) {
            return;
        }

        String configured = BaseConfig.getSound();
        if (configured == null || configured.isBlank()) {
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(configured.trim().toLowerCase(Locale.ROOT));
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound == null) {
            return;
        }

        player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static String money(BigDecimal amount) {
        return GuiShopPlatform.get().formatCurrency(amount);
    }

}
