package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimeline;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;

public class NextLogic extends AbstractButtonLogic {
    private ITimelinePosition currentEndPosition;

    public NextLogic(IEnigmaPlayer enigmaPlayer) {
        super(enigmaPlayer);
        ITimeline timeline = enigmaPlayer.getTimeline();
        currentEndPosition = timeline.getCurrentEndBound();
        timeline.addListener(new BaseTimelineListener() {
            @Override
            public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                currentEndPosition = end;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onActivate() {
        getEnigmaPlayer().getControls().seekTo(currentEndPosition);
    }
}
