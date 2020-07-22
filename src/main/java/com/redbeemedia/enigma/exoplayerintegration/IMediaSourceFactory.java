package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.source.MediaSource;
import com.redbeemedia.enigma.exoplayerintegration.util.MediaSourceFactoryConfigurator;

public interface IMediaSourceFactory {
    MediaSource createMediaSource(MediaSourceFactoryConfigurator configurator);
}
