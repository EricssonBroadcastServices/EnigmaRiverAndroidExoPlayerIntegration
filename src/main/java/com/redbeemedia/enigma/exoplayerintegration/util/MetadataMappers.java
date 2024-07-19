package com.redbeemedia.enigma.exoplayerintegration.util;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.emsg.EventMessage;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.redbeemedia.enigma.core.player.timeline.EnigmaEventMessage;
import com.redbeemedia.enigma.core.player.timeline.EnigmaMetadata;
import com.redbeemedia.enigma.core.player.timeline.EnigmaHlsMediaPlaylist;

import java.util.ArrayList;

public class MetadataMappers {
    public static EnigmaMetadata mapMetadata(Metadata metadata) {
        ArrayList<EnigmaMetadata.Entry> entries = new ArrayList<>();
        for (int i = 0; i < metadata.length(); i++) {
            entries.add(mapMetadataEntry(metadata.get(i)));
        }
        return new EnigmaMetadata(metadata.presentationTimeUs, entries);
    }

    public static EnigmaMetadata.Entry mapMetadataEntry(Metadata.Entry entry) {
        Format format = entry.getWrappedMetadataFormat();
        EnigmaMetadata.Format enigmaFormat = null;
        if (format != null) {
            enigmaFormat = new EnigmaMetadata.Format(
                    format.id,
                    format.label,
                    format.selectionFlags,
                    format.roleFlags,
                    format.bitrate,
                    format.codecs,
                    format.containerMimeType,
                    format.sampleMimeType,
                    format.maxInputSize,
                    format.initializationData,
                    format.subsampleOffsetUs,
                    format.width,
                    format.height,
                    format.frameRate,
                    format.rotationDegrees,
                    format.pixelWidthHeightRatio,
                    format.projectionData,
                    format.stereoMode,
                    format.channelCount,
                    format.sampleRate,
                    format.pcmEncoding,
                    format.encoderDelay,
                    format.encoderPadding,
                    format.language,
                    format.accessibilityChannel);
        }
        if (entry instanceof EventMessage) {
            return new EnigmaEventMessage(
                    enigmaFormat,
                    entry.getWrappedMetadataBytes(),
                    ((EventMessage) entry).schemeIdUri,
                    ((EventMessage) entry).value,
                    ((EventMessage) entry).durationMs,
                    ((EventMessage) entry).id,
                    ((EventMessage) entry).messageData
            );
        } else {
            return new EnigmaMetadata.Entry(
                    enigmaFormat,
                    entry.getWrappedMetadataBytes()
            );
        }
    }

    public static EnigmaHlsMediaPlaylist mapHlsMetadata(HlsMediaPlaylist metadata) {
        ArrayList<EnigmaHlsMediaPlaylist.Segment> segments = new ArrayList<>();
        for (HlsMediaPlaylist.Segment seg : metadata.segments) {
            segments.add(mapHlsSegment(seg));
        }
        ArrayList<EnigmaHlsMediaPlaylist.Part> trailingParts = new ArrayList<>();
        for (HlsMediaPlaylist.Part part : metadata.trailingParts) {
            trailingParts.add(mapHlsPart(part));
        }
        return new EnigmaHlsMediaPlaylist(
                metadata.playlistType,
                metadata.baseUri,
                metadata.tags,
                metadata.startOffsetUs,
                metadata.preciseStart,
                metadata.startTimeUs,
                metadata.hasDiscontinuitySequence,
                metadata.discontinuitySequence,
                metadata.mediaSequence,
                metadata.version,
                metadata.targetDurationUs,
                metadata.partTargetDurationUs,
                metadata.hasIndependentSegments,
                metadata.hasEndTag,
                metadata.hasProgramDateTime,
                metadata.durationUs,
                metadata.hasPositiveStartOffset,
                segments,
                trailingParts
        );
    }

    static EnigmaHlsMediaPlaylist.Part mapHlsPart(HlsMediaPlaylist.Part part) {
        return new EnigmaHlsMediaPlaylist.Part(
                part.url,
                part.durationUs,
                part.relativeDiscontinuitySequence,
                part.relativeStartTimeUs,
                part.fullSegmentEncryptionKeyUri,
                part.encryptionIV,
                part.byteRangeOffset,
                part.byteRangeLength,
                part.hasGapTag,
                part.isIndependent,
                part.isPreload
        );
    }

    static EnigmaHlsMediaPlaylist.Segment mapHlsSegment(HlsMediaPlaylist.Segment segment) {
        ArrayList<EnigmaHlsMediaPlaylist.Part> parts = new ArrayList<>();
        for (HlsMediaPlaylist.Part part : segment.parts) {
            parts.add(mapHlsPart(part));
        }
        return new EnigmaHlsMediaPlaylist.Segment(
                segment.url,
                segment.durationUs,
                segment.relativeDiscontinuitySequence,
                segment.relativeStartTimeUs,
                segment.fullSegmentEncryptionKeyUri,
                segment.encryptionIV,
                segment.byteRangeOffset,
                segment.byteRangeLength,
                segment.hasGapTag,
                segment.title,
                parts
        );
    }
}
