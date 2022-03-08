package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy;

public class EnigmaMediaSourceFactory implements MediaSource.Factory {

    private MediaSource.Factory internalFactory;

    public MediaSource.Factory setInternalFactory(MediaSourceFactory internalFactory) {
        this.internalFactory = internalFactory;
        return this;
    }

    @Override
    public MediaSource.Factory setDrmSessionManagerProvider(@Nullable DrmSessionManagerProvider drmSessionManagerProvider) {
        return internalFactory.setDrmSessionManagerProvider(drmSessionManagerProvider);
    }

    @Override
    public MediaSource.Factory setLoadErrorHandlingPolicy(@Nullable LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        return internalFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
    }

    @Override
    public int[] getSupportedTypes() {
        return internalFactory.getSupportedTypes();
    }

    @Override
    public MediaSource createMediaSource(MediaItem mediaItem) {
        return internalFactory.createMediaSource(mediaItem);
    }
}
