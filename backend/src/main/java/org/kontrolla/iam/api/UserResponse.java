package org.kontrolla.iam.api;

import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * API response describing a user.
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
public record UserResponse(
		UUID id,
		String email,
		String firstName,
		String lastName,
		boolean active,
		Set<GlobalRole> globalRoles,
		Instant createdAt,
		Instant updatedAt
) {

	/**
	 * Maps a user entity to the API response shape.
	 *
	 * @param user the user to map
	 * @return the mapped response
	 */
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.isActive(),
				Set.copyOf(user.getGlobalRoles()),
				user.getCreatedAt(),
				user.getUpdatedAt()
		);
	}
}
