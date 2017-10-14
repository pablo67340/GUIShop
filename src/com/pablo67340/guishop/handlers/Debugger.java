package com.pablo67340.guishop.handlers;

public class Debugger {

	/**
	 * The error message
	 * 
	 */
	private String errorMessage;

	/**
	 * True/False if fatal error
	 */
	private Boolean hasExploded;

	/**
	 * Default Constructor, Fatal false.
	 * 
	 */
	public Debugger() {
		hasExploded = false;
	}

	/**
	 * Set the {@link Debugger}'s error message.
	 * 
	 * @param Error
	 */
	public void setErrorMessage(String input) {
		errorMessage = input;
	}

	/**
	 * Get the {@link Debugger}'s error message.
	 * 
	 * @return Error
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Set the {@link Debugger}'s fatal value.
	 * 
	 * @param Fatal
	 */
	public void setHasExploded(Boolean input) {
		hasExploded = input;
	}

	/**
	 * Set the {@link Debugger}'s fatal value.
	 * 
	 * @return hasExploded
	 */
	public Boolean hasExploded() {
		return hasExploded;
	}

}
