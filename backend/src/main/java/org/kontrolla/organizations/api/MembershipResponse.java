package org.kontrolla.organizations.api;

import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
				membership.isAccessAllEstablishments(),
				membership.getAccessibleEstablishments().stream()
						.map(MembershipEstablishmentResponse::from)
						.sorted(java.util.Comparator.comparing(MembershipEstablishmentResponse::name, String.CASE_INSENSITIVE_ORDER))
						.toList(),
				membership.getCreatedAt(),
				membership.getUpdatedAt()
		);
	}
}
