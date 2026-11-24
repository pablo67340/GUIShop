package com.pablo67340.guishop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathUtil {

    // Pattern to match numbers with optional suffix (1k, 1.5M, 2B, etc.)
    private static final Pattern ABBREVIATED_PATTERN = Pattern.compile(
        "^\\s*([+-]?[\\d,]+\\.?\\d*)\\s*([kKmMbBtT]?)\\s*$"
    );

    // Decimal formats for abbreviated output
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat ABBREVIATED_FORMAT = new DecimalFormat("#.##");

    public static double round(double value, int places) {
        if (places < 0) return Math.round(value);

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Parses a string that may contain abbreviated numbers.
     * Supports formats like:
     * - "1000" -> 1000
     * - "1,000" -> 1000
     * - "1k" or "1K" -> 1000
     * - "1.5k" -> 1500
     * - "1M" or "1m" -> 1,000,000
     * - "1B" or "1b" -> 1,000,000,000
     * - "1T" or "1t" -> 1,000,000,000,000
     * - "240,000" -> 240000
     *
     * @param input The string to parse
     * @return The parsed BigDecimal value
     * @throws NumberFormatException if the input cannot be parsed
     */
    public static BigDecimal parseAbbreviatedNumber(String input) throws NumberFormatException {
        if (input == null || input.trim().isEmpty()) {
            throw new NumberFormatException("Input is null or empty");
        }

        String trimmed = input.trim();
        
        // Remove commas from the number
        String noCommas = trimmed.replace(",", "");
        
        Matcher matcher = ABBREVIATED_PATTERN.matcher(noCommas);
        
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid number format: " + input);
        }

        String numberPart = matcher.group(1);
        String suffix = matcher.group(2).toUpperCase();

        BigDecimal value;
        try {
            value = new BigDecimal(numberPart);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number: " + numberPart);
        }

        // Apply multiplier based on suffix
        BigDecimal multiplier = getMultiplier(suffix);
        return value.multiply(multiplier);
    }

    /**
     * Gets the multiplier for a given suffix.
     */
    private static BigDecimal getMultiplier(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return BigDecimal.ONE;
        }
        
        return switch (suffix) {
            case "K" -> new BigDecimal("1000");
            case "M" -> new BigDecimal("1000000");
            case "B" -> new BigDecimal("1000000000");
            case "T" -> new BigDecimal("1000000000000");
            default -> BigDecimal.ONE;
        };
    }

    /**
     * Formats a number as an abbreviated string (e.g., 1.5k, 2.3M).
     *
     * @param value The value to format
     * @return The abbreviated string
     */
    public static String formatAbbreviated(double value) {
        return formatAbbreviated(BigDecimal.valueOf(value));
    }

    /**
     * Formats a BigDecimal as an abbreviated string (e.g., 1.5k, 2.3M).
     *
     * @param value The value to format
     * @return The abbreviated string
     */
    public static String formatAbbreviated(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        double absValue = value.abs().doubleValue();
        String sign = value.compareTo(BigDecimal.ZERO) < 0 ? "-" : "";

        if (absValue >= 1_000_000_000_000.0) {
            return sign + ABBREVIATED_FORMAT.format(absValue / 1_000_000_000_000.0) + "T";
        } else if (absValue >= 1_000_000_000.0) {
            return sign + ABBREVIATED_FORMAT.format(absValue / 1_000_000_000.0) + "B";
        } else if (absValue >= 1_000_000.0) {
            return sign + ABBREVIATED_FORMAT.format(absValue / 1_000_000.0) + "M";
        } else if (absValue >= 1_000.0) {
            return sign + ABBREVIATED_FORMAT.format(absValue / 1_000.0) + "k";
        } else {
            return ABBREVIATED_FORMAT.format(value.doubleValue());
        }
    }

    /**
     * Formats a number with commas (e.g., 1,234,567.89).
     *
     * @param value The value to format
     * @return The formatted string with commas
     */
    public static String formatWithCommas(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    /**
     * Formats a BigDecimal with commas (e.g., 1,234,567.89).
     *
     * @param value The value to format
     * @return The formatted string with commas
     */
    public static String formatWithCommas(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return DECIMAL_FORMAT.format(value.doubleValue());
    }

    /**
     * Attempts to parse an abbreviated number, returns null on failure.
     *
     * @param input The string to parse
     * @return The parsed BigDecimal, or null if parsing failed
     */
    public static BigDecimal tryParseAbbreviated(String input) {
        try {
            return parseAbbreviatedNumber(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if a string is a valid abbreviated number.
     *
     * @param input The string to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidAbbreviatedNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        String noCommas = input.trim().replace(",", "");
        return ABBREVIATED_PATTERN.matcher(noCommas).matches();
    }
}
