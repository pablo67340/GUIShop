package com.pablo67340.guishop.neoforge.defaults;

import com.pablo67340.guishop.neoforge.config.JsonFiles;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Writes a full set of shops the first time the mod runs with none.
 * <p>
 * Without this the mod is inert until someone builds a catalogue by hand,
 * which is a poor first impression for something whose point is that you never
 * have to edit a config. What gets written is ordinary JSON in the normal
 * place, so it is a starting point rather than something built in: edit it in
 * game, or delete a shop you do not want.
 * <p>
 * Prices come from {@link PriceBook}, which derives them from the recipes the
 * server actually loaded.
 */
public final class DefaultShops {

    /**
     * Items a price would be wrong for rather than merely unknown.
     * <p>
     * Most unobtainable blocks need no mention: nothing makes bedrock and
     * nothing is seeded as bedrock, so it never gets a price and never
     * appears. These are the ones that do have a recipe or a seed but should
     * not be on sale anyway.
     */
    private static final Set<String> EXCLUDED = Set.of(
            "bedrock", "barrier", "light", "structure_block", "structure_void", "jigsaw",
            "command_block", "chain_command_block", "repeating_command_block",
            "command_block_minecart", "debug_stick", "knowledge_book", "spawner",
            "trial_spawner", "vault", "budding_amethyst", "reinforced_deepslate",
            "end_portal_frame", "filled_map", "written_book", "enchanted_book",
            "player_head", "petrified_oak_slab", "suspicious_sand", "suspicious_gravel",
            "trial_key", "ominous_trial_key", "ominous_bottle");

    private DefaultShops() {
    }

    /**
     * Generates the shops and the menu if, and only if, there are none.
     * <p>
     * Deliberately all or nothing. A server that has deleted every shop on
     * purpose is indistinguishable from a new one, but a server with a single
     * shop clearly means it, and quietly adding thirteen more to it would be
     * worse than doing nothing.
     */
    public static void generateIfMissing(MinecraftServer server) {
        Path dataFolder = GuiShopPlatform.get().dataFolder();
        Path shopsDir = dataFolder.resolve("shops");

        if (hasShops(shopsDir)) {
            return;
        }

        GuiShopPlatform.get().logger().info(
                "No shops found. Generating a default set priced from this server's recipes.");

        PriceBook prices = PriceBook.build(server);
        Map<ShopCategory, List<Item>> grouped = group(prices);

        int written = 0;
        int items = 0;
        for (Map.Entry<ShopCategory, List<Item>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            if (writeShop(shopsDir, entry.getKey(), entry.getValue(), prices)) {
                written++;
                items += entry.getValue().size();
            }
        }

        writeMenu(dataFolder, grouped);

        GuiShopPlatform.get().logger().info("Generated " + written + " shop(s) with " + items
                + " item(s), priced from " + prices.size() + " recipe-derived values"
                + (prices.adjustments() == 0
                        ? "."
                        : ", " + prices.adjustments() + " sell price(s) capped to keep crafting"
                                + " unprofitable."));
    }

    /**
     * Whether anything is already configured, counting legacy YAML so a server
     * mid-migration is not handed a second catalogue.
     */
    private static boolean hasShops(Path shopsDir) {
        if (!Files.isDirectory(shopsDir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(shopsDir)) {
            return stream.anyMatch(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return Files.isRegularFile(path) && (name.endsWith(".json") || name.endsWith(".yml"));
            });
        } catch (IOException exception) {
            // Unreadable is not empty. Generating into a directory we cannot
            // list risks writing over shops that are already there.
            GuiShopPlatform.get().logger().warning(
                    "Could not list " + shopsDir + ": " + exception.getMessage());
            return true;
        }
    }

    // ------------------------------------------------------------- selection

    private static Map<ShopCategory, List<Item>> group(PriceBook prices) {
        Map<ShopCategory, List<Item>> grouped = new EnumMap<>(ShopCategory.class);
        for (ShopCategory category : ShopCategory.values()) {
            grouped.put(category, new ArrayList<>());
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (!sellable(item, prices)) {
                continue;
            }
            grouped.get(ShopCategory.of(item)).add(item);
        }

        // Cheapest first, so a shop opens on what a new player can afford.
        grouped.values().forEach(items -> items.sort(
                Comparator.comparingDouble(prices::buyPrice).thenComparing(DefaultShops::path)));

        return grouped;
    }

    private static boolean sellable(Item item, PriceBook prices) {
        if (item == Items.AIR || !prices.has(item)) {
            return false;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (!ResourceLocation.DEFAULT_NAMESPACE.equals(key.getNamespace())) {
            return false;
        }

        String path = key.getPath();
        return !EXCLUDED.contains(path) && !path.endsWith("_spawn_egg");
    }

    // --------------------------------------------------------------- writing

    private static boolean writeShop(Path shopsDir, ShopCategory category, List<Item> items,
                                     PriceBook prices) {
        List<Map<String, Object>> entries = new ArrayList<>(items.size());
        for (Item item : items) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "SHOP");
            entry.put("id", path(item).toUpperCase(Locale.ROOT));
            entry.put("buy-price", prices.buyPrice(item));
            entry.put("sell-price", prices.sellPrice(item));
            entries.add(entry);
        }

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", category.title());
        document.put("items", entries);

        return JsonFiles.write(shopsDir.resolve(category.shopName() + ".json"), document);
    }

    /**
     * Writes the menu, but never over one that exists: a server can have its
     * own menu pointing at shops it has not built yet.
     */
    private static void writeMenu(Path dataFolder, Map<ShopCategory, List<Item>> grouped) {
        Path menu = dataFolder.resolve("menu.json");
        if (Files.exists(menu) || Files.exists(dataFolder.resolve("menu.yml"))) {
            GuiShopPlatform.get().logger().info(
                    "Left the existing menu alone. Add the new shops to it to reach them.");
            return;
        }

        List<Map<String, Object>> icons = new ArrayList<>();
        for (Map.Entry<ShopCategory, List<Item>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            ShopCategory category = entry.getKey();

            Map<String, Object> icon = new LinkedHashMap<>();
            icon.put("id", category.icon());
            icon.put("name", category.title());
            icon.put("lore", List.of(
                    "&7" + category.blurb(),
                    "&8" + entry.getValue().size() + " items",
                    "",
                    "&e\u25b6 Click to browse"));
            icon.put("target-shop", category.shopName());
            icons.add(icon);
        }

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("items", icons);
        JsonFiles.write(menu, document);
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
