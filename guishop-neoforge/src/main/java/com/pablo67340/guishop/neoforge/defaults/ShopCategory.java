package com.pablo67340.guishop.neoforge.defaults;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The shops a fresh install starts with, and the rule that decides which one
 * an item belongs to.
 * <p>
 * Declared in order, and the first match wins, so an item lands in exactly one
 * shop. That matters for more than tidiness: an item carrying two prices could
 * be bought in one shop and sold in the other, and the arithmetic in
 * {@link PriceBook} would have nothing to say about it.
 * <p>
 * The order is roughly most specific first. Food is decided by whether the
 * item can be eaten, so it has to come before the material rules that would
 * otherwise claim bread as a wheat product.
 */
enum ShopCategory {

    // Cake is a block you eat from rather than an edible item, so it has no
    // food component to find it by.
    FOOD("Food", "COOKED_BEEF", "&6&lFood", "Something to eat",
            ((Predicate<Item>) item -> item.components().has(DataComponents.FOOD))
                    .or(named("cake"))),

    COMBAT("Combat", "DIAMOND_SWORD", "&c&lCombat", "Weapons, armour and ammunition",
            tagged(ItemTags.SWORDS, ItemTags.ARROWS, ItemTags.HEAD_ARMOR, ItemTags.CHEST_ARMOR,
                    ItemTags.LEG_ARMOR, ItemTags.FOOT_ARMOR)
                    .or(named("bow", "crossbow", "shield", "trident", "mace", "totem_of_undying",
                            "tnt", "firework_rocket", "wolf_armor", "fire_charge"))),

    TOOLS("Tools", "DIAMOND_PICKAXE", "&b&lTools", "Pickaxes, shovels and everything else",
            tagged(ItemTags.PICKAXES, ItemTags.AXES, ItemTags.SHOVELS, ItemTags.HOES,
                    ItemTags.COMPASSES)
                    .or(named("shears", "flint_and_steel", "fishing_rod", "bucket", "clock",
                            "spyglass", "brush", "lead", "name_tag", "saddle", "elytra",
                            "carrot_on_a_stick", "warped_fungus_on_a_stick", "recovery_compass",
                            "map", "writable_book", "book", "bundle", "paper"))
                    .or(suffix("_bucket", "_horse_armor"))
                    // Workstations. Held tools and the benches that use them
                    // are what a player goes looking for in the same trip.
                    .or(named("furnace", "blast_furnace", "smoker", "stonecutter",
                            "cartography_table", "fletching_table", "crafting_table",
                            "smithing_table", "loom", "composter", "barrel"))),

    REDSTONE("Redstone", "REDSTONE", "&4&lRedstone", "Wiring, pistons and contraptions",
            tagged(ItemTags.RAILS, ItemTags.BUTTONS, ItemTags.WOODEN_PRESSURE_PLATES)
                    .or(contains("redstone", "piston", "minecart", "copper_bulb"))
                    .or(named("observer", "comparator", "repeater", "hopper", "dropper",
                            "dispenser", "lever", "tripwire_hook", "daylight_detector", "target",
                            "note_block", "sculk_sensor", "calibrated_sculk_sensor",
                            "lightning_rod", "crafter", "slime_block", "honey_block",
                            "light_weighted_pressure_plate", "heavy_weighted_pressure_plate",
                            "stone_pressure_plate", "polished_blackstone_pressure_plate"))),

    BREWING("Brewing", "BREWING_STAND", "&5&lBrewing", "Potion ingredients and enchanting",
            named("brewing_stand", "glass_bottle", "cauldron", "blaze_powder", "magma_cream",
                    "fermented_spider_eye", "glistering_melon_slice", "golden_carrot",
                    "ghast_tear", "rabbit_foot", "phantom_membrane", "dragon_breath",
                    "nether_wart", "spider_eye", "blaze_rod", "redstone_lamp",
                    "enchanting_table", "bookshelf", "experience_bottle", "lectern",
                    "grindstone", "anvil", "chipped_anvil", "damaged_anvil", "smithing_table")),

