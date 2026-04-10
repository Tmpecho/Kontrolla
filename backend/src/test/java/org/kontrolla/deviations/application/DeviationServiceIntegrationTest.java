package org.kontrolla.deviations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationEvent;
import org.kontrolla.deviations.domain.DeviationEventType;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DeviationServiceIntegrationTest {

  @Autowired private DeviationService deviationService;

  @Autowired private DeviationRepository deviationRepository;

  @Autowired private ChecklistRunRepository checklistRunRepository;

  @Autowired private ChecklistDefinitionRepository checklistDefinitionRepository;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private EstablishmentRepository establishmentRepository;

  @Autowired private OrganizationMembershipRepository organizationMembershipRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void employeeCanCreateDeviationAndManagerFollowUpActionsAppendTimelineEvents() {
    User manager = createUser("manager@example.com", "Manager", "User", true);
    User reporter = createUser("reporter@example.com", "Reporter", "User", true);
    User assignee = createUser("assignee@example.com", "Assignee", "User", true);
    Organization organization = createOrganization("Kontrolla Deviations");
    Establishment establishment = createEstablishment(organization, "Kitchen");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organization, reporter, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organization, assignee, OrganizationRole.ORG_EMPLOYEE, true);

    Deviation createdDeviation =
        deviationService.createDeviation(
            currentUser(reporter),
            "Cold storage issue",
            "Walk-in fridge measured 11C during opening checks.",
            DeviationCategory.TEMPERATURE,
            DeviationSeverity.HIGH,
            organization.getId(),
            establishment.getId());

    deviationService.assignDeviation(
        organization.getId(),
        establishment.getId(),
        createdDeviation.getId(),
        assignee.getId(),
        currentUser(manager));
    deviationService.updateDeviationStatus(
        organization.getId(),
        establishment.getId(),
        createdDeviation.getId(),
        DeviationStatus.IN_PROGRESS,
        currentUser(manager));
    deviationService.updateDeviationDetails(
        organization.getId(),
        establishment.getId(),
        createdDeviation.getId(),
        "Cold storage temperature deviation",
        "Walk-in fridge measured 11C and products were moved to backup cooling.",
        DeviationSeverity.CRITICAL,
        DeviationCategory.STORAGE,
        currentUser(manager));
    deviationService.addTimelineNote(
        organization.getId(),
        establishment.getId(),
        createdDeviation.getId(),
        "Thermostat recalibrated and temperature rechecked.",
        currentUser(manager));

    Deviation reloadedDeviation =
        deviationService.getDeviation(
            organization.getId(),
            establishment.getId(),
            createdDeviation.getId(),
            currentUser(manager));

    assertThat(reloadedDeviation.getCreatedByUser().getId()).isEqualTo(reporter.getId());
    assertThat(reloadedDeviation.getAssignedToUser().getId()).isEqualTo(assignee.getId());
    assertThat(reloadedDeviation.getStatus()).isEqualTo(DeviationStatus.IN_PROGRESS);
    assertThat(reloadedDeviation.getSeverity()).isEqualTo(DeviationSeverity.CRITICAL);
    assertThat(reloadedDeviation.getCategory()).isEqualTo(DeviationCategory.STORAGE);
    assertThat(reloadedDeviation.getTitle()).isEqualTo("Cold storage temperature deviation");
    assertThat(reloadedDeviation.getEvents())
        .extracting(DeviationEvent::getEventType)
        .containsExactly(
            DeviationEventType.REPORTED,
            DeviationEventType.ASSIGNED,
            DeviationEventType.STATUS_CHANGED,
            DeviationEventType.DETAILS_UPDATED,
            DeviationEventType.NOTE_ADDED);
    assertThat(
            reloadedDeviation.getEvents().get(reloadedDeviation.getEvents().size() - 1).getNote())
        .isEqualTo("Thermostat recalibrated and temperature rechecked.");
  }

  @Test
  void deviationsCanOnlyBeAssignedToActiveOrganizationMembers() {
    User manager = createUser("manager2@example.com", "Manager", "User", true);
    User outsider = createUser("outsider@example.com", "Outsider", "User", true);
    Organization organization = createOrganization("Kontrolla Assignment Rules");
    Establishment establishment = createEstablishment(organization, "Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    Deviation deviation =
        deviationService.createDeviation(
            currentUser(manager),
            "Missing cleaning log",
            "No cleaning signature found for the closing shift.",
            DeviationCategory.HYGIENE,
            DeviationSeverity.MEDIUM,
            organization.getId(),
            establishment.getId());

    assertThatThrownBy(
            () ->
                deviationService.assignDeviation(
                    organization.getId(),
                    establishment.getId(),
                    deviation.getId(),
                    outsider.getId(),
                    currentUser(manager)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void repeatingSameAssignmentStatusAndDetailsDoesNotAppendDuplicateEvents() {
    User manager = createUser("manager3@example.com", "Manager", "User", true);
    User assignee = createUser("assignee3@example.com", "Assignee", "User", true);
    Organization organization = createOrganization("Kontrolla Idempotency");
    Establishment establishment = createEstablishment(organization, "Main Kitchen");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organization, assignee, OrganizationRole.ORG_EMPLOYEE, true);

    Deviation deviation =
        deviationService.createDeviation(
            currentUser(manager),
            "  Thermometer calibration missing  ",
            "  Calibration record was not available during inspection.  ",
            DeviationCategory.DOCUMENTATION_AND_TRAINING,
            DeviationSeverity.MEDIUM,
            organization.getId(),
            establishment.getId());

    deviationService.assignDeviation(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        assignee.getId(),
        currentUser(manager));
    deviationService.assignDeviation(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        assignee.getId(),
        currentUser(manager));
    deviationService.updateDeviationStatus(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        DeviationStatus.IN_PROGRESS,
        currentUser(manager));
    deviationService.updateDeviationStatus(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        DeviationStatus.IN_PROGRESS,
        currentUser(manager));
    deviationService.updateDeviationDetails(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        "  Thermometer calibration missing  ",
        "  Calibration record was not available during inspection.  ",
        DeviationSeverity.MEDIUM,
        DeviationCategory.DOCUMENTATION_AND_TRAINING,
        currentUser(manager));

    Deviation reloadedDeviation =
        deviationService.getDeviation(
            organization.getId(), establishment.getId(), deviation.getId(), currentUser(manager));

    assertThat(reloadedDeviation.getEvents())
        .extracting(DeviationEvent::getEventType)
        .containsExactly(
            DeviationEventType.REPORTED,
            DeviationEventType.ASSIGNED,
            DeviationEventType.STATUS_CHANGED);
    assertThat(reloadedDeviation.getTitle()).isEqualTo("Thermometer calibration missing");
    assertThat(reloadedDeviation.getDescription())
        .isEqualTo("Calibration record was not available during inspection.");
  }

  @Test
  void timelineNotesAreTrimmedBeforeBeingStored() {
    User manager = createUser("manager4@example.com", "Manager", "User", true);
    Organization organization = createOrganization("Kontrolla Trimmed Notes");
    Establishment establishment = createEstablishment(organization, "Prep Room");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    Deviation deviation =
        deviationService.createDeviation(
            currentUser(manager),
            "Open chemical container",
            "Cleaning chemical container was stored without a lid.",
            DeviationCategory.STORAGE,
            DeviationSeverity.HIGH,
            organization.getId(),
            establishment.getId());

    deviationService.addTimelineNote(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        "   Container sealed and returned to marked shelf.   ",
        currentUser(manager));

    Deviation reloadedDeviation =
        deviationService.getDeviation(
            organization.getId(), establishment.getId(), deviation.getId(), currentUser(manager));

    assertThat(reloadedDeviation.getEvents()).hasSize(2);
    assertThat(reloadedDeviation.getEvents().getLast().getNote())
        .isEqualTo("Container sealed and returned to marked shelf.");
  }

  private User createUser(String email, String firstName, String lastName, boolean active) {
    User user = new User(email, firstName, lastName, "hashed-password", active, Set.of());
    return userRepository.saveAndFlush(user);
  }

  private Organization createOrganization(String name) {
    Organization organization = new Organization(name, OrganizationStatus.ACTIVE);
    return organizationRepository.saveAndFlush(organization);
  }

  private Establishment createEstablishment(Organization organization, String name) {
    Establishment establishment =
        new Establishment(
            organization, name, EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE);
    return establishmentRepository.saveAndFlush(establishment);
  }

  private void createMembership(
      Organization organization, User user, OrganizationRole role, boolean active) {
    OrganizationMembership membership =
        new OrganizationMembership(organization, user, role, active);
    organizationMembershipRepository.saveAndFlush(membership);
  }

  private CurrentUser currentUser(User user) {
    return new CurrentUser(user.getId(), user.getEmail(), user.getGlobalRoles());
  }
}
