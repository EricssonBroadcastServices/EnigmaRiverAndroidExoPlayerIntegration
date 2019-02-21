package com.redbeemedia.enigma.exoplayerintegration.error;

import com.google.android.exoplayer2.ExoPlaybackException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InternalExoPlayerErrorCodes {
    // List of EXCEPTIONS
    private static final Map<String,Integer> lookupTable;
    static {
        Map<String,Integer> lookupTableBuild = new HashMap<>();
        // GENERAL
        //
        // [ 100 ... 199 ]
        lookupTableBuild.put("unhandledformatexception",100); //UNHANDLED_FORMAT_EXCEPTION
        lookupTableBuild.put("configurationexception",101); //CONFIGURATION_EXCEPTION
        lookupTableBuild.put("initializationexception",102); //INITIALIZATION_EXCEPTION
        lookupTableBuild.put("writeexception",103); //WRITE_EXCEPTION
        lookupTableBuild.put("missingschemedataexception",104); //MISSING_SCHEME_DATA_EXCEPTION
        lookupTableBuild.put("invalidaudiotracktimestampexception",105); //INVALID_AUDIO_TRACK_TIMESTAMP_EXCEPTION
        lookupTableBuild.put("glexception",106); //GL_EXCEPTION
        lookupTableBuild.put("runtimeexception",107); //RUNTIME_EXCEPTION
        lookupTableBuild.put("cachedatasinkexception",108); //CACHE_DATA_SINK_EXCEPTION
        lookupTableBuild.put("cacheexception",109); //CACHE_EXCEPTION
        lookupTableBuild.put("illegalseekpositionexception",110); //ILLEGAL_SEEK_POSITION_EXCEPTION
        lookupTableBuild.put("illegalstateexception",111); //ILLEGAL_STATE_EXCEPTION
        lookupTableBuild.put("illegalargumentexception",112); //ILLEGAL_ARGUMENT_EXCEPTION
        lookupTableBuild.put("eofexception",113); //EOF_EXCEPTION

        // IOException related
        //
        // [ 200 ... 299 ]
        lookupTableBuild.put("interruptedioexception",200); //INTERRUPTED_IO_EXCEPTION
        lookupTableBuild.put("parserexception",201); //PARSER_EXCEPTION
        lookupTableBuild.put("downloadexception",202); //DOWNLOAD_EXCEPTION
        lookupTableBuild.put("behindlivewindowexception",203); //BEHIND_LIVE_WINDOW_EXCEPTION
        lookupTableBuild.put("illegalclippingexception",204); //ILLEGAL_CLIPPING_EXCEPTION
        lookupTableBuild.put("illegalmergeexception",205); //ILLEGAL_MERGE_EXCEPTION
        lookupTableBuild.put("adloadexception",206); //AD_LOAD_EXCEPTION
        lookupTableBuild.put("assetdatasourceexception",207); //ASSET_DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("contentdatasourceexception",208); //CONTENT_DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("datasourceexception",209); //DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("filedatasourceexception",210); //FILE_DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("httpdatasourceexception",211); //HTTP_DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("udpdatasourceexception",212); //UDP_DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("unexpectedloaderexception",213); //UNEXPECTED_LOADER_EXCEPTION
        lookupTableBuild.put("rawresourcedatasourceexception",214); //RAW_RESOURCE_DATA_SOURCE_EXCEPTION
        lookupTableBuild.put("prioritytoolowexception",215); //PRIORITY_TOO_LOW_EXCEPTION
        lookupTableBuild.put("dashmanifeststaleexception",216); //DASH_MANIFEST_STALE_EXCEPTION
        lookupTableBuild.put("samplequeuemappingexception",217); //SAMPLE_QUEUE_MAPPING_EXCEPTION
        lookupTableBuild.put("playliststuckexception",218); //PLAYLIST_STUCK_EXCEPTION
        lookupTableBuild.put("playlistresetexception",219); //PLAYLIST_RESET_EXCEPTION
        lookupTableBuild.put("simulatedioexception",220); //SIMULATED_IO_EXCEPTION
        lookupTableBuild.put("invalidcontenttypeexception",221); //INVALID_CONTENT_TYPE_EXCEPTION
        lookupTableBuild.put("invalidresponsecodeexception",222); //INVALID_RESPONSE_CODE_EXCEPTION
        lookupTableBuild.put("openexception",223); //OPEN_EXCEPTION
        lookupTableBuild.put("unsupportedformatexception",224); //UNSUPPORTED_FORMAT_EXCEPTION
        lookupTableBuild.put("unhandlededitlistexception",225); //UNHANDLED_EDIT_LIST_EXCEPTION
        lookupTableBuild.put("unrecognizedinputformatexception",226); //UNRECOGNIZED_INPUT_FORMAT_EXCEPTION
        lookupTableBuild.put("missingfieldexception",227); //MISSING_FIELD_EXCEPTION

        // DRM related
        //
        // [ 300 ... 399 ]
        lookupTableBuild.put("cryptoexception",300); //CRYPTO_EXCEPTION
        lookupTableBuild.put("deniedbyserverexception",301); //DENIED_BY_SERVER_EXCEPTION
        lookupTableBuild.put("mediadrmexception",302); //MEDIA_DRM_EXCEPTION
        lookupTableBuild.put("notprovisionedexception",303); //NOT_PROVISIONED_EXCEPTION
        lookupTableBuild.put("unsupportedschemeexception",304); //UNSUPPORTED_SCHEME_EXCEPTION
        lookupTableBuild.put("mediacryptoexception",305); //MEDIA_CRYPTO_EXCEPTION
        lookupTableBuild.put("drmsessionexception",306); //DRM_SESSION_EXCEPTION
        lookupTableBuild.put("decryptionexception",307); //DECRYPTION_EXCEPTION
        lookupTableBuild.put("keysexpiredexception",308); //KEYS_EXPIRED_EXCEPTION
        lookupTableBuild.put("unsupporteddrmexception",309); //UNSUPPORTED_DRM_EXCEPTION
        lookupTableBuild.put("mediadrmstateexception",310); //MEDIA_DRM_STATE_EXCEPTION

        // DECODER related
        //
        // [ 400 ... 499 ]
        lookupTableBuild.put("ffmpegdecoderexception",400); //FFMPEG_DECODER_EXCEPTION
        lookupTableBuild.put("flacdecoderexception",401); //FLAC_DECODER_EXCEPTION
        lookupTableBuild.put("opusdecoderexception",402); //OPUS_DECODER_EXCEPTION
        lookupTableBuild.put("decoderinitializationexception",403); //DECODER_INITIALIZATION_EXCEPTION
        lookupTableBuild.put("decoderqueryexception",404); //DECODER_QUERY_EXCEPTION
        lookupTableBuild.put("metadatadecoderexception",405); //METADATA_DECODER_EXCEPTION
        lookupTableBuild.put("subtitledecoderexception",406); //SUBTITLE_DECODER_EXCEPTION
        lookupTableBuild.put("audiodecoderexception",407); //AUDIO_DECODER_EXCEPTION
        lookupTableBuild.put("flacframedecodeexception",408); //FLAC_FRAME_DECODE_EXCEPTION
        lookupTableBuild.put("vpxdecoderexception",409); //VPX_DECODER_EXCEPTION
        lookupTableBuild.put("codecexception",410); //CODEC_EXCEPTION

        lookupTable = Collections.unmodifiableMap(lookupTableBuild);
    }

    public static int getInternalErrorCode(ExoPlaybackException error, int defaultErrorCode) {
        switch (error.type) {
            case ExoPlaybackException.TYPE_SOURCE:
                return getErrorCodeFromName(normalizedName(error.getSourceException()), defaultErrorCode);
            case ExoPlaybackException.TYPE_RENDERER:
                return getErrorCodeFromName(normalizedName(error.getRendererException()), defaultErrorCode);
            case ExoPlaybackException.TYPE_UNEXPECTED:
                return getErrorCodeFromName(normalizedName(error.getUnexpectedException()), defaultErrorCode);
            default:
                return defaultErrorCode;
        }
    }

    private static String normalizedName(Exception e) {
        return e.getClass().getSimpleName().toLowerCase(Locale.ENGLISH);
    }

    private static int getErrorCodeFromName(String exceptionName, int fallback) {
        Integer code = lookupTable.get(exceptionName);
        return code != null ? code.intValue() : fallback;
    }
}
