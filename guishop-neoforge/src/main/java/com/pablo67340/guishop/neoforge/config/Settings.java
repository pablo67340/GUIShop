package com.pablo67340.guishop.neoforge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.platform.GuiShopPlatform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The mod's own settings, read from {@code settings.json} in the data folder.
 * <p>
 * This is the NeoForge counterpart to the plugin's {@code config.yml}, trimmed
 * to the settings that still mean something: anything that only existed to lay
 * out a chest inventory is gone. The file is created with defaults on first
 * start, and unknown keys are left alone so a newer file can be read by an
 * older build without losing anything.
 */
public final class Settings {

    private static final String FILE_NAME = "settings.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * The currency markers, which live alongside the lore templates rather than
     * in a message file as they do on Bukkit.
     */
    private static String currencyPrefix = "$";
    private static String currencySuffix = "";

    /** What a player's account is opened at, the first time they shop. */
    private static BigDecimal startingBalance = BigDecimal.valueOf(1000);

    private Settings() {
    }

    public static String getCurrencyPrefix() {
        return currencyPrefix;
    }

    public static String getCurrencySuffix() {
        return currencySuffix;
    }

    public static BigDecimal getStartingBalance() {
        return startingBalance;
    }

    /** How the settings screen should present a setting. */
    public enum Kind {
        FLAG,
        TEXT,
        NUMBER
    }

    /**
     * One setting as the in-game screen shows it.
     *
     * @param key   the settings.json key, dotted for nested ones
     * @param label what to call it on screen
     * @param kind  which control to draw
     * @param hint  a short explanation, shown under the control
     */
    public record Spec(String key, String label, Kind kind, String hint) {
    }

    /**
     * The settings the in-game screen can change, in display order.
     * <p>
     * Deliberately not everything in the file: the lore templates are long
     * format strings that are far easier to edit in a text editor than through
     * a game screen, and getting one wrong breaks every price line.
     */
    public static List<Spec> editable() {
        return List.of(
                new Spec("abbreviate-prices", "Abbreviate prices", Kind.FLAG,
                        "Show 1.5K instead of 1,500"),
                new Spec("show-market-info", "Show price movement", Kind.FLAG,
                        "Mark prices that have drifted from their base"),
                new Spec("hide-non-buyable", "Hide sell-only items", Kind.FLAG,
                        "Leave items with no buy price out of shops"),
                new Spec("dynamic-pricing", "Dynamic pricing", Kind.FLAG,
                        "Let supply and demand move prices"),
                new Spec("debug-mode", "Debug logging", Kind.FLAG,
                        "Write detailed diagnostics to the debug log"),
                new Spec("currency.prefix", "Currency prefix", Kind.TEXT,
                        "Goes before the amount, such as $"),
                new Spec("currency.suffix", "Currency suffix", Kind.TEXT,
                        "Goes after the amount, such as coins"),
                new Spec("currency.starting-balance", "Starting balance", Kind.NUMBER,
                        "What a new player's account opens at"),
                new Spec("titles.menu", "Menu title", Kind.TEXT,
                        "Heading on the main menu, with & colour codes"));
    }

