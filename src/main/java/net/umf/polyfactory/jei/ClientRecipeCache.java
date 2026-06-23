package net.umf.polyfactory.jei;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.umf.polyfactory.PolyFactory;

/**
 * The client no longer exposes a queryable-by-type recipe manager (only
 * {@code RecipeAccess#propertySet}/{@code stonecutterRecipes}), so mods that need the full recipe
 * set - like our JEI plugin - have to capture it from NeoForge's {@link RecipesReceivedEvent}
 * when it fires instead of pulling it from the level on demand.
 */
@EventBusSubscriber(modid = PolyFactory.MODID, value = Dist.CLIENT)
public final class ClientRecipeCache {

    private static RecipeMap recipeMap = RecipeMap.EMPTY;

    private ClientRecipeCache() {}

    @SubscribeEvent
    static void onRecipesReceived(RecipesReceivedEvent event) {
        recipeMap = event.getRecipeMap();
    }

    public static RecipeMap get() {
        return recipeMap;
    }
}
