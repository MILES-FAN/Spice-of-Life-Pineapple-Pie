/**
 * Much of the following code was adapted from Cyclic's storage bag code.
 * Copyright for portions of the code are held by Samson Basset (Lothrazar)
 * as part of Cyclic, under the MIT license.
 */
package com.milesfan.solpineapplepie.item.foodcontainer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FoodContainerCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    private final int slots;
    private final LazyOptional<ItemStackHandler> inventory;
    private final ItemStack stack;

    public FoodContainerCapabilityProvider(ItemStack stack, int slots) {
        this.stack = stack;
        this.slots = slots;
        
        // Create the inventory handler
        ItemStackHandler handler = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return !(stack.getItem() instanceof FoodContainerItem) && super.isItemValid(slot, stack);
            }
            
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                // When contents change, update the NBT tag
                CompoundTag tag = stack.getOrCreateTag();
                tag.put("InventoryContents", serializeNBT());
            }
        };
        
        // Initialize from existing NBT if available
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("InventoryContents")) {
            handler.deserializeNBT(tag.getCompound("InventoryContents"));
        }
        
        this.inventory = LazyOptional.of(() -> handler);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return inventory.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        if (inventory.isPresent()) {
            return inventory.resolve().get().serializeNBT();
        }
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        inventory.ifPresent(h -> h.deserializeNBT(nbt));
    }
}