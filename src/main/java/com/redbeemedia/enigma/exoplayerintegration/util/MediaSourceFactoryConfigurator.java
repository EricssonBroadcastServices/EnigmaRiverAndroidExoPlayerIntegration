package com.redbeemedia.enigma.exoplayerintegration.util;

import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControls;
import com.redbeemedia.enigma.core.time.Duration;

/**
 * Configures MediaSourceFactories based on a {@link com.redbeemedia.enigma.core.player.IPlayerImplementationControls.ILoadRequest}.
 */
public class MediaSourceFactoryConfigurator {
    private final IPlayerImplementationControls.ILoadRequest loadRequest;
    private final DrmSessionManager drmSessionManager;

    public MediaSourceFactoryConfigurator(DrmSessionManager drmSessionManager, IPlayerImplementationControls.ILoadRequest loadRequest) {
        this.loadRequest = loadRequest;
        this.drmSessionManager = drmSessionManager;
    }

    public DashMediaSource.Factory configure(DashMediaSource.Factory factory) {
        configureInternal(new IMediaSourceFactoryAdapter() {
            @Override
            public void setLiveDelay(Duration liveDelay) {
                factory.setLivePresentationDelayMs(liveDelay.inWholeUnits(Duration.Unit.MILLISECONDS), true);
            }
        });
        return factory.setDrmSessionManagerProvider(mediaItem -> drmSessionManager);
    }

    public SsMediaSource.Factory configure(SsMediaSource.Factory factory) {
        configureInternal(new IMediaSourceFactoryAdapter() {
            @Override
            public void setLiveDelay(Duration liveDelay) {
                factory.setLivePresentationDelayMs(liveDelay.inWholeUnits(Duration.Unit.MILLISECONDS));
            }
        });
        return factory.setDrmSessionManagerProvider(mediaItem -> drmSessionManager);
    }


    public HlsMediaSource.Factory configure(HlsMediaSource.Factory factory) {
        configureInternal(new IMediaSourceFactoryAdapter() {
            @Override
            public void setLiveDelay(Duration liveDelay) {
                // Not supported
            }
        });
        return factory.setDrmSessionManagerProvider(mediaItem -> drmSessionManager);
    }

    public ProgressiveMediaSource.Factory configure(ProgressiveMediaSource.Factory factory) {
        configureInternal(new IMediaSourceFactoryAdapter() {
            @Override
            public void setLiveDelay(Duration liveDelay) {
                // Not supported
            }
        });
        return factory;
    }

    private void configureInternal(IMediaSourceFactoryAdapter factoryAdapter) {
        Duration duration = loadRequest.getLiveDelay();
        if(duration != null) {
            factoryAdapter.setLiveDelay(duration);
        }
    }

    private interface IMediaSourceFactoryAdapter {
        void setLiveDelay(Duration liveDelay);
    }
}
