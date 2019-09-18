package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.restriction.IContractRestriction;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.exoplayerintegration.util.ContractRestrictionsTracker;

public abstract class AbstractSeekStepLogic extends AbstractButtonLogic {
    public static final Duration DEFAULT_SEEK_STEP = Duration.seconds(15);

    private boolean enabled = true;

    public AbstractSeekStepLogic(IEnigmaPlayer enigmaPlayer, IContractRestriction<Boolean> contractRestriction, boolean enabledFallback) {
        super(enigmaPlayer);

        ContractRestrictionsTracker contractRestrictionsTracker = new ContractRestrictionsTracker();
        contractRestrictionsTracker.addValueChangedListener((oldValue, newValue) -> {
            if(newValue != null) {
                enabled = newValue.getValue(contractRestriction, enabledFallback);
            } else {
                enabled = enabledFallback;
            }
            updateButtonGraphics();
        });
        contractRestrictionsTracker.connectTo(enigmaPlayer);
    }

    @Override
    public void onActivate() {
        ITimelinePosition currentPosition = getEnigmaPlayer().getTimeline().getCurrentPosition();
        if(currentPosition != null) {
            getEnigmaPlayer().getControls().seekTo(getSeekPosition(currentPosition));
        }
    }

    protected abstract ITimelinePosition getSeekPosition(ITimelinePosition currentPosition);

    public boolean isEnabled() {
        return enabled;
    }
}
