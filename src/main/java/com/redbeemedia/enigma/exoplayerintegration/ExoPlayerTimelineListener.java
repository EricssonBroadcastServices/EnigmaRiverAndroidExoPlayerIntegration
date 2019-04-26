package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.core.player.ITimelinePositionFactory;

public class ExoPlayerTimelineListener implements Player.EventListener {
    private Player player;
    private IPlayerImplementationListener listener;
    private ITimelinePositionFactory timelinePositionFactory;

    public ExoPlayerTimelineListener(Player player, IPlayerImplementationListener listener, ITimelinePositionFactory timelinePositionFactory) {
        this.player = player;
        this.listener = listener;
        this.timelinePositionFactory = timelinePositionFactory;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
        if(timeline.getWindowCount() == 0) {
            listener.onTimelineBoundsChanged(null, null);
        } else {
            long duration = TimelineUtil.getDurationMs(player, timeline, player.getCurrentWindowIndex());
            long start = TimelineUtil.getStartMs(player, timeline, player.getCurrentWindowIndex());
            listener.onTimelineBoundsChanged(timelinePositionFactory.newPosition(start), timelinePositionFactory.newPosition(duration));
        }
    }
}
