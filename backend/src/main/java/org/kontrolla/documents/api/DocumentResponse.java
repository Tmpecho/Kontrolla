package org.kontrolla.documents.api;

import org.kontrolla.documents.domain.Document;
import org.kontrolla.documents.domain.DocumentServiceArea;
import org.kontrolla.documents.domain.DocumentStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
