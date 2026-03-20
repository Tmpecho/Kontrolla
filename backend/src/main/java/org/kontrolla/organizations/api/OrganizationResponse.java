package org.kontrolla.organizations.api;

import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
		UUID id,
		String name,
		OrganizationStatus status,
		Instant createdAt,
		Instant updatedAt
) {

	public static OrganizationResponse from(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getStatus(),
				organization.getCreatedAt(),
				organization.getUpdatedAt()
		);
	}
}
