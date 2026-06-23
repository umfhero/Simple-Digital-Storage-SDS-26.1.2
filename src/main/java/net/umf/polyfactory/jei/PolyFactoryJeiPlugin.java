package net.umf.polyfactory.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.recipe.FabricatingRecipe;
import net.umf.polyfactory.recipe.ModRecipes;

import java.util.List;

/**
 * JEI integration for the Fabricator. The Fabricator is a workstation (like a furnace) that
 * processes {@link FabricatingRecipe}s rather than consuming them as a crafting ingredient, so it
 * must be registered as a crafting station for the {@code polyfactory:fabricating} category -
 * otherwise JEI's "uses" lookup on the Fabricator item has nothing to point to.
 */
@JeiPlugin
public class PolyFactoryJeiPlugin implements IModPlugin {

    public static final IRecipeType<FabricatingRecipe> FABRICATING =
            IRecipeType.create(PolyFactory.MODID, "fabricating", FabricatingRecipe.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(PolyFactory.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FabricatingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // The client no longer keeps a queryable-by-type RecipeManager (RecipeAccess only exposes
        // propertySet/stonecutterRecipes), so the live recipe set has to come from the RecipeMap
        // NeoForge hands us via RecipesReceivedEvent - see ClientRecipeCache.
        List<FabricatingRecipe> recipes = ClientRecipeCache.get().byType(ModRecipes.FABRICATING_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(FABRICATING, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(FABRICATING, PolyFactory.FABRICATOR_ITEM.get());
    }
}
