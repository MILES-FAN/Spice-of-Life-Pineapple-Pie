package com.milesfan.solpineapplepie.integration;

import com.milesfan.solpineapplepie.SOLPineapplePie;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central integration manager for all mod integrations
 * This class handles loading integrations only when appropriate mods are present
 */
public class IntegrationManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // List of supported mod IDs
    private static final String CREATE_MOD_ID = "create";
    
    /**
     * Initialize all integrations
     * Call this during mod initialization
     */
    public static void init() {
        LOGGER.info("Initializing mod integrations for " + SOLPineapplePie.MOD_ID);
        
        // Check for Create mod
        if (ModList.get().isLoaded(CREATE_MOD_ID)) {
            LOGGER.info("Found Create mod, enabling integration");
            // Create integration is handled via event subscribers
        }
        
        // Add other mod integrations here as needed
    }
    
    /**
     * Check if a specific mod is loaded
     * @param modId The mod ID to check
     * @return True if the mod is loaded
     */
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}