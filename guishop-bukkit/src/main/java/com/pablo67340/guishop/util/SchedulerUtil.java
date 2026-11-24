package com.pablo67340.guishop.util;

import com.pablo67340.guishop.GUIShop;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

/**
 * Scheduler utility that provides cross-compatibility between
 * Spigot, Paper, and Folia servers using FoliaLib.
 * 
 * On Folia: Uses regionized schedulers (entity/region/global)
 * On Paper/Spigot: Falls back to standard BukkitScheduler
 */
public class SchedulerUtil {
    
    private static FoliaLib foliaLib;
    
    /**
     * Initialize the scheduler utility.
     * Call this in onEnable().
     */
    public static void init(GUIShop plugin) {
        foliaLib = new FoliaLib(plugin);
    }
    
    /**
     * Check if the server is running Folia.
     */
    public static boolean isFolia() {
        return foliaLib != null && foliaLib.isFolia();
    }
    
    /**
     * Run a task on the next server tick (global region on Folia).
     * Use for non-entity/location specific tasks.
     */
    public static void runTask(Runnable task) {
        foliaLib.getScheduler().runNextTick(t -> task.run());
    }
    
    /**
     * Run a task asynchronously.
     * Safe on both Folia and Paper/Spigot.
     */
    public static void runTaskAsync(Runnable task) {
        foliaLib.getScheduler().runAsync(t -> task.run());
    }
    
    /**
     * Run a task after a delay (in ticks) on the global region.
     */
    public static void runTaskLater(Runnable task, long delayTicks) {
        foliaLib.getScheduler().runLater(t -> task.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Run a task asynchronously after a delay (in ticks).
     */
    public static void runTaskLaterAsync(Runnable task, long delayTicks) {
        foliaLib.getScheduler().runLaterAsync(t -> task.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Run a repeating task asynchronously and capture the task reference.
     * @param holder TaskHolder to receive the WrappedTask reference
     * @param task The task to run
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     */
    public static void runTaskTimerAsync(TaskHolder holder, Runnable task, long delayTicks, long periodTicks) {
        foliaLib.getScheduler().runTimerAsync(t -> {
            holder.setTask(t);
            task.run();
        }, delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }
    
    // ==================== Entity-based scheduling (required for Folia) ====================
    
    /**
     * Run a task for a specific entity on its region thread.
     * REQUIRED for Folia when dealing with player/entity operations.
     * 
     * @param entity The entity (usually a Player)
     * @param task The task to run
     */
    public static void runAtEntity(Entity entity, Runnable task) {
        foliaLib.getScheduler().runAtEntity(entity, t -> task.run());
    }
    
    /**
     * Run a task for a specific entity after a delay.
     * REQUIRED for Folia when dealing with player/entity operations.
     */
    public static void runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        foliaLib.getScheduler().runAtEntityLater(entity, t -> task.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Run a repeating task for a specific entity.
     */
    public static void runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        foliaLib.getScheduler().runAtEntityTimer(entity, t -> task.run(), delayTicks * 50, periodTicks * 50, TimeUnit.MILLISECONDS);
    }
    
    // ==================== Location-based scheduling ====================
    
    /**
     * Run a task at a specific location's region.
     * Use for block/world operations at a specific location.
     */
    public static void runAtLocation(Location location, Runnable task) {
        foliaLib.getScheduler().runAtLocation(location, t -> task.run());
    }
    
    /**
     * Run a task at a specific location after a delay.
     */
    public static void runAtLocationLater(Location location, Runnable task, long delayTicks) {
        foliaLib.getScheduler().runAtLocationLater(location, t -> task.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
    }
    
    // ==================== Task cancellation ====================
    
    /**
     * Cancel a wrapped task.
     */
    public static void cancelTask(WrappedTask task) {
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Cancel a task holder.
     */
    public static void cancelTask(TaskHolder holder) {
        if (holder != null && holder.getTask() != null) {
            holder.getTask().cancel();
        }
    }
    
    /**
     * Cancel all tasks from GUIShop.
     * Call this in onDisable().
     */
    public static void cancelAllTasks() {
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
    }
    
    /**
     * Holder class for capturing WrappedTask references from async callbacks.
     */
    public static class TaskHolder {
        private volatile WrappedTask task;
        
        public WrappedTask getTask() {
            return task;
        }
        
        public void setTask(WrappedTask task) {
            this.task = task;
        }
    }
}
