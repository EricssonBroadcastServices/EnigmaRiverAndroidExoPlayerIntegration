package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimeline;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;

public class PreviousLogic extends AbstractButtonLogic {
    private ITimelinePosition currentStartPosition;

    public PreviousLogic(IEnigmaPlayer enigmaPlayer) {
        super(enigmaPlayer);
        ITimeline timeline = enigmaPlayer.getTimeline();
        currentStartPosition = timeline.getCurrentStartBound();
        timeline.addListener(new BaseTimelineListener() {
            @Override
            public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                currentStartPosition = start;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onActivate() {
        getEnigmaPlayer().getControls().seekTo(currentStartPosition);
    }
}
