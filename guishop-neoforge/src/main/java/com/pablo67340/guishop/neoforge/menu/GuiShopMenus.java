package com.pablo67340.guishop.neoforge.menu;

import com.pablo67340.guishop.neoforge.GuiShopMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's container types.
 * <p>
 * Only the sell container needs one: every other screen in the mod is driven by
 * payloads rather than by slots, and a container is only worth the registration
 * when a player has to be able to pick items up and put them down.
 */
public final class GuiShopMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, GuiShopMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SellMenu>> SELL =
            MENUS.register("sell", () -> new MenuType<>(SellMenu::new, FeatureFlags.VANILLA_SET));

    private GuiShopMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
