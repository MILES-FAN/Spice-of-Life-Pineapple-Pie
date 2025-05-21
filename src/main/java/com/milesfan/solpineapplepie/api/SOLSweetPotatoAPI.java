package com.milesfan.solpineapplepie.api;

import com.milesfan.solpineapplepie.tracking.CapabilityHandler;
import com.milesfan.solpineapplepie.tracking.FoodList;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 Provides a stable API for interfacing with Spice of Life: Carrot Edition.
 */
public final class SOLSweetPotatoAPI
{
	public static final Capability<FoodCapability> foodCapability = CapabilityManager.get(new CapabilityToken<>() {});;
	
	private SOLSweetPotatoAPI() {}
	
	/**
	 Retrieves the {@link FoodCapability} for the given player.
	 */
	public static FoodCapability getFoodCapability(Player player) {
		return FoodList.get(player);
	}
	
	/**
	 Synchronizes the food list for the given player to the client, updating their max health in the process.
	 */
	public static void syncFoodList(Player player) {
		CapabilityHandler.syncFoodList(player);
	}
}
