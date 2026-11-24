package com.pablo67340.guishop.util;

import com.pablo67340.guishop.platform.GuiShopContext;
import com.pablo67340.guishop.platform.GuiShopPlatform;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author bryce.wilkinson
 */
public final class LogUtil {

    @Getter
    @Setter
    private ArrayList<String> debugLogCache = new ArrayList<>();

    @Getter
    @Setter
    private ArrayList<String> transactionLogCache = new ArrayList<>();

    @Getter
    @Setter
    private ArrayList<String> mainLogCache = new ArrayList<>();

    private final Path mainLog;
    private final Path debugLog;
    private final Path transactionLog;

    public final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public LogUtil() {
        Path logs = context().dataFolder().resolve("Logs");
        mainLog = logs.resolve("main.log");
        debugLog = logs.resolve("debug.log");
        transactionLog = logs.resolve("transaction.log");

        try {
            Files.createDirectories(logs);
        } catch (IOException exception) {
            context().logger().warning("Could not create the log directory (" + logs + "): " + exception.getMessage());
        }
    }

    private static GuiShopContext context() {
        return GuiShopPlatform.get();
    }

    private String timestamp() {
        return new SimpleDateFormat(DATE_FORMAT_NOW).format(Calendar.getInstance().getTime());
    }

    public void transactionLog(String input) {
        if (context().isTransactionLog()) {
            context().logger().info("TRANSACTION: " + input);
        }

        getTransactionLogCache().add("[" + timestamp() + "] TRANSACTION: " + input);
    }

    public void log(String input) {
        context().logger().info("LOG: " + input);

        getMainLogCache().add("[" + timestamp() + "] LOG: " + input);
    }

    public void debugLog(String input) {
        if (context().isDebugMode()) {
            context().logger().info("DEBUG: " + input);
        }

        getDebugLogCache().add("[" + timestamp() + "] DEBUG: " + input);
    }

    /**
     * Flush all cached logs to disk. Should be called periodically and on shutdown.
     */
    public void flushLogs() {
        // Write main log cache
        if (!mainLogCache.isEmpty()) {
            write(mainLog, new ArrayList<>(mainLogCache));
            mainLogCache.clear();
        }

        // Write debug log cache
        if (!debugLogCache.isEmpty()) {
            write(debugLog, new ArrayList<>(debugLogCache));
            debugLogCache.clear();
        }

        // Write transaction log cache
        if (!transactionLogCache.isEmpty()) {
            write(transactionLog, new ArrayList<>(transactionLogCache));
            transactionLogCache.clear();
        }
    }

    /**
     * Check and rotate log files if they exceed the maximum size (50MB).
     */
    public void checkAndRotateLogs() {
        try {
            final long MAX_SIZE = 52428800L; // 50MB

            for (Path log : List.of(mainLog, debugLog, transactionLog)) {
                if (Files.exists(log) && Files.size(log) >= MAX_SIZE) {
                    Files.delete(log);
                }
            }
        } catch (IOException e) {
            context().logger().warning("Error checking log file sizes: " + e.getMessage());
        }
    }

    private void write(Path path, List<String> write) {
        if (write == null || write.isEmpty()) {
            return;
        }

        try {
            // Ensure parent directory exists
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            Files.write(
                    path,
                    write,
                    StandardCharsets.UTF_8,
                    Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (IOException exception) {
            context().logger().warning(
                    "An error occurred while trying to write to a logging file! (" + path + "): " + exception.getMessage());
        }
    }

}
