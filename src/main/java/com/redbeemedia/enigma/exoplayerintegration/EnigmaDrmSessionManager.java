package com.redbeemedia.enigma.exoplayerintegration;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.redbeemedia.enigma.core.util.OpenContainer;
import com.redbeemedia.enigma.core.util.OpenContainerUtil;

import java.util.Map;
import java.util.UUID;

/*package-protected*/ class EnigmaDrmSessionManager implements DrmSessionManager {
    private final IDrmSessionManagerFactory sessionManagerFactory;
    private final DefaultDrmSessionManager normalSessionManager;
    private final OpenContainer<DrmSessionManager> activeSessionManager;
    private final ExoMediaDrm drm;

    public EnigmaDrmSessionManager(ExoMediaDrm.Provider mediaDrmProvider, MediaDrmCallback mediaDrmCallback, ExoMediaDrm mediaDrm) {
        sessionManagerFactory = () -> new DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, mediaDrmProvider)
                .setMultiSession(false)
                .build(mediaDrmCallback);
        this.drm = mediaDrm;
        normalSessionManager = sessionManagerFactory.newDrmSessionManager();
        activeSessionManager = new OpenContainer<>(normalSessionManager);
    }

    @Override
    public void prepare() {
        normalSessionManager.prepare();
    }

    @Override
    public void release() {
        normalSessionManager.release();
    }

    public void reset() {
        OpenContainerUtil.setValueSynchronized(activeSessionManager, normalSessionManager, null);
    }

    public void useOfflineManager(byte[] drmKeys) {
        DefaultDrmSessionManager drmSessionManager = sessionManagerFactory.newDrmSessionManager();
        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_QUERY, drmKeys);
        drmSessionManager.prepare();
        OpenContainerUtil.setValueSynchronized(activeSessionManager, drmSessionManager, null);
    }

    @Nullable
    @Override
    public DrmSession acquireSession(Looper playbackLooper, @Nullable DrmSessionEventListener.EventDispatcher eventDispatcher, Format format) {
        DrmSessionManager sessionManager = OpenContainerUtil.getValueSynchronized(activeSessionManager);
        return sessionManager.acquireSession(playbackLooper, eventDispatcher, format);
    }

    @Nullable
    @Override
    public Class<? extends ExoMediaCrypto> getExoMediaCryptoType(Format format) {
        if(format.exoMediaCryptoType == null) { return null; }
        if(!format.exoMediaCryptoType.equals(com.google.android.exoplayer2.drm.UnsupportedMediaCrypto.class)) {
            return activeSessionManager.value.getExoMediaCryptoType(format);
        }
        return drm.getExoMediaCryptoType();
    }

    private interface IDrmSessionManagerFactory {
        DefaultDrmSessionManager newDrmSessionManager();
    }
}
