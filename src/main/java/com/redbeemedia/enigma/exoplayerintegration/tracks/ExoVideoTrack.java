package com.redbeemedia.enigma.exoplayerintegration.tracks;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.redbeemedia.enigma.core.player.track.BasePlayerImplementationTrack;
import com.redbeemedia.enigma.core.video.IVideoTrack;

public class ExoVideoTrack extends BasePlayerImplementationTrack implements IVideoTrack {
    private final int bitrateInBytesPerSecond;

    public ExoVideoTrack(Format format) {
        this.bitrateInBytesPerSecond = convertBitrate(format.bitrate);
    }

    private static int convertBitrate(int bitrate) {
        if(bitrate == Format.NO_VALUE) {
            return -1; // Format.NO_VALUE is -1, but this is a more explicit bridge between the member contracts.
        } else {
            return bitrate;
        }
    }

    @Override
    public int getBitrate() {
        return bitrateInBytesPerSecond;
    }

    @Override
    public int hashCode() {
        return bitrateInBytesPerSecond;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof ExoVideoTrack) {
            ExoVideoTrack other = (ExoVideoTrack) obj;
            return this.bitrateInBytesPerSecond == other.bitrateInBytesPerSecond;
        } else {
            return false;
        }
    }
}
