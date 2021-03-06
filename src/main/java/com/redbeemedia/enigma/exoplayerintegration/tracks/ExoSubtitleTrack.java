package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;
import com.redbeemedia.enigma.exoplayerintegration.ExoUtil;

public final class ExoSubtitleTrack extends AbstractExoTrack implements ISubtitleTrack {
    public ExoSubtitleTrack(String language) {
        super(language);
    }


    @Override
    public String getLanguageCode() {
        return super.getLanguageCode();
    }

    @Override
    public void applyTo(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setPreferredTextLanguage(getLanguageCode());
        parametersBuilder.setRendererDisabled(ExoUtil.DEFAULT_TEXT_RENDERER_INDEX, false);
        trackSelector.setParameters(parametersBuilder.build());
    }

    public static void applyNone(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setRendererDisabled(ExoUtil.DEFAULT_TEXT_RENDERER_INDEX, true);
        trackSelector.setParameters(parametersBuilder.build());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ExoSubtitleTrack && super.equals(obj);
    }
}
