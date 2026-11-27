package com.pablo67340.guishop.neoforge.statistics;

import com.pablo67340.guishop.neoforge.storage.GuiShopDatabase;
import com.pablo67340.guishop.statistics.PlayerStats;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * What players have bought and sold.
 * <p>
 * Totals are kept per player so they can be shown instantly, and every trade
 * is also appended as a row, which is what makes "most sold item" answerable
 * at all. The plugin does the same, and this reads the same shape of data, so
 * a server that later wants its history somewhere else has a familiar schema
 * to point at.
 * <p>
 * Rows are queued and written on the same timer the balances use rather than
 * at the moment of a trade. A shop with a queue of players should not be doing
 * a synchronous disk write per click, and nothing reads the rows except
 * commands, which can be a few seconds behind without anyone noticing.
 * <p>
 * Without a database this does nothing at all. Statistics are a nice thing to
 * have, not a reason to stop a server selling items.
 */
public final class StatisticsStore {

    /** Beyond this, a flush is doing more harm than the data is worth. */
    private static final int MAX_PENDING = 10_000;

    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Queue<Transaction> pending = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Boolean> payNotifications = new ConcurrentHashMap<>();

    private GuiShopDatabase database;

    /**
     * @param database the open database, or null to record nothing
     */
    public void attach(GuiShopDatabase database) {
        this.database = database != null && database.isAvailable() ? database : null;
    }

    public boolean isAvailable() {
        return database != null;
    }

    // -------------------------------------------------------------- recording

    public void recordPurchase(UUID player, String material, int quantity, BigDecimal price) {
        record(player, material, quantity, price, true);
    }

    public void recordSale(UUID player, String material, int quantity, BigDecimal price) {
        record(player, material, quantity, price, false);
    }

    private void record(UUID player, String material, int quantity, BigDecimal price, boolean bought) {
        if (database == null || quantity <= 0 || material == null || material.isBlank()) {
            return;
        }

        PlayerStats stats = stats(player);
        if (bought) {
            stats.addSpent(price);
            stats.addItemsBought(quantity);
        } else {
            stats.addEarned(price);
            stats.addItemsSold(quantity);
        }

        if (pending.size() < MAX_PENDING) {
            pending.add(new Transaction(player, material, quantity,
                    price.doubleValue(), bought, System.currentTimeMillis()));
        }
    }

    // ---------------------------------------------------------------- reading

    /**
     * This player's totals, loading them the first time they are asked for.
     * <p>
     * Never null: a player who has never traded has zeroes, which is the right
     * answer rather than something for every caller to check.
     */
    public PlayerStats stats(UUID player) {
        PlayerStats cached = cache.get(player);
        if (cached != null) {
            return cached;
        }

        PlayerStats loaded = read(player);
        PlayerStats existing = cache.putIfAbsent(player, loaded);
        return existing == null ? loaded : existing;
    }

    /** Loads this player's totals ahead of them being asked for. */
    public void loadPlayer(UUID player) {
        stats(player);
    }

    /**
     * Drops a player's totals from memory, having written anything outstanding.
     */
    public void unloadPlayer(UUID player) {
        flush();
        cache.remove(player);
        payNotifications.remove(player);
    }

