package com.pablo67340.guishop.definition;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.MatLib;
import com.pablo67340.guishop.util.MatLib.MatLibEntry;
import com.pablo67340.guishop.util.XMaterial;
import com.pablo67340.guishop.util.XPotion;

import space.arim.legacyitemconstructor.LegacyItemConstructor;

import lombok.Getter;
import lombok.Setter;

public final class Item {

    /**
     * The name of this {@link Item} when presented on the GUI.
     */
    @Getter
    @Setter
    private String shopName, buyName;

    @Getter
    @Setter
    private int slot;

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

    /**
     * The slot of this {@link Item} when presented on the GUI.
     */
    @Getter
    @Setter
    private ItemType itemType;

    @Getter
    @Setter
    private List<String> buyLore, shopLore;

    @Getter
    @Setter
    private List<String> commands;

    /**
     * The enchantsments on this {@link Item}.
     */
    @Getter
    @Setter
    private String[] enchantments;

    /**
     * The preparsed potion if this item is a potion
     *
     */
    @Getter
    private Potion potion;

    private static final String SPAWNER_MATERIAL = XMaterial.SPAWNER.parseMaterial().name();

    /**
     * Materials to which a potion type may be applied. <br>
     * This always has length 3, for normal potions, splash potions, and
     * lingering potions. <br>
     * <br>
     * None of the elements are null but the last can be empty.
     *
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

    public boolean hasBuyName() {
        return buyName != null;
    }

    public boolean hasShopLore() {
        return (shopLore != null) && !shopLore.isEmpty();
    }

    public boolean hasBuyLore() {
        return (buyLore != null) && !buyLore.isEmpty();
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
        return potion != null;
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
     * Sets and parses the potion type of the item. <br>
     * Remember to call {@link #isAnyPotion()} first
     *
     * @param type the potion type for the item from the shops.yml
     * @param splash whether the potion is a splash potion
     * @param extended whether the potion has extended duration
     * @param level the tier/amplifier of the potion
     */
    @SuppressWarnings("deprecation")
    public void parsePotionType(String type, boolean splash, boolean extended, int level) {
        if (type != null) {
            XPotion xpotion = XPotion.matchXPotion(type).orElse(null);
            if (xpotion != null) {
                PotionEffectType effectType = xpotion.parsePotionEffectType();
                if (effectType != null) {
                    for (PotionType vanillaType : PotionType.values()) {
                        if (vanillaType.getEffectType() != null && vanillaType.getEffectType().equals(effectType)) {
                            potion = new Potion(vanillaType);
                            potion.setSplash(splash);
                            if (!vanillaType.isInstant()) {
                                potion.setHasExtendedDuration(extended);
                            }
                            if (level > 0 && level <= vanillaType.getMaxLevel()) {
                                potion.setLevel(level);
                            }
                            return;
                        }
                    }
                }
            }
            Main.log("Invalid potion info: {type=" + type + ",splash=" + splash + ",extended=" + extended + ",level=" + level + "}");
        }
    }

    /**
     * Whether the item is a mob spawner
     *
     * @return if the item
     */
    public boolean isMobSpawner() {
        if (material.equalsIgnoreCase(SPAWNER_MATERIAL)) {
            return true;
        }
        XMaterial spawnerXMaterial = XMaterial.SPAWNER;
        return material.equalsIgnoreCase(spawnerXMaterial.name()) || spawnerXMaterial.anyMatchLegacy(material);
    }

    /**
     * Checks whether the item has a defined AND nonzero sell price. <br>
     * For the sell price to be defined it must be an integer or double.
     *
     * @return true if the sell price is valid, false otherwise
     */
    public boolean hasSellPrice() {
        // instanceof does the null-check for us
        return (sellPrice instanceof Double && ((Double) sellPrice) != 0D)
                || (sellPrice instanceof Integer && ((Integer) sellPrice) != 0);
    }

