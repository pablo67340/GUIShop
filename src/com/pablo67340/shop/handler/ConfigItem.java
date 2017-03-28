package com.pablo67340.shop.handler;

import org.apache.commons.lang.StringUtils;

public class ConfigItem {

	private String name;
	
	private String shop;
	
	private String id;
	
	private String description;
	
	private int slot;
	
	private Double buy;
	
	private Double sell;
	
	private Boolean enabled;
	
	private Boolean isMenu;
	
	public void setName(String input){
		name = input;
	}
	
	public String getName(){
		return name;
	}
	
	public void setShop(String input){
		shop = input;
	}
	
	public String getShop(){
		return shop;
	}
	
	public void setDescription(String input){
		description = input;
	}
	
	public String getDescription(){
		return description;
	}
	
	public void setEnabled(Boolean input){
		enabled = input;
	}
	
	public Boolean getEnabled(){
		return enabled;
	}
	
	public void setIsMenu(Boolean input){
		isMenu = input;
	}
	
	public Boolean isMenu(){
		return isMenu;
	}
	
	public void setSlot(Integer input){
		slot = input;
	}
	
	public Integer getSlot(){
		return slot;
	}
	
	public void setID(String input){
		id = input;
	}
	
	public String getID(){
		return StringUtils.substringBefore(id, ":");
	}
	
	public String getData(){
		return StringUtils.substringAfter(id, ":");
	}
	
	public void setBuy(Double input){
		buy = input;
	}
	
	public Double getBuy(){
		return buy;
	}
	
	public void setSell(Double input){
		sell = input;
	}
	
	public Double getSell(){
		return sell;
	}
	
	
	
}
