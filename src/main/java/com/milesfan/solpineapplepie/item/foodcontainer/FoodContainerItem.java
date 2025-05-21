package com.milesfan.solpineapplepie.item.foodcontainer;

import com.milesfan.solpineapplepie.SOLPineapplePie;
import com.milesfan.solpineapplepie.integration.Origins;
import com.milesfan.solpineapplepie.tracking.FoodList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.entity.SlotAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.milesfan.solpineapplepie.lib.Localization.localized;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

@Mod.EventBusSubscriber(modid = SOLPineapplePie.MOD_ID)
public class FoodContainerItem extends Item {
    private String displayName;
    private int nslots;

    public FoodContainerItem(int nslots, String displayName) {
        super(new Properties().stacksTo(1).setNoRepair());

        this.displayName = displayName;
        this.nslots = nslots;
    }

    @SubscribeEvent
    public static void onCraftItem(PlayerEvent.ItemCraftedEvent event) {
        Container container = event.getInventory();
        int size = container.getContainerSize();
        for (int i = 0 ; i < size ; i++) {
            Item item = container.getItem(i).getItem();
            if (item instanceof FoodContainerItem foodContainerItem) {
                // --- コンテナアイテム使ったクラフトの場合
                // --- そのアイテムの中身を、新しいアイテムの中身にコピーする
                ItemStackHandler handler = getInventory(event.getCrafting());
                ItemStackHandler oldHander = getInventory(container.getItem(i));
                for (int j = 0 ; j < foodContainerItem.nslots ; j++)
                    handler.setStackInSlot(j, oldHander.getStackInSlot(j));
            }
        }
    }

    @Override
    public boolean isEdible() {
        return true;
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        // 获取午餐盒中的物品处理器
        ItemStackHandler handler = getInventory(stack);
        if (handler == null) {
            // 如果没有内容，返回空的食物属性但仍然可以尝试使用
            return new FoodProperties.Builder()
                .nutrition(0)
                .saturationMod(0)
                .alwaysEat()  // 始终可以尝试使用，即使没有食物
                .build();
        }
        
        // 查找最佳食物
        if (entity instanceof Player) {
            Player player = (Player) entity;
            int bestFoodSlot = getBestFoodSlot(handler, player);
            
            if (bestFoodSlot >= 0) {
                ItemStack bestFood = handler.getStackInSlot(bestFoodSlot);
                if (bestFood.isEdible() && !bestFood.isEmpty()) {
                    // 返回最佳食物的食物属性
                    FoodProperties originalProps = bestFood.getFoodProperties(entity);
                    if (originalProps != null) {
                        // 复制原始食物属性到新的构建器
                        FoodProperties.Builder builder = new FoodProperties.Builder()
                            .nutrition(originalProps.getNutrition())
                            .saturationMod(originalProps.getSaturationModifier())
                            .alwaysEat();  // 午餐盒始终可以使用
                        
                        // 如果有特殊效果，也复制过来
                        originalProps.getEffects().forEach(pair -> {
                            // 使用新的 effect 方法，传入 Supplier
                            builder.effect(() -> pair.getFirst(), pair.getSecond());
                        });
                        
                        // 如果有食物特性，也复制
                        if (originalProps.isMeat()) {
                            builder.meat();
                        }
                        
                        if (originalProps.isFastFood()) {
                            builder.fast();
                        }
                        
                        return builder.build();
                    }
                }
            }
        }
        
        // 如果没有玩家实体或没找到合适的食物，返回可以尝试使用的空属性
        return new FoodProperties.Builder()
            .nutrition(0)
            .saturationMod(0)
            .alwaysEat()  // 仍然可以右键尝试使用
            .build();
    }
    
    // 重写无参数版本的getFoodProperties以保持兼容性
    @Override
    public FoodProperties getFoodProperties() {
        // 返回可以始终使用的空食物属性
        return new FoodProperties.Builder().build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide && player.isCrouching()) {
            NetworkHooks.openScreen((ServerPlayer) player, new FoodContainerProvider(displayName), player.blockPosition());
        }

