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

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/establishments/{establishmentId}/temperature-units")
public class TemperatureController {

  private final TemperatureService temperatureService;

  public TemperatureController(TemperatureService temperatureService) {
    this.temperatureService = temperatureService;
  }

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
