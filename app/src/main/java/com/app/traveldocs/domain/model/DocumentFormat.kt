package com.app.traveldocs.domain.model

/**
 * File formats we know how to handle.
 *
 * The core set (PDF, JPG, PNG, VIDEO) is always available.
 * Extended formats (WEBP, HEIC, BMP, GIF, DICOM) are behind the "Extended Image Formats"
 * experimental feature flag — mostly because HEIC needs API 28+ and DICOM is niche.
 */
enum class DocumentFormat {
    PDF,
    JPG,
    PNG,
    VIDEO,

    // Extended formats (experimental)
    WEBP,
    HEIC,   // iOS photos — needs API 28+
    BMP,    // Legacy scanner output
    GIF,    // Rarely useful for docs, but trivial to support
    DICOM,  // Medical imaging — custom parser, no third-party deps
    AUDIO,   // MP3, M4A, WAV — playable media
    UNKNOWN, // Unsupported format — stored as-is, tagged __UNSUPPORTED
}
