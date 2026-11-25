package com.pablo67340.guishop.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the option lists the picker shows.
 * <p>
 * Everything comes from the live registries, so modded items, entities and
 * enchantments appear without this class knowing anything about them.
 */
final class Choices {

    /** The vanilla namespace, which the config may leave off. */
    private static final String VANILLA = "minecraft";

    /**
     * Built once each: the registries do not change while the game is running,
     * and building the item list means walking a few thousand entries.
     */
    private static List<PickerScreen.Choice> items;
    private static List<PickerScreen.Choice> entities;

    private Choices() {
    }

    /**
     * Every registered item.
     */
    static List<PickerScreen.Choice> items() {
        if (items != null) {
            return items;
        }

        List<PickerScreen.Choice> built = new ArrayList<>();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            var item = entry.getValue();

            // Air is registered but is not a thing anyone can sell.
            if (item == Items.AIR) {
                continue;
            }

            built.add(PickerScreen.Choice.of(configValue(id), item.getDescription(),
                    id.toString(), new ItemStack(item)));
        }

        built.sort(byLabel());
        items = List.copyOf(built);
        return items;
    }

    /**
     * Every registered entity type, for a spawner's mob.
     */
    static List<PickerScreen.Choice> entities() {
        if (entities != null) {
            return entities;
        }

        List<PickerScreen.Choice> built = new ArrayList<>();
        built.add(none("No mob"));

        for (var entry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            EntityType<?> type = entry.getValue();

            // The spawn egg makes the list recognisable at a glance; plenty of
            // entities have none, which the spawner stands in for.
            SpawnEggItem egg = SpawnEggItem.byId(type);
            ItemStack icon = new ItemStack(egg == null ? Items.SPAWNER : egg);

            built.add(PickerScreen.Choice.of(configValue(id), type.getDescription(),
                    id.toString(), icon));
        }

        built.sort(byLabel());
        entities = List.copyOf(built);
        return entities;
    }

    /**
     * Every enchantment the connected world has.
     * <p>
     * Not cached, and read from the world's registries rather than the built-in
     * ones, because enchantments are data-driven in this version: a datapack or
     * another server can have a different set.
     */
    static List<PickerScreen.Choice> enchantments() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        List<PickerScreen.Choice> built = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            Enchantment enchantment = holder.value();

            // An enchanted book shows the glint, which reads as "enchantment"
            // far faster than a plain book would.
            built.add(PickerScreen.Choice.of(configValue(id), enchantment.description(),
                    id + "  max " + enchantment.getMaxLevel(),
                    new ItemStack(Items.ENCHANTED_BOOK)));
        });

        built.sort(byLabel());
        return List.copyOf(built);
    }

    /**
     * The highest level the given enchantment allows, so the editor can keep a
     * chosen level sensible.
     *
     * @param configured an enchantment as written in the config
     * @return the maximum level, or 1 if the enchantment is unknown here
     */
    static int maxLevel(String configured) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 1;
        }

        ResourceLocation id = namespaced(configured);
        if (id == null) {
            return 1;
        }

        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(holder -> holder.key().location().equals(id))
                .findFirst()
                .map(holder -> holder.value().getMaxLevel())
                .orElse(1);
    }

    private static PickerScreen.Choice none(String label) {
        return PickerScreen.Choice.of("", Component.literal(label), "clears the field", ItemStack.EMPTY);
    }

    /**
     * A list of shop names, for an item that links to another shop.
     */
    static List<PickerScreen.Choice> shops(List<String> names) {
        List<PickerScreen.Choice> built = new ArrayList<>();
        built.add(none("Not a shortcut"));

        for (String name : names) {
            built.add(PickerScreen.Choice.of(name, Component.literal(name), "shop", ItemStack.EMPTY));
        }
        return List.copyOf(built);
    }

    /**
     * How a registry id is written into the config.
     * <p>
     * Vanilla entries keep the short upper-case spelling the existing configs
     * use, while anything modded carries its namespace, since that is the only
     * way to tell two mods' items apart.
     */
    private static String configValue(ResourceLocation id) {
        return VANILLA.equals(id.getNamespace())
                ? id.getPath().toUpperCase(Locale.ROOT)
                : id.toString();
    }

    /**
     * Mirrors the loader: an unqualified config name means vanilla.
     */
    private static ResourceLocation namespaced(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        String name = configured.trim().toLowerCase(Locale.ROOT);
        return ResourceLocation.tryParse(name.contains(":") ? name : VANILLA + ":" + name);
    }

    /**
     * Alphabetical by what the player reads, not by registry id, so searching
     * for a name lands somewhere predictable. The "none" entry sorts first
     * because its value is empty.
     */
    private static java.util.Comparator<PickerScreen.Choice> byLabel() {
        return java.util.Comparator
                .comparing((PickerScreen.Choice choice) -> choice.value().isEmpty() ? 0 : 1)
                .thenComparing(choice -> choice.label().getString());
    }
}
