package com.redbeemedia.enigma.exoplayerintegration;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControlResultHandler;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;

/*package-protected*/ abstract class ControlRequestResolutionException extends Exception {
    private ControlRequestResolutionException() {} //Limit instantiation to file


    public abstract void apply(IPlayerImplementationControlResultHandler resultHandler);

    public static ControlRequestResolutionException onRejected(IControlResultHandler.IRejectReason rejectReason) {
        return new ControlRequestResolutionException() {
            @Override
            public void apply(IPlayerImplementationControlResultHandler resultHandler) {
                resultHandler.onRejected(rejectReason);
            }
        };
    }

    public static ControlRequestResolutionException onCancelled() {
        return new ControlRequestResolutionException() {
            @Override
            public void apply(IPlayerImplementationControlResultHandler resultHandler) {
                resultHandler.onCancelled();
            }
        };
    }

    public static ControlRequestResolutionException onError(EnigmaError error) {
        return new ControlRequestResolutionException() {
            @Override
            public void apply(IPlayerImplementationControlResultHandler resultHandler) {
                resultHandler.onError(error);
            }
        };
    }
}
