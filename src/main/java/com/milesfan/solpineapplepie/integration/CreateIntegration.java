package com.milesfan.solpineapplepie.integration;

import com.milesfan.solpineapplepie.SOLPineapplePie;
import com.milesfan.solpineapplepie.item.foodcontainer.FoodContainerItem;
import com.milesfan.solpineapplepie.item.foodcontainer.FoodSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import com.milesfan.solpineapplepie.item.SOLPineapplePieItems;
import java.util.stream.Stream;

/**
 * Integration with Create mod to support mechanical hands/deployers with FoodContainer.
 * Uses soft dependency pattern to avoid crashes when Create mod is not present.
 */
@Mod.EventBusSubscriber(modid = SOLPineapplePie.MOD_ID)
public class CreateIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CREATE_MOD_ID = "create";
    private static boolean isCreateLoaded = false;
    
    // Initialize in static block to safely check if Create is loaded
    static {
        isCreateLoaded = ModList.get().isLoaded(CREATE_MOD_ID);
        if (isCreateLoaded) {
            LOGGER.info("Create mod detected, enabling integration for FoodContainer");
        }
    }

    /**
     * Register our deployer handler with Create via IMC
     */
    @SubscribeEvent
    public static void onInterModComms(InterModEnqueueEvent event) {
        if (!isCreateLoaded) {
            return; // Skip if Create is not loaded
        }
        
        try {
            // Register our deployer behavior using a Class object reference to avoid direct class references
            InterModComms.sendTo(CREATE_MOD_ID, "registerDeployerHandler", () -> {
                try {
                    return Class.forName("com.milesfan.solpineapplepie.integration.CreateIntegration$FoodContainerDeployerHandler")
                        .getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    LOGGER.error("Failed to create deployer handler instance", e);
                    return null;
                }
            });
            LOGGER.info("Successfully sent IMC message to Create for FoodContainer integration");
        } catch (Exception e) {
            LOGGER.error("Error sending IMC to Create mod", e);
        }
    }
    
    /**
     * Register a deployer recipe serializer - this is needed for deployment recipes
     */
    @SubscribeEvent
    public static void onRegisterSerializers(RegisterEvent event) {
        if (!isCreateLoaded) {
            return; // Skip if Create is not loaded
        }
        
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS)) {
            try {
                // Load Create API classes dynamically
                Class<?> processingRecipeBuilderClass = Class.forName("com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder");
                
                // Use reflection to invoke the static compatibility method
                Object recipeBuilder = processingRecipeBuilderClass.getMethod("compatibility", String.class)
                    .invoke(null, "deploying");
                
                // Continue building the recipe using reflection
                recipeBuilder = processingRecipeBuilderClass.getMethod("require", Ingredient.class)
                    .invoke(recipeBuilder, tagAllFoodContainers());
                
                recipeBuilder = processingRecipeBuilderClass.getMethod("require", Ingredient.class)
                    .invoke(recipeBuilder, tagAllFoodItems());
                
                recipeBuilder = processingRecipeBuilderClass.getMethod("output", ItemStack.class, float.class)
                    .invoke(recipeBuilder, itemWithNBT(), 1.0f);
                
                // Build and register the recipe
                Object recipe = processingRecipeBuilderClass.getMethod("build", ResourceLocation.class)
                    .invoke(recipeBuilder, new ResourceLocation(SOLPineapplePie.MOD_ID, "fill_food_container"));
                
                // Set registry name
                recipe.getClass().getMethod("setRegistryName", String.class, String.class)
                    .invoke(recipe, SOLPineapplePie.MOD_ID, "fill_food_container");
                
                LOGGER.info("Successfully registered FoodContainer filling recipe");
            } catch (Exception e) {
                // Log the error but don't crash
                LOGGER.error("Failed to register Create recipes for FoodContainer", e);
            }
        }
    }
    
    /**
     * Create a tag reference for all food containers
     */
    private static Ingredient tagAllFoodContainers() {
        // 使用已注册的食物容器项目
        return Ingredient.of(
            SOLPineapplePieItems.LUNCHBOX.get(),
            SOLPineapplePieItems.LUNCHBAG.get(),
            SOLPineapplePieItems.GOLDEN_LUNCHBOX.get()
        );
    }
    
    /**
     * Create a tag reference for all food items
     */
    private static Ingredient tagAllFoodItems() {
        // Simplified approach - just return an empty ingredient
        // The actual filtering will happen in the deployer handler
        return Ingredient.EMPTY;
    }
    
    /**
     * Create an example filled container for display purposes
     */
    private static ItemStack itemWithNBT() {
        ItemStack exampleStack = new ItemStack(SOLPineapplePieItems.LUNCHBOX.get());
        CompoundTag tag = exampleStack.getOrCreateTag();
        tag.putString("ExampleFood", "Contains Food");
        return exampleStack;
    }
    
    /**
     * Interface that mirrors Create's DeployerHandler.DeployerApplicationHandler
     * to avoid direct class references.
     * This must match the Create API interface exactly.
     */
    public interface IDeployerApplicationHandler {
        boolean canApply(ItemStack deployed, ItemStack target, Level world);
        InteractionResult handle(ItemStack deployed, ItemStack target, Object player, Level world);
    }
    
    /**
     * Custom deployer handler for food containers.
     * Use String literals for all Create API references to allow
     * classloading to fail gracefully when Create is absent.
     */
    public static class FoodContainerDeployerHandler implements IDeployerApplicationHandler {
        
        @Override
        public boolean canApply(ItemStack container, ItemStack food, Level world) {
            // Check if the held item is a food container
            if (!(container.getItem() instanceof FoodContainerItem)) {
                return false;
            }
            
            // Check if the deployed item is a valid food
            if (!FoodSlot.canHold(food) || food.getItem() instanceof FoodContainerItem) {
                return false;
            }
            
            // Check if there's space in the container
            ItemStackHandler handler = FoodContainerItem.getInventory(container);
            if (handler == null) {
                return false;
            }
            
            // Check if there's available space
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack slotStack = handler.getStackInSlot(i);
                if (slotStack.isEmpty() || 
                    (ItemStack.isSameItemSameTags(slotStack, food) && 
                     slotStack.getCount() < slotStack.getMaxStackSize())) {
                    return true;
                }
            }
            
            return false;
        }
        
        @Override
        public InteractionResult handle(ItemStack container, ItemStack food, Object player, Level world) {
            if (!(container.getItem() instanceof FoodContainerItem)) {
                return InteractionResult.PASS;
            }
            
            // Simulate the behavior of the container trying to accept food
            boolean added = tryAddFoodToContainer(container, food);
            
            if (added) {
                try {
                    // Get the block position of the player using reflection to avoid direct Create dependency
                    Object blockPosObj = player.getClass().getMethod("blockPosition").invoke(player);
                    
                    // Convert the Object to a BlockPos
                    net.minecraft.core.BlockPos blockPos = (net.minecraft.core.BlockPos) blockPosObj;
                    
                    // Trigger appropriate sounds/particles
                    world.playSound(null, blockPos, 
                                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 
                                    net.minecraft.sounds.SoundSource.BLOCKS, 
                                    0.5f, 1.0f);
                } catch (Exception e) {
                    LOGGER.error("Error getting deployer position", e);
                }
                
                return InteractionResult.SUCCESS;
            }
            
            return InteractionResult.PASS;
        }
        
        /**
         * Utility method to add food to container - similar to FoodContainerItem.tryAddItemToContainer
         */
        private boolean tryAddFoodToContainer(ItemStack container, ItemStack food) {
            // Get the container's inventory
            ItemStackHandler handler = FoodContainerItem.getInventory(container);
            if (handler == null) {
                return false;
            }
            
            // First try to stack with existing items
            boolean added = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack existingStack = handler.getStackInSlot(i);
                if (!existingStack.isEmpty() && 
                    ItemStack.isSameItemSameTags(existingStack, food) && 
                    existingStack.getCount() < existingStack.getMaxStackSize()) {
                    
                    // Found matching item with space, add to stack
                    int spaceAvailable = existingStack.getMaxStackSize() - existingStack.getCount();
                    int amountToAdd = Math.min(1, spaceAvailable);
                    
                    existingStack.grow(amountToAdd);
                    handler.setStackInSlot(i, existingStack);
                    
                    // Decrease the count of the original item stack
                    food.shrink(amountToAdd);
                    
                    added = true;
                    break;
                }
            }
            
            // If we couldn't stack, try to find an empty slot
            if (!added) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack existingStack = handler.getStackInSlot(i);
                    if (existingStack.isEmpty()) {
                        // Found an empty slot, insert the item
                        ItemStack itemCopy = food.copy();
                        itemCopy.setCount(1);
                        handler.setStackInSlot(i, itemCopy);
                        
                        // Decrease the count of the original item stack
                        food.shrink(1);
                        
                        added = true;
                        break;
                    }
                }
            }
            
            // Make sure to update the container's NBT
            CompoundTag tag = container.getOrCreateTag();
            tag.put("InventoryContents", handler.serializeNBT());
            
            return added;
        }
    }
}