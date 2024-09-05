package com.pablo67340.guishop.listenable;

import better.reload.api.ReloadEvent;
import com.pablo67340.guishop.GUIShop;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ReloadListener implements Listener {

    /**
     * An instance of a {@link ReloadListener} that will be used to handle this
     * specific object reference from other classes, even though methods here
     * will be static.
     */
    public static final ReloadListener INSTANCE = new ReloadListener();

    @EventHandler
    public void onReload(ReloadEvent event) {
        GUIShop.getINSTANCE().reload(event.getCommandSender(), false);
    }
}
