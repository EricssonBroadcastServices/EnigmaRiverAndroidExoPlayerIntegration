package com.redbeemedia.enigma.exoplayerintegration.error;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.redbeemedia.enigma.core.error.PlayerImplementationError;

import java.io.IOException;
import java.io.Writer;

public class ExoPlayerError extends PlayerImplementationError  {
    private static final String INTERNAL_ERROR_CODE_FIELD_NAME = "ExoPlayerErrorCode";

    private final ExoPlaybackException exoPlaybackException;

    public ExoPlayerError(ExoPlaybackException exoPlaybackException) {
        super(internalCode(exoPlaybackException), INTERNAL_ERROR_CODE_FIELD_NAME);
        this.exoPlaybackException = exoPlaybackException;
    }

    public ExoPlaybackException getExoPlaybackException() {
        return exoPlaybackException;
    }

    private static int internalCode(ExoPlaybackException e) {
        return InternalExoPlayerErrorCodes.getInternalErrorCode(e,1);
    }

    @Override
    public void writeTrace(Writer writer) throws IOException {
        super.writeTrace(writer);
        addExceptionStackTrace(writer, exoPlaybackException);
    }
}
