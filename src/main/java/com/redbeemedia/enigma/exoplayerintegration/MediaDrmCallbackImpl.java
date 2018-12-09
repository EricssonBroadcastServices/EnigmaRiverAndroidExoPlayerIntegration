package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;

import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.player.DrmInfo;
import com.redbeemedia.enigma.core.player.IDrmProvider;

import java.util.UUID;

//TODO: change the name
public class MediaDrmCallbackImpl implements MediaDrmCallback {
    private IDrmProvider drmProvider;
    private HttpMediaDrmCallback mediaDrmCallback;
    private HttpDataSource.Factory licenseDataSourceFactory;

    public MediaDrmCallbackImpl(Context context, String appName) {
        licenseDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(context, appName));
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
        return mediaDrmCallback.executeProvisionRequest(uuid, request);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
        DrmInfo drmInfo = drmProvider.getDrmInfo();
            mediaDrmCallback = new HttpMediaDrmCallback(drmInfo.getLicenseUrl(), licenseDataSourceFactory);
            if (drmInfo.getDrmKeyRequestPropertiesArray() != null) {
                String[] keyRequestPropertiesArray = drmInfo.getDrmKeyRequestPropertiesArray();
                if (keyRequestPropertiesArray != null) {
                    for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                        mediaDrmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                            keyRequestPropertiesArray[i + 1]);
                    }
                }
            }
            return mediaDrmCallback.executeKeyRequest(uuid, request);
    }

    public void setDrmProvider(IDrmProvider drmProvider) {
        this.drmProvider = drmProvider;
    }
}
