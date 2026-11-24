package com.pablo67340.guishop.platform;

import com.pablo67340.guishop.util.LogUtil;

import java.nio.file.Path;

/**
 * Everything core needs from the host platform, in one place. Each platform
 * module provides an implementation and registers it with
 * {@link GuiShopPlatform} during startup.
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
}
