package com.redbeemedia.enigma.exoplayerintegration.tracks;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;
import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;

public class ExoSubtitleTrack extends BasePlayerImplementationTrack implements ISubtitleTrack {
    private static final int TRACK_TEXT = 2;
    private final Format format;

    public ExoSubtitleTrack(Format format) {
        this.format = format;
    }


    @Override
    public String getLanguageCode() {
        return format.language;
    }

    public void applyTo(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setPreferredTextLanguage(format.language);
        parametersBuilder.setRendererDisabled(TRACK_TEXT, false);
        trackSelector.setParameters(parametersBuilder.build());
    }

    public static void applyNone(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setRendererDisabled(TRACK_TEXT, true);
        trackSelector.setParameters(parametersBuilder.build());
    }
}
