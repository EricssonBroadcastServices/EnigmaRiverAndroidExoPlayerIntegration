package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;
import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;
import com.redbeemedia.enigma.exoplayerintegration.ExoUtil;

import java.util.Objects;

public class ExoSubtitleTrack extends BasePlayerImplementationTrack implements ISubtitleTrack {
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
        return obj instanceof ExoSubtitleTrack && Objects.equals(this.format, ((ExoSubtitleTrack) obj).format);
    }

    @Override
    public int hashCode() {
        return format != null ? format.hashCode() : 0;
    }
}
