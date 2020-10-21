package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;
import com.redbeemedia.enigma.core.video.IVideoTrack;

import java.util.Arrays;

public class ExoVideoTrack extends BasePlayerImplementationTrack implements IVideoTrack {
    private final int bitrateInBytesPerSecond;
    private final int width;
    private final int height;

    public ExoVideoTrack(Format format) {
        this.bitrateInBytesPerSecond = convertInt(format.bitrate);
        this.width = convertInt(format.width);
        this.height = convertInt(format.height);
    }

    private static int convertInt(int integer) {
        if(integer == Format.NO_VALUE) {
            return -1; // Format.NO_VALUE is -1, but this is a more explicit bridge between the member contracts.
        } else {
            return integer;
        }
    }

    @Override
    public int getBitrate() {
        return bitrateInBytesPerSecond;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{bitrateInBytesPerSecond, width, height});
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof ExoVideoTrack) {
            ExoVideoTrack other = (ExoVideoTrack) obj;
            return this.bitrateInBytesPerSecond == other.bitrateInBytesPerSecond
                && this.width == other.width
                && this.height == other.height;
        } else {
            return false;
        }
    }
}
