package com.pablo67340.guishop.handlers;

public final class Utils {

	/**
	 * True/False if GUIShop should use signs only.
	 */
	private static boolean signsOnly;

	/**
	 * Common Language strings set in configuration.
	 * 
	 */
	@SuppressWarnings("unused")
	private static String added, cantSell, cantBuy, prefix, purchased, menuName, notEnoughPre, notEnoughPost, signTitle,
			sellCommand, sellTitle, sold, taken, sound, full, currency;

	/**
	 * Number of rows for the {@link Menu} GUI.
	 * 
	 */
	private static Integer menuRows;

	/**
	 * True/False to set escape only mode, purchase sounds, and creator mode.
	 * 
	 */
	private static Boolean escapeOnly, enableSound, enableCreator;

	/**
	 * The text that represents what comes before messages sent to the
	 * {@link Player}.
	 * <p>
	 * i.e. [GUIShop]
	 * 
	 * @return the {@link Shop}'s prefix.
	 */
	public static String getPrefix() {
		return prefix;
	}

	/**
	 * The message sent to the {@link Player} if an item cannot be purchased
	 * 
	 * @return the {@link Shop}'s prefix.
	 */
	public static String getCantBuy() {
		return cantBuy;
	}

	/**
	 * 
	 * @param Cant
	 *            Buy String
	 * 
	 *            Sets the string sent to player when an item cannot be purchased.
	 */
	public static void setCantBuy(String input) {
		cantBuy = input;
	}

	/**
	 * Gets the sound on purchase.
	 * 
	 * @return the sound name.
	 */
	public static String getSound() {
		return sound;
	}

	/**
	 * Checks if sound is enabled.
	 * 
	 * @return sound enabled.
	 */
	public static Boolean isSoundEnabled() {
		return enableSound;
	}

	/**
	 * enables the sound that plays when a purchase is made, around the
	 * {@link Player}.
	 * 
	 * @param input
	 *            The boolean to set.
	 */
	public static void setSoundEnabled(Boolean input) {
		enableSound = input;
	}

	/**
	 * Sets the sound that plays when a purchase is made, around the {@link Player}.
	 * 
	 * @param sound
	 *            The text to set.
	 */
	public static void setSound(String input) {
		sound = input;
	}

	/**
	 * Sets the text that represents what comes before messages sent to the
	 * {@link Player}.
	 * 
	 * @param prefix
	 *            The text to set.
	 */
	public static void setCreatorEnabled(Boolean input) {
		enableCreator = input;
	}

	/**
	 * Gets the creator boolean.
	 * 
	 * @return the creator boolean.
	 */
	public static Boolean getCreatorEnabled() {
		return enableCreator;
	}

	/**
	 * Sets the text that represents what comes before messages sent to the
	 * {@link Player}.
	 * 
	 * @param prefix
	 *            The text to set.
	 */
	public static void setPrefix(String text) {
		prefix = text;
	}

	/**
	 * Gets the name of the {@link Menu}.
	 * 
	 * @return the menu's name.
	 */
	public static String getMenuName() {
		return menuName;
	}

	/**
	 * Sets the name of the {@link Menu}.
	 * 
	 * @param text
	 *            The text to set.
	 */
	public static void setMenuName(String text) {
		menuName = text;
	}

	/**
	 * Gets the title of the {@link Sell} menu.
	 * 
	 * @return the title of the sell menu.
	 */
	public static String getSellTitle() {
		return sellTitle;
	}

	/**
	 * Sets the title of the {@link Sell} menu.
	 * 
	 * @param text
	 *            The text to set.
	 */
	public static void setSellTitle(String text) {
		sellTitle = text;
	}

	/**
	 * Gets the title on the sign that handles the {@link Shop}.
	 * 
	 * @return the sign's title.
	 */
	public static String getSignTitle() {
		return signTitle;
	}

