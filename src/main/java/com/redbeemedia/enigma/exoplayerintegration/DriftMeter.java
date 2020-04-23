package com.redbeemedia.enigma.exoplayerintegration;

import android.os.Handler;
import android.os.SystemClock;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.IEnigmaPlayerEnvironment;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.util.Collector;
import com.redbeemedia.enigma.core.util.HandlerWrapper;
import com.redbeemedia.enigma.exoplayerintegration.drift.IDriftListener;
import com.redbeemedia.enigma.exoplayerintegration.drift.ISpeedHandler;

/*package-protected*/ class DriftMeter implements IEnigmaPlayerEnvironment.IEnigmaPlayerReadyListener  {
    private final ExoPlayer player;
    private final Handler handler;
    private final Timeline.Window window = new Timeline.Window();
    private final ListenerCollector listeners = new ListenerCollector();

    private long offsetMs = C.TIME_UNSET;

    public DriftMeter(ExoPlayer player, Handler handler) {
        this.player = player;
        this.handler = handler;
    }

    public boolean markReferencePoint() {
        long startTimeMs = getTimeMs();

        long positionMs;
        synchronized (window) {
            Timeline.Window currentWindow = player.getCurrentTimeline().getWindow(player.getCurrentWindowIndex(), window);
            if(!currentWindow.isDynamic) {
                return false;
            }
            if(currentWindow.windowStartTimeMs == C.TIME_UNSET) {
                return false;
            }
            positionMs = currentWindow.windowStartTimeMs + player.getCurrentPosition();
        }
        offsetMs = positionMs-startTimeMs;
        return true;
    }

    public void clearReferencePoint() {
        offsetMs = C.TIME_UNSET;
    }

    public boolean isActive() {
        return offsetMs != C.TIME_UNSET;
    }

    public long getTargetPositionMs() {
        return offsetMs + getTimeMs();
    }

    public long getCurrentPositionMs() {
        long windowStartTimeMs;
        synchronized (window) {
            windowStartTimeMs = player.getCurrentTimeline().getWindow(player.getCurrentWindowIndex(), window).windowStartTimeMs;
        }
        if(windowStartTimeMs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        return windowStartTimeMs + player.getCurrentPosition();
    }

    public long getDrift() {
        if(isActive()) {
            long currentPositionMs = getCurrentPositionMs();
            if(currentPositionMs == C.TIME_UNSET) {
                return 0;
            } else {
                return getTargetPositionMs()-currentPositionMs;
            }
        } else {
            return 0;
        }
    }

    protected long getTimeMs() {
        return SystemClock.uptimeMillis();
    }

    @Override
    public void onReady(IEnigmaPlayer enigmaPlayer) {
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(to != null) {
                    DriftMeter.this.clearReferencePoint();
                    if(enigmaPlayer.getState() == EnigmaPlayerState.PLAYING) {
                        updateReferencePoint(enigmaPlayer);
                    } else {
                        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
                            @Override
                            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                                if(to == EnigmaPlayerState.PLAYING) {
                                    updateReferencePoint(enigmaPlayer);
                                    enigmaPlayer.removeListener(this);
                                }
                            }
                        }, handler);
                    }
                }
            }
        }, handler);
        enigmaPlayer.getTimeline().addListener(new BaseTimelineListener() {
            @Override
            public void onCurrentPositionChanged(ITimelinePosition timelinePosition) {
                long driftMs = DriftMeter.this.getDrift();
                Duration drift = Duration.millis(driftMs);

                listeners.onDriftUpdated(speed -> {
                    PlaybackParameters currentPlaybackParameters = player.getPlaybackParameters();
                    player.setPlaybackParameters(new PlaybackParameters(speed, currentPlaybackParameters.pitch, currentPlaybackParameters.skipSilence));
                }, drift);
            }
        }, handler);
    }

    private void updateReferencePoint(IEnigmaPlayer enigmaPlayer) {
        if(player.isCurrentWindowDynamic()) {
            if(!markReferencePoint()) {
                enigmaPlayer.getTimeline().addListener(new BaseTimelineListener() {
                    @Override
                    public void onCurrentPositionChanged(ITimelinePosition timelinePosition) {
                        DriftMeter.this.markReferencePoint();
                        enigmaPlayer.getTimeline().removeListener(this);
                    }
                }, handler);
            }
        }
    }

    public boolean addDriftListener(IDriftListener driftListener) {
        return listeners.addListener(driftListener, new HandlerWrapper(handler));
    }

    public boolean removeDriftListener(IDriftListener driftListener) {
        return listeners.removeListener(driftListener);
    }

    private static class ListenerCollector extends Collector<IDriftListener> implements IDriftListener {
        public ListenerCollector() {
            super(IDriftListener.class);
        }

        @Override
        public void onDriftUpdated(ISpeedHandler speedHandler, Duration drift) {
            forEach(listener -> listener.onDriftUpdated(speedHandler, drift));
        }
    }
}
