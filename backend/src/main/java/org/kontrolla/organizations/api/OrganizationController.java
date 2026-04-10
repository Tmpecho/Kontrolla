package org.kontrolla.organizations.api;

import java.util.UUID;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API for reading organization information available to the current user. */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

  private final OrganizationService organizationService;

  /**
   * Creates a controller backed by the organization service.
   *
   * @param organizationService service handling organization operations
   */
  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  /**
   * Returns a single organization by id.
   *
   * @param organizationId the organization identifier
   * @param currentUser the authenticated user
   * @return the organization response
   */
  @GetMapping("/{organizationId}")
  public OrganizationResponse getOrganization(
      @PathVariable UUID organizationId, @AuthenticationPrincipal CurrentUser currentUser) {
    return OrganizationResponse.from(
        organizationService.getOrganization(organizationId, currentUser));
  }
}
