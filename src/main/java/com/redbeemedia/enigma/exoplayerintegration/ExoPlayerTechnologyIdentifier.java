package com.redbeemedia.enigma.exoplayerintegration;

import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.redbeemedia.enigma.core.player.IPlaybackTechnologyIdentifier;

/*package-protected*/ class ExoPlayerTechnologyIdentifier implements IPlaybackTechnologyIdentifier {
    private static final ExoPlayerTechnologyIdentifier instance = new ExoPlayerTechnologyIdentifier();

    @Override
    public String getTechnologyName() {
        return ExoPlayerLibraryInfo.TAG;
    }

    @Override
    public String getTechnologyVersion() {
        return ExoPlayerLibraryInfo.VERSION;
    }


    public static ExoPlayerTechnologyIdentifier get() {
        return instance;
    }
}
