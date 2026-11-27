package com.pablo67340.guishop.neoforge.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pablo67340.guishop.neoforge.storage.GuiShopDatabase;
import com.pablo67340.guishop.platform.GuiShopPlatform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player balances, held in memory and written to the database.
 * <p>
 * The Bukkit plugin can defer to whatever economy Vault exposes. Nothing
 * equivalent exists here, so the mod keeps its own; without them there is
 * nothing for a purchase to spend.
 * <p>
 * Every balance is kept in memory rather than only those of online players.
 * Offline players are paid by {@code /pay} and adjusted by the admin commands,
 * and a few thousand decimals is nothing next to a loaded chunk. Writes go to
 * disk on a timer and at shutdown, not on every change, so a busy shop is not
 * a busy disk.
 * <p>
 * If SQLite could not be loaded this falls back to {@code balances.json}, which
 * is what the mod used before and is still perfectly serviceable. Nothing else
 * in the class knows which is in use.
 */
public final class BalanceStore {

    private static final String FILE_NAME = "balances.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Written to on the server thread and read while rendering command output
     * and flushing, so a concurrent map avoids having to reason about all
     * three.
     */
    private final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

    /** Last known name per account, so {@code /bal} can be answered offline. */
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    /**
     * Who has changed since the last write. Tracked per player rather than as
     * one flag so a flush writes the handful of rows that moved rather than
     * every account on the server.
     */
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    private BigDecimal startingBalance = BigDecimal.valueOf(1000);

    private GuiShopDatabase database;

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    /**
     * @param database the open database, or null to use the JSON file
     */
    public void attach(GuiShopDatabase database) {
        this.database = database != null && database.isAvailable() ? database : null;
    }

    // ------------------------------------------------------------- balances

    /**
     * The player's balance, creating the account at the starting balance the
     * first time it is asked for.
     */
    public BigDecimal get(UUID player) {
        return balances.computeIfAbsent(player, key -> {
            dirty.add(key);
            return startingBalance;
        });
    }

    /**
     * Whether this player has an account, without opening one.
     * <p>
     * Separate from {@link #get} so reading someone's balance cannot create it,
     * which would otherwise let a mistyped name leave an account behind.
     */
    public boolean hasAccount(UUID player) {
        return balances.containsKey(player);
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
        dirty.add(player);
    }

    /**
     * Forgets the account entirely, so the next read opens a fresh one at the
     * starting balance.
     */
    public void reset(UUID player) {
        balances.put(player, startingBalance);
        dirty.add(player);
    }

    /**
     * Records the name an account was last seen under, for the commands that
     * take a player who is not online.
     */
    public void rememberName(UUID player, String name) {
        if (name != null && !name.isBlank() && !name.equals(names.put(player, name))) {
            dirty.add(player);
        }
    }

    public String nameOf(UUID player) {
        return names.getOrDefault(player, "");
    }

    /**
     * Every name an account has been seen under.
     * <p>
     * Handed out as a plain collection because command completion asks for it
     * on every keystroke, and sorting the balances to get at the names would
     * be work done for nothing.
     */
    public Collection<String> knownNames() {
        return names.values();
    }

    /**
     * The richest accounts first.
     *
     * @return uuid to balance, at most {@code limit} entries
     */
    public Map<UUID, BigDecimal> top(int limit) {
        Map<UUID, BigDecimal> ranked = new LinkedHashMap<>();
        balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed())
                .limit(Math.max(0, limit))
                .forEach(entry -> ranked.put(entry.getKey(), entry.getValue()));
        return ranked;
    }

    // ------------------------------------------------------------ persistence

    public void load() {
        balances.clear();
        names.clear();
        dirty.clear();

        if (database == null) {
            loadJson();
            return;
        }

        boolean read = database.run("reading balances", connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT uuid, username, balance FROM player_balances")) {
                while (rows.next()) {
                    UUID uuid = parseUuid(rows.getString("uuid"));
                    if (uuid == null) {
                        continue;
                    }
                    balances.put(uuid, BigDecimal.valueOf(rows.getDouble("balance")));
                    String username = rows.getString("username");
                    if (username != null && !username.isBlank()) {
                        names.put(uuid, username);
                    }
                }
            }
        });

        if (!read) {
            // The database is open but unreadable, which is worse than not
            // having one: carrying on would overwrite it with nothing.
            database = null;
            loadJson();
            return;
        }

        if (balances.isEmpty()) {
            importJson();
        }
    }

    /**
     * Writes whatever has changed since the last call.
     */
    public void save() {
        if (dirty.isEmpty()) {
            return;
        }

        if (database == null) {
            saveJson();
            return;
        }

        // Copied before writing so a trade during the flush is not lost: it
        // marks the player dirty again and is picked up next time.
        List<UUID> pending = new ArrayList<>(dirty);
        dirty.removeAll(pending);

        boolean written = database.runBatch("writing balances", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_balances (uuid, username, balance, last_seen)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        username  = excluded.username,
                        balance   = excluded.balance,
                        last_seen = excluded.last_seen""")) {
                long now = System.currentTimeMillis();
                for (UUID uuid : pending) {
                    BigDecimal balance = balances.get(uuid);
                    if (balance == null) {
                        continue;
                    }
                    statement.setString(1, uuid.toString());
                    statement.setString(2, names.get(uuid));
                    statement.setDouble(3, balance.doubleValue());
                    statement.setLong(4, now);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });

        if (!written) {
            // Put them back, so the next flush tries again rather than the
            // change being lost because one write failed.
            dirty.addAll(pending);
        }
    }

    // -------------------------------------------------------- the JSON file

    /**
     * Reads balances written before the database existed, then sets the file
     * aside.
     * <p>
     * Kept as a backup rather than deleted, the same way the YAML shop configs
     * are: a migration that destroys the only copy of everyone's money is not
     * one anybody should have to trust.
     */
    private void importJson() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }

        loadJson();
        if (balances.isEmpty()) {
            return;
        }

        dirty.addAll(balances.keySet());
        save();

        Path backup = file.resolveSibling(FILE_NAME + ".bak");
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            GuiShopPlatform.get().logger().info(
                    "Moved " + balances.size() + " balance(s) from " + FILE_NAME
                            + " into the database. The old file is kept as " + backup.getFileName() + ".");
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Imported balances from " + FILE_NAME + " but could not rename it: "
                            + exception.getMessage() + ". Delete it by hand, or it will be"
                            + " imported again only if the database is ever emptied.");
        }
    }

    private void loadJson() {
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
                UUID uuid = parseUuid(key);
                if (uuid == null) {
                    GuiShopPlatform.get().logger().warning(
                            "Skipping unreadable balance entry '" + key + "' in " + FILE_NAME + ".");
                    return;
                }
                try {
                    // Via toString because Gson reads every JSON number as a
                    // double, which would round large balances.
                    balances.put(uuid, new BigDecimal(String.valueOf(value)));
                } catch (NumberFormatException exception) {
                    GuiShopPlatform.get().logger().warning(
                            "Skipping unreadable balance for '" + key + "' in " + FILE_NAME + ".");
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
    private void saveJson() {
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
            dirty.clear();
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not write " + FILE_NAME + ": " + exception.getMessage());
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    private static Path file() {
        return GuiShopPlatform.get().dataFolder().resolve(FILE_NAME);
    }
}
