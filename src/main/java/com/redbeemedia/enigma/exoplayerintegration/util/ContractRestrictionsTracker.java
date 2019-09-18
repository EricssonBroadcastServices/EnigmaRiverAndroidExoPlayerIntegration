package com.redbeemedia.enigma.exoplayerintegration.util;

import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.restriction.IContractRestrictions;
import com.redbeemedia.enigma.core.util.IValueChangedListener;
import com.redbeemedia.enigma.core.util.OpenContainer;
import com.redbeemedia.enigma.core.util.OpenContainerUtil;
import com.redbeemedia.enigma.core.util.ValueChangedCollector;

public class ContractRestrictionsTracker {
    private final ValueChangedCollector<IContractRestrictions> valueChangedCollector = new ValueChangedCollector<>();
    private final OpenContainer<IContractRestrictions> contractRestrictions = new OpenContainer<>(null);
    private final CurrentPlaybackSessionListener listener = new CurrentPlaybackSessionListener() {
        @Override
        public void onNewPlaybackSession(IPlaybackSession playbackSession) {
            OpenContainerUtil.setValueSynchronized(contractRestrictions, playbackSession.getContractRestrictions(), valueChangedCollector);
        }

        @Override
        public void onContractRestrictionsChanged(IContractRestrictions oldContractRestrictions, IContractRestrictions newContractRestrictions) {
            OpenContainerUtil.setValueSynchronized(contractRestrictions, newContractRestrictions, valueChangedCollector);
        }
    };;

    public IContractRestrictions getContractRestrictions() {
        return OpenContainerUtil.getValueSynchronized(contractRestrictions);
    }

    public void addValueChangedListener(IValueChangedListener<IContractRestrictions> listener) {
        valueChangedCollector.addListener(listener);
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer) {
        listener.connectTo(enigmaPlayer);
    }
}
