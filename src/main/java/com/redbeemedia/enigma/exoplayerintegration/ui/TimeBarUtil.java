package com.redbeemedia.enigma.exoplayerintegration.ui;

import com.google.android.exoplayer2.ui.TimeBar;
import com.redbeemedia.enigma.core.ads.AdDetector;
import com.redbeemedia.enigma.core.ads.AdIncludedTimeline;
import com.redbeemedia.enigma.core.ads.IAdDetector;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimeline;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.core.virtualui.IVirtualControls;
import com.redbeemedia.enigma.core.virtualui.impl.VirtualControls;

public class TimeBarUtil {

    public static void connect(TimeBar timeBar, IEnigmaPlayer enigmaPlayer, IVirtualControls virtualControls) {
        new TimeBarHelper(timeBar, enigmaPlayer, virtualControls);
    }

    private static class TimeBarHelper {
        private final TimeBar timeBar;
        private final IVirtualControls iVirtualControls;
        private ITimelinePosition currentStartBound;
        private ITimelinePosition currentEndBound;
        private ITimelinePosition currentPosition;
        private ITimeline iTimeline;
        private IEnigmaPlayer enigmaPlayer;

        public TimeBarHelper(TimeBar timeBar, IEnigmaPlayer enigmaPlayer, IVirtualControls virtualControls) {
            this.timeBar = timeBar;
            ITimeline timeline = enigmaPlayer.getTimeline();
            this.enigmaPlayer = enigmaPlayer;
            this.iTimeline = timeline;
            this.iVirtualControls = virtualControls;
            currentStartBound = timeline.getCurrentStartBound();
            currentEndBound = timeline.getCurrentEndBound();
            currentPosition = timeline.getCurrentPosition();
            updateTimeBar();
            timeline.addListener(new BaseTimelineListener() {
                @Override
                public void onCurrentPositionChanged(ITimelinePosition timelinePosition) {
                    currentPosition = timelinePosition;
                    updateTimeBar();
                }

                @Override
                public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                    currentStartBound = start;
                    currentEndBound = end;
                    updateTimeBar();
                }

                @Override
                public void onVisibilityChanged(boolean visible) {
                    AndroidThreadUtil.runOnUiThread(() -> {
                        timeBar.setEnabled(visible);
                    });
                }
            });
            timeBar.addListener(new TimeBar.OnScrubListener() {
                @Override
                public void onScrubStart(TimeBar timeBar, long position) {
                }

                @Override
                public void onScrubMove(TimeBar timeBar, long position) {
                }

                @Override
                public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                    if(!canceled) {
                        AndroidThreadUtil.runOnUiThread(() -> {
                            if(currentStartBound != null) {
                                enigmaPlayer.getControls().seekTo(currentStartBound.add(Duration.millis(position)));
                            } else {
                                enigmaPlayer.getControls().seekTo(position);
                            }
                        });
                    }
                }
            });
        }

        private void updateTimeBar() {
            AndroidThreadUtil.runOnUiThread(() -> {
                if (timeBar instanceof ExoTimebar) {
                    ((ExoTimebar) timeBar).setAllowUpdate(true);
                }
                if (getDurationInMillis() != 0) {
                    timeBar.setDuration(getDurationInMillis());
                }
                if (getPositionInMillis() != 0) {
                    timeBar.setPosition(getPositionInMillis());
                }
                if (timeBar instanceof ExoTimebar) {
                    ((ExoTimebar) timeBar).setAllowUpdate(false);
                }
            });
            hideVirtualControlsWhenAdIsBeingPlayed();
        }

        private void hideVirtualControlsWhenAdIsBeingPlayed() {
            if (iTimeline instanceof AdIncludedTimeline) {
                boolean newEnabled = true;
                // this check if ad is being played or not
                if (((AdIncludedTimeline) iTimeline).getCurrentAdBreak() != null) {
                    newEnabled = false;
                }
                detectIfAdIsFinishedAndJumpToOriginalScrubPosition(newEnabled);
                setEnabledVirtualButtonsWhenStateChange(newEnabled);
            }
        }

        private void detectIfAdIsFinishedAndJumpToOriginalScrubPosition(boolean newEnabled) {
            if (!iVirtualControls.getFastForward().isEnabled() && newEnabled) {
                // at this moment, ad is being finished
                IAdDetector iadDetector = enigmaPlayer.getAdDetector();
                AdDetector adDetector = (AdDetector) iadDetector;
                if (adDetector.getJumpOnOriginalScrubTime() != null) {
                    enigmaPlayer.getControls().seekTo(adDetector.getJumpOnOriginalScrubTime());
                    adDetector.setJumpOnOriginalScrubTime(null);
                }
            }
        }

        private void setEnabledVirtualButtonsWhenStateChange(boolean newEnabled) {
            VirtualControls virtualControls = (VirtualControls) this.iVirtualControls;
            virtualControls.setEnabled(iVirtualControls.getFastForward(), newEnabled);
            virtualControls.setEnabled(iVirtualControls.getRewind(), newEnabled);
            virtualControls.setEnabled(iVirtualControls.getRestart(), newEnabled);
            virtualControls.setEnabled(iVirtualControls.getNextProgram(), newEnabled);
            virtualControls.setEnabled(iVirtualControls.getPreviousProgram(), newEnabled);
            virtualControls.setEnabled(iVirtualControls.getSeekBar(), newEnabled);
        }

        private long getDurationInMillis() {
            if(currentEndBound != null && currentStartBound != null) {
                return currentEndBound.subtract(currentStartBound).inWholeUnits(Duration.Unit.MILLISECONDS);
            } else {
                return 0L;
            }
        }

        private long getPositionInMillis() {
            if(currentPosition != null && currentStartBound != null) {
                return currentPosition.subtract(currentStartBound).inWholeUnits(Duration.Unit.MILLISECONDS);
            } else {
                return 0L;
            }
        }
    }
}
