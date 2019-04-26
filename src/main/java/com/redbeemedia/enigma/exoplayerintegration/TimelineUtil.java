package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;

/*package-protected*/ class TimelineUtil {
    private static Timeline.Window reusableWindow = new Timeline.Window();

    public static long getDurationMs(Player player, Timeline timeline, int windowIndex) {
        synchronized (reusableWindow) {
            timeline.getWindow(windowIndex, reusableWindow);
            return reusableWindow.durationUs / 1000;
        }
    }

    public static long getStartMs(Player player, Timeline timeline, int windowIndex) {
        return 0;
    }
}