    DYES("Dyes and Wool", "PINK_WOOL", "&d&lDyes and Wool", "Colour for everything",
            tagged(ItemTags.WOOL, ItemTags.WOOL_CARPETS, ItemTags.BEDS, ItemTags.BANNERS,
                    ItemTags.DYEABLE)
                    .or(suffix("_dye", "_stained_glass", "_stained_glass_pane", "_shulker_box",
                            "_concrete", "_concrete_powder", "_glazed_terracotta"))
                    .or(named("white_dye", "bone_meal", "ink_sac", "glow_ink_sac", "cocoa_beans",
                            "shulker_box", "loom"))),

    DECORATION("Decoration", "FLOWER_POT", "&e&lDecoration", "Making the place look lived in",
            tagged(ItemTags.FLOWERS, ItemTags.SMALL_FLOWERS, ItemTags.TALL_FLOWERS,
                    ItemTags.CANDLES, ItemTags.SIGNS, ItemTags.HANGING_SIGNS, ItemTags.SKULLS,
                    ItemTags.DECORATED_POT_SHERDS)
                    .or(contains("torch", "lantern", "glass_pane", "campfire", "banner_pattern"))
                    .or(named("painting", "item_frame", "glow_item_frame", "flower_pot",
                            "armor_stand", "chain", "bell", "decorated_pot", "end_rod",
                            "conduit", "beacon", "jukebox", "scaffolding", "ladder", "cobweb"))),

    WOOD("Wood", "OAK_LOG", "&6&lWood", "Logs, planks and everything cut from them",
            tagged(ItemTags.LOGS, ItemTags.PLANKS, ItemTags.SAPLINGS, ItemTags.LEAVES,
                    ItemTags.BOATS, ItemTags.CHEST_BOATS, ItemTags.WOODEN_SLABS,
                    ItemTags.WOODEN_STAIRS, ItemTags.WOODEN_DOORS, ItemTags.WOODEN_TRAPDOORS,
                    ItemTags.WOODEN_FENCES, ItemTags.WOODEN_BUTTONS, ItemTags.FENCE_GATES,
                    ItemTags.BAMBOO_BLOCKS)
                    .or(contains("bamboo"))
                    .or(named("stick", "barrel", "chest", "trapped_chest", "crafting_table",
                            "bowl", "wooden_fence"))),

