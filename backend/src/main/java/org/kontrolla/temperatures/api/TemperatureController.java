package org.kontrolla.temperatures.api;

import jakarta.validation.Valid;
import org.kontrolla.temperatures.application.CreateTemperatureUnitCommand;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.temperatures.application.CreateTemperatureLogCommand;
import org.kontrolla.temperatures.application.TemperatureService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST API for managing temperature units and logged readings for an
 * establishment.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments/{establishmentId}/temperature-units")
public class TemperatureController {

  private final TemperatureService temperatureService;

  /**
   * Creates a controller backed by the temperature service.
   *
   * @param temperatureService service handling temperature unit operations
   */
  public TemperatureController(TemperatureService temperatureService) {
    this.temperatureService = temperatureService;
  }

  /**
   * Lists temperature units for an establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param currentUser the authenticated user
   * @return the temperature units for the establishment
   */
  @GetMapping
  public List<TemperatureUnitResponse> listTemperatureUnits(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser
  ) {
    return temperatureService.listTemperatureUnits(organizationId, establishmentId, currentUser)
        .stream()
        .map(TemperatureUnitResponse::from)
        .toList();
  }

  /**
   * Creates a new temperature unit for an establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param currentUser the authenticated user
   * @param request the request payload
   * @return the created temperature unit
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TemperatureUnitResponse createTemperatureUnit(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody CreateTemperatureUnitRequest request
  ) {
    return TemperatureUnitResponse.from(
        temperatureService.createTemperatureUnit(
            organizationId,
            establishmentId,
            new CreateTemperatureUnitCommand(
                request.name(),
                request.location(),
                request.type(),
                request.dueByTime(),
                request.minimumTemperature(),
                request.maximumTemperature()
            ),
            currentUser
        )
    );
  }

  /**
   * Records a new temperature reading for a specific temperature unit.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param temperatureUnitId the temperature unit identifier
   * @param currentUser the authenticated user
   * @param request the request payload
   * @return the created temperature log entry
   */
  @PostMapping("/{temperatureUnitId}/logs")
  @ResponseStatus(HttpStatus.CREATED)
  public TemperatureLogEntryResponse createTemperatureLog(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID temperatureUnitId,
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody CreateTemperatureLogRequest request
  ) {
    return TemperatureLogEntryResponse.from(
        temperatureService.createTemperatureLog(
            organizationId,
            establishmentId,
            temperatureUnitId,
            new CreateTemperatureLogCommand(
                request.temperatureCelsius(),
                request.measuredAt(),
                request.note()
            ),
            currentUser
        )
    );
  }

  /**
   * Deletes an existing temperature unit.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param temperatureUnitId the temperature unit identifier
   * @param currentUser the authenticated user
   */
  @DeleteMapping("/{temperatureUnitId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTemperatureUnit(
      @PathVariable UUID organizationId,
      @PathVariable UUID establishmentId,
      @PathVariable UUID temperatureUnitId,
      @AuthenticationPrincipal CurrentUser currentUser
  ) {
    temperatureService.deleteTemperatureUnit(
        organizationId,
        establishmentId,
        temperatureUnitId,
        currentUser
    );
  }
}
