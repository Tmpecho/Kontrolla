package org.kontrolla.documents.api;

import java.time.Instant;
import java.util.UUID;
import org.kontrolla.documents.domain.DocumentAuditAssignment;

/**
 * API response describing a document audit assignment.
 *
 * @param userId the assigned user identifier
 * @param userEmail the assigned user email
 * @param userFirstName the assigned user first name
 * @param userLastName the assigned user last name
 * @param acknowledgedAt when the assignment was acknowledged, if applicable
 */
public record DocumentAuditAssignmentResponse(
    UUID userId,
    String userEmail,
    String userFirstName,
    String userLastName,
    Instant acknowledgedAt) {

  /**
   * Maps an audit assignment entity to the API response shape.
   *
   * @param assignment the assignment to map
   * @return the mapped response
   */
  public static DocumentAuditAssignmentResponse from(DocumentAuditAssignment assignment) {
    return new DocumentAuditAssignmentResponse(
        assignment.getUser().getId(),
        assignment.getUser().getEmail(),
        assignment.getUser().getFirstName(),
        assignment.getUser().getLastName(),
        assignment.getAcknowledgedAt());
  }
}
