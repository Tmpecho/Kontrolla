package org.kontrolla.documents.application;

/**
 * File download payload for a stored document.
 *
 * @param fileName the downloaded file name
 * @param contentType the file content type
 * @param fileSizeBytes the file size in bytes
 * @param content the raw file content
 */
public record DocumentFileDownload(
    String fileName,
    String contentType,
    long fileSizeBytes,
    byte[] content
) {
}
