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
import java.util.Map;
import java.util.stream.Stream;

/**
 * Discovers and holds the shops defined under {@code <datafolder>/shops}.
 */
public final class ShopRegistry {

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
     * Reads menu.yml and every {@code .yml} in the shops directory.
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

    private void loadMenu() {
        menu = null;

        Path menuFile = GuiShopPlatform.get().dataFolder().resolve("menu.yml");
        if (!Files.isRegularFile(menuFile)) {
            GuiShopPlatform.get().logger().warning(
                    "No menu.yml at " + menuFile + ". The shop menu will be unavailable.");
            return;
        }

        menu = YamlShopLoader.loadMenu(menuFile);
        if (menu != null) {
            GuiShopPlatform.get().logUtil().debugLog("Loaded menu with "
                    + menu.getPages().size() + " page(s) and " + countItems(menu) + " item(s).");
        }
    }

    /**
     * Reads every {@code .yml} in the shops directory. Shop names come from the
     * file names, as in the Bukkit plugin's folder-based layout.
     */
    private int loadShops() {
        shops.clear();

        Path shopsDir = GuiShopPlatform.get().dataFolder().resolve("shops");
        if (!Files.isDirectory(shopsDir)) {
            GuiShopPlatform.get().logger().warning(
                    "No shops directory at " + shopsDir + ". Copy an existing GUIShop shops folder there to load shops.");
            return 0;
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(shopsDir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".yml") || name.endsWith(".yaml");
                    })
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not list " + shopsDir + ": " + exception.getMessage());
            return 0;
        }

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String name = fileName.substring(0, fileName.lastIndexOf('.'));

            NeoShop shop = YamlShopLoader.loadShop(file, name);
            if (shop == null) {
                continue;
            }

            shops.put(name, shop);
            GuiShopPlatform.get().logUtil().debugLog("Loaded shop '" + name + "' with "
                    + shop.getPages().size() + " page(s) and " + countItems(shop) + " item(s).");
        }

        return shops.size();
    }

    /**
     * The main menu, or null if menu.yml is missing or unreadable.
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
        // Shop names are configured by file name, so be forgiving about case.
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