    FARMING("Farming", "WHEAT", "&a&lFarming", "Crops, seeds and the ground to grow them in",
            tagged(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                    .or(suffix("_seeds"))
                    .or(named("wheat", "carrot", "potato", "beetroot", "melon", "melon_slice",
                            "pumpkin", "carved_pumpkin", "sugar_cane", "cactus", "kelp",
                            "dried_kelp", "dried_kelp_block", "hay_block", "composter",
                            "sweet_berries", "glow_berries", "cocoa_beans", "sugar", "egg",
                            "honeycomb", "honeycomb_block", "honey_bottle", "beehive",
                            "bee_nest", "moss_block", "moss_carpet", "pitcher_plant",
                            "torchflower", "pitcher_pod", "torchflower_seeds"))),

    MINERALS("Minerals", "DIAMOND", "&b&lMinerals", "Ore, ingots and the blocks they make",
            suffix("_ingot", "_nugget", "_ore", "_scrap")
                    .or(prefix("raw_"))
                    .or(contains("copper"))
                    .or(named("glowstone_dust",
                            "coal", "charcoal", "diamond", "emerald", "lapis_lazuli", "quartz",
                            "amethyst_shard", "amethyst_block", "netherite_ingot", "coal_block",
                            "iron_block", "gold_block", "diamond_block", "emerald_block",
                            "lapis_block", "netherite_block", "redstone_block", "ancient_debris"))),

    DROPS("Mob Drops", "ENDER_PEARL", "&5&lMob Drops", "What the neighbours leave behind",
            named("bone", "bone_block", "string", "gunpowder", "leather", "feather",
                    "ender_pearl", "ender_eye", "slime_ball", "rotten_flesh", "ink_sac",
                    "prismarine_shard", "prismarine_crystals", "shulker_shell",
                    "nautilus_shell", "heart_of_the_sea", "nether_star", "echo_shard",
                    "turtle_scute", "armadillo_scute", "rabbit_hide", "phantom_membrane")),

    BUILDING("Building", "BRICKS", "&7&lBuilding", "Worked stone, glass and brick",
            tagged(ItemTags.STONE_BRICKS, ItemTags.TERRACOTTA, ItemTags.SLABS, ItemTags.STAIRS,
                    ItemTags.WALLS, ItemTags.FENCES, ItemTags.DOORS, ItemTags.TRAPDOORS)
                    .or(contains("brick", "glass", "prismarine", "purpur", "quartz", "polished",
                            "chiseled", "cut_", "smooth_", "cracked_", "mossy_", "tiles"))
                    .or(named("stone", "terracotta", "glowstone", "sea_lantern", "bricks",
                            "iron_bars", "packed_mud"))),

    NATURAL("Natural", "GRASS_BLOCK", "&2&lNatural", "The world as you found it",
            tagged(ItemTags.DIRT, ItemTags.SAND, ItemTags.COALS)
                    // Growing things that are neither a crop nor a flower.
                    .or(contains("mushroom", "fungus", "vine", "roots", "dripleaf", "sculk",
                            "froglight", "chorus"))
                    .or(named("short_grass", "tall_grass", "fern", "large_fern", "dead_bush",
                            "seagrass", "nether_sprouts", "glow_lichen", "lily_pad",
                            "nether_wart_block", "warped_wart_block", "shroomlight",
                            "gilded_blackstone"))
                    .or(named("cobblestone", "cobbled_deepslate", "deepslate", "tuff", "calcite",
                            "gravel", "clay", "clay_ball", "flint", "obsidian", "crying_obsidian",
                            "netherrack", "soul_sand", "soul_soil", "basalt", "blackstone",
                            "magma_block", "end_stone", "sandstone", "red_sandstone", "ice",
                            "packed_ice", "blue_ice", "snow", "snow_block", "snowball",
                            "sponge", "wet_sponge", "mud", "sculk", "granite", "diorite",
                            "andesite", "dripstone_block", "pointed_dripstone"))),

    MISC("Miscellaneous", "CHEST", "&f&lMiscellaneous", "Everything else worth a price",
            item -> true);

    private final String shopName;
    private final String icon;
    private final String title;
    private final String blurb;
    private final Predicate<Item> rule;

    ShopCategory(String shopName, String icon, String title, String blurb, Predicate<Item> rule) {
        this.shopName = shopName;
        this.icon = icon;
        this.title = title;
        this.blurb = blurb;
        this.rule = rule;
    }

    String shopName() {
        return shopName;
    }

    String icon() {
        return icon;
    }

    String title() {
        return title;
    }

    String blurb() {
        return blurb;
    }

    /** The first category that claims this item. Never null; MISC takes the rest. */
    static ShopCategory of(Item item) {
        for (ShopCategory category : values()) {
            if (category.rule.test(item)) {
                return category;
            }
        }
        return MISC;
    }

    // ------------------------------------------------------------ rule parts

    private static Predicate<Item> tagged(TagKey<Item>... tags) {
        List<TagKey<Item>> wanted = List.of(tags);
        return item -> wanted.stream().anyMatch(tag -> item.builtInRegistryHolder().is(tag));
    }

    private static Predicate<Item> named(String... ids) {
        Set<String> wanted = Set.of(ids);
        return item -> wanted.contains(path(item));
    }

    private static Predicate<Item> suffix(String... suffixes) {
        List<String> wanted = List.of(suffixes);
        return item -> wanted.stream().anyMatch(path(item)::endsWith);
    }

    private static Predicate<Item> prefix(String... prefixes) {
        List<String> wanted = List.of(prefixes);
        return item -> wanted.stream().anyMatch(path(item)::startsWith);
    }

    private static Predicate<Item> contains(String... fragments) {
        List<String> wanted = List.of(fragments);
        return item -> wanted.stream().anyMatch(path(item)::contains);
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
