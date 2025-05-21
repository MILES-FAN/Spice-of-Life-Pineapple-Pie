package com.milesfan.solpineapplepie.tracking;

import com.milesfan.solpineapplepie.ConfigHandler;
import com.milesfan.solpineapplepie.SOLPineapplePieConfig;
import com.milesfan.solpineapplepie.api.FoodCapability;
import com.milesfan.solpineapplepie.api.SOLSweetPotatoAPI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;


@ParametersAreNonnullByDefault
public final class FoodList implements FoodCapability {
	private static final String NBT_KEY_FOOD_LIST = "foodList";
	private static final String NBT_KEY_UNIQUE_FOOD = "food";
	private static final String NBT_KEY_LAST_EATEN = "lastEaten";
	private static final String NBT_KEY_FOODS_EATEN = "foodsEaten";
	
	public static FoodList get(Player player) {
		return (FoodList) player.getCapability(SOLSweetPotatoAPI.foodCapability)
			.orElseThrow(FoodListNotFoundException::new);
	}

	private static final int MAX_FOODS_EATEN = 1000;
	private int foodsEaten = 0;
	// Keys - foods eaten, Values - lastEaten, i.e. # meals ago the food was last eaten
	private final Map<FoodInstance, Integer> uniqueFoods = new HashMap<>();
	
	public FoodList() {}
	
