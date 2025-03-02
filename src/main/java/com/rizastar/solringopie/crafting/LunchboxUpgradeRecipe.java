package com.rizastar.solringopie.crafting;

import com.rizastar.solringopie.item.SOLRingoPieItems;
import com.rizastar.solringopie.item.foodcontainer.FoodContainerItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class LunchboxUpgradeRecipe extends ShapedRecipe  {
    public static final Set<ResourceLocation> REGISTERED_RECIPES = new LinkedHashSet<>();
    private final ShapedRecipe compose;

    public  LunchboxUpgradeRecipe(ShapedRecipe compose, Level level) {
        super(compose.getId(), compose.getGroup(), compose.category(), compose.getRecipeWidth(), compose.getRecipeHeight(), compose.getIngredients(), compose.getResultItem(level.registryAccess()));
        this.compose = compose;
        REGISTERED_RECIPES.add(compose.getId());
    }

    private Optional<ItemStack> getLunchbox(CraftingContainer inv) {
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack slotStack = inv.getItem(slot);
            if (slotStack.getItem() instanceof FoodContainerItem) {
                return Optional.of(slotStack);
            }
        }

        return Optional.empty();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SOLRingoPieItems.LUNCHBOX_UPGRADE_RECIPE_SERIALIZER.get();
    }

    public static class Serializer extends RecipeWrapperSerializer<ShapedRecipe, LunchboxUpgradeRecipe> {
        public Serializer() {
            super(LunchboxUpgradeRecipe::new, RecipeSerializer.SHAPED_RECIPE);
        }
    }
}
