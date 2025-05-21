package com.milesfan.solpineapplepie.client.gui.elements;

import com.milesfan.solpineapplepie.client.TooltipHandler;
import com.milesfan.solpineapplepie.tracking.FoodInstance;
import com.milesfan.solpineapplepie.tracking.FoodList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;


/**
 * Renders an ItemStack representing a food in the FoodList. Has a unique tooltip that displays that food item's
 * contribution to the food diversity.
 */
public class UIFoodQueueItem extends UIItemStack{
    private final int lastEaten;

    public UIFoodQueueItem(ItemStack itemStack, int lastEaten) {
        super(itemStack);

        this.lastEaten = lastEaten;
    }

    @Override
    protected void renderTooltip(GuiGraphics matrices, int mouseX, int mouseY) {
        List<Component> tooltip = getFoodQueueTooltip();
        renderTooltip(matrices, itemStack, tooltip, mouseX, mouseY);
    }

    private List<Component> getFoodQueueTooltip() {
        Component foodName =  Component.translatable(itemStack.getItem().getDescriptionId(itemStack))
                .withStyle(itemStack.getRarity().color);

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(foodName);

        Component space = Component.literal("");
        tooltip.add(space);

        double contribution = FoodList.calculateDiversityContribution(new FoodInstance(itemStack.getItem()), lastEaten);

        TooltipHandler.addDiversityInfoTooltips(tooltip, contribution, lastEaten);

        return tooltip;
    }
}
