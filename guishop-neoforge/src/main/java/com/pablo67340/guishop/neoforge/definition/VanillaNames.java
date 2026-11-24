package com.pablo67340.guishop.neoforge.definition;

import com.pablo67340.guishop.definition.PotionInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.FireworkExplosion;

import java.util.Locale;
import java.util.Map;

/**
 * Translates the names used in GUIShop's configs into vanilla registry ids.
 * <p>
 * Most names already match once lowercased, so these tables only carry the ones
 * that genuinely differ. Both the modern and the legacy Bukkit spellings are
 * accepted, because configs written over the years contain both.
 */
public final class VanillaNames {

    /**
     * Legacy Bukkit enchantment names, which were based on the old NMS
     * identifiers rather than the in-game names.
     */
    private static final Map<String, String> ENCHANTMENT_ALIASES = Map.ofEntries(
            Map.entry("damage_all", "sharpness"),
            Map.entry("damage_undead", "smite"),
            Map.entry("damage_arthropods", "bane_of_arthropods"),
            Map.entry("dig_speed", "efficiency"),
            Map.entry("durability", "unbreaking"),
            Map.entry("loot_bonus_blocks", "fortune"),
            Map.entry("loot_bonus_mobs", "looting"),
            Map.entry("arrow_damage", "power"),
            Map.entry("arrow_knockback", "punch"),
            Map.entry("arrow_fire", "flame"),
            Map.entry("arrow_infinite", "infinity"),
            Map.entry("protection_environmental", "protection"),
            Map.entry("protection_fire", "fire_protection"),
            Map.entry("protection_fall", "feather_falling"),
            Map.entry("protection_explosions", "blast_protection"),
            Map.entry("protection_projectile", "projectile_protection"),
            Map.entry("oxygen", "respiration"),
            Map.entry("water_worker", "aqua_affinity"),
            Map.entry("sweeping", "sweeping_edge"),
            Map.entry("luck", "luck_of_the_sea"),
            Map.entry("fishing_speed", "lure"));

    /**
     * Bukkit potion type names that differ from the vanilla potion ids.
     */
    private static final Map<String, String> POTION_ALIASES = Map.ofEntries(
            Map.entry("speed", "swiftness"),
            Map.entry("jump", "leaping"),
            Map.entry("instant_heal", "healing"),
            Map.entry("instant_health", "healing"),
            Map.entry("instant_damage", "harming"),
            Map.entry("regen", "regeneration"));

    private VanillaNames() {
    }

    /**
     * @param configured an enchantment name such as {@code SHARPNESS} or the
     *                   legacy {@code DAMAGE_ALL}
     * @return the registry id, or null if the name is unusable
     */
    public static ResourceLocation enchantment(String configured) {
        return resolve(configured, ENCHANTMENT_ALIASES);
    }

    /**
     * Builds the potion id, applying the duration and strength variants the
     * same way vanilla names them.
     *
     * @return the registry id, or null if the type is unusable
     */
    public static ResourceLocation potion(PotionInfo info) {
        ResourceLocation base = resolve(info.getType(), POTION_ALIASES);
        if (base == null) {
            return null;
        }

        // Vanilla has no combined long+strong variant, so strength wins, which
        // is also what Bukkit does when both are set.
        String prefix = "";
        if (Boolean.TRUE.equals(info.getUpgraded())) {
            prefix = "strong_";
        } else if (Boolean.TRUE.equals(info.getExtended())) {
            prefix = "long_";
        }

        if (prefix.isEmpty()) {
            return base;
        }
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), prefix + base.getPath());
    }

    /**
     * Firework explosion shapes. The config already uses vanilla-style names
     * such as {@code small_ball}, but older files use the Bukkit spellings.
     */
    public static FireworkExplosion.Shape explosionShape(String configured) {
        if (configured == null) {
            return FireworkExplosion.Shape.SMALL_BALL;
        }

        return switch (configured.trim().toLowerCase(Locale.ROOT)) {
            case "large_ball", "ball_large" -> FireworkExplosion.Shape.LARGE_BALL;
            case "star", "star_shaped" -> FireworkExplosion.Shape.STAR;
            case "creeper", "creeper_shaped" -> FireworkExplosion.Shape.CREEPER;
            case "burst" -> FireworkExplosion.Shape.BURST;
            default -> FireworkExplosion.Shape.SMALL_BALL;
        };
    }

    /**
     * Normalises a configured name to a registry id, accepting an explicit
     * namespace so modded entries work.
     */
    private static ResourceLocation resolve(String configured, Map<String, String> aliases) {
        if (configured == null || configured.isBlank()) {
            return null;
        }

        String name = configured.trim().toLowerCase(Locale.ROOT);
        if (name.contains(":")) {
            return ResourceLocation.tryParse(name);
        }

        return ResourceLocation.tryParse("minecraft:" + aliases.getOrDefault(name, name));
    }
}
