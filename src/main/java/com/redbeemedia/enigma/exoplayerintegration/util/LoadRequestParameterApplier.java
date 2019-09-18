package com.redbeemedia.enigma.exoplayerintegration.util;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControls;

public abstract class LoadRequestParameterApplier {
    private final IPlayerImplementationControls.ILoadRequest loadRequest;

    public LoadRequestParameterApplier(IPlayerImplementationControls.ILoadRequest loadRequest) {
        if(loadRequest == null) {
            throw new NullPointerException("loadRequest was null");
        }
        this.loadRequest = loadRequest;
    }

    public void applyTo(DefaultTrackSelector trackSelector) {
        try {
            DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
            applyMaxBitrate(parametersBuilder);
            applyMaxResolutionHeight(parametersBuilder);
            trackSelector.setParameters(parametersBuilder.build());
        } catch (Exception e) {
            onException(e);
        }
    }

    private void applyMaxBitrate(DefaultTrackSelector.ParametersBuilder parametersBuilder) {
        try {
            Integer maxBitrate = loadRequest.getMaxBitrate();
            parametersBuilder.setMaxVideoBitrate(maxBitrate != null ? maxBitrate : Integer.MAX_VALUE);
        } catch (Exception e) {
            onException(e);
        }
    }

    private void applyMaxResolutionHeight(DefaultTrackSelector.ParametersBuilder parametersBuilder) {
        try {
            Integer maxResoultionHeight = loadRequest.getMaxResoultionHeight();
            parametersBuilder.setMaxVideoSize(Integer.MAX_VALUE, maxResoultionHeight != null ? maxResoultionHeight : Integer.MAX_VALUE);
        } catch (Exception e) {
            onException(e);
        }
    }


    protected abstract void onException(Exception e);
}
