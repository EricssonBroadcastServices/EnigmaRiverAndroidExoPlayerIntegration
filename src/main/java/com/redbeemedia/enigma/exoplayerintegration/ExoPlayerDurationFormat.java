package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.util.Util;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.time.IDurationFormat;

import java.util.Formatter;
import java.util.Locale;

public class ExoPlayerDurationFormat implements IDurationFormat {
    private StringBuilder formatBuilder = new StringBuilder();
    private Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());

    @Override
    public String format(Duration duration) {
        return Util.getStringForTime(formatBuilder, formatter, duration.inWholeUnits(Duration.Unit.MILLISECONDS));
    }
}
