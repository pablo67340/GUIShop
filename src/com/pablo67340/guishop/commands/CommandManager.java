package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import static com.pablo67340.guishop.GUIShop.debugLog;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;

/**
 * The command manager
 */
public class CommandManager {

    private Field knownCommandsField;
    private CommandMap bukkitCommandMap;
    private Method syncCommandsMethod;
    private final Map<String, Command> registered = new HashMap<>();

    public CommandManager() {
        try {
            Method commandMapMethod = Bukkit.getServer().getClass().getMethod("getCommandMap");
            this.bukkitCommandMap = (CommandMap) commandMapMethod.invoke(Bukkit.getServer());

            knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            Class<?> craftServer = Bukkit.getServer().getClass();
            syncCommandsMethod = craftServer.getDeclaredMethod("syncCommands");
            syncCommandsMethod.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            GUIShop.debugLog("Error registering Commands: " + e.getMessage());
        }
    }

    /**
     * Sync the commands to the server. Mainly used to make tab completer work
     * in 1.13+
     */
    public void syncCommand() {
        if (syncCommandsMethod == null) {
            return;
        }

        try {
            syncCommandsMethod.invoke(Bukkit.getServer());
        } catch (IllegalAccessException | InvocationTargetException e) {
            Bukkit.getLogger().log(Level.WARNING, "Error when syncing commands", e);
        }
    }

    /**
     * Unregister a command from the known commands
     *
     * @param command the command
     *
     * @throws IllegalAccessException if this can not get the known commands
     */
    public void unregisterFromKnownCommands(@NotNull final Command command) throws IllegalAccessException {
        Map<?, ?> knownCommands = (Map<?, ?>) knownCommandsField.get(bukkitCommandMap);
        knownCommands.values().removeIf(command::equals);
        command.unregister(bukkitCommandMap);
    }

    /**
     * Register the command to the command map
     *
     * @param label the label of the command
     * @param command the command
     */
    public void registerCommandToCommandMap(@NotNull final String label, @NotNull final Command command) {
        bukkitCommandMap.register(label, command);
    }

    /**
     * Register the command
     *
     * @param command the command object
     */
    public final void register(@NotNull final Command command) {
        String name = command.getLabel();
        if (this.registered.containsKey(name)) {
            GUIShop.log("ERROR: tried to register a command that is already in use by a different plugin! Command: " + name);
            return;
        }

        registerCommandToCommandMap(GUIShop.getINSTANCE().getName(), command);
        this.registered.put(name, command);
    }

    /**
     * Unregister the command
     *
     * @param command the command object
     */
    public final void unregister(@NotNull final Command command) {
        try {
            unregisterFromKnownCommands(command);
            this.registered.remove(command.getLabel());
        } catch (ReflectiveOperationException e) {
            GUIShop.debugLog("Error occured unregistering command: " + e.getMessage());
        }
    }

    /**
     * Unregister the command
     *
     * @param command the command label
     */
    public final void unregister(@NotNull final String command) {
        if (this.registered.containsKey(command)) {
            unregister(this.registered.remove(command));
        }
    }

    /**
     * Unregister all commands
     */
    public final void unregisterAll() {
        this.registered.values().forEach(command -> {
            try {
                unregisterFromKnownCommands(command);
            } catch (ReflectiveOperationException e) {
                GUIShop.debugLog("Error occured unregistering command: " + command.getLabel() + ": " + e.getMessage());
            }
        });
        this.registered.clear();
    }

    /**
     * Get registered commands
     *
     * @return the map contains the name and the command object
     */
    @NotNull
    public final Map<String, Command> getRegistered() {
        return Collections.unmodifiableMap(this.registered);
    }

    public void registerCommands() {
        // Register buy commands if there are any
        if (!GUIShop.BUY_COMMANDS.isEmpty()) {
            debugLog("Registering/unregistering shop commands: " + StringUtils.join(GUIShop.BUY_COMMANDS, ", "));
            BuyCommand buyCommand = new BuyCommand(new ArrayList<>(GUIShop.BUY_COMMANDS));
            register(buyCommand);
        }

        // Register sell commands if there are any
        if (!GUIShop.SELL_COMMANDS.isEmpty()) {
            debugLog("Registering/unregistering sell commands: " + StringUtils.join(GUIShop.SELL_COMMANDS, ", "));
            SellCommand sellCommand = new SellCommand(new ArrayList<>(GUIShop.SELL_COMMANDS));
            register(sellCommand);
        }
    }
}
