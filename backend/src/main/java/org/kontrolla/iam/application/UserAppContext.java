package org.kontrolla.iam.application;

import java.util.UUID;
import org.kontrolla.organizations.domain.OrganizationRole;

/**
 * Resolved organization and establishment context associated with an authenticated user session.
 *
 * @param organizationId the selected organization identifier
 * @param organizationName the selected organization name
 * @param organizationRole the user's role in the organization
 * @param establishmentId the selected establishment identifier
 * @param establishmentName the selected establishment name
 */
public record UserAppContext(
    UUID organizationId,
    String organizationName,
    OrganizationRole organizationRole,
    UUID establishmentId,
    String establishmentName) {}
