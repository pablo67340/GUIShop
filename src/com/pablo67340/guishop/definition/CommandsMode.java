package com.pablo67340.guishop.definition;

import com.pablo67340.guishop.GUIShop;

import java.util.logging.Level;

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
     * @param cmdModeStr the config option string, null if not set
     * @return the parsed commands mode, never null
     */
    public static CommandsMode parseFromConfig(String cmdModeStr) {
        if (cmdModeStr == null) {
            // Fallback to legacy option register-commands
            return (GUIShop.getINSTANCE().getMainConfig().getBoolean("register-commands", true)) ? REGISTER : NONE;
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
                GUIShop.getINSTANCE().getLogger().log(Level.WARNING, "Unknown commands-mode {0}", cmdModeStr);
                return REGISTER;
        }
    }

}
