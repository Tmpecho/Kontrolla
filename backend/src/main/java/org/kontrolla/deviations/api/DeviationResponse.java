package org.kontrolla.deviations.api;

import java.time.Instant;
import java.util.UUID;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;

/**
 * API response describing a deviation summary.
 *
 * @param id the deviation identifier
 * @param organizationId the owning organization identifier
 * @param establishmentId the owning establishment identifier
 * @param createdByUserId the creator user identifier
 * @param assignedToUserId the assigned user identifier, if any
 * @param title the deviation title
 * @param description the deviation description
 * @param status the deviation status
 * @param severity the deviation severity
 * @param category the deviation category
 * @param createdAt when the deviation was created
 * @param updatedAt when the deviation was last updated
 */
public record DeviationResponse(
    UUID id,
    UUID organizationId,
    UUID establishmentId,
    UUID createdByUserId,
    UUID assignedToUserId,
    String title,
    String description,
    DeviationStatus status,
    DeviationSeverity severity,
    DeviationCategory category,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Maps a deviation entity to the API response shape.
   *
   * @param deviation the deviation to map
   * @return the mapped response
   */
  public static DeviationResponse from(Deviation deviation) {
    return new DeviationResponse(
        deviation.getId(),
        deviation.getOrganization().getId(),
        deviation.getEstablishment().getId(),
        deviation.getCreatedByUser().getId(),
        deviation.getAssignedToUser() == null ? null : deviation.getAssignedToUser().getId(),
        deviation.getTitle(),
        deviation.getDescription(),
        deviation.getStatus(),
        deviation.getSeverity(),
        deviation.getCategory(),
        deviation.getCreatedAt(),
        deviation.getUpdatedAt());
  }
}