	private final LazyOptional<FoodList> capabilityOptional = LazyOptional.of(() -> this);
	
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
		return capability == SOLSweetPotatoAPI.foodCapability ? capabilityOptional.cast() : LazyOptional.empty();
	}

	@Nullable
	private CompoundTag serializeUniqueFood(Map.Entry<FoodInstance, Integer> foodPair) {
		FoodInstance food = foodPair.getKey();
		Integer lastEaten = foodPair.getValue();

		String encodedFood = food.encode();
		if (encodedFood == null) {
			return null;
		}

		CompoundTag tag = new CompoundTag();
		StringTag s = StringTag.valueOf(encodedFood);
		FloatTag i = FloatTag.valueOf(lastEaten);
		tag.put(NBT_KEY_UNIQUE_FOOD, s);
		tag.put(NBT_KEY_LAST_EATEN, i);

		return tag;
	}
	
	/** used for persistent storage */
	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		
		ListTag list = new ListTag();

		uniqueFoods.entrySet().stream()
			.map(this::serializeUniqueFood)
			.filter(Objects::nonNull)
			.forEach(list::add);
		tag.put(NBT_KEY_FOOD_LIST, list);
		tag.put(NBT_KEY_FOODS_EATEN, IntTag.valueOf(foodsEaten));

		return tag;
	}

	@Nullable
	private Pair<FoodInstance, Integer> deserializeUniqueFood(Pair<String, Float> encoded) {
		FoodInstance uniqueFood = FoodInstance.decode(encoded.getKey());
		Integer lastEaten = Math.round(encoded.getValue());

		if (uniqueFood == null)
			return null;

		return new ImmutablePair<>(uniqueFood, lastEaten);
	}

	/** used for persistent storage */
	@Override
	public void deserializeNBT(CompoundTag tag) {
		ListTag list = tag.getList(NBT_KEY_FOOD_LIST, Tag.TAG_COMPOUND);

		uniqueFoods.clear();
		list.stream()
			.map(nbt-> (CompoundTag) nbt)
			.map(nbt -> new ImmutablePair<>(nbt.getString(NBT_KEY_UNIQUE_FOOD), nbt.getFloat(NBT_KEY_LAST_EATEN)))
			.map(this::deserializeUniqueFood)
			.filter(Objects::nonNull)
			.forEach(pair -> uniqueFoods.put(pair.getKey(), pair.getValue()));
		foodsEaten = tag.getInt(NBT_KEY_FOODS_EATEN);
	}

	public void addFood(Item food, Map<FoodInstance, Integer> foodMap) {
		if (!SOLPineapplePieConfig.shouldCount(food) && !SOLPineapplePieConfig.shouldForbiddenCount()) {
			return;
		}

		if (foodsEaten < MAX_FOODS_EATEN) {
			foodsEaten++;
		}

		ArrayList<FoodInstance> toRemove = new ArrayList<>();

		for (Map.Entry<FoodInstance, Integer> entry : foodMap.entrySet()) {
			FoodInstance foodInstance = entry.getKey();
			Integer lastEaten = entry.getValue();

			lastEaten++;
			foodMap.put(foodInstance, lastEaten);

			if (lastEaten >= SOLPineapplePieConfig.size()) {
				toRemove.add(foodInstance);
			}
		}

		for (FoodInstance foodInstance : toRemove) {
			foodMap.remove(foodInstance);
		}

		if (SOLPineapplePieConfig.shouldCount(food)) {
			FoodInstance newlyEaten = new FoodInstance(food);
			foodMap.put(newlyEaten, 0);
		}
	}
	
	public void addFood(Item food) {
		addFood(food, uniqueFoods);
	}

	/**
	 * @return The change in food diversity from eating the food.
	 */
	public double simulateFoodAdd(Item food) {
		if (!SOLPineapplePieConfig.shouldCount(food) && !SOLPineapplePieConfig.shouldForbiddenCount()) {
			return 0.0;
		}
		double change = 0.0;

		for (Map.Entry<FoodInstance, Integer> entry : uniqueFoods.entrySet()) {
			FoodInstance foodInstance = entry.getKey();
			Integer lastEaten = entry.getValue();

			double diversityContribution = calculateDiversityContribution(foodInstance, lastEaten);
			lastEaten++;

			if (foodInstance.getItem().equals(food)) {
				change -= diversityContribution;
			}
			else if (lastEaten >= SOLPineapplePieConfig.size()) {
				change -= diversityContribution;
			}
			else {
				double newDiversityContribution = calculateDiversityContribution(foodInstance, lastEaten);
				change += (newDiversityContribution - diversityContribution);
			}
		}

		if (SOLPineapplePieConfig.shouldCount(food)) {
			change += calculateDiversityContribution(new FoodInstance(food), 0);
		}

		return change;
	}

	@Override
	public double foodDiversity() {
		return foodDiversity(uniqueFoods.entrySet());
	}

	public static double foodDiversity(Set<Map.Entry<FoodInstance, Integer>> foodData) {
		double diversity = 0;

		for (Map.Entry<FoodInstance, Integer> entry : foodData) {
			FoodInstance food = entry.getKey();
			Integer lastEaten = entry.getValue();

			diversity += calculateDiversityContribution(food, lastEaten);
		}

		return diversity;
	}

	public static double calculateDiversityContribution(FoodInstance food, int lastEaten) {
		return calculateTimePenalty(lastEaten) * getComplexity(food);
	}

	private static double calculateTimePenalty(int lastEaten) {
		int size = SOLPineapplePieConfig.size();
		int startDecay = SOLPineapplePieConfig.startDecay();
		int endDecay = SOLPineapplePieConfig.endDecay();
		double minContribution = SOLPineapplePieConfig.minContribution();

		if (startDecay > endDecay || startDecay < 0 || endDecay > size ||
				startDecay > size || minContribution > 1 || minContribution < 0) {
			// invalid
			return 0.0;
		}

		if (lastEaten <= startDecay) {
			return 1.0;
		}
		else if (lastEaten > endDecay) {
			return minContribution;
		}

		double slope = (1.0 - minContribution) / (double) (startDecay - endDecay);
		return slope * (lastEaten - startDecay) + 1.0;
	}

	public static double getComplexity(FoodInstance food) {
		if (ConfigHandler.complexityMap != null && ConfigHandler.complexityMap.containsKey(food)) {
			return ConfigHandler.complexityMap.get(food);
		}

		var foodProperties = food.item.getFoodProperties();
		if (foodProperties != null)
		{
			var nutrition = foodProperties.getNutrition();
			var saturation = nutrition * foodProperties.getSaturationModifier();
			var average = ((saturation + nutrition) / 2);

			if (average < 5) {
				return average * SOLPineapplePieConfig.defaultContribution() / 5f;
			}

			return SOLPineapplePieConfig.defaultContribution() * 4 * Math.log10(average - 4) + 1;
		}

		return SOLPineapplePieConfig.defaultContribution();
	}

	public Set<Map.Entry<FoodInstance, Integer>> getData() {
		return uniqueFoods.entrySet();
	}

	public int getLastEaten(Item food) {
		if (!hasEaten(food)) {
			return -1;
		}

		return uniqueFoods.get(new FoodInstance(food));
	}

	@Override
	public boolean hasEaten(Item food) {
		if (!food.isEdible()) return false;
		return uniqueFoods.containsKey(new FoodInstance(food));
	}
	
	public void clearFood() {
		uniqueFoods.clear();
	}
	
	public Set<FoodInstance> getEatenFoods() {
		return uniqueFoods.keySet();
	}

	public int getFoodsEaten() {
		return foodsEaten;
	}

	public void resetFoodsEaten() {
		foodsEaten = 0;
	}
	
	public static class FoodListNotFoundException extends RuntimeException {
		public FoodListNotFoundException() {
			super("Player must have food capability attached, but none was found.");
		}
	}
}
