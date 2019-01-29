package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;

import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.player.IDrmInfo;
import com.redbeemedia.enigma.core.player.IDrmProvider;

import java.util.UUID;

/*package-protected*/ class MediaDrmFromProviderCallback implements MediaDrmCallback {
    private IDrmProvider drmProvider;
    private HttpDataSource.Factory licenseDataSourceFactory;

    public MediaDrmFromProviderCallback(Context context, String appName) {
        licenseDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(context, appName));
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
        MediaDrmCallback mediaDrmCallback = createDelegateCallback(drmProvider.getDrmInfo());
        return mediaDrmCallback.executeProvisionRequest(uuid, request);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
        MediaDrmCallback mediaDrmCallback = createDelegateCallback(drmProvider.getDrmInfo());
        return mediaDrmCallback.executeKeyRequest(uuid, request);
    }

    private MediaDrmCallback createDelegateCallback(IDrmInfo drmInfo) {
        HttpMediaDrmCallback httpMediaDrmCallback = new HttpMediaDrmCallback(drmInfo.getLicenseUrl(), licenseDataSourceFactory);
        if (drmInfo.getDrmKeyRequestPropertiesArray() != null) {
            String[] keyRequestPropertiesArray = drmInfo.getDrmKeyRequestPropertiesArray();
            if (keyRequestPropertiesArray != null) {
                for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                    httpMediaDrmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                            keyRequestPropertiesArray[i + 1]);
                }
            }
        }
        return httpMediaDrmCallback;
    }

    public void setDrmProvider(IDrmProvider drmProvider) {
        this.drmProvider = drmProvider;
    }
}
