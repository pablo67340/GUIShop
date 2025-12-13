package com.pablo67340.guishop.util;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.config.Config;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
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

    private File mainLog;
    private File debugLog;
    private File transactionLog;

    public final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public LogUtil() {
        // Initialize log files - delay file creation until first use
        mainLog = new File(GUIShop.getINSTANCE().getDataFolder().getPath(), "/Logs/main.log");
        debugLog = new File(GUIShop.getINSTANCE().getDataFolder().getPath(), "/Logs/debug.log");
        transactionLog = new File(GUIShop.getINSTANCE().getDataFolder().getPath(), "/Logs/transaction.log");
        
        // Ensure log directory exists
        mainLog.getParentFile().mkdirs();
    }

    public void transactionLog(String input) {
        if (Config.isTransactionLog()) {
            GUIShop.getINSTANCE().getLogger().log(Level.INFO, "TRANSACTION: {0}", input);
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

        getTransactionLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] TRANSACTION: " + input);
    }

    public void log(String input) {
        GUIShop.getINSTANCE().getLogger().log(Level.INFO, "LOG: {0}", input);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

        getMainLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] LOG: " + input);
    }

    public void debugLog(String input) {
        if (Config.isDebugMode()) {
            GUIShop.getINSTANCE().getLogger().log(Level.INFO, "DEBUG: {0}", input);
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_NOW);

        getDebugLogCache().add("[" + simpleDateFormat.format(calendar.getTime()) + "] DEBUG: " + input);
    }

    /**
     * Flush all cached logs to disk. Should be called periodically and on shutdown.
     */
    public void flushLogs() {
        // Write main log cache
        if (!mainLogCache.isEmpty()) {
            write(mainLog.toPath(), new ArrayList<>(mainLogCache));
            mainLogCache.clear();
        }
        
        // Write debug log cache
        if (!debugLogCache.isEmpty()) {
            write(debugLog.toPath(), new ArrayList<>(debugLogCache));
            debugLogCache.clear();
        }
        
        // Write transaction log cache
        if (!transactionLogCache.isEmpty()) {
            write(transactionLog.toPath(), new ArrayList<>(transactionLogCache));
            transactionLogCache.clear();
        }
    }

    /**
     * Check and rotate log files if they exceed the maximum size (50MB).
     */
    public void checkAndRotateLogs() {
        try {
            final long MAX_SIZE = 52428800L; // 50MB
            
            if (mainLog.exists() && Files.size(mainLog.toPath()) >= MAX_SIZE) {
                mainLog.delete();
            }
            if (debugLog.exists() && Files.size(debugLog.toPath()) >= MAX_SIZE) {
                debugLog.delete();
            }
            if (transactionLog.exists() && Files.size(transactionLog.toPath()) >= MAX_SIZE) {
                transactionLog.delete();
            }
        } catch (IOException e) {
            GUIShop.getINSTANCE().getLogger().log(Level.WARNING, "Error checking log file sizes: " + e.getMessage());
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
            GUIShop.getINSTANCE().getLogger().log(Level.WARNING, 
                    "An error occurred while trying to write to a logging file! (" + path + "): " + exception.getMessage());
        }
    }

}
