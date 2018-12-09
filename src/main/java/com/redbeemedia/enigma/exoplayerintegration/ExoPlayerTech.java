package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
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
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.player.IEnigmaPlayerEnvironment;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

import java.util.UUID;

public class ExoPlayerTech implements IPlayerImplementation {

    private MediaSource mediaSource;
    private final DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private FrameworkMediaDrm mediaDrm;
    private DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    private MediaDrmCallbackImpl mediaDrmCallback;
    private TrackSelector trackSelector;

    public ExoPlayerTech(Context context, String appName) {
        mediaDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, appName));
        trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        mediaDrmCallback = new MediaDrmCallbackImpl(context,appName);
        DefaultRenderersFactory rendersFactory =
            new DefaultRenderersFactory(context, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        try {
            drmSessionManager = buildDrmSessionManagerV18(Util.getDrmUuid("widevine"), false);
        } catch (UnsupportedDrmException e) {
            throw new RuntimeException(e);
        }
        this.player = ExoPlayerFactory.newSimpleInstance(context, rendersFactory, trackSelector, drmSessionManager);
    }

    @Override
    public void install(IEnigmaPlayerEnvironment environment) {
        mediaDrmCallback.setDrmProvider(environment.getDrmProvider());
    }

    @Override
    public void startPlayback(String url) {
        mediaSource = buildMediaSource(Uri.parse(url));
        AndroidThreadUtil.runOnUiThread(() -> {
            player.prepare(mediaSource, true, false);
            player.setPlayWhenReady(true);
        });
    }

    public void attachView(View view) {
        if (view instanceof PlayerView) {
            ((PlayerView) view).setPlayer(player);
        } else {
            throw new IllegalArgumentException("Attaching view of type " + view.getClass().getName() + " is not yet supported.");
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
            mediaSource = null;
            trackSelector = null;
        }
        releaseMediaDrm();
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

    private DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(
        UUID uuid, boolean multiSession)
        throws UnsupportedDrmException {
        releaseMediaDrm();
        mediaDrm = FrameworkMediaDrm.newInstance(uuid);
        return new DefaultDrmSessionManager<>(uuid, mediaDrm, mediaDrmCallback, null, multiSession);
    }

    private void releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }
}
