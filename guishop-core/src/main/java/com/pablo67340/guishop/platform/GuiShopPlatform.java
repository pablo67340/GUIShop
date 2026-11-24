package com.pablo67340.guishop.platform;

/**
 * Holds the active {@link GuiShopContext}.
 * <p>
 * A static holder rather than constructor injection, because core classes are
 * reached from hundreds of existing call sites that have no context to thread
 * through. {@link #clear()} exists so the plugin's hard-reload path can drop the
 * reference alongside its other singletons.
 */
public final class GuiShopPlatform {

    private static volatile GuiShopContext context;

    private GuiShopPlatform() {
    }

    public static void set(GuiShopContext context) {
        GuiShopPlatform.context = context;
    }

    public static GuiShopContext get() {
        GuiShopContext current = context;
        if (current == null) {
            throw new IllegalStateException(
                    "No GuiShopContext registered. The platform module must call GuiShopPlatform.set() during startup.");
        }
        return current;
    }

    /**
     * @return whether a context is currently registered, for shutdown paths that
     *         must not throw.
     */
    public static boolean isAvailable() {
        return context != null;
    }

    public static void clear() {
        context = null;
    }
}
