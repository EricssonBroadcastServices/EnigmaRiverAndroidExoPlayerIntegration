package com.redbeemedia.enigma.exoplayerintegration;

import static com.google.android.exoplayer2.C.CRYPTO_TYPE_UNSUPPORTED;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.analytics.PlayerId;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionEventListener;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.redbeemedia.enigma.core.util.OpenContainer;
import com.redbeemedia.enigma.core.util.OpenContainerUtil;

/*package-protected*/ class EnigmaDrmSessionManager implements DrmSessionManager {
    private final IDrmSessionManagerFactory sessionManagerFactory;
    private final DefaultDrmSessionManager normalSessionManager;
    private final OpenContainer<DrmSessionManager> activeSessionManager;
    private final ReusableExoMediaDrm drm;

    public EnigmaDrmSessionManager(ExoMediaDrm.Provider mediaDrmProvider, MediaDrmCallback mediaDrmCallback, ReusableExoMediaDrm mediaDrm) {
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
        try {
            normalSessionManager.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        if (normalSessionManager != null) {
            normalSessionManager.release();
        }
    }

    @Override
    public void setPlayer(Looper looper, PlayerId playerId) {
        DrmSessionManager sessionManager = OpenContainerUtil.getValueSynchronized(activeSessionManager);
        sessionManager.setPlayer(looper, playerId);
    }

    @Nullable
    @Override
    public DrmSession acquireSession(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher, Format format) {
        DrmSessionManager sessionManager = OpenContainerUtil.getValueSynchronized(activeSessionManager);
        return sessionManager.acquireSession(eventDispatcher, format);
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


    @Override
    public int getCryptoType(Format format) {
        DrmSessionManager sessionManager = OpenContainerUtil.getValueSynchronized(activeSessionManager);
        return sessionManager.getCryptoType(format);
    }

    public void setMode(int modeDownload, byte[] drmKeys) {
        normalSessionManager.setMode(modeDownload, drmKeys);
    }

    private interface IDrmSessionManagerFactory {
        DefaultDrmSessionManager newDrmSessionManager();
    }

    public ReusableExoMediaDrm getDrm() {
        return drm;
    }
}
