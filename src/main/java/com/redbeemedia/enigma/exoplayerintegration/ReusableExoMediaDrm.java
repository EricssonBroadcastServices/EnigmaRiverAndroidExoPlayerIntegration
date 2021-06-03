package com.redbeemedia.enigma.exoplayerintegration;

import android.media.DeniedByServerException;
import android.media.MediaCryptoException;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.os.PersistableBundle;

import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;

/*package-protected*/ class ReusableExoMediaDrm implements ExoMediaDrm {
    private final ExoMediaDrmFactory factory;
    private ExoMediaDrm wrapped;

    public ReusableExoMediaDrm(ExoMediaDrmFactory factory) throws UnsupportedDrmException {
        this.factory = factory;
        this.wrapped = factory.create();
    }

    @Override
    public void setOnEventListener(OnEventListener listener) {
        wrapped.setOnEventListener(listener);
    }

    @Override
    public void setOnKeyStatusChangeListener(OnKeyStatusChangeListener listener) {
        wrapped.setOnKeyStatusChangeListener(listener);
    }

    @Override
    public void setOnExpirationUpdateListener(@Nullable OnExpirationUpdateListener listener) {
        wrapped.setOnExpirationUpdateListener(listener);
    }

    @Override
    public byte[] openSession() throws MediaDrmException {
        return wrapped.openSession();
    }

    @Override
    public void closeSession(byte[] sessionId) {
        wrapped.closeSession(sessionId);
    }

    @Override
    public KeyRequest getKeyRequest(byte[] scope, @Nullable List<DrmInitData.SchemeData> schemeDatas, int keyType, @Nullable HashMap<String, String> optionalParameters) throws NotProvisionedException {
        return wrapped.getKeyRequest(scope, schemeDatas, keyType, optionalParameters);
    }

    @Override
    public byte[] provideKeyResponse(byte[] scope, byte[] response) throws NotProvisionedException, DeniedByServerException {
        return wrapped.provideKeyResponse(scope, response);
    }

    @Override
    public ProvisionRequest getProvisionRequest() {
        return wrapped.getProvisionRequest();
    }

    @Override
    public void provideProvisionResponse(byte[] response) throws DeniedByServerException {
        wrapped.provideProvisionResponse(response);
    }

    @Override
    public Map<String, String> queryKeyStatus(byte[] sessionId) {
        return wrapped.queryKeyStatus(sessionId);
    }

    @Override
    public void acquire() {
        wrapped.acquire();
    }

    @Override
    public void release() {
        synchronized (this) {
            if(wrapped != null) {
                wrapped.release();
            }
            wrapped = null;
        }
    }

    public synchronized void revive() {
        if(wrapped == null) {
            try {
                wrapped = factory.create();
            } catch (UnsupportedDrmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void restoreKeys(byte[] sessionId, byte[] keySetId) {
        wrapped.restoreKeys(sessionId, keySetId);
    }

    @Nullable
    @Override
    public PersistableBundle getMetrics() {
        return null;
    }

    @Override
    public String getPropertyString(String propertyName) {
        return wrapped.getPropertyString(propertyName);
    }

    @Override
    public byte[] getPropertyByteArray(String propertyName) {
        return wrapped.getPropertyByteArray(propertyName);
    }

    @Override
    public void setPropertyString(String propertyName, String value) {
        wrapped.setPropertyString(propertyName, value);
    }

    @Override
    public void setPropertyByteArray(String propertyName, byte[] value) {
        wrapped.setPropertyByteArray(propertyName, value);
    }

    @Override
    public ExoMediaCrypto createMediaCrypto(byte[] initData) throws MediaCryptoException {
        return wrapped.createMediaCrypto(initData);
    }

    @Override
    public Class<? extends ExoMediaCrypto> getExoMediaCryptoType() {
        return wrapped.getExoMediaCryptoType();
    }

    public interface ExoMediaDrmFactory {
        ExoMediaDrm create() throws UnsupportedDrmException;
    }
}
