package org.kontrolla.iam.api;

import org.kontrolla.iam.application.UserAppContext;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.UUID;

/**
 * API response describing the resolved application context for the user.
 *
 * @param organizationId the selected organization identifier
 * @param organizationName the selected organization name
 * @param organizationRole the user's organization role
 * @param establishmentId the selected establishment identifier
 * @param establishmentName the selected establishment name
 */
public record UserAppContextResponse(
    UUID organizationId,
    String organizationName,
    OrganizationRole organizationRole,
    UUID establishmentId,
    String establishmentName
) {

  /**
   * Maps an application user context to the API response shape.
   *
   * @param userAppContext the context to map
   * @return the mapped response
   */
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
