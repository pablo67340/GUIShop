package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.JsonFiles;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.network.ItemFieldsPayload;
import com.pablo67340.guishop.neoforge.network.RequestItemPayload;
import com.pablo67340.guishop.neoforge.network.SaveItemPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.neoforge.permission.GuiShopPermissions;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Applies edits made in the shop screen to the shop files.
 * <p>
 * The permission is re-checked here, not taken from the request. The screen
 * only shows editing controls to someone who may edit, but a modified client
 * can send the packet regardless, so the decision has to be made again on the
 * server.
 */
public final class EditService {

    /**
     * Properties that hold a list in the config. The editor presents them as
     * one line per entry, so they are split again on the way in.
     */
    private static final List<String> LIST_FIELDS = List.of("lore", "shop-lore", "buy-lore", "commands");

    /** Properties whose value is a number rather than text. */
    private static final List<String> NUMBER_FIELDS = List.of("buy-price", "sell-price", "custom-model-data");

    /** Properties whose value is a flag. */
    private static final List<String> FLAG_FIELDS = List.of("sudo", "use-dynamic-pricing");

    private EditService() {
    }

    /**
     * Answers an editor's request for an item's current properties.
     * <p>
     * Permission is checked here too: the raw config is not something an
     * ordinary client should be able to ask for.
     */
    public static void handle(ServerPlayer player, RequestItemPayload request) {
        if (!GuiShopPermissions.canEdit(player)) {
            refuse(player, "You do not have permission to edit shops.");
            return;
        }

        ShopRegistry registry = GuiShopServer.registry();
        if (registry == null) {
            return;
        }

        Map<String, Object> document = registry.document(request.shop());
        if (document == null) {
            refuse(player, "No shop named '" + request.shop() + "'.");
            return;
        }

        List<Object> items = itemsOf(document);
        if (request.index() < 0 || request.index() >= items.size()) {
            refuse(player, "That item is no longer there. Reopen the shop.");
            return;
        }

        GuiShopNetworkSender.send(player, new ItemFieldsPayload(
                request.shop(), request.index(), asText(items.get(request.index()))));
    }

    /**
     * Flattens an item's config entry into the text the editor edits.
     * <p>
     * Lists become one entry per line, and numbers lose a trailing {@code .0}
     * so a price reads as the editor would write it rather than as Gson parsed
     * it.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> asText(Object raw) {
        Map<String, String> text = new LinkedHashMap<>();
        if (!(raw instanceof Map)) {
            return text;
        }

        ((Map<String, Object>) raw).forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if (value instanceof List<?> list) {
                text.put(key, list.stream().map(String::valueOf).collect(Collectors.joining("\n")));
            } else if (value instanceof Double number && number == Math.floor(number)
                    && !number.isInfinite()) {
                text.put(key, String.valueOf(number.longValue()));
            } else if (value instanceof Map) {
                // Structured values such as potion-info are not editable here,
                // so they are left out rather than shown as something the
                // editor would mangle on save.
                return;
            } else {
                text.put(key, String.valueOf(value));
            }
        });
        return text;
    }

    public static void handle(ServerPlayer player, SaveItemPayload request) {
        if (!GuiShopPermissions.canEdit(player)) {
            // Logged, because an unprivileged client sending this is either a
            // bug or someone probing.
            GuiShopPlatform.get().logger().warning(player.getGameProfile().getName()
                    + " tried to edit a shop item without permission.");
            refuse(player, "You do not have permission to edit shops.");
            return;
        }

        ShopRegistry registry = GuiShopServer.registry();
        if (registry == null) {
            return;
        }

        Map<String, Object> document = registry.document(request.shop());
        Path file = registry.file(request.shop());
        if (document == null || file == null) {
            refuse(player, "No shop named '" + request.shop() + "'.");
            return;
        }

        List<Object> items = itemsOf(document);
        if (request.index() < 0 || request.index() >= items.size()) {
            refuse(player, "That item is no longer there. Reopen the shop.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> item = items.get(request.index()) instanceof Map
                ? (Map<String, Object>) items.get(request.index())
                : new LinkedHashMap<>();

        apply(item, request.fields());
        items.set(request.index(), item);

        if (!JsonFiles.write(file, document)) {
            refuse(player, "Could not write " + file.getFileName() + ". See the server log.");
            return;
        }

        registry.refresh(request.shop());

        // Everyone looking at the shop, not just the editor: a price change is
        // not something other players should keep seeing the old value of.
        GuiShopNetworkSender.broadcastCatalogue(player);

        succeed(player, "Saved to " + file.getFileName() + ".");
    }

    /**
     * Writes the submitted values onto the item, converting each to the type
     * its config key expects.
     * <p>
     * A blank value removes the property, which is how a field is cleared.
     * Leaving it as an empty string would mean an item with a name of nothing
     * rather than no name at all.
     */
    private static void apply(Map<String, Object> item, Map<String, String> fields) {
        fields.forEach((key, value) -> {
            String trimmed = value == null ? "" : value.trim();

            if (trimmed.isEmpty()) {
                item.remove(key);
                return;
            }

            if (LIST_FIELDS.contains(key)) {
                item.put(key, lines(value));
            } else if (NUMBER_FIELDS.contains(key)) {
                Double number = number(trimmed);
                if (number == null) {
                    item.remove(key);
                } else {
                    item.put(key, number);
                }
            } else if (FLAG_FIELDS.contains(key)) {
                item.put(key, Boolean.parseBoolean(trimmed));
            } else if ("type".equals(key)) {
                item.put(key, trimmed.toUpperCase(Locale.ROOT));
            } else {
                item.put(key, trimmed);
            }
        });
    }

    /** Splits a multi-line field, dropping blank lines at either end. */
    private static List<String> lines(String value) {
        List<String> lines = new ArrayList<>();
        for (String line : value.split("\n", -1)) {
            lines.add(line);
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        while (!lines.isEmpty() && lines.get(0).isBlank()) {
            lines.remove(0);
        }
        return lines;
    }

    private static Double number(String value) {
        try {
            return Double.valueOf(value.replace(",", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * The item list, created if the document somehow has none, so an edit to an
     * otherwise valid file is not refused.
     */
    @SuppressWarnings("unchecked")
    private static List<Object> itemsOf(Map<String, Object> document) {
        Object existing = document.get("items");
        if (existing instanceof List) {
            return (List<Object>) existing;
        }
        List<Object> items = new ArrayList<>();
        document.put("items", items);
        return items;
    }

    private static void succeed(ServerPlayer player, String message) {
        GuiShopNetworkSender.send(player, new TradeResultPayload(
                Component.literal(message).withStyle(ChatFormatting.GREEN), true,
                GuiShopServer.balances().get(player.getUUID()).doubleValue()));
    }

    private static void refuse(ServerPlayer player, String message) {
        GuiShopNetworkSender.send(player, new TradeResultPayload(
                Component.literal(message).withStyle(ChatFormatting.RED), false,
                GuiShopServer.balances().get(player.getUUID()).doubleValue()));
    }
}
