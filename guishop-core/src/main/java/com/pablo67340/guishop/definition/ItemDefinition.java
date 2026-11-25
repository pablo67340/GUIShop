package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

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

    // --------------------------------------------------------------- pricing

    /**
     * Whether live pricing should be consulted for this item, which needs both
     * the global setting and the item's own opt-in.
     */
    public boolean shouldUseDynamicPricing() {
        return BaseConfig.isDynamicPricing() && useDynamicPricing;
    }

    /**
     * The buy price for a quantity, through the live-pricing provider when one
     * applies and a plain multiplication otherwise.
     *
     * @param quantity how many of the item are being bought
     */
    public BigDecimal calculateBuyPrice(int quantity) {
        // A sell price must be defined for dynamic pricing to have a spread.
        if (shouldUseDynamicPricing() && hasSellPrice()) {
            DynamicPriceProvider provider = GuiShopPlatform.get().dynamicPriceProvider();
            if (provider != null) {
                return provider.calculateBuyPrice(getItemString(), quantity,
                        getBuyPriceAsDecimal(), getSellPriceAsDecimal());
            }
        }
        return getBuyPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * The sell price for a quantity, through the live-pricing provider when one
     * applies and a plain multiplication otherwise.
     *
     * @param quantity how many of the item are being sold
     */
    public BigDecimal calculateSellPrice(int quantity) {
        // A buy price must be defined for dynamic pricing to have a spread.
        if (shouldUseDynamicPricing() && hasBuyPrice()) {
            DynamicPriceProvider provider = GuiShopPlatform.get().dynamicPriceProvider();
            if (provider != null) {
                return provider.calculateSellPrice(getItemString(), quantity,
                        getBuyPriceAsDecimal(), getSellPriceAsDecimal());
            }
        }
        return getSellPriceAsDecimal().multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * The key this item is tracked under for live pricing. Spawners carry their
     * mob type so each one is priced separately.
     * <p>
     * The format is relied on by stored pricing data, so it must not change.
     */
    public String getItemString() {
        if (isMobSpawner() && hasMobType()) {
            return material.toUpperCase(Locale.ROOT) + ":spawner:" + mobType.toLowerCase(Locale.ROOT);
        }
        return material.toUpperCase(Locale.ROOT);
    }

    /**
     * Whether this item is a mob spawner, which is priced per mob type.
     * <p>
     * Overridden where the platform can identify spawners more reliably than by
     * name across game versions.
     */
    public boolean isMobSpawner() {
        return material != null && material.toUpperCase(Locale.ROOT).endsWith("SPAWNER");
    }

    // ------------------------------------------------------------ price lore

    /**
     * The configured line describing this item's buy price, or the configured
     * line explaining why it cannot be bought.
     *
     * @param quantity how many of the item the price should cover
     */
    public String getBuyLore(int quantity) {
        if (hasBuyPrice()) {
            BigDecimal buyPrice = getBuyPriceAsDecimal();
            if (buyPrice.compareTo(BigDecimal.ZERO) > 0) {
                return buildPriceLore("buy", "buy-market",
                        buyPrice.multiply(BigDecimal.valueOf(quantity)),
                        calculateBuyPrice(quantity));
            }
            // A buy price of zero is a giveaway rather than a missing price.
            return lore("free");
        }
        return lore("cannot-buy");
    }

    /**
     * The configured line describing this item's sell price, or the configured
     * line explaining why it cannot be sold.
     *
     * @param quantity how many of the item the price should cover
     */
    public String getSellLore(int quantity) {
        if (hasSellPrice()) {
            return buildPriceLore("sell", "sell-market",
                    getSellPriceAsDecimal().multiply(BigDecimal.valueOf(quantity)),
                    calculateSellPrice(quantity));
        }
        return lore("cannot-sell");
    }

    /**
     * Fills in one of the price templates.
     * <p>
     * The market template is only used when the live price has actually drifted
     * from the configured one, so a statically priced item always reads as a
     * plain price rather than as an unchanged market price.
     *
     * @param plainKey     the lore key for the standard price line
     * @param marketKey    the lore key for the price line showing movement
     * @param baseTotal    the configured price for the quantity
     * @param currentTotal the live price for the quantity
     */
    private String buildPriceLore(String plainKey, String marketKey, BigDecimal baseTotal, BigDecimal currentTotal) {
        String currentAmount = formatCurrency(currentTotal);
        String marketTemplate = lore(marketKey);

        if (!BaseConfig.isShowMarketInfo() || marketTemplate == null
                || baseTotal.compareTo(BigDecimal.ZERO) <= 0
                || baseTotal.compareTo(currentTotal) == 0) {
            String plain = lore(plainKey);
            return plain == null ? currentAmount : plain.replace("%amount%", currentAmount);
        }

        BigDecimal difference = currentTotal.subtract(baseTotal);
        BigDecimal percent = difference.abs().multiply(BigDecimal.valueOf(100))
                .divide(baseTotal, 2, RoundingMode.HALF_UP);
        String trend = BaseConfig.getLoreConfig().lores
                .getOrDefault(difference.signum() > 0 ? "trend-up" : "trend-down", "");

        // Trend goes in first so its own placeholders resolve below.
        return marketTemplate.replace("%trend%", trend)
                .replace("%amount%", currentAmount)
                .replace("%base_amount%", formatCurrency(baseTotal))
                .replace("%change_amount%", formatCurrency(difference.abs()))
                .replace("%change_percent%", percent.toPlainString() + "%");
    }

    private String formatCurrency(BigDecimal value) {
        return GuiShopPlatform.get().formatCurrency(value);
    }

    private static String lore(String key) {
        return BaseConfig.getLoreConfig().lores.get(key);
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
