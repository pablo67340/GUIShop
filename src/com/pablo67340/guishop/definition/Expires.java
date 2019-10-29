package com.pablo67340.guishop.definition;

import java.util.Calendar;
import java.util.Date;

import lombok.Getter;

public class Expires {

    @Getter
    private Date expiration;

    Expires(Integer seconds, String unit, String startDate) {
        setExpiration(seconds, unit, startDate);

    }

    @SuppressWarnings("deprecation")
    private void setExpiration(Integer duration, String unit, String startDate) {
        int calUnit = 0;
        if (unit.equalsIgnoreCase("s")) {
            calUnit = Calendar.SECOND;
        } else if (unit.equalsIgnoreCase("h")) {
            calUnit = Calendar.HOUR;
        } else if (unit.equalsIgnoreCase("m")) {
            calUnit = Calendar.MINUTE;
        }
        Date date = new Date(startDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(calUnit, duration);
        expiration = cal.getTime();
    }

    public boolean isExpired() {
        Date checkDate = new Date();
        return checkDate.after(expiration);
    }

}
