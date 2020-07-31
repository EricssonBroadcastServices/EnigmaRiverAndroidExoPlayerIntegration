package com.redbeemedia.enigma.exoplayerintegration;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.redbeemedia.enigma.core.util.OpenContainer;
import com.redbeemedia.enigma.core.util.OpenContainerUtil;

import java.util.Map;

/*package-protected*/ class EnigmaDrmSessionManager<T extends ExoMediaCrypto> implements DrmSessionManager<T> {
    private final IDrmSessionManagerFactory<T> sessionManagerFactory;
    private final DefaultDrmSessionManager<T> normalSessionManager;
    private final OpenContainer<DrmSessionManager<T>> activeSessionManager;

    public EnigmaDrmSessionManager(ExoMediaDrm<T> mediaDrm, MediaDrmCallback mediaDrmCallback) {
        sessionManagerFactory = () -> new DefaultDrmSessionManager<>(C.WIDEVINE_UUID, mediaDrm, mediaDrmCallback, null, false);
        normalSessionManager = sessionManagerFactory.newDrmSessionManager();
        activeSessionManager = new OpenContainer<>(normalSessionManager);
    }


    @Override
    public boolean canAcquireSession(DrmInitData drmInitData) {
        return OpenContainerUtil.getValueSynchronized(activeSessionManager).canAcquireSession(drmInitData);
    }

    @Override
    public DrmSession<T> acquireSession(Looper playbackLooper, DrmInitData drmInitData) {
        DrmSessionManager<T> sessionManager = OpenContainerUtil.getValueSynchronized(activeSessionManager);
        DrmSession<T> drmSession = sessionManager.acquireSession(playbackLooper, drmInitData);
        return new ManagerAwareDrmSession<>(sessionManager, drmSession);
    }

    @Override
    public void releaseSession(DrmSession<T> drmSession) {
        if(drmSession instanceof ManagerAwareDrmSession) {
            ManagerAwareDrmSession managerAwareDrmSession = (ManagerAwareDrmSession) drmSession;
            managerAwareDrmSession.manager.releaseSession(managerAwareDrmSession.wrapped);
        } else {
            if(drmSession != null) {
                throw new IllegalArgumentException("Unknown session type "+drmSession.getClass());
            }
        }
    }

    public void reset() {
        OpenContainerUtil.setValueSynchronized(activeSessionManager, normalSessionManager, null);
    }

    public void useOfflineManager(byte[] drmKeys) {
        DefaultDrmSessionManager<T> drmSessionManager = sessionManagerFactory.newDrmSessionManager();
        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_QUERY, drmKeys);
        OpenContainerUtil.setValueSynchronized(activeSessionManager, drmSessionManager, null);
    }

    private static class ManagerAwareDrmSession<T extends ExoMediaCrypto> implements DrmSession<T> {
        private final DrmSessionManager<T> manager;
        private final DrmSession<T> wrapped;

        public ManagerAwareDrmSession(DrmSessionManager<T> manager, DrmSession<T> wrapped) {
            this.manager = manager;
            this.wrapped = wrapped;
        }

        @Override
        public int getState() {
            return wrapped.getState();
        }

        @Nullable
        @Override
        public DrmSessionException getError() {
            return wrapped.getError();
        }

        @Nullable
        @Override
        public T getMediaCrypto() {
            return wrapped.getMediaCrypto();
        }

        @Nullable
        @Override
        public Map<String, String> queryKeyStatus() {
            return wrapped.queryKeyStatus();
        }

        @Nullable
        @Override
        public byte[] getOfflineLicenseKeySetId() {
            return wrapped.getOfflineLicenseKeySetId();
        }
    }

    private interface IDrmSessionManagerFactory<T extends ExoMediaCrypto> {
        DefaultDrmSessionManager<T> newDrmSessionManager();
    }
}
