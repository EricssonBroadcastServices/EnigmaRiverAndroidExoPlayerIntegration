package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;

public class PlayLogic extends AbstractButtonLogic {
    private EnigmaPlayerState playerState;

    public PlayLogic(IEnigmaPlayer enigmaPlayer) {
        super(enigmaPlayer);
        this.playerState = enigmaPlayer.getState();
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                playerState = to;
                updateButtonGraphics();
            }
        });
        updateButtonGraphics();
    }

    @Override
    public boolean isEnabled() {
        return playerState != EnigmaPlayerState.PLAYING;
    }

    @Override
    public void onActivate() {
        getEnigmaPlayer().getControls().start();
    }
}
