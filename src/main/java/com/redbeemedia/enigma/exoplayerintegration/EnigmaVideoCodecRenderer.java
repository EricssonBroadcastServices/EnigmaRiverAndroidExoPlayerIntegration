package com.redbeemedia.enigma.exoplayerintegration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class EnigmaVideoCodecRenderer extends MediaCodecVideoRenderer {
    public EnigmaVideoCodecRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        super(context, mediaCodecSelector);
    }

    public EnigmaVideoCodecRenderer(
            Context context,
            MediaCodecAdapter.Factory codecAdapterFactory,
            MediaCodecSelector mediaCodecSelector,
            long allowedJoiningTimeMs,
            boolean enableDecoderFallback,
            @Nullable Handler eventHandler,
            @Nullable VideoRendererEventListener eventListener,
            int maxDroppedFramesToNotify) {

        super(
                context,
                codecAdapterFactory,
                mediaCodecSelector,
                allowedJoiningTimeMs,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                maxDroppedFramesToNotify,
                /* assumedMinimumCodecOperatingRate= */ 30);
    }


    @Override
    protected @Capabilities int supportsFormat(MediaCodecSelector mediaCodecSelector, Format format)
            throws MediaCodecUtil.DecoderQueryException {
        String mimeType = format.sampleMimeType;
        if (!MimeTypes.isVideo(mimeType)) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE);
        }
        @Nullable DrmInitData drmInitData = format.drmInitData;
        // Assume encrypted content requires secure decoders.
        boolean requiresSecureDecryption = drmInitData != null;
        // Done by us, because secure awesome decoder has issue on MiBox4
        boolean requiresSecureDecoder = false;
        List<MediaCodecInfo> decoderInfos =
                getDecoderInfos(
                        mediaCodecSelector,
                        format,
                        requiresSecureDecoder, false);

        if (requiresSecureDecryption && decoderInfos.isEmpty()) {
            // No secure decoders are available. Fall back to non-secure decoders.
            decoderInfos =
                    getDecoderInfos(
                            mediaCodecSelector,
                            format,
                            false,
                            false);
        }
        if (decoderInfos.isEmpty()) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE);
        }
        if (!supportsFormatDrm(format)) {
            return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_DRM);
        }
        // Check whether the first decoder supports the format. This is the preferred decoder for the
        // format's MIME type, according to the MediaCodecSelector.
        MediaCodecInfo decoderInfo = decoderInfos.get(0);
        boolean isFormatSupported = decoderInfo.isFormatSupported(format);
        boolean isPreferredDecoder = true;
        if (!isFormatSupported) {
            // Check whether any of the other decoders support the format.
            for (int i = 1; i < decoderInfos.size(); i++) {
                MediaCodecInfo otherDecoderInfo = decoderInfos.get(i);
                if (otherDecoderInfo.isFormatSupported(format)) {
                    decoderInfo = otherDecoderInfo;
                    isFormatSupported = true;
                    isPreferredDecoder = false;
                    break;
                }
            }
        }
        @C.FormatSupport
        int formatSupport = isFormatSupported ? C.FORMAT_HANDLED : C.FORMAT_EXCEEDS_CAPABILITIES;
        @AdaptiveSupport
        int adaptiveSupport =
                decoderInfo.isSeamlessAdaptationSupported(format)
                        ? ADAPTIVE_SEAMLESS
                        : ADAPTIVE_NOT_SEAMLESS;
        @HardwareAccelerationSupport
        int hardwareAccelerationSupport =
                decoderInfo.hardwareAccelerated
                        ? HARDWARE_ACCELERATION_SUPPORTED
                        : HARDWARE_ACCELERATION_NOT_SUPPORTED;
        @DecoderSupport
        int decoderSupport = isPreferredDecoder ? DECODER_SUPPORT_PRIMARY : DECODER_SUPPORT_FALLBACK;

        @TunnelingSupport int tunnelingSupport = TUNNELING_NOT_SUPPORTED;
        if (isFormatSupported) {
            List<MediaCodecInfo> tunnelingDecoderInfos =
                    getDecoderInfos(
                            mediaCodecSelector,
                            format,
                            requiresSecureDecryption,
                            /* requiresTunnelingDecoder= */ true);
            if (!tunnelingDecoderInfos.isEmpty()) {
                MediaCodecInfo tunnelingDecoderInfo =
                        MediaCodecUtil.getDecoderInfosSortedByFormatSupport(tunnelingDecoderInfos, format)
                                .get(0);
                if (tunnelingDecoderInfo.isFormatSupported(format)
                        && tunnelingDecoderInfo.isSeamlessAdaptationSupported(format)) {
                    tunnelingSupport = TUNNELING_SUPPORTED;
                }
            }
        }

        return RendererCapabilities.create(
                formatSupport,
                adaptiveSupport,
                tunnelingSupport,
                hardwareAccelerationSupport,
                decoderSupport);
    }

    private static List<MediaCodecInfo> getDecoderInfos(
            MediaCodecSelector mediaCodecSelector,
            Format format,
            boolean requiresSecureDecoder,
            boolean requiresTunnelingDecoder)
            throws MediaCodecUtil.DecoderQueryException {
        @Nullable String mimeType = format.sampleMimeType;
        if (mimeType == null) {
            return ImmutableList.of();
        }
        //requiresSecureDecoder = false;
        List<MediaCodecInfo> decoderInfos =
                mediaCodecSelector.getDecoderInfos(
                        mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
        @Nullable String alternativeMimeType = MediaCodecUtil.getAlternativeCodecMimeType(format);
        if (alternativeMimeType == null) {
            return ImmutableList.copyOf(decoderInfos);
        }
        List<MediaCodecInfo> alternativeDecoderInfos =
                mediaCodecSelector.getDecoderInfos(
                        alternativeMimeType, requiresSecureDecoder, requiresTunnelingDecoder);

        return ImmutableList.<MediaCodecInfo>builder()
                .addAll(decoderInfos)
                .addAll(alternativeDecoderInfos)
                .build();
    }

    @Override
    protected List<MediaCodecInfo> getDecoderInfos(
            MediaCodecSelector mediaCodecSelector, Format format, boolean requiresSecureDecoder)
            throws MediaCodecUtil.DecoderQueryException {
        // Done by us, because secure awesome decoder has issue on MiBox4
        requiresSecureDecoder = false;
        return MediaCodecUtil.getDecoderInfosSortedByFormatSupport(
                getDecoderInfos(mediaCodecSelector, format, requiresSecureDecoder, false), format);
    }
}
