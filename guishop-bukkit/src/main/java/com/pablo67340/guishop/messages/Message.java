package com.pablo67340.guishop.messages;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;

public class Message {

    /*
     * Copyright to https://github.com/Amejonah1200/simple-message-system
     */

    protected final String path;
    protected final String defaultMessage;
    protected String customMessage;

    /**
     * Instantiates a new SimpleMessage.
     *
     * @param path           The message path in the configuration.
     * @param defaultMessage The default message specified in the default
     *                       configuration.
     * @param customMessage  The custom message.
     */
    public Message(@NotNull String path, @NotNull String defaultMessage, @Nullable String customMessage) {
        this.path = Objects.requireNonNull(path, "Path cannot be null!");
        this.defaultMessage = ChatColor
                .translateAlternateColorCodes('&', Objects.requireNonNull(defaultMessage, "Default " + path + " cannot be null!"));
        this.customMessage = customMessage != null ? ChatColor.translateAlternateColorCodes('&', customMessage) : null;
    }

    /**
     * Instantiates a new SimpleMessage.
     *
     * @param path           the path in the configuration
     * @param defaultMessage the default message specified in the default
     *                       configuration.
     */
    public Message(@NotNull String path, @NotNull String defaultMessage) {
        this(path, defaultMessage, null);
    }

    /**
     * Gets path to this message.
     *
     * @return the path
     */
    @NotNull
    public String getPath() {
        return path;
    }

    /**
     * Gets default message.
     *
     * @return the default message
     */
    @NotNull
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Gets custom message.
     *
     * @return the custom message
     */
    @Nullable
    public String getCustomMessage() {
        return customMessage;
    }

    /**
     * Sets custom message and translates color codes with "&amp;" to "§".
     *
     * @param customMessage the custom message to set
     */
    public void setCustomMessage(@Nullable String customMessage) {
        this.customMessage = customMessage != null ? ChatColor.translateAlternateColorCodes('&', customMessage) : null;
    }

    /**
     * Gets raw message, if the custom message is null, it will return the
     * default message.
     *
     * @return the raw message
     */
    @NotNull
    public String getRawMessage() {
        return customMessage != null ? customMessage : defaultMessage;
    }

    @Override
    public String toString() {
        return "SimpleMessage{" + "path='" + path + '\'' + ", defaultMessage='" + defaultMessage + '\''
                + ", customMessage='" + customMessage + '\'' + '}';
    }
}
