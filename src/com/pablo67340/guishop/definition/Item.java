package com.pablo67340.guishop.definition;

import com.cryptomorin.xseries.XMaterial;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.ConfigUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

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
    private boolean useDynamicPricing, disableQty, resolveFailed = false;

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
        if (ConfigUtil.isDynamicPricing() && isUseDynamicPricing() && hasSellPrice()) {

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
        if (ConfigUtil.isDynamicPricing() && isUseDynamicPricing() && hasBuyPrice()) {

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

                return ConfigUtil.getBuyLore().replace("{amount}",
                        ConfigUtil.getCurrency() + Main.economyFormat(calculateBuyPrice(quantity)) + ConfigUtil.getCurrencySuffix());
            }
            return ConfigUtil.getFreeLore();
        }
        return ConfigUtil.getCannotBuy();
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
            return ConfigUtil.getSellLore().replace("{amount}",
                    ConfigUtil.getCurrency() + Main.economyFormat(calculateSellPrice(quantity)) + ConfigUtil.getCurrencySuffix());
        }
        return ConfigUtil.getCannotSell();
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

    public static Item parse(ItemStack itemStack, Integer slot, String shop) {
        Item item = new Item();

        if (itemStack != null) {
            NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
            Main.debugLog(comp.toString());
            item.setMaterial(itemStack.getType().toString());
            item.setSlot(slot);
            item.setShop(shop);
            if (comp.hasKey("itemType")) {
                item.setItemType(ItemType.valueOf(comp.getString("itemType")));
            }
            if (comp.hasKey("buyPrice")) {
                Object buyPrice = getBuyPrice(itemStack);
                Main.debugLog("had buyPrice comp: " + buyPrice);
                item.setBuyPrice(buyPrice);
            }
            if (comp.hasKey("sellPrice")) {
                Object sellPrice = getSellPrice(itemStack);
                item.setSellPrice(sellPrice);
            }

            if (comp.hasKey("shopName")) {
                item.setShopName(comp.getString("shopName"));
            }
            
            if (comp.hasKey("targetShop")) {
                item.setTargetShop(comp.getString("targetShop"));
            }

            if (comp.hasKey("buyName")) {
                item.setBuyName(comp.getString("buyName"));
            }
            
            if (comp.hasKey("name")) {
                item.setName(comp.getString("name"));
            }

            if (comp.hasKey("enchantments")) {
                item.setEnchantments(comp.getString("enchantments").split(" "));
            }

            if (comp.hasKey("commands")) {
                item.setItemType(ItemType.COMMAND);
                item.setCommands(Arrays.asList(comp.getString("commands").split("::")));
            }

            if (comp.hasKey("mobType")) {
                item.setMobType(comp.getString("mobType"));
            }

            if (comp.hasKey("buyLoreLines")) {
                String line = comp.getString("buyLoreLines");
                String[] parsedLore = line.split("::");
                item.setBuyLore(Arrays.asList(parsedLore));
            }

            if (comp.hasKey("shopLoreLines")) {
                String line = comp.getString("shopLoreLines");
                Main.debugLog("Item had Shop Lore " + line);
                String[] parsedLore = line.split("::");
                item.setShopLore(Arrays.asList(parsedLore));
            }
            
            if (comp.hasKey("LoreLines")) {
                String line = comp.getString("LoreLines");
                Main.debugLog("Item had Lore " + line);
                String[] parsedLore = line.split("::");
                item.setLore(Arrays.asList(parsedLore));
            }

            if (comp.hasKey("customNBT")) {
                item.setNBT(comp.getString("customNBT"));
            }
        }

        return item;
    }

    /**
     * Gets the buyPrice of an item using NBT, or <code>null</code> if not
     * defined
     *
     * @param item ItemStack to get the BuyPrice on
     * @return Buy Price either Double/Int
     */
    public static Object getBuyPrice(ItemStack item) {
        NBTTagCompound comp = ItemNBTUtil.getTag(item);

        if (comp.hasKey("buyPrice")) {
            Double vl = comp.getDouble("buyPrice");
            return vl;
        }
        return null;
    }

    /**
     * Gets the sellPrice of an item using NBT, or <code>null</code> if not
     * defined
     *
     * @param item ItemStack to get the SellPrice of
     * @return Sell Price either Double/Int
     */
    public static Object getSellPrice(ItemStack item) {
        NBTTagCompound comp = ItemNBTUtil.getTag(item);

        if (comp.hasKey("sellPrice")) {
            Double vl = comp.getDouble("sellPrice");
            return vl;
        }
        return null;
    }

    public static Item deserialize(Map<String, Object> serialized, Integer slot, String shop) {
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
            } else if (entry.getKey().equalsIgnoreCase("name")) {
                item.setName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-name")) {
                item.setBuyName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("shop-lore")) {
                item.setShopLore((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-lore")) {
                item.setBuyLore((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("lore")) {
                item.setLore((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-price")) {
                if (entry.getValue() instanceof Double) {
                    item.setBuyPrice((Double) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    item.setBuyPrice((Integer) entry.getValue());
                }
            } else if (entry.getKey().equalsIgnoreCase("sell-price")) {
                if (entry.getValue() instanceof Double) {
                    item.setSellPrice((Double) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    item.setSellPrice((Integer) entry.getValue());
                }
            } else if (entry.getKey().equalsIgnoreCase("commands")) {
                item.setCommands((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("target-shop")) {
                item.setTargetShop((String) entry.getValue());
                item.setItemType(ItemType.ITEM);
            } else if (entry.getKey().equalsIgnoreCase("enchantments")) {
                item.setEnchantments(((String) entry.getValue()).split(" "));
            } else if (entry.getKey().equalsIgnoreCase("custom-nbt")) {
                item.setNBT((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("mob-type")) {
                item.setMobType((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("potion-info")) {
                ConfigurationSection section = (ConfigurationSection) entry.getValue();
                Map<String, Object> potionInfo = section.getValues(true);
                item.setPotionInfo(new PotionInfo((String) potionInfo.get("type"), (Boolean) potionInfo.get("splash"), (Boolean) potionInfo.get("extended"), (Boolean) potionInfo.get("upgraded")));
            }
        }
        return item;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        if (itemType != ItemType.DUMMY) {
            serialized.put("type", itemType.toString());
        }
        serialized.put("id", material);
        if (hasShopName()) {
            serialized.put("shop-name", shopName);
        }
        if (hasBuyName()) {
            serialized.put("buy-name", buyName);
        }
        if (hasName()) {
            serialized.put("name", name);
        }
        if (hasShopLore()) {
            serialized.put("shop-lore", shopLore);
        }
        if (hasBuyLore()) {
            serialized.put("buy-lore", buyLore);
        }
        if (hasLore()) {
            serialized.put("lore", lore);
        }
        if (hasBuyPrice()) {
            serialized.put("buy-price", buyPrice);
        }
        if (hasSellPrice()) {
            serialized.put("sell-price", sellPrice);
        }
        if (hasCommands()) {
            serialized.put("commands", commands);
        }
        if (hasTargetShop()) {
            serialized.put("target-shop", targetShop);
        }
        if (hasEnchantments()) {
            String parsed = "";
            for (String str : enchantments) {
                parsed += str + " ";
            }
            serialized.put("enchantments", parsed);
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

        return serialized;
    }

}
