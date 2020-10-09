package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.redbeemedia.enigma.core.player.IPlayerImplementationListener;
import com.redbeemedia.enigma.core.player.ITimelinePositionFactory;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

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
        try {
            long duration = AndroidThreadUtil.getBlockingOnUiThread(() -> player.getDuration());
            if (timeline.getWindowCount() == 0 || duration == C.TIME_UNSET) {
                listener.onTimelineBoundsChanged(null, null);
            } else {
                listener.onTimelineBoundsChanged(timelinePositionFactory.newPosition(0), timelinePositionFactory.newPosition(duration));
            }
        } catch (InterruptedException e) { throw new RuntimeException((e)); }
    }

    @Override
    public void onSeekProcessed() {
        listener.onPositionChanged();
    }
}
