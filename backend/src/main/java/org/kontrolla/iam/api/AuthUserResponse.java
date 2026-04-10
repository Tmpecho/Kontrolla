package org.kontrolla.iam.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;

/**
 * API response describing the authenticated user returned by auth endpoints.
 *
 * @param id the user identifier
 * @param email the user email
 * @param firstName the user first name
 * @param lastName the user last name
 * @param active whether the user is active
 * @param globalRoles the user's global roles
 * @param createdAt when the user was created
 * @param updatedAt when the user was last updated
 */
public record AuthUserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    boolean active,
    Set<GlobalRole> globalRoles,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Maps a user entity to the auth user response shape.
   *
   * @param user the user to map
   * @return the mapped response
   */
  public static AuthUserResponse from(User user) {
    return new AuthUserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.isActive(),
        Set.copyOf(user.getGlobalRoles()),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
