package org.kontrolla.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.deviations.application.DeviationService;
import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.notifications.domain.NotificationType;
import org.kontrolla.notifications.infrastructure.NotificationRepository;
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

@SpringBootTest
@ActiveProfiles("test")
class DeviationNotificationIntegrationTest {

  @Autowired private DeviationService deviationService;

  @Autowired private NotificationRepository notificationRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private OrganizationMembershipRepository organizationMembershipRepository;

  @Autowired private EstablishmentRepository establishmentRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void deviationAssignmentStatusAndNotesNotifyTheAssignedUser() {
    User manager = createUser("deviation-notify-manager@example.com");
    User employee = createUser("deviation-notify-employee@example.com");
    Organization organization = createOrganization("Deviation Notification Org");
    Establishment establishment =
        createEstablishment(organization, "Deviation Notification Restaurant");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

    Deviation deviation =
        deviationService.createDeviation(
            currentUser(manager),
            "Walk-in fridge too warm",
            "Morning check recorded 10C in the walk-in fridge.",
            DeviationCategory.TEMPERATURE,
            DeviationSeverity.HIGH,
            organization.getId(),
            establishment.getId());

    deviationService.assignDeviation(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        employee.getId(),
        currentUser(manager));
    deviationService.updateDeviationStatus(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        DeviationStatus.IN_PROGRESS,
        currentUser(manager));
    deviationService.addTimelineNote(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        "Thermostat recalibrated and follow-up temperature check passed.",
        currentUser(manager));

    List<org.kontrolla.notifications.domain.Notification> notifications =
        notificationRepository.findAll().stream()
            .sorted(
                Comparator.comparing(org.kontrolla.notifications.domain.Notification::getCreatedAt))
            .toList();

    assertThat(notifications)
        .extracting(notification -> notification.getType())
        .containsExactly(
            NotificationType.DEVIATION_ASSIGNED,
            NotificationType.DEVIATION_STATUS_CHANGED,
            NotificationType.DEVIATION_NOTE_ADDED);
    assertThat(notifications)
        .allSatisfy(
            notification -> {
              assertThat(notification.getRecipientUserId()).isEqualTo(employee.getId());
              assertThat(notification.getResourceId()).isEqualTo(deviation.getId());
            });
  }

  @Test
  void deviationNotificationsDoNotNotifyTheActorAboutTheirOwnActions() {
    User manager = createUser("deviation-self-manager@example.com");
    Organization organization = createOrganization("Deviation Self Org");
    Establishment establishment = createEstablishment(organization, "Deviation Self Restaurant");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER);

    Deviation deviation =
        deviationService.createDeviation(
            currentUser(manager),
            "Door policy issue",
            "Shift lead needs to document the issue.",
            DeviationCategory.AGE_CONTROL,
            DeviationSeverity.MEDIUM,
            organization.getId(),
            establishment.getId());

    deviationService.assignDeviation(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        manager.getId(),
        currentUser(manager));
    deviationService.updateDeviationStatus(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        DeviationStatus.IN_PROGRESS,
        currentUser(manager));
    deviationService.addTimelineNote(
        organization.getId(),
        establishment.getId(),
        deviation.getId(),
        "Manager followed up directly.",
        currentUser(manager));

    assertThat(notificationRepository.count()).isZero();
  }

  private User createUser(String email) {
    User user =
        new User(email, "Test", "User", passwordEncoder.encode("password123"), true, Set.of());
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

  private OrganizationMembership createMembership(
      Organization organization, User user, OrganizationRole role) {
    OrganizationMembership membership = new OrganizationMembership(organization, user, role, true);
    return organizationMembershipRepository.saveAndFlush(membership);
  }

  private CurrentUser currentUser(User user) {
    return new CurrentUser(user.getId(), user.getEmail(), Set.<GlobalRole>of());
  }
}
