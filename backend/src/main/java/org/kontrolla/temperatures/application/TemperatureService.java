package org.kontrolla.temperatures.application;

import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.iam.application.UserAccessService;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.temperatures.domain.TemperatureLog;
import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.infrastructure.TemperatureUnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TemperatureService {

  private final TemperatureUnitRepository temperatureUnitRepository;
  private final EstablishmentService establishmentService;
  private final UserAccessService userAccessService;

  public TemperatureService(
      TemperatureUnitRepository temperatureUnitRepository,
      EstablishmentService establishmentService,
      UserAccessService userAccessService
  ) {
    this.temperatureUnitRepository = temperatureUnitRepository;
    this.establishmentService = establishmentService;
    this.userAccessService = userAccessService;
  }

  @Transactional(readOnly = true)
  public List<TemperatureUnitView> listTemperatureUnits(
      UUID organizationId,
      UUID establishmentId,
      CurrentUser currentUser
  ) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    return temperatureUnitRepository.findByEstablishmentIdAndOrganizationIdOrderByNameAsc(
            establishmentId,
            organizationId
        ).stream()
        .map(TemperatureUnitView::from)
        .toList();
  }

  @Transactional
  public TemperatureLogEntryView createTemperatureLog(
      UUID organizationId,
      UUID establishmentId,
      UUID temperatureUnitId,
      CreateTemperatureLogCommand command,
      CurrentUser currentUser
  ) {
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    validateCreateCommand(command);

    TemperatureUnit temperatureUnit = temperatureUnitRepository
        .findByIdAndEstablishmentIdAndOrganizationId(temperatureUnitId, establishmentId, organizationId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "temperature_unit_not_found",
            "Temperature unit not found"
        ));
    User actor = userAccessService.getCurrentUserOrThrow(currentUser);
    String normalizedNote = normalizeOptionalText(command.note());

    validateOutOfRangeNote(temperatureUnit, command, normalizedNote);

    TemperatureLog temperatureLog = new TemperatureLog(
        command.measuredAt(),
        command.temperatureCelsius(),
        normalizedNote,
        actor
    );
    temperatureUnit.addLog(temperatureLog);
    temperatureUnitRepository.save(temperatureUnit);

    return TemperatureLogEntryView.from(temperatureLog);
  }

  private void validateCreateCommand(CreateTemperatureLogCommand command) {
    if (command == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_log_required",
          "Temperature log payload is required"
      );
    }

    if (command.temperatureCelsius() == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_required",
          "Temperature is required"
      );
    }

    if (command.measuredAt() == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "measured_at_required",
          "Measured timestamp is required"
      );
    }
  }

  private void validateOutOfRangeNote(
      TemperatureUnit temperatureUnit,
      CreateTemperatureLogCommand command,
      String normalizedNote
  ) {
    if (!temperatureUnit.isWithinRange(command.temperatureCelsius()) && normalizedNote == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_note_required",
          "A note is required for out-of-range temperature readings"
      );
    }
  }

  private String normalizeOptionalText(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
