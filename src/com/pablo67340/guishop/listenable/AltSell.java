package com.pablo67340.guishop.listenable;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.definition.AltSellPane;
import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.util.Config;

public class AltSell {

	private final Item subjectItem;
	private final Gui gui;

	private AltSellPane pane;
	
	private final Item indicatorItem;
	private final Item addItem;
	private final Item removeItem;
	private final Item confirmItem;
	private final Item cancelItem;
	
	public AltSell(Item subjectItem) {
		this.subjectItem = subjectItem;
		gui = new Gui(Main.getINSTANCE(), 6, ChatColor.translateAlternateColorCodes('&', Config.getAltSellTitle()));
		indicatorItem = new Item();
		indicatorItem.setMaterial(Config.getAltSellIndicatorMaterial());
		addItem = new Item();
		addItem.setMaterial(Config.getAltSellAddMaterial());
		removeItem = new Item();
		removeItem.setMaterial(Config.getAltSellRemoveMaterial());
		confirmItem = new Item();
		confirmItem.setMaterial(Config.getAltSellConfirmMaterial());
		cancelItem = new Item();
		cancelItem.setMaterial(Config.getAltSellCancelMaterial());
	}
	
	private GuiItem setQuantityAndGet(ItemStack item, int quantity) {
		item.setAmount(quantity);
		return new GuiItem(item);
	}
	
	public void open(Player player) {
		if (!player.hasPermission("guishop.sell")) {
			player.sendMessage(Config.getNoPermission());
			return;
		}
		GuiItem gItem = subjectItem.parseMaterial();
		GuiItem gIndicator = indicatorItem.parseMaterial();
		GuiItem gAddItem = addItem.parseMaterial();
		GuiItem gRemoveItem = removeItem.parseMaterial();
		GuiItem gConfirmItem = confirmItem.parseMaterial();
		GuiItem gCancelItem = cancelItem.parseMaterial();
		if (gItem != null && gIndicator != null && gAddItem != null && gRemoveItem != null && gConfirmItem != null
				&& gCancelItem != null) {
			GuiItem[] addRemoveItems = new GuiItem[6];
			ItemStack addItem = gAddItem.getItem();
			addRemoveItems[0] = setQuantityAndGet(addItem.clone(), Config.getAltSellQuantity1());
			addRemoveItems[1] = setQuantityAndGet(addItem.clone(), Config.getAltSellQuantity2());
			addRemoveItems[2] = setQuantityAndGet(addItem.clone(), Config.getAltSellQuantity3());
			ItemStack removeItem = gRemoveItem.getItem();
			addRemoveItems[3] = setQuantityAndGet(removeItem.clone(), Config.getAltSellQuantity1());
			addRemoveItems[4] = setQuantityAndGet(removeItem.clone(), Config.getAltSellQuantity2());
			addRemoveItems[5] = setQuantityAndGet(removeItem.clone(), Config.getAltSellQuantity3());
			pane = new AltSellPane(gItem, addRemoveItems, gIndicator,
					Item.renameGuiItem(gConfirmItem, Config.getAltSellConfirmName()), Item.renameGuiItem(gCancelItem, Config.getAltSellCancelName()));
			pane.setSubjectQuantity(1);
			pane.setIndicatorName(subjectItem.getSellLore(1));
			gui.addPane(pane);
			gui.setOnTopClick(this::onClick);
			gui.setOnBottomClick(event -> event.setCancelled(true));
			gui.show(player);
		} else {
			Main.log("One or more of the materials you defined in the alt sell GUI are not valid.");
		}
	}
	
	private void changeQuantity(int delta) {
		int previous = pane.getSubjectQuantity();
		int update = previous + delta;
		if (update < 1) {
			update = 1;
		}
		update = pane.setSubjectQuantity(update);
		if (update != previous) {
			pane.setIndicatorName(subjectItem.getSellLore(update));
			gui.update();
		}
	}
	
	private void sell(Player player, ItemStack itemStack) {
		// remove IF's IF-uuid NBT tag
		NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
		comp.remove("IF-uuid");
		itemStack = ItemNBTUtil.setNBTTag(comp, itemStack);

		Main.debugLog(itemStack.toString());

		int amount = itemStack.getAmount();
		Map<Integer, ItemStack> result = player.getInventory().removeItem(itemStack);
		if (result.isEmpty()) {

			Sell.roundAndGiveMoney(player, subjectItem.calculateSellPrice(amount));
			// buy price must be defined for dynamic pricing to work
			if (subjectItem.hasBuyPrice() && Config.isDynamicPricing()) {
				Main.getDYNAMICPRICING().sellItem(subjectItem.getItemString(), amount);
			}
		} else {
			ItemStack addBack = result.get(0).clone();
			addBack.setAmount(amount - addBack.getAmount());
			if (addBack.getAmount() > 0) {
				player.getInventory().addItem(addBack);
			}
			player.sendMessage(Config.getAltSellNotEnough().replace("{amount}", Integer.toString(amount)));
		}
	}
	
	private void onClick(InventoryClickEvent evt) {
		evt.setCancelled(true);
		switch (evt.getSlot()) {
		case 18:
		case 19:
		case 20:
			changeQuantity(evt.getCurrentItem().getAmount());
			break;
		case 24:
		case 25:
		case 26:
			changeQuantity(-evt.getCurrentItem().getAmount());
			break;
		case 48:
			sell((Player) evt.getWhoClicked(), evt.getInventory().getItem(13));
			break;
		case 50:
			evt.getWhoClicked().closeInventory();
			break;
		default:
			break;
		}
	}
	
}
