package com.pablo67340.guishop.neoforge.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This player's own shop preferences, kept on their machine.
 * <p>
 * Separate from the server settings because they are nobody else's business:
 * how someone likes their shop list sorted should not need a permission, and
 * should follow them between servers.
 */
final class ClientPrefs {

    /** How the shop grid is ordered by default. */
    enum Sort {
        CONFIG("As configured"),
        NAME("Name"),
        PRICE_LOW("Cheapest first"),
        PRICE_HIGH("Dearest first");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        Sort next() {
            Sort[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    private static final String FILE_NAME = "guishop-client.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Sort sort = Sort.CONFIG;
    private static boolean confirmPurchases = true;
    private static boolean hideUnaffordable;
    private static boolean showWorth = true;

    private ClientPrefs() {
    }

    static Sort sort() {
        return sort;
    }

    static void setSort(Sort value) {
        sort = value;
        save();
    }

    static boolean confirmPurchases() {
        return confirmPurchases;
    }

    static void setConfirmPurchases(boolean value) {
        confirmPurchases = value;
        save();
    }

    static boolean hideUnaffordable() {
        return hideUnaffordable;
    }

    static void setHideUnaffordable(boolean value) {
        hideUnaffordable = value;
        save();
    }

    /**
     * Whether to write sell values into item tooltips.
     * <p>
     * Mine rather than the server's, which is the point: the plugin's
     * {@code /gs toggleworth} had to be a server command that remembered a
     * preference per player, because the server was drawing the lore. Here it
     * is a switch on the machine doing the drawing.
     */
    static boolean showWorth() {
        return showWorth;
    }

    static void setShowWorth(boolean value) {
        showWorth = value;
        save();
    }

    /** Flips the worth display and says what it is now. */
    static boolean toggleWorth() {
        setShowWorth(!showWorth);
        return showWorth;
    }

    /**
     * The preferences the settings screen offers, so it does not need its own
     * list of them.
     */
    static List<Toggle> toggles() {
        return List.of(
                new Toggle("Confirm purchases",
                        "Open a panel before trading, instead of buying one on click",
                        ClientPrefs::confirmPurchases, ClientPrefs::setConfirmPurchases),
                new Toggle("Hide what I cannot afford",
                        "Leave items out of the list while your balance is too low",
                        ClientPrefs::hideUnaffordable, ClientPrefs::setHideUnaffordable),
                new Toggle("Show worth in tooltips",
                        "Add what an item sells for to its tooltip, anywhere in the game",
                        ClientPrefs::showWorth, ClientPrefs::setShowWorth));
    }

    /**
     * One on/off preference, as the settings screen shows it.
     */
    record Toggle(String label, String hint,
                  java.util.function.BooleanSupplier read,
                  java.util.function.Consumer<Boolean> write) {
    }

    // ------------------------------------------------------------ persistence

    static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Object> stored = GSON.fromJson(reader,
                    new TypeToken<Map<String, Object>>() { }.getType());
            if (stored == null) {
                return;
            }

            sort = parseSort(stored.get("sort"));
            confirmPurchases = bool(stored.get("confirm-purchases"), true);
            hideUnaffordable = bool(stored.get("hide-unaffordable"), false);
            showWorth = bool(stored.get("show-worth"), true);
        } catch (IOException | JsonSyntaxException exception) {
            // Preferences are not worth bothering the player about; the
            // defaults are perfectly usable.
            sort = Sort.CONFIG;
            confirmPurchases = true;
            hideUnaffordable = false;
            showWorth = true;
        }
    }

    private static void save() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("sort", sort.name());
        document.put("confirm-purchases", confirmPurchases);
        document.put("hide-unaffordable", hideUnaffordable);
        document.put("show-worth", showWorth);

        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(document, writer);
            }
        } catch (IOException exception) {
            // Losing a preference on restart is not worth interrupting play.
        }
    }

    private static Sort parseSort(Object raw) {
        if (raw == null) {
            return Sort.CONFIG;
        }
        try {
            return Sort.valueOf(String.valueOf(raw).trim());
        } catch (IllegalArgumentException exception) {
            return Sort.CONFIG;
        }
    }

    private static boolean bool(Object raw, boolean fallback) {
        return raw instanceof Boolean value ? value : fallback;
    }

    /**
     * In the game's config directory rather than the mod's data folder, which
     * on a client points at a server's worth of files it has no business in.
     */
    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }
}
