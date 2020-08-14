package com.redbeemedia.enigma.exoplayerintegration;

import com.redbeemedia.enigma.core.context.EnigmaRiverContext;
import com.redbeemedia.enigma.core.context.IModuleContextInitialization;

public class ExoPlayerIntegrationContext {
    private static volatile InitializedContext initializedContext = null;

    private ExoPlayerIntegrationContext() {} // Disable instantiation

    /**
     * Called by core module through reflection.
     */
    private static synchronized void initialize(IModuleContextInitialization initialization) {
        if(initializedContext == null) {
            initializedContext = new InitializedContext(initialization);
        } else {
            throw new IllegalStateException("ExoPlayerIntegrationContext already initialized.");
        }
    }

    public static synchronized void assertInitialized() {
        if(initializedContext == null) {
            // If EnigmaRiverContext is not yet initialized,
            // getVersion() will throw an exception. This
            // indicates that the reason this module is not
            // yet initialized is that the parent module is
            // not initialized.
            String version = EnigmaRiverContext.getVersion();
            throw new IllegalStateException("ExoPlayerIntegrationContext was not initialized from core module. Make sure "+version+" is used for all Enigma River SDK modules.");
        }
    }


    private static class InitializedContext {
        public InitializedContext(IModuleContextInitialization initialization) {
        }
    }
}
