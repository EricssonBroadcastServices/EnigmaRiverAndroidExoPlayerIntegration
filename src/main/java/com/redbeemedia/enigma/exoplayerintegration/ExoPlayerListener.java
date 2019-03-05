package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.exoplayerintegration.error.ExoPlayerError;

/*package-protected*/ class ExoPlayerListener implements Player.EventListener {
    private IPlayerImplementationListener listener;

    public ExoPlayerListener(IPlayerImplementationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        listener.onError(new ExoPlayerError(error));
    }
}
