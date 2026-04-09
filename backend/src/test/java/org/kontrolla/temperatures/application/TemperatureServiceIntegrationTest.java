package org.kontrolla.temperatures.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.common.exception.ApplicationException;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.kontrolla.support.TestDataCleaner;
import org.kontrolla.temperatures.domain.TemperatureLog;
import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.domain.TemperatureUnitType;
import org.kontrolla.temperatures.infrastructure.TemperatureUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TemperatureServiceIntegrationTest {

  @Autowired
  private TemperatureService temperatureService;

  @Autowired
  private TemperatureUnitRepository temperatureUnitRepository;

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private EstablishmentRepository establishmentRepository;

  @Autowired
  private OrganizationMembershipRepository organizationMembershipRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void employeeWithEstablishmentAccessCanListUnitsAndCreateTemperatureLogs() {
    User employee = createUser("temperature-employee@example.com", "Maria", "Nilsen");
    User previousLogger = createUser("temperature-logger@example.com", "Jonas", "Berg");
    Organization organization = createOrganization("Kontrolla Temperature");
    Establishment establishment = createEstablishment(organization, "Kitchen");
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organization, previousLogger, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit freezer = createTemperatureUnit(
        organization,
        establishment,
        "Dessert freezer",
        "Cold dessert station",
        TemperatureUnitType.FREEZER,
        LocalTime.of(20, 30),
        new BigDecimal("-23.00"),
        new BigDecimal("-18.00")
    );
    freezer.addLog(new TemperatureLog(
        Instant.parse("2026-04-08T18:05:00Z"),
        new BigDecimal("-20.60"),
        "Evening close completed.",
        previousLogger
    ));
    temperatureUnitRepository.saveAndFlush(freezer);

    TemperatureUnit fridge = createTemperatureUnit(
        organization,
        establishment,
        "Sushi prep fridge",
        "Hot kitchen",
        TemperatureUnitType.FRIDGE,
        LocalTime.of(8, 30),
        new BigDecimal("2.00"),
        new BigDecimal("4.00")
    );
    temperatureUnitRepository.saveAndFlush(fridge);

    List<TemperatureUnitView> units = temperatureService.listTemperatureUnits(
        organization.getId(),
        establishment.getId(),
        currentUser(employee)
    );

    TemperatureLogEntryView createdLog = temperatureService.createTemperatureLog(
        organization.getId(),
        establishment.getId(),
        fridge.getId(),
        new CreateTemperatureLogCommand(
            new BigDecimal("3.20"),
            Instant.parse("2026-04-09T06:10:00Z"),
            "  Opening check completed.  "
        ),
        currentUser(employee)
    );

    TemperatureUnit persistedFridge = temperatureUnitRepository
        .findByEstablishmentIdAndOrganizationIdOrderByNameAsc(establishment.getId(), organization.getId())
        .stream()
        .filter(unit -> unit.getId().equals(fridge.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(units).hasSize(2);
    assertThat(units.get(0).name()).isEqualTo("Dessert freezer");
    assertThat(units.get(0).logs()).hasSize(1);
    assertThat(units.get(0).logs().getFirst().loggedByName()).isEqualTo("Jonas Berg");
    assertThat(units.get(1).name()).isEqualTo("Sushi prep fridge");
    assertThat(createdLog.id()).isNotNull();
    assertThat(createdLog.temperatureCelsius()).isEqualByComparingTo("3.20");
    assertThat(createdLog.note()).isEqualTo("Opening check completed.");
    assertThat(createdLog.loggedByName()).isEqualTo("Maria Nilsen");
    assertThat(persistedFridge.getLogs()).hasSize(1);
    assertThat(persistedFridge.getLogs().getFirst().getLoggedByUser().getId()).isEqualTo(employee.getId());
  }

  @Test
  void outOfRangeTemperatureRequiresNote() {
    User employee = createUser("temperature-note@example.com", "Sofie", "Hansen");
    Organization organization = createOrganization("Kontrolla Temperature Note");
    Establishment establishment = createEstablishment(organization, "Freezer Room");
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit unit = createTemperatureUnit(
        organization,
        establishment,
        "Frozen storage A",
        "Basement freezer room",
        TemperatureUnitType.FREEZER,
        LocalTime.of(7, 45),
        new BigDecimal("-24.00"),
        new BigDecimal("-18.00")
    );
    temperatureUnitRepository.saveAndFlush(unit);

    assertThatThrownBy(() -> temperatureService.createTemperatureLog(
        organization.getId(),
        establishment.getId(),
        unit.getId(),
        new CreateTemperatureLogCommand(
            new BigDecimal("-17.40"),
            Instant.parse("2026-04-09T05:45:00Z"),
            "   "
        ),
        currentUser(employee)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("A note is required for out-of-range temperature readings");
  }

  @Test
  void outsiderFromAnotherOrganizationCannotListOrCreateTemperatureLogs() {
    User member = createUser("temperature-member@example.com", "Henrik", "Solberg");
    User outsider = createUser("temperature-outsider@example.com", "Lina", "Dahl");
    Organization organizationA = createOrganization("Org A Temperature");
    Organization organizationB = createOrganization("Org B Temperature");
    Establishment establishment = createEstablishment(organizationA, "Front Bar");
    createMembership(organizationA, member, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organizationB, outsider, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit unit = createTemperatureUnit(
        organizationA,
        establishment,
        "Bar garnish fridge",
        "Front bar",
        TemperatureUnitType.FRIDGE,
        LocalTime.of(10, 15),
        new BigDecimal("2.00"),
        new BigDecimal("5.00")
    );
    temperatureUnitRepository.saveAndFlush(unit);

    assertThatThrownBy(() -> temperatureService.listTemperatureUnits(
        organizationA.getId(),
        establishment.getId(),
        currentUser(outsider)
    ))
        .isInstanceOf(ForbiddenException.class);

    assertThatThrownBy(() -> temperatureService.createTemperatureLog(
        organizationA.getId(),
        establishment.getId(),
        unit.getId(),
        new CreateTemperatureLogCommand(
            new BigDecimal("4.40"),
            Instant.parse("2026-04-09T08:00:00Z"),
            null
        ),
        currentUser(outsider)
    ))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void listTemperatureUnitsLimitsReturnedLogsToMostRecentSeven() {
    User logger = createUser("temperature-history@example.com", "History", "Logger");
    Organization organization = createOrganization("Kontrolla Temperature History");
    Establishment establishment = createEstablishment(organization, "Kitchen");
    createMembership(organization, logger, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit unit = createTemperatureUnit(
        organization,
        establishment,
        "Walk-in fridge",
        "Receiving room",
        TemperatureUnitType.FRIDGE,
        LocalTime.of(8, 30),
        new BigDecimal("2.00"),
        new BigDecimal("4.00")
    );

    for (int i = 0; i < 8; i++) {
      unit.addLog(new TemperatureLog(
          Instant.parse("2026-04-%02dT06:10:00Z".formatted(9 - i)),
          new BigDecimal("3.20"),
          "Log %d".formatted(i),
          logger
      ));
    }
    temperatureUnitRepository.saveAndFlush(unit);

    List<TemperatureUnitView> units = temperatureService.listTemperatureUnits(
        organization.getId(),
        establishment.getId(),
        currentUser(logger)
    );

    assertThat(units).hasSize(1);
    assertThat(units.getFirst().logs()).hasSize(7);
    assertThat(units.getFirst().logs().getFirst().measuredAt()).isEqualTo(Instant.parse("2026-04-09T06:10:00Z"));
    assertThat(units.getFirst().logs().getLast().measuredAt()).isEqualTo(Instant.parse("2026-04-03T06:10:00Z"));
  }

  @Test
  void listTemperatureUnitsFallsBackToEmailWhenLoggerNameIsBlank() {
    User logger = createUser("temperature-fallback@example.com", "", "");
    Organization organization = createOrganization("Kontrolla Temperature Display");
    Establishment establishment = createEstablishment(organization, "Kitchen");
    createMembership(organization, logger, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit unit = createTemperatureUnit(
        organization,
        establishment,
        "Sushi prep fridge",
        "Hot kitchen",
        TemperatureUnitType.FRIDGE,
        LocalTime.of(8, 30),
        new BigDecimal("2.00"),
        new BigDecimal("4.00")
    );
    unit.addLog(new TemperatureLog(
        Instant.parse("2026-04-09T06:10:00Z"),
        new BigDecimal("3.20"),
        "Opening check completed.",
        logger
    ));
    temperatureUnitRepository.saveAndFlush(unit);

    List<TemperatureUnitView> units = temperatureService.listTemperatureUnits(
        organization.getId(),
        establishment.getId(),
        currentUser(logger)
    );

    assertThat(units).hasSize(1);
    assertThat(units.getFirst().logs().getFirst().loggedByName()).isEqualTo("temperature-fallback@example.com");
  }

  private User createUser(String email, String firstName, String lastName) {
    User user = new User(
        email,
        firstName,
        lastName,
        passwordEncoder.encode("password123"),
        true,
        Set.of()
    );
    return userRepository.saveAndFlush(user);
  }

  private Organization createOrganization(String name) {
    return organizationRepository.saveAndFlush(new Organization(name, OrganizationStatus.ACTIVE));
  }

  private Establishment createEstablishment(Organization organization, String name) {
    return establishmentRepository.saveAndFlush(new Establishment(
        organization,
        name,
        EstablishmentType.RESTAURANT,
        EstablishmentStatus.ACTIVE
    ));
  }

  private void createMembership(Organization organization, User user, OrganizationRole role, boolean active) {
    organizationMembershipRepository.saveAndFlush(new OrganizationMembership(organization, user, role, active));
  }

  private TemperatureUnit createTemperatureUnit(
      Organization organization,
      Establishment establishment,
      String name,
      String location,
      TemperatureUnitType type,
      LocalTime dueByTime,
      BigDecimal minimumTemperature,
      BigDecimal maximumTemperature
  ) {
    return new TemperatureUnit(
        organization,
        establishment,
        name,
        location,
        type,
        dueByTime,
        minimumTemperature,
        maximumTemperature
    );
  }

  private CurrentUser currentUser(User user) {
    return new CurrentUser(user.getId(), user.getEmail(), Set.of());
  }
}
