package com.pablo67340.shop.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.pablo67340.shop.main.Main;

public final class Creator {

	private final Player player;
	public Chest chest;
	public String name;
	private List<String> lore = new ArrayList<>(2);

	public Creator(Player p) {
		this.player = p;
		this.lore.add(" ");
		this.lore.add(" ");
		this.lore.add(" ");
	}
	
	/**
	 * 
	 * @param The
	 *            chest loc
	 * 
	 *            Set the chest needed to config GUIShop
	 */
	public void setChest() {
		BlockState potentialchest = this.player.getTargetBlock((Set<Material>) null, 100).getState();
		this.chest = (Chest) potentialchest;
		this.player.sendMessage(Utils.getPrefix() + " Target chest has been set!");
	}

	/**
	 * 
	 * @param {@link
	 * 			Player}
	 * 
	 *            Open the player's chest
	 */
	public void openChest() {
		this.player.openInventory(chest.getInventory());
	}

	/**
	 * 
	 * @param {@link
	 * 			Player}
	 * 
	 *            Clear the player's chest
	 */
	public void clearChest() {
		this.chest.getInventory().clear();
	}

	/**
	 * 
	 * @param The
	 *            shop name
	 * 
	 *            Set the current edited shop
	 */
	public void setShopName(String input) {
		this.name = input;
		this.player.sendMessage(Utils.getPrefix() + " Shop name set!");
	}

	/**
	 * 
	 * @param Item
	 *            Price
	 * 
	 *            Set an item's buy price
	 */
	@SuppressWarnings("deprecation")
	public void setPrice(Double price) {
		ItemStack item;
		if (Main.getInstance().isOdin()) {
			item = this.player.getInventory().getItemInMainHand();
		} else {
			item = this.player.getItemInHand();
		}
		ItemMeta im = item.getItemMeta();
		if (im.getLore() == null) {
			this.lore.set(0, " ");
			this.lore.set(1, " ");
			this.lore.set(2, " ");
		}
		this.lore.set(0, price.toString());
		im.setLore(lore);
		item.setItemMeta(im);
		this.player.sendMessage(Utils.getPrefix() + " Price set: " + price);
	}

	/**
	 * 
	 * @param Item
	 *            Sell value
	 * 
	 *            Set an item's sell price
	 */
	@SuppressWarnings("deprecation")
	public void setSell(Double sell) {
		ItemStack item;
		if (Main.getInstance().isOdin()) {
			item = this.player.getInventory().getItemInMainHand();
		} else {
			item = this.player.getItemInHand();
		}
		ItemMeta im = item.getItemMeta();
		if (im.getLore() == null) {
			this.lore.set(0, " ");
			this.lore.set(1, " ");
			this.lore.set(2, " ");
		}
		this.lore.set(1, sell.toString());
		im.setLore(lore);
		item.setItemMeta(im);
		this.player.sendMessage(Utils.getPrefix() + " Sell value set: " + sell);
	}

	/**
	 * 
	 * @param Item
	 *            Name
	 * 
	 *            Set the item's name
	 */
	@SuppressWarnings("deprecation")
	public void setName(String name) {
		ItemStack item;
		if (Main.getInstance().isOdin()) {
			item = this.player.getInventory().getItemInMainHand();
		} else {
			item = this.player.getItemInHand();
		}
		ItemMeta im = item.getItemMeta();
		if (im.getLore() == null) {
			this.lore.set(0, " ");
			this.lore.set(1, " ");
			this.lore.set(2, " ");
		}
		this.lore.set(2, name);
		im.setLore(lore);
		item.setItemMeta(im);
		this.player.sendMessage(Utils.getPrefix() + " name value set: " + name);
	}

