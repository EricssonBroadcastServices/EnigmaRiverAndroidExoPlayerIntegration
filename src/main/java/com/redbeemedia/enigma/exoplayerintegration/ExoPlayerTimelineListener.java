package com.redbeemedia.enigma.exoplayerintegration;

import android.os.AsyncTask;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest;
import com.redbeemedia.enigma.core.format.EnigmaMediaFormat;
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
    public void onTimelineChanged(Timeline timeline, int reason) {
        try {
            long duration = AndroidThreadUtil.getBlockingOnUiThread(() -> player.getDuration());
            if (timeline.getWindowCount() == 0 || duration == C.TIME_UNSET) {
                listener.onTimelineBoundsChanged(null, null);
            } else {
                listener.onTimelineBoundsChanged(timelinePositionFactory.newPosition(0), timelinePositionFactory.newPosition(duration));
            }
        } catch (InterruptedException e) { throw new RuntimeException((e)); }

        Object manifestReference = player.getCurrentManifest();

        if (manifestReference != null) {
            if (manifestReference instanceof DashManifest) {
                // Not implemented
            } else if (manifestReference instanceof HlsManifest) {
                HlsManifest manifest = (HlsManifest)manifestReference;
                AsyncTask.execute(() -> {
                    listener.onManifestChanged(manifest.mediaPlaylist.baseUri, EnigmaMediaFormat.StreamFormat.HLS, manifest.mediaPlaylist.startTimeUs / 1000L);;
                });
            } else if (manifestReference instanceof SsManifest) {
                // Not implemented
            }
        }
    }

    @Override
    public void onSeekProcessed() {
        listener.onPositionChanged();
    }
}
