package org.kontrolla.iam.api;

import org.kontrolla.iam.application.UserAppContext;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.UUID;

public record UserAppContextResponse(
    UUID organizationId,
    String organizationName,
    UUID establishmentId,
    String establishmentName,
    OrganizationRole organizationRole
) {

  public static UserAppContextResponse from(UserAppContext userAppContext) {
    return new UserAppContextResponse(
        userAppContext.organizationId(),
        userAppContext.organizationName(),
        userAppContext.establishmentId(),
        userAppContext.establishmentName(),
        userAppContext.organizationRole()
    );
  }
}
