package com.fanlink.ocr;

/**
 * @param configured false when no OCR provider credentials are set (local/dev run) —
 *                    callers should treat this as "could not verify" rather than a hard failure.
 */
public record OcrResult(String rawText, boolean configured) {
}
