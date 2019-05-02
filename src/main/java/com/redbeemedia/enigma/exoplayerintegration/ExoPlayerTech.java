package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;
import android.net.Uri;
import android.view.View;

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
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.error.IllegalSeekPositionError;
import com.redbeemedia.enigma.core.error.UnexpectedError;
import com.redbeemedia.enigma.core.format.EnigmaMediaFormat;
import com.redbeemedia.enigma.core.player.IEnigmaPlayerEnvironment;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControlResultHandler;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControls;
import com.redbeemedia.enigma.core.player.IPlayerImplementationInternals;
import com.redbeemedia.enigma.core.player.ITimelinePositionFactory;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExoPlayerTech implements IPlayerImplementation {
    private final DataSource.Factory mediaDataSourceFactory;
    private final ReusableExoMediaDrm<FrameworkMediaCrypto> mediaDrm;
    private SimpleExoPlayer player;
    private PlayerView playerView = null;
    private boolean hideControllerCalled = false;
    private MediaDrmFromProviderCallback mediaDrmCallback;
    private MediaFormatSpecification supportedFormats = new MediaFormatSpecification();

    public ExoPlayerTech(Context context, String appName) {
        this.mediaDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, appName));
        this.mediaDrmCallback = new MediaDrmFromProviderCallback(context,appName);

        for(EnigmaMediaFormat format : initSupportedFormats(new HashSet<>())) {
            supportedFormats.add(format);
        }

        TrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
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

    public void attachView(View view) {
        if (view instanceof PlayerView) {
            ((PlayerView) view).setPlayer(player);
            playerView = (PlayerView) view;
            if(hideControllerCalled) {
                hideControllerOnPlayerView(playerView);
            }
        } else {
            throw new IllegalArgumentException("Attaching view of type " + view.getClass().getName() + " is not yet supported.");
        }
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
