package com.pablo67340.guishop.definition;

import java.util.Calendar;
import java.util.Date;

public class Expires {
	
	
	private Date expiration;
	
	
	public Expires(Integer seconds, String unit, String startDate) {
		setExpiration(seconds, unit, startDate);
	}
	
	
	public Date getExpiration() {
		return expiration;
	}
	
	@SuppressWarnings("deprecation")
	public void setExpiration(Integer duration, String unit, String startDate) {
		int calUnit = 0;
		if (unit.equalsIgnoreCase("s")) {
			calUnit = Calendar.SECOND;
		}else if (unit.equalsIgnoreCase("h")) {
			calUnit = Calendar.HOUR;
		}else if (unit.equalsIgnoreCase("m")) {
			calUnit = Calendar.MINUTE;
		}
		Date date = new Date(startDate);
		Calendar cal = Calendar.getInstance();
	    cal.setTime(date);
	    cal.add(calUnit, duration);
	    expiration = cal.getTime();
	}
	
	public Boolean isExpired() {
		Date checkDate = new Date();
		return checkDate.after(expiration);
	}

}
