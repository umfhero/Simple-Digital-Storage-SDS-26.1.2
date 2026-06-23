package net.umf.polyfactory.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.polyfactory.PolyFactory;

/**
 * Deferred registration of the Fabricator's recipe type and serializer.
 */
public final class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, PolyFactory.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PolyFactory.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FabricatingRecipe>> FABRICATING_TYPE =
            RECIPE_TYPES.register("fabricating",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(PolyFactory.MODID, "fabricating")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FabricatingRecipe>> FABRICATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("fabricating", () -> FabricatingRecipe.SERIALIZER);

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
