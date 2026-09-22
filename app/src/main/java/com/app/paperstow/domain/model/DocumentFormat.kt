package com.app.paperstow.domain.model

/**
 * File formats Paperstow can stow.
 *
 * Core travel papers: PDF, JPEG, PNG, WEBP, HEIC, BMP, GIF, plain text, markdown, and GPX.
 * VIDEO / AUDIO / DICOM remain in the enum so older stored files still open;
 * they are not offered as import types.
 */
enum class DocumentFormat {
    PDF,
    JPG,
    PNG,
    TEXT,
    MARKDOWN,
    GPX,
    WEBP,
    HEIC,
    BMP,
    GIF,
    VIDEO,
    AUDIO,
    DICOM,
    UNKNOWN,
}
