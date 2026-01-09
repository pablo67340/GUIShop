package com.pablo67340.guishop.definition;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.PDCUtil;
import com.pablo67340.guishop.util.SkullCreator;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.FireworkEffect;
import org.bukkit.Color;


import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.pablo67340.guishop.util.StringUtil;

public final class Item implements ConfigurationSerializable {

    /**
     * The name of this {@link Item} when presented on the GUI.
     */
    @Getter
    @Setter
    private String shopName, buyName, shop, targetShop, name, skullUUID, NBT;

    @Getter
    @Setter
    private Integer slot, customModelData;

    @Getter
    @Setter
    private int configSlot;

    /**
     * The Material of this {@link Item}.
     */
    @Getter
    @Setter
    private String material;

    /**
     * The price to buy this {@link Item}.
     */
    @Getter
    @Setter
    private Object buyPrice;

    /**
     * The mob ID of this item if it's a spawner {@link Item}.
     */
    @Getter
    @Setter
    private String mobType;

    /**
     * The reason a resolution may has failed.
     */
    @Getter
    @Setter
    private String resolveReason;

    /**
     * The amount of money given when selling this {@link Item}.
     */
    @Getter
    @Setter
    private Object sellPrice;

    /**
     * Whether this item, specifically, uses dynamic pricing
     */
    @Getter
    @Setter
    private boolean useDynamicPricing;

    @Getter
    @Setter
    private QuantityValue quantityValue;

    @Getter
    private boolean resolveFailed = false;

    /**
     * The slot of this {@link Item} when presented on the GUI.
     */
    @Getter
    @Setter
    private ItemType itemType = ItemType.DUMMY;

    @Getter
    @Setter
    private List<String> buyLore, shopLore, lore, itemFlags;

    @Getter
    @Setter
    private List<String> commands;

    /**
     * The enchantsments on this {@link Item}.
     */
    @Getter
    @Setter
    private String[] enchantments;

    @Getter
    @Setter
    private PotionInfo potionInfo;

    @Getter
    @Setter
    private FireworkInfo fireworkInfo;

    @Getter
    @Setter
    private Permission permission;

    private static final String SPAWNER_MATERIAL = XMaterial.SPAWNER.parseMaterial().name();

    /**
     * Materials to which a potion type may be applied. <br>
     * This always has length 3, for normal potions, splash potions, and
     * lingering potions. <br>
     * <br>
     * None of the elements are null but the last can be empty.
     */
    private static final String[] POTION_MATERIALS;

    static {
        String potionName = XMaterial.POTION.parseMaterial().name();

        // splash potion is not separate on all versions
        Material splashPotionMaterial = XMaterial.SPLASH_POTION.parseMaterial();
        String splashPotionName = (splashPotionMaterial != null) ? splashPotionMaterial.name() : "";
        // lingering potion does not exist on all versions
        Material lingerPotionMaterial = XMaterial.LINGERING_POTION.parseMaterial();
        String lingerPotionName = (lingerPotionMaterial != null) ? lingerPotionMaterial.name() : "";

        POTION_MATERIALS = new String[]{potionName, splashPotionName, lingerPotionName};
    }

    public boolean hasShopName() {
        return (shopName != null) && !shopName.isEmpty();
    }

    public boolean hasName() {
        return (name != null) && !name.isEmpty();
    }

    public boolean hasBuyName() {
        return buyName != null;
    }

    public boolean hasTargetShop() {
        return targetShop != null;
    }

    public boolean hasShopLore() {
        return (shopLore != null) && !shopLore.isEmpty();
    }

    public boolean hasLore() {
        return (lore != null) && !lore.isEmpty();
    }

    public boolean hasBuyLore() {
        return (buyLore != null) && !buyLore.isEmpty();
    }

    public boolean hasPermission() {
        return permission != null;
    }

    public boolean hasEnchantments() {
        return (enchantments != null) && (enchantments.length != 0) && !enchantments[0].isEmpty();
    }

    public boolean hasCommands() {
        return (commands != null) && !commands.isEmpty();
    }

    /**
     * If this item has a potion. <br>
     * If is not a potion, this will always return <code>false</code>
     *
     * @return true if the item has a potion, false otherwise
     */
    public boolean hasPotion() {
        return potionInfo != null;
    }

    /**
     * If this item has firework info.
     *
     * @return true if the item has firework info, false otherwise
     */
    public boolean hasFirework() {
        return fireworkInfo != null;
    }

    /**
     * If the specified material is a potion, either a normal potion, splash
     * potion, or lingering potion.
     *
     * @param material the material
     * @return true if a potion, false otherwise
     */
    private static boolean isPotionMaterial(String material) {
        return (POTION_MATERIALS[0].equalsIgnoreCase(material) || POTION_MATERIALS[1].equalsIgnoreCase(material)
                || POTION_MATERIALS[2].equalsIgnoreCase(material));
    }

    /**
     * Whether this item's material is a potion, splash potion, or lingering
     * potion
     *
     * @return true if the material is some kind of potion, false otherwise
     */
    public boolean isAnyPotion() {
        return isPotionMaterial(material);
    }

    /**
     * Whether the item is a mob spawner
     *
     * @return if the item
     */
    public boolean isMobSpawner() {
        Optional<XMaterial> mat = XMaterial.matchXMaterial(material);
        return mat.orElse(null) == XMaterial.SPAWNER;
    }

    /**
     * Checks whether the item has a defined AND nonzero sell price. <br>
     * For the sell price to be defined it must be an integer or double.
     *
     * @return true if the sell price is valid, false otherwise
     */
    public boolean hasSellPrice() {
        // instanceof does the null-check for us
        return (sellPrice instanceof BigDecimal && ((BigDecimal) sellPrice).compareTo(BigDecimal.ZERO) > 0)
                || (sellPrice instanceof Integer && ((Integer) sellPrice) > 0);
    }

    /**
     * Checks whether the item has a defined buy price. <br>
     * For the buy price to be defined it must be an integer or double.
     *
     * @return true if the buy price is valid, false otherwise
     */
    public boolean hasBuyPrice() {
        // instanceof does the null-check for us
        return (buyPrice instanceof BigDecimal) || (buyPrice instanceof Integer);
    }

    /**
     * Checks whether the mob type is defined
     *
     * @return true if defined, false otherwise
     */
    public boolean hasMobType() {
        return (mobType != null) && !mobType.isEmpty();
    }

    /**
     * Assuming the buy price is an integer or a double, get it as a double.
     * Remember to check {@link #hasBuyPrice()} first
     *
     * @return the buy price as a double
     */
    public BigDecimal getBuyPriceAsDecimal() {
        return (buyPrice instanceof BigDecimal) ? (BigDecimal) buyPrice : BigDecimal.valueOf(((Integer) buyPrice).doubleValue());
    }

    /**
     * Assuming the sell price is an integer or a double, get it as a double.
     * Remember to check {@link #hasSellPrice()} first
     *
     * @return the sell price as a double
     */
    public BigDecimal getSellPriceAsDecimal() {
        return (sellPrice instanceof BigDecimal) ? (BigDecimal) sellPrice : BigDecimal.valueOf(((Integer) sellPrice).doubleValue());
    }

