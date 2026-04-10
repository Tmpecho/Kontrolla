package org.kontrolla.establishments.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.kontrolla.common.api.PageResponse;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for listing, creating, and reading establishments together with serving-hours
 * configuration.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments")
public class EstablishmentController {

  private final EstablishmentService establishmentService;

  /**
   * Creates a controller backed by the establishment service.
   *
   * @param establishmentService service handling establishment operations
   */
  public EstablishmentController(EstablishmentService establishmentService) {
    this.establishmentService = establishmentService;
  }

  /**
   * Lists establishments within an organization that are accessible to the current user.
   *
   * @param organizationId the organization identifier
   * @param currentUser the authenticated user
   * @param pageable pagination information
   * @return a page of establishment responses
   */
  @GetMapping
  public PageResponse<EstablishmentResponse> listEstablishments(
      @PathVariable UUID organizationId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
    return PageResponse.from(
        establishmentService.listEstablishments(organizationId, currentUser, pageable),
        EstablishmentResponse::from);
  }

  /**
   * Creates a new establishment in an organization.
   *
   * @param organizationId the organization identifier
   * @param currentUser the authenticated user
   * @param request the request payload
   * @return the created establishment response
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EstablishmentResponse createEstablishment(
      @PathVariable UUID organizationId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody CreateEstablishmentRequest request) {
    Establishment establishment =
        establishmentService.createEstablishment(
            organizationId,
            request.name(),
            request.type(),
            request.status() == null ? EstablishmentStatus.ACTIVE : request.status(),
            currentUser);
    return EstablishmentResponse.from(establishment);
  }

  /**
   * Returns a single establishment by id.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param currentUser the authenticated user
   * @return the establishment response
   */
  @GetMapping("/{establishmentId}")
  public EstablishmentResponse getEstablishment(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser) {
    return EstablishmentResponse.from(
        establishmentService.getEstablishment(organizationId, establishmentId, currentUser));
  }

  /**
   * Returns serving hours for every weekday for an establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param currentUser the authenticated user
   * @return the serving-hours responses
   */
  @GetMapping("/{establishmentId}/serving-hours")
  public java.util.List<ServingHoursDayResponse> getServingHours(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser) {
    return establishmentService
        .getServingHours(organizationId, establishmentId, currentUser)
        .stream()
        .map(ServingHoursDayResponse::from)
        .toList();
  }

  /**
   * Updates serving hours for every weekday for an establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param currentUser the authenticated user
   * @param request the serving-hours update payload
   * @return the updated serving-hours responses
   */
  @PutMapping("/{establishmentId}/serving-hours")
  public java.util.List<ServingHoursDayResponse> updateServingHours(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody java.util.List<@Valid UpdateServingHoursDayRequest> request) {
    return establishmentService
        .updateServingHours(
            organizationId,
            establishmentId,
            request.stream().map(UpdateServingHoursDayRequest::toCommand).toList(),
            currentUser)
        .stream()
        .map(ServingHoursDayResponse::from)
        .toList();
  }
}
