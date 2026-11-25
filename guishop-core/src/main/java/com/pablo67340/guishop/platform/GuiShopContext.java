package com.pablo67340.guishop.platform;

import com.pablo67340.guishop.api.DynamicPriceProvider;
import com.pablo67340.guishop.util.LogUtil;

import java.math.BigDecimal;
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

    /**
     * Renders a monetary value as players should see it, including the currency
     * prefix and suffix.
     * <p>
     * Platform-specific because the digit count comes from whichever economy is
     * installed, which core knows nothing about.
     */
    String formatCurrency(BigDecimal value);

    /**
     * The live-pricing provider, or null when dynamic pricing is unavailable.
     * Callers must handle null rather than assuming the setting implies one.
     */
    DynamicPriceProvider dynamicPriceProvider();
}
