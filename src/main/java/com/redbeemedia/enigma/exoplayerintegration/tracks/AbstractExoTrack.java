package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;

import java.util.Objects;

/*package-protected*/ abstract class AbstractExoTrack extends BasePlayerImplementationTrack {
    private final String language;

    public AbstractExoTrack(String language) {
        this.language = language;
    }

    protected String getLanguageCode() {
        return language;
    }

    public abstract void applyTo(DefaultTrackSelector trackSelector);

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof AbstractExoTrack && Objects.equals(this.language, ((AbstractExoTrack) obj).language);
    }

    @Override
    public int hashCode() {
        return language != null ? language.hashCode() : 0;
    }
}
