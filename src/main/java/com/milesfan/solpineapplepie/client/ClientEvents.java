package com.milesfan.solpineapplepie.client;

import com.milesfan.solpineapplepie.SOLPineapplePie;
import com.milesfan.solpineapplepie.client.gui.FoodBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.milesfan.solpineapplepie.client.SOLClientRegistry.OPEN_FOOD_BOOK;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLPineapplePie.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    @SubscribeEvent
    public static void handleKeypress(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        if (OPEN_FOOD_BOOK != null && OPEN_FOOD_BOOK.isDown()) {
            FoodBookScreen.open(player);
        }
    }
}
