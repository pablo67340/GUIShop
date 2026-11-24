package com.pablo67340.guishop.platform;

/**
 * Task scheduling, as provided by the host platform.
 * <p>
 * Deliberately narrow: this declares only what core actually needs, rather than
 * mirroring the platform's full scheduler. Entity- and location-targeted
 * scheduling, which Folia requires and which has no meaning off Bukkit, stays
 * in the platform module.
 */
public interface PlatformScheduler {

    /**
     * Runs a repeating task off the main thread.
     *
     * @param task        the task to run
     * @param delayTicks  ticks to wait before the first run
     * @param periodTicks ticks between runs
     * @return a handle that cancels the repeat
     */
    CancellableTask runTimerAsync(Runnable task, long delayTicks, long periodTicks);
}
