package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.exoplayerintegration.error.ExoPlayerError;

/*package-protected*/ class ExoPlayerListener implements Player.EventListener {
    private IPlayerImplementationListener listener;
    private int lastState = Player.STATE_IDLE;

    public ExoPlayerListener(IPlayerImplementationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        listener.onError(new ExoPlayerError(error));
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if(playbackState == Player.STATE_READY) {
            if(lastState != Player.STATE_READY) {
                listener.onLoadCompleted();
            }
            if(playWhenReady) {
                listener.onPlaybackStarted();
            } else {
                listener.onPlaybackPaused();
            }
        } else if(playbackState == Player.STATE_ENDED) {
            listener.onStreamEnded();
        }
        this.lastState = playbackState;
    }
}
