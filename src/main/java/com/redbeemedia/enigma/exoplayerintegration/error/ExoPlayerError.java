package com.redbeemedia.enigma.exoplayerintegration.error;

import com.google.android.exoplayer2.PlaybackException;
import com.redbeemedia.enigma.core.error.PlayerImplementationError;

import java.io.IOException;
import java.io.Writer;

public class ExoPlayerError extends PlayerImplementationError  {
    private static final String INTERNAL_ERROR_CODE_FIELD_NAME = "ExoPlayerErrorCode";

    private final PlaybackException exoPlaybackException;

    public ExoPlayerError(PlaybackException exoPlaybackException) {
        super(internalCode(exoPlaybackException), INTERNAL_ERROR_CODE_FIELD_NAME, exoPlaybackException.getMessage());
        this.exoPlaybackException = exoPlaybackException;
    }

    public PlaybackException getExoPlaybackException() {
        return exoPlaybackException;
    }

    private static int internalCode(PlaybackException e) {
        return InternalExoPlayerErrorCodes.getInternalErrorCode(e,1);
    }

    @Override
    public void writeTrace(Writer writer) throws IOException {
        super.writeTrace(writer);
        addExceptionStackTrace(writer, exoPlaybackException);
    }
}