	/**
	 * 
	 * @param Shop
	 *            Name
	 * 
	 *            Save's the specified shop to the config
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	public void saveShop() {

		Integer index = 0;
		for (ItemStack itm : this.chest.getInventory().getContents()) {
			List<Map> citem = new ArrayList<>(this.chest.getInventory().getContents().length);
			if (itm != null) {
				if (itm.hasItemMeta()) {
					index += 1;
					Map<String, String> iName = new HashMap<>();
					Map<String, Double> iBuy = new HashMap<>();
					Map<String, Double> iSell = new HashMap<>();
					Map<String, String> iItemID = new HashMap<>();
					Map<String, String> iEnchantments = new HashMap<>();
					Map<String, Integer> iSlot = new HashMap<>();
					Map<String, Integer> iQty = new HashMap<>();
					ItemMeta im = itm.getItemMeta();
					List<String> lore = im.getLore();
					Double price = 0.0;
					Double sell = 0.0;
					String name = "";
					if (!lore.get(0).equalsIgnoreCase(" ")) {
						price = Double.parseDouble(lore.get(0));
					}

					if (!lore.get(1).equalsIgnoreCase(" ")) {
						sell = Double.parseDouble(lore.get(1));
					}

					if (!lore.get(2).equalsIgnoreCase(" ")) {
						name = lore.get(2);
					}

					String itemID = Integer.toString(itm.getTypeId());
					String data = Byte.toString(itm.getData().getData());
					itemID = itemID + ":" + data;

					String enchantments = "";

					for (Enchantment ench : itm.getEnchantments().keySet()) {
						enchantments += ench.getName() + ":" + itm.getEnchantmentLevel(ench) + " ";
					}
					iItemID.put("id", itemID);
					iName.put("name", name);
					iBuy.put("buy-price", price);
					iSell.put("sell-price", sell);
					iSlot.put("slot", index - 1);
					iEnchantments.put("enchantments", enchantments.trim());
					iQty.put("qty", itm.getAmount());

					citem.add(iItemID);
					citem.add(iSlot);
					citem.add(iName);
					citem.add(iBuy);
					citem.add(iSell);
					citem.add(iEnchantments);
					citem.add(iQty);

					Main.getInstance().getCustomConfig().set(this.name + "." + index, citem);

					for (Player p : Bukkit.getOnlinePlayers()) {
						Main.MENUS.get(p.getName()).load();
					}
				} else {

				}
			} else {
				Main.getInstance().getCustomConfig().set(this.name + ".index", null);
			}
		}
		try {
			Main.getInstance().getCustomConfig().save(Main.getInstance().specialf);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Main.INSTANCE.reloadConfig();
		Main.INSTANCE.createFiles();
		Main.INSTANCE.loadDefaults();
		Shop.loadShops();
		player.sendMessage(Utils.getPrefix() + " Shop saved!");
	}

	/**
	 * 
	 * @param Shop
	 *            name
	 * 
	 *            Loads the specific shop from the config
	 */
	@SuppressWarnings("deprecation")
	public void loadShop() {
		Boolean hasData = false;
		this.chest.getInventory().clear();
		for (String str : Main.getInstance().getCustomConfig().getKeys(true)) {
			if (str.contains(".") && str.contains(this.name)) {
				Item item = new Item();
				List<Map<?, ?>> citem = Main.getInstance().getCustomConfig().getMapList(str);
				for (Map<?, ?> map : citem) {
					if (map.containsKey("id")) {
						if (((String) map.get("id")).contains(":")) {
							hasData = true;
							String itemID = (String) map.get("id");
							itemID = StringUtils.substringBefore(itemID, ":");
							String data = (String) map.get("id");
							data = StringUtils.substringAfter(data, ":");
							item.setId(Integer.parseInt(itemID));
							item.setData(Integer.parseInt(data));
						} else {
							item.setId(Integer.parseInt((String) map.get("id")));
						}
					} else if (map.containsKey("slot")) {
						item.setSlot((Integer) map.get("slot"));
					} else if (map.containsKey("name")) {
						item.setName((String) map.get("name"));
					} else if (map.containsKey("buy-price")) {
						Integer buy;
						Double buy2;
						try {
							buy = (Integer) map.get("buy-price");
							item.setBuyPrice(buy);
						} catch (Exception e) {
							buy2 = (Double) map.get("buy-price");
							item.setBuyPrice(buy2);
						}
					} else if (map.containsKey("sell-price")) {
						Integer sell;
						Double sell2;
						if (hasData == true) {
							Main.PRICETABLE.get(this.name).put(item.getId() + ":" + item.getData(),
									new Price(item.getBuyPrice(), item.getSellPrice(), item.getQty()));
						} else {
							Main.PRICETABLE.get(this.name).put(Integer.toString(item.getId()),
									new Price(item.getBuyPrice(), item.getSellPrice(), item.getQty()));
						}

						try {
							sell = (Integer) map.get("sell-price");
							item.setSellPrice(sell);
						} catch (Exception e) {
							sell2 = (Double) map.get("sell-price");
							item.setSellPrice(sell2);
						}
					}
				}

				ItemStack itemStack = new ItemStack(Material.AIR);
				if (hasData == true) {
					itemStack = new ItemStack(item.getId(), 1, (short) item.getData());
				} else {
					itemStack = new ItemStack(item.getId(), 1);
				}

				ItemMeta itemMeta = itemStack.getItemMeta();

				itemMeta.setLore(Arrays.asList(Double.toString(item.getBuyPrice()),
						Double.toString(item.getSellPrice()), item.getName()));

				if (item.getName() != null)
					itemMeta.setDisplayName(item.getName());
				itemStack.setItemMeta(itemMeta);

				if (item.getEnchantments() != null) {

					for (String enc : item.getEnchantments()) {
						String enchantment = StringUtils.substringBefore(enc, ":");
						String level = StringUtils.substringAfter(enc, ":");
						itemStack.addUnsafeEnchantment(Enchantment.getByName(enchantment), Integer.parseInt(level));
					}
				}
				this.chest.getInventory().setItem(item.getSlot(), itemStack);
				if (!Utils.getEscapeOnly()) {
					int backButton = 0;
					short data = 0;

					String backButtonId = Main.INSTANCE.getConfig().getString("back-button-item");

					if (backButtonId.contains(":")) {
						String[] args = backButtonId.split(":");

						backButton = Integer.parseInt(args[0]);
						data = Short.parseShort(args[1]);
					}

					ItemStack backButtonItem = new ItemStack(Material.getMaterial(backButton), 1, data);

					ItemMeta backButtonMeta = backButtonItem.getItemMeta();

					backButtonMeta.setDisplayName(
							ChatColor.translateAlternateColorCodes('&', Main.INSTANCE.getConfig().getString("back")));

					backButtonItem.setItemMeta(backButtonMeta);

					this.chest.getInventory().setItem(this.chest.getInventory().getSize(), backButtonItem);
				}

			}
		}
		player.sendMessage(Utils.getPrefix() + " Shop loaded!");
	}

}
