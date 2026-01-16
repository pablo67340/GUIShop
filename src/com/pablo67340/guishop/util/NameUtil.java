package com.pablo67340.guishop.util;

import com.pablo67340.guishop.GUIShop;

import java.util.*;

public class NameUtil {

    public static String nearestShop(String input) {
        List<String> availableShops = new ArrayList<>(GUIShop.getINSTANCE().getConfigManager().getShopNames());

        TreeMap<Double, String> sorted = new TreeMap<>();

        for (String shop : availableShops) {
            double distance = JaroWinklerDistance.apply(shop.toLowerCase(Locale.ROOT), input.toLowerCase(Locale.ROOT));

            if (distance < 0.90) continue;

            sorted.put(distance, shop);
        }

        return getOrNull(sorted.lastEntry());
    }

    public static String getOrNull(Map.Entry<Double, String> entry) {
        return entry == null ? null : entry.getValue();
    }

    public static class JaroWinklerDistance {

        public static Double apply(final CharSequence left, final CharSequence right) {
            final double defaultScalingFactor = 0.1;
            final double percentageRoundValue = 100.0;

            if (left == null || right == null) {
                throw new IllegalArgumentException("Strings must not be null");
            }

            int[] mtp = matches(left, right);
            double m = mtp[0];
            if (m == 0) {
                return 0D;
            }
            double j = ((m / left.length() + m / right.length() + (m - mtp[1]) / m)) / 3;
            double jw = j < 0.7D ? j : j + Math.min(defaultScalingFactor, 1D / mtp[3]) * mtp[2] * (1D - j);
            return Math.round(jw * percentageRoundValue) / percentageRoundValue;
        }

        protected static int[] matches(final CharSequence first, final CharSequence second) {
            CharSequence max, min;
            if (first.length() > second.length()) {
                max = first;
                min = second;
            } else {
                max = second;
                min = first;
            }
            int range = Math.max(max.length() / 2 - 1, 0);
            int[] matchIndexes = new int[min.length()];
            Arrays.fill(matchIndexes, -1);
            boolean[] matchFlags = new boolean[max.length()];
            int matches = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                char c1 = min.charAt(mi);
                for (int xi = Math.max(mi - range, 0), xn = Math.min(mi + range + 1, max.length()); xi < xn; xi++) {
                    if (!matchFlags[xi] && c1 == max.charAt(xi)) {
                        matchIndexes[mi] = xi;
                        matchFlags[xi] = true;
                        matches++;
                        break;
                    }
                }
            }
            char[] ms1 = new char[matches];
            char[] ms2 = new char[matches];
            for (int i = 0, si = 0; i < min.length(); i++) {
                if (matchIndexes[i] != -1) {
                    ms1[si] = min.charAt(i);
                    si++;
                }
            }
            for (int i = 0, si = 0; i < max.length(); i++) {
                if (matchFlags[i]) {
                    ms2[si] = max.charAt(i);
                    si++;
                }
            }
            int transpositions = 0;
            for (int mi = 0; mi < ms1.length; mi++) {
                if (ms1[mi] != ms2[mi]) {
                    transpositions++;
                }
            }
            int prefix = 0;
            for (int mi = 0; mi < min.length(); mi++) {
                if (first.charAt(mi) == second.charAt(mi)) {
                    prefix++;
                } else {
                    break;
                }
            }
            return new int[] { matches, transpositions / 2, prefix, max.length() };
        }

    }

    /**
     * Format a material name to be human-readable.
     * Example: "DIAMOND_SWORD" -> "Diamond Sword"
     * 
     * @param material The material name (uppercase with underscores)
     * @return Formatted name with proper capitalization
     */
    public static String formatMaterialName(String material) {
        if (material == null || material.isEmpty()) {
            return "";
        }
        
        // Split by underscores and capitalize each word
        String[] words = material.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        
        return result.toString();
    }

}
