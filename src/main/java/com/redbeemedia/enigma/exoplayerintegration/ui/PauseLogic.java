package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.restriction.ContractRestriction;
import com.redbeemedia.enigma.core.restriction.IContractRestrictions;
import com.redbeemedia.enigma.exoplayerintegration.util.ContractRestrictionsTracker;

public class PauseLogic extends AbstractButtonLogic {
    private boolean enabled = true;
    private EnigmaPlayerState playerState;

    public PauseLogic(IEnigmaPlayer enigmaPlayer) {
        super(enigmaPlayer);
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                super.onPlaybackSessionChanged(from, to);
            }
        });
        ContractRestrictionsTracker contractRestrictionsTracker = new ContractRestrictionsTracker();
        updateEnabled(contractRestrictionsTracker.getContractRestrictions());
        contractRestrictionsTracker.addValueChangedListener((oldValue, newValue) -> updateEnabled(newValue));
        contractRestrictionsTracker.connectTo(enigmaPlayer);
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

    private void updateEnabled(IContractRestrictions contractRestrictions) {
        if(contractRestrictions != null) {
            enabled = contractRestrictions.getValue(ContractRestriction.TIMESHIFT_ENABLED, true);
            updateButtonGraphics();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && (playerState == EnigmaPlayerState.PLAYING);
    }

    @Override
    public void onActivate() {
        getEnigmaPlayer().getControls().pause();
    }
}
