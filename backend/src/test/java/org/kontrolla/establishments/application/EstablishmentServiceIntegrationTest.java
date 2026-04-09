package org.kontrolla.establishments.application;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class EstablishmentServiceIntegrationTest {

  @Autowired
  private EstablishmentService establishmentService;

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
  void managerCanUpdateAndReadServingHoursForAllDays() {
    User manager = createUser("serving-hours-manager@example.com");
    Organization organization = createOrganization("Serving Hours Org");
    Establishment establishment = createEstablishment(organization, "Downtown Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    List<UpdateServingHoursDayCommand> commands = weeklySchedule(
        LocalTime.of(10, 0),
        LocalTime.of(22, 0)
    );

    List<ServingHoursDayView> updated = establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        commands,
        currentUser(manager)
    );

    List<ServingHoursDayView> reloaded = establishmentService.getServingHours(
        organization.getId(),
        establishment.getId(),
        currentUser(manager)
    );

    assertThat(updated).hasSize(7);
    assertThat(updated)
        .extracting(ServingHoursDayView::dayOfWeek)
        .containsExactly(DayOfWeek.values());

    assertThat(reloaded).hasSize(7);
    assertThat(reloaded).allSatisfy(day -> {
      assertThat(day.closed()).isFalse();
      assertThat(day.opensAt()).isEqualTo(LocalTime.of(10, 0));
      assertThat(day.closesAt()).isEqualTo(LocalTime.of(22, 0));
    });
  }

  @Test
  void getServingHoursReturnsClosedDaysByDefaultWhenNothingIsConfigured() {
    User employee = createUser("serving-hours-default@example.com");
    Organization organization = createOrganization("Serving Hours Default Org");
    Establishment establishment = createEstablishment(organization, "Quiet Cafe");
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);

    List<ServingHoursDayView> servingHours = establishmentService.getServingHours(
        organization.getId(),
        establishment.getId(),
        currentUser(employee)
    );

    assertThat(servingHours).hasSize(7);
    assertThat(servingHours)
        .extracting(ServingHoursDayView::dayOfWeek)
        .containsExactly(DayOfWeek.values());
    assertThat(servingHours).allSatisfy(day -> {
      assertThat(day.closed()).isTrue();
      assertThat(day.opensAt()).isNull();
      assertThat(day.closesAt()).isNull();
    });
  }

  @Test
  void employeeCanReadServingHoursButCannotUpdateThem() {
    User manager = createUser("serving-hours-read-manager@example.com");
    User employee = createUser("serving-hours-read-employee@example.com");
    Organization organization = createOrganization("Serving Hours Access Org");
    Establishment establishment = createEstablishment(organization, "Access Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);

    establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        weeklySchedule(LocalTime.of(9, 0), LocalTime.of(21, 0)),
        currentUser(manager)
    );

    List<ServingHoursDayView> visibleToEmployee = establishmentService.getServingHours(
        organization.getId(),
        establishment.getId(),
        currentUser(employee)
    );

    assertThat(visibleToEmployee).hasSize(7);
    assertThatThrownBy(() -> establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        weeklySchedule(LocalTime.of(11, 0), LocalTime.of(23, 0)),
        currentUser(employee)
    )).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void updateServingHoursRejectsMissingDays() {
    User manager = createUser("serving-hours-missing-days@example.com");
    Organization organization = createOrganization("Serving Hours Missing Days Org");
    Establishment establishment = createEstablishment(organization, "Missing Days Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    List<UpdateServingHoursDayCommand> commands = Arrays.stream(DayOfWeek.values())
        .limit(6)
        .map(day -> new UpdateServingHoursDayCommand(day, false, LocalTime.of(10, 0), LocalTime.of(22, 0)))
        .toList();

    assertThatThrownBy(() -> establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        commands,
        currentUser(manager)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Serving hours must include exactly one entry for each day of the week");
  }

  @Test
  void updateServingHoursRejectsDuplicateDays() {
    User manager = createUser("serving-hours-duplicate-days@example.com");
    Organization organization = createOrganization("Serving Hours Duplicate Days Org");
    Establishment establishment = createEstablishment(organization, "Duplicate Days Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    List<UpdateServingHoursDayCommand> commands = List.of(
        new UpdateServingHoursDayCommand(DayOfWeek.MONDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
        new UpdateServingHoursDayCommand(DayOfWeek.MONDAY, false, LocalTime.of(11, 0), LocalTime.of(23, 0)),
        new UpdateServingHoursDayCommand(DayOfWeek.TUESDAY, true, null, null),
        new UpdateServingHoursDayCommand(DayOfWeek.WEDNESDAY, true, null, null),
        new UpdateServingHoursDayCommand(DayOfWeek.THURSDAY, true, null, null),
        new UpdateServingHoursDayCommand(DayOfWeek.FRIDAY, true, null, null),
        new UpdateServingHoursDayCommand(DayOfWeek.SATURDAY, true, null, null)
    );

    assertThatThrownBy(() -> establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        commands,
        currentUser(manager)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Serving hours must not contain duplicate days");
  }

  @Test
  void updateServingHoursRejectsClosedDayWithTimes() {
    User manager = createUser("serving-hours-closed-with-times@example.com");
    Organization organization = createOrganization("Serving Hours Closed Times Org");
    Establishment establishment = createEstablishment(organization, "Closed Times Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    List<UpdateServingHoursDayCommand> commands = new ArrayList<>(closedWeek());
    commands.set(0, new UpdateServingHoursDayCommand(
        DayOfWeek.MONDAY,
        true,
        LocalTime.of(10, 0),
        LocalTime.of(22, 0)
    ));

    assertThatThrownBy(() -> establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        commands,
        currentUser(manager)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Closed days cannot include opening or closing times");
  }

  @Test
  void updateServingHoursRejectsOpenDayMissingOneTime() {
    User manager = createUser("serving-hours-missing-time@example.com");
    Organization organization = createOrganization("Serving Hours Missing Time Org");
    Establishment establishment = createEstablishment(organization, "Missing Time Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    List<UpdateServingHoursDayCommand> commands = new ArrayList<>(closedWeek());
    commands.set(0, new UpdateServingHoursDayCommand(
        DayOfWeek.MONDAY,
        false,
        LocalTime.of(10, 0),
        null
    ));

    assertThatThrownBy(() -> establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        commands,
        currentUser(manager)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Open days must include both opening and closing times");
  }

  @Test
  void updateServingHoursRejectsEqualOpeningAndClosingTimes() {
    User manager = createUser("serving-hours-equal-times@example.com");
    Organization organization = createOrganization("Serving Hours Equal Times Org");
    Establishment establishment = createEstablishment(organization, "Equal Times Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    List<UpdateServingHoursDayCommand> commands = new ArrayList<>(closedWeek());
    commands.set(0, new UpdateServingHoursDayCommand(
        DayOfWeek.MONDAY,
        false,
        LocalTime.of(10, 0),
        LocalTime.of(10, 0)
    ));

    assertThatThrownBy(() -> establishmentService.updateServingHours(
        organization.getId(),
        establishment.getId(),
        commands,
        currentUser(manager)
    ))
        .isInstanceOf(ApplicationException.class)
        .hasMessage("Opening and closing times must differ");
  }

  private List<UpdateServingHoursDayCommand> weeklySchedule(LocalTime opensAt, LocalTime closesAt) {
    return Arrays.stream(DayOfWeek.values())
        .map(day -> new UpdateServingHoursDayCommand(day, false, opensAt, closesAt))
        .toList();
  }

  private List<UpdateServingHoursDayCommand> closedWeek() {
    return Arrays.stream(DayOfWeek.values())
        .map(day -> new UpdateServingHoursDayCommand(day, true, null, null))
        .toList();
  }

  private User createUser(String email) {
    return userRepository.saveAndFlush(new User(
        email,
        "Test",
        "User",
        passwordEncoder.encode("password123"),
        true,
        Set.of()
    ));
  }

  private Organization createOrganization(String name) {
    return organizationRepository.saveAndFlush(new Organization(name, OrganizationStatus.ACTIVE));
  }

  private Establishment createEstablishment(Organization organization, String name) {
    return establishmentRepository.saveAndFlush(new Establishment(
        organization,
        name,
        EstablishmentType.BAR,
        EstablishmentStatus.ACTIVE
    ));
  }

  private void createMembership(Organization organization, User user, OrganizationRole role, boolean active) {
    organizationMembershipRepository.saveAndFlush(new OrganizationMembership(organization, user, role, active));
  }

  private CurrentUser currentUser(User user) {
    return new CurrentUser(user.getId(), user.getEmail(), user.getGlobalRoles());
  }
}