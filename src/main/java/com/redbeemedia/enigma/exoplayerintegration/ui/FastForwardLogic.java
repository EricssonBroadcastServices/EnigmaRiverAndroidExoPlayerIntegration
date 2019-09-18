package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.restriction.ContractRestriction;
import com.redbeemedia.enigma.core.time.Duration;

public class FastForwardLogic extends AbstractSeekStepLogic {
    private Duration seekStep;

    public FastForwardLogic(IEnigmaPlayer enigmaPlayer) {
        super(enigmaPlayer, ContractRestriction.FASTFORWARD_ENABLED, true);
        this.seekStep = DEFAULT_SEEK_STEP;
    }

    @Override
    protected ITimelinePosition getSeekPosition(ITimelinePosition currentPosition) {
        return currentPosition.add(seekStep);
    }
}
