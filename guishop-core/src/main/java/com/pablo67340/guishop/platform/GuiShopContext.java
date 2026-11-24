package com.pablo67340.guishop.platform;

import com.pablo67340.guishop.util.LogUtil;

import java.nio.file.Path;

/**
 * Everything core needs from the host platform, in one place. Each platform
 * module provides an implementation and registers it with
 * {@link GuiShopPlatform} during startup.
 * <p>
 * The two config flags are here rather than read from a core config model
 * because configuration loading still lives in the platform modules. They move
 * out once core owns the config POJOs.
 */
public interface GuiShopContext {

    /**
     * The plugin or mod data directory, where logs and databases are written.
     */
    Path dataFolder();

    PlatformLogger logger();

    /**
     * The caching logger used for the plugin's main, debug and transaction logs.
     * Prefer this over {@link #logger()} for anything that should land in those
     * files.
     */
    LogUtil logUtil();

    PlatformScheduler scheduler();

    boolean isDebugMode();

    boolean isTransactionLog();
}
