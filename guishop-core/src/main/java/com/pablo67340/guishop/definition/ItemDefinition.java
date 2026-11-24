package com.pablo67340.guishop.definition;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * The platform-independent description of a shop item: everything a shop entry
 * is, as opposed to how a particular server platform renders it.
 * <p>
 * Fields are protected rather than private so platform subclasses can read them
 * directly; Lombok still exposes public accessors for everything.
 * <p>
 * Anything that needs a {@code Material}, an {@code ItemStack}, the plugin
 * instance or the loaded config lives in the platform subclass instead. On
 * Bukkit that is {@link Item}.
 */
public abstract class ItemDefinition {

    /**
     * The name of this item when presented on the GUI.
     */
    @Getter
    @Setter
    protected String shopName, buyName, shop, targetShop, name, skullUUID, NBT;

    @Getter
    @Setter
    protected Integer slot, customModelData;

    @Getter
    @Setter
    protected int configSlot;

    /**
     * The Material of this item.
     */
    @Getter
    @Setter
    protected String material;

    /**
     * The price to buy this item.
     */
    @Getter
    @Setter
    protected Object buyPrice;

    /**
     * The mob ID of this item if it's a spawner item.
     */
    @Getter
    @Setter
    protected String mobType;

    /**
     * The reason a resolution may has failed.
     */
    @Getter
    @Setter
    protected String resolveReason;

    /**
     * The amount of money given when selling this item.
     */
    @Getter
    @Setter
    protected Object sellPrice;

    /**
     * Whether this item, specifically, uses dynamic pricing.
     * Defaults to true - items inherit global dynamic pricing setting.
     * Set to false to exempt this specific item from dynamic pricing.
     */
    @Getter
    @Setter
    protected boolean useDynamicPricing = true;

    @Getter
    @Setter
    protected QuantityValue quantityValue;

    @Getter
    protected boolean resolveFailed = false;

    /**
     * The behaviour of this item when clicked.
     */
    @Getter
    @Setter
    protected ItemType itemType = ItemType.DUMMY;

    @Getter
    @Setter
    protected List<String> buyLore, shopLore, lore, itemFlags;

    @Getter
    @Setter
    protected List<String> commands;

    /**
     * Whether commands should be run as the player (sudo) instead of console.
     * Defaults to false (run as console).
     */
    @Getter
    @Setter
    protected boolean sudo = false;

    /**
     * The enchantsments on this item.
     */
    @Getter
    @Setter
    protected String[] enchantments;

    @Getter
    @Setter
    protected PotionInfo potionInfo;

    @Getter
    @Setter
    protected FireworkInfo fireworkInfo;

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
     * If this item has firework info.
     *
     * @return true if the item has firework info, false otherwise
     */
    public boolean hasFirework() {
        return fireworkInfo != null;
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

    public void setResolveFailed(String reason) {
        resolveFailed = true;
        resolveReason = reason;
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
