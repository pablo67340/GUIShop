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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private Settings() {
    }

    public static String getCurrencyPrefix() {
        return currencyPrefix;
    }

    public static String getCurrencySuffix() {
        return currencySuffix;
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

        apply(document);
    }

    // ---------------------------------------------------------------- reading

    private static void apply(Map<String, Object> document) {
        BaseConfig.setDebugMode(bool(document.get("debug-mode"), BaseConfig.isDebugMode()));
        BaseConfig.setAbbreviatePrices(bool(document.get("abbreviate-prices"), BaseConfig.isAbbreviatePrices()));
        BaseConfig.setShowMarketInfo(bool(document.get("show-market-info"), BaseConfig.isShowMarketInfo()));
        BaseConfig.setHideNonBuyable(bool(document.get("hide-non-buyable"), BaseConfig.isHideNonBuyable()));
        BaseConfig.setDynamicPricing(bool(document.get("dynamic-pricing"), BaseConfig.isDynamicPricing()));

        Map<String, Object> currency = ShopLoader.map(document.get("currency"));
        if (currency != null) {
            currencyPrefix = ShopLoader.string(currency.get("prefix"), currencyPrefix);
            currencySuffix = ShopLoader.string(currency.get("suffix"), currencySuffix);
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
