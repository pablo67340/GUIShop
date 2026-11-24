package com.pablo67340.guishop.neoforge.definition;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pablo67340.guishop.definition.FireworkInfo;
import com.pablo67340.guishop.definition.PotionInfo;
import com.pablo67340.guishop.neoforge.util.TextUtil;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds the {@link ItemStack} a configured shop item is displayed and sold as.
 * <p>
 * Kept apart from {@link NeoItem} so the item stays a plain data holder and all
 * the config-to-component translation lives in one place.
 */
public final class ItemStackFactory {

    private ItemStackFactory() {
    }

    /**
     * @param registries needed because enchantments are a datapack registry and
     *                   cannot be looked up without the server's registry access
     */
    public static ItemStack create(NeoItem item, HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(resolveItem(item));
        if (stack.isEmpty()) {
            return stack;
        }

        applyNbt(item, stack);
        applyNameAndLore(item, stack);
        applyEnchantments(item, stack, registries);
        applyPotion(item, stack);
        applyFirework(item, stack);
        applySkull(item, stack);
        applyCustomModelData(item, stack);
        applySpawner(item, stack);
        // Last, because the hide flags act on the components applied above.
        applyItemFlags(item, stack);

        return stack;
    }

    // ------------------------------------------------------------- resolution

    /**
     * Resolves the configured material, honouring the potion variant flags so
     * {@code splash: true} yields a splash potion as the config documents.
     */
    private static net.minecraft.world.item.Item resolveItem(NeoItem item) {
        net.minecraft.world.item.Item resolved = item.resolveItem();

        PotionInfo potion = item.getPotionInfo();
        if (potion == null || resolved == Items.AIR) {
            return resolved;
        }

        boolean isPotion = resolved == Items.POTION
                || resolved == Items.SPLASH_POTION
                || resolved == Items.LINGERING_POTION;
        if (!isPotion) {
            return resolved;
        }

        if (Boolean.TRUE.equals(potion.getLingering())) {
            return Items.LINGERING_POTION;
        }
        if (Boolean.TRUE.equals(potion.getSplash())) {
            return Items.SPLASH_POTION;
        }
        return resolved;
    }

    // ------------------------------------------------------------- components

    private static void applyNameAndLore(NeoItem item, ItemStack stack) {
        String displayName = item.hasShopName() ? item.getShopName() : item.getName();
        if (displayName != null && !displayName.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, TextUtil.toComponent(displayName));
        }

        List<String> configured = item.hasShopLore() ? item.getShopLore()
                : item.hasLore() ? item.getLore() : null;
        if (configured == null) {
            return;
        }

