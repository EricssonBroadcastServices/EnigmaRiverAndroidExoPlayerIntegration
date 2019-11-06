package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.core.player.track.IPlayerImplementationTrack;
import com.redbeemedia.enigma.exoplayerintegration.error.ExoPlayerError;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoAudioTrack;
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
        List<IPlayerImplementationTrack> tracks = new ArrayList<>();

        for(int i = 0; i < trackGroups.length; ++i) {
            TrackGroup trackGroup = trackGroups.get(i);
            for(int j = 0; j < trackGroup.length; ++j) {
                Format format = trackGroup.getFormat(j);
                if(isTextMimeType(format.containerMimeType) || isTextMimeType(format.sampleMimeType)) {
                    ExoSubtitleTrack subtitleTrack = new ExoSubtitleTrack(format.language);
                    if(!tracks.contains(subtitleTrack)) {
                        tracks.add(subtitleTrack);
                    }
                }
                if(isAudioType(format.containerMimeType)) {
                    ExoAudioTrack audioTrack = new ExoAudioTrack(format.language);
                    if(!tracks.contains(audioTrack)) {
                        tracks.add(audioTrack);
                    }
                }
            }
        }

        listener.onTracksChanged(tracks);
        listener.onSubtitleTrackSelectionChanged(getSelected(trackSelections.get(ExoUtil.DEFAULT_TEXT_RENDERER_INDEX), format -> new ExoSubtitleTrack(format.language)));
        listener.onAudioTrackSelectionChanged(getSelected(trackSelections.get(ExoUtil.DEFAULT_AUDIO_RENDERER_INDEX), format -> new ExoAudioTrack(format.language)));
    }

    private static boolean isTextMimeType(String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_TEXT;
    }

    private static boolean isAudioType(String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_AUDIO;
    }

    private static <T> T getSelected(TrackSelection trackSelection, IFormatWrapper<T> wrapper) {
        if(trackSelection != null) {
            Format selectedFormat = trackSelection.getSelectedFormat();
            if (selectedFormat != null) {
                return wrapper.wrap(selectedFormat);
            }
        }
        return null;
    }

    private interface IFormatWrapper<T> {
        T wrap(Format format);
    }
}
