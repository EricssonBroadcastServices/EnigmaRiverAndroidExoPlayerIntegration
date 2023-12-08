package com.redbeemedia.enigma.exoplayerintegration;

import android.media.MediaCodec;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoTimeoutException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.KeysExpiredException;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.NonNullApi;
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
import java.util.Objects;

@NonNullApi
/*package-protected*/ class ExoPlayerListener implements Player.Listener {
    private final IPlayerImplementationListener listener;
    private boolean signalLoadedOnReady = true;
    private final ExoPlayerTech exoPlayerTech;

    public ExoPlayerListener(IPlayerImplementationListener listener, ExoPlayerTech exoPlayerTech) {
        this.listener = listener;
        this.exoPlayerTech = exoPlayerTech;

    }

    @Override
    public void onPlayerError(PlaybackException error) {
        if(isKeysExpiredException(error)) {
            listener.onError(new DrmKeysExpiredError(error));
        } else if (isExoTimeoutException(error)) {
            // playback stopped and will be reported as Aborted in analytics
            // because this exception comes when timeout happen in screen release
            listener.onPlaybackStopped();
        } else {
            listener.onError(new ExoPlayerError(error));
        }
    }

    private boolean isKeysExpiredException(PlaybackException error) {
        if (error instanceof  ExoPlaybackException) {
            ExoPlaybackException exoplaybackExcepton  = (ExoPlaybackException) error;
            if (exoplaybackExcepton.type == ExoPlaybackException.TYPE_RENDERER) {
                //Check if KeysExpiredException
                Throwable exception = exoplaybackExcepton.getRendererException();
                while (exception != null) {
                    if (exception instanceof KeysExpiredException) {
                        return true;
                    }
                    exception = exception.getCause();
                }
                //Check if MediaCodec.CryptoException
                exception = exoplaybackExcepton.getRendererException();
                while (exception != null) {
                    if (exception instanceof MediaCodec.CryptoException) {
                        return true;
                    }
                    exception = exception.getCause();
                }
            }
        }
        return false;
    }

    private boolean isExoTimeoutException(PlaybackException error) {
        if (error instanceof ExoPlaybackException) {
            ExoPlaybackException exoplaybackExcepton = (ExoPlaybackException) error;
            Throwable cause = Objects.requireNonNull(exoplaybackExcepton.getCause());
            // This exception occur when releasing or screen-surface release timeout
            // https://developer.android.com/reference/androidx/media3/exoplayer/ExoTimeoutException
            if (cause instanceof ExoTimeoutException) {
                ExoTimeoutException exoTimeoutException = (ExoTimeoutException) cause;
                if (exoTimeoutException.timeoutOperation == ExoTimeoutException.TIMEOUT_OPERATION_RELEASE
                        || exoTimeoutException.timeoutOperation == ExoTimeoutException.TIMEOUT_OPERATION_DETACH_SURFACE) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
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
    }

    public void onLoadingNewMediaSource() {
        AndroidThreadUtil.runOnUiThread(() -> signalLoadedOnReady = true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        List<IPlayerImplementationTrack> tracks = new ArrayList<>();
        for(int i = 0; i < trackGroups.length; ++i) {
            TrackGroup trackGroup = trackGroups.get(i);
            for(int j = 0; j < trackGroup.length; ++j) {
                Format format = trackGroup.getFormat(j);
                String label = format.label;
                if (label == null) {
                    label = format.language;
                }
                if(isTextMimeType(format.containerMimeType) || isTextMimeType(format.sampleMimeType)) {
                    ExoSubtitleTrack subtitleTrack = new ExoSubtitleTrack(label, format.language, format.id, format.roleFlags);
                    if(!tracks.contains(subtitleTrack)) {
                        tracks.add(subtitleTrack);
                    }
                }
                if(isAudioType(format.containerMimeType)) {
                    if (!exoPlayerTech.isAudioTrackSupported(format)) {
                        continue;
                    }
                    ExoAudioTrack audioTrack = new ExoAudioTrack(trackGroup, label, format.language, format.id, format.roleFlags);
                    if(!tracks.contains(audioTrack)) {
                        tracks.add(audioTrack);
                    }
                }
                if(isVideoType(format.containerMimeType) || isVideoType(format.sampleMimeType)) {
                    ExoVideoTrack videoTrack = new ExoVideoTrack(format, format.id);
                    if(!tracks.contains(videoTrack)) {
                        tracks.add(videoTrack);
                    }
                }
            }
        }

        // push event for video track
        for (TrackSelection trackSelection : trackSelections.getAll()) {
            if (trackSelection instanceof FixedTrackSelection) {
                int selectedIndex = ((FixedTrackSelection) trackSelection).getSelectedIndex();
                Format format = trackSelection.getFormat(selectedIndex);
                if (isVideoType(format.containerMimeType) || isVideoType(format.sampleMimeType)) {
                    listener.onVideoTrackSelectionChanged(new ExoVideoTrack(format, format.id));
                    break;
                }
            } else if (trackSelection instanceof AdaptiveTrackSelection) {
                int selectedIndex = ((AdaptiveTrackSelection) trackSelection).getSelectedIndex();
                Format format = trackSelection.getFormat(selectedIndex);
                if (isVideoType(format.containerMimeType) || isVideoType(format.sampleMimeType)) {
                    listener.onVideoTrackSelectionChanged(new ExoVideoTrack(format, format.id));
                }
            }
        }
        listener.onTracksChanged(tracks);
    }

    // TODO: replace deprecated onTracksChanged by onTracksInfoChanged, once we figure out how to retrieve the current ABR selected track...
    /*
    @Override
    public void onTracksInfoChanged(@NotNull TracksInfo tracksInfo)
    {
        List<IPlayerImplementationTrack> tracks = new ArrayList<>();
        for (TracksInfo.TrackGroupInfo trackGroupInfo : tracksInfo.getTrackGroupInfos())
        {
            TrackGroup trackGroup = trackGroupInfo.getTrackGroup();
            for (int ii = 0; ii < trackGroup.length; ii++)
            {
                final Format format = trackGroup.getFormat(ii);
                String label = format.label;
                if (label == null) {
                    label = format.language;
                }
                if(isTextMimeType(format.containerMimeType) || isTextMimeType(format.sampleMimeType)) {
                    ExoSubtitleTrack subtitleTrack = new ExoSubtitleTrack(label, format.language, format.id, format.roleFlags);
                    if(!tracks.contains(subtitleTrack)) {
                        tracks.add(subtitleTrack);
                    }
                }
                if(isAudioType(format.containerMimeType)) {
                    ExoAudioTrack audioTrack = new ExoAudioTrack(label, format.language, format.id, format.roleFlags);
                    if(!tracks.contains(audioTrack)) {
                        tracks.add(audioTrack);
                    }
                }
                if(isVideoType(format.containerMimeType) || isVideoType(format.sampleMimeType)) {
                    ExoVideoTrack videoTrack = new ExoVideoTrack(format, format.id);
                    if(!tracks.contains(videoTrack)) {
                        tracks.add(videoTrack);
                    }
                }
            }
        }

        //listener.onTracksChanged(tracks);
    }*/

    private static boolean isTextMimeType(@Nullable String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_TEXT;
    }

    private static boolean isAudioType(@Nullable String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_AUDIO;
    }

    private static boolean isVideoType(@Nullable String mimeType) {
        return MimeTypes.getTrackType(mimeType) == C.TRACK_TYPE_VIDEO;
    }

}
