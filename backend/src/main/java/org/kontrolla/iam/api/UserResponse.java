package org.kontrolla.iam.api;

import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
