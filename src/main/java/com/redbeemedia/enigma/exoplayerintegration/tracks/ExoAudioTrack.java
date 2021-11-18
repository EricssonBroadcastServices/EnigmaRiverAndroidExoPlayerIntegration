package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.audio.IAudioTrack;
import com.redbeemedia.enigma.exoplayerintegration.ExoUtil;

public final class ExoAudioTrack extends AbstractExoTrack implements IAudioTrack {
    public ExoAudioTrack(String label, String code) {
        super(label, code);
    }


    @Override
    public String getLabel() {
        return super.getLabel();
    }

    @Override
    public String getCode() {
        return super.getCode();
    }

    @Override
    public void applyTo(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setPreferredAudioLanguage(getLabel());
        parametersBuilder.setRendererDisabled(ExoUtil.DEFAULT_AUDIO_RENDERER_INDEX, false);
        trackSelector.setParameters(parametersBuilder.build());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ExoAudioTrack && super.equals(obj);
    }
}
