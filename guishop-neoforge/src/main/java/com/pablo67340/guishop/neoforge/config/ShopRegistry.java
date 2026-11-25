package com.pablo67340.guishop.neoforge.config;

import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.neoforge.network.OpenShopPayload;
import com.pablo67340.guishop.neoforge.network.ShopViewBuilder;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.core.HolderLookup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Discovers and holds the shops defined under {@code <datafolder>/shops}.
 * <p>
 * Shops are JSON. A legacy {@code .yml} from the Bukkit plugin is converted on
 * first sight, so an existing server's configs carry over without being
 * rewritten by hand.
 */
public final class ShopRegistry {

    private static final String JSON_SUFFIX = ".json";
    private static final String MENU_NAME = "menu";

    /**
     * Shown when menu.json declares no title. The chest GUI took the menu's
     * title from config.yml, so a migrated menu usually has none of its own.
     */
    private static final String MENU_TITLE = "Shop Menu";

    private final Map<String, NeoShop> shops = new LinkedHashMap<>();
    private NeoShop menu;

    /**
     * The catalogue sent to clients, built on first use and discarded on reload.
     * <p>
     * Building it resolves every item and assembles its components, so doing
     * that once per reload rather than once per /shop keeps the command cheap
     * no matter how large the shops get.
     */
    private OpenShopPayload catalogue;

    /**
     * Reads the menu and every shop, converting any legacy YAML first.
     *
     * @return the number of shops loaded
     */
    public int loadAll() {
        catalogue = null;
        loadMenu();
        return loadShops();
    }

    /**
     * @param registries the server's registry access, needed to resolve
     *                   enchantments when the items are assembled
     */
    public OpenShopPayload catalogue(HolderLookup.Provider registries) {
        if (catalogue == null) {
            catalogue = ShopViewBuilder.build(this, registries);
        }
        return catalogue;
    }

    // ----------------------------------------------------------------- loading

    private void loadMenu() {
        menu = null;

        Path dataFolder = GuiShopPlatform.get().dataFolder();
        Path json = dataFolder.resolve(MENU_NAME + JSON_SUFFIX);

        Map<String, Object> converted = convertIfLegacy(
                dataFolder.resolve("menu.yml"), json, MENU_NAME, true);

        if (converted != null) {
            menu = ShopLoader.fromDocument(converted, MENU_NAME, MENU_TITLE);
        } else if (Files.isRegularFile(json)) {
            menu = ShopLoader.load(json, MENU_NAME, MENU_TITLE);
        } else {
            GuiShopPlatform.get().logger().warning(
                    "No " + json.getFileName() + " in " + dataFolder + ". The shop menu will be unavailable.");
            return;
        }

        if (menu != null) {
            GuiShopPlatform.get().logUtil().debugLog("Loaded menu with " + countItems(menu) + " item(s).");
        }
    }

    private int loadShops() {
        shops.clear();

        Path shopsDir = GuiShopPlatform.get().dataFolder().resolve("shops");
        if (!Files.isDirectory(shopsDir)) {
            GuiShopPlatform.get().logger().warning(
                    "No shops directory at " + shopsDir + ". Copy an existing GUIShop shops folder there to load shops.");
            return 0;
        }

        convertLegacyShops(shopsDir);

        for (Path file : filesWithSuffix(shopsDir, JSON_SUFFIX)) {
            String name = withoutSuffix(file, JSON_SUFFIX);

            // A shop's file name doubles as a readable title, unlike the menu's.
            NeoShop shop = ShopLoader.load(file, name, name);
            if (shop == null) {
                continue;
            }

            shops.put(name, shop);
            GuiShopPlatform.get().logUtil().debugLog(
                    "Loaded shop '" + name + "' with " + countItems(shop) + " item(s).");
        }

        return shops.size();
    }

    /**
     * Converts every legacy shop file before loading, so a mixed directory ends
     * up consistently on JSON.
     */
    private void convertLegacyShops(Path shopsDir) {
        for (Path file : filesWithSuffix(shopsDir, ".yml")) {
            String name = withoutSuffix(file, ".yml");
            convertIfLegacy(file, shopsDir.resolve(name + JSON_SUFFIX), name, false);
        }
    }

    private Map<String, Object> convertIfLegacy(Path yaml, Path json, String name, boolean isMenu) {
        if (!Files.isRegularFile(yaml)) {
            return null;
        }
        return ShopMigrator.migrate(yaml, json, name, isMenu);
    }

    private static List<Path> filesWithSuffix(Path directory, String suffix) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(suffix))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not list " + directory + ": " + exception.getMessage());
            return List.of();
        }
    }

    private static String withoutSuffix(Path file, String suffix) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - suffix.length());
    }

    // ------------------------------------------------------------------ access

    /**
     * The main menu, or null if it is missing or unreadable.
     */
    public NeoShop getMenu() {
        return menu;
    }

    /**
     * Reports menu icons whose target-shop names a shop that does not exist,
     * which would otherwise only show up as a dead click.
     */
    public List<String> brokenMenuTargets() {
        if (menu == null) {
            return List.of();
        }
        return menu.getPages().values().stream()
                .flatMap(page -> page.getItems().values().stream())
                .map(NeoItem::getTargetShop)
                .filter(target -> target != null && !target.isBlank())
                .filter(target -> get(target) == null)
                .distinct()
                .toList();
    }

    public NeoShop get(String name) {
        NeoShop exact = shops.get(name);
        if (exact != null) {
            return exact;
        }
        // Shop names come from file names, so be forgiving about case.
        return shops.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public Collection<String> names() {
        return shops.keySet();
    }

    public Collection<NeoShop> all() {
        return shops.values();
    }

    public static int countItems(NeoShop shop) {
        return shop.getPages().values().stream()
                .mapToInt(page -> page.getItems().size())
                .sum();
    }

    public int totalItems() {
        return shops.values().stream().mapToInt(ShopRegistry::countItems).sum();
    }

    /**
     * How many loaded items actually have a price, which is the quickest way to
     * spot a shop whose prices are mis-typed in the config.
     */
    public int tradeableItems() {
        return (int) shops.values().stream()
                .flatMap(shop -> shop.getPages().values().stream())
                .flatMap(page -> page.getItems().values().stream())
                .filter(item -> item.hasBuyPrice() || item.hasSellPrice())
                .count();
    }
}
