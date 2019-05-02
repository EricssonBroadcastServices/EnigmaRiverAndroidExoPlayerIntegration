package com.redbeemedia.enigma.exoplayerintegration;

import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;

public class ExoRejectReason implements IControlResultHandler.IRejectReason {
    private final IControlResultHandler.RejectReasonType type;
    private final String details;

    public ExoRejectReason(IControlResultHandler.RejectReasonType type, String details) {
        this.type = type;
        this.details = details;
    }

    @Override
    public IControlResultHandler.RejectReasonType getType() {
        return type;
    }

    @Override
    public String getDetails() {
        return details;
    }
}
