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
    public ArrayList<String> debugLogCache = new ArrayList<>();

    @Getter
    @Setter
    public ArrayList<String> transactionLogCache = new ArrayList<>();

    @Getter
    @Setter
    public ArrayList<String> mainLogCache = new ArrayList<>();

    public File mainLog = new File(GUIShop.getINSTANCE().getDataFolder().getPath(), "/Logs/main.log");
    public File debugLog = new File(GUIShop.getINSTANCE().getDataFolder().getPath(), "/Logs/debug.log");
    public File transactionLog = new File(GUIShop.getINSTANCE().getDataFolder().getPath(), "/Logs/transaction.log");

    public final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public LogUtil() {
        write(mainLog.toPath(), getMainLogCache());
        write(debugLog.toPath(), getDebugLogCache());
        write(transactionLog.toPath(), getTransactionLogCache());
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
    
    public void write(Path path, List<String> write) {
        try {
            Files.write(
                    path,
                    write,
                    StandardCharsets.UTF_8,
                    Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
            );
        } catch (IOException exception) {
            debugLog("An error occurred while trying to write to a logging file! (" + path + ")");
        }
    }

}
