package org.kontrolla.organizations.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;

/**
 * API response describing an organization membership.
 *
 * @param id the membership identifier
 * @param userId the member user identifier
 * @param userEmail the member email
 * @param userFirstName the member first name
 * @param userLastName the member last name
 * @param role the organization role
 * @param active whether the membership is active
 * @param allEstablishments whether the membership has organization-wide establishment access
 * @param establishments the explicitly assigned establishments
 * @param createdAt when the membership was created
 * @param updatedAt when the membership was last updated
 */
public record MembershipResponse(
    UUID id,
    UUID userId,
    String userEmail,
    String userFirstName,
    String userLastName,
    OrganizationRole role,
    boolean active,
    boolean allEstablishments,
    List<MembershipEstablishmentResponse> establishments,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Maps a membership entity to the API response shape.
   *
   * @param membership the membership to map
   * @return the mapped response
   */
  public static MembershipResponse from(OrganizationMembership membership) {
    return new MembershipResponse(
        membership.getId(),
        membership.getUser().getId(),
        membership.getUser().getEmail(),
        membership.getUser().getFirstName(),
        membership.getUser().getLastName(),
        membership.getRole(),
        membership.isActive(),
        membership.isAccessAllEstablishments(),
        membership.getAccessibleEstablishments().stream()
            .map(MembershipEstablishmentResponse::from)
            .sorted(
                java.util.Comparator.comparing(
                    MembershipEstablishmentResponse::name, String.CASE_INSENSITIVE_ORDER))
            .toList(),
        membership.getCreatedAt(),
        membership.getUpdatedAt());
  }
}
