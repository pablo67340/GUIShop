package com.pablo67340.guishop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {

    public static double round(double value, int places) {
        if (places < 0) return Math.round(value);

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
