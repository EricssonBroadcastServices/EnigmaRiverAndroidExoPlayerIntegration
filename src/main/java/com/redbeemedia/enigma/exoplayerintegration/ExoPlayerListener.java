package com.redbeemedia.enigma.exoplayerintegration;

import android.media.MediaCodec;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.KeysExpiredException;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.redbeemedia.enigma.core.error.DrmKeysExpiredError;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.core.player.track.IPlayerImplementationTrack;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.exoplayerintegration.error.ExoPlayerError;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoAudioTrack;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoSubtitleTrack;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoVideoTrack;

import java.util.ArrayList;
import java.util.List;

/*package-protected*/ class ExoPlayerListener implements Player.EventListener {
    private IPlayerImplementationListener listener;
    private int lastState = Player.STATE_IDLE;
    private boolean signalLoadedOnReady = true;

    public ExoPlayerListener(IPlayerImplementationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if(isKeysExpiredException(error)) {
            listener.onError(new DrmKeysExpiredError(error));
        } else {
            listener.onError(new ExoPlayerError(error));
        }
    }

    private boolean isKeysExpiredException(ExoPlaybackException error) {
        if(error.type == ExoPlaybackException.TYPE_RENDERER) {
            //Check if KeysExpiredException
            Throwable exception = error.getRendererException();
            while(exception != null) {
                if(exception instanceof KeysExpiredException) {
                    return true;
                }
                exception = exception.getCause();
            }
            //Check if MediaCodec.CryptoException
            exception = error.getRendererException();
            while(exception != null) {
                if(exception instanceof MediaCodec.CryptoException) {
                    return true;
                }
                exception = exception.getCause();
            }
        }
        return false;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(playbackState == Player.STATE_READY) {
            if(signalLoadedOnReady) {
                listener.onLoadCompleted();
                signalLoadedOnReady = false;
            }
            if(playWhenReady) {
                listener.onPlaybackStarted();
            } else {
                listener.onPlaybackPaused();
            }
        } else if(playbackState == Player.STATE_ENDED) {
            listener.onStreamEnded();
            signalLoadedOnReady = true;
        } else if(playbackState == Player.STATE_BUFFERING) {
            listener.onPlaybackBuffering();
        } else if(playbackState == Player.STATE_IDLE) {
            signalLoadedOnReady = true;
        }
        this.lastState = playbackState;
    }

    public void onLoadingNewMediaSource() {
        AndroidThreadUtil.runOnUiThread(() -> signalLoadedOnReady = true);
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
                if(isVideoType(format.containerMimeType) || isVideoType(format.sampleMimeType)) {
                    ExoVideoTrack videoTrack = new ExoVideoTrack(format);
                    if(!tracks.contains(videoTrack)) {
                        tracks.add(videoTrack);
                    }
                }
            }
        }

        listener.onTracksChanged(tracks);
    }

    private static boolean isTextMimeType(String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_TEXT;
    }

    private static boolean isAudioType(String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_AUDIO;
    }

    private static boolean isVideoType(String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_VIDEO;
    }

}
