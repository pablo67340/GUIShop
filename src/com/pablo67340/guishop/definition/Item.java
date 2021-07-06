package com.pablo67340.guishop.definition;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTCompound;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTContainer;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.listenable.Shop;
import com.pablo67340.guishop.util.ConfigUtil;
import com.pablo67340.guishop.util.SkullCreator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

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
    private boolean useDynamicPricing, disableQty;

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
     * Assumming {@link #hasBuyPrice()} = <code>true</code>, calculate the buy
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
        if (ConfigUtil.isDynamicPricing() && isUseDynamicPricing() && hasSellPrice()) {

            return Main.getDYNAMICPRICING().calculateBuyPrice(getItemString(), quantity, getBuyPriceAsDecimal(),
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
        if (ConfigUtil.isDynamicPricing() && isUseDynamicPricing() && hasBuyPrice()) {

            return Main.getDYNAMICPRICING().calculateSellPrice(getItemString(), quantity, getBuyPriceAsDecimal(),
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
            NBTItem nbti = new NBTItem(item);

            if (nbti.hasKey("GUIShopSpawner")) {
                mobType = nbti.getString("GUIShopSpawner");

            } else if (nbti.hasKey("BlockEntityTag")) {
                NBTCompound subCmp = (NBTCompound) nbti.getCompound("BlockEntityTag");
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
            NBTItem comp = new NBTItem(itemStack);
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

    public ItemStack toItemStack(Player player, boolean isMenu) {
        ItemStack itemStack = null;

        try {
            itemStack = XMaterial.matchXMaterial(getMaterial()).get().parseItem();
        } catch (NoSuchElementException ex) {
            setResolveFailed("Item has Invalid Material");
        }

        Main.debugLog("Adding item to slot: " + getSlot());
        if (itemStack == null || itemStack.getType() == null || isResolveFailed()) {
            Main.log("Item: " + getMaterial() + " could not be resolved (invalid material). Are you using an old server version?");
            setResolveFailed("Item has Invalid Material");
            return getErrorStack();
        }

        if (itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial() && hasSkullUUID()) {
            itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(getSkullUUID()), getSkullUUID());
        }

        // Checks if an item is either a shop item or command item. This also handles
        // Null items as there is a item type switch in the lines above.
        if (getItemType() != ItemType.DUMMY) {

            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta == null) {
                Main.log("Item: " + getMaterial() + " could not be resolved (null meta).");
                setResolveFailed("Item has invalid Item Meta");
                return getErrorStack();
            }

            List<String> itemLore = new ArrayList<>();

            if (!isMenu) {
                itemLore.add(getBuyLore(1));
                itemLore.add(getSellLore(1));
            }

            if (player != null) {
                if (!Main.getCREATOR().contains(player.getName())) {
                    if (hasShopName() && !isMenu) {
                        assert itemMeta != null;
                        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getShopName()));
                    } else if (hasName()) {
                        assert itemMeta != null;
                        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getName()));
                    } else if (isMobSpawner() && !isMenu) {
                        String mobName = getMobType();
                        mobName = mobName.toLowerCase();
                        mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
                        assert itemMeta != null;
                        itemMeta.setDisplayName(mobName + " Spawner");
                    }
                    if (hasShopLore() && !isMenu) {
                        getShopLore().forEach(str -> {
                            if (!itemLore.contains(str) && !itemLore.contains(ConfigUtil.getBuyLore().replace("{AMOUNT}", calculateBuyPrice(1).toPlainString()))) {
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                            }
                        });
                    } else if (hasLore() && isMenu) {
                        getLore().forEach(str -> {
                            itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                        });
                    }
                    itemMeta.setLore(itemLore);
                    itemStack.setItemMeta(itemMeta);
                } else {
                    itemLore.add(" ");
                    itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fItem Type: &r" + getItemType().toString()));
                    if (hasShopName() && !isMenu) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fShop Name: &r" + getShopName()));
                    }
                    if (hasMobType()) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fMob Type: &r" + getMobType()));
                    }
                    if (hasBuyName() && !isMenu) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fBuy Name: &r" + getBuyName()));
                    }
                    if (hasBuyLore() && !isMenu) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fBuy Lore: &r"));
                        getBuyLore().forEach(str -> {
                            itemLore.add(str);
                        });
                    }
                    if (hasShopLore() && !isMenu) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fShop Lore: &r"));
                        getShopLore().forEach(str -> {
                            itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                        });
                    }
                    if (hasLore() && isMenu) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fLore: &r"));
                        getLore().forEach(str -> {
                            itemLore.add(ChatColor.translateAlternateColorCodes('&', str));
                        });
                    }
                    if (hasCommands() && !isMenu) {
                        itemLore.add(" ");
                        itemLore.add(ChatColor.translateAlternateColorCodes('&', "&fCommands: "));
                        getCommands().forEach(str -> {
                            if (str.length() > 20) {
                                String s = ChatColor.translateAlternateColorCodes('&', "/" + str);
                                s = s.substring(0, Math.min(s.length(), 20));
                                itemLore.add(s + "...");
                            } else {
                                itemLore.add("/" + str);
                            }
                        });
                    }
                    if (hasEnchantments()) {
                        String encLore = "";
                        for (String str : getEnchantments()) {
                            encLore += str + " ";
                        }
                        itemLore.add("Enchantments: " + encLore.trim());
                    }
                    if (!itemLore.isEmpty()) {
                        assert itemMeta != null;
                        itemMeta.setLore(itemLore);
                    }
                    itemStack.setItemMeta(itemMeta);
                    NBTItem comp = new NBTItem(itemStack);
                    Main.debugLog("USER IN CREATOR.Setting item Buy Price");
                    if (hasBuyPrice() && !isMenu) {
                        comp.setDouble("buyPrice", getBuyPriceAsDecimal().doubleValue());
                    }
                    if (hasSellPrice() && !isMenu) {
                        comp.setDouble("sellPrice", getSellPriceAsDecimal().doubleValue());
                    }
                    if (hasBuyName() && !isMenu) {
                        comp.setString("buyName", getBuyName());
                    }
                    if (hasShopName() && !isMenu) {
                        comp.setString("shopName", getShopName());
                    }
                    if (hasName() && isMenu) {
                        comp.setString("name", getName());
                    }
                    if (hasMobType()) {
                        comp.setString("mob-type", getMobType());
                    }
                    if (hasEnchantments()) {
                        String itemEnchantments = "";
                        for (String str : getEnchantments()) {
                            itemEnchantments += str + ",";
                        }
                        comp.setString("enchantments", itemEnchantments);
                    }
                    if (hasShopLore() && !isMenu) {
                        String lor = "";
                        int index = 0;
                        for (String str : getShopLore()) {
                            if (index != (getShopLore().size() - 1)) {
                                lor += str + "::";
                            } else {
                                lor += str;
                            }
                            index += 1;
                        }
                        comp.setString("shopLoreLines", lor);
                    }
                    if (hasBuyLore() && !isMenu) {
                        String lor = "";
                        int index = 0;
                        for (String str : getBuyLore()) {
                            if (index != (getBuyLore().size() - 1)) {
                                lor += str + "::";
                            } else {
                                lor += str;
                            }
                            index += 1;
                        }
                        comp.setString("buyLoreLines", lor);
                    }
                    if (hasLore() && isMenu) {
                        String lor = "";
                        int index = 0;
                        for (String str : getLore()) {
                            if (index != (getLore().size() - 1)) {
                                lor += str + "::";
                            } else {
                                lor += str;
                            }
                            index += 1;
                        }
                        comp.setString("loreLines", lor);
                    }
                    if (hasCommands() && !isMenu) {
                        String lor = "";
                        int index = 0;
                        for (String str : getCommands()) {
                            if (index != (getCommands().size() - 1)) {
                                lor += str + "::";
                            } else {
                                lor += str;
                            }
                            index += 1;
                        }
                        comp.setString("commands", lor);
                    }
                    comp.setString("itemType", getItemType().toString());
                    itemStack = comp.getItem();
                }
            }

            if (hasItemFlags()) {
                for (String flag : itemFlags) {
                    itemMeta.addItemFlags(ItemFlag.valueOf(flag));
                }
            }
            if (hasCustomModelID()) {
                itemMeta.setCustomModelData(getCustomModelData());
            }

            if (hasEnchantments()) {
                if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                    for (String enc : getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        assert meta != null;
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(meta);
                    }
                } else {
                    for (String enc : getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(itemMeta);
                    }
                }
            }
            itemStack.setItemMeta(itemMeta);

            if (hasNBT()) {
                NBTContainer container = new NBTContainer(getNBT());
                NBTItem nbti = new NBTItem(itemStack);
                nbti.mergeCompound(container);
                itemStack = nbti.getItem();
                if (itemStack == null) {
                    Main.log("Error Parsing Custom NBT for Item: " + getMaterial() + " in Shop: " + shop + ". Please fix or remove custom-nbt value.");
                    setResolveFailed("Item has Invalid Custom NBT");
                    return getErrorStack();
                }

            }

            if (hasPotion()) {
                PotionInfo pi = getPotionInfo();
                if (XMaterial.isNewVersion()) {

                    if (pi.getSplash()) {
                        itemStack = new ItemStack(Material.SPLASH_POTION);
                        itemStack.setItemMeta(itemMeta);
                    }
                    PotionMeta pm = (PotionMeta) itemStack.getItemMeta();

                    PotionData pd;
                    try {
                        pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                        pm.setBasePotionData(pd);
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().contains("upgradable")) {
                            Main.log("Potion: " + pi.getType() + " Is not upgradable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                            pi.setUpgraded(false);
                            pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                            pm.setBasePotionData(pd);
                        } else if (ex.getMessage().contains("extended")) {
                            Main.log("Potion: " + pi.getType() + " Is not extendable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                            pi.setExtended(false);
                            pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                            pm.setBasePotionData(pd);
                        }
                    }
                    itemStack.setItemMeta(pm);
                } else {
                    Potion potion = new Potion(PotionType.valueOf(pi.getType()), pi.getUpgraded() == true ? 2 : 1, pi.getSplash(), pi.getExtended());
                    potion.apply(itemStack);
                }
            }
        } else {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (hasShopName()) {
                assert itemMeta != null;
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getShopName()));
            }
            itemStack.setItemMeta(itemMeta);
        }

        // Create Page
        Main.debugLog("Setting item to slot: " + getSlot());

        return itemStack;
    }

    public Boolean isItemFromItemStack(ItemStack input) {
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
                    String enchantment = StringUtils.substringBefore(enc, ":");
                    Integer level = Integer.parseInt(StringUtils.substringAfter(enc, ":"));
                    Enchantment targetEnchantment = XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment();
                    if (!input.getEnchantments().containsKey(targetEnchantment)) {
                        return false;
                    } else {
                        if (!input.getEnchantments().get(targetEnchantment).equals(level)) {
                            return false;
                        }
                    }
                }
            } else {
                ItemMeta itemMeta = input.getItemMeta();
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                for (String enc : getEnchantments()) {
                    String enchantment = StringUtils.substringBefore(enc, ":");
                    Integer level = Integer.parseInt(StringUtils.substringAfter(enc, ":"));
                    Enchantment targetEnchantment = XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment();
                    if (!meta.getStoredEnchants().containsKey(targetEnchantment)) {
                        return false;
                    } else {
                        if (!meta.getStoredEnchants().get(targetEnchantment).equals(level)) {
                            return false;
                        }
                    }
                }
            }
        }
        if (hasPotion()) {
            PotionMeta pm = (PotionMeta) input.getItemMeta();
            PotionData pd = pm.getBasePotionData();
            if (pd.isExtended() != getPotionInfo().getExtended()) {
                return false;
            }
            if (pd.isUpgraded() != getPotionInfo().getUpgraded()) {
                return false;
            }
            if (!pd.getType().toString().equals(getPotionInfo().getType())) {
                return false;
            }
        }
        if (hasNBT()) {

            NBTItem tag = new NBTItem(input);

            // We need to make a fake item, as Minecraft will
            // automatically re-cast some of the NBTTag's do 
            // more optimized object types. This will ensure
            // everything is matching Minecraft's optimized tag casting.
            ItemStack fakeItem = new ItemStack(input.getType());
            NBTItem fakeComp = new NBTItem(fakeItem);
            NBTContainer container = new NBTContainer(getNBT());
            fakeComp.mergeCompound(container);
            
            
            for (String key : tag.getKeys()) {
                if (!fakeComp.hasKey(key)) {
                    return false;
                }else{
                    if (!fakeComp.equals(tag)){
                        return false;
                    }
                }
            }

        }
        if (hasSkullUUID() && ConfigUtil.isSellSkullUUID()) {
            SkullMeta sm = (SkullMeta) input.getItemMeta();
            if (!sm.getOwningPlayer().getUniqueId().toString().equals(skullUUID)) {
                return false;
            }
        }
        if (isMobSpawner()) {
            NBTItem tag = new NBTItem(input);
            if (!tag.getString("GUIShopSpawner").equals(mobType)) {
                return false;
            }
        }
        return true;
    }

    public ItemStack toBuyItemStack(Integer quantity, Player player, Shop currentShop) {

        ItemStack itemStack = null;

        try {
            itemStack = XMaterial.matchXMaterial(getMaterial()).get().parseItem();
            itemStack.setAmount(quantity);
        } catch (NoSuchElementException ex) {
            setResolveFailed("Item has Invalid Material");
        }

        if (hasSkullUUID() && itemStack.getType() == XMaterial.matchXMaterial("PLAYER_HEAD").get().parseMaterial()) {
            itemStack = SkullCreator.itemFromBase64(itemStack, SkullCreator.getBase64FromUUID(getSkullUUID()), getSkullUUID());
        }

        if (hasPotion()) {
            PotionInfo pi = getPotionInfo();
            if (XMaterial.isNewVersion()) {

                if (pi.getSplash()) {
                    itemStack = new ItemStack(Material.SPLASH_POTION);
                }
                PotionMeta pm = (PotionMeta) itemStack.getItemMeta();

                PotionData pd;
                try {
                    pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                    pm.setBasePotionData(pd);
                } catch (IllegalArgumentException ex) {
                    if (ex.getMessage().contains("upgradable")) {
                        Main.log("Potion: " + pi.getType() + " Is not upgradable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                        pi.setUpgraded(false);
                        pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                        pm.setBasePotionData(pd);
                    } else if (ex.getMessage().contains("extended")) {
                        Main.log("Potion: " + pi.getType() + " Is not extendable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                        pi.setExtended(false);
                        pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                        pm.setBasePotionData(pd);
                    }
                }
                itemStack.setItemMeta(pm);
            } else {
                Potion potion = new Potion(PotionType.valueOf(pi.getType()), pi.getUpgraded() == true ? 2 : 1, pi.getSplash(), pi.getExtended());
                potion.apply(itemStack);
            }
        }

        ItemMeta itemMeta = itemStack.getItemMeta();

        List<String> itemLore = new ArrayList<>();
        if (hasBuyLore()) {
            getBuyLore().forEach(str -> {
                itemLore.add(ChatColor.translateAlternateColorCodes('&', Main.placeholderIfy(str, player, this)));
            });
        }

        itemMeta.setLore(itemLore);

        if (hasBuyName()) {
            assert itemMeta != null;
            itemMeta.setDisplayName(
                    ChatColor.translateAlternateColorCodes('&', Main.placeholderIfy(getBuyName(), player, this)));
        } else if (Item.isSpawnerItem(itemStack)) {
            String mobName = getMobType();
            mobName = mobName.toLowerCase();
            mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
            assert itemMeta != null;
            itemMeta.setDisplayName(mobName + " Spawner");
        }

        if (hasCustomModelID()) {
            itemMeta.setCustomModelData(getCustomModelData());
        }

        if (hasEnchantments()) {
            if (itemStack.getType() == Material.ENCHANTED_BOOK) {
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
                for (String enc : getEnchantments()) {
                    String enchantment = StringUtils.substringBefore(enc, ":");
                    String level = StringUtils.substringAfter(enc, ":");
                    assert meta != null;
                    meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
                    itemStack.setItemMeta(meta);
                }
            } else {
                for (String enc : getEnchantments()) {
                    String enchantment = StringUtils.substringBefore(enc, ":");
                    String level = StringUtils.substringAfter(enc, ":");
                    itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(), Integer.parseInt(level), true);
                    itemStack.setItemMeta(itemMeta);
                }
            }
        } else {
            itemStack.setItemMeta(itemMeta);
        }
        if (isMobSpawner()) {

            EntityType type = parseMobSpawnerType();
            if (type == null) {
                Main.log("Invalid Mob Spawner Entity Type: " + getMobType() + " In Shop: " + currentShop.getShop());

            } else {
                String entityValue = type.name();
                Main.debugLog("Attaching " + entityValue + " to purchased spawner");

                NBTItem tag = new NBTItem(itemStack);
                tag.setString("GUIShopSpawner", entityValue);
                itemStack = tag.getItem();
            }
        }
        if (hasNBT()) {
            NBTContainer container = new NBTContainer(getNBT());
            NBTItem nbti = new NBTItem(itemStack);
            nbti.mergeCompound(container);
            itemStack = nbti.getItem();
            if (itemStack == null) {
                Main.log("Error Parsing Custom NBT for Item: " + getMaterial() + " in Shop: " + shop + ". Please fix or remove custom-nbt value.");
                setResolveFailed("Item has Invalid Custom NBT");
                return getErrorStack();
            }

        }
        return itemStack;
    }

    /**
     * Gets the buyPrice of an item using NBT, or <code>null</code> if not
     * defined
     *
     * @param item ItemStack to get the BuyPrice on
     * @return Buy Price either Double/Int
     */
    public static BigDecimal getBuyPrice(ItemStack item) {
        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("buyPrice")) {
            BigDecimal vl = BigDecimal.valueOf(comp.getDouble("buyPrice"));
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
    public static BigDecimal getSellPrice(ItemStack item) {
        NBTItem comp = new NBTItem(item);

        if (comp.hasKey("sellPrice")) {
            BigDecimal vl = BigDecimal.valueOf(comp.getDouble("sellPrice"));
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
            } else if (entry.getKey().equalsIgnoreCase("disable-qty")) {
                item.setDisableQty((Boolean) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("name")) {
                item.setName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-name")) {
                item.setBuyName((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("skull-uuid")) {
                item.setSkullUUID((String) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("shop-lore")) {
                item.setShopLore((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-lore")) {
                item.setBuyLore((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("item-flags")) {
                item.setItemFlags((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("lore")) {
                item.setLore((List<String>) entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase("buy-price")) {
                if (entry.getValue() instanceof Double) {
                    Double buyPrice = (Double) entry.getValue();
                    BigDecimal buyPrice2 = BigDecimal.valueOf(buyPrice);
                    item.setBuyPrice(buyPrice2);
                } else if (entry.getValue() instanceof Integer) {
                    item.setBuyPrice((Integer) entry.getValue());
                }
            } else if (entry.getKey().equalsIgnoreCase("sell-price")) {
                if (entry.getValue() instanceof Double) {
                    Double sellPrice = (Double) entry.getValue();
                    BigDecimal sellPrice2 = BigDecimal.valueOf(sellPrice);
                    item.setSellPrice(sellPrice2);
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
        if (hasSkullUUID()) {
            serialized.put("skull-uuid", skullUUID);
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
        if (hasItemFlags()) {
            serialized.put("item-flags", itemFlags);
        }
        if (hasLore()) {
            serialized.put("lore", lore);
        }
        if (isDisableQty()) {
            serialized.put("disable-qty", disableQty);
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

}
