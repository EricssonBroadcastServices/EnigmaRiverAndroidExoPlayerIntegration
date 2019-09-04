package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.core.player.track.IPlayerImplementationTrack;
import com.redbeemedia.enigma.exoplayerintegration.error.ExoPlayerError;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoSubtitleTrack;

import java.util.ArrayList;
import java.util.List;

/*package-protected*/ class ExoPlayerListener implements Player.EventListener {
    private IPlayerImplementationListener listener;
    private int lastState = Player.STATE_IDLE;

    public ExoPlayerListener(IPlayerImplementationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        listener.onError(new ExoPlayerError(error));
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(playbackState == Player.STATE_READY) {
            if(lastState != Player.STATE_READY) {
                listener.onLoadCompleted();
            }
            if(playWhenReady) {
                listener.onPlaybackStarted();
            } else {
                listener.onPlaybackPaused();
            }
        } else if(playbackState == Player.STATE_ENDED) {
            listener.onStreamEnded();
        }
        this.lastState = playbackState;
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        //This is where we would pick out audio tracks in the future also
        List<IPlayerImplementationTrack> tracks = new ArrayList<>();

        for(int i = 0; i < trackGroups.length; ++i) {
            TrackGroup trackGroup = trackGroups.get(i);
            for(int j = 0; j < trackGroup.length; ++j) {
                Format format = trackGroup.getFormat(j);
                if(isTextMimeType(format.containerMimeType) || isTextMimeType(format.sampleMimeType)) {
                    tracks.add(new ExoSubtitleTrack(format));
                }
            }
        }

        listener.onTracksChanged(tracks);
    }

    private static boolean isTextMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("text/");
    }
}
