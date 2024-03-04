package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.redbeemedia.enigma.core.audio.IAudioTrack;
import com.redbeemedia.enigma.exoplayerintegration.ExoUtil;

public final class ExoAudioTrack extends AbstractExoTrack implements IAudioTrack {
    private final TrackGroup trackGroup;

    public ExoAudioTrack(TrackGroup trackGroup, String label, String code, String id, int role)
    {
        super(label, code, id, role);
        this.trackGroup = trackGroup;
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
        DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
        // Selection should rather be done on language + role
        // using overrides as a workaround because some customers did not set the roles properly...
        parametersBuilder.setOverrideForType(new TrackSelectionOverride(trackGroup,0));
        /*
        parametersBuilder.setPreferredAudioLanguage(getCode());
        parametersBuilder.setPreferredAudioRoleFlags(getRoleFlag());
         */
        parametersBuilder.setRendererDisabled(ExoUtil.DEFAULT_AUDIO_RENDERER_INDEX, false);
        trackSelector.setParameters(parametersBuilder.build());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ExoAudioTrack && super.equals(obj);
    }
}
