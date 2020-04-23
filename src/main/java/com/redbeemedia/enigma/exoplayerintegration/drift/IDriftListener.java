package com.redbeemedia.enigma.exoplayerintegration.drift;

import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.util.IInternalListener;

public interface IDriftListener extends IInternalListener {
    /**
     * @param speedHandler Used to update playback speed
     * @param drift A positive drift indicates that the playback position is lagging behind and
     *              the playback speed should typically be increased
     */
    void onDriftUpdated(ISpeedHandler speedHandler, Duration drift);
}