    /**
     * Checks whether the item has a defined buy price. <br>
     * For the buy price to be defined it must be an integer or double.
     *
     * @return true if the buy price is valid, false otherwise
     */
    public boolean hasBuyPrice() {
        // instanceof does the null-check for us
        return (buyPrice instanceof Double) || (buyPrice instanceof Integer);
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
    public double getBuyPriceAsDouble() {
        return (buyPrice instanceof Double) ? (Double) buyPrice : ((Integer) buyPrice).doubleValue();
    }

    /**
     * Assuming the sell price is an integer or a double, get it as a double.
     * Remember to check {@link #hasSellPrice()} first
     *
     * @return the sell price as a double
     */
    public double getSellPriceAsDouble() {
        return (sellPrice instanceof Double) ? (Double) sellPrice : ((Integer) sellPrice).doubleValue();
    }

    /**
     * Assumming {@link #hasBuyPrice()} = <code>true</code>, calculate the buy
     * price taking based on the given quantity. <br>
     * If dynamic pricing is enabled, the DynamicPriceProvider is used for
     * calculations. Otherwise, the buy price and the quantity are simply
     * multiplied.
     *
     * @param quantity the quantity of the item
     * @return the calculated buy price
     */
    public double calculateBuyPrice(int quantity) {
        // sell price must be defined and nonzero for dynamic pricing to work
        if (Config.isDynamicPricing() && isUseDynamicPricing() && hasSellPrice()) {

            return Main.getDYNAMICPRICING().calculateBuyPrice(getItemString(), quantity, getBuyPriceAsDouble(),
                    getSellPriceAsDouble());
        }
        // default to fixed pricing
        return getBuyPriceAsDouble() * quantity;
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
    public double calculateSellPrice(int quantity) {
        // buy price must be defined for dynamic pricing to work
        if (Config.isDynamicPricing() && isUseDynamicPricing() && hasBuyPrice()) {

            return Main.getDYNAMICPRICING().calculateSellPrice(getItemString(), quantity, getBuyPriceAsDouble(),
                    getSellPriceAsDouble());
        }
        // default to fixed pricing
        return getSellPriceAsDouble() * quantity;
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

            double buyPriceAsDouble = getBuyPriceAsDouble();
            if (buyPriceAsDouble != 0) {

                return Config.getBuyLore().replace("{amount}",
                        Config.getCurrency() + Main.economyFormat(calculateBuyPrice(quantity)) + Config.getCurrencySuffix());
            }
            return Config.getFreeLore();
        }
        return Config.getCannotBuy();
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
            return Config.getSellLore().replace("{amount}",
                    Config.getCurrency() + Main.economyFormat(calculateSellPrice(quantity)) + Config.getCurrencySuffix());
        }
        return Config.getCannotSell();
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
            NBTTagCompound cmp = ItemNBTUtil.getTag(item);
            if (cmp.hasKey("GUIShopSpawner")) {
                mobType = cmp.getString("GUIShopSpawner");

            } else if (cmp.hasKey("BlockEntityTag")) {
                NBTTagCompound subCmp = (NBTTagCompound) cmp.get("BlockEntityTag");
                mobType = subCmp.getString("EntityId").toUpperCase();

            } else {
                // default to pig
                mobType = "PIG";
            }

            return item.getType().toString().toUpperCase() + ":spawner:" + mobType.toLowerCase();

        }
        return item.getType().toString().toUpperCase();
    }

    /**
     * Parses the material of this Item. <br>
     * If the material cannot be resolved, <code>null</code> should be returned.
     * <br>
     * <br>
     * This operation is somewhat resource intensive. Consider running
     * asynchronously. (Keep in mind thread safety of course)
     *
     * @return a gui item using the appropriate itemstack
     */
    public GuiItem parseMaterial() {

        GuiItem gItem;

        /*
	* First attempt
	* Use XMaterial
        */
        XMaterial xmaterial = XMaterial.matchXMaterial(getMaterial()).orElse(null);
        ItemStack itemStack = (xmaterial != null) ? xmaterial.parseItem() : null;

        if (itemStack != null && itemStack.getItemMeta() != null) { // If underlying itemstack cannot be resolved (is null or null meta), fail immediately
            try {
                gItem = new GuiItem(itemStack);
                return gItem; // if itemStack is nonnull and no exception thrown, attempt has succeeded
            } catch (Exception ex2) {
                if (Config.isDebugMode()) {
                    Main.debugLog("Error parsing material: "+ex2.getMessage());
                }
            }
        }

        Main.debugLog("Failed to find item by Material: " + getMaterial() + ". Attempting workarounds...");
        /*
	* Second attempt
	* "OFF"/"ON" fix
        */
        if (getMaterial().endsWith("_ON")) { // Only use OFF fix if _ON is detected

            try {
                // remove the "_ON" and add "_OFF"
                itemStack = new ItemStack(Material.valueOf(getMaterial().substring(0, getMaterial().length() - 2) + "OFF"));
                gItem = new GuiItem(itemStack);
                return gItem; // if no exception thrown, attempt has succeeded
            } catch (Exception ex3) {
                if (Config.isDebugMode()) {
                    Main.debugLog("Error parsing item attempt 3: "+ex3.getMessage());
                }
            }

            Main.debugLog("OFF Fix for: " + getMaterial() + " Failed. Attempting ItemID Lookup...");
        }

        /*
	* Third attempt
	* Using MatLib
        */
        // Final Stand, lets try to find this user's item
        MatLibEntry itemInfo = MatLib.getMAP().get(getMaterial());
        if (itemInfo != null) {

            int id = itemInfo.getId();
            short data = itemInfo.getData();

            try {

                itemStack = LegacyItemConstructor.invoke(id, 1, data); // can never be null

                gItem = new GuiItem(itemStack);
                return gItem;
            } catch (Exception ex4) {
                if (Config.isDebugMode()) {
                    Main.debugLog("Error parsing item attemp 4: "+ex4.getMessage());
                }
            } catch (NoSuchMethodError nsme) {
                // For servers missing the legacy constructor https://pastebin.com/GDy2ih9s
            }
        }

        Main.debugLog("ItemID Fix for: " + getMaterial() + " Failed. Falling back to air.");

        setItemType(ItemType.BLANK);
        setEnchantments(null);
        // null indicates failure
        return null;
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

        Main.debugLog("Failed to find entity type using EntityType#fromName");
        try {
            return EntityType.valueOf(getMobType());
        } catch (IllegalArgumentException ignored) {
        }

        Main.debugLog("Failed to find entity type using EntityType#valueOf");
        return null;
    }

    /**
     * Renames a GuiItem
     *
     * @param gItem the gui item
     * @param name the new name
     * @return an updated gui item
     */
    public static GuiItem renameGuiItem(GuiItem gItem, String name) {
        ItemStack item = gItem.getItem().clone();
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(name);
        item.setItemMeta(itemMeta);
        return new GuiItem(item);
    }

}