	/**
	 * Sets the title on the sign that handles the {@link Shop}.
	 * 
	 * @param text
	 */
	public static void setSignTitle(String text) {
		signTitle = text;
	}

	/**
	 * Sets if shops close with escape handles the {@link Shop}.
	 * 
	 * @param boolean
	 */
	public static void setEscapeOnly(Boolean input) {
		escapeOnly = input;
	}

	/**
	 * Gets whether the {@link Shop} is closed with the escape key
	 * 
	 * @return {@code true} will be closed when the escape key is pressed,
	 *         otherwise, {@code false}.
	 */
	public static Boolean getEscapeOnly() {
		return escapeOnly;
	}

	/**
	 * Gets whether the {@link Shop} can only be opened from a sign.
	 * 
	 * @return {@code true} if the shop can only be opened from a sign, otherwise,
	 *         {@code false}.
	 */
	public static boolean isSignsOnly() {
		return signsOnly;
	}

	/**
	 * Sets whether the {@link Shop} can only be opened from a sign.
	 * 
	 * @param flag
	 *            The flag to set.
	 */
	public static void setSignsOnly(boolean flag) {
		signsOnly = flag;
	}

	/**
	 * Gets the response for not enough money.
	 * 
	 * @return Response String
	 */
	public static String getNotEnoughPre() {
		return notEnoughPre;
	}

	/**
	 * Sets the response for not enough money.
	 * 
	 */
	public static void setNotEnoughPre(String text) {
		notEnoughPre = text;
	}

	/**
	 * Gets the response for not enough money post.
	 * 
	 * @return The response string
	 */
	public static String getNotEnoughPost() {
		return notEnoughPost;
	}

	/**
	 * Sets the response for not enough money post.
	 * 
	 */
	public static void setNotEnoughPost(String text) {
		notEnoughPost = text;
	}

	/**
	 * Gets the response for item purchase.
	 * 
	 * @return The response string
	 */
	public static String getPurchased() {
		return purchased;
	}

	/**
	 * Sets the response for item purchased.
	 * 
	 */
	public static void setPurchased(String text) {
		purchased = text;
	}

	/**
	 * Gets the response for amount paid.
	 * 
	 * @return The response string
	 */
	public static String getTaken() {
		return taken;
	}

	/**
	 * Sets the response for amount paid.
	 * 
	 */
	public static void setTaken(String text) {
		taken = text;
	}

	/**
	 * Gets the response for item sold.
	 * 
	 * @return The response string
	 */
	public static String getSold() {
		return sold;
	}

	/**
	 * Sets the response for item sold.
	 * 
	 */
	public static void setSold(String text) {
		sold = text;
	}

	/**
	 * Gets the response for money received.
	 * 
	 * @return The response string
	 */
	public static String getAdded() {
		return added;
	}

	/**
	 * Sets the response for money received.
	 * 
	 */
	public static void setAdded(String text) {
		added = text;
	}

	/**
	 * Gets the response for cant sell.
	 * 
	 * @return The response string
	 */
	public static String getCantSell() {
		return cantSell;
	}

	/**
	 * Sets the response for cant sell.
	 * 
	 */
	public static void setCantSell(String text) {
		cantSell = text;
	}

	/**
	 * Gets the number of rows for the menu
	 * 
	 * @return The number of rows
	 */
	public static Integer getMenuRows() {
		return menuRows;
	}

	/**
	 * Sets the rows for the menu
	 * 
	 */
	public static void setMenuRows(Integer input) {
		menuRows = input;
	}

	/**
	 * Sets the full inventory message
	 * 
	 */
	public static void setFull(String input) {
		full = input;
	}

	/**
	 * Gets the rows for the menu
	 * 
	 */
	public static String getFull() {
		return full;
	}

	/**
	 * Sets the Currency symbol
	 * 
	 */
	public static void setCurrency(String input) {
		currency = input;
	}

	/**
	 * Gets the currency Symbol
	 * 
	 */
	public static String getCurrency() {
		return currency;
	}

}
