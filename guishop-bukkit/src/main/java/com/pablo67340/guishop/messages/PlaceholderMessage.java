package com.pablo67340.guishop.messages;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class PlaceholderMessage extends Message {

    /*
     * Copyright to https://github.com/Amejonah1200/simple-message-system
     */

    /**
     * The placeholder delimiter. Ex. %name%
     */
    public static final char DELIMITER = '%';
    protected final String[] placeholders;
    protected Object[] message;

    /**
     * Creates the message array and sets the fields.
     *
     * @param path           The message path in the configuration.
     * @param defaultMessage The default message specified in the default
     *                       configuration.
     * @param placeholders   The placeholders.
     */
    public PlaceholderMessage(@NotNull String path, @NotNull String defaultMessage, @Nullable String[] placeholders) {
        super(path, defaultMessage);
        this.placeholders = placeholders;
        generateMessage();
    }

    /**
     * Sets the custom message and generates the message array and translates
     * color codes with "&amp;" to "§".
     *
     * @param customMessage The new custom message.
     */
    @Override
    public void setCustomMessage(@Nullable String customMessage) {
        customMessage = customMessage != null ? ChatColor.translateAlternateColorCodes('&', customMessage) : null;
        if (Objects.equals(getDefaultMessage(), customMessage) || Objects.equals(getCustomMessage(), customMessage)) {
            return;
        }
        this.customMessage = customMessage;
        if (customMessage != null) {
            generateMessage();
        } else {
            message = null;
        }
    }

    /**
     * Generates the message array.
     */
    private void generateMessage() {
        String messageStr = getRawMessage();
        if (placeholders == null || placeholders.length == 0) {
            message = null;
        } else {
            message = split(messageStr).toArray(new Object[0]);
        }
    }

    /**
     * Splits an input in an List of Objects: integers for the index of the
     * placeholders, Strings for other parts of the message.
     *
     * @param input Input to split.
     * @return The message in for of a list.
     */
    @NotNull
    private List<Object> split(String input) {
        List<Object> output = new LinkedList<>();
        List<String> placeholders = Arrays.stream(this.placeholders).collect(Collectors.toList());
        boolean inside = false;
        StringBuilder temp = new StringBuilder();
        int id;
        // building the message array
        for (char c : input.toCharArray()) {
            if (c == DELIMITER) {
                if (inside) {
                    if ((id = placeholders.indexOf(temp.toString())) != -1) {
                        output.add(id);
                    } else {
                        output.add("%" + temp + "%");
                    }
                } else if (temp.length() > 0) {
                    output.add(temp.toString());
                }
                temp = new StringBuilder();
                inside = !inside;
            } else {
                temp.append(c);
            }
        }
        if (temp.length() > 0) {
            output.add(temp.toString());
        }
        // compressing the message array
        List<Object> tempList = new ArrayList<>(output);
        output.clear();
        temp = new StringBuilder();
        for (Object obj : tempList) {
            if (obj instanceof String) {
                temp.append(obj);
            } else {
                if (temp.length() > 0) {
                    output.add(temp.toString());
                    temp = new StringBuilder();
                }
                output.add(obj);
            }
        }
        if (temp.length() > 0) {
            output.add(temp.toString());
        }
        return output;
    }

    /**
     * Tries to translate this message with given parameters. All parameters
     * will be converted to strings. The order is specified in the default
     * messages.yml
     *
     * @param params Parameters to inject.
     * @return The translated message.
     */
    @NotNull
    public String translate(@Nullable Object... params) {
        if (message == null || params == null || params.length == 0) {
            return getRawMessage();
        }
        StringBuilder output = new StringBuilder();
        for (Object obj : message) {
            if (obj instanceof String) {
                output.append(obj);
            } else {
                if ((int) obj >= params.length) {
                    output.append("%" + placeholders[(int) obj] + "%");
                } else {
                    output.append(params[(int) obj]);
                }
            }
        }
        return output.toString();
    }

    @Override
    public String toString() {
        return "PlaceholderMessage{" + "placeholders="
                + (placeholders == null ? "null" : Arrays.asList(placeholders).toString()) + ", message="
                + (message == null ? "null" : Arrays.asList(message).toString()) + ", path='" + path + '\''
                + ", defaultMessage='" + defaultMessage + '\'' + ", customMessage='" + customMessage + '\'' + '}';
    }
}
