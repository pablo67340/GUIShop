package com.pablo67340.guishop.neoforge.config;

import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoPage;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads GUIShop's shop YAML into the shared core model.
 * <p>
 * The format is the Bukkit plugin's, so an existing server's {@code shops/}
 * directory can be dropped in unchanged.
 */
public final class YamlShopLoader {

    private YamlShopLoader() {
    }

    /**
     * Loads one shop file.
     *
     * @param file the shop YAML
     * @param name the shop name, which is the file name without its extension
     * @return the parsed shop, or null if the file could not be read
     */
    public static NeoShop loadShop(Path file, String name) {
        Map<String, Object> root = read(file);
        if (root == null) {
            return null;
        }

        NeoShop shop = new NeoShop();
        shop.setName(name);
        shop.setTitle(string(root.get("title"), name));
        shop.setConfiguredRows(integer(root.get("rows"), 0));

        Map<String, Object> pages = map(root.get("pages"));
        if (pages == null) {
            warn("Shop '" + name + "' has no pages section.");
            return shop;
        }

        for (Map.Entry<String, Object> pageEntry : pages.entrySet()) {
            Map<String, Object> pageData = map(pageEntry.getValue());
            if (pageData == null) {
                continue;
            }

            NeoPage page = new NeoPage();
            // A page may set its own row count, otherwise the shop's applies.
            page.setConfiguredRows(integer(pageData.get("rows"), shop.getConfiguredRows()));

            Map<String, Object> items = map(pageData.get("items"));
            if (items != null) {
                for (Map.Entry<String, Object> itemEntry : items.entrySet()) {
                    Map<String, Object> itemData = map(itemEntry.getValue());
                    if (itemData == null) {
                        continue;
                    }

                    NeoItem item = readItem(itemData, itemEntry.getKey(), name);
                    if (item != null) {
                        page.getItems().put(itemEntry.getKey(), item);
                    }
                }
            }

            shop.getPages().put(pageEntry.getKey(), page);
        }

        shop.determineHighestSlots();
        return shop;
    }

    private static NeoItem readItem(Map<String, Object> data, String slotKey, String shopName) {
        Integer slot = parseSlot(slotKey);
        if (slot == null) {
            warn("Shop '" + shopName + "' has a non-numeric slot '" + slotKey + "'.");
            return null;
        }

        NeoItem item = new NeoItem();
        item.setShop(shopName);
        item.setSlot(slot);
        item.setConfigSlot(slot);
        item.setMaterial(string(data.get("id"), null));
        item.setItemType(parseType(data.get("type")));

        item.setShopName(string(data.get("shop-name"), null));
        item.setBuyName(string(data.get("buy-name"), null));
        item.setName(string(data.get("name"), null));
        item.setTargetShop(string(data.get("target-shop"), null));
        item.setMobType(string(data.get("mob-type"), null));
        item.setNBT(string(data.get("nbt"), null));

        item.setShopLore(stringList(data.get("shop-lore")));
        item.setLore(stringList(data.get("lore")));
        item.setBuyLore(stringList(data.get("buy-lore")));
        item.setCommands(stringList(data.get("commands")));

        if (data.get("buy-price") != null) {
            item.setBuyPrice(data.get("buy-price"));
        }
        if (data.get("sell-price") != null) {
            item.setSellPrice(data.get("sell-price"));
        }
        if (data.get("use-dynamic-pricing") != null) {
            item.setUseDynamicPricing(Boolean.parseBoolean(String.valueOf(data.get("use-dynamic-pricing"))));
        }
        if (data.get("sudo") != null) {
            item.setSudo(Boolean.parseBoolean(String.valueOf(data.get("sudo"))));
        }

        String enchantments = string(data.get("enchantments"), null);
        if (enchantments != null) {
            item.setEnchantments(enchantments.split(" "));
        }

        return item;
    }

    /**
     * Mirrors the Bukkit module's defaulting: an entry with prices but no
     * declared type is a normal shop item.
     */
    private static ItemType parseType(Object raw) {
        if (raw == null) {
            return ItemType.SHOP;
        }
        try {
            return ItemType.valueOf(String.valueOf(raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ItemType.SHOP;
        }
    }

    private static Integer parseSlot(String key) {
        try {
            return Integer.parseInt(key.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Map<String, Object> read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            Map<String, Object> root = map(loaded);
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
    static Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        return ((List<Object>) list).stream().map(String::valueOf).toList();
    }

    static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void warn(String message) {
        GuiShopPlatform.get().logger().warning(message);
    }
}
