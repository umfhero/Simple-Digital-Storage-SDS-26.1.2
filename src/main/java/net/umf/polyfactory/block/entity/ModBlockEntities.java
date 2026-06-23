package net.umf.polyfactory.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.polyfactory.PolyFactory;

/**
 * Deferred registration of all block entity types for Poly Factory.
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PolyFactory.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FabricatorBlockEntity>> FABRICATOR =
            BLOCK_ENTITY_TYPES.register("fabricator",
                    () -> new BlockEntityType<>(FabricatorBlockEntity::new, PolyFactory.FABRICATOR.get()));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
