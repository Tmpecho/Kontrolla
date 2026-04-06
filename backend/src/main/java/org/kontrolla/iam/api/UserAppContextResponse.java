package org.kontrolla.iam.api;

import org.kontrolla.iam.application.UserAppContext;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.UUID;

public record UserAppContextResponse(
		UUID organizationId,
		String organizationName,
		OrganizationRole organizationRole,
		UUID establishmentId,
		String establishmentName
) {

	public static UserAppContextResponse from(UserAppContext userAppContext) {
		return new UserAppContextResponse(
				userAppContext.organizationId(),
				userAppContext.organizationName(),
				userAppContext.organizationRole(),
				userAppContext.establishmentId(),
				userAppContext.establishmentName()
		);
	}
}
