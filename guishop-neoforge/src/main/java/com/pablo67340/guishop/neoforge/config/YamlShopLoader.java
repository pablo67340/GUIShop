package com.pablo67340.guishop.neoforge.config;

import com.pablo67340.guishop.definition.FireworkInfo;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoPage;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import com.pablo67340.guishop.util.MathUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.math.BigDecimal;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** menu.yml nests everything under this key; shop files do not. */
    private static final String MENU_ROOT_KEY = "Menu";

    /**
     * Loads one shop file.
     *
     * @param file the shop YAML
     * @param name the shop name, which is the file name without its extension
     * @return the parsed shop, or null if the file could not be read
     */
    public static NeoShop loadShop(Path file, String name) {
        return load(file, name, null);
    }

    /**
     * Loads menu.yml.
     * <p>
     * The menu is the same structure as a shop - a titled, paged grid of items -
     * so it uses the same model. Its items navigate rather than sell, which the
     * item type already captures.
     *
     * @return the parsed menu, or null if the file could not be read
     */
    public static NeoShop loadMenu(Path file) {
        return load(file, MENU_ROOT_KEY, MENU_ROOT_KEY);
    }

    /**
     * @param rootKey the key the body is nested under, or null if it is at the
     *                top level
     */
    private static NeoShop load(Path file, String name, String rootKey) {
        Map<String, Object> root = read(file);
        if (root == null) {
            return null;
        }

        if (rootKey != null) {
            root = map(root.get(rootKey));
            if (root == null) {
                warn("'" + file.getFileName() + "' has no " + rootKey + " section.");
                return null;
            }
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

        ItemType type = parseType(data.get("type"), data.get("target-shop") != null);
        item.setItemType(type);
        item.setMaterial(materialFor(data.get("id"), type));

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

        Object buyPrice = price(data.get("buy-price"));
        if (buyPrice != null) {
            item.setBuyPrice(buyPrice);
        }
        Object sellPrice = price(data.get("sell-price"));
        if (sellPrice != null) {
            item.setSellPrice(sellPrice);
        }
        if (data.get("use-dynamic-pricing") != null) {
            item.setUseDynamicPricing(Boolean.parseBoolean(String.valueOf(data.get("use-dynamic-pricing"))));
        }
        if (data.get("sudo") != null) {
            item.setSudo(Boolean.parseBoolean(String.valueOf(data.get("sudo"))));
        }

        item.setEnchantments(enchantments(data.get("enchantments")));
        item.setItemFlags(spaceSeparated(data.get("item-flags")));
        item.setSkullUUID(string(data.get("skull-uuid"), null));
        item.setNBT(string(data.get("nbt"), null));

        if (data.get("custom-model-data") != null) {
            try {
                item.setCustomModelData(Integer.parseInt(String.valueOf(data.get("custom-model-data")).trim()));
            } catch (NumberFormatException exception) {
                warn(describe(shopName, slotKey) + " has an invalid custom-model-data.");
            }
        }

        item.setPotionInfo(potionInfo(data.get("potion-info")));
        item.setFireworkInfo(fireworkInfo(data.get("firework-info")));

        return item;
    }

    /**
     * Enchantments are one string of {@code NAME:LEVEL} pairs. Both comma and
     * space separators appear in configs written over the years.
     */
    private static String[] enchantments(Object raw) {
        String text = string(raw, null);
        if (text == null || text.isBlank()) {
            return null;
        }

        String cleaned = text.replaceAll("[,\\s]+$", "").trim();
        String[] parts = cleaned.contains(",") ? cleaned.split(",") : cleaned.split(" ");

        List<String> entries = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isBlank()) {
                entries.add(part.trim());
            }
        }
        return entries.isEmpty() ? null : entries.toArray(new String[0]);
    }

    private static List<String> spaceSeparated(Object raw) {
        String text = string(raw, null);
        if (text == null || text.isBlank()) {
            return null;
        }
        return Arrays.stream(text.trim().split("\\s+")).filter(part -> !part.isBlank()).toList();
    }

    private static PotionInfo potionInfo(Object raw) {
        Map<String, Object> data = map(raw);
        if (data == null) {
            return null;
        }

        return new PotionInfo(
                string(data.get("type"), "FIRE_RESISTANCE"),
                flag(data.get("splash")),
                flag(data.get("lingering")),
                flag(data.get("extended")),
                flag(data.get("upgraded")));
    }

    private static FireworkInfo fireworkInfo(Object raw) {
        Map<String, Object> data = map(raw);
        if (data == null) {
            return null;
        }

        List<FireworkInfo.ExplosionInfo> explosions = new ArrayList<>();
        if (data.get("explosions") instanceof List<?> list) {
            for (Object element : list) {
                Map<String, Object> explosion = map(element);
                if (explosion == null) {
                    continue;
                }
                explosions.add(new FireworkInfo.ExplosionInfo(
                        string(explosion.get("shape"), "small_ball"),
                        colours(explosion.get("colors")),
                        colours(explosion.get("fade-colors")),
                        flag(explosion.get("flicker")),
                        flag(explosion.get("trail"))));
            }
        }

        return new FireworkInfo(integer(data.get("flight"), 1), explosions);
    }

    /**
     * Firework colours are packed RGB integers.
     */
    private static List<Integer> colours(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }

        List<Integer> colours = new ArrayList<>(list.size());
        for (Object element : list) {
            if (element instanceof Number number) {
                colours.add(number.intValue());
            } else {
                try {
                    colours.add(Integer.parseInt(String.valueOf(element).trim()));
                } catch (NumberFormatException ignored) {
                    // A malformed colour is skipped rather than failing the item.
                }
            }
        }
        return colours;
    }

    private static boolean flag(Object raw) {
        return raw != null && Boolean.parseBoolean(String.valueOf(raw).trim());
    }

    private static String describe(String shopName, String slotKey) {
        return "Shop '" + shopName + "' slot " + slotKey;
    }

    /**
     * Mirrors the Bukkit module's defaulting: an entry with no declared type is
     * a normal shop item, unless it names a target shop, as the menu's icons do.
     */
    private static ItemType parseType(Object raw, boolean hasTargetShop) {
        ItemType fallback = hasTargetShop ? ItemType.SHOP_SHORTCUT : ItemType.SHOP;
        if (raw == null) {
            return fallback;
        }
        try {
            return ItemType.valueOf(String.valueOf(raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    /**
     * Navigation entries such as PLAYER_BALANCE are conventionally configured
     * without an id, so give those types their customary icon.
     */
    private static String materialFor(Object raw, ItemType type) {
        String material = string(raw, null);
        if (material != null && !material.isBlank()) {
            return material;
        }
        return type == ItemType.PLAYER_BALANCE ? "PLAYER_HEAD" : material;
    }

    /**
     * Normalises a configured price to a type {@code ItemDefinition} accepts.
     * <p>
     * It only recognises {@code BigDecimal} and {@code Integer}, so a raw
     * {@code Double} straight from the YAML parser would silently read as
     * having no price at all. The Bukkit module converts the same way.
     *
     * @return the price, or null if absent or unparseable
     */
    private static Object price(Object raw) {
        if (raw instanceof Integer) {
            return raw;
        }
        if (raw instanceof Number) {
            // Via toString so large or precise values are not rounded through a double.
            return new BigDecimal(raw.toString());
        }
        if (raw instanceof String text && !text.isBlank()) {
            // Tolerates a quoted or abbreviated price such as "1.5k".
            return MathUtil.tryParseAbbreviated(text.trim());
        }
        return null;
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
