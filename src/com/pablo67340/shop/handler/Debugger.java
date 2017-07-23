package com.pablo67340.shop.handler;

public class Debugger {

	
	// Well this class is supposed to be some cool debugger one day.
	// Maybe I should make it display exactly what you did wrong in your configs? 
	// You think that's too much code? Too overboard? Meh.. whatever
	// Might as well start the class now and see where it leads (kill me)
	private String errorMessage;
	
	private Boolean hasExploded;
	
	public Debugger(){
		hasExploded = false;
	}
	
	public void setErrorMessage(String input){
		errorMessage = input;
	}
	
	public String getErrorMessage(){
		return errorMessage;
	}
	
	public void setHasExploded(Boolean input){
		hasExploded = input;
	}
	
	public Boolean hasExploded(){
		return hasExploded;
	}
	
}