    /**
     * The current value of every editable setting, as the screen should show
     * it. Sent to permitted players so the screen does not need its own copy
     * of the file.
     */
    public static Map<String, String> currentValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("abbreviate-prices", String.valueOf(BaseConfig.isAbbreviatePrices()));
        values.put("show-market-info", String.valueOf(BaseConfig.isShowMarketInfo()));
        values.put("hide-non-buyable", String.valueOf(BaseConfig.isHideNonBuyable()));
        values.put("dynamic-pricing", String.valueOf(BaseConfig.isDynamicPricing()));
        values.put("debug-mode", String.valueOf(BaseConfig.isDebugMode()));
        values.put("currency.prefix", currencyPrefix);
        values.put("currency.suffix", currencySuffix);
        values.put("currency.starting-balance", startingBalance.toPlainString());
        values.put("titles.menu", nullToEmpty(BaseConfig.getTitlesConfig().getMenuTitle()));
        return values;
    }

    /**
     * Applies settings changed in game and writes the file.
     * <p>
     * Only keys from {@link #editable()} are honoured, so a request cannot
     * reach a setting the screen does not offer.
     *
     * @return how many settings actually changed
     */
    public static int apply(Map<String, String> values) {
        Map<String, String> before = currentValues();
        Set<String> allowed = editable().stream().map(Spec::key).collect(Collectors.toSet());

        int changed = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();

            if (!allowed.contains(key) || value.equals(before.get(key))) {
                continue;
            }
            if (set(key, value)) {
                changed++;
            }
        }

        if (changed > 0) {
            writeCurrent();
        }
        return changed;
    }

    /**
     * @return whether the value was accepted, so an unparseable number does
     *         not count as a change
     */
    private static boolean set(String key, String value) {
        switch (key) {
            case "abbreviate-prices" -> BaseConfig.setAbbreviatePrices(Boolean.parseBoolean(value));
            case "show-market-info" -> BaseConfig.setShowMarketInfo(Boolean.parseBoolean(value));
            case "hide-non-buyable" -> BaseConfig.setHideNonBuyable(Boolean.parseBoolean(value));
            case "dynamic-pricing" -> BaseConfig.setDynamicPricing(Boolean.parseBoolean(value));
            case "debug-mode" -> BaseConfig.setDebugMode(Boolean.parseBoolean(value));
            case "currency.prefix" -> currencyPrefix = value;
            case "currency.suffix" -> currencySuffix = value;
            case "titles.menu" -> BaseConfig.getTitlesConfig().setMenuTitle(value);
            case "currency.starting-balance" -> {
                BigDecimal parsed = decimal(value, null);
                if (parsed == null || parsed.signum() < 0) {
                    return false;
                }
                startingBalance = parsed;
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Reads the settings file, writing it with defaults if it is absent.
     * <p>
     * Defaults are applied first either way, so a file that omits a key still
     * yields a usable value rather than a null that surfaces later as a missing
     * lore line.
     */
    public static void load() {
        applyDefaults();

        Path file = GuiShopPlatform.get().dataFolder().resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            writeDefaults(file);
            return;
        }

        Map<String, Object> document = read(file);
        if (document == null) {
            GuiShopPlatform.get().logger().warning(
                    "Using default settings because " + FILE_NAME + " could not be read.");
            return;
        }

        applyDocument(document);
    }

    // ---------------------------------------------------------------- reading

    private static void applyDocument(Map<String, Object> document) {
        BaseConfig.setDebugMode(bool(document.get("debug-mode"), BaseConfig.isDebugMode()));
        BaseConfig.setAbbreviatePrices(bool(document.get("abbreviate-prices"), BaseConfig.isAbbreviatePrices()));
        BaseConfig.setShowMarketInfo(bool(document.get("show-market-info"), BaseConfig.isShowMarketInfo()));
        BaseConfig.setHideNonBuyable(bool(document.get("hide-non-buyable"), BaseConfig.isHideNonBuyable()));
        BaseConfig.setDynamicPricing(bool(document.get("dynamic-pricing"), BaseConfig.isDynamicPricing()));

        Map<String, Object> currency = ShopLoader.map(document.get("currency"));
        if (currency != null) {
            currencyPrefix = ShopLoader.string(currency.get("prefix"), currencyPrefix);
            currencySuffix = ShopLoader.string(currency.get("suffix"), currencySuffix);
            startingBalance = decimal(currency.get("starting-balance"), startingBalance);
        }

        Map<String, Object> titles = ShopLoader.map(document.get("titles"));
        if (titles != null) {
            BaseConfig.getTitlesConfig().setMenuTitle(
                    ShopLoader.string(titles.get("menu"), BaseConfig.getTitlesConfig().getMenuTitle()));
        }

        // Only keys the file actually defines are overridden, so a partial
        // lores block keeps the defaults for everything it leaves out.
        Map<String, Object> lores = ShopLoader.map(document.get("lores"));
        if (lores != null) {
            lores.forEach((key, value) -> {
                if (value != null) {
                    BaseConfig.getLoreConfig().lores.put(key, String.valueOf(value));
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, new TypeToken<Map<String, Object>>() { }.getType());
        } catch (IOException | JsonSyntaxException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not read " + FILE_NAME + ": " + exception.getMessage());
            return null;
        }
    }

    private static boolean bool(Object raw, boolean fallback) {
        if (raw instanceof Boolean value) {
            return value;
        }
        return raw == null ? fallback : Boolean.parseBoolean(String.valueOf(raw).trim());
    }

    /**
     * Via toString rather than doubleValue, so a large balance written in the
     * file is not rounded by Gson's double on the way in.
     */
    private static BigDecimal decimal(Object raw, BigDecimal fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    // --------------------------------------------------------------- defaults

    /**
     * Mirrors the plugin's shipped config.yml, so a server moving across sees
     * the same prices and wording it already had.
     */
    private static void applyDefaults() {
        BaseConfig.setDebugMode(false);
        BaseConfig.setAbbreviatePrices(true);
        BaseConfig.setShowMarketInfo(false);
        BaseConfig.setHideNonBuyable(false);
        BaseConfig.setDynamicPricing(false);

        currencyPrefix = "$";
        currencySuffix = "";
        startingBalance = BigDecimal.valueOf(1000);

        BaseConfig.getTitlesConfig().setMenuTitle("&8&lShop Menu");

        BaseConfig.getLoreConfig().lores.putAll(defaultLores());
    }

    private static Map<String, String> defaultLores() {
        Map<String, String> lores = new LinkedHashMap<>();
        lores.put("buy", "&fBuy: &c%amount%");
        lores.put("sell", "&fSell: &c%amount%");
        lores.put("free", "&fBuy: &aFREE");
        lores.put("cannot-buy", "&cCannot buy");
        lores.put("cannot-sell", "&cCannot sell");
        lores.put("buy-market", "&fBuy: &m%base_amount%&r %trend% &c%amount%");
        lores.put("sell-market", "&fSell: &m%base_amount%&r %trend% &c%amount%");
        lores.put("trend-up", "&a\u25B2%change_percent%");
        lores.put("trend-down", "&c\u25BC%change_percent%");
        return lores;
    }

    /**
     * Writes the settings as they currently stand.
     * <p>
     * Built from the live values rather than from the file, so a setting
     * changed in game is written alongside everything that was loaded, and the
     * lore templates survive even though the screen cannot edit them.
     */
    private static void writeCurrent() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("debug-mode", BaseConfig.isDebugMode());
        document.put("abbreviate-prices", BaseConfig.isAbbreviatePrices());
        document.put("show-market-info", BaseConfig.isShowMarketInfo());
        document.put("hide-non-buyable", BaseConfig.isHideNonBuyable());
        document.put("dynamic-pricing", BaseConfig.isDynamicPricing());

        Map<String, Object> currency = new LinkedHashMap<>();
        currency.put("prefix", currencyPrefix);
        currency.put("suffix", currencySuffix);
        currency.put("starting-balance", startingBalance);
        document.put("currency", currency);

        Map<String, Object> titles = new LinkedHashMap<>();
        titles.put("menu", BaseConfig.getTitlesConfig().getMenuTitle());
        document.put("titles", titles);

        document.put("lores", new LinkedHashMap<>(BaseConfig.getLoreConfig().lores));

        JsonFiles.write(GuiShopPlatform.get().dataFolder().resolve(FILE_NAME), document);
    }

    private static void writeDefaults(Path file) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("debug-mode", BaseConfig.isDebugMode());
        document.put("abbreviate-prices", BaseConfig.isAbbreviatePrices());
        document.put("show-market-info", BaseConfig.isShowMarketInfo());
        document.put("hide-non-buyable", BaseConfig.isHideNonBuyable());
        document.put("dynamic-pricing", BaseConfig.isDynamicPricing());

        Map<String, Object> currency = new LinkedHashMap<>();
        currency.put("prefix", currencyPrefix);
        currency.put("suffix", currencySuffix);
        currency.put("starting-balance", startingBalance);
        document.put("currency", currency);

        Map<String, Object> titles = new LinkedHashMap<>();
        titles.put("menu", BaseConfig.getTitlesConfig().getMenuTitle());
        document.put("titles", titles);

        document.put("lores", defaultLores());

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(document, writer);
            }
            GuiShopPlatform.get().logger().info("Created " + FILE_NAME + " with default settings.");
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not create " + FILE_NAME + ": " + exception.getMessage());
        }
    }
}
