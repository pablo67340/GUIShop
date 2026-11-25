package com.pablo67340.guishop.neoforge.config;

import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the Bukkit plugin's YAML shop format, purely so it can be migrated.
 * <p>
 * Normal loading goes through {@link ShopLoader}; this exists to convert an
 * existing server's configs once. It flattens the page and slot scaffolding,
 * which only existed to place items in a fixed 54-slot chest grid and has no
 * meaning for a screen that reflows its contents.
 */
final class LegacyYamlReader {

    /** menu.yml nests everything under this key; shop files do not. */
    private static final String MENU_ROOT_KEY = "Menu";

    private LegacyYamlReader() {
    }

    /**
     * Converts a legacy shop file into the flat document the JSON format uses.
     *
     * @param isMenu whether to expect the menu's extra nesting
     * @return the flattened document, or null if the file could not be read
     */
    static Map<String, Object> read(Path file, String name, boolean isMenu) {
        Map<String, Object> root = parse(file);
        if (root == null) {
            return null;
        }

        if (isMenu) {
            root = asMap(root.get(MENU_ROOT_KEY));
            if (root == null) {
                warn("'" + file.getFileName() + "' has no " + MENU_ROOT_KEY + " section.");
                return null;
            }
        }

        Map<String, Object> document = new LinkedHashMap<>();
        // Only carried over when the source actually had one. The menu's title
        // lived in config.yml rather than menu.yml, so inventing one here would
        // bake a file name in as a display title.
        if (root.get("title") != null) {
            document.put("title", String.valueOf(root.get("title")));
        }
        document.put("items", flattenItems(root, file));
        return document;
    }

    /**
     * Walks the pages in order, and the items within each page in slot order,
     * dropping the entries that only padded the chest grid.
     */
    private static List<Map<String, Object>> flattenItems(Map<String, Object> root, Path file) {
        List<Map<String, Object>> flattened = new ArrayList<>();

        Map<String, Object> pages = asMap(root.get("pages"));
        if (pages == null) {
            warn("'" + file.getFileName() + "' has no pages section.");
            return flattened;
        }

        for (Map.Entry<String, Object> pageEntry : pages.entrySet()) {
            Map<String, Object> page = asMap(pageEntry.getValue());
            if (page == null) {
                continue;
            }

            Map<String, Object> items = asMap(page.get("items"));
            if (items == null) {
                continue;
            }

            items.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> slotOf(entry.getKey())))
                    .forEach(entry -> {
                        Map<String, Object> item = asMap(entry.getValue());
                        if (item != null && !isChestScaffolding(item)) {
                            flattened.add(stripSlotKeys(item));
                        }
                    });
        }

        return flattened;
    }

    /**
     * Pagination arrows, page counters, the balance icon and filler panes exist
     * only to lay out a chest inventory, so they are not carried over.
     */
    private static boolean isChestScaffolding(Map<String, Object> item) {
        Object declared = item.get("type");
        if (declared == null) {
            return false;
        }

        try {
            ItemType type = ItemType.valueOf(String.valueOf(declared).trim().toUpperCase(Locale.ROOT));
            return type.isNavigationType() || type == ItemType.BLANK || type == ItemType.DUMMY;
        } catch (IllegalArgumentException unknown) {
            return false;
        }
    }

    /**
     * Removes the keys that described where an item sat in the grid. Ordering is
     * carried by the list itself now.
     */
    private static Map<String, Object> stripSlotKeys(Map<String, Object> item) {
        Map<String, Object> copy = new LinkedHashMap<>(item);
        copy.remove("slot");
        copy.remove("rows");
        return copy;
    }

    private static int slotOf(String key) {
        try {
            return Integer.parseInt(key.trim());
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private static Map<String, Object> parse(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Object> root = asMap(new Yaml().load(reader));
            if (root == null) {
                warn("Could not read " + file.getFileName() + ": not a YAML mapping.");
            }
            return root;
        } catch (IOException | RuntimeException exception) {
            warn("Could not read " + file.getFileName() + ": " + exception.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static void warn(String message) {
        GuiShopPlatform.get().logger().warning(message);
    }
}