    /**
     * Assuming {@link #hasBuyPrice()} = <code>true</code>, calculate the buy
     * price taking based on the given quantity. <br>
     * If dynamic pricing is enabled, the DynamicPriceProvider is used for
     * calculations. Otherwise, the buy price and the quantity are simply
     * multiplied.
     *
     * @param quantity the quantity of the item
     * @return the calculated buy price
     */
    public BigDecimal calculateBuyPrice(int quantity) {
        // sell price must be defined and nonzero for dynamic pricing to work
        if (Config.isDynamicPricing() && isUseDynamicPricing() && hasSellPrice()) {
            return GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().calculateBuyPrice(getItemString(), quantity, getBuyPriceAsDecimal(),
                    getSellPriceAsDecimal());
        }
        // default to fixed pricing
        return getBuyPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Assumming {@link #hasSellPrice()} = <code>true</code>, calculate the sell
     * price taking based on the given quantity. <br>
     * If dynamic pricing is enabled, the DynamicPriceProvider is used for
     * calculations. Otherwise, the sell price and the quantity are simply
     * mmultiplied.
     *
     * @param quantity the quantity of the item
     * @return the calculated sell price
     */
    public BigDecimal calculateSellPrice(int quantity) {
        // buy price must be defined for dynamic pricing to work
        if (Config.isDynamicPricing() && isUseDynamicPricing() && hasBuyPrice()) {

            return GUIShop.getINSTANCE().getMiscUtils().getDYNAMICPRICING().calculateSellPrice(getItemString(), quantity, getBuyPriceAsDecimal(),
                    getSellPriceAsDecimal());
        }
        // default to fixed pricing
        return getSellPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Gets the lore display for this item's buy price. <br>
     * If there is no buy price, <code>Config.getCannotBuy()</code> is returned.
     * If free, <code>Config.getFreeLore</code> is returned. Otherwise, the buy
     * price is calculated based on the quantity, and the lore displaying the
     * calculated buy price is returned. Takes into account dynamic pricing, if
     * enabled.
     *
     * @param quantity the quantity of the item
     * @return the buy price lore
     */
    public String getBuyLore(int quantity) {
        if (hasBuyPrice()) {
            BigDecimal buyPriceAsDouble = getBuyPriceAsDecimal();
            if (buyPriceAsDouble.compareTo(BigDecimal.ZERO) > 0) {
                return Config.getLoreConfig().lores.get("buy").replace("%amount%",
                        GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
                                + GUIShop.getINSTANCE().getMiscUtils().economyFormat(calculateBuyPrice(quantity))
                                + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix"));
            }
            return Config.getLoreConfig().lores.get("free");
        }
        return Config.getLoreConfig().lores.get("cannot-buy");
    }

    /**
     * Gets the lore display for this item's sell price. <br>
     * If there is no sell price, <code>Config.getCannotSell()</code> is
     * returned. Otherwise, the sell price is calculated based on the quantity,
     * and the lore displaying the calculated sell price is returned. Takes into
     * account dynamic pricing, if enabled.
     *
     * @param quantity the quantity of the item
     * @return the sell price lore
     */
    public String getSellLore(int quantity) {
        if (hasSellPrice()) {
            return Config.getLoreConfig().lores.get("sell").replace("%amount%",
                    GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-prefix")
                            + GUIShop.getINSTANCE().getMiscUtils().economyFormat(calculateSellPrice(quantity))
                            + GUIShop.getINSTANCE().getConfigManager().getMessageSystem().translate("messages.currency-suffix"));
        }
        return Config.getLoreConfig().lores.get("cannot-sell");
    }

    /**
     * If the item is a mob spawner, <code>getMaterial().toUpperCase
     * + ":" + getMobType().toLowerCase()</code> is returned. Otherwise,
     * <code>getMaterial().toUpperCase</code> is simply returned.
     *
     * @return the item string representation
     */
    public String getItemString() {
        if (isMobSpawner()) {
            return material.toUpperCase() + ":spawner:" + getMobType().toLowerCase();
        }
        return material.toUpperCase();
    }

    /**
     * Checks if the item is a mob spawner, accounting for differences in server
     * versions.
     *
     * @param item the itemstack
     * @return whether the item is a mob spawner
     */
    public static boolean isSpawnerItem(ItemStack item) {
        return item.getType().name().equals(SPAWNER_MATERIAL);
    }

    /**
     * Equivalent of {@link Item#getItemString()} for an <i>ItemStack</i>, i.e.,
     * any minecraft item, not just a shop item. <br>
     * <br>
     * If the item is a mob spawner, <code>item.getType().toString().toUpperCase
     * + ":" + mobType.toString().toLowerCase()</code> is returned where
     * <i>mobtype</i> is the mob type of the mob spawner. Otherwise,
     * <code>getType().toString().toUpperCase</code> is simply returned.
     *
     * @param item the itemstack
     * @return the item string representation of the itemstack
     */
    public static String getItemStringForItemStack(ItemStack item) {
        if (isSpawnerItem(item)) {
            String mobType;
            // Check PDC for GUIShop spawner data
            String pdcMob = PDCUtil.getString(item, PDCUtil.KEY_SPAWNER_MOB);
            if (pdcMob != null) {
                mobType = pdcMob;
            } else {
                // Default to pig if no spawner type is set
                mobType = "PIG";
            }

            return item.getType().toString().toUpperCase() + ":spawner:" + mobType.toLowerCase();
        }
        return item.getType().toString().toUpperCase();
    }

    /**
     * Parses the mob type of this item if it is a spawner item. <br>
     * Remember to check {@link #isMobSpawner()}
     *
     * @return the entity type, or <code>null</code> if invalid
     */
    public EntityType parseMobSpawnerType() {
        @SuppressWarnings("deprecation")
        EntityType type = EntityType.fromName(getMobType());
        if (type != null) {
            return type;
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Failed to find entity type using EntityType#fromName");
        try {
            return EntityType.valueOf(getMobType());
        } catch (IllegalArgumentException ignored) {
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Failed to find entity type using EntityType#valueOf");
        return null;
    }


    public Boolean hasSkullUUID() {
        return this.skullUUID != null;
    }

    public Boolean hasCustomModelID() {
        return this.customModelData != null;
    }

    public Boolean hasItemFlags() {
        return (itemFlags != null) && !itemFlags.isEmpty();
    }

    public Boolean hasNBT() {
        return NBT != null;
    }

    /**
     * Resolves the PotionType from PotionInfo, handling the 1.20.5+ naming changes.
     * In 1.20.5+, extended/upgraded potions are separate types (e.g., LONG_SWIFTNESS, STRONG_SWIFTNESS).
     * This method also maps old names to new names (e.g., SPEED → SWIFTNESS).
     */
    private static PotionType resolvePotionType(PotionInfo info) {
        if (info == null || info.getType() == null) {
            return null;
        }

        String typeName = info.getType().toUpperCase();
        
        // Map old potion names to new 1.20.5+ names
        switch (typeName) {
            case "SPEED": typeName = "SWIFTNESS"; break;
            case "INSTANT_HEAL": typeName = "HEALING"; break;
            case "INSTANT_DAMAGE": typeName = "HARMING"; break;
            case "JUMP": typeName = "LEAPING"; break;
            case "REGEN": typeName = "REGENERATION"; break;
        }

        // Build the full type name with prefix for extended/upgraded variants
        String fullTypeName = typeName;
        if (info.getExtended() != null && info.getExtended()) {
            fullTypeName = "LONG_" + typeName;
        } else if (info.getUpgraded() != null && info.getUpgraded()) {
            fullTypeName = "STRONG_" + typeName;
        }

        // Try to find the potion type
        try {
            return PotionType.valueOf(fullTypeName);
        } catch (IllegalArgumentException e) {
            // If the prefixed version doesn't exist, try the base name
            try {
                return PotionType.valueOf(typeName);
            } catch (IllegalArgumentException e2) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Could not resolve potion type: " + info.getType() + 
                    " (tried: " + fullTypeName + ", " + typeName + ")");
                return null;
            }
        }
    }

    /**
     * Converts a shape string to a FireworkEffect.Type.
     *
     * @param shape the shape string (small_ball, large_ball, star, creeper, burst)
     * @return the corresponding FireworkEffect.Type
     */
    private static FireworkEffect.Type getFireworkEffectType(String shape) {
        if (shape == null) {
            return FireworkEffect.Type.BALL;
        }
        switch (shape.toLowerCase()) {
            case "small_ball":
            case "ball":
            case "0":
                return FireworkEffect.Type.BALL;
            case "large_ball":
            case "ball_large":
            case "1":
                return FireworkEffect.Type.BALL_LARGE;
            case "star":
            case "2":
                return FireworkEffect.Type.STAR;
            case "creeper":
            case "3":
                return FireworkEffect.Type.CREEPER;
            case "burst":
            case "4":
                return FireworkEffect.Type.BURST;
            default:
                return FireworkEffect.Type.BALL;
        }
    }

    public static Item parse(ItemStack itemStack, Integer slot, String shop) {
        Item item = new Item();

        if (itemStack != null) {
            GUIShop.getINSTANCE().getLogUtil().debugLog("Loading item from ItemStack: " + itemStack.getType());
            item.setMaterial(itemStack.getType().toString());
            item.setSlot(slot);
            item.setShop(shop);
            
            // Read all PDC data
            String itemType = PDCUtil.getString(itemStack, PDCUtil.KEY_ITEM_TYPE);
            GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: Read itemType from PDC = " + itemType);
            if (itemType != null) {
                item.setItemType(ItemType.valueOf(itemType));
                GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: Set itemType to " + item.getItemType());
            } else {
                GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: itemType is null, defaulting to " + item.getItemType());
            }

            String permission = PDCUtil.getString(itemStack, PDCUtil.KEY_PERMISSION);
            if (permission != null) {
                item.setPermission(new Permission(permission));
            }

            Double buyPrice = PDCUtil.getDouble(itemStack, PDCUtil.KEY_BUY_PRICE);
            GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: Read buyPrice from PDC = " + buyPrice);
            if (buyPrice != null) {
                item.setBuyPrice(BigDecimal.valueOf(buyPrice));
                GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: Set buyPrice to " + item.getBuyPriceAsDecimal());
            }

            Double sellPrice = PDCUtil.getDouble(itemStack, PDCUtil.KEY_SELL_PRICE);
            GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: Read sellPrice from PDC = " + sellPrice);
            if (sellPrice != null) {
                item.setSellPrice(BigDecimal.valueOf(sellPrice));
                GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM PARSE: Set sellPrice to " + item.getSellPriceAsDecimal());
            }

            String shopName = PDCUtil.getString(itemStack, PDCUtil.KEY_SHOP_NAME);
            if (shopName != null) {
                item.setShopName(shopName);
            }

            String targetShop = PDCUtil.getString(itemStack, PDCUtil.KEY_TARGET_SHOP);
            if (targetShop != null) {
                item.setTargetShop(targetShop);
            }

            String buyName = PDCUtil.getString(itemStack, PDCUtil.KEY_BUY_NAME);
            if (buyName != null) {
                item.setBuyName(buyName);
            }

            String name = PDCUtil.getString(itemStack, PDCUtil.KEY_NAME);
            if (name != null) {
                item.setName(name);
            }

            String enchantments = PDCUtil.getString(itemStack, PDCUtil.KEY_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                // Handle both comma-separated (PDC format) and space-separated (config format)
                // Also remove any trailing commas or spaces
                String cleaned = enchantments.replaceAll("[,\\s]+$", "");
                String[] parts;
                if (cleaned.contains(",")) {
                    parts = cleaned.split(",");
                } else {
                    parts = cleaned.split(" ");
                }
                // Filter out any empty strings
                List<String> validEnchants = new ArrayList<>();
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        validEnchants.add(trimmed);
                    }
                }
                if (!validEnchants.isEmpty()) {
                    item.setEnchantments(validEnchants.toArray(new String[0]));
            }
            }

            String commands = PDCUtil.getString(itemStack, PDCUtil.KEY_COMMANDS);
            if (commands != null) {
                item.setItemType(ItemType.COMMAND);
                item.setCommands(Arrays.asList(commands.split("::")));
            }

            String potion = PDCUtil.getString(itemStack, PDCUtil.KEY_POTION);
            if (potion != null) {
                String[] splitInfo = potion.split("::");
                item.setPotionInfo(
                        new PotionInfo(splitInfo[0], Boolean.parseBoolean(splitInfo[1]), Boolean.parseBoolean(splitInfo[2]), Boolean.parseBoolean(splitInfo[3])));
            }

            Integer quantity = PDCUtil.getInteger(itemStack, PDCUtil.KEY_QUANTITY);
            if (quantity != null) {
                item.setQuantityValue(new QuantityValue().setQuantity(quantity));
            }

            String skullUUID = PDCUtil.getString(itemStack, PDCUtil.KEY_SKULL_UUID);
            if (skullUUID != null) {
                item.setSkullUUID(skullUUID);
            }

            String mobType = PDCUtil.getString(itemStack, PDCUtil.KEY_MOB_TYPE);
            if (mobType != null) {
                item.setMobType(mobType);
            }

            String buyLoreLines = PDCUtil.getString(itemStack, PDCUtil.KEY_BUY_LORE_LINES);
            if (buyLoreLines != null) {
                String[] parsedLore = buyLoreLines.split("::");
                item.setBuyLore(Arrays.asList(parsedLore));
            }

            String shopLoreLines = PDCUtil.getString(itemStack, PDCUtil.KEY_SHOP_LORE_LINES);
            if (shopLoreLines != null) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Item had shop lore " + shopLoreLines);
                String[] parsedLore = shopLoreLines.split("::");
                item.setShopLore(Arrays.asList(parsedLore));
            }

            String loreLines = PDCUtil.getString(itemStack, PDCUtil.KEY_LORE_LINES);
            if (loreLines != null) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("Item had lore " + loreLines);
                String[] parsedLore = loreLines.split("::");
                item.setLore(Arrays.asList(parsedLore));
            }

            String customNBT = PDCUtil.getString(itemStack, PDCUtil.KEY_CUSTOM_NBT);
            if (customNBT != null) {
                item.setNBT(customNBT);
            }
        }
        return item;
    }

