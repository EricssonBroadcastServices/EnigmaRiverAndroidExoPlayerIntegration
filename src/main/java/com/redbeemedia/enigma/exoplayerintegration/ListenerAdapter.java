package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.exoplayerintegration.error.ExoPlayerError;

/*package-protected*/ class ListenerAdapter implements Player.EventListener {
    private IPlayerImplementationListener listener;

    public ListenerAdapter(IPlayerImplementationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        listener.onError(new ExoPlayerError(error));
    }
}
