package com.pablo67340.guishop.neoforge.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pablo67340.guishop.definition.FireworkInfo;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.neoforge.definition.NeoItem;
import com.pablo67340.guishop.neoforge.definition.NeoPage;
import com.pablo67340.guishop.neoforge.definition.NeoShop;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import com.pablo67340.guishop.util.MathUtil;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a shop or the menu from its JSON file.
 * <p>
 * The format is a title and a flat, ordered list of items. There are no pages
 * or slot numbers: the screen reflows its contents, so position in the list is
 * the only ordering that matters.
 */
public final class ShopLoader {

    private static final Gson GSON = new Gson();

    /** The single page the flat item list is held in, to fit the shared model. */
    static final String PAGE_KEY = "0";

    private ShopLoader() {
    }

    /**
     * @param name          the shop's identity, which is the file name without
     *                      its extension
     * @param titleFallback what to show as the heading when the file declares
     *                      no title of its own
     * @return the parsed shop, or null if the file could not be read
     */
    public static NeoShop load(Path file, String name, String titleFallback) {
        Map<String, Object> document = readDocument(file);
        if (document == null) {
            return null;
        }
        return fromDocument(document, name, titleFallback);
    }

    /**
     * Builds a shop from an already-parsed document, which migration also uses
     * so a converted file does not have to be read back from disk.
     */
    static NeoShop fromDocument(Map<String, Object> document, String name, String titleFallback) {
        NeoShop shop = new NeoShop();
        shop.setName(name);
        shop.setTitle(string(document.get("title"), titleFallback));

        NeoPage page = new NeoPage();
        List<?> items = document.get("items") instanceof List<?> list ? list : List.of();

        int index = 0;
        for (Object element : items) {
            Map<String, Object> data = map(element);
            if (data == null) {
                continue;
            }

            NeoItem item = readItem(data, index, name);
            page.getItems().put(String.valueOf(index), item);
            index++;
        }

        shop.getPages().put(PAGE_KEY, page);
        shop.determineHighestSlots();
        return shop;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readDocument(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Object> document = GSON.fromJson(reader,
                    new TypeToken<Map<String, Object>>() { }.getType());
            if (document == null) {
                warn("Could not read " + file.getFileName() + ": empty document.");
            }
            return document;
        } catch (IOException | JsonSyntaxException exception) {
            warn("Could not read " + file.getFileName() + ": " + exception.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ items

    /**
     * @param index the item's position in the list, used for ordering and to
     *              identify it in diagnostics
     */
    private static NeoItem readItem(Map<String, Object> data, int index, String shopName) {
        NeoItem item = new NeoItem();
        item.setShop(shopName);
        item.setSlot(index);
        item.setConfigSlot(index);

        ItemType type = parseType(data.get("type"), data.get("target-shop") != null);
        item.setItemType(type);
        item.setMaterial(materialFor(data.get("id"), type));

        item.setShopName(string(data.get("shop-name"), null));
        item.setBuyName(string(data.get("buy-name"), null));
        item.setName(string(data.get("name"), null));
        item.setTargetShop(string(data.get("target-shop"), null));
        item.setMobType(string(data.get("mob-type"), null));
        item.setNBT(string(data.get("nbt"), null));
        item.setSkullUUID(string(data.get("skull-uuid"), null));

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
            item.setUseDynamicPricing(flag(data.get("use-dynamic-pricing")));
        }
        if (data.get("sudo") != null) {
            item.setSudo(flag(data.get("sudo")));
        }

        item.setEnchantments(enchantments(data.get("enchantments")));
        item.setItemFlags(spaceSeparated(data.get("item-flags")));

        if (data.get("custom-model-data") != null) {
            item.setCustomModelData(integer(data.get("custom-model-data"), 0));
        }

        item.setPotionInfo(potionInfo(data.get("potion-info")));
        item.setFireworkInfo(fireworkInfo(data.get("firework-info")));

        return item;
    }

    /**
     * An entry with no declared type is a normal shop item, unless it names a
     * target shop, as the menu's icons do.
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
        return type == ItemType.PLAYER_BALANCE ? "PLAYER_HEAD" : null;
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

    /** Firework colours are packed RGB integers. */
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

    // -------------------------------------------------------------- utilities

    /**
     * Normalises a configured price to a type {@code ItemDefinition} accepts.
     * <p>
     * It only recognises {@code BigDecimal} and {@code Integer}, so a raw
     * {@code Double} from either parser would silently read as no price at all.
     *
     * @return the price, or null if absent or unparseable
     */
    private static Object price(Object raw) {
        if (raw instanceof Integer) {
            return raw;
        }
        if (raw instanceof Number) {
            // Via toString so values are not rounded through a double.
            return new BigDecimal(raw.toString());
        }
        if (raw instanceof String text && !text.isBlank()) {
            // Tolerates a quoted or abbreviated price such as "1.5k".
            return MathUtil.tryParseAbbreviated(text.trim());
        }
        return null;
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

    /**
     * JSON numbers arrive as doubles, so an integer field has to be narrowed
     * rather than parsed from its text form.
     */
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

    private static boolean flag(Object raw) {
        return raw != null && Boolean.parseBoolean(String.valueOf(raw).trim());
    }

    private static void warn(String message) {
        GuiShopPlatform.get().logger().warning(message);
    }
}
