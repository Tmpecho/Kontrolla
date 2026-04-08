package org.kontrolla.documents.application;

public record DocumentFileDownload(
    String fileName,
    String contentType,
    long fileSizeBytes,
    byte[] content
) {
}