        if (!player.isCrouching()) {
            return processRightClick(world, player, hand);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private InteractionResultHolder<ItemStack> processRightClick(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isInventoryEmpty(stack) ||
                (ModList.get().isLoaded("origins") && Origins.hasRestrictedDiet(player))) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.canEat(false)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    private static boolean isInventoryEmpty(ItemStack container) {
        ItemStackHandler handler = getInventory(container);
        if (handler == null) {
            return true;
        }

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.isEdible()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FoodContainerCapabilityProvider(stack, nslots);
    }

    @Nullable
    public static ItemStackHandler getInventory(ItemStack bag) {
        if (bag.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent())
            return (ItemStackHandler) bag.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
        return null;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return stack;
        }

        Player player = (Player) entity;
        ItemStackHandler handler = getInventory(stack);
        if (handler == null) {
            return stack;
        }

        int bestFoodSlot = getBestFoodSlot(handler, player);
        if (bestFoodSlot < 0) {
            return stack;
        }

        ItemStack bestFood = handler.getStackInSlot(bestFoodSlot);
        ItemStack foodCopy = bestFood.copy();
        if (bestFood.isEdible() && !bestFood.isEmpty()) {
            ItemStack result = bestFood.finishUsingItem(world, entity);
            // put bowls/bottles etc. into player inventory
            if (!result.isEdible()) {
                handler.setStackInSlot(bestFoodSlot, ItemStack.EMPTY);
                Player playerEntity = (Player) entity;

                if (!playerEntity.getInventory().add(result)) {
                    playerEntity.drop(result, false);
                }
            }

            if (!world.isClientSide) {
                // Fire an event instead of directly updating the food list, so that
                // SoL: Carrot Edition registers the eaten food too.
                ForgeEventFactory.onItemUseFinish(player, foodCopy, 0, result);
            }
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack){
        return 32;
    }

    public static int getBestFoodSlot(ItemStackHandler handler, Player player) {
        FoodList foodList = FoodList.get(player);

        double maxDiversity = -Double.MAX_VALUE;
        int bestFoodSlot = -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack food = handler.getStackInSlot(i);

            if (!food.isEdible() || food.isEmpty())
                continue;
            double diversityChange = foodList.simulateFoodAdd(food.getItem());
            if (diversityChange > maxDiversity) {
                maxDiversity = diversityChange;
                bestFoodSlot = i;
            }
        }

        return bestFoodSlot;
    }

    public static double getFoodDiversity(ItemStack lunchboxStack, Player player) {
        ItemStackHandler handler = getInventory(lunchboxStack);
        if (handler == null) {
            return 0.0;
        }
        
        // 获取最佳食物槽位
        int bestFoodSlot = getBestFoodSlot(handler, player);
        if (bestFoodSlot < 0) {
            return 0.0;
        }
        
        ItemStack bestFood = handler.getStackInSlot(bestFoodSlot);
        if (!bestFood.isEdible() || bestFood.isEmpty()) {
            return 0.0;
        }
        
        // 获取玩家的食物列表
        FoodList foodList = FoodList.get(player);
        
        // 检查最佳食物的多样性贡献
        return foodList.simulateFoodAdd(bestFood.getItem());
    }
    

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        // 先调用 super，确保基础 tooltip 被添加
        super.appendHoverText(stack, world, tooltip, flag);
        
        // 添加容器内容列表
        ItemStackHandler handler = getInventory(stack);
        if (handler != null) {
            // 添加一个空行
            tooltip.add(Component.literal(""));
            
            // 添加标题
            tooltip.add(Component.literal(localized("tooltip", "container.contents", "内容物:")).withStyle(ChatFormatting.YELLOW));
            
            // 检查容器是否为空
            boolean isEmpty = true;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack foodStack = handler.getStackInSlot(i);
                if (!foodStack.isEmpty()) {
                    isEmpty = false;
                    // 添加食物名称
                    Component foodName = foodStack.getHoverName().copy();
                    tooltip.add(Component.literal(" • ").append(foodName).withStyle(ChatFormatting.GRAY));
                }
            }
            
