package com.pablo67340.guishop.neoforge.defaults;

import com.pablo67340.guishop.platform.GuiShopPlatform;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out what every craftable item is worth by following vanilla's recipes.
 * <p>
 * The seed table in {@link SeedPrices} fixes the price of things nothing can
 * make. Everything else takes the cheapest recipe that reaches it, so an iron
 * block costs what nine ingots cost and a hopper costs its five ingots and a
 * chest, whichever way round the player gets there.
 * <p>
 * <b>Why that stops the shop being a money printer.</b> Buying an item back is
 * always a fraction of what it sells for, and an item's price is never more
 * than the sum of its ingredients. Buy the ingredients, craft them, sell the
 * result, and you get back a fraction of what you paid. Vanilla's reverse
 * recipes, a block back into nine ingots, are ordinary recipes here too, so
 * the same holds in that direction. {@link #audit} then checks every recipe
 * individually, because rounding the prices to readable numbers could
 * otherwise reopen by a penny what the arithmetic closed.
 */
final class PriceBook {

    /** What an item sells back for, as a fraction of its purchase price. */
    private static final double SELL_RATIO = 0.25;

    /** Enough to carry a price along the longest vanilla crafting chain. */
    private static final int RELAXATION_PASSES = 32;

    private static final double EPSILON = 1.0e-9;

    private final Map<Item, Double> buy;
    private final Map<Item, Double> sell;
    private final int adjusted;

    private PriceBook(Map<Item, Double> buy, Map<Item, Double> sell, int adjusted) {
        this.buy = buy;
        this.sell = sell;
        this.adjusted = adjusted;
    }

    static PriceBook build(MinecraftServer server) {
        List<Craft> crafts = collect(server);
        Map<Item, Double> cost = relax(crafts, seeds());

        Map<Item, Double> buy = new HashMap<>(cost.size());
        Map<Item, Double> sell = new HashMap<>(cost.size());
        cost.forEach((item, value) -> {
            double price = round(Math.max(value, 0.25));
            buy.put(item, price);
            sell.put(item, floor(price * SELL_RATIO));
        });

        int adjusted = audit(crafts, buy, sell);
        return new PriceBook(buy, sell, adjusted);
    }

    boolean has(Item item) {
        return buy.containsKey(item);
    }

    double buyPrice(Item item) {
        return buy.get(item);
    }

    double sellPrice(Item item) {
        return sell.get(item);
    }

    int size() {
        return buy.size();
    }

    /** How many sell prices the audit had to pull down. Expected to be small. */
    int adjustments() {
        return adjusted;
    }

    // ------------------------------------------------------------- gathering

    /**
     * One way of making one item, reduced to what pricing needs: the result,
     * how many come out, and the choices available for each ingredient.
     */
    private record Craft(Item result, int count, List<Item[]> inputs) {
    }

    private static List<Craft> collect(MinecraftServer server) {
        HolderLookup.Provider registries = server.registryAccess();
        List<Craft> crafts = new ArrayList<>();

        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            // Vanilla recipes only. A mod's cheaper route to a vanilla item
            // would otherwise set the price of it for everyone.
            if (!ResourceLocation.DEFAULT_NAMESPACE.equals(holder.id().getNamespace())) {
                continue;
            }

            Craft craft = read(holder.value(), registries);
            if (craft != null) {
                crafts.add(craft);
            }
        }

        crafts.addAll(smithing());
        return crafts;
    }

    private static Craft read(Recipe<?> recipe, HolderLookup.Provider registries) {
        if (recipe.isSpecial()) {
            return null;
        }

        ItemStack result;
        NonNullList<Ingredient> ingredients;
        try {
            result = recipe.getResultItem(registries);
            ingredients = recipe.getIngredients();
        } catch (RuntimeException exception) {
            // A recipe that cannot describe itself outside the game is not
            // worth failing config generation over.
            return null;
        }

        if (result == null || result.isEmpty() || ingredients.isEmpty()) {
            return null;
        }

        List<Item[]> inputs = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            // Shaped recipes pad their grid with empty slots.
            if (ingredient.isEmpty()) {
                continue;
            }
            Item[] options = Arrays.stream(ingredient.getItems())
                    .map(ItemStack::getItem)
                    .distinct()
                    .toArray(Item[]::new);
            if (options.length == 0) {
                return null;
            }
            inputs.add(options);
        }

        return inputs.isEmpty() ? null : new Craft(result.getItem(), Math.max(1, result.getCount()), inputs);
    }

    /**
     * Netherite gear, which smithing does not expose its ingredients through.
     * <p>
     * The template is left out. It is reusable, so charging for it would price
     * every netherite item as though the first one were the only one.
     */
    private static List<Craft> smithing() {
        return List.of(
                upgrade(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD),
                upgrade(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE),
                upgrade(Items.DIAMOND_AXE, Items.NETHERITE_AXE),
                upgrade(Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL),
                upgrade(Items.DIAMOND_HOE, Items.NETHERITE_HOE),
                upgrade(Items.DIAMOND_HELMET, Items.NETHERITE_HELMET),
                upgrade(Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE),
                upgrade(Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS),
                upgrade(Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS));
    }

    private static Craft upgrade(Item from, Item to) {
        return new Craft(to, 1, List.of(new Item[]{from}, new Item[]{Items.NETHERITE_INGOT}));
    }

    private static Map<Item, Double> seeds() {
        Map<Item, Double> costs = new LinkedHashMap<>();
        SeedPrices.values().forEach((id, price) -> {
            ResourceLocation key = ResourceLocation.tryParse("minecraft:" + id);
            Item item = key == null ? null : BuiltInRegistries.ITEM.get(key);
            if (item == null || item == Items.AIR) {
                GuiShopPlatform.get().logUtil().debugLog(
                        "Seed price for unknown item '" + id + "' ignored.");
                return;
            }
            costs.put(item, price);
        });
        return costs;
    }

    // ------------------------------------------------------------- the solve

    /**
     * Settles every price by repeatedly taking the cheapest known way to make
     * each item.
     * <p>
     * Prices only ever fall, and nothing falls below its seed, so this reaches
     * a fixed point rather than oscillating. Looping is what lets a price
     * travel a chain in any order: logs to planks to sticks to a pickaxe, with
     * the recipes visited in whatever order the game listed them.
     */
    private static Map<Item, Double> relax(List<Craft> crafts, Map<Item, Double> seeded) {
        Map<Item, Double> cost = new LinkedHashMap<>(seeded);

        for (int pass = 0; pass < RELAXATION_PASSES; pass++) {
            boolean changed = false;

            for (Craft craft : crafts) {
                Double total = inputCost(craft, cost);
                if (total == null) {
                    continue;
                }

                double derived = total / craft.count();
                Double current = cost.get(craft.result());
                if (current == null || derived < current - EPSILON) {
                    cost.put(craft.result(), derived);
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }

        return cost;
    }

    /**
     * What the ingredients come to, taking the cheapest acceptable item for
     * each slot.
     *
     * @return null if any slot has nothing priced, meaning this recipe cannot
     *         say anything about its result yet
     */
    private static Double inputCost(Craft craft, Map<Item, Double> prices) {
        double total = 0;
        for (Item[] options : craft.inputs()) {
            double cheapest = Double.MAX_VALUE;
            for (Item option : options) {
                Double price = prices.get(option);
                if (price != null && price < cheapest) {
                    cheapest = price;
                }
            }
            if (cheapest == Double.MAX_VALUE) {
                return null;
            }
            total += cheapest;
        }
        return total;
    }

    // ----------------------------------------------------------------- audit

    /**
     * Checks that no recipe can be bought into and sold out of at a profit,
     * and pulls the sell price down where one can.
     * <p>
     * The derivation already guarantees this, but the prices players see are
     * rounded, and a recipe whose ingredients each lost a fraction to rounding
     * while the result gained one is exactly where a loop would appear. Only
     * recipes whose ingredients are all purchasable here are checked: the rest
     * cannot be used to extract money from the shop no matter what they cost.
     *
     * @return how many prices were changed
     */
    private static int audit(List<Craft> crafts, Map<Item, Double> buy, Map<Item, Double> sell) {
        int adjusted = 0;

        for (int pass = 0; pass < 8; pass++) {
            boolean changed = false;

            for (Craft craft : crafts) {
                if (!sell.containsKey(craft.result())) {
                    continue;
                }
                Double outlay = inputCost(craft, buy);
                if (outlay == null) {
                    continue;
                }

                double revenue = sell.get(craft.result()) * craft.count();
                if (revenue > outlay + EPSILON) {
                    sell.put(craft.result(), floor(outlay / craft.count()));
                    changed = true;
                    adjusted++;
                }
            }

            if (!changed) {
                break;
            }
        }

        return adjusted;
    }

    // ------------------------------------------------------------- rounding

    /**
     * Rounds a price to something a player can read, coarser as it grows: no
     * one needs a diamond block priced to the penny.
     */
    private static double round(double value) {
        if (value >= 1000) {
            return Math.round(value / 10.0) * 10.0;
        }
        if (value >= 100) {
            return Math.round(value);
        }
        return Math.round(value * 100.0) / 100.0;
    }

    /** Always downwards, so rounding a sell price cannot create a profit. */
    private static double floor(double value) {
        return Math.max(0.01, Math.floor(value * 100.0) / 100.0);
    }
}
