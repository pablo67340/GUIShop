package com.pablo67340.guishop.definition;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.MatLib;
import com.pablo67340.guishop.util.XMaterial;

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

	public Boolean hasShopName() {
		return (shopName != null ? shopName.equalsIgnoreCase("") ? false : true : false);
	}

	public Boolean hasBuyName() {
		return buyName != null;
	}

	public Boolean hasShopLore() {
		if (shopLore == null) {
			return false;
		}
		if (shopLore.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean hasBuyLore() {
		if (buyLore == null) {
			return false;
		}
		if (buyLore.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean hasEnchantments() {
		if (enchantments == null) {
			return false;
		}
		if (enchantments[0].equalsIgnoreCase("")) {
			return false;
		}
		return true;
	}

	public boolean hasCommands() {
		if (commands == null) {
			return false;
		}
		if (commands.size() == 0) {
			return false;
		}
		return true;
	}

	public Boolean isMobSpawner() {
		return material.equalsIgnoreCase("SPAWNER") || material.equalsIgnoreCase("MOB_SPAWNER");
	}

	public Boolean hasSellPrice() {
		if (sellPrice == null) {
			return false;
		}

		if (sellPrice instanceof Boolean) {
			if (((Boolean) sellPrice) == false) {
				return false;
			}
		}

		if (sellPrice instanceof Double) {
			if (((Double) sellPrice) == 0.0) {
				return false;
			}
		}
		return true;
	}

	public Boolean hasBuyPrice() {
		if (buyPrice == null) {
			return false;
		}

		if (buyPrice instanceof Boolean) {
			if (((Boolean) buyPrice) == false) {
				return false;
			}
		}
		return true;
	}
	
	public Boolean hasMobType() {
		if (mobType == null) {
			return false;
		}
		if (mobType.equalsIgnoreCase("")) {
			return false;
		}
		return true;
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
	 * Assumming {@link #hasBuyPrice()} = <code>true</code>,
	 * calculate the buy price taking based on the given quantity. <br>
	 * If dynamic pricing is enabled, the DynamicPriceProvider is used for calculations.
	 * Otherwise, the buy price and the quantity are simply multiplied.
	 * 
	 * @param quantity the quantity of the item
	 * @return the calculated buy price
	 */
	public double calculateBuyPrice(int quantity) {
		// sell price must be defined and nonzero for dynamic pricing to work
		if (Config.isDynamicPricing() && hasSellPrice()) {
			
			return Main.getDYNAMICPRICING().calculateBuyPrice(getItemString(), quantity, getBuyPriceAsDouble(),
					getSellPriceAsDouble());
		}
		// default to fixed pricing
		return getBuyPriceAsDouble() * quantity;
	}
	
	/**
	 * Assumming {@link #hasSellPrice()} = <code>true</code>,
	 * calculate the sell price taking based on the given quantity. <br>
	 * If dynamic pricing is enabled, the DynamicPriceProvider is used for calculations.
	 * Otherwise, the sell price and the quantity are simply mmultiplied.
	 * 
	 * @param quantity the quantity of the item
	 * @return the calculated sell price
	 */
	public double calculateSellPrice(int quantity) {
		// buy price must be defined for dynamic pricing to work
		if (Config.isDynamicPricing() && hasBuyPrice()) {
			
			return Main.getDYNAMICPRICING().calculateSellPrice(getItemString(), quantity, getBuyPriceAsDouble(),
					getSellPriceAsDouble());
		}
		// default to fixed pricing
		return getSellPriceAsDouble() * quantity;
	}
	
	/**
	 * Gets the lore display for this item's buy price. <br>
	 * If there is no buy price, <code>Config.getCannotBuy()</code> is returned.
	 * If free, <code>Config.getFreeLore</code> is returned.
	 * Otherwise, the buy price is calculated based on the quantity, and the
	 * lore displaying the calculated buy price is returned. Takes into
	 * account dynamic pricing, if enabled.
	 * 
	 * @param quantity the quantity of the item
	 * @return the buy price lore
	 */
	public String getBuyLore(int quantity) {
		if (hasBuyPrice()) {

			double buyPriceAsDouble = getBuyPriceAsDouble();
			if (buyPriceAsDouble != 0) {

				return Config.getBuyLore().replace("{amount}",
						Config.getCurrency() + calculateBuyPrice(quantity) + Config.getCurrencySuffix());
			}
			return Config.getFreeLore();
		}
		return Config.getCannotBuy();
	}
	
	/**
	 * Gets the lore display for this item's sell price. <br>
	 * If there is no sell price, <code>Config.getCannotSell()</code> is returned.
	 * Otherwise, the sell price is calculated based on the quantity, and the
	 * lore displaying the calculated sell price is returned. Takes into
	 * account dynamic pricing, if enabled.
	 * 
	 * @param quantity the quantity of the item
	 * @return the sell price lore
	 */
	public String getSellLore(int quantity) {
		if (hasSellPrice()) {
			return Config.getSellLore().replace("{amount}",
					Config.getCurrency() + calculateSellPrice(quantity) + Config.getCurrencySuffix());
		}
		return Config.getCannotSell();
	}
	
	/**
	 * If the item is a mob spawner, <code>getMaterial().toUpperCase
	 * + ":" + getMobType().toLowerCase()</code> is returned.
	 * Otherwise, <code>getMaterial().toUpperCase</code> is simply returned.
	 * 
	 * @return the item string representation
	 */
	public String getItemString() {
		return (isMobSpawner()) ? material.toUpperCase() + ":" + getMobType().toLowerCase() : material.toUpperCase();
	}
	
	/**
	 * Assuming the sell price is an integer or a double,
	 * create a corresponding {@link Price} for this item. <br>
	 * If this item has no buy price, the result's
	 * {@link Price#getBuyPrice()} will return <code>0</code>.
	 * 
	 * @return a price object reflecting this item's pricing
	 */
	public Price generatePricing() {
		double buyPrice = (hasBuyPrice()) ? getBuyPriceAsDouble() : -1D;
		double sellPrice = (hasSellPrice()) ? getSellPriceAsDouble() : -1D;
		return new Price(buyPrice, sellPrice);
	}
	
	/**
	 * Equivalent of {@link Item#getItemString()} for an <i>ItemStack</i>,
	 * i.e., any minecraft item, not just a shop item. <br>
	 * <br>
	 * If the item is a mob spawner, <code>item.getType().toString().toUpperCase
	 * + ":" + mobType.toString().toLowerCase()</code> is returned where
	 * <i>mobtype</i> is the mob type of the mob spawner.
	 * Otherwise, <code>getType().toString().toUpperCase</code> is simply returned.
	 * 
	 * @param item the itemstack
	 * @return the item string representation of the itemstack
	 */
	public static String getItemStringForItemStack(ItemStack item) {
		if (item.getType().name().equals("SPAWNER") /* 1.13+ */
		        || item.getType().name().equals("MOB_SPAWNER") /* 1.7 - 1.12 */
		   ) {

			Object data;
			NBTTagCompound cmp = ItemNBTUtil.getTag(item);
			if (cmp.hasKey("GUIShopSpawner")) {
				data = cmp.getString("GUIShopSpawner");
			} else {
				data = MobType.valueOf(cmp.getString("EntityId"));
			}

			return item.getType().toString().toUpperCase() + ":" + data.toString().toLowerCase();

		}
		return item.getType().toString().toUpperCase();
	}
	
	/**
	 * Parses the material of this Item. <br>
	 * If the material cannot be resolved, <code>null</code> should be returned. <br>
	 * <br>
	 * This operation is somewhat resource intensive. Consider running asynchronously.
	 * (Keep in mind thread safety of course)
	 * 
	 * @return a gui item using the appropriate itemstack
	 */
	public GuiItem parseMaterial() {

		GuiItem gItem = null;
		ItemStack itemStack = XMaterial.matchXMaterial(getMaterial()).get().parseItem();

		if (itemStack != null) { // Change since 7.3.9: If underlying itemstack cannot be resolved (is null), fail immediately
			try {
				gItem = new GuiItem(itemStack);
				return gItem; // if itemStack is nonnull and no exception thrown, attempt has succeeded
			} catch (Exception ex2) {}
		}

		if (getMaterial().endsWith("_ON")) { // Change since 7.3.9: Only use OFF fix if _ON is detected

			Main.debugLog("Failed to find item by Material: " + getMaterial() + ". Attempting OFF Fix...");

			try {
				// remove the "_ON" and add "_OFF"
				itemStack = new ItemStack(Material.valueOf(getMaterial().substring(0, getMaterial().length() - 2) + "OFF"));
				gItem = new GuiItem(itemStack);
				return gItem; // if no exception thrown, attempt has succeeded
			} catch (Exception ex3) {}

			Main.debugLog("OFF Fix for: " + getMaterial() + " Failed. Attempting ItemID Lookup...");
		}

		// Final Stand, lets try to find this user's item
		try {
			String itemID = MatLib.getMAP().get(getMaterial());
			String[] idParts = itemID.split(":");
			int id = Integer.parseInt(idParts[0]);
			short data = Short.parseShort(idParts[1]);

			itemStack = LegacyItemConstructor.invoke(id, 1, data); // can never be null

			gItem = new GuiItem(itemStack);
			return gItem;
		} catch (Exception ex4) {}

		Main.debugLog("ItemID Fix for: " + getMaterial() + " Failed. Falling back to air.");

		setItemType(ItemType.BLANK);
		setEnchantments(null);
		// null indicates failure
		return null;
	}

}
