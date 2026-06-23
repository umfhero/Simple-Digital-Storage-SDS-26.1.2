package net.umf.polyfactory.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.polyfactory.PolyFactory;

/**
 * Deferred registration of all menu (container) types for Poly Factory.
 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PolyFactory.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<FabricatorMenu>> FABRICATOR_MENU =
            MENU_TYPES.register("fabricator_menu",
                    () -> IMenuTypeExtension.create(FabricatorMenu::clientFactory));

    private ModMenuTypes() {}

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
