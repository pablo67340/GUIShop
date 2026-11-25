package com.pablo67340.guishop.neoforge.server;

import com.pablo67340.guishop.neoforge.config.JsonFiles;
import com.pablo67340.guishop.neoforge.config.Messages;
import com.pablo67340.guishop.neoforge.config.ShopRegistry;
import com.pablo67340.guishop.neoforge.network.ShopAdminPayload;
import com.pablo67340.guishop.neoforge.network.TradeResultPayload;
import com.pablo67340.guishop.neoforge.permission.GuiShopPermissions;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Creates and removes shops and the items in them.
 * <p>
 * Every action re-checks the permission rather than trusting that the screen
 * only offered it, and the file name is derived here from a validated name
 * rather than taken from the request, so a crafted packet cannot reach outside
 * the shops directory.
 */
public final class ShopAdminService {

    /**
     * What a shop may be called. Deliberately strict: the name becomes a file
     * name, so anything with a separator, a dot or a colon in it is refused
     * rather than sanitised into something the author did not ask for.
     */
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private static final String JSON_SUFFIX = ".json";

    private ShopAdminService() {
    }

    public static void handle(ServerPlayer player, ShopAdminPayload request) {
        if (!GuiShopPermissions.canEdit(player)) {
            GuiShopPlatform.get().logger().warning(player.getGameProfile().getName()
                    + " tried to change shops without permission.");
            refuse(player, Messages.get("no-edit-permission"));
            return;
        }

        ShopRegistry registry = GuiShopServer.registry();
        if (registry == null) {
            return;
        }

        switch (request.action()) {
            case CREATE_SHOP -> createShop(player, registry, request.shop());
            case DELETE_SHOP -> deleteShop(player, registry, request.shop());
            case ADD_ITEM -> addItem(player, registry, request.shop());
            case DELETE_ITEM -> deleteItem(player, registry, request.shop(), request.entryId());
        }
    }

    // ------------------------------------------------------------------ shops

    private static void createShop(ServerPlayer player, ShopRegistry registry, String name) {
        if (!VALID_NAME.matcher(name).matches()) {
            refuse(player, Component.literal(
                    "A shop name may only use letters, digits, dashes and underscores."));
            return;
        }

        Path file = shopsFolder().resolve(name + JSON_SUFFIX);
        if (Files.exists(file)) {
            refuse(player, Component.literal("A shop called '" + name + "' already exists."));
            return;
        }

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", "&8&l" + name);
        document.put("items", new ArrayList<>());

        if (!JsonFiles.write(file, document)) {
            refuse(player, Messages.get("error-saving", "error", file.getFileName()));
            return;
        }

        reloadAndTell(player, registry, "Created shop '" + name + "'. Add items with Edit mode.");
    }

    /**
     * Renames the file aside rather than deleting it.
     * <p>
     * A shop is someone's work, and a mis-click should not be the end of it.
     * The suffix keeps it out of the loader, which only reads {@code .json}.
     */
    private static void deleteShop(ServerPlayer player, ShopRegistry registry, String name) {
        Path file = registry.file(name);
        if (file == null || !Files.isRegularFile(file)) {
            refuse(player, Messages.get("shop-not-found", "shop", name));
            return;
        }

        Path backup = file.resolveSibling(file.getFileName() + ".deleted");
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            refuse(player, Messages.get("error-saving", "error", exception.getMessage()));
            return;
        }

        reloadAndTell(player, registry,
                "Deleted shop '" + name + "'. Kept " + backup.getFileName() + ".");
    }

    private static Path shopsFolder() {
        return GuiShopPlatform.get().dataFolder().resolve("shops");
    }

    // ------------------------------------------------------------------ items

    /**
     * Appends a starter item, which the author then edits.
     * <p>
     * Given a price so that it shows up at all: an entry with neither price is
     * filtered out of the shop as decoration, which would look like the button
     * had done nothing.
     */
    private static void addItem(ServerPlayer player, ShopRegistry registry, String shop) {
        Map<String, Object> document = registry.document(shop);
        Path file = registry.file(shop);
        if (document == null || file == null) {
            refuse(player, Messages.get("shop-not-found", "shop", shop));
            return;
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", "STONE");
        item.put("type", "SHOP");
        item.put("buy-price", 1);
        item.put("sell-price", 1);

        List<Object> items = itemsOf(document);
        items.add(item);
        document.put("items", items);

        if (!JsonFiles.write(file, document)) {
            refuse(player, Messages.get("error-saving", "error", file.getFileName()));
            return;
        }

        registry.refresh(shop);
        GuiShopNetworkSender.broadcastCatalogue(player);
        succeed(player, Component.literal("Added an item to '" + shop
                + "'. It is the last one; click it to edit."));
    }

    private static void deleteItem(ServerPlayer player, ShopRegistry registry, String shop, int entryId) {
        Map<String, Object> document = registry.document(shop);
        Path file = registry.file(shop);
        if (document == null || file == null) {
            refuse(player, Messages.get("shop-not-found", "shop", shop));
            return;
        }

        // The id is the entry's position in the file, which is what the loader
        // records as its slot and what the catalogue sends out.
        List<Object> items = itemsOf(document);
        if (entryId < 0 || entryId >= items.size()) {
            refuse(player, Messages.get("item-gone"));
            return;
        }

        items.remove(entryId);
        document.put("items", items);

        if (!JsonFiles.write(file, document)) {
            refuse(player, Messages.get("error-saving", "error", file.getFileName()));
            return;
        }

        // Removing an entry shifts everything after it, so every client needs
        // the new numbering before it can click anything else.
        registry.refresh(shop);
        GuiShopNetworkSender.broadcastCatalogue(player);
        succeed(player, Component.literal("Deleted the item from '" + shop + "'."));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> itemsOf(Map<String, Object> document) {
        Object raw = document.get("items");
        return raw instanceof List<?> list ? new ArrayList<>((List<Object>) list) : new ArrayList<>();
    }

    // --------------------------------------------------------------- feedback

    /**
     * A created or deleted shop changes which files exist, so the whole
     * registry is reloaded rather than one shop refreshed.
     */
    private static void reloadAndTell(ServerPlayer player, ShopRegistry registry, String message) {
        int loaded = registry.loadAll();
        GuiShopNetworkSender.broadcastCatalogue(player);

        GuiShopPlatform.get().logger().info(player.getGameProfile().getName()
                + ": " + message.toLowerCase(Locale.ROOT) + " " + loaded + " shop(s) now loaded.");
        succeed(player, Component.literal(message));
    }

    private static void succeed(ServerPlayer player, Component message) {
        send(player, message, true);
    }

    private static void refuse(ServerPlayer player, Component message) {
        send(player, message, false);
    }

    private static void send(ServerPlayer player, Component message, boolean success) {
        GuiShopNetworkSender.send(player, new TradeResultPayload(message, success,
                GuiShopServer.balances().get(player.getUUID()).doubleValue()));
    }
}
