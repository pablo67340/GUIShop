package com.pablo67340.guishop.neoforge.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pablo67340.guishop.platform.GuiShopPlatform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player balances, held in memory and persisted to {@code balances.json}.
 * <p>
 * The Bukkit plugin can defer to whatever economy Vault exposes. Nothing
 * equivalent exists here, so the mod keeps its own balances; without them there
 * is nothing for a purchase to spend. JSON rather than the plugin's SQLite
 * because Minecraft already bundles Gson, and a shop's balance file is small
 * enough that a database would buy nothing.
 */
public final class BalanceStore {

    private static final String FILE_NAME = "balances.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Written to only on the server thread but read while rendering command
     * output, so a concurrent map avoids having to reason about both.
     */
    private final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

    private BigDecimal startingBalance = BigDecimal.valueOf(1000);

    /** Set when a balance changes, so an unchanged store is not rewritten. */
    private volatile boolean dirty;

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    /**
     * The player's balance, creating the account at the starting balance the
     * first time it is asked for.
     */
    public BigDecimal get(UUID player) {
        return balances.computeIfAbsent(player, key -> {
            dirty = true;
            return startingBalance;
        });
    }

    public boolean has(UUID player, BigDecimal amount) {
        return get(player).compareTo(amount) >= 0;
    }

    /**
     * Takes money, if there is enough.
     *
     * @return false if the balance would go negative, in which case nothing is
     *         deducted
     */
    public boolean withdraw(UUID player, BigDecimal amount) {
        BigDecimal current = get(player);
        if (current.compareTo(amount) < 0) {
            return false;
        }
        set(player, current.subtract(amount));
        return true;
    }

    public void deposit(UUID player, BigDecimal amount) {
        set(player, get(player).add(amount));
    }

    public void set(UUID player, BigDecimal amount) {
        balances.put(player, amount);
        dirty = true;
    }

    // ------------------------------------------------------------ persistence

    public void load() {
        balances.clear();
        dirty = false;

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

            stored.forEach((key, value) -> {
                try {
                    // Via toString because Gson reads every JSON number as a
                    // double, which would round large balances.
                    balances.put(UUID.fromString(key), new BigDecimal(String.valueOf(value)));
                } catch (IllegalArgumentException exception) {
                    GuiShopPlatform.get().logger().warning(
                            "Skipping unreadable balance entry '" + key + "' in " + FILE_NAME + ".");
                }
            });
        } catch (IOException | JsonSyntaxException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not read " + FILE_NAME + ": " + exception.getMessage()
                            + ". Balances will start fresh, and the file will not be overwritten"
                            + " until a balance changes.");
        }
    }

    /**
     * Writes the balances, through a temporary file so a crash mid-write cannot
     * leave players with a truncated file and no money.
     */
    public void save() {
        if (!dirty) {
            return;
        }

        Map<String, String> plain = new LinkedHashMap<>();
        balances.forEach((uuid, balance) -> plain.put(uuid.toString(), balance.toPlainString()));

        Path file = file();
        Path temporary = file.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(plain, writer);
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not write " + FILE_NAME + ": " + exception.getMessage());
        }
    }

    private static Path file() {
        return GuiShopPlatform.get().dataFolder().resolve(FILE_NAME);
    }
}