    public ItemStack toItemStack(Player player, boolean isMenu) {
        ItemStack itemStack = null;

        try {
            itemStack = XMaterial.matchXMaterial(getMaterial()).get().parseItem();
        } catch (NoSuchElementException | NullPointerException exception) {
            setResolveFailed("Item has invalid material");
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Adding item to slot: " + getSlot());
        if (itemStack == null || isResolveFailed()) {
            GUIShop.getINSTANCE().getLogUtil().log("Item: " + getMaterial() + " could not be resolved (invalid material). Are you using an old server version?");
            setResolveFailed("Item has invalid material");
            return getErrorStack();
        }

        if (itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial() && hasSkullUUID()) {
            itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(getSkullUUID()), getSkullUUID());
        }

        // Checks if an item is either a shop item or command item. This also handles
        // Null items as there is an item type switch in the lines above.
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (getItemType() != ItemType.DUMMY) {
            if (itemMeta == null) {
                GUIShop.getINSTANCE().getLogUtil().log("Item: " + getMaterial() + " could not be resolved (null meta).");
                setResolveFailed("Item has invalid item meta");
                return getErrorStack();
            }

            List<String> itemLore = new ArrayList<>();

            if (!isMenu) {
                itemLore.add(getBuyLore(1));
                itemLore.add(getSellLore(1));
            }

            if (player != null) {
                if (!GUIShop.getCREATOR().contains(player.getUniqueId())) {
                    if (hasShopName() && !isMenu) {
                        itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getShopName(), player, this));
                    } else if (hasName()) {
                        itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getName(), player, this));
                    } else if (isMobSpawner() && !isMenu) {
                        String mobName = getMobType();
                        mobName = mobName.toLowerCase();
                        mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
                        itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(GUIShop.getINSTANCE().getConfigManager().getMainConfig().getString("spawner-name").replace("%type%", mobName), player, this));
                    }
                    if (hasShopLore() && !isMenu) {
                        getShopLore().forEach(str -> itemLore.add(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(str, player, this)));
                    } else if (hasLore() && isMenu) {
                        getLore().forEach(str -> itemLore.add(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(str, player, this)));
                    }
                    itemMeta.setLore(itemLore);
                    itemStack.setItemMeta(itemMeta);
                } else {
                    itemLore.add(Config.getLoreConfig().lores.get("type").replace("%type%", getItemType().toString()));
                    if (hasShopName()) {
                        itemLore.add(Config.getLoreConfig().lores.get("shop-name").replace("%name%", getShopName()));
                    }
                    if (hasName()) {
                        itemLore.add(Config.getLoreConfig().lores.get("name").replace("%name%", getName()));
                    }
                    if (hasMobType()) {
                        itemLore.add(Config.getLoreConfig().lores.get("mob-type").replace("%type%", getMobType()));
                    }
                    if (hasBuyName()) {
                        itemLore.add(Config.getLoreConfig().lores.get("buy-name").replace("%name%", getName()));
                    }
                    if (hasBuyLore()) {
                        itemLore.add(Config.getLoreConfig().lores.get("buy-lore"));
                        getBuyLore().forEach(str -> itemLore.add(ChatColor.translateAlternateColorCodes('&', str)));
                    }
                    if (hasShopLore()) {
                        itemLore.add(Config.getLoreConfig().lores.get("shop-lore"));
                        getShopLore().forEach(str -> itemLore.add(ChatColor.translateAlternateColorCodes('&', str)));
                    }
                    if (hasLore()) {
                        itemLore.add(Config.getLoreConfig().lores.get("lore"));
                        getLore().forEach(str -> itemLore.add(ChatColor.translateAlternateColorCodes('&', str)));
                    }
                    if (hasCommands()) {
                        itemLore.add(Config.getLoreConfig().lores.get("commands"));
                        getCommands().forEach(str -> {
                            if (str.length() > 20) {
                                String s = ChatColor.translateAlternateColorCodes('&', "/" + str);
                                s = s.substring(0, 20);
                                itemLore.add(s + "...");
                            } else {
                                itemLore.add("/" + str);
                            }
                        });
                    }
                    if (hasEnchantments()) {
                        StringBuilder encLore = new StringBuilder();
                        for (String str : getEnchantments()) {
                            encLore.append(str).append(" ");
                        }
                        itemLore.add(Config.getLoreConfig().lores.get("enchantments").replace("%enchantments%", encLore));
                    }
                    if (hasTargetShop()) {
                        itemLore.add(Config.getLoreConfig().lores.get("target-shop").replace("%shop%", getTargetShop()));
                    }
                    if (hasPotion()) {
                        String infoString = potionInfo.getType() + " " + potionInfo.getSplash() + " " + potionInfo.getExtended() + " " + potionInfo.getUpgraded();
                        itemLore.add(Config.getLoreConfig().lores.get("potion-info").replace("%info%", infoString));
                    }
                    if (hasNBT()) {
                        itemLore.add(Config.getLoreConfig().lores.get("nbt").replace("%nbt%", getNBT()));
                    }
                    if (hasSkullUUID()) {
                        itemLore.add(Config.getLoreConfig().lores.get("skull-uuid").replace("%uuid%", getSkullUUID()));
                    }
                    if (!itemLore.isEmpty()) {
                        itemMeta.setLore(itemLore);
                    }
                    if (hasPermission()) {
                        itemLore.add(Config.getLoreConfig().lores.get("permission").replace("%permission%", getPermission().getPermission()));
                    }
                    if (getQuantityValue() != null) {
                        itemLore.add(Config.getLoreConfig().lores.get("quantity").replace("%quantity%", (getQuantityValue().getQuantity() == -1 ? true : getQuantityValue().getQuantity() == 1 ? false : getQuantityValue().getQuantity()).toString()));
                    }

                    itemStack.setItemMeta(itemMeta);
                    
                    // Store all data in PDC
                    if (hasBuyPrice()) {
                        PDCUtil.setDouble(itemStack, PDCUtil.KEY_BUY_PRICE, getBuyPriceAsDecimal().doubleValue());
                    }
                    if (hasSellPrice()) {
                        PDCUtil.setDouble(itemStack, PDCUtil.KEY_SELL_PRICE, getSellPriceAsDecimal().doubleValue());
                    }
                    if (hasBuyName()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_BUY_NAME, getBuyName());
                    }
                    if (hasShopName()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_SHOP_NAME, getShopName());
                    }
                    if (hasName()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_NAME, getName());
                    }
                    if (hasMobType()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_MOB_TYPE, getMobType());
                    }
                    if (hasEnchantments()) {
                        StringBuilder itemEnchantments = new StringBuilder();
                        for (String str : getEnchantments()) {
                            itemEnchantments.append(str).append(",");
                        }
                        PDCUtil.setString(itemStack, PDCUtil.KEY_ENCHANTMENTS, itemEnchantments.toString());
                    }
                    if (hasShopLore()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_SHOP_LORE_LINES, String.join("::", getShopLore()));
                    }
                    if (hasBuyLore()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_BUY_LORE_LINES, String.join("::", getBuyLore()));
                    }
                    if (hasLore()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_LORE_LINES, String.join("::", getLore()));
                    }
                    if (hasCommands()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_COMMANDS, String.join("::", getCommands()));
                    }
                    if (hasSkullUUID()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_SKULL_UUID, getSkullUUID());
                    }
                    if (getQuantityValue() != null) {
                        PDCUtil.setInteger(itemStack, PDCUtil.KEY_QUANTITY, getQuantityValue().getQuantity());
                    }
                    if (hasPotion()) {
                        String[] values = {getPotionInfo().getType(), getPotionInfo().getSplash().toString(), getPotionInfo().getExtended().toString(), getPotionInfo().getUpgraded().toString()};
                        PDCUtil.setString(itemStack, PDCUtil.KEY_POTION, String.join("::", values));
                    }
                    if (hasPermission()) {
                        PDCUtil.setString(itemStack, PDCUtil.KEY_PERMISSION, getPermission().getPermission());
                    }
                    PDCUtil.setString(itemStack, PDCUtil.KEY_ITEM_TYPE, getItemType().toString());
                }
            }

            if (hasItemFlags()) {
                for (String flag : itemFlags) {
                    try {
                        itemMeta.addItemFlags(ItemFlag.valueOf(flag));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            if (hasCustomModelID()) {
                itemMeta.setCustomModelData(getCustomModelData());
            }

            if (hasEnchantments()) {
                if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                    for (String enc : getEnchantments()) {
                        try {
                            String enchantment = StringUtil.substringBefore(enc, ":");
                            String level = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                            if (!level.isEmpty()) {
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                    }
                        } catch (NoSuchElementException | NumberFormatException e) {
                            GUIShop.getINSTANCE().getLogUtil().log("&cSkipping malformed enchantment: " + enc);
                        }
                    }
                    itemStack.setItemMeta(meta);
                } else {
                    for (String enc : getEnchantments()) {
                        try {
                            String enchantment = StringUtil.substringBefore(enc, ":");
                            String level = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                            if (!level.isEmpty()) {
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                    }
                        } catch (NoSuchElementException | NumberFormatException e) {
                            GUIShop.getINSTANCE().getLogUtil().log("&cSkipping malformed enchantment: " + enc);
                        }
                    }
                    itemStack.setItemMeta(itemMeta);
                }
            }

            itemStack.setItemMeta(itemMeta);

            if (hasNBT()) {
                // Store custom-nbt value in PDC for reference
                // Note: Raw NBT application is deprecated. Use other config options instead.
                PDCUtil.setString(itemStack, PDCUtil.KEY_CUSTOM_NBT, getNBT());
                GUIShop.getINSTANCE().getLogUtil().debugLog("custom-nbt stored for item: " + getMaterial() + 
                    ". Note: Raw NBT application is deprecated. Consider using other config options.");
            }

            if (hasPotion()) {
                PotionInfo potionInfo = getPotionInfo();

                if (potionInfo.getSplash()) {
                    itemStack = new ItemStack(Material.SPLASH_POTION);
                    itemStack.setItemMeta(itemMeta);
                }

                PotionMeta pMeta = (PotionMeta) itemStack.getItemMeta();
                PotionType potionType = resolvePotionType(potionInfo);
                if (potionType != null) {
                    pMeta.setBasePotionType(potionType);
                    itemStack.setItemMeta(pMeta);
                } else {
                    GUIShop.getINSTANCE().getLogUtil().log("Warning: Could not resolve potion type: " + potionInfo.getType());
                }
            }
        } else {
            // DUMMY items (menu items) - apply name, enchantments, item-flags, custom model data
            if (hasName()) {
                itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getName(), player, this));
            } else if (hasShopName()) {
                itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getShopName(), player, this));
            }
            
            // Apply lore for menu items
            if (hasLore()) {
                List<String> itemLore = new ArrayList<>();
                getLore().forEach(str -> itemLore.add(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(str, player, this)));
                itemMeta.setLore(itemLore);
            }
            
            // Apply item flags for menu items
            if (hasItemFlags()) {
                for (String flag : itemFlags) {
                    try {
                        itemMeta.addItemFlags(ItemFlag.valueOf(flag));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            
            // Apply custom model data for menu items
            if (hasCustomModelID()) {
                itemMeta.setCustomModelData(getCustomModelData());
            }
            
            // Apply enchantments for menu items
            if (hasEnchantments()) {
                if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                    for (String enc : getEnchantments()) {
                        try {
                            String enchantment = StringUtil.substringBefore(enc, ":");
                            String level = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                            if (!level.isEmpty()) {
                            meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                            }
                        } catch (NoSuchElementException | NumberFormatException ignored) {
                        }
                    }
                    itemStack.setItemMeta(meta);
                } else {
                    for (String enc : getEnchantments()) {
                        try {
                            String enchantment = StringUtil.substringBefore(enc, ":");
                            String level = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                            if (!level.isEmpty()) {
                            itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                            }
                        } catch (NoSuchElementException | NumberFormatException ignored) {
                        }
                    }
                    itemStack.setItemMeta(itemMeta);
                }
            } else {
                itemStack.setItemMeta(itemMeta);
            }
        }

        // Apply firework info if present
        if (hasFirework() && itemStack.getType() == Material.FIREWORK_ROCKET) {
            FireworkMeta fMeta = (FireworkMeta) itemStack.getItemMeta();
            fMeta.setPower(fireworkInfo.getFlight());
            
            for (FireworkInfo.ExplosionInfo explosion : fireworkInfo.getExplosions()) {
                FireworkEffect.Builder effectBuilder = FireworkEffect.builder();
                
                // Set shape
                FireworkEffect.Type effectType = getFireworkEffectType(explosion.getShape());
                effectBuilder.with(effectType);
                
                // Set colors
                for (Integer colorInt : explosion.getColors()) {
                    effectBuilder.withColor(Color.fromRGB(colorInt));
                }
                
                // Set fade colors
                for (Integer fadeColorInt : explosion.getFadeColors()) {
                    effectBuilder.withFade(Color.fromRGB(fadeColorInt));
                }
                
                // Set flicker and trail
                effectBuilder.flicker(explosion.isHasFlicker());
                effectBuilder.trail(explosion.isHasTrail());
                
                fMeta.addEffect(effectBuilder.build());
            }
            
            itemStack.setItemMeta(fMeta);
        }

        // Store all data in PDC for ALL items (not just creator mode)
        if (hasBuyPrice()) {
            PDCUtil.setDouble(itemStack, PDCUtil.KEY_BUY_PRICE, getBuyPriceAsDecimal().doubleValue());
        }
        if (hasSellPrice()) {
            PDCUtil.setDouble(itemStack, PDCUtil.KEY_SELL_PRICE, getSellPriceAsDecimal().doubleValue());
        }
        if (hasBuyName()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_BUY_NAME, getBuyName());
        }
        if (hasShopName()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_SHOP_NAME, getShopName());
        }
        if (hasName()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_NAME, getName());
        }
        if (hasMobType()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_MOB_TYPE, getMobType());
        }
        if (isMobSpawner() && hasMobType()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_SPAWNER_MOB, getMobType());
        }
        if (hasEnchantments()) {
            StringBuilder itemEnchantments = new StringBuilder();
            for (String str : getEnchantments()) {
                itemEnchantments.append(str).append(",");
            }
            PDCUtil.setString(itemStack, PDCUtil.KEY_ENCHANTMENTS, itemEnchantments.toString());
        }
        if (hasShopLore()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_SHOP_LORE_LINES, String.join("::", getShopLore()));
        }
        if (hasBuyLore()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_BUY_LORE_LINES, String.join("::", getBuyLore()));
        }
        if (hasLore()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_LORE_LINES, String.join("::", getLore()));
        }
        if (hasCommands()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_COMMANDS, String.join("::", getCommands()));
        }
        if (hasSkullUUID()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_SKULL_UUID, getSkullUUID());
        }
        if (getQuantityValue() != null) {
            PDCUtil.setInteger(itemStack, PDCUtil.KEY_QUANTITY, getQuantityValue().getQuantity());
        }
        if (hasPotion()) {
            String[] values = {getPotionInfo().getType(), getPotionInfo().getSplash().toString(), getPotionInfo().getExtended().toString(), getPotionInfo().getUpgraded().toString()};
            PDCUtil.setString(itemStack, PDCUtil.KEY_POTION, String.join("::", values));
        }
        if (hasPermission()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_PERMISSION, getPermission().getPermission());
        }
        if (hasTargetShop()) {
            PDCUtil.setString(itemStack, PDCUtil.KEY_TARGET_SHOP, getTargetShop());
        }
        PDCUtil.setString(itemStack, PDCUtil.KEY_ITEM_TYPE, getItemType().toString());

        // Create Page
        GUIShop.getINSTANCE().getLogUtil().debugLog("Setting item to slot: " + getSlot());

        GUIShop.getINSTANCE().getLogUtil().debugLog("ITEMSTACK: "+itemStack.toString());

        return itemStack;
    }

    public boolean isItemFromItemStack(ItemStack input) {
        if (!hasPotion()) {
            if (input.getType() != XMaterial.matchXMaterial(getMaterial()).get().parseMaterial()) {
                return false;
            }
        } else {
            if (input.getType() != XMaterial.matchXMaterial(getMaterial()).get().parseMaterial() && input.getType() != XMaterial.matchXMaterial("SPLASH_POTION").get().parseMaterial()) {
                return false;
            }
        }
        if (hasEnchantments()) {
            if (input.getType() != XMaterial.matchXMaterial("ENCHANTED_BOOK").get().parseMaterial()) {
                for (String enc : getEnchantments()) {
                    try {
                        String enchantment = StringUtil.substringBefore(enc, ":");
                        String levelStr = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                        if (levelStr.isEmpty()) continue;
                        Integer level = Integer.parseInt(levelStr);
                    Enchantment targetEnchantment = XEnchantment.matchXEnchantment(enchantment).get().getEnchant();
                    if (!input.getEnchantments().containsKey(targetEnchantment)) {
                        return false;
                    } else {
                        if (!input.getEnchantments().get(targetEnchantment).equals(level)) {
                            return false;
                        }
                        }
                    } catch (NoSuchElementException | NumberFormatException ignored) {
                        // Skip malformed enchantment entries
                    }
                }
            } else {
                ItemMeta itemMeta = input.getItemMeta();
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                for (String enc : getEnchantments()) {
                    try {
                        String enchantment = StringUtil.substringBefore(enc, ":");
                        String levelStr = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                        if (levelStr.isEmpty()) continue;
                        Integer level = Integer.parseInt(levelStr);
                    Enchantment targetEnchantment = XEnchantment.matchXEnchantment(enchantment).get().getEnchant();
                    if (!meta.getStoredEnchants().containsKey(targetEnchantment)) {
                        return false;
                    } else {
                        if (!meta.getStoredEnchants().get(targetEnchantment).equals(level)) {
                            return false;
                        }
                        }
                    } catch (NoSuchElementException | NumberFormatException ignored) {
                        // Skip malformed enchantment entries
                    }
                }
            }
        }
        if (hasPotion()) {
            PotionMeta pm = (PotionMeta) input.getItemMeta();
            PotionType inputType = pm.getBasePotionType();
            PotionType expectedType = resolvePotionType(getPotionInfo());
            
            if (inputType == null || expectedType == null) {
                return false;
            }
            if (inputType != expectedType) {
                return false;
            }
        }
        if (hasNBT()) {
            // Check if input item has the same custom-nbt stored in PDC
            String inputNBT = PDCUtil.getString(input, PDCUtil.KEY_CUSTOM_NBT);
            if (inputNBT == null || !inputNBT.equals(getNBT())) {
                        return false;
            }
        }
        if (hasSkullUUID() && Config.isSellSkullUUID()) {
            SkullMeta sm = (SkullMeta) input.getItemMeta();
            if (sm.getOwningPlayer() == null || !sm.getOwningPlayer().getUniqueId().toString().equals(skullUUID)) {
                return false;
            }
        }
        if (isMobSpawner()) {
            String inputMobType = PDCUtil.getString(input, PDCUtil.KEY_SPAWNER_MOB);
            return inputMobType != null && inputMobType.equals(mobType);
        }
        return true;
    }

    public ItemStack toBuyItemStack(int quantity, Player player, Shop currentShop) {
        ItemStack itemStack = null;

        try {
            itemStack = XMaterial.matchXMaterial(getMaterial()).get().parseItem();
            itemStack.setAmount(quantity);
        } catch (NoSuchElementException ex) {
            setResolveFailed("Item has invalid material");
        }

        if (hasSkullUUID() && itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial()) {
            itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(getSkullUUID()), getSkullUUID());
        }

        if (hasPotion()) {
            PotionInfo pi = getPotionInfo();

            if (pi.getSplash()) {
                itemStack = new ItemStack(Material.SPLASH_POTION);
            }
            PotionMeta pm = (PotionMeta) itemStack.getItemMeta();
            PotionType potionType = resolvePotionType(pi);
            if (potionType != null) {
                pm.setBasePotionType(potionType);
                itemStack.setItemMeta(pm);
            } else {
                GUIShop.getINSTANCE().getLogUtil().log("Warning: Could not resolve potion type: " + pi.getType());
            }
        }

        // Apply firework info if present
        if (hasFirework() && itemStack.getType() == Material.FIREWORK_ROCKET) {
            FireworkMeta fMeta = (FireworkMeta) itemStack.getItemMeta();
            fMeta.setPower(fireworkInfo.getFlight());
            
            for (FireworkInfo.ExplosionInfo explosion : fireworkInfo.getExplosions()) {
                FireworkEffect.Builder effectBuilder = FireworkEffect.builder();
                
                // Set shape
                FireworkEffect.Type effectType = getFireworkEffectType(explosion.getShape());
                effectBuilder.with(effectType);
                
                // Set colors
                for (Integer colorInt : explosion.getColors()) {
                    effectBuilder.withColor(Color.fromRGB(colorInt));
                }
                
                // Set fade colors
                for (Integer fadeColorInt : explosion.getFadeColors()) {
                    effectBuilder.withFade(Color.fromRGB(fadeColorInt));
                }
                
                // Set flicker and trail
                effectBuilder.flicker(explosion.isHasFlicker());
                effectBuilder.trail(explosion.isHasTrail());
                
                fMeta.addEffect(effectBuilder.build());
            }
            
            itemStack.setItemMeta(fMeta);
        }

        ItemMeta itemMeta = itemStack.getItemMeta();

        List<String> itemLore = new ArrayList<>();
        if (hasBuyLore()) {
            getBuyLore().forEach(str -> {
                itemLore.add(ChatColor.translateAlternateColorCodes('&', GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(str, player, this)));
            });
        }

        itemMeta.setLore(itemLore);

        if (hasBuyName()) {
            itemMeta.setDisplayName(
                    ChatColor.translateAlternateColorCodes('&', GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getBuyName(), player, this)));
        } else if (Item.isSpawnerItem(itemStack)) {
            String mobName = getMobType();
            mobName = mobName.toLowerCase();
            mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
            itemMeta.setDisplayName(mobName + " Spawner");
        }

        if (hasCustomModelID()) {
            itemMeta.setCustomModelData(getCustomModelData());
        }

        if (hasEnchantments()) {
            if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                for (String enc : getEnchantments()) {
                    try {
                        String enchantment = StringUtil.substringBefore(enc, ":");
                        String level = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                        if (!level.isEmpty()) {
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                    }
                    } catch (NoSuchElementException | NullPointerException | NumberFormatException ignored) {
                }
                }
                itemStack.setItemMeta(meta);
            } else {
                for (String enc : getEnchantments()) {
                    try {
                        String enchantment = StringUtil.substringBefore(enc, ":");
                        String level = StringUtil.substringAfter(enc, ":").replaceAll("[^0-9]", "");
                        if (!level.isEmpty()) {
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                    }
                    } catch (NoSuchElementException | NullPointerException | NumberFormatException ignored) {
                }
                }
                itemStack.setItemMeta(itemMeta);
            }
        } else {
            itemStack.setItemMeta(itemMeta);
        }
        if (isMobSpawner()) {
            EntityType type = parseMobSpawnerType();
            if (type == null) {
                GUIShop.getINSTANCE().getLogUtil().log("Invalid mob spawner entity type: " + getMobType() + " in Shop: " + currentShop.getShop());
            } else {
                String entityValue = type.name();
                GUIShop.getINSTANCE().getLogUtil().debugLog("Attaching " + entityValue + " to purchased spawner.");

                PDCUtil.setString(itemStack, PDCUtil.KEY_SPAWNER_MOB, entityValue);
            }
        }
        if (hasNBT()) {
            // Store custom-nbt value in PDC for reference (needed for matching custom NBT items)
            PDCUtil.setString(itemStack, PDCUtil.KEY_CUSTOM_NBT, getNBT());
        }
        
        // NOTE: We intentionally do NOT store buy/sell prices, commands, or item type in PDC
        // on purchased items. This allows bought items to stack with vanilla items after
        // placing and breaking. The selling system uses ITEMTABLE lookup by material type,
        // so it doesn't need PDC data. Only spawners (mob type) and custom NBT items need PDC.
        
        return itemStack;
    }

    /**
     * Gets the buyPrice of an item using PDC, or <code>null</code> if not
     * defined
     *
     * @param item ItemStack to get the BuyPrice on
     * @return Buy Price as BigDecimal
     */
    public static BigDecimal getBuyPrice(ItemStack item) {
        Double price = PDCUtil.getDouble(item, PDCUtil.KEY_BUY_PRICE);
        return price != null ? BigDecimal.valueOf(price) : null;
    }

    /**
     * Gets the sellPrice of an item using PDC, or <code>null</code> if not
     * defined
     *
     * @param item ItemStack to get the SellPrice of
     * @return Sell Price as BigDecimal
     */
    public static BigDecimal getSellPrice(ItemStack item) {
        Double price = PDCUtil.getDouble(item, PDCUtil.KEY_SELL_PRICE);
        return price != null ? BigDecimal.valueOf(price) : null;
    }

    public static Item deserialize(Map<String, Object> serialized, Integer slot, String shop) {
        GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Loading from config at slot " + slot + " in " + shop);
        GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Raw config data = " + serialized);
        Item item = new Item();
        item.setSlot(slot);
        item.setShop(shop);
        for (Entry<String, Object> entry : serialized.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("type")) {
                item.setItemType(ItemType.valueOf((String) entry.getValue()));
            } else if (entry.getKey().equalsIgnoreCase("id")) {
                item.setMaterial((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("shop-name")) {
                item.setShopName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("quantity")) {
                QuantityValue quantityValue = new QuantityValue();

                if (entry.getValue() instanceof Integer integer) {
                    quantityValue.setQuantity(integer);
                } else {
                    String quantity = entry.getKey();

                    try {
                        Integer.parseInt(quantity);
                    } catch (NumberFormatException exception) {
                        quantityValue.setDisabled(!Boolean.parseBoolean(quantity));
                        continue;
                    }

                    quantityValue.setQuantity(Integer.parseInt(quantity));
                }

                item.setQuantityValue(quantityValue);
            } else if (entry.getKey().equalsIgnoreCase("name")) {
                item.setName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-name")) {
                item.setBuyName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("skull-uuid")) {
                item.setSkullUUID((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("shop-lore")) {
                if (entry.getValue() instanceof List) {
                    item.setShopLore((List<String>) entry.getValue());
                } else {
                    item.setShopLore(Arrays.asList(((String) entry.getValue()).split("\n")));
                }
            } else if (entry.getKey().equalsIgnoreCase("buy-lore")) {
                if (entry.getValue() instanceof List) {
                    item.setBuyLore((List<String>) entry.getValue());
                } else {
                    item.setBuyLore(Arrays.asList(((String) entry.getValue()).split("\n")));
                }
            } else if (entry.getKey().equalsIgnoreCase("item-flags")) {
                item.setItemFlags(Arrays.stream(((String) entry.getValue()).split(" ")).filter(flag -> {
                    try {
                        ItemFlag.valueOf(flag);
                        return true;
                    } catch (NullPointerException | IllegalArgumentException exception) {
                        GUIShop.getINSTANCE().getLogUtil().log("&cInvalid item flag found: " + flag + "&c! Skipping enchantment.");
                        return false;
                    }
                }).collect(Collectors.toList()));
            } else if (entry.getKey().equalsIgnoreCase("lore")) {
                if (entry.getValue() instanceof List) {
                    item.setLore((List<String>) entry.getValue());
                } else {
                    item.setLore(Arrays.asList(((String) entry.getValue()).split("\n")));
                }
            } else if (entry.getKey().equalsIgnoreCase("buy-price")) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: buy-price raw value = " + entry.getValue() + " (class=" + entry.getValue().getClass().getSimpleName() + ")");
                if (entry.getValue() instanceof Double buyPrice) {
                    BigDecimal buyPrice2 = BigDecimal.valueOf(buyPrice);
                    item.setBuyPrice(buyPrice2);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Set buyPrice (from Double) = " + buyPrice2);
                } else if (entry.getValue() instanceof Integer) {
                    item.setBuyPrice(entry.getValue());
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Set buyPrice (from Integer) = " + entry.getValue());
                }
            } else if (entry.getKey().equalsIgnoreCase("sell-price")) {
                GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: sell-price raw value = " + entry.getValue() + " (class=" + entry.getValue().getClass().getSimpleName() + ")");
                if (entry.getValue() instanceof Double sellPrice) {
                    BigDecimal sellPrice2 = BigDecimal.valueOf(sellPrice);
                    item.setSellPrice(sellPrice2);
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Set sellPrice (from Double) = " + sellPrice2);
                } else if (entry.getValue() instanceof Integer) {
                    item.setSellPrice(entry.getValue());
                    GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Set sellPrice (from Integer) = " + entry.getValue());
                }
            } else if (entry.getKey().equalsIgnoreCase("commands")) {
                item.setItemType(ItemType.COMMAND);
                if (entry.getValue() instanceof List) {
                    item.setCommands((List<String>) entry.getValue());
                } else {
                    item.setCommands(Collections.singletonList(entry.getValue().toString()));
                }
            } else if (entry.getKey().equalsIgnoreCase("target-shop")) {
                item.setItemType(ItemType.SHOP_SHORTCUT);
                item.setTargetShop(entry.getValue().toString());
            } else if (entry.getKey().equalsIgnoreCase("enchantments")) {
                // Clean up trailing commas/spaces and handle both comma and space separators
                String enchantStr = ((String) entry.getValue()).replaceAll("[,\\s]+$", "").trim();
                String[] parts;
                if (enchantStr.contains(",")) {
                    parts = enchantStr.split(",");
                } else {
                    parts = enchantStr.split(" ");
                }
                item.setEnchantments(Arrays.stream(parts)
                    .map(String::trim)
                    .filter(enchant -> !enchant.isEmpty())
                    .filter(enchant -> {
                    try {
                            // Extract just the enchantment name (before the colon) for validation
                            String enchantmentName = StringUtil.substringBefore(enchant, ":");
                            XEnchantment.matchXEnchantment(enchantmentName).get().getEnchant();
                        return true;
                    } catch (NoSuchElementException | NullPointerException exception) {
                        GUIShop.getINSTANCE().getLogUtil().log("&cInvalid enchantment found: " + enchant + "&c! Skipping enchantment.");
                        return false;
                    }
                }).toArray(String[]::new));
            } else if (entry.getKey().equalsIgnoreCase("custom-nbt")) {
                if (entry.getValue() instanceof List) {
                    item.setNBT(String.join("", (List<String>) entry.getValue()));
                } else {
                    item.setNBT(entry.getValue().toString());
                }
            } else if (entry.getKey().equalsIgnoreCase("mob-type")) {
                item.setMobType(entry.getValue().toString());
            } else if (entry.getKey().equalsIgnoreCase("potion-info")) {
                ConfigurationSection section = (ConfigurationSection) entry.getValue();
                Map<String, Object> potionInfo = section.getValues(true);
                item.setPotionInfo(new PotionInfo(
                        potionInfo.get("type") != null ? potionInfo.get("type").toString() : "FIRE_RESISTANCE",
                        potionInfo.get("splash") != null && Boolean.parseBoolean(potionInfo.get("splash").toString()),
                        potionInfo.get("extended") != null && Boolean.parseBoolean(potionInfo.get("extended").toString()),
                        potionInfo.get("upgraded") != null && Boolean.parseBoolean(potionInfo.get("upgraded").toString())));
            } else if (entry.getKey().equalsIgnoreCase("firework-info")) {
                ConfigurationSection section = (ConfigurationSection) entry.getValue();
                Map<String, Object> fireworkData = section.getValues(true);
                
                int flight = fireworkData.get("flight") != null ? Integer.parseInt(fireworkData.get("flight").toString()) : 1;
                List<FireworkInfo.ExplosionInfo> explosions = new ArrayList<>();
                
                if (fireworkData.get("explosions") instanceof List) {
                    List<?> explosionList = (List<?>) fireworkData.get("explosions");
                    for (Object explosionObj : explosionList) {
                        if (explosionObj instanceof Map) {
                            Map<String, Object> expMap = (Map<String, Object>) explosionObj;
                            String shape = expMap.get("shape") != null ? expMap.get("shape").toString() : "small_ball";
                            boolean hasFlicker = expMap.get("flicker") != null && Boolean.parseBoolean(expMap.get("flicker").toString());
                            boolean hasTrail = expMap.get("trail") != null && Boolean.parseBoolean(expMap.get("trail").toString());
                            
                            List<Integer> colors = new ArrayList<>();
                            if (expMap.get("colors") instanceof List) {
                                for (Object c : (List<?>) expMap.get("colors")) {
                                    colors.add(Integer.parseInt(c.toString()));
                                }
                            }
                            
                            List<Integer> fadeColors = new ArrayList<>();
                            if (expMap.get("fade-colors") instanceof List) {
                                for (Object c : (List<?>) expMap.get("fade-colors")) {
                                    fadeColors.add(Integer.parseInt(c.toString()));
                                }
                            }
                            
                            explosions.add(new FireworkInfo.ExplosionInfo(shape, colors, fadeColors, hasFlicker, hasTrail));
                        }
                    }
                }
                
                item.setFireworkInfo(new FireworkInfo(flight, explosions));
            } else if (entry.getKey().equalsIgnoreCase("permission")) {
                item.setPermission(new Permission(entry.getValue().toString()));
            } else if (entry.getKey().equalsIgnoreCase("custom-model-data")) {
                try {
                    item.setCustomModelData(Integer.parseInt(entry.getValue().toString()));
                } catch (NumberFormatException exception) {
                    GUIShop.getINSTANCE().getLogUtil().log("Item in " + (shop != null ? "shop " + shop : "menu") + " and slot " + slot + " has an invalid custom model data!");
                }
            }
        }
        
        // If item type is still DUMMY but has buy/sell prices, default to SHOP
        // This handles items in config that don't have an explicit type: SHOP line
        if (item.getItemType() == ItemType.DUMMY && (item.hasBuyPrice() || item.hasSellPrice())) {
            item.setItemType(ItemType.SHOP);
            GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM DESERIALIZE: Defaulted itemType to SHOP (has prices)");
        }
        
        return item;
    }

    @Override
    public @NotNull
    Map<String, Object> serialize() {
        GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM SERIALIZE: itemType=" + itemType + " hasBuyPrice=" + hasBuyPrice() + " hasSellPrice=" + hasSellPrice());
        GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM SERIALIZE: buyPrice field=" + buyPrice + " (class=" + (buyPrice != null ? buyPrice.getClass().getSimpleName() : "null") + ")");
        GUIShop.getINSTANCE().getLogUtil().debugLog("ITEM SERIALIZE: sellPrice field=" + sellPrice + " (class=" + (sellPrice != null ? sellPrice.getClass().getSimpleName() : "null") + ")");
        
        Map<String, Object> serialized = new HashMap<>();
        if (itemType != ItemType.DUMMY) {
            serialized.put("type", itemType.toString());
        }
        serialized.put("id", material);
        
        // For DUMMY items, use 'name' field (default to space if not set), don't use shop-name/buy-name
        if (itemType == ItemType.DUMMY) {
            // Use actual name if set, otherwise default to a space (prevents vanilla item name showing)
            serialized.put("name", hasName() ? name : " ");
        } else {
            // SHOP/COMMAND/etc items use shop-name and buy-name, NOT the generic name field
        if (hasShopName()) {
            serialized.put("shop-name", shopName);
        }
        if (hasBuyName()) {
            serialized.put("buy-name", buyName);
            }
            // Don't serialize 'name' for SHOP items - let vanilla item name show if no shop-name set
        }
        if (hasSkullUUID()) {
            serialized.put("skull-uuid", skullUUID);
        }
        if (hasShopLore()) {
            serialized.put("shop-lore", shopLore);
        }
        if (hasBuyLore()) {
            serialized.put("buy-lore", buyLore);
        }
        if (hasItemFlags()) {
            serialized.put("item-flags", String.join(" ", itemFlags));
        }
        if (hasLore()) {
            serialized.put("lore", lore);
        }
        if (getQuantityValue() != null) {
            serialized.put("quantity", getQuantityValue().getQuantity() == -1 ? true : getQuantityValue().getQuantity() == 1 ? false : getQuantityValue().getQuantity());
        }
        if (hasBuyPrice()) {
            serialized.put("buy-price", ((BigDecimal) buyPrice).doubleValue());
        }
        if (hasSellPrice()) {
            serialized.put("sell-price", ((BigDecimal) sellPrice).doubleValue());
        }
        if (hasCommands()) {
            serialized.put("commands", commands);
        }
        if (hasTargetShop()) {
            serialized.put("target-shop", targetShop);
        }
        if (hasEnchantments()) {
            StringBuilder parsed = new StringBuilder();
            for (String str : enchantments) {
                parsed.append(str).append(" ");
            }
            serialized.put("enchantments", parsed.toString());
        }
        if (hasNBT()) {
            serialized.put("custom-nbt", NBT);
        }
        if (hasMobType()) {
            serialized.put("mob-type", mobType);
        }
        if (hasPotion()) {
            Map<String, Object> pInfo = new HashMap<>();
            pInfo.put("type", this.potionInfo.getType());
            pInfo.put("splash", this.potionInfo.getSplash());
            pInfo.put("extended", this.potionInfo.getExtended());
            pInfo.put("upgraded", this.potionInfo.getUpgraded());
            serialized.put("potion-info", pInfo);
        }
        if (hasFirework()) {
            Map<String, Object> fInfo = new HashMap<>();
            fInfo.put("flight", this.fireworkInfo.getFlight());
            List<Map<String, Object>> explosionsList = new ArrayList<>();
            for (FireworkInfo.ExplosionInfo exp : this.fireworkInfo.getExplosions()) {
                Map<String, Object> expMap = new HashMap<>();
                expMap.put("shape", exp.getShape());
                expMap.put("colors", exp.getColors());
                expMap.put("fade-colors", exp.getFadeColors());
                expMap.put("flicker", exp.isHasFlicker());
                expMap.put("trail", exp.isHasTrail());
                explosionsList.add(expMap);
            }
            fInfo.put("explosions", explosionsList);
            serialized.put("firework-info", fInfo);
        }
        if (hasPermission()) {
            serialized.put("permission", getPermission().getPermission());
        }
        if (hasCustomModelID()) {
            serialized.put("custom-model-data", getCustomModelData());
        }

        return serialized;
    }

    public void setResolveFailed(String reason) {
        resolveFailed = true;
        resolveReason = reason;
    }

    public ItemStack getErrorStack() {
        ItemStack itemStack = new ItemStack(Material.BARRIER);
        ItemMeta im = itemStack.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c" + resolveReason));
        itemStack.setItemMeta(im);
        return itemStack;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Buy Name: ");
        builder.append(this.getBuyName());
        builder.append(" Buy Price: ");
        builder.append(this.buyPrice);
        builder.append(" Config Slot: ");
        builder.append(this.configSlot);
        builder.append(" Custom Model Data: ");
        builder.append(this.customModelData);
        builder.append(" Item Type: ");
        builder.append(this.itemType);
        builder.append(" Material: ");
        builder.append(this.material);
        builder.append(" MobType: ");
        builder.append(this.mobType);
        builder.append(" Name: ");
        builder.append(this.name);
        builder.append(" Resolve Failed: ");
        builder.append(this.resolveFailed);
        builder.append(" Shop Name: ");
        builder.append(this.shopName);
        builder.append(" Skull UUID: ");
        builder.append(this.skullUUID);
        builder.append(" Slot: ");
        builder.append(this.slot);
        builder.append(" Target Shop: ");
        builder.append(this.targetShop);
        return builder.toString();
    }
}
