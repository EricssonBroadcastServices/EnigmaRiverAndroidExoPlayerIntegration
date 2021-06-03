package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;

import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallbackException;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.drm.IDrmInfo;
import com.redbeemedia.enigma.core.drm.IDrmProvider;

import java.util.Map;
import java.util.UUID;

/*package-protected*/ class MediaDrmFromProviderCallback implements MediaDrmCallback {
    private IDrmProvider drmProvider;
    private HttpDataSource.Factory licenseDataSourceFactory;

    public MediaDrmFromProviderCallback(Context context, String appName) {
        licenseDataSourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(Util.getUserAgent(context, appName));
        //TORD: new DefaultHttpDataSourceFactory(Util.getUserAgent(context, appName));
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws MediaDrmCallbackException {
        MediaDrmCallback mediaDrmCallback = createDelegateCallback(drmProvider.getDrmInfo());
        return mediaDrmCallback.executeProvisionRequest(uuid, request);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws MediaDrmCallbackException {
        MediaDrmCallback mediaDrmCallback = createDelegateCallback(drmProvider.getDrmInfo());
        return mediaDrmCallback.executeKeyRequest(uuid, request);
    }

    private MediaDrmCallback createDelegateCallback(IDrmInfo drmInfo) {
        HttpMediaDrmCallback httpMediaDrmCallback = new HttpMediaDrmCallback(drmInfo.getLicenseUrl(), licenseDataSourceFactory);

        for(Map.Entry<String, String> entry : drmInfo.getDrmKeyRequestProperties()) {
            httpMediaDrmCallback.setKeyRequestProperty(entry.getKey(), entry.getValue());
        }
        return httpMediaDrmCallback;
    }

    public void setDrmProvider(IDrmProvider drmProvider) {
        this.drmProvider = drmProvider;
    }
}
