package com.redbeemedia.enigma.exoplayerintegration;

import java.util.ArrayList;
import java.util.List;

/*package-protected*/ class Activation implements IActivation {
    private boolean active = false;
    private List<Runnable> queued = new ArrayList<>();

    @Override
    public void whenActive(Runnable runnable) {
        boolean runNow = false;
        synchronized (this) {
            if(active) {
                runNow = true;
            } else {
                queued.add(runnable);
            }
        }
        if(runNow) {
            runnable.run();
        }
    }

    @Override
    public synchronized void activate() {
        active = true;
        RuntimeException exception = null;
        for(Runnable runnable : queued) {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                if(exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        queued.clear();
        queued = null;
        if(exception != null) {
            throw exception;
        }
    }
}
