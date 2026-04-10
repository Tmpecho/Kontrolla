package org.kontrolla.documents.api;

import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.documents.domain.DocumentStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API response describing a document and its audit state.
 *
 * @param id the document identifier
 * @param organizationId the owning organization identifier
 * @param establishmentId the owning establishment identifier
 * @param createdByUserId the creator user identifier
 * @param serviceArea the document service area
 * @param title the document title
 * @param holderName the document holder name
 * @param issueDate the issue date
 * @param renewalDate the renewal date
 * @param fileName the stored file name
 * @param contentType the file content type
 * @param fileSizeBytes the file size in bytes
 * @param status the derived document status
 * @param auditAssignments the audit assignments for the document
 * @param createdAt when the document was created
 * @param updatedAt when the document was last updated
 */
public record DocumentResponse(
    UUID id,
    UUID organizationId,
    UUID establishmentId,
    UUID createdByUserId,
    DocumentServiceArea serviceArea,
    String title,
    String holderName,
    LocalDate issueDate,
    LocalDate renewalDate,
    String fileName,
    String contentType,
    long fileSizeBytes,
    DocumentStatus status,
    List<DocumentAuditAssignmentResponse> auditAssignments,
    Instant createdAt,
    Instant updatedAt
) {

  /**
   * Maps a document entity to the API response shape using the supplied clock
   * to derive status.
   *
   * @param document the document to map
   * @param clock the clock used to calculate document status
   * @return the mapped response
   */
  public static DocumentResponse from(Document document, Clock clock) {
    return new DocumentResponse(
        document.getId(),
        document.getOrganization().getId(),
        document.getEstablishment().getId(),
        document.getCreatedByUser().getId(),
        document.getServiceArea(),
        document.getTitle(),
        document.getHolderName(),
        document.getIssueDate(),
        document.getRenewalDate(),
        document.getFileName(),
        document.getContentType(),
        document.getFileSizeBytes(),
        document.getStatus(LocalDate.now(clock)),
        document.getAuditAssignments().stream()
            .map(DocumentAuditAssignmentResponse::from)
            .toList(),
        document.getCreatedAt(),
        document.getUpdatedAt()
    );
  }
}
