package com.redbeemedia.enigma.exoplayerintegration;

import com.redbeemedia.enigma.core.format.EnigmaMediaFormat;
import com.redbeemedia.enigma.core.format.IMediaFormatSupportSpec;

import java.util.HashSet;
import java.util.Set;

/*package-protected*/ class MediaFormatSpecification implements IMediaFormatSupportSpec {
    private final Set<EnigmaMediaFormat> supportedFormats = new HashSet<>();

    @Override
    public boolean supports(EnigmaMediaFormat enigmaMediaFormat) {
        return supportedFormats.contains(enigmaMediaFormat);
    }

    public void add(EnigmaMediaFormat mediaFormat) {
        supportedFormats.add(mediaFormat);
    }
}
