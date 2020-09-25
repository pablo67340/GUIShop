package com.pablo67340.guishop.commands;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.pablo67340.guishop.Main;

/**
 * Listener when the commands mode is
 * {@link com.pablo67340.guishop.definition.CommandsMode#INTERCEPT CommandsMode.INTERCEPT}.
 *
 */
public class CommandsInterceptor implements Listener {

    private static CommandsInterceptor inst;

    /**
     * Registers the listener if not yet registered
     *
     */
    public static void register() {
        if (inst == null) {
            inst = new CommandsInterceptor();
            Bukkit.getPluginManager().registerEvents(inst, Main.getINSTANCE());
        }
    }

    /**
     * Unregisters the listener if already registered
     *
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
        GuishopUserCommand userCmds = Main.getINSTANCE().getUserCommands();

        if (Main.BUY_COMMANDS.contains(cut[0])) {
            userCmds.buyCommand(evt.getPlayer(), (cut.length >= 2) ? cut[1] : null);
            evt.setCancelled(true);

        } else if (Main.SELL_COMMANDS.contains(cut[0])) {
            userCmds.sellCommand(evt.getPlayer());
            evt.setCancelled(true);
        }
    }

}
