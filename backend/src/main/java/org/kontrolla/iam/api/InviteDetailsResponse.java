package org.kontrolla.iam.api;

import java.time.Instant;
import org.kontrolla.iam.application.InviteDetails;

/**
 * API response exposing public information about an invitation.
 *
 * @param email the invited email address
 * @param firstName the invited user's first name
 * @param lastName the invited user's last name
 * @param organizationName the invited organization name
 * @param expiresAt when the invitation expires
 */
public record InviteDetailsResponse(
    String email, String firstName, String lastName, String organizationName, Instant expiresAt) {

  /**
   * Maps invite details to the API response shape.
   *
   * @param inviteDetails the invite details to map
   * @return the mapped response
   */
  public static InviteDetailsResponse from(InviteDetails inviteDetails) {
    return new InviteDetailsResponse(
        inviteDetails.email(),
        inviteDetails.firstName(),
        inviteDetails.lastName(),
        inviteDetails.organizationName(),
        inviteDetails.expiresAt());
  }
}
