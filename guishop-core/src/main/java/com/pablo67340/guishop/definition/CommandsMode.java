package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.platform.GuiShopPlatform;

public enum CommandsMode {

    /**
     * Intercepts all commands using the PlayerCommandPreprocessEvent. <br>
     * Useful for server owners with plugin conflicts. Does not support tab
     * complete.
     */
    INTERCEPT,
    /**
     * Registers the commands using reflection.
     */
    REGISTER,
    NONE;

    /**
     * Parses the commands mode configuration option from the config
     *
     * @param cmdModeStr      the config option string, null if not set
     * @param registerLegacy  value of the legacy {@code register-commands}
     *                        option, used only when {@code cmdModeStr} is null.
     *                        Passed in rather than read here so this enum stays
     *                        independent of any particular config format.
     * @return the parsed commands mode, never null
     */
    public static CommandsMode parseFromConfig(String cmdModeStr, boolean registerLegacy) {
        if (cmdModeStr == null) {
            // Fallback to legacy option register-commands
            return registerLegacy ? REGISTER : NONE;
        }
        switch (cmdModeStr) {
            case "INTERCEPT":
                return INTERCEPT;
            case "REGISTER":
                return REGISTER;
            case "NONE":
                return NONE;
            default:
                // User specified unknown mode, warn in console and use default option
                GuiShopPlatform.get().logger().warning("Unknown commands-mode " + cmdModeStr);
                return REGISTER;
        }
    }

}
