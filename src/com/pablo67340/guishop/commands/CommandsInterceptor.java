package com.pablo67340.guishop.commands;

import com.pablo67340.guishop.GUIShop;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listener when the commands mode is
 * {@link com.pablo67340.guishop.definition.CommandsMode#INTERCEPT CommandsMode.INTERCEPT}.
 */
public class CommandsInterceptor implements Listener {

    private static CommandsInterceptor inst;

    /**
     * Registers the listener if not yet registered
     */
    public static void register() {
        if (inst == null) {
            inst = new CommandsInterceptor();
            Bukkit.getPluginManager().registerEvents(inst, GUIShop.getINSTANCE());
        }
    }

    /**
     * Unregisters the listener if already registered
     */
    public static void unregister() {
        if (inst != null) {
            HandlerList.unregisterAll(inst);
            inst = null;
        }
    }

    /*
     * By ignoring cancelled events we can avoid incompatibilities with some plugins,
     * e.g. anti-combat logging plugins, which block commands selectively.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent evt) {
        String[] cut = evt.getMessage().substring(1).split(" ");
        UserCommand userCommands = GUIShop.getINSTANCE().getUserCommands();

        if (GUIShop.BUY_COMMANDS.contains(cut[0])) {
            userCommands.buyCommand(evt.getPlayer(), (cut.length >= 2) ? cut[1] : null);
            evt.setCancelled(true);
        } else if (GUIShop.SELL_COMMANDS.contains(cut[0])) {
            userCommands.sellCommand(evt.getPlayer());
            evt.setCancelled(true);
        }
    }
}
