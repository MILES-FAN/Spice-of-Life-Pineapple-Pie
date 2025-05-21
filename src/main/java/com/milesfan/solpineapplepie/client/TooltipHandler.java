package com.milesfan.solpineapplepie.client;

import com.milesfan.solpineapplepie.SOLPineapplePie;
import com.milesfan.solpineapplepie.SOLPineapplePieConfig;
import com.milesfan.solpineapplepie.tracking.FoodInstance;
import com.milesfan.solpineapplepie.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static com.milesfan.solpineapplepie.lib.Localization.localized;
import static com.milesfan.solpineapplepie.lib.Localization.localizedComponent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLPineapplePie.MOD_ID)
public final class TooltipHandler {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (!SOLPineapplePieConfig.isFoodTooltipEnabled()) return;
		
		Player player = event.getEntity();
		if (player == null) return;
		
		Item food = event.getItemStack().getItem();
		if (!food.isEdible()) return;
		
		FoodList foodList = FoodList.get(player);
		boolean hasBeenEaten = foodList.hasEaten(food);
		boolean isAllowed = SOLPineapplePieConfig.isAllowed(food);

		List<Component> tooltip = event.getToolTip();
		if (!isAllowed) {
			tooltip.add(localizedTooltip("disabled", ChatFormatting.DARK_GRAY));
		} else {
			if (hasBeenEaten) {
				int lastEaten = foodList.getLastEaten(food);
				double contribution = FoodList.calculateDiversityContribution(new FoodInstance(food), lastEaten);

				addDiversityInfoTooltips(tooltip, contribution, lastEaten);
			}
			else {
				addDiversityInfoTooltips(tooltip, FoodList.getComplexity(new FoodInstance(food)), -1);
			}
		}
	}
	
	private static Component localizedTooltip(String path, ChatFormatting color) {
		return localizedComponent("tooltip", path).withStyle(style -> style.applyFormat(color));
	}

	public static List<Component> addDiversityInfoTooltips(List<Component> tooltip, double contribution, int lastEaten) {
		String contribution_path = lastEaten == -1 ? "food_book.queue.tooltip.uneaten_contribution_label" : "food_book.queue.tooltip.contribution_label";
		tooltip.add(Component.literal(localized("gui", contribution_path) + ": " + String.format("%.2f", contribution)).withStyle(lastEaten == -1 ? ChatFormatting.GRAY : ChatFormatting.GOLD));

		if (lastEaten != -1)
		{
			String last_eaten_path = "food_book.queue.tooltip.last_eaten_label";
			if (lastEaten == 1)
			{
				last_eaten_path = "food_book.queue.tooltip.last_eaten_label_singular";
			}
			tooltip.add(Component.literal(localized("gui", last_eaten_path, lastEaten)).withStyle(ChatFormatting.GRAY));
		}

		return tooltip;
	}
	
	private TooltipHandler() {}
}
