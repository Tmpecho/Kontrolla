package org.kontrolla.iam.application;

import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.UUID;

public record UserAppContext(
    UUID organizationId,
    String organizationName,
    UUID establishmentId,
    String establishmentName,
    OrganizationRole organizationRole
) {
}
