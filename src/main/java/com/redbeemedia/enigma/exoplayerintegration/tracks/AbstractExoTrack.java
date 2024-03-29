package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;

import java.util.Objects;

/*package-protected*/ abstract class AbstractExoTrack extends BasePlayerImplementationTrack {
    private final String label;
    private final String code;
    protected final String trackId;
    private final @C.RoleFlags int roleFlag;
    private final @C.SelectionFlags int selectionFlag;

    public AbstractExoTrack(String label, String code) {
        this.label = label;
        this.code = code;
        this.trackId = null;
        this.roleFlag = C.ROLE_FLAG_MAIN;
        this.selectionFlag = C.SELECTION_FLAG_DEFAULT;
    }

    public AbstractExoTrack(String label, String code, String id, @C.RoleFlags int role) {
        this.label = label;
        this.code = code;
        this.trackId = id;
        this.roleFlag = role;
        this.selectionFlag = C.SELECTION_FLAG_DEFAULT;
    }

    public AbstractExoTrack(String label, String code, String id, @C.RoleFlags int role, @C.SelectionFlags int selectionFlag) {
        this.label = label;
        this.code = code;
        this.trackId = id;
        this.roleFlag = role;
        this.selectionFlag = selectionFlag;
    }

    public AbstractExoTrack(String id){
        this.code = null;
        this.label = null;
        this.trackId = id;
        this.roleFlag = C.ROLE_FLAG_MAIN;
        this.selectionFlag = C.SELECTION_FLAG_DEFAULT;
    }

    protected String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }

    public @C.RoleFlags int getRoleFlag()
    {
        return roleFlag;
    }

    public boolean isForcedTrack() {
        return selectionFlag == C.SELECTION_FLAG_FORCED;
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
