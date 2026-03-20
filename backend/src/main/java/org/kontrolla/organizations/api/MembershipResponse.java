package org.kontrolla.organizations.api;

import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
		UUID id,
		UUID userId,
		String userEmail,
		String userFirstName,
		String userLastName,
		OrganizationRole role,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static MembershipResponse from(OrganizationMembership membership) {
		return new MembershipResponse(
				membership.getId(),
				membership.getUser().getId(),
				membership.getUser().getEmail(),
				membership.getUser().getFirstName(),
				membership.getUser().getLastName(),
				membership.getRole(),
				membership.isActive(),
				membership.getCreatedAt(),
				membership.getUpdatedAt()
		);
	}
}
