package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.audio.IAudioTrack;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;
import com.redbeemedia.enigma.exoplayerintegration.ExoUtil;

import java.util.Objects;

public class ExoAudioTrack extends BasePlayerImplementationTrack implements IAudioTrack {
    private final Format format;

    public ExoAudioTrack(Format format) {
        this.format = format;
    }


    @Override
    public String getLanguageCode() {
        return format.language;
    }

    public void applyTo(DefaultTrackSelector trackSelector) {
        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
        parametersBuilder.setPreferredAudioLanguage(format.language);
        parametersBuilder.setRendererDisabled(ExoUtil.DEFAULT_AUDIO_RENDERER_INDEX, false);
        trackSelector.setParameters(parametersBuilder.build());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ExoAudioTrack && Objects.equals(this.format, ((ExoAudioTrack) obj).format);
    }

    @Override
    public int hashCode() {
        return format != null ? format.hashCode() : 0;
    }
}
