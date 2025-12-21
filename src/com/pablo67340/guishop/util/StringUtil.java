package com.pablo67340.guishop.util;

import java.util.Collection;

/**
 * Simple string utility methods to replace Apache Commons Lang3 dependency.
 */
public final class StringUtil {

    private StringUtil() {
        // Utility class
    }

    /**
     * Check if a string is null, empty, or contains only whitespace.
     * Equivalent to Apache Commons StringUtils.isBlank()
     *
     * @param str the string to check
     * @return true if null, empty, or whitespace only
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if a string is not blank.
     * Equivalent to Apache Commons StringUtils.isNotBlank()
     *
     * @param str the string to check
     * @return true if not null, not empty, and not whitespace only
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Get the substring before the first occurrence of a separator.
     * Equivalent to Apache Commons StringUtils.substringBefore()
     *
     * @param str the string to search
     * @param separator the separator to find
     * @return the substring before the separator, or the original string if not found
     */
    public static String substringBefore(String str, String separator) {
        if (str == null || separator == null) {
            return str;
        }
        int pos = str.indexOf(separator);
        if (pos < 0) {
            return str;
        }
        return str.substring(0, pos);
    }

    /**
     * Get the substring after the first occurrence of a separator.
     * Equivalent to Apache Commons StringUtils.substringAfter()
     *
     * @param str the string to search
     * @param separator the separator to find
     * @return the substring after the separator, or empty string if not found
     */
    public static String substringAfter(String str, String separator) {
        if (str == null || separator == null) {
            return "";
        }
        int pos = str.indexOf(separator);
        if (pos < 0) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    /**
     * Join a collection of strings with a separator.
     * Equivalent to Apache Commons StringUtils.join()
     *
     * @param collection the collection to join
     * @param separator the separator to use
     * @return the joined string
     */
    public static String join(Collection<?> collection, String separator) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(separator);
            }
            sb.append(item);
            first = false;
        }
        return sb.toString();
    }
}
