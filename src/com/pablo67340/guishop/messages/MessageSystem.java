package com.pablo67340.guishop.messages;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessageSystem {
    /*
     * Copyright to https://github.com/Amejonah1200/simple-message-system
     */
    private final Map<String, Message> messages = new HashMap<>();

    /**
     * Instantiates a new SimpleMessageSystem with default and custom messages.
     *
     * @param javaPlugin the java plugin
     * @param messagesConfiguration the messages configuration
     */
    public MessageSystem(JavaPlugin javaPlugin, ConfigurationSection messagesConfiguration) {
        generateDefaults(javaPlugin.getResource("messages.yml"));
        if (messagesConfiguration != null) loadCustomMessages(messagesConfiguration);
    }

    /**
     * Instantiates a new SimpleMessageSystem with loading defaults but not custom messages.
     *
     * @param javaPlugin the java plugin
     */
    public MessageSystem(JavaPlugin javaPlugin) {
        this(javaPlugin, null);
    }

    /**
     * Instantiates a new SimpleMessageSystem without loading defaults or custom messages.
     */
    public MessageSystem() {}

    /**
     * Generate defaults using internal file given as parameter.
     *
     * @param inputStream the file inputstream
     */
    public void generateDefaults(InputStream inputStream) {
        if (inputStream == null) return;
        try (Reader reader = new InputStreamReader(inputStream)) {
            YamlConfiguration internalMessages = YamlConfiguration.loadConfiguration(reader);
            String message;
            synchronized (this.messages) {
                this.messages.clear();
                for (String path : internalMessages.getKeys(true)) {
                    if (path.endsWith(".message")) {
                        path = path.substring(0, path.length() - 8);
                        message = internalMessages.isString(path + ".message") ? internalMessages
                                .getString(path + ".message") : internalMessages.isList(path + ".message") ? String
                                .join("\n", internalMessages.getStringList(path + ".message")) : null;
                        if (message == null) continue;
                        messages.put(path, new PlaceholderMessage(path, message,
                                internalMessages.getStringList(path + ".placeholders").toArray(new String[0])));
                    } else if (internalMessages.isString(path))
                        messages.put(path, new Message(path, Objects.requireNonNull(internalMessages.getString(path))));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load custom messages using the configuration given.
     *
     * @param configuration the configuration
     */
    public void loadCustomMessages(@NotNull final ConfigurationSection configuration) {
        Objects.requireNonNull(configuration, "Configuration cannot be null!");
        synchronized (this.messages) {
            this.messages.values().forEach(message -> {
                String s = configuration.getString(message.getPath());
                message.setCustomMessage(s != null ? ChatColor.translateAlternateColorCodes('&', s) : null);
            });
        }
    }

    /**
     * Save default messages and return if it was changed.
     *
     * @param configuration the configuration to apply to
     * @param override if it should override
     *
     * @return if changed
     */
    public boolean saveDefaultMessages(@NotNull final ConfigurationSection configuration, boolean override) {
        Objects.requireNonNull(configuration, "Configuration cannot be null!");
        boolean changed = false;
        synchronized (this.messages) {
            for (Message message : this.messages.values()) {
                if (!configuration.isSet(message.getPath()) || override) {
                    configuration.set(message.getPath(), message.getDefaultMessage().replace('§', '&'));
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Translate the message (path to it) with given parameters.
     *
     * @param message the path to message
     * @param params the parameters
     *
     * @return translated message
     */
    @NotNull
    public String translate(@NotNull String message, @Nullable Object... params) {
        Message simpleMessage;
        synchronized (messages) {
            simpleMessage = messages.get(Objects.requireNonNull(message, "Message cannot be null!"));
        }
        if (simpleMessage == null) return "Message cannot be null!";
        if (simpleMessage instanceof PlaceholderMessage) return ((PlaceholderMessage) simpleMessage).translate(params);
        return simpleMessage.getRawMessage();
    }
}
