package com.redbeemedia.enigma.exoplayerintegration.util;

import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;

public class CurrentPlaybackSessionListener extends BasePlaybackSessionListener {

    public void onNewPlaybackSession(IPlaybackSession playbackSession) {
    }

    public final void connectTo(IEnigmaPlayer enigmaPlayer) {
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(from != null) {
                    from.removeListener(CurrentPlaybackSessionListener.this);
                }
                if(to != null) {
                    to.addListener(CurrentPlaybackSessionListener.this);
                    onNewPlaybackSession(to);
                }
            }
        });
    }
}
