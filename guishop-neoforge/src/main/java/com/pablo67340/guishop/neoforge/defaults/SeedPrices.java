package com.pablo67340.guishop.neoforge.defaults;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Starting values for the things no recipe can make.
 * <p>
 * Every other price in the generated shops is worked out from these by
 * following vanilla's recipes, so this table is the only place a number is
 * chosen rather than derived. Keeping it to raw drops and gathered blocks is
 * what makes the result consistent: if iron ore is worth this much then an
 * iron ingot, a bucket and a hopper all follow from it, and no two of them can
 * disagree.
 * <p>
 * The scale is roughly one unit per block of dirt. A player with the default
 * starting balance can kit themselves out in iron but not diamond.
 * <p>
 * An item named here that does not exist on this version is ignored, so the
 * table can mention things from either side of a version change.
 */
final class SeedPrices {

    private SeedPrices() {
    }

    static Map<String, Double> values() {
        Map<String, Double> seeds = new LinkedHashMap<>();

        // ------------------------------------------------------------ stone
        put(seeds, 1.0, "dirt", "coarse_dirt", "rooted_dirt", "cobblestone", "netherrack");
        put(seeds, 2.0, "sand", "red_sand", "gravel", "cobbled_deepslate", "deepslate", "tuff",
                "basalt", "blackstone", "mud", "sandstone");
        put(seeds, 3.0, "grass_block", "podzol", "clay_ball", "dripstone_block", "pointed_dripstone");
        put(seeds, 4.0, "calcite", "end_stone", "soul_sand", "soul_soil", "snowball", "ice", "flint");
        put(seeds, 6.0, "mycelium", "moss_block", "sculk");
        put(seeds, 8.0, "magma_block");
        put(seeds, 25.0, "obsidian");
        put(seeds, 60.0, "crying_obsidian", "gilded_blackstone");
        put(seeds, 80.0, "sponge");
        put(seeds, 100.0, "sculk_catalyst", "sculk_shrieker");
        put(seeds, 40.0, "ochre_froglight", "verdant_froglight", "pearlescent_froglight");

        // ------------------------------------------------------------- ores
        // Priced as what they drop, since the ore blocks themselves come from
        // silk touch rather than from mining normally.
        put(seeds, 10.0, "coal");
        put(seeds, 12.0, "raw_copper", "redstone", "glowstone_dust");
        put(seeds, 14.0, "lapis_lazuli", "quartz");
        put(seeds, 25.0, "amethyst_shard");
        put(seeds, 30.0, "raw_iron");
        put(seeds, 70.0, "raw_gold");
        put(seeds, 250.0, "emerald");
        put(seeds, 300.0, "diamond");
        put(seeds, 900.0, "ancient_debris");

        // ------------------------------------------------------------- wood
        // A log makes four planks, so this sets the price of most wooden
        // things further down the chain.
        put(seeds, 8.0, "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log",
                "dark_oak_log", "mangrove_log", "cherry_log", "crimson_stem", "warped_stem");
        put(seeds, 6.0, "bamboo_block");
        put(seeds, 2.0, "bamboo");
        put(seeds, 2.0, "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves",
                "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "cherry_leaves",
                "azalea_leaves", "flowering_azalea_leaves");
        put(seeds, 10.0, "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
                "acacia_sapling", "dark_oak_sapling", "cherry_sapling", "mangrove_propagule",
                "azalea", "flowering_azalea");
        put(seeds, 5.0, "brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus");
        put(seeds, 4.0, "brown_mushroom_block", "red_mushroom_block", "mushroom_stem",
                "nether_wart_block", "warped_wart_block", "shroomlight");

        // ---------------------------------------------------------- foliage
        put(seeds, 3.0, "short_grass", "tall_grass", "fern", "large_fern", "dead_bush",
                "seagrass", "nether_sprouts", "crimson_roots", "warped_roots");
        put(seeds, 4.0, "vine", "twisting_vines", "weeping_vines", "hanging_roots", "glow_lichen");
        put(seeds, 6.0, "lily_pad", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
                "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy",
                "cornflower", "lily_of_the_valley", "torchflower", "wither_rose",
                "sunflower", "lilac", "rose_bush", "peony", "pink_petals",
                "small_dripleaf", "big_dripleaf");
        put(seeds, 20.0, "spore_blossom", "chorus_fruit");

        // ---------------------------------------------------------- farming
        put(seeds, 2.0, "wheat_seeds", "beetroot_seeds", "melon_seeds", "pumpkin_seeds", "kelp");
        put(seeds, 40.0, "torchflower_seeds", "pitcher_pod");
        put(seeds, 3.0, "cactus", "melon_slice");
        put(seeds, 4.0, "sugar_cane");
        put(seeds, 5.0, "beetroot");
        put(seeds, 6.0, "wheat", "carrot", "potato");
        put(seeds, 8.0, "apple", "pumpkin", "nether_wart", "cocoa_beans", "glow_berries", "egg");
        put(seeds, 6.0, "sweet_berries");
        put(seeds, 12.0, "honeycomb");
        put(seeds, 15.0, "honey_bottle");

        // -------------------------------------------------------- mob drops
        put(seeds, 4.0, "rotten_flesh");
        put(seeds, 6.0, "feather", "string");
        put(seeds, 8.0, "bone", "ink_sac", "rabbit_hide");
        put(seeds, 8.0, "chicken", "cod", "mutton", "rabbit");
        put(seeds, 10.0, "beef", "porkchop", "salmon");
        put(seeds, 12.0, "spider_eye", "white_wool");
        put(seeds, 20.0, "leather", "slime_ball", "gunpowder", "glow_ink_sac", "prismarine_shard",
                "tropical_fish", "pufferfish");
        put(seeds, 30.0, "prismarine_crystals");
        put(seeds, 60.0, "rabbit_foot");
        put(seeds, 70.0, "ender_pearl", "phantom_membrane");
        put(seeds, 90.0, "blaze_rod");
        put(seeds, 120.0, "turtle_scute", "armadillo_scute");
        put(seeds, 150.0, "ghast_tear", "nautilus_shell", "skeleton_skull", "zombie_head",
                "creeper_head", "piglin_head");
        put(seeds, 200.0, "dragon_breath", "echo_shard");
        put(seeds, 350.0, "shulker_shell");
        put(seeds, 400.0, "wither_skeleton_skull");
        put(seeds, 1500.0, "heart_of_the_sea", "totem_of_undying");
        put(seeds, 2000.0, "dragon_head");
        put(seeds, 6000.0, "nether_star", "elytra");
        put(seeds, 8000.0, "enchanted_golden_apple");

        // ------------------------------------------------------------ tools
        // No recipe makes these, but they are ordinary survival items.
        put(seeds, 100.0, "name_tag");
        put(seeds, 120.0, "saddle");
        put(seeds, 800.0, "trident");

        // ---------------------------------------------------------- buckets
        // A bucket is craftable and prices itself; these are what a filled one
        // is worth, a little over the empty bucket it takes.
        put(seeds, 95.0, "water_bucket", "powder_snow_bucket");
        put(seeds, 100.0, "milk_bucket");
        put(seeds, 130.0, "lava_bucket");

        return seeds;
    }

    private static void put(Map<String, Double> seeds, double price, String... ids) {
        for (String id : ids) {
            seeds.put(id, price);
        }
    }
}
