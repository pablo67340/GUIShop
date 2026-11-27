package com.pablo67340.guishop.neoforge.storage;

import com.pablo67340.guishop.platform.GuiShopPlatform;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The one SQLite database everything persistent is kept in.
 * <p>
 * The plugin keeps balances and statistics in separate files because each
 * manager opened its own. One file here: they are written together, read
 * together, and a single file is one thing to back up rather than three.
 * <p>
 * <b>The driver may not load.</b> Paper and Spigot ship sqlite-jdbc, so the
 * plugin can assume it. Nothing in Minecraft or NeoForge does, so the mod
 * carries it, and the native builds it carries are trimmed to the platforms a
 * server actually runs on. On anything else {@link #isAvailable()} is false
 * from the start and the callers fall back to their JSON files, which is worth
 * far more than refusing to start.
 */
public final class GuiShopDatabase {

    /** Beside the configs, under the folder the plugin also used for this. */
    private static final String RELATIVE_PATH = "Data/guishop.db";

    private Connection connection;

    /**
     * Guards every statement. SQLite serialises writes anyway, and the
     * alternative is reasoning about the server thread, the flush thread and
     * the login thread sharing one connection.
     */
    private final Object lock = new Object();

    /**
     * Opens the database and creates anything missing.
     *
     * @return whether the database can be used. False is a normal outcome, not
     *         an error: the caller is expected to carry on without it
     */
    public boolean open() {
        if (connection != null) {
            return true;
        }

        Path file = GuiShopPlatform.get().dataFolder().resolve(RELATIVE_PATH);
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException exception) {
            GuiShopPlatform.get().logger().warning(
                    "Could not create " + file.getParent() + ": " + exception.getMessage()
                            + ". Falling back to file storage.");
            return false;
        }

        try {
            // Through the data source rather than DriverManager, which finds
            // its drivers by service loader. Under NeoForge's module layer the
            // driver is in our own jar and the service lookup does not see it.
            SQLiteDataSource source = new SQLiteDataSource();
            source.setUrl("jdbc:sqlite:" + file.toAbsolutePath());
            connection = source.getConnection();
            createTables();
            GuiShopPlatform.get().logger().info("Opened " + RELATIVE_PATH + ".");
            return true;
        } catch (SQLException exception) {
            warnUnavailable(exception.getMessage());
            return false;
        } catch (LinkageError error) {
            // What an unsupported platform looks like: the classes are here,
            // the native library for this OS and architecture is not.
            warnUnavailable("no native SQLite build for " + System.getProperty("os.name")
                    + " on " + System.getProperty("os.arch"));
            return false;
        }
    }

    private void warnUnavailable(String reason) {
        connection = null;
        GuiShopPlatform.get().logger().warning(
                "SQLite is not available (" + reason + "). Balances and statistics will use"
                        + " JSON files instead. Everything still works; only the statistics"
                        + " history is unavailable.");
    }

    public boolean isAvailable() {
        return connection != null;
    }

    /**
     * Runs work against the database, doing nothing if it is not available.
     * <p>
     * Taking the connection through here rather than exposing it means no
     * caller has to remember the availability check, and a failing statement
     * cannot take the server down with it.
     *
     * @param what used only to say what failed, if anything does
     * @return whether the work ran without error
     */
    public boolean run(String what, SqlWork work) {
        if (connection == null) {
            return false;
        }

        synchronized (lock) {
            try {
                work.run(connection);
                return true;
            } catch (SQLException exception) {
                GuiShopPlatform.get().logger().warning(
                        "Database error while " + what + ": " + exception.getMessage());
                return false;
            }
        }
    }

    /**
     * Runs several statements as one transaction, so a crash part way through
     * a flush cannot leave half of it written.
     */
    public boolean runBatch(String what, SqlWork work) {
        return run(what, link -> {
            boolean previous = link.getAutoCommit();
            link.setAutoCommit(false);
            try {
                work.run(link);
                link.commit();
            } catch (SQLException exception) {
                link.rollback();
                throw exception;
            } finally {
                link.setAutoCommit(previous);
            }
        });
    }

    public void close() {
        synchronized (lock) {
            if (connection == null) {
                return;
            }
            try {
                connection.close();
            } catch (SQLException exception) {
                GuiShopPlatform.get().logger().warning(
                        "Could not close the database cleanly: " + exception.getMessage());
            }
            connection = null;
        }
    }

    /**
     * The schema, created on every start so an upgrade adds what it needs
     * without a migration step.
     * <p>
     * Money is stored as a double rather than text. It costs exactness beyond
     * fifteen significant digits, which no balance reaches, and buys ordering:
     * the top-spender queries sort on these columns, and text would sort
     * "9" above "10".
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_balances (
                        uuid      TEXT PRIMARY KEY,
                        username  TEXT,
                        balance   REAL NOT NULL DEFAULT 0,
                        last_seen INTEGER
                    )""");

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid         TEXT PRIMARY KEY,
                        total_spent  REAL    NOT NULL DEFAULT 0,
                        total_earned REAL    NOT NULL DEFAULT 0,
                        items_bought INTEGER NOT NULL DEFAULT 0,
                        items_sold   INTEGER NOT NULL DEFAULT 0
                    )""");

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS item_transactions (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid      TEXT    NOT NULL,
                        material  TEXT    NOT NULL,
                        quantity  INTEGER NOT NULL,
                        price     REAL    NOT NULL,
                        type      TEXT    NOT NULL,
                        timestamp INTEGER NOT NULL
                    )""");

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_preferences (
                        uuid             TEXT PRIMARY KEY,
                        pay_notifications INTEGER NOT NULL DEFAULT 1
                    )""");

            // Both top-item queries group by player and by material, and the
            // table grows by a row per trade forever.
            statement.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_transactions_player"
                            + " ON item_transactions(uuid, type, material)");
            statement.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_transactions_material"
                            + " ON item_transactions(type, material)");
        }
    }

    /**
     * Work to run against an open connection.
     */
    @FunctionalInterface
    public interface SqlWork {
        void run(Connection connection) throws SQLException;
    }
}