        List<Component> lore = new ArrayList<>(configured.size());
        configured.forEach(line -> lore.add(TextUtil.toComponent(line)));
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }

    private static void applyEnchantments(NeoItem item, ItemStack stack, HolderLookup.Provider registries) {
        String[] configured = item.getEnchantments();
        if (configured == null || configured.length == 0) {
            return;
        }

        HolderLookup.RegistryLookup<Enchantment> lookup =
                registries.lookup(Registries.ENCHANTMENT).orElse(null);
        if (lookup == null) {
            warn("Cannot apply enchantments without the enchantment registry.");
            return;
        }

        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));

        for (String entry : configured) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            // The config format is NAME:LEVEL, with the level optional.
            String[] parts = entry.split(":");
            String name = parts[0].trim();

            int level = 1;
            if (parts.length > 1) {
                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException exception) {
                    warn("Enchantment '" + entry + "' on " + describe(item) + " has a non-numeric level.");
                    continue;
                }
            }

            ResourceLocation key = VanillaNames.enchantment(name);
            Optional<Holder.Reference<Enchantment>> enchantment = key == null
                    ? Optional.empty()
                    : lookup.get(ResourceKey.create(Registries.ENCHANTMENT, key));

            if (enchantment.isEmpty()) {
                warn("Unknown enchantment '" + name + "' on " + describe(item) + ".");
                continue;
            }

            enchantments.set(enchantment.get(), level);
        }

        stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
    }

    private static void applyPotion(NeoItem item, ItemStack stack) {
        PotionInfo info = item.getPotionInfo();
        if (info == null) {
            return;
        }

        ResourceLocation key = VanillaNames.potion(info);
        if (key == null) {
            warn("Potion type '" + info.getType() + "' on " + describe(item) + " is unusable.");
            return;
        }

        Optional<Holder.Reference<Potion>> potion =
                BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, key));
        if (potion.isEmpty()) {
            warn("No potion registered as '" + key + "' for " + describe(item) + ".");
            return;
        }

        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion.get()));
    }

    private static void applyFirework(NeoItem item, ItemStack stack) {
        FireworkInfo info = item.getFireworkInfo();
        if (info == null) {
            return;
        }

        List<FireworkExplosion> explosions = new ArrayList<>();
        for (FireworkInfo.ExplosionInfo explosion : info.getExplosions()) {
            explosions.add(new FireworkExplosion(
                    VanillaNames.explosionShape(explosion.getShape()),
                    toIntList(explosion.getColors()),
                    toIntList(explosion.getFadeColors()),
                    explosion.isHasTrail(),
                    explosion.isHasFlicker()));
        }

        stack.set(DataComponents.FIREWORKS, new Fireworks(info.getFlight(), explosions));
    }

    private static void applySkull(NeoItem item, ItemStack stack) {
        if (!item.hasSkullUUID()) {
            return;
        }

        String value = item.getSkullUUID().trim();
        try {
            // A plain UUID names an existing player; anything else is a base64
            // texture, which has to travel as a profile property instead.
            stack.set(DataComponents.PROFILE, new ResolvableProfile(
                    new GameProfile(UUID.fromString(value), "")));
        } catch (IllegalArgumentException notAUuid) {
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(value.getBytes()), "");
            profile.getProperties().put("textures", new Property("textures", value));
            stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        }
    }

    private static void applyCustomModelData(NeoItem item, ItemStack stack) {
        if (item.hasCustomModelID()) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(item.getCustomModelData()));
        }
    }

    /**
     * Spawners carry their mob in block entity data rather than on the item, so
     * the configured mob survives being placed.
     */
    private static void applySpawner(NeoItem item, ItemStack stack) {
        if (!item.hasMobType() || stack.getItem() != Items.SPAWNER) {
            return;
        }

        ResourceLocation entity = namespaced(item.getMobType());
        if (entity == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entity)) {
            warn("Unknown mob type '" + item.getMobType() + "' on " + describe(item) + ".");
            return;
        }

        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", entity.toString());

        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entityTag);

        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putString("id", "minecraft:mob_spawner");
        blockEntity.put("SpawnData", spawnData);

        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntity));
    }

    /**
     * Raw NBT is applied as custom data.
     * <p>
     * Bukkit NBT keys do not map onto this version's item components, so this
     * preserves the data rather than reinterpreting it. Applied first so the
     * configured components below take precedence.
     */
    private static void applyNbt(NeoItem item, ItemStack stack) {
        if (!item.hasNBT()) {
            return;
        }

        try {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(TagParser.parseTag(item.getNBT())));
        } catch (CommandSyntaxException | RuntimeException exception) {
            warn("Could not parse nbt on " + describe(item) + ": " + exception.getMessage());
        }
    }

    /**
     * Maps the Bukkit item flags onto this version's equivalents, which are
     * per-component tooltip switches rather than one flag set.
     */
    private static void applyItemFlags(NeoItem item, ItemStack stack) {
        if (!item.hasItemFlags()) {
            return;
        }

        for (String flag : item.getItemFlags()) {
            switch (flag.trim().toUpperCase(Locale.ROOT)) {
                case "HIDE_ENCHANTS" -> stack.set(DataComponents.ENCHANTMENTS,
                        stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                                .withTooltip(false));

                case "HIDE_ATTRIBUTES" -> stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                                .withTooltip(false));

                case "HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP", "HIDE_ITEM_SPECIFICS" ->
                        stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

                case "HIDE_DYE" -> {
                    DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
                    if (dyed != null) {
                        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(dyed.rgb(), false));
                    }
                }

                case "HIDE_UNBREAKABLE" -> {
                    if (stack.has(DataComponents.UNBREAKABLE)) {
                        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
                    }
                }

                default -> GuiShopPlatform.get().logUtil().debugLog("Item flag '" + flag + "' on "
                        + describe(item) + " has no equivalent on this version.");
            }
        }
    }

    // -------------------------------------------------------------- utilities

    /**
     * Adds the vanilla namespace when the config omits it, while leaving an
     * explicit one alone so modded entries work.
     */
    static ResourceLocation namespaced(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        String name = configured.trim().toLowerCase(Locale.ROOT);
        return ResourceLocation.tryParse(name.contains(":") ? name : "minecraft:" + name);
    }

    private static IntList toIntList(List<Integer> colors) {
        return colors == null || colors.isEmpty() ? IntList.of() : new IntArrayList(colors);
    }

    private static String describe(NeoItem item) {
        return (item.getShop() == null ? "menu" : "shop " + item.getShop()) + " slot " + item.getSlot();
    }

    private static void warn(String message) {
        GuiShopPlatform.get().logger().warning(message);
    }
}
