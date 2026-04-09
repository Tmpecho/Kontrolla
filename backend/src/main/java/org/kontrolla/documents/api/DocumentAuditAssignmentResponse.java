package org.kontrolla.documents.api;

import org.kontrolla.documents.domain.DocumentAuditAssignment;

import java.time.Instant;
import java.util.UUID;

public record DocumentAuditAssignmentResponse(
    UUID userId,
    String userEmail,
    String userFirstName,
    String userLastName,
    Instant acknowledgedAt
) {

  public static DocumentAuditAssignmentResponse from(DocumentAuditAssignment assignment) {
    return new DocumentAuditAssignmentResponse(
        assignment.getUser().getId(),
        assignment.getUser().getEmail(),
        assignment.getUser().getFirstName(),
        assignment.getUser().getLastName(),
        assignment.getAcknowledgedAt()
    );
  }
}
