package com.pablo67340.guishop.definition;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTCompound;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTContainer;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NBTItem;
import com.github.stefvanschie.inventoryframework.shade.nbtapi.NbtApiException;
import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import com.pablo67340.guishop.listenable.Shop;
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
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

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
            NBTItem cmp = new NBTItem(item);
            if (cmp.hasKey("GUIShopSpawner")) {
                mobType = cmp.getString("GUIShopSpawner");
            } else if (cmp.hasKey("BlockEntityTag")) {
                NBTCompound subCmp = cmp.getCompound("BlockEntityTag");
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

        GUIShop.getINSTANCE().getLogUtil().debugLog("Failed to find entity type using EntityType#fromName");
        try {
            return EntityType.valueOf(getMobType());
        } catch (IllegalArgumentException ignored) {
        }

        GUIShop.getINSTANCE().getLogUtil().debugLog("Failed to find entity type using EntityType#valueOf");
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
            NBTItem component = new NBTItem(itemStack);
            GUIShop.getINSTANCE().getLogUtil().debugLog(component.toString());
            item.setMaterial(itemStack.getType().toString());
            item.setSlot(slot);
            item.setShop(shop);
            if (component.hasKey("itemType")) {
                item.setItemType(ItemType.valueOf(component.getString("itemType")));
            }

            if (component.hasKey("permission")) {
                item.setPermission(new Permission(component.getString("permission")));
            }

            if (component.hasKey("buyPrice")) {
                Object buyPrice = getBuyPrice(itemStack);
                item.setBuyPrice(buyPrice);
            }

            if (component.hasKey("sellPrice")) {
                Object sellPrice = getSellPrice(itemStack);
                item.setSellPrice(sellPrice);
            }

            if (component.hasKey("shopName")) {
                item.setShopName(component.getString("shopName"));
            }

            if (component.hasKey("targetShop")) {
                item.setTargetShop(component.getString("targetShop"));
            }

            if (component.hasKey("buyName")) {
                item.setBuyName(component.getString("buyName"));
            }

            if (component.hasKey("name")) {
                item.setName(component.getString("name"));
            }

            if (component.hasKey("enchantments")) {
                item.setEnchantments(component.getString("enchantments").split(" "));
            }

            if (component.hasKey("commands")) {
                item.setItemType(ItemType.COMMAND);
                item.setCommands(Arrays.asList(component.getString("commands").split("::")));
            }

            if (component.hasKey("potion")) {
                String[] splitInfo = component.getString("potion").split("::");
                item.setPotionInfo(
                        new PotionInfo(splitInfo[0], Boolean.parseBoolean(splitInfo[1]), Boolean.parseBoolean(splitInfo[2]), Boolean.parseBoolean(splitInfo[3])));
            }

            if (component.hasKey("quantity")) {
                item.setQuantityValue(new QuantityValue().setQuantity(component.getInteger("quantity")));
            }

            if (component.hasKey("skullUUID")) {
                item.setSkullUUID(component.getString("skullUUID"));
            }

            if (component.hasKey("mobType")) {
                item.setMobType(component.getString("mobType"));
            }

            if (component.hasKey("buyLoreLines")) {
                String line = component.getString("buyLoreLines");
                String[] parsedLore = line.split("::");
                item.setBuyLore(Arrays.asList(parsedLore));
            }

            if (component.hasKey("shopLoreLines")) {
                String line = component.getString("shopLoreLines");
                GUIShop.getINSTANCE().getLogUtil().debugLog("Item had shop lore " + line);
                String[] parsedLore = line.split("::");
                item.setShopLore(Arrays.asList(parsedLore));
            }

            if (component.hasKey("loreLines")) {
                String line = component.getString("loreLines");
                GUIShop.getINSTANCE().getLogUtil().debugLog("Item had lore " + line);
                String[] parsedLore = line.split("::");
                item.setLore(Arrays.asList(parsedLore));
            }

            if (component.hasKey("customNBT")) {
                item.setNBT(component.getString("customNBT"));
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
                        getShopLore().forEach(str -> {
                            if (!itemLore.contains(str)) {
                                itemLore.add(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(str, player, this));
                            }
                        });
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
                    NBTItem comp = new NBTItem(itemStack);
                    if (hasBuyPrice()) {
                        comp.setDouble("buyPrice", getBuyPriceAsDecimal().doubleValue());
                    }
                    if (hasSellPrice()) {
                        comp.setDouble("sellPrice", getSellPriceAsDecimal().doubleValue());
                    }
                    if (hasBuyName()) {
                        comp.setString("buyName", getBuyName());
                    }
                    if (hasShopName()) {
                        comp.setString("shopName", getShopName());
                    }
                    if (hasName()) {
                        comp.setString("name", getName());
                    }
                    if (hasMobType()) {
                        comp.setString("mobType", getMobType());
                    }
                    if (hasEnchantments()) {
                        StringBuilder itemEnchantments = new StringBuilder();
                        for (String str : getEnchantments()) {
                            itemEnchantments.append(str).append(",");
                        }
                        comp.setString("enchantments", itemEnchantments.toString());
                    }
                    if (hasShopLore()) {
                        StringBuilder lor = new StringBuilder();
                        int index = 0;
                        for (String str : getShopLore()) {
                            if (index != (getShopLore().size() - 1)) {
                                lor.append(str).append("::");
                            } else {
                                lor.append(str);
                            }
                            index += 1;
                        }
                        comp.setString("shopLoreLines", lor.toString());
                    }
                    if (hasBuyLore()) {
                        StringBuilder lor = new StringBuilder();
                        int index = 0;
                        for (String str : getBuyLore()) {
                            if (index != (getBuyLore().size() - 1)) {
                                lor.append(str).append("::");
                            } else {
                                lor.append(str);
                            }
                            index += 1;
                        }
                        comp.setString("buyLoreLines", lor.toString());
                    }
                    if (hasLore()) {
                        StringBuilder lor = new StringBuilder();
                        int index = 0;
                        for (String str : getLore()) {
                            if (index != (getLore().size() - 1)) {
                                lor.append(str).append("::");
                            } else {
                                lor.append(str);
                            }
                            index += 1;
                        }
                        comp.setString("loreLines", lor.toString());
                    }
                    if (hasCommands()) {
                        StringBuilder lor = new StringBuilder();
                        int index = 0;
                        for (String str : getCommands()) {
                            if (index != (getCommands().size() - 1)) {
                                lor.append(str).append("::");
                            } else {
                                lor.append(str);
                            }
                            index += 1;
                        }
                        comp.setString("commands", lor.toString());
                    }
                    if (hasSkullUUID()) {
                        comp.setString("skullUUID", getSkullUUID());
                    }
                    if (getQuantityValue() != null) {
                        comp.setInteger("quantity", getQuantityValue().getQuantity());
                    }
                    if (hasPotion()) {
                        String[] values = {getPotionInfo().getType(), getPotionInfo().getSplash().toString(), getPotionInfo().getExtended().toString(), getPotionInfo().getUpgraded().toString()};
                        comp.setString("potion", String.join("::", values));
                    }
                    if (hasPermission()) {
                        comp.setString("permission", getPermission().getPermission());
                    }
                    comp.setString("itemType", getItemType().toString());
                    itemStack = comp.getItem();
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
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(meta);
                    }
                } else {
                    for (String enc : getEnchantments()) {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(itemMeta);
                    }
                }
            }

            itemStack.setItemMeta(itemMeta);

            if (hasNBT()) {
                try {
                    ItemStack tempItem = itemStack.clone();
                    NBTContainer container = new NBTContainer(getNBT());
                    NBTItem nbti = new NBTItem(tempItem);
                    nbti.mergeCompound(container);
                    tempItem = nbti.getItem();

                    if (tempItem == null) {
                        GUIShop.getINSTANCE().getLogUtil().log("Error parsing custom NBT for item: " + getMaterial() + " in shop: " + getShop() + ". Please fix or remove custom-nbt value.");
                        setResolveFailed("Item has Invalid Custom NBT");
                        return getErrorStack();
                    } else {
                        itemStack = nbti.getItem();
                    }
                } catch (NbtApiException exception) {
                    GUIShop.getINSTANCE().getLogUtil().log("Error parsing custom NBT for item: " + getMaterial() + " in shop: " + getShop() + ". Please fix or remove custom-nbt value.");
                    setResolveFailed("Item has Invalid Custom NBT");
                }
            }

            if (hasPotion()) {
                PotionInfo potionInfo = getPotionInfo();

                if (XMaterial.getVersion() > 18) {
                    if (potionInfo.getSplash()) {
                        itemStack = new ItemStack(Material.SPLASH_POTION);
                        itemStack.setItemMeta(itemMeta);
                    }
                    PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

                    PotionData potionData;
                    try {
                        potionData = new PotionData(PotionType.valueOf(potionInfo.getType()), potionInfo.getExtended(), potionInfo.getUpgraded());
                        potionMeta.setBasePotionData(potionData);
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage().contains("upgradable")) {
                            GUIShop.getINSTANCE().getLogUtil().log("Potion: " + potionInfo.getType() + " Is not upgradable. Please fix this. Potion has automatically been downgraded.");
                            potionInfo.setUpgraded(false);
                            potionData = new PotionData(PotionType.valueOf(potionInfo.getType()), potionInfo.getExtended(), potionInfo.getUpgraded());
                            potionMeta.setBasePotionData(potionData);
                        } else if (ex.getMessage().contains("extended")) {
                            GUIShop.getINSTANCE().getLogUtil().log("Potion: " + potionInfo.getType() + " Is not extendable. Please fix this. Potion has automatically been downgraded.");
                            potionInfo.setExtended(false);
                            potionData = new PotionData(PotionType.valueOf(potionInfo.getType()), potionInfo.getExtended(), potionInfo.getUpgraded());
                            potionMeta.setBasePotionData(potionData);
                        }
                    }
                    itemStack.setItemMeta(potionMeta);
                } else {
                    Potion potion = new Potion(PotionType.valueOf(potionInfo.getType()), potionInfo.getUpgraded() ? 2 : 1, potionInfo.getSplash(), potionInfo.getExtended());
                    potion.apply(itemStack);
                }
            }
        } else {
            if (hasName()) {
                itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getName(), player, this));
            } else if (hasShopName()) {
                itemMeta.setDisplayName(GUIShop.getINSTANCE().getMiscUtils().placeholderIfy(getShopName(), player, this));
            }
            itemStack.setItemMeta(itemMeta);
        }

        // Create Page
        GUIShop.getINSTANCE().getLogUtil().debugLog("Setting item to slot: " + getSlot());

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
                    String enchantment = StringUtils.substringBefore(enc, ":");
                    Integer level = Integer.parseInt(StringUtils.substringAfter(enc, ":"));
                    Enchantment targetEnchantment = XEnchantment.matchXEnchantment(enchantment).get().getEnchant();
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
                    Enchantment targetEnchantment = XEnchantment.matchXEnchantment(enchantment).get().getEnchant();
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
                } else {
                    if (!fakeComp.equals(tag)) {
                        return false;
                    }
                }
            }
        }
        if (hasSkullUUID() && Config.isSellSkullUUID()) {
            SkullMeta sm = (SkullMeta) input.getItemMeta();
            if (!sm.getOwningPlayer().getUniqueId().toString().equals(skullUUID)) {
                return false;
            }
        }
        if (isMobSpawner()) {
            NBTCompound tag = new NBTItem(input);
            return tag.getString("GUIShopSpawner").equals(mobType);
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
            if (XMaterial.getVersion() > 18) {
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
                        GUIShop.getINSTANCE().getLogUtil().log("Potion: " + pi.getType() + " Is not upgradable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                        pi.setUpgraded(false);
                        pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                        pm.setBasePotionData(pd);
                    } else if (ex.getMessage().contains("extended")) {
                        GUIShop.getINSTANCE().getLogUtil().log("Potion: " + pi.getType() + " Is not extendable. Please fix this in menu.yml. Potion has automatically been downgraded.");
                        pi.setExtended(false);
                        pd = new PotionData(PotionType.valueOf(pi.getType()), pi.getExtended(), pi.getUpgraded());
                        pm.setBasePotionData(pd);
                    }
                }
                itemStack.setItemMeta(pm);
            } else {
                Potion potion = new Potion(PotionType.valueOf(pi.getType()), pi.getUpgraded() ? 2 : 1, pi.getSplash(), pi.getExtended());
                potion.apply(itemStack);
            }
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
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        meta.addStoredEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(meta);
                    } catch (NoSuchElementException | NullPointerException ignored) {
                    }
                }
            } else {
                for (String enc : getEnchantments()) {
                    try {
                        String enchantment = StringUtils.substringBefore(enc, ":");
                        String level = StringUtils.substringAfter(enc, ":");
                        itemMeta.addEnchant(XEnchantment.matchXEnchantment(enchantment).get().getEnchant(), Integer.parseInt(level), true);
                        itemStack.setItemMeta(itemMeta);
                    } catch (NoSuchElementException | NullPointerException ignored) {
                    }
                }
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
                GUIShop.getINSTANCE().getLogUtil().log("Error parsing custom NBT for item: " + getMaterial() + " in shop: " + shop + ". Please fix or remove custom-nbt value.");
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
        NBTCompound comp = new NBTItem(item);

        if (comp.hasKey("buyPrice")) {
            return BigDecimal.valueOf(comp.getDouble("buyPrice"));
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
        NBTCompound comp = new NBTItem(item);

        if (comp.hasKey("sellPrice")) {
            return BigDecimal.valueOf(comp.getDouble("sellPrice"));
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
                if (entry.getValue() instanceof Double buyPrice) {
                    BigDecimal buyPrice2 = BigDecimal.valueOf(buyPrice);
                    item.setBuyPrice(buyPrice2);
                } else if (entry.getValue() instanceof Integer) {
                    item.setBuyPrice(entry.getValue());
                }
            } else if (entry.getKey().equalsIgnoreCase("sell-price")) {
                if (entry.getValue() instanceof Double sellPrice) {
                    BigDecimal sellPrice2 = BigDecimal.valueOf(sellPrice);
                    item.setSellPrice(sellPrice2);
                } else if (entry.getValue() instanceof Integer) {
                    item.setSellPrice(entry.getValue());
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
                item.setEnchantments(Arrays.stream(((String) entry.getValue()).split(" ")).filter(enchant -> {
                    try {
                        XEnchantment.matchXEnchantment(enchant).get().getEnchant();
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
        return item;
    }

    @Override
    public @NotNull
    Map<String, Object> serialize() {
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
