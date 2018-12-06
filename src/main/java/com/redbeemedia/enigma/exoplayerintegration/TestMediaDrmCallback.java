package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;

import java.util.UUID;

public class TestMediaDrmCallback implements MediaDrmCallback {

    private MediaDrmCallback mediaDrmCallback;

    public TestMediaDrmCallback(MediaDrmCallback mediaDrmCallback) {
        this.mediaDrmCallback = mediaDrmCallback;
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) throws Exception {
        return mediaDrmCallback.executeProvisionRequest(uuid, request);
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) throws Exception {
        return mediaDrmCallback.executeKeyRequest(uuid,request);
    }

    public void setMediaDrmCallback(MediaDrmCallback mediaDrmCallback) {
        this.mediaDrmCallback = mediaDrmCallback;

    }
}
