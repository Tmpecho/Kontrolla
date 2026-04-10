package org.kontrolla.organizations.api;

import jakarta.validation.Valid;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.organizations.application.OrganizationService;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Platform-admin REST API for creating and listing organizations. */
@RestController
@RequestMapping("/api/v1/admin/organizations")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminOrganizationController {

  private final OrganizationService organizationService;

  /**
   * Creates a controller backed by the organization service.
   *
   * @param organizationService service handling organization operations
   */
  public AdminOrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  /**
   * Creates a new organization.
   *
   * @param request the request payload
   * @return the created organization response
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrganizationResponse createOrganization(
      @Valid @RequestBody CreateOrganizationRequest request) {
    Organization organization =
        organizationService.createOrganization(
            request.name(),
            request.status() == null ? OrganizationStatus.ACTIVE : request.status());
    return OrganizationResponse.from(organization);
  }

  /**
   * Lists organizations for platform administrators.
   *
   * @param pageable pagination information
   * @return a page of organization responses
   */
  @GetMapping
  public PageResponse<OrganizationResponse> listOrganizations(
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    return PageResponse.from(
        organizationService.listOrganizations(pageable), OrganizationResponse::from);
  }
}
