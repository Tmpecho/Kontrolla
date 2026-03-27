package org.kontrolla.iam.application;

import java.util.UUID;

public record UserAppContext(
		UUID organizationId,
		String organizationName,
		UUID establishmentId,
		String establishmentName
) {
}
