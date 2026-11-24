package com.pablo67340.guishop.config;

import lombok.Getter;
import lombok.Setter;

/**
 * The Bukkit settings holder. Everything platform-independent is inherited from
 * {@link BaseConfig}, so {@code Config.isDebugMode()} and friends still resolve
 * here; this class adds only the settings that describe a chest-inventory GUI.
 */
public final class Config extends BaseConfig {

    /**
     * Pagination buttons. Holds live {@link com.pablo67340.guishop.definition.Item}
     * instances, so it has no meaning off Bukkit.
     */
    @Getter
    @Setter
    private static ButtonConfig buttonConfig = new ButtonConfig();

    /**
     * Transaction GUI materials and quantities, in terms of XSeries materials.
     */
    @Getter
    @Setter
    private static TransactionGuiConfig transactionGuiConfig = new TransactionGuiConfig();

    private Config() {
    }
}
