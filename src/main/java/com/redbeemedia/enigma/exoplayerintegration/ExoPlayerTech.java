package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.IllegalSeekPositionException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.error.IllegalSeekPositionError;
import com.redbeemedia.enigma.core.error.UnexpectedError;
import com.redbeemedia.enigma.core.format.EnigmaMediaFormat;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.IEnigmaPlayerEnvironment;
import com.redbeemedia.enigma.core.player.IPlaybackTechnologyIdentifier;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControlResultHandler;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControls;
import com.redbeemedia.enigma.core.player.IPlayerImplementationInternals;
import com.redbeemedia.enigma.core.player.ITimelinePositionFactory;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimeline;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.player.timeline.TimelinePositionFormat;
import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoSubtitleTrack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ExoPlayerTech implements IPlayerImplementation {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DataSource.Factory mediaDataSourceFactory;
    private final ReusableExoMediaDrm<FrameworkMediaCrypto> mediaDrm;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private PlayerView playerView = null;
    private boolean hideControllerCalled = false;
    private MediaDrmFromProviderCallback mediaDrmCallback;
    private MediaFormatSpecification supportedFormats = new MediaFormatSpecification();
    private TimelinePositionFormat timestampFormat = TimelinePositionFormat.newFormat(new ExoPlayerDurationFormat(), "HH:mm");

    private final IActivation customTimestampViewsAdded = new Activation();
    private TextView positionView;
    private TextView durationView;

    public ExoPlayerTech(Context context, String appName) {
        this.mediaDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, appName));
        this.mediaDrmCallback = new MediaDrmFromProviderCallback(context,appName);

        for(EnigmaMediaFormat format : initSupportedFormats(new HashSet<>())) {
            supportedFormats.add(format);
        }

        this.trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());

        DefaultRenderersFactory rendersFactory =
            new DefaultRenderersFactory(context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        try {
            final UUID widewineUUID = Util.getDrmUuid("widevine");
            this.mediaDrm = new ReusableExoMediaDrm<FrameworkMediaCrypto>(new ReusableExoMediaDrm.ExoMediaDrmFactory<FrameworkMediaCrypto>() {
                @Override
                public ExoMediaDrm<FrameworkMediaCrypto> create() throws UnsupportedDrmException {
                    return FrameworkMediaDrm.newInstance(widewineUUID);
                }
            });
            //TODO check if mediaDrm needs to be released or if it is released when the player is released.
            DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = new DefaultDrmSessionManager<>(widewineUUID, mediaDrm, mediaDrmCallback, null, false);
            this.player = ExoPlayerFactory.newSimpleInstance(context, rendersFactory, trackSelector, drmSessionManager);
        } catch (UnsupportedDrmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void install(IEnigmaPlayerEnvironment environment) {
        mediaDrmCallback.setDrmProvider(environment.getDrmProvider());
        environment.setMediaFormatSupportSpec(supportedFormats);
        ITimelinePositionFactory timelinePositionFactory = environment.getTimelinePositionFactory();
        player.addListener(new ExoPlayerListener(environment.getPlayerImplementationListener()));
        player.addListener(new ExoPlayerTimelineListener(player, environment.getPlayerImplementationListener(), timelinePositionFactory));
        environment.setControls(new Controls());
        environment.setInternals(new Internals(timelinePositionFactory));
        environment.addEnigmaPlayerReadyListener(enigmaPlayer -> ExoPlayerTech.this.onReady(enigmaPlayer));
    }

    public void setTimestampFormat(TimelinePositionFormat timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    private class Controls implements IPlayerImplementationControls {
        @Override
        public void load(String url, IPlayerImplementationControlResultHandler resultHandler) {
            final MediaSource mediaSource;
            try {
                mediaSource = buildMediaSource(Uri.parse(url));
            } catch (RuntimeException e) {
                resultHandler.onError(new UnexpectedError(e));
                return;
            }
            AndroidThreadUtil.runOnUiThread(() -> {
                mediaDrm.revive();
                player.prepare(mediaSource, false, false);
                resultHandler.onDone();
            });
        }

        @Override
        public void start(IPlayerImplementationControlResultHandler resultHandler) {
            AndroidThreadUtil.runOnUiThread(() -> {
                player.setPlayWhenReady(true);
                resultHandler.onDone();
            });;
        }

        @Override
        public void pause(IPlayerImplementationControlResultHandler resultHandler) {
            AndroidThreadUtil.runOnUiThread(() -> {
                player.setPlayWhenReady(false);
                resultHandler.onDone();
            });
        }

        @Override
        public void stop(IPlayerImplementationControlResultHandler resultHandler) {
            AndroidThreadUtil.runOnUiThread(() -> {
                player.stop(true);
                resultHandler.onDone();
            });
        }

        @Override
        public void seekTo(ISeekPosition seekPosition, IPlayerImplementationControlResultHandler resultHandler) {
            if (seekPosition == ISeekPosition.TIMELINE_START) {
                AndroidThreadUtil.runOnUiThread(() -> {
                    try {
                        player.seekTo(0, 0);
                        resultHandler.onDone();
                    } catch (IllegalSeekPositionException e) {
                        resultHandler.onError(new IllegalSeekPositionError(e));
                    }
                });
            } else if (seekPosition == ISeekPosition.LIVE_EDGE) {
                if(!player.isCurrentWindowDynamic()) {
                    resultHandler.onRejected(new ExoRejectReason(IControlResultHandler.RejectReasonType.INAPPLICABLE_FOR_CURRENT_STREAM, "Video is not dynamic"));
                } else {
                    AndroidThreadUtil.runOnUiThread(() -> {
                        try {
                            player.seekToDefaultPosition();
                            resultHandler.onDone();
                        } catch (IllegalSeekPositionException e) {
                            resultHandler.onError(new IllegalSeekPositionError(e));
                        }
                    });
                }
            } else if(seekPosition instanceof IPlayerImplementationControls.TimelineRelativePosition) {
                long millis = ((TimelineRelativePosition) seekPosition).getMillis();
                String uuid = UUID.randomUUID().toString();
                AndroidThreadUtil.runOnUiThread(() -> {
                    player.seekTo(millis);
                    player.addListener(new Player.EventListener() {
                        @Override
                        public void onSeekProcessed() {
                            player.removeListener(this);
                            resultHandler.onDone();
                        }
                    });
                });
            } else if (seekPosition instanceof ExoPlayerPosition) {
                try {
                    ((ExoPlayerPosition) seekPosition).seek(player);
                    resultHandler.onDone();
                } catch (IllegalSeekPositionException e) {
                    resultHandler.onError(new UnexpectedError(e));
                }
            } else {
                resultHandler.onRejected(new ExoRejectReason(IControlResultHandler.RejectReasonType.ILLEGAL_ARGUMENT, "Can't handle seekPosition of supplied type"));
            }
        }

        @Override
        public void setVolume(float volume, IPlayerImplementationControlResultHandler resultHandler) {
            AndroidThreadUtil.runOnUiThread(() -> {
                try {
                    player.setVolume(volume);
                } catch (RuntimeException e) {
                    resultHandler.onError(new UnexpectedError(e));
                    return;
                }
                resultHandler.onDone();
            });
        }

        @Override
        public void setSubtitleTrack(ISubtitleTrack track, final IPlayerImplementationControlResultHandler resultHandler) {
            if(track != null && !(track instanceof  ExoSubtitleTrack)) {
                resultHandler.onRejected(new ExoRejectReason(IControlResultHandler.RejectReasonType.ILLEGAL_ARGUMENT, ISubtitleTrack.class.getSimpleName()+" must originate from ExoPlayer-integration"));
                return;
            }
            final ExoSubtitleTrack exoSubtitleTrack = (ExoSubtitleTrack) track;
            AndroidThreadUtil.runOnUiThread(() -> {
                try {
                    if(exoSubtitleTrack != null) {
                        exoSubtitleTrack.applyTo(trackSelector);
                    } else {
                        ExoSubtitleTrack.applyNone(trackSelector);
                    }
                } catch (RuntimeException e) {
                    resultHandler.onError(new UnexpectedError(e));
                    return;
                }
                resultHandler.onDone();
            });
        }
    }

    private class Internals implements IPlayerImplementationInternals {
        private ITimelinePositionFactory timelinePositionFactory;

        public Internals(ITimelinePositionFactory timelinePositionFactory) {
            this.timelinePositionFactory = timelinePositionFactory;
        }

        @Override
        public ITimelinePosition getCurrentPosition() {
            return timelinePositionFactory.newPosition(player.getCurrentPosition());
        }

        @Override
        public ITimelinePosition getCurrentStartBound() {
            Timeline timeline = player.getCurrentTimeline();
            if(timeline.getWindowCount() > 0) {
                long startMs = TimelineUtil.getStartMs(player, timeline, player.getCurrentWindowIndex());
                return timelinePositionFactory.newPosition(startMs);
            } else {
                return null;
            }
        }

        @Override
        public ITimelinePosition getCurrentEndBound() {
            Timeline timeline = player.getCurrentTimeline();
            if(timeline.getWindowCount() > 0) {
                long duration = TimelineUtil.getDurationMs(player, timeline, player.getCurrentWindowIndex());
                return timelinePositionFactory.newPosition(duration);
            } else {
                return null;
            }
        }

        @Override
        public IPlaybackTechnologyIdentifier getTechnologyIdentifier() {
            return ExoPlayerTechnologyIdentifier.get();
        }
    }

    @Override
    public void release() {
        this.player.release();
        this.mediaDrm.release();
    }

    protected Set<EnigmaMediaFormat> initSupportedFormats(Set<EnigmaMediaFormat> supportedFormats) {
        supportedFormats.add(new EnigmaMediaFormat(EnigmaMediaFormat.StreamFormat.DASH, EnigmaMediaFormat.DrmTechnology.NONE));
        supportedFormats.add(new EnigmaMediaFormat(EnigmaMediaFormat.StreamFormat.DASH, EnigmaMediaFormat.DrmTechnology.WIDEVINE));
        return supportedFormats;
    }


    private void onReady(final IEnigmaPlayer enigmaPlayer) {
        customTimestampViewsAdded.whenActive(new Runnable() {
            @Override
            public void run() {
                ITimeline timeline = enigmaPlayer.getTimeline();
                setTimestamp(positionView, timeline.getCurrentPosition());
                setTimestamp(durationView, timeline.getCurrentEndBound());
                timeline.addListener(new BaseTimelineListener() {
                    @Override
                    public void onCurrentPositionChanged(ITimelinePosition timelinePosition) {
                        setTimestamp(positionView, timelinePosition);
                    }

                    @Override
                    public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                        setTimestamp(durationView, end);
                    }
                }, handler);
            }

            private void setTimestamp(TextView view, ITimelinePosition timelinePosition) {
                view.setText(timelinePosition != null ? timelinePosition.toString(timestampFormat) : "");
            }
        });
    }

    public void attachView(View view) {
        if (view instanceof PlayerView) {
            ((PlayerView) view).setPlayer(player);
            playerView = (PlayerView) view;
            if(hideControllerCalled) {
                hideControllerOnPlayerView(playerView);
            } else {
                View exoDurationView = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_duration);
                View exoPositionView = playerView.findViewById(com.google.android.exoplayer2.ui.R.id.exo_position);
                if (exoDurationView != null || exoPositionView != null) {
                    if(exoDurationView == null || exoPositionView == null) {
                        throw new IllegalStateException("Only one of R.id.exo_duration and R.id.exo_position found");
                    }
                    durationView = replaceExoPlayerTextView(exoDurationView);
                    positionView = replaceExoPlayerTextView(exoPositionView);

                    customTimestampViewsAdded.activate();
                }
            }
        } else {
            throw new IllegalArgumentException("Attaching view of type " + view.getClass().getName() + " is not yet supported.");
        }
    }

    private static TextView replaceExoPlayerTextView(View exoPlayerTextView) {
        ViewGroup viewParent = (ViewGroup) exoPlayerTextView.getParent();
        TextView replacement = (TextView) LayoutInflater.from(exoPlayerTextView.getContext()).inflate(R.layout.exoplayer_timestamp_replacement_view, viewParent, false);
        replaceView(exoPlayerTextView, replacement);
        return replacement;
    }

    private static void replaceView(View oldView, View newView) {
        ViewGroup parent = (ViewGroup) oldView.getParent();
        int index = parent.indexOfChild(oldView);
        parent.removeView(oldView);
        parent.addView(newView, index);
    }

    public void hideController() {
        hideControllerCalled = true;
        if(playerView != null) {
            hideControllerOnPlayerView(playerView);
        }
    }

    private static void hideControllerOnPlayerView(PlayerView playerView) {
        playerView.hideController();
        playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if(visibility == View.VISIBLE) {
                    playerView.hideController();
                }
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri) {
        @C.ContentType int type = Util.inferContentType(uri);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
                    .createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }
}
