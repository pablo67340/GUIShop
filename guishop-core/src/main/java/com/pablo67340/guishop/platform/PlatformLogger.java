package com.pablo67340.guishop.platform;

/**
 * Console logging, as provided by the host platform. Bukkit backs this with the
 * plugin logger so output keeps its [GUIShop] prefix; a NeoForge module would
 * back it with SLF4J.
 */
public interface PlatformLogger {

    void info(String message);

    void warning(String message);
}
