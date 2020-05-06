package com.redbeemedia.enigma.exoplayerintegration;

import com.redbeemedia.enigma.core.context.EnigmaRiverContext;

public class ExoPlayerIntegrationContext {
    private static boolean initialized = false;

    private ExoPlayerIntegrationContext() {} // Disable instantiation

    /**
     * Called by core module through reflection.
     */
    private static synchronized void initialize() {
        initialized = true;
    }

    public static synchronized void assertInitialized() {
        if(!initialized) {
            // If EnigmaRiverContext is not yet initialized,
            // getVersion() will throw an exception. This
            // indicates that the reason this module is not
            // yet initialized is that the parent module is
            // not initialized.
            String version = EnigmaRiverContext.getVersion();
            throw new IllegalStateException("ExoPlayerIntegrationContext was not initialized from core module. Make sure "+version+" is used for all Enigma River SDK modules.");
        }
    }
}
