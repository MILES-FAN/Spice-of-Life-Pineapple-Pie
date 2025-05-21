package com.milesfan.solpineapplepie.client.gui;

import com.milesfan.solpineapplepie.SOLPineapplePieConfig;
import com.milesfan.solpineapplepie.client.gui.elements.UIBox;
import com.milesfan.solpineapplepie.client.gui.elements.UIElement;
import com.milesfan.solpineapplepie.client.gui.elements.UILabel;

import java.awt.*;

import static com.milesfan.solpineapplepie.lib.Localization.localized;

public class DiversityPage extends Page {
    DiversityPage(double foodDiversity, int foodEaten, Rectangle frame) {
        super(frame, localized("gui", "food_book.stats"));

        // Dummy box to center the diversity display
        mainStack.addChild(new UIBox(new Rectangle(0, 0, 1, 35), new Color(0, 0, 0, 0)));

        UIElement diversityDisplay = statWithIcon(
                icon(FoodBookScreen.carrotImage),
                String.format("%.2f", foodDiversity),
                localized("gui", "food_book.stats.current_diversity")
        );
        mainStack.addChild(diversityDisplay);

        if (foodEaten < SOLPineapplePieConfig.minFoodsToActivate()) {
            int diff = SOLPineapplePieConfig.minFoodsToActivate() - foodEaten;
            mainStack.addChild(new UIBox(new Rectangle(0, 0, 1, 10), new Color(0, 0, 0, 0)));
            UILabel minFoodLabel1 = new UILabel(localized("gui", "food_book.stats.min_warning1", diff));
            minFoodLabel1.color = FoodBookScreen.inactiveRed;
            UILabel minFoodLabel2 = new UILabel(localized("gui", "food_book.stats.min_warning2", diff));
            minFoodLabel2.color = FoodBookScreen.inactiveRed;
            mainStack.addChild(minFoodLabel1);
            mainStack.addChild(minFoodLabel2);
        }

        updateMainStack();
    }
}
