package com.pablo67340.guishop.neoforge.config;

import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.platform.GuiShopPlatform;

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

    /**
     * Reads every {@code .yml} in the shops directory. Shop names come from the
     * file names, as in the Bukkit plugin's folder-based layout.
     *
     * @return the number of shops loaded
     */
    public int loadAll() {
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
}
