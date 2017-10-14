package com.pablo67340.guishop.handlers;

public class Page {

	/**
	 * Item array containing all items loaded from shops.yml per shop.
	 */
	private Item[] contents;

	/**
	 * Placeholder constructor.
	 */
	public Page() {

	}

	/**
	 * Get the contents of the Item array
	 * 
	 * @return Items
	 */
	public Item[] getContents() {
		return contents;
	}

	/**
	 * Set the contents of the {@link Shop}'s {@link Page}
	 * 
	 * @param Items
	 */
	public void setContents(Item[] inventoryContents) {
		contents = new Item[inventoryContents.length];
		contents = inventoryContents;
		
	}

}
