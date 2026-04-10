package org.kontrolla.temperatures.application;

import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.establishments.application.EstablishmentService;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.iam.application.UserAccessService;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.application.OrganizationAccessService;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.temperatures.domain.TemperatureLog;
import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.infrastructure.TemperatureLogRepository;
import org.kontrolla.temperatures.infrastructure.TemperatureUnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates temperature unit management and temperature logging for an
 * establishment.
 */
@Service
public class TemperatureService {

  private final TemperatureUnitRepository temperatureUnitRepository;
  private final TemperatureLogRepository temperatureLogRepository;
  private final OrganizationAccessService organizationAccessService;
  private final EstablishmentService establishmentService;
  private final UserAccessService userAccessService;

  /**
   * Creates a service for managing temperature units and their logged readings.
   *
   * @param temperatureUnitRepository repository for temperature units
   * @param temperatureLogRepository repository for temperature logs
   * @param organizationAccessService service for organization-level access checks
   * @param establishmentService service for establishment access and lookup
   * @param userAccessService service for resolving the current user
   */
  public TemperatureService(
      TemperatureUnitRepository temperatureUnitRepository,
      TemperatureLogRepository temperatureLogRepository,
      OrganizationAccessService organizationAccessService,
      EstablishmentService establishmentService,
      UserAccessService userAccessService
  ) {
    this.temperatureUnitRepository = temperatureUnitRepository;
    this.temperatureLogRepository = temperatureLogRepository;
    this.organizationAccessService = organizationAccessService;
    this.establishmentService = establishmentService;
    this.userAccessService = userAccessService;
  }

  /**
   * Lists temperature units for the specified establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param currentUser the authenticated user
   * @return the temperature units for the establishment
   */
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

  /**
   * Creates a new temperature unit for the specified establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param command the creation command
   * @param currentUser the authenticated user
   * @return the created temperature unit
   */
  @Transactional
  public TemperatureUnitView createTemperatureUnit(
      UUID organizationId,
      UUID establishmentId,
      CreateTemperatureUnitCommand command,
      CurrentUser currentUser
  ) {
    organizationAccessService.requireMembershipManagement(currentUser, organizationId);
    Organization organization = organizationAccessService.getOrganizationOrThrow(organizationId);
    Establishment establishment = establishmentService.getEstablishment(organizationId, establishmentId, currentUser);
    validateCreateUnitCommand(command);

    String normalizedName = normalizeRequiredText(command.name());
    String normalizedLocation = normalizeRequiredText(command.location());

    boolean duplicateExists = temperatureUnitRepository
        .findByEstablishmentIdAndOrganizationIdOrderByNameAsc(establishmentId, organizationId)
        .stream()
        .anyMatch(unit -> unit.getName().equalsIgnoreCase(normalizedName));
    if (duplicateExists) {
      throw new ConflictException(
          "temperature_unit_already_exists",
          "A temperature unit with that name already exists for this establishment"
      );
    }

    TemperatureUnit unit = new TemperatureUnit(
        organization,
        establishment,
        normalizedName,
        normalizedLocation,
        command.type(),
        command.dueByTime(),
        command.minimumTemperature(),
        command.maximumTemperature()
    );

    return TemperatureUnitView.from(temperatureUnitRepository.save(unit));
  }

  /**
   * Records a new temperature reading for a specific temperature unit.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param temperatureUnitId the temperature unit identifier
   * @param command the log creation command
   * @param currentUser the authenticated user
   * @return the created temperature log entry view
   */
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
    TemperatureLog persistedTemperatureLog = temperatureLogRepository.saveAndFlush(temperatureLog);

    return TemperatureLogEntryView.from(persistedTemperatureLog);
  }

  /**
   * Deletes a temperature unit from the specified establishment.
   *
   * @param organizationId the organization identifier
   * @param establishmentId the establishment identifier
   * @param temperatureUnitId the temperature unit identifier
   * @param currentUser the authenticated user
   */
  @Transactional
  public void deleteTemperatureUnit(
      UUID organizationId,
      UUID establishmentId,
      UUID temperatureUnitId,
      CurrentUser currentUser
  ) {
    organizationAccessService.requireMembershipManagement(currentUser, organizationId);
    establishmentService.getEstablishment(organizationId, establishmentId, currentUser);

    TemperatureUnit temperatureUnit = temperatureUnitRepository
        .findByIdAndEstablishmentIdAndOrganizationId(temperatureUnitId, establishmentId, organizationId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "temperature_unit_not_found",
            "Temperature unit not found"
        ));

    temperatureUnitRepository.delete(temperatureUnit);
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

  private void validateCreateUnitCommand(CreateTemperatureUnitCommand command) {
    if (command == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_unit_required",
          "Temperature unit payload is required"
      );
    }

    if (command.type() == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_unit_type_required",
          "Temperature unit type is required"
      );
    }

    if (command.dueByTime() == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_due_time_required",
          "Due time is required"
      );
    }

    if (command.minimumTemperature() == null || command.maximumTemperature() == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_range_required",
          "Minimum and maximum temperatures are required"
      );
    }

    if (command.maximumTemperature().compareTo(command.minimumTemperature()) < 0) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "invalid_temperature_range",
          "Maximum temperature cannot be below minimum temperature"
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

  private String normalizeRequiredText(String value) {
    if (value == null) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_unit_field_required",
          "Required text fields must be provided"
      );
    }

    String normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST,
          "temperature_unit_field_required",
          "Required text fields must not be blank"
      );
    }

    return normalized;
  }
}
