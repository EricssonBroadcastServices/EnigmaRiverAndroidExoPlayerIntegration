package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;

import java.util.Objects;

/*package-protected*/ abstract class AbstractExoTrack extends BasePlayerImplementationTrack {
    private final String label;
    private final String code;

    public AbstractExoTrack(String label, String code) {
        this.label = label;
        this.code = code;
    }

    protected String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }

    public abstract void applyTo(DefaultTrackSelector trackSelector);

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof AbstractExoTrack && Objects.equals(this.label, ((AbstractExoTrack) obj).label);
    }

    @Override
    public int hashCode() {
        return label != null ? label.hashCode() : 0;
    }
}
