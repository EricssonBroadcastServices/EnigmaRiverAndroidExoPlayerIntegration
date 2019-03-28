package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.ExoPlayer;
import com.redbeemedia.enigma.core.player.IPlayerImplementationControls;

/*package-protected*/ class ExoPlayerPosition implements IPlayerImplementationControls.ISeekPosition {
    private int windowIndex;
    private long positionMs;

    public ExoPlayerPosition(int windowIndex, long positionMs) {
        this.windowIndex = windowIndex;
        this.positionMs = positionMs;
    }

    public void seek(ExoPlayer player) {
        player.seekTo(windowIndex, positionMs);
    }

    public static ExoPlayerPosition getCurrent(ExoPlayer exoPlayer) {
        return new ExoPlayerPosition(exoPlayer.getCurrentWindowIndex(), exoPlayer.getCurrentPosition());
    }
}
