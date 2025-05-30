package com.milesfan.solpineapplepie.utils;

import com.milesfan.solpineapplepie.SOLPineapplePie;
import com.milesfan.solpineapplepie.tracking.FoodInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ResourceLocationException;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.sql.SQLData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexityParser {
    public static Map<FoodInstance, Double> parse(List<String> unparsed) {
        Map<FoodInstance, Double> complexityMap = new HashMap<>();

        for (String complexityString : unparsed) {
            String[] s = complexityString.split(",", 0);
            if (s.length != 2) {
                SOLPineapplePie.LOGGER.warn("Invalid complexity specification: " + complexityString);
                continue;
            }

            String foodString = s[0];
            double complexity = 1.0;
            try {
                complexity = Double.parseDouble(s[1]);
            }
            catch (NumberFormatException e) {
                SOLPineapplePie.LOGGER.warn("Second argument in complexity specification needs to be a number: " + complexityString);
                continue;
            }

            Item item;
            try {
                item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(foodString));
            }
            catch (ResourceLocationException e) {
                SOLPineapplePie.LOGGER.warn("Invalid item name: " + foodString);
                continue;
            }
            if (item == null) {
                SOLPineapplePie.LOGGER.warn("Invalid item name: " + foodString);
                continue;
            }

            String modIdForItem = foodString.split(":")[0];
//            String isLoaded = ModList.get().isLoaded(modIdForItem) ? "YES" : "NO";
//            String infoMsg = "MOD '" + modIdForItem + "' for " + foodString + " is loaded : " + isLoaded;
//            SOLPineapplePie.LOGGER.info(infoMsg);
            if (!ModList.get().isLoaded(modIdForItem)) {
                String msg = "MOD '" + modIdForItem + "' is not load : " + foodString;
                SOLPineapplePie.LOGGER.info(msg);
                continue;
            }

            if (!item.isEdible()) {
                SOLPineapplePie.LOGGER.warn("Item is not food: " + foodString);
                continue;
            }

            FoodInstance food = new FoodInstance(item);

            if (food.encode() == null) {
                SOLPineapplePie.LOGGER.warn("Item does not exist: " + foodString);
                continue;
            }

            complexityMap.put(food, complexity);
        }
        return complexityMap;
    }
}
