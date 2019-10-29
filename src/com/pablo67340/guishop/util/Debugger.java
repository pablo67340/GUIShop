package com.pablo67340.guishop.util;

import lombok.Getter;
import lombok.Setter;

public class Debugger {

	/**
	 * The error message
	 * 
	 */
	@Getter
	@Setter
	private String errorMessage;

	/**
	 * True/False if fatal error
	 */
	@Getter
	@Setter
	private Boolean hasExploded;

	/**
	 * Default Constructor, Fatal false.
	 * 
	 */
	public Debugger() {
		hasExploded = false;
	}
}
