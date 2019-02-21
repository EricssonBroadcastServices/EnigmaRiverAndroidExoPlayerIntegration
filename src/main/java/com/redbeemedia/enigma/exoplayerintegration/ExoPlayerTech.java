package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
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
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.format.EnigmaMediaFormat;
import com.redbeemedia.enigma.core.player.IEnigmaPlayerEnvironment;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExoPlayerTech implements IPlayerImplementation {
    private final DataSource.Factory mediaDataSourceFactory;
    private final ReusableExoMediaDrm<FrameworkMediaCrypto> mediaDrm;
    private SimpleExoPlayer player;
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
        player.addListener(new ListenerAdapter(environment.getPlayerImplementationListener()));
    }

    @Override
    public void startPlayback(String url) {
        final MediaSource mediaSource = buildMediaSource(Uri.parse(url));
        AndroidThreadUtil.runOnUiThread(() -> {
            mediaDrm.revive();
            player.prepare(mediaSource, true, false);
            player.setPlayWhenReady(true);
        });
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
        } else {
            throw new IllegalArgumentException("Attaching view of type " + view.getClass().getName() + " is not yet supported.");
        }
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
