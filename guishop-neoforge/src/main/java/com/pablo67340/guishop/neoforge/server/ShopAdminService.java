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

    /**
     * Whether a name could be a shop, without saying whether one exists.
     */
    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name.trim()).matches();
    }

    /**
     * Writes an empty shop file.
     * <p>
     * Shared with {@link EditService}, which creates a shop when a menu
     * shortcut is saved pointing at one that is not there yet, so that the
     * name rule and the file's shape are defined once.
     *
     * @return null on success, or why it could not be done
     */
    static String createShopFile(String name) {
        if (!isValidName(name)) {
            return "A shop name may only use letters, digits, dashes and underscores.";
        }

        Path file = shopsFolder().resolve(name.trim() + JSON_SUFFIX);
        if (Files.exists(file)) {
            return "A shop called '" + name.trim() + "' already exists.";
        }

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", "&8&l" + name.trim());
        document.put("items", new ArrayList<>());

        return JsonFiles.write(file, document)
                ? null
                : "Could not write " + file.getFileName() + ".";
    }

    private static void createShop(ServerPlayer player, ShopRegistry registry, String name) {
        String problem = createShopFile(name);
        if (problem != null) {
            refuse(player, Component.literal(problem));
            return;
        }

        if (!addMenuIcon(registry, name)) {
            // The shop itself is fine, so this is a warning rather than a
            // failure; /shop <name> still opens it.
            reloadAndTell(player, registry, "Created shop '" + name
                    + "', but could not add it to the menu. Open it with /shop " + name + ".");
            return;
        }

        reloadAndTell(player, registry, "Created shop '" + name
                + "'. It is in the menu; open it and use Edit to add items.");
    }

    /**
     * Puts a shortcut to the new shop in the menu.
     * <p>
     * Without this the shop would exist and load but nothing would link to it,
     * so the only way in would be to know its name and type it. A chest with
     * the shop's name is a plain default the author can then edit like any
     * other icon.
     *
     * @return whether the menu was updated
     */
    private static boolean addMenuIcon(ShopRegistry registry, String shop) {
        Map<String, Object> menu = registry.document(ShopRegistry.MENU_NAME);
        Path menuFile = registry.file(ShopRegistry.MENU_NAME);
        if (menu == null || menuFile == null) {
            return false;
        }

        Map<String, Object> icon = new LinkedHashMap<>();
        icon.put("id", "CHEST");
        // Spelled out rather than left to be inferred from target-shop, so the
        // editor shows what it is and the author can see what to change if
        // they would rather sell something here than open a shop.
        icon.put("type", "SHOP_SHORTCUT");
        // The key the editor's one name field writes, so a generated entry and
        // an edited one are spelled the same way.
        icon.put("shop-name", "&f&l" + shop);
        icon.put("lore", List.of("&7Click to browse"));
        icon.put("target-shop", shop);

        List<Object> icons = itemsOf(menu);
        icons.add(icon);
        menu.put("items", icons);

        return JsonFiles.write(menuFile, menu);
    }

    /**
     * Drops any menu icon pointing at a shop that is going away, so the menu
     * does not keep a button that opens nothing.
     */
    private static void removeMenuIcons(ShopRegistry registry, String shop) {
        Map<String, Object> menu = registry.document(ShopRegistry.MENU_NAME);
        Path menuFile = registry.file(ShopRegistry.MENU_NAME);
        if (menu == null || menuFile == null) {
            return;
        }

        List<Object> icons = itemsOf(menu);
        boolean removed = icons.removeIf(entry -> entry instanceof Map<?, ?> map
                && map.get("target-shop") instanceof String target
                && target.equalsIgnoreCase(shop));

        if (removed) {
            menu.put("items", icons);
            JsonFiles.write(menuFile, menu);
        }
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

        removeMenuIcons(registry, name);

        reloadAndTell(player, registry,
                "Deleted shop '" + name + "'. Kept " + backup.getFileName() + ".");
    }

    private static Path shopsFolder() {
        return GuiShopPlatform.get().dataFolder().resolve("shops");
    }

    // ------------------------------------------------------------------ items

    /**
     * Appends a starter entry, which the author then edits.
     * <p>
     * In a shop that is a priced item, since an entry with neither price is
     * filtered out as decoration and the button would look like it had done
     * nothing. On the menu it is an empty shortcut instead: the author picks
     * which shop it opens, and only then, on save, is that shop's file
     * created. Nothing is written for a shop that was never settled on.
     */
    private static void addItem(ServerPlayer player, ShopRegistry registry, String shop) {
        Map<String, Object> document = registry.document(shop);
        Path file = registry.file(shop);
        if (document == null || file == null) {
            refuse(player, Messages.get("shop-not-found", "shop", shop));
            return;
        }

        boolean isMenu = ShopRegistry.MENU_NAME.equalsIgnoreCase(shop);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", isMenu ? "CHEST" : "STONE");
        item.put("type", isMenu ? "SHOP_SHORTCUT" : "ITEM");
        if (isMenu) {
            item.put("shop-name", "&f&lNew shop");
        } else {
            item.put("buy-price", 1);
            item.put("sell-price", 1);
        }

        List<Object> items = itemsOf(document);
        items.add(item);
        document.put("items", items);

        if (!JsonFiles.write(file, document)) {
            refuse(player, Messages.get("error-saving", "error", file.getFileName()));
            return;
        }

        registry.refresh(shop);
        GuiShopNetworkSender.broadcastCatalogue(player);
        succeed(player, Component.literal(isMenu
                ? "Added a shortcut. Click it and choose which shop it opens."
                : "Added an item to '" + shop + "'. It is the last one; click it to edit."));
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
