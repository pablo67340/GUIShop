package com.pablo67340.guishop.neoforge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pablo67340.guishop.config.BaseConfig;
import com.pablo67340.guishop.config.WorthConfig;
import com.pablo67340.guishop.platform.GuiShopPlatform;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's own settings, read from {@code settings.json} in the data folder.
 * <p>
 * This is the NeoForge counterpart to the plugin's {@code config.yml}, trimmed
 * to the settings that still mean something. Everything that only existed to
 * lay out a chest inventory is gone: sign shops, the escape-to-go-back
 * switches, the alternative sell GUI's button materials, the chest window
 * titles and the Bukkit command-registration mode. Those are not hidden, they
 * genuinely have nothing to act on here.
 * <p>
 * The file is created with defaults on first start, and keys it does not define
 * keep their defaults rather than becoming null.
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

    // ------------------------------------------------------------------ specs

    /** How the settings screen should present a setting. */
    public enum Kind {
        FLAG,
        TEXT,
        NUMBER,

        /**
         * A registered sound id. Stored as text like any other, but the UI
         * offers the list rather than leaving it to be spelled correctly.
         */
        SOUND
    }

    /**
     * Which tab a setting belongs on.
     */
    public enum Category {
        GENERAL("General"),
        SHOPS("Shops"),
        CURRENCY("Money"),
        WORTH("Worth", true),
        LORE("Prices", true),
        MESSAGES("Messages", true);

        private final String label;
        private final boolean longText;

        Category(String label) {
            this(label, false);
        }

        Category(String label, boolean longText) {
            this.label = label;
            this.longText = longText;
        }

        public String label() {
            return label;
        }

        /**
         * Whether this tab's values are long format strings, which want a
         * full-width box under the label rather than a narrow one beside it.
         */
        public boolean isLongText() {
            return longText;
        }
    }

    /**
     * One setting as the in-game screen shows it.
     *
     * @param key      the settings.json key, dotted for nested ones
     * @param label    what to call it on screen
     * @param kind     which control to draw
     * @param hint     a short explanation, shown under the label
     * @param category which tab it belongs on
     */
    public record Spec(String key, String label, Kind kind, String hint, Category category) {
    }

    /**
     * Every setting the in-game screen can change.
     * <p>
     * Rebuilt per call rather than cached because the message list is derived
     * from {@link Messages}, and this is only read when a screen opens.
     */
    public static List<Spec> editable() {
        List<Spec> specs = new ArrayList<>(List.of(
                new Spec("debug-mode", "Debug logging", Kind.FLAG,
                        "Write detailed diagnostics to the debug log", Category.GENERAL),
                new Spec("transaction-log", "Transaction log", Kind.FLAG,
                        "Record every purchase and sale to a log file", Category.GENERAL),
                new Spec("sound-enabled", "Trade sound", Kind.FLAG,
                        "Play a sound when a trade goes through", Category.GENERAL),
                new Spec("sound", "Sound", Kind.SOUND,
                        "Which sound to play; the list can be listened to",
                        Category.GENERAL),
                new Spec("disabled-worlds", "Disabled worlds", Kind.TEXT,
                        "Comma-separated dimension ids the shop cannot be opened in",
                        Category.GENERAL),
                new Spec("sell-skull-uuid", "Match head owners", Kind.FLAG,
                        "Only sell a player head if its owner matches the config",
                        Category.GENERAL),

                new Spec("titles.menu", "Menu title", Kind.TEXT,
                        "Heading on the main menu, with & colour codes", Category.SHOPS),
                new Spec("abbreviate-prices", "Abbreviate prices", Kind.FLAG,
                        "Show 1.5K instead of 1,500", Category.SHOPS),
                new Spec("show-market-info", "Show price movement", Kind.FLAG,
                        "Mark prices that have drifted from their base", Category.SHOPS),
                new Spec("hide-non-buyable", "Hide sell-only items", Kind.FLAG,
                        "Leave items with no buy price out of shops", Category.SHOPS),
                new Spec("dynamic-pricing", "Dynamic pricing", Kind.FLAG,
                        "Let supply and demand move prices. Not yet active on NeoForge",
                        Category.SHOPS),

                new Spec("currency.prefix", "Currency prefix", Kind.TEXT,
                        "Goes before the amount, such as $", Category.CURRENCY),
                new Spec("currency.suffix", "Currency suffix", Kind.TEXT,
                        "Goes after the amount, such as coins", Category.CURRENCY),
                new Spec("currency.starting-balance", "Starting balance", Kind.NUMBER,
                        "What a new player's account opens at", Category.CURRENCY),

                new Spec("worth.enabled", "Show worth in tooltips", Kind.FLAG,
                        "Add sell values to item tooltips everywhere in the game",
                        Category.WORTH),
                new Spec("worth.format", "Worth line", Kind.TEXT,
                        "%worth%, %worth_single%, %amount%", Category.WORTH),
                new Spec("worth.only-sellable", "Only priced items", Kind.FLAG,
                        "Say nothing at all about an item no shop buys", Category.WORTH),
                new Spec("worth.not-sellable-format", "Unsellable line", Kind.TEXT,
                        "Shown instead, when the setting above is off", Category.WORTH),
                new Spec("worth.position-top", "Put it near the top", Kind.FLAG,
                        "Under the item's name rather than at the end of the tooltip",
                        Category.WORTH),
                new Spec("worth.blank-line", "Separate it", Kind.FLAG,
                        "Leave an empty line between the worth and the rest", Category.WORTH),
                new Spec("worth.hidden-names", "Skip items named", Kind.TEXT,
                        "Comma-separated text; an item whose name contains any of it"
                                + " gets no worth line", Category.WORTH),
                new Spec("worth.hidden-lore", "Skip items with lore", Kind.TEXT,
                        "Comma-separated text matched against the existing tooltip",
                        Category.WORTH)));

        // Derived rather than listed, so adding a lore template or a message in
        // one place puts it on the screen too.
        defaultLores().keySet().forEach(key -> specs.add(new Spec(
                "lores." + key, readableKey(key), Kind.TEXT,
                lorePlaceholders(key), Category.LORE)));

        Messages.keys().forEach(key -> specs.add(new Spec(
                "messages." + key, readableKey(key), Kind.TEXT,
                "", Category.MESSAGES)));

        return List.copyOf(specs);
    }

    /**
     * Turns a config key into a label, since spelling every one out twice would
     * only be a chance for them to disagree.
     */
    private static String readableKey(String key) {
        String spaced = key.replace('-', ' ').replace('.', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /**
     * What a price line may substitute, which is the part that is easy to get
     * wrong by hand.
     */
    private static String lorePlaceholders(String key) {
        return switch (key) {
            case "buy-market", "sell-market" -> "%amount%, %base_amount%, %trend%";
            case "trend-up", "trend-down" -> "%change_percent%";
            case "free", "cannot-buy", "cannot-sell" -> "no placeholders";
            default -> "%amount%";
        };
    }

    /**
     * The current value of every editable setting, as the screen should show
     * it. Sent to permitted players so the screen does not need its own copy of
     * the file.
     */
    public static Map<String, String> currentValues() {
        Map<String, String> values = new LinkedHashMap<>();

        values.put("debug-mode", String.valueOf(BaseConfig.isDebugMode()));
        values.put("transaction-log", String.valueOf(BaseConfig.isTransactionLog()));
        values.put("sound-enabled", String.valueOf(BaseConfig.isSoundEnabled()));
        values.put("sound", nullToEmpty(BaseConfig.getSound()));
        values.put("disabled-worlds", String.join(", ", disabledWorlds()));
        values.put("sell-skull-uuid", String.valueOf(BaseConfig.isSellSkullUUID()));

        values.put("titles.menu", nullToEmpty(BaseConfig.getTitlesConfig().getMenuTitle()));
        values.put("abbreviate-prices", String.valueOf(BaseConfig.isAbbreviatePrices()));
        values.put("show-market-info", String.valueOf(BaseConfig.isShowMarketInfo()));
        values.put("hide-non-buyable", String.valueOf(BaseConfig.isHideNonBuyable()));
        values.put("dynamic-pricing", String.valueOf(BaseConfig.isDynamicPricing()));

        values.put("currency.prefix", currencyPrefix);
        values.put("currency.suffix", currencySuffix);
        values.put("currency.starting-balance", startingBalance.toPlainString());

        values.put("worth.enabled", String.valueOf(WorthConfig.isEnabled()));
        values.put("worth.format", nullToEmpty(WorthConfig.getFormat()));
        values.put("worth.only-sellable", String.valueOf(WorthConfig.isOnlyShowSellable()));
        values.put("worth.not-sellable-format", nullToEmpty(WorthConfig.getNotSellableFormat()));
        values.put("worth.position-top", String.valueOf(WorthConfig.isPositionTop()));
        values.put("worth.blank-line", String.valueOf(WorthConfig.isAddBlankLine()));
        values.put("worth.hidden-names", String.join(", ", WorthConfig.getBlacklistedItemNames()));
        values.put("worth.hidden-lore", String.join(", ", WorthConfig.getIgnoreLoreContaining()));

        BaseConfig.getLoreConfig().lores.forEach((key, value) ->
                values.put("lores." + key, nullToEmpty(value)));

        Messages.all().forEach((key, value) -> values.put("messages." + key, value));

        return values;
    }

    /**
     * A null list reads as empty everywhere rather than being guarded at each
     * use.
     */
    private static List<String> disabledWorlds() {
        List<String> worlds = BaseConfig.getDisabledWorlds();
        return worlds == null ? List.of() : worlds;
    }

    // ------------------------------------------------------------------ apply

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
        List<String> allowed = editable().stream().map(Spec::key).toList();

        int changed = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();

            if (!allowed.contains(key) || value.equals(before.get(key))) {
                continue;
            }
            if (set(key, value)) {
                changed++;
            }
        }

        if (changed > 0) {
            write();
        }
        return changed;
    }

    /**
     * @return whether the value was accepted, so an unparseable number does not
     *         count as a change
     */
    private static boolean set(String key, String value) {
        if (key.startsWith("lores.")) {
            BaseConfig.getLoreConfig().lores.put(key.substring("lores.".length()), value);
            return true;
        }
        if (key.startsWith("messages.")) {
            return Messages.set(key.substring("messages.".length()), value);
        }

        switch (key) {
            case "debug-mode" -> BaseConfig.setDebugMode(flag(value));
            case "transaction-log" -> BaseConfig.setTransactionLog(flag(value));
            case "sound-enabled" -> BaseConfig.setSoundEnabled(flag(value));
            case "sound" -> BaseConfig.setSound(value.trim());
            case "disabled-worlds" -> BaseConfig.setDisabledWorlds(splitList(value));
            case "sell-skull-uuid" -> BaseConfig.setSellSkullUUID(flag(value));

            case "titles.menu" -> BaseConfig.getTitlesConfig().setMenuTitle(value);
            case "abbreviate-prices" -> BaseConfig.setAbbreviatePrices(flag(value));
            case "show-market-info" -> BaseConfig.setShowMarketInfo(flag(value));
            case "hide-non-buyable" -> BaseConfig.setHideNonBuyable(flag(value));
            case "dynamic-pricing" -> BaseConfig.setDynamicPricing(flag(value));

            case "worth.enabled" -> WorthConfig.setEnabled(flag(value));
            case "worth.format" -> WorthConfig.setFormat(value);
            case "worth.only-sellable" -> WorthConfig.setOnlyShowSellable(flag(value));
            case "worth.not-sellable-format" -> WorthConfig.setNotSellableFormat(value);
            case "worth.position-top" -> WorthConfig.setPosition(flag(value) ? "TOP" : "BOTTOM");
            case "worth.blank-line" -> WorthConfig.setAddBlankLine(flag(value));
            case "worth.hidden-names" -> WorthConfig.setBlacklistedItemNames(splitPatterns(value));
            case "worth.hidden-lore" -> WorthConfig.setIgnoreLoreContaining(splitPatterns(value));

            case "currency.prefix" -> currencyPrefix = value;
            case "currency.suffix" -> currencySuffix = value;
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

    private static boolean flag(String value) {
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Blank entries are dropped so a trailing comma does not become a world
     * named "".
     */
    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .map(entry -> entry.toLowerCase(Locale.ROOT))
                .toList();
    }

    /**
     * The same, keeping the author's capitalisation. These are matched against
     * item names case-insensitively, so lowercasing them would only make the
     * settings screen show "backpack" back to someone who typed "Backpack".
     */
    private static List<String> splitPatterns(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // ---------------------------------------------------------------- loading

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
            write();
            GuiShopPlatform.get().logger().info("Created " + FILE_NAME + " with default settings.");
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

    private static void applyDocument(Map<String, Object> document) {
        BaseConfig.setDebugMode(bool(document.get("debug-mode"), BaseConfig.isDebugMode()));
        BaseConfig.setTransactionLog(bool(document.get("transaction-log"), BaseConfig.isTransactionLog()));
        BaseConfig.setSoundEnabled(bool(document.get("sound-enabled"), BaseConfig.isSoundEnabled()));
        BaseConfig.setSellSkullUUID(bool(document.get("sell-skull-uuid"), BaseConfig.isSellSkullUUID()));
        BaseConfig.setAbbreviatePrices(bool(document.get("abbreviate-prices"), BaseConfig.isAbbreviatePrices()));
        BaseConfig.setShowMarketInfo(bool(document.get("show-market-info"), BaseConfig.isShowMarketInfo()));
        BaseConfig.setHideNonBuyable(bool(document.get("hide-non-buyable"), BaseConfig.isHideNonBuyable()));
        BaseConfig.setDynamicPricing(bool(document.get("dynamic-pricing"), BaseConfig.isDynamicPricing()));

        if (document.get("sound") != null) {
            BaseConfig.setSound(String.valueOf(document.get("sound")));
        }
        if (document.get("disabled-worlds") instanceof List<?> list) {
            BaseConfig.setDisabledWorlds(list.stream()
                    .map(entry -> String.valueOf(entry).toLowerCase(Locale.ROOT))
                    .toList());
        }

        Map<String, Object> currency = ShopLoader.map(document.get("currency"));
        if (currency != null) {
            currencyPrefix = ShopLoader.string(currency.get("prefix"), currencyPrefix);
            currencySuffix = ShopLoader.string(currency.get("suffix"), currencySuffix);
            startingBalance = decimal(currency.get("starting-balance"), startingBalance);
        }

        Map<String, Object> worth = ShopLoader.map(document.get("worth"));
        if (worth != null) {
            WorthConfig.setEnabled(bool(worth.get("enabled"), WorthConfig.isEnabled()));
            WorthConfig.setFormat(ShopLoader.string(worth.get("format"), WorthConfig.getFormat()));
            WorthConfig.setOnlyShowSellable(
                    bool(worth.get("only-sellable"), WorthConfig.isOnlyShowSellable()));
            WorthConfig.setNotSellableFormat(ShopLoader.string(
                    worth.get("not-sellable-format"), WorthConfig.getNotSellableFormat()));
            WorthConfig.setAddBlankLine(bool(worth.get("blank-line"), WorthConfig.isAddBlankLine()));
            if (worth.get("position-top") != null) {
                WorthConfig.setPosition(bool(worth.get("position-top"), false) ? "TOP" : "BOTTOM");
            }
            WorthConfig.setBlacklistedItemNames(stringList(worth.get("hidden-names")));
            WorthConfig.setIgnoreLoreContaining(stringList(worth.get("hidden-lore")));
        }

        Map<String, Object> titles = ShopLoader.map(document.get("titles"));
        if (titles != null) {
            BaseConfig.getTitlesConfig().setMenuTitle(
                    ShopLoader.string(titles.get("menu"), BaseConfig.getTitlesConfig().getMenuTitle()));
        }

        // Only keys the file actually defines are overridden, so a partial
        // block keeps the defaults for everything it leaves out.
        Map<String, Object> lores = ShopLoader.map(document.get("lores"));
        if (lores != null) {
            lores.forEach((key, value) -> {
                if (value != null) {
                    BaseConfig.getLoreConfig().lores.put(key, String.valueOf(value));
                }
            });
        }

        Messages.applyDocument(ShopLoader.map(document.get("messages")));
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

    /**
     * A list of strings from the file, which may be absent or a single string
     * rather than a list.
     */
    private static List<String> stringList(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(entry -> !entry.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        if (raw == null || String.valueOf(raw).isBlank()) {
            return new ArrayList<>();
        }
        return splitPatterns(String.valueOf(raw));
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
        BaseConfig.setTransactionLog(false);
        BaseConfig.setSoundEnabled(true);
        BaseConfig.setSound("entity.experience_orb.pickup");
        BaseConfig.setDisabledWorlds(List.of());
        BaseConfig.setSellSkullUUID(false);

        BaseConfig.setAbbreviatePrices(true);
        BaseConfig.setShowMarketInfo(false);
        BaseConfig.setHideNonBuyable(false);
        BaseConfig.setDynamicPricing(false);

        currencyPrefix = "$";
        currencySuffix = "";
        startingBalance = BigDecimal.valueOf(1000);

        // The plugin's worth.yml defaults, minus the settings that only
        // existed because it was rewriting lore inside packets: the inventory
        // blacklist, the armour slots and the player-inventory-only switch all
        // described where the rewriting was allowed to happen, and nothing is
        // being rewritten here.
        WorthConfig.setEnabled(true);
        WorthConfig.setFormat("&7Worth: &a%worth%");
        WorthConfig.setOnlyShowSellable(true);
        WorthConfig.setNotSellableFormat("&7Worth: &cNot sellable");
        WorthConfig.setPosition("BOTTOM");
        WorthConfig.setAddBlankLine(true);
        WorthConfig.setBlacklistedItemNames(new ArrayList<>());
        WorthConfig.setIgnoreLoreContaining(new ArrayList<>());

        BaseConfig.getTitlesConfig().setMenuTitle("&8&lShop Menu");
        BaseConfig.getLoreConfig().lores.putAll(defaultLores());

        Messages.reset();
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

    // --------------------------------------------------------------- writing

    /**
     * Writes the settings as they currently stand.
     * <p>
     * Built from the live values rather than from the file, so a setting
     * changed in game is written alongside everything that was loaded. One
     * builder serves both the first-run file and later saves, so the two cannot
     * drift apart.
     */
    private static void write() {
        Map<String, Object> document = new LinkedHashMap<>();

        document.put("debug-mode", BaseConfig.isDebugMode());
        document.put("transaction-log", BaseConfig.isTransactionLog());
        document.put("sound-enabled", BaseConfig.isSoundEnabled());
        document.put("sound", nullToEmpty(BaseConfig.getSound()));
        document.put("disabled-worlds", disabledWorlds());
        document.put("sell-skull-uuid", BaseConfig.isSellSkullUUID());

        document.put("abbreviate-prices", BaseConfig.isAbbreviatePrices());
        document.put("show-market-info", BaseConfig.isShowMarketInfo());
        document.put("hide-non-buyable", BaseConfig.isHideNonBuyable());
        document.put("dynamic-pricing", BaseConfig.isDynamicPricing());

        Map<String, Object> currency = new LinkedHashMap<>();
        currency.put("prefix", currencyPrefix);
        currency.put("suffix", currencySuffix);
        currency.put("starting-balance", startingBalance);
        document.put("currency", currency);

        Map<String, Object> worth = new LinkedHashMap<>();
        worth.put("enabled", WorthConfig.isEnabled());
        worth.put("format", nullToEmpty(WorthConfig.getFormat()));
        worth.put("only-sellable", WorthConfig.isOnlyShowSellable());
        worth.put("not-sellable-format", nullToEmpty(WorthConfig.getNotSellableFormat()));
        worth.put("position-top", WorthConfig.isPositionTop());
        worth.put("blank-line", WorthConfig.isAddBlankLine());
        worth.put("hidden-names", WorthConfig.getBlacklistedItemNames());
        worth.put("hidden-lore", WorthConfig.getIgnoreLoreContaining());
        document.put("worth", worth);

        Map<String, Object> titles = new LinkedHashMap<>();
        titles.put("menu", BaseConfig.getTitlesConfig().getMenuTitle());
        document.put("titles", titles);

        document.put("lores", new LinkedHashMap<>(BaseConfig.getLoreConfig().lores));
        document.put("messages", Messages.all());

        JsonFiles.write(GuiShopPlatform.get().dataFolder().resolve(FILE_NAME), document);
    }
}
