package net.umf.polyfactory.recipe;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.umf.polyfactory.PolyFactory;

/**
 * NeoForge only sends a client the recipe types it opts into here - everything else (including
 * custom types like ours) is invisible to the client's RecipeMap, no matter how the recipes are
 * registered server-side. Without this, JEI (and the vanilla recipe book) never learns the
 * Fabricator has any recipes at all.
 */
@EventBusSubscriber(modid = PolyFactory.MODID)
public final class ModRecipeSync {

    private ModRecipeSync() {}

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.FABRICATING_TYPE.get());
    }
}
