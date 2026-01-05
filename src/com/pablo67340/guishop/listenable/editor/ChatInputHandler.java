package com.pablo67340.guishop.listenable.editor;

import com.pablo67340.guishop.GUIShop;
import com.pablo67340.guishop.util.MathUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.pablo67340.guishop.util.SchedulerUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Handles chat-based input for the item editor.
 * Allows players to type values in chat that get captured and processed.
 */
public class ChatInputHandler implements Listener {

    private static ChatInputHandler instance;
    
    // Map of player UUID to their pending input handler
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
    
    private boolean registered = false;

    private ChatInputHandler() {}

    public static ChatInputHandler getInstance() {
        if (instance == null) {
            instance = new ChatInputHandler();
        }
        return instance;
    }

    /**
     * Register this listener if not already registered.
     */
    public void register() {
        if (!registered) {
            GUIShop.getINSTANCE().getServer().getPluginManager().registerEvents(this, GUIShop.getINSTANCE());
            registered = true;
        }
    }

    /**
     * Unregister this listener.
     */
    public void unregister() {
        if (registered) {
            HandlerList.unregisterAll(this);
            pendingInputs.clear();
            registered = false;
        }
    }

    /**
     * Request text input from a player.
     *
     * @param player The player to request input from
     * @param prompt The prompt to show the player
     * @param callback The callback to run with the input (runs on main thread)
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        requestInput(player, prompt, callback, null, 60);
    }

    /**
     * Request text input from a player with timeout.
     *
     * @param player The player to request input from
     * @param prompt The prompt to show the player
     * @param callback The callback to run with the input (runs on main thread)
     * @param cancelCallback The callback to run if cancelled or timed out (can be null)
     * @param timeoutSeconds How long to wait before timing out
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback, 
                            Runnable cancelCallback, int timeoutSeconds) {
        register(); // Ensure we're registered
        
        UUID uuid = player.getUniqueId();
        
        // Cancel any existing pending input
        cancelInput(player);
        
        // Create the pending input
        PendingInput pending = new PendingInput(callback, cancelCallback);
        pendingInputs.put(uuid, pending);
        
        // Send the prompt
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GUIShop Editor");
        player.sendMessage(ChatColor.YELLOW + prompt);
        player.sendMessage(ChatColor.GRAY + "Type your answer in chat, or type '" + ChatColor.RED + "cancel" + ChatColor.GRAY + "' to cancel.");
        player.sendMessage("");
        
        // Set up timeout
        SchedulerUtil.runAtEntityLater(player, () -> {
            PendingInput input = pendingInputs.remove(uuid);
            if (input != null && input.cancelCallback != null) {
                player.sendMessage(ChatColor.RED + "Input timed out.");
                input.cancelCallback.run();
            }
        }, timeoutSeconds * 20L);
    }

    /**
     * Request a numeric input from a player.
     * Supports abbreviated formats like 1k, 1.5M, 1B, etc.
     *
     * @param player The player to request input from
     * @param prompt The prompt to show the player
     * @param callback The callback to run with the parsed number
     */
    public void requestNumber(Player player, String prompt, Consumer<BigDecimal> callback) {
        requestNumber(player, prompt, callback, null, 60);
    }

    /**
     * Request a numeric input from a player with timeout.
     * Supports abbreviated formats like 1k, 1.5M, 1B, 240,000, etc.
     *
     * @param player The player to request input from
     * @param prompt The prompt to show the player
     * @param callback The callback to run with the parsed number
     * @param cancelCallback The callback to run if cancelled or timed out
     * @param timeoutSeconds How long to wait
     */
    public void requestNumber(Player player, String prompt, Consumer<BigDecimal> callback,
                             Runnable cancelCallback, int timeoutSeconds) {
        String fullPrompt = prompt + "\n" + ChatColor.GRAY + 
            "(Supports: 1000, 1,000, 1k, 1.5M, 2B, or 'false' to disable)";
        
        requestInput(player, fullPrompt, input -> {
            // Check for 'false' to disable
            if (input.equalsIgnoreCase("false")) {
                callback.accept(null); // null indicates disabled
                return;
            }
            
            BigDecimal value = MathUtil.tryParseAbbreviated(input);
            if (value != null) {
                callback.accept(value);
            } else {
                player.sendMessage(ChatColor.RED + "Invalid number format. Please try again.");
                // Re-request
                requestNumber(player, prompt, callback, cancelCallback, timeoutSeconds);
            }
        }, cancelCallback, timeoutSeconds);
    }

    /**
     * Cancel any pending input for a player.
     */
    public void cancelInput(Player player) {
        PendingInput pending = pendingInputs.remove(player.getUniqueId());
        if (pending != null && pending.cancelCallback != null) {
            pending.cancelCallback.run();
        }
    }

    /**
     * Check if a player has pending input.
     */
    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = pendingInputs.remove(player.getUniqueId());
        
        if (pending != null) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            
            // Check for cancel
            if (message.equalsIgnoreCase("cancel")) {
                // Run on player's thread (main thread on Paper/Spigot, entity region on Folia)
                SchedulerUtil.runAtEntity(player, () -> {
                    player.sendMessage(ChatColor.YELLOW + "Input cancelled.");
                    if (pending.cancelCallback != null) {
                        pending.cancelCallback.run();
                    }
                });
                return;
            }
            
            // Run callback on player's thread (main thread on Paper/Spigot, entity region on Folia)
            SchedulerUtil.runAtEntity(player, () -> pending.callback.accept(message));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Internal class to hold pending input data.
     */
    private static class PendingInput {
        final Consumer<String> callback;
        final Runnable cancelCallback;

        PendingInput(Consumer<String> callback, Runnable cancelCallback) {
            this.callback = callback;
            this.cancelCallback = cancelCallback;
        }
    }
}