            // 如果容器为空，显示提示信息
            if (isEmpty) {
                tooltip.add(Component.literal(" " + localized("tooltip", "container.empty", "空")).withStyle(ChatFormatting.GRAY));
            }
        }
        
        // 只在客户端添加多样性提示 - 使用安全的方式检查是否在客户端
        if (world != null && world.isClientSide()) {
            // 使用条件编译方式调用客户端方法
            addClientDiversityTooltip(stack, tooltip);
        }
    }

    // 使用@OnlyIn注解确保这个方法只在客户端被加载
    @OnlyIn(Dist.CLIENT)
    private void addClientDiversityTooltip(ItemStack stack, List<Component> tooltip) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            double diversity = getFoodDiversity(stack, player);
            if (diversity > 0) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal(localized("tooltip", "lunchbox.diversity", String.format("%.2f", diversity))).withStyle(ChatFormatting.GREEN));
            }
        }
    }
    
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack container, ItemStack food, Slot slot, ClickAction action, 
                                            Player player, SlotAccess access) {
        // 修改为检查右键点击而不是左键点击
        if (action == ClickAction.SECONDARY && FoodSlot.canHold(food) && !(food.getItem() instanceof FoodContainerItem)) {
            return tryAddItemToContainer(container, food, null);
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack container, Slot slot, ClickAction action, Player player) {
        // 修改为检查右键点击而不是左键点击
        if (action == ClickAction.SECONDARY) {
            ItemStack targetStack = slot.getItem();
            
            // Check if the target slot contains an edible item and is not a food container
            if (FoodSlot.canHold(targetStack) && !(targetStack.getItem() instanceof FoodContainerItem)) {
                return tryAddItemToContainer(container, targetStack, slot);
            }
        }
        return false;
    }
    
    /**
     * Helper method to add an item to the food container
     * @param container The food container item stack
     * @param itemToAdd The item stack to add to the container
     * @param sourceSlot The slot containing the item (can be null if not from a slot)
     * @return true if the item was added, false otherwise
     */
    private boolean tryAddItemToContainer(ItemStack container, ItemStack itemToAdd, @Nullable Slot sourceSlot) {
        // Get the container's inventory
        ItemStackHandler handler = getInventory(container);
        if (handler != null) {
            // First try to stack with existing items
            boolean added = false;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack existingStack = handler.getStackInSlot(i);
                if (!existingStack.isEmpty() && ItemStack.isSameItemSameTags(existingStack, itemToAdd) && existingStack.getCount() < existingStack.getMaxStackSize()) {
                    // Found matching item with space, add to stack
                    int spaceAvailable = existingStack.getMaxStackSize() - existingStack.getCount();
                    int amountToAdd = Math.min(1, spaceAvailable);
                    
                    existingStack.grow(amountToAdd);
                    handler.setStackInSlot(i, existingStack);
                    
                    // Decrease the count of the original item stack
                    itemToAdd.shrink(amountToAdd);
                    
                    // Update the slot if provided
                    if (sourceSlot != null) {
                        sourceSlot.set(itemToAdd);
                    }
                    
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
                        ItemStack itemCopy = itemToAdd.copy();
                        itemCopy.setCount(1);
                        handler.setStackInSlot(i, itemCopy);
                        
                        // Decrease the count of the original item stack
                        itemToAdd.shrink(1);
                        
                        // Update the slot if provided
                        if (sourceSlot != null) {
                            sourceSlot.set(itemToAdd);
                        }
                        
                        added = true;
                        break;
                    }
                }
            }
            
            return added;
        }
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
    }

    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        
        // Get the capability data and store it in the main tag
        ItemStackHandler handler = getInventory(stack);
        if (handler != null) {
            tag.put("InventoryContents", handler.serializeNBT());
        }
        
        return tag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
        if (nbt == null) {
            stack.setTag(null);
            return;
        }
        
        stack.setTag(nbt);
        
        // If we have stored inventory data, restore it to the capability
        if (nbt.contains("InventoryContents")) {
            ItemStackHandler handler = getInventory(stack);
            if (handler != null) {
                handler.deserializeNBT(nbt.getCompound("InventoryContents"));
            }
        }
    }
}
