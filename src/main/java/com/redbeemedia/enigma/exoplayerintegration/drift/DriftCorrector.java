package com.redbeemedia.enigma.exoplayerintegration.drift;

import com.redbeemedia.enigma.core.time.Duration;

public class DriftCorrector implements IDriftListener {
    private final Duration driftTolerance;
    private final float speedAdjustment;

    public DriftCorrector() {
        this(1.3f);
    }

    public DriftCorrector(Duration driftTolerance) {
        this(driftTolerance, 1.3f);
    }

    public DriftCorrector(float speedAdjustment) {
        this(Duration.seconds(1).multiply(0.1f),speedAdjustment);
    }

    public DriftCorrector(Duration driftTolerance, float speedAdjustment) {
        this.driftTolerance = driftTolerance;
        this.speedAdjustment = speedAdjustment;
    }

    @Override
    public void onDriftUpdated(ISpeedHandler speedHandler, Duration drift) {
        float driftInSeconds = drift.inUnits(Duration.Unit.SECONDS);
        float driftToleranceInSeconds = driftTolerance.inUnits(Duration.Unit.SECONDS);

        float playbackSpeed;
        if(driftInSeconds > driftToleranceInSeconds) {
            playbackSpeed = speedAdjustment;
        } else if(driftInSeconds < -driftToleranceInSeconds) {
            playbackSpeed = 1f/speedAdjustment;
        } else {
            playbackSpeed = 1f;
        }
        speedHandler.setPlaybackSpeed(playbackSpeed);
    }
}