    private PlayerStats read(UUID player) {
        PlayerStats stats = new PlayerStats(player);
        if (database == null) {
            return stats;
        }

        database.run("reading statistics", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT total_spent, total_earned, items_bought, items_sold"
                            + " FROM player_stats WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet row = statement.executeQuery()) {
                    if (row.next()) {
                        stats.setTotalSpent(BigDecimal.valueOf(row.getDouble("total_spent")));
                        stats.setTotalEarned(BigDecimal.valueOf(row.getDouble("total_earned")));
                        stats.setItemsBought(row.getInt("items_bought"));
                        stats.setItemsSold(row.getInt("items_sold"));
                    }
                }
            }
        });

        stats.setTopBoughtItems(topItems(player, true, 3));
        stats.setTopSoldItems(topItems(player, false, 3));
        return stats;
    }

    /**
     * This player's most traded items, most first.
     *
     * @param bought true for things they bought, false for things they sold
     * @return material to total quantity, in order
     */
    public Map<String, Integer> topItems(UUID player, boolean bought, int limit) {
        return queryTopItems(
                "SELECT material, SUM(quantity) AS total FROM item_transactions"
                        + " WHERE uuid = ? AND type = ? GROUP BY material"
                        + " ORDER BY total DESC LIMIT ?",
                statement -> {
                    statement.setString(1, player.toString());
                    statement.setString(2, bought ? "BUY" : "SELL");
                    statement.setInt(3, limit);
                });
    }

    /**
     * The most traded items across everyone.
     */
    public Map<String, Integer> serverTopItems(boolean bought, int limit) {
        return queryTopItems(
                "SELECT material, SUM(quantity) AS total FROM item_transactions"
                        + " WHERE type = ? GROUP BY material ORDER BY total DESC LIMIT ?",
                statement -> {
                    statement.setString(1, bought ? "BUY" : "SELL");
                    statement.setInt(2, limit);
                });
    }

    private Map<String, Integer> queryTopItems(String sql, Binder binder) {
        Map<String, Integer> top = new LinkedHashMap<>();
        if (database == null) {
            return top;
        }

        database.run("reading top items", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        top.put(rows.getString("material"), rows.getInt("total"));
                    }
                }
            }
        });
        return top;
    }

    /**
     * Forgets everything recorded about a player.
     */
    public void reset(UUID player) {
        cache.remove(player);
        if (database == null) {
            return;
        }

        database.runBatch("resetting statistics", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM player_stats WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM item_transactions WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                statement.executeUpdate();
            }
        });
    }

    // ------------------------------------------------------ pay notifications

    /**
     * Whether this player wants to be told when someone pays them. On by
     * default, which is what someone who has never touched the setting expects.
     */
    public boolean payNotifications(UUID player) {
        Boolean cached = payNotifications.get(player);
        if (cached != null) {
            return cached;
        }

        boolean[] enabled = {true};
        if (database != null) {
            database.run("reading preferences", connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT pay_notifications FROM player_preferences WHERE uuid = ?")) {
                    statement.setString(1, player.toString());
                    try (ResultSet row = statement.executeQuery()) {
                        if (row.next()) {
                            enabled[0] = row.getInt("pay_notifications") != 0;
                        }
                    }
                }
            });
        }

        payNotifications.put(player, enabled[0]);
        return enabled[0];
    }

    public void setPayNotifications(UUID player, boolean enabled) {
        payNotifications.put(player, enabled);
        if (database == null) {
            return;
        }

        database.run("writing preferences", connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_preferences (uuid, pay_notifications) VALUES (?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET pay_notifications = excluded.pay_notifications""")) {
                statement.setString(1, player.toString());
                statement.setInt(2, enabled ? 1 : 0);
                statement.executeUpdate();
            }
        });
    }

    // ----------------------------------------------------------------- saving

    /**
     * Writes the queued trades and the totals they changed.
     */
    public void flush() {
        if (database == null || pending.isEmpty()) {
            return;
        }

        // Drained rather than iterated, so a trade arriving mid-flush joins the
        // next one instead of being written twice or not at all.
        List<Transaction> batch = new ArrayList<>();
        for (Transaction transaction = pending.poll();
             transaction != null;
             transaction = pending.poll()) {
            batch.add(transaction);
        }
        if (batch.isEmpty()) {
            return;
        }

        boolean written = database.runBatch("writing statistics", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO item_transactions (uuid, material, quantity, price, type, timestamp)"
                            + " VALUES (?, ?, ?, ?, ?, ?)")) {
                for (Transaction transaction : batch) {
                    statement.setString(1, transaction.player().toString());
                    statement.setString(2, transaction.material());
                    statement.setInt(3, transaction.quantity());
                    statement.setDouble(4, transaction.price());
                    statement.setString(5, transaction.bought() ? "BUY" : "SELL");
                    statement.setLong(6, transaction.timestamp());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_stats (uuid, total_spent, total_earned, items_bought, items_sold)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        total_spent  = excluded.total_spent,
                        total_earned = excluded.total_earned,
                        items_bought = excluded.items_bought,
                        items_sold   = excluded.items_sold""")) {
                // The running totals are authoritative, so the whole row is
                // written rather than incremented. Two servers on one database
                // would fight over that, but so would every other part of this.
                for (UUID player : batch.stream().map(Transaction::player).distinct().toList()) {
                    PlayerStats stats = cache.get(player);
                    if (stats == null) {
                        continue;
                    }
                    statement.setString(1, player.toString());
                    statement.setDouble(2, stats.getTotalSpent().doubleValue());
                    statement.setDouble(3, stats.getTotalEarned().doubleValue());
                    statement.setInt(4, stats.getItemsBought());
                    statement.setInt(5, stats.getItemsSold());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });

        if (!written) {
            // Back on the queue, oldest last, which costs ordering within the
            // batch and keeps the data.
            batch.forEach(pending::add);
        }
    }

    /**
     * One recorded trade, waiting to be written.
     */
    private record Transaction(UUID player, String material, int quantity,
                               double price, boolean bought, long timestamp) {
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws java.sql.SQLException;
    }
}
