package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.IllegalSeekPositionException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.ads.AdIncludedTimeline;
import com.redbeemedia.enigma.core.audio.IAudioTrack;
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
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.core.util.OpenContainer;
import com.redbeemedia.enigma.core.util.OpenContainerUtil;
import com.redbeemedia.enigma.core.virtualui.IVirtualButton;
import com.redbeemedia.enigma.core.virtualui.IVirtualControls;
import com.redbeemedia.enigma.core.virtualui.IVirtualControlsSettings;
import com.redbeemedia.enigma.core.virtualui.VirtualControlsSettings;
import com.redbeemedia.enigma.core.virtualui.impl.VirtualControls;
import com.redbeemedia.enigma.exoplayerintegration.drift.IDriftListener;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoAudioTrack;
import com.redbeemedia.enigma.exoplayerintegration.tracks.ExoSubtitleTrack;
import com.redbeemedia.enigma.exoplayerintegration.ui.ExoButton;
import com.redbeemedia.enigma.exoplayerintegration.ui.TimeBarUtil;
import com.redbeemedia.enigma.exoplayerintegration.util.LoadRequestParameterApplier;
import com.redbeemedia.enigma.exoplayerintegration.util.MediaSourceFactoryConfigurator;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class ExoPlayerTech implements IPlayerImplementation {
    private static final String TAG = "ExoPlayerTech";
    private static final UUID WIDEVINE_UUID = Util.getDrmUuid("widevine");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DataSource.Factory mediaDataSourceFactory;
    private final ReusableExoMediaDrm mediaDrm;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private PlayerView playerView = null;
    private boolean hideControllerCalled = false;
    private final EnigmaDrmSessionManager drmSessionManager;
    private MediaDrmFromProviderCallback mediaDrmCallback;
    private MediaFormatSpecification supportedFormats = new MediaFormatSpecification();
    private TimelinePositionFormat timestampFormat = TimelinePositionFormat.newFormat(new ExoPlayerDurationFormat(), "HH:mm");
    private OpenContainer<ExoPlayerListener> exoPlayerListener = new OpenContainer<>(null);

    private final IActivation customTimestampViewsAdded = new Activation();
    private final IActivation playerViewControlsReady = new Activation();
    private TextView positionView;
    private TextView durationView;
    private boolean released;
    private Duration liveDelay;
    private Internals organs;

    private DriftMeter driftMeter;

    private EnigmaMediaSourceFactory mediaSourceFactory = new EnigmaMediaSourceFactory();
    public ExoPlayerTech(Context context, String appName) {
        ExoPlayerIntegrationContext.assertInitialized(); //Assert module initialized

        this.mediaDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, appName));

        this.mediaDrmCallback = new MediaDrmFromProviderCallback(context,appName);

        for(EnigmaMediaFormat format : initSupportedFormats(new HashSet<>())) {
            supportedFormats.add(format);
        }

        this.trackSelector = new DefaultTrackSelector(context, new AdaptiveTrackSelection.Factory());

        DefaultRenderersFactory rendersFactory =
            new DefaultRenderersFactory(context);
        rendersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        try {
            if(supportedFormats.isWidewineSupported()) {
                ExoMediaDrm drm = FrameworkMediaDrm.newInstance(WIDEVINE_UUID);
                this.mediaDrm = new ReusableExoMediaDrm(() -> drm);
                drmSessionManager = new EnigmaDrmSessionManager(new ExoMediaDrm.AppManagedProvider(this.mediaDrm), mediaDrmCallback, this.mediaDrm);
            } else {
                this.mediaDrm = null;
                drmSessionManager = null;
            }

            this.player = new ExoPlayer.Builder(context, rendersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setRenderersFactory(new EnigmaRendererFactory(context))
                    .build();//ExoPlayerFactory.newSimpleInstance(context, rendersFactory, trackSelector, drmSessionManager);

            this.driftMeter = new DriftMeter(player, handler);
        } catch (UnsupportedDrmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void install(IEnigmaPlayerEnvironment environment) {
        mediaDrmCallback.setDrmProvider(environment.getDrmProvider());
        environment.setMediaFormatSupportSpec(supportedFormats);
        ITimelinePositionFactory timelinePositionFactory = environment.getTimelinePositionFactory();

        ExoPlayerListener newExoPlayerListener = new ExoPlayerListener(environment.getPlayerImplementationListener());
        OpenContainerUtil.setValueSynchronized(exoPlayerListener, newExoPlayerListener, null);
        player.addListener(newExoPlayerListener);

        player.addListener(new ExoPlayerTimelineListener(player, environment.getPlayerImplementationListener(), timelinePositionFactory));
        environment.setControls(new Controls());
        organs = new Internals(timelinePositionFactory);
        environment.setInternals(organs);
        environment.addEnigmaPlayerReadyListener(enigmaPlayer -> ExoPlayerTech.this.onReady(enigmaPlayer));
        environment.addEnigmaPlayerReadyListener(driftMeter);
    }

    public void setTimestampFormat(TimelinePositionFormat timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    @Override
    public void setupPlayerNotificationManager(PlayerNotificationManager manager) {
        manager.setPlayer(player);
    }

    private class Controls implements IPlayerImplementationControls {
        @Override
        public void load(ILoadRequest loadRequest, IPlayerImplementationControlResultHandler resultHandler) {
            drmSessionManager.reset();
            liveDelay = loadRequest.getLiveDelay();
            final LoadRequestParameterApplier parameterApplier = new LoadRequestParameterApplier(loadRequest) {
                @Override
                protected void onException(Exception e) {
                    Log.d(TAG, "Exception while trying to apply load-parameters", e);
                }
            };
            final MediaSource mediaSource;
            try {
                mediaSource = buildMediaSource(loadRequest);
            } catch(ControlRequestResolutionException earlyResolution) {
                earlyResolution.apply(resultHandler);
                return;
            } catch (RuntimeException e) {
                resultHandler.onError(new UnexpectedError(e));
                return;
            }
            AndroidThreadUtil.runOnUiThread(() -> {
                if(released) { return; }

                try {

                    if(mediaDrm != null) {
                        mediaDrm.revive();
                    }
                    parameterApplier.applyTo(trackSelector);
                    player.setMediaSource(mediaSource);
                    player.prepare();
                    ExoPlayerListener listenerSnapshot = OpenContainerUtil.getValueSynchronized(exoPlayerListener);
                    if(listenerSnapshot != null) {
                        listenerSnapshot.onLoadingNewMediaSource();
                    }
                } catch (Exception e) {
                    resultHandler.onError(new UnexpectedError(e));
                    return;
                }
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
            if(released) { return; }
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
                AndroidThreadUtil.runOnUiThread(() -> {
                    if(!player.isCurrentWindowDynamic()) {
                        resultHandler.onRejected(new ExoRejectReason(IControlResultHandler.RejectReasonType.INAPPLICABLE_FOR_CURRENT_STREAM, "Video is not dynamic"));
                    } else {
                        try {
                            player.seekToDefaultPosition();
                            resultHandler.onDone();
                        } catch (IllegalSeekPositionException e) {
                            resultHandler.onError(new IllegalSeekPositionError(e));
                        }
                    }
                });
            } else if(seekPosition instanceof IPlayerImplementationControls.TimelineRelativePosition) {
                long millis = ((TimelineRelativePosition) seekPosition).getMillis();
                AndroidThreadUtil.runOnUiThread(() -> {
                    player.seekTo(millis);
                    player.addListener(new Player.Listener() {
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
            if(released) { return; }
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
            if(released) { return; }
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

        @Override
        public void setAudioTrack(IAudioTrack track, final IPlayerImplementationControlResultHandler resultHandler) {
            if(released) { return; }
            if(track != null && !(track instanceof ExoAudioTrack)) {
                resultHandler.onRejected(new ExoRejectReason(IControlResultHandler.RejectReasonType.ILLEGAL_ARGUMENT, IAudioTrack.class.getSimpleName()+" must originate from ExoPlayer-integration"));
                return;
            }
            final ExoAudioTrack exoAudioTrack = (ExoAudioTrack) track;
            AndroidThreadUtil.runOnUiThread(() -> {
                try {
                    if(exoAudioTrack != null) {
                        exoAudioTrack.applyTo(trackSelector);
                    } else {
                        resultHandler.onRejected(new ExoRejectReason(IControlResultHandler.RejectReasonType.ILLEGAL_ARGUMENT, "track was null"));
                        return;
                    }
                } catch (RuntimeException e) {
                    resultHandler.onError(new UnexpectedError(e));
                    return;
                }
                resultHandler.onDone();
            });
        }

        @Override
        public void setMaxVideoTrackDimensions(int width, int height, IPlayerImplementationControlResultHandler controlResultHandler) {
            if(released) { return; }
            AndroidThreadUtil.runOnUiThread(() -> {
                try {
                    DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
                    parametersBuilder.setMaxVideoSize(width, height);
                    trackSelector.setParameters(parametersBuilder.build());
                    controlResultHandler.onDone();
                } catch (RuntimeException e){
                    controlResultHandler.onError(new UnexpectedError(e));
                    return;
                }
            });
        }
    }

    private static final int OPERATION_TIMEOUT = 500;

    private class Internals implements IPlayerImplementationInternals {
        private ITimelinePositionFactory timelinePositionFactory;
        private final Timeline.Window reusableWindow = new Timeline.Window();
        private boolean released;
        public void release() {
            released = true;
        }

        public Internals(ITimelinePositionFactory timelinePositionFactory) {
            this.timelinePositionFactory = timelinePositionFactory;
        }

        @Override
        public ITimelinePosition getCurrentPosition() {
            try {
                return AndroidThreadUtil.getBlockingOnUiThread(OPERATION_TIMEOUT, () -> timelinePositionFactory.newPosition(player.getCurrentPosition()));
            } catch (TimeoutException e) {
                Log.e(TAG, "TimeoutException: " + e.getLocalizedMessage());
                return timelinePositionFactory.newPosition(0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ITimelinePosition getCurrentStartBound() {
            if(released) { return null; }
            try {
                Timeline timeline = AndroidThreadUtil.getBlockingOnUiThread(OPERATION_TIMEOUT, () -> player.getCurrentTimeline());
                if(timeline.getWindowCount() > 0) {
                    return timelinePositionFactory.newPosition(0);
                } else {
                    return null;
                }
            } catch (InterruptedException | TimeoutException e) {
                return null;
            }
        }

        @Override
        public ITimelinePosition getCurrentEndBound() {
            if(released) { return null; }
            try {
                long duration = AndroidThreadUtil.getBlockingOnUiThread(OPERATION_TIMEOUT, () -> player.getDuration());
                ITimelinePosition iTimelinePosition = duration != C.TIME_UNSET ? timelinePositionFactory.newPosition(duration) : null;
                if (iTimelinePosition != null && liveDelay != null) {
                    iTimelinePosition = iTimelinePosition.subtract(liveDelay);
                }
                return iTimelinePosition;
            } catch (InterruptedException | TimeoutException e) {
                return null;
            }
        }

        @Override
        public ITimelinePosition getLivePosition() {
            if(released) { return null; }
            try {
                return AndroidThreadUtil.getBlockingOnUiThread(OPERATION_TIMEOUT, () -> {
                    Timeline timeline = player.getCurrentTimeline();
                    if(timeline.getWindowCount() > 0) {
                        int currentWindowIndex = player.getCurrentMediaItemIndex();
                        long position;
                        synchronized (reusableWindow) {
                            timeline.getWindow(currentWindowIndex, reusableWindow);
                            position = reusableWindow.getDefaultPositionMs();
                        }
                        return position == C.TIME_UNSET ? null : timelinePositionFactory.newPosition(position);
                    } else {
                        return null;
                    }
                });
            } catch (TimeoutException e) {
                e.printStackTrace();
                return null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public IPlaybackTechnologyIdentifier getTechnologyIdentifier() {
            return ExoPlayerTechnologyIdentifier.get();
        }
    }

    @Override
    public void release() {
        released = true;
        this.driftMeter.release();
        this.player.release();
        if(this.mediaDrm != null) {
            this.mediaDrm.release();
        }
        if(organs != null) { organs.release(); }
    }

    @Override
    public void updateTimeBar(long millis) {
        TimeBar timeBar = playerView.findViewById(R.id.exo_progress);
        AndroidThreadUtil.runOnUiThread(() -> {
            if (millis != 0) {
                timeBar.setPosition(millis);
            }
        });
    }

    protected Set<EnigmaMediaFormat> initSupportedFormats(Set<EnigmaMediaFormat> supportedFormats) {
        supportedFormats.add(new EnigmaMediaFormat(EnigmaMediaFormat.StreamFormat.DASH, EnigmaMediaFormat.DrmTechnology.NONE));

        //Check if we can instantiate FrameworkMediaDrm using WIDEWINE_UUID. If we get any exception, this devices can not support Widewine
        if(canInstantiateWidewineDrm()) {
            supportedFormats.add(new EnigmaMediaFormat(EnigmaMediaFormat.StreamFormat.DASH, EnigmaMediaFormat.DrmTechnology.WIDEVINE));
        } else {
            Log.d(TAG, "Widewine does not seem to be supported on this device");
        }

        supportedFormats.add(new EnigmaMediaFormat(EnigmaMediaFormat.StreamFormat.HLS, EnigmaMediaFormat.DrmTechnology.NONE));
        supportedFormats.add(new EnigmaMediaFormat(EnigmaMediaFormat.StreamFormat.MP3, EnigmaMediaFormat.DrmTechnology.NONE));

        return supportedFormats;
    }

    private boolean canInstantiateWidewineDrm() {
        try {
            FrameworkMediaDrm.newInstance(WIDEVINE_UUID);
            return true;
        } catch (Exception e) {
            return false;
        }
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
                        if(timelinePosition != null) {
                            ITimelinePosition startBound = timeline.getCurrentStartBound();
                            if(startBound != null) {
                                if(timelinePosition.before(startBound)) {
                                    timelinePosition = startBound;
                                }
                            }

                            ITimelinePosition endBound = timeline.getCurrentEndBound();
                            if(endBound != null) {
                                if(timelinePosition.after(endBound)) {
                                    timelinePosition = endBound;
                                }
                            }
                        }
                        setTimestamp(positionView, timelinePosition);
                    }

                    @Override
                    public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                        setTimestamp(durationView, end);
                        showAdsMarkerOnTimeline(enigmaPlayer);
                    }
                }, handler);
            }

            private void setTimestamp(TextView view, ITimelinePosition timelinePosition) {
                view.setText(timelinePosition != null ? timelinePosition.toString(timestampFormat) : "");
            }
        });
        playerViewControlsReady.whenActive(new Runnable() {
            private void connectButtonIfExists(ExoButton exoButton, IVirtualButton virtualButton) {
                connectButtonIfExists(exoButton, virtualButton, false);
            }
            private void connectButtonIfExists(ExoButton exoButton, IVirtualButton virtualButton, boolean hideIfDisabled) {
                if(exoButton != null) {
                    exoButton.setVirtualButton(virtualButton, handler);
                    if(hideIfDisabled) {
                        exoButton.hideIfDisabled();
                    }
                }
            }
            @Override
            public void run() {
                IVirtualControls virtualControls = VirtualControls.create(enigmaPlayer, createVirtualControlsSettings());
                enigmaPlayer.setVirtualControls(virtualControls);

                connectButtonIfExists(playerView.findViewById(R.id.exo_ffwd), virtualControls.getFastForward());
                connectButtonIfExists(playerView.findViewById(R.id.exo_rew), virtualControls.getRewind());
                connectButtonIfExists(playerView.findViewById(R.id.exo_pause), virtualControls.getPause(), true);
                connectButtonIfExists(playerView.findViewById(R.id.exo_play), virtualControls.getPlay(), true);
                connectButtonIfExists(playerView.findViewById(R.id.exo_next), virtualControls.getNextProgram());
                connectButtonIfExists(playerView.findViewById(R.id.exo_prev), virtualControls.getPreviousProgram());

                TimeBar timeBar = playerView.findViewById(R.id.exo_progress);
                TimeBarUtil.connect(timeBar, enigmaPlayer, virtualControls);
            }
        });
    }

    private void showAdsMarkerOnTimeline(IEnigmaPlayer enigmaPlayer) {
        AdIncludedTimeline adIncludedTimeline = (AdIncludedTimeline) enigmaPlayer.getTimeline();
        List<ITimelinePosition> adBreaksPositions = adIncludedTimeline.getAdBreaksPositions();
        if(adBreaksPositions != null) {
            long[] adGroupTimesMs = new long[adBreaksPositions.size()];
            for (int i = 0; i < adBreaksPositions.size(); i++) {
                ITimelinePosition adTime = adBreaksPositions.get(i);
                adGroupTimesMs[i] = adTime.getStart();
            }
            boolean[] playedAdGroups = new boolean[adGroupTimesMs.length];
            playerView.setExtraAdGroupMarkers(adGroupTimesMs, playedAdGroups);
        }
    }

    /**
     * Override this to change or implement your own VirtualControlsSettings.
     * @return
     */
    protected IVirtualControlsSettings createVirtualControlsSettings() {
        return new VirtualControlsSettings();
    }

    public void attachView(View view) {
        if (view instanceof PlayerView) {
            PlayerView.switchTargetView(player, playerView, (PlayerView) view);
            playerView = (PlayerView) view;

            if(hideControllerCalled) {
                hideControllerOnPlayerView(playerView);
                playerViewControlsReady.destroy();
            } else {
                View exoDurationView = playerView.findViewById(R.id.exo_duration);
                View exoPositionView = playerView.findViewById(R.id.exo_position);
                if (exoDurationView != null || exoPositionView != null) {
                    if(exoDurationView == null || exoPositionView == null) {
                        throw new IllegalStateException("Only one of R.id.exo_integration_duration and R.id.exo_integration_position found");
                    }
                    durationView = replaceExoPlayerTextView(exoDurationView);
                    positionView = replaceExoPlayerTextView(exoPositionView);

                    customTimestampViewsAdded.activate();
                }

                playerViewControlsReady.activate();
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

    public boolean addDriftListener(IDriftListener driftListener) {
        return driftMeter.addDriftListener(driftListener);
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

    private MediaSource buildMediaSource(IPlayerImplementationControls.ILoadRequest loadRequest) throws ControlRequestResolutionException {
        if(loadRequest instanceof IPlayerImplementationControls.IStreamLoadRequest) {
            IPlayerImplementationControls.IStreamLoadRequest typedLR = (IPlayerImplementationControls.IStreamLoadRequest) loadRequest;
            return buildMediaSource(Uri.parse(typedLR.getUrl()),loadRequest);
        } else if(loadRequest instanceof IPlayerImplementationControls.IDownloadedLoadRequest) {
            IPlayerImplementationControls.IDownloadedLoadRequest typedLR = (IPlayerImplementationControls.IDownloadedLoadRequest)loadRequest;
            Object downloadData = typedLR.getDownloadData();
            if(!(downloadData instanceof IMediaSourceFactory)) {
                throw ControlRequestResolutionException.onRejected(new ExoRejectReason(
                        IControlResultHandler.RejectReasonType.ILLEGAL_ARGUMENT,
                        "Unrecognized downloadData"));
            }
            IMediaSourceFactory mediaSourceFactory = (IMediaSourceFactory)downloadData;

            if(downloadData instanceof IOfflineDrmKeySource) {
                byte[] drmKeys = ((IOfflineDrmKeySource)downloadData).getDrmKeys();
                if(drmKeys != null) {
                    drmSessionManager.useOfflineManager(drmKeys);
                }
            }

            return mediaSourceFactory.createMediaSource(new MediaSourceFactoryConfigurator(drmSessionManager, loadRequest));
        } else {
            throw ControlRequestResolutionException.onRejected(new ExoRejectReason(
                    IControlResultHandler.RejectReasonType.ILLEGAL_ARGUMENT,
                    "LoadRequest type not supported by this player implementation"));
        }
    }

    private MediaSource buildMediaSource(Uri uri, IPlayerImplementationControls.ILoadRequest loadRequest) {
        @C.ContentType int type = Util.inferContentType(uri);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(uri);
        Duration liveDelay = loadRequest.getLiveDelay();
        if (liveDelay != null) {
            MediaItem.LiveConfiguration.Builder liveConfigurationBuilder = new MediaItem.LiveConfiguration.Builder();
            liveConfigurationBuilder.setTargetOffsetMs(liveDelay.inWholeUnits(Duration.Unit.MILLISECONDS));
            liveConfigurationBuilder.setMaxOffsetMs(liveDelay.inWholeUnits(Duration.Unit.MILLISECONDS));
            liveConfigurationBuilder.setMinOffsetMs(liveDelay.inWholeUnits(Duration.Unit.MILLISECONDS));
            builder.setLiveConfiguration(liveConfigurationBuilder.build());
        }

        MediaSourceFactory internalMediaSourceFactory = null;
        switch (type) {
            case C.TYPE_DASH:
                internalMediaSourceFactory = new DashMediaSource.Factory(mediaDataSourceFactory);
                break;
            case C.TYPE_SS:
                internalMediaSourceFactory = new SsMediaSource.Factory(mediaDataSourceFactory);
                break;
            case C.TYPE_HLS:
                internalMediaSourceFactory = new HlsMediaSource.Factory(mediaDataSourceFactory);
                break;
            case C.TYPE_OTHER:
                internalMediaSourceFactory = new ProgressiveMediaSource.Factory(mediaDataSourceFactory);
                break;
            default:
                throw new InvalidParameterException("Unsupported type: " + type);
        }

        if(type != C.TYPE_OTHER) {
            internalMediaSourceFactory.setDrmSessionManagerProvider(mediaItem -> drmSessionManager);
        }
        mediaSourceFactory.setInternalFactory(internalMediaSourceFactory);
        return mediaSourceFactory.createMediaSource(builder.build());
    }
}
