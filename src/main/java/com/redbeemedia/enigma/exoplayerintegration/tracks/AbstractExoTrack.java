package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;

import java.util.Objects;

/*package-protected*/ abstract class AbstractExoTrack extends BasePlayerImplementationTrack {
    private final String label;
    private final String code;
    protected final String trackId;

    public AbstractExoTrack(String label, String code) {
        this.label = label;
        this.code = code;
        this.trackId = null;
    }

    public AbstractExoTrack(String label, String code, String id) {
        this.label = label;
        this.code = code;
        this.trackId = id;
    }

    public AbstractExoTrack(String id){
        this.code = null;
        this.label = null;
        this.trackId = id;
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

    public String getTrackId() {
        return trackId;
    }
}
