package org.kontrolla.notifications.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.application.ChecklistDefinitionService;
import org.kontrolla.checklists.application.ChecklistRunService;
import org.kontrolla.checklists.application.ChecklistSchedulerService;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChecklistNotificationIntegrationTest {

	@Autowired
	private ChecklistDefinitionService checklistDefinitionService;

	@Autowired
	private ChecklistRunService checklistRunService;

	@Autowired
	private ChecklistSchedulerService checklistSchedulerService;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void assigningChecklistRunCreatesNotificationsForNewAssigneesOnly() {
		User manager = createUser("checklist-notify-manager@example.com");
		User employee = createUser("checklist-notify-employee@example.com");
		Organization organization = createOrganization("Checklist Notification Org");
		Establishment establishment = createEstablishment(organization, "Checklist Notification Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager, Instant.parse("2026-04-07T09:00:00Z"), ChecklistRunStatus.PENDING);

		checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(employee.getId(), manager.getId()),
				currentUser(manager)
		);

		assertThat(notificationRepository.findAll())
				.singleElement()
				.satisfies(notification -> {
					assertThat(notification.getRecipientUserId()).isEqualTo(employee.getId());
					assertThat(notification.getType()).isEqualTo(NotificationType.CHECKLIST_ASSIGNED);
					assertThat(notification.getResourceId()).isEqualTo(run.getId());
				});

		checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(employee.getId()),
				currentUser(manager)
		);

		assertThat(notificationRepository.count()).isEqualTo(1);
	}

	@Test
	void markingAssignedChecklistRunOverdueCreatesOneNotificationPerAssigneeOnce() {
		User manager = createUser("checklist-overdue-manager@example.com");
		User employee = createUser("checklist-overdue-employee@example.com");
		Organization organization = createOrganization("Checklist Overdue Org");
		Establishment establishment = createEstablishment(organization, "Checklist Overdue Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager, Instant.parse("2026-04-07T07:00:00Z"), ChecklistRunStatus.PENDING);
		checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(employee.getId()),
				currentUser(manager)
		);

		notificationRepository.deleteAll();

		int updatedRuns = checklistSchedulerService.markOverdueRuns(
				organization.getId(),
				establishment.getId(),
				Instant.parse("2026-04-07T08:30:00Z"),
				currentUser(manager)
		);

		assertThat(updatedRuns).isEqualTo(1);
		assertThat(notificationRepository.findAll())
				.singleElement()
				.satisfies(notification -> {
					assertThat(notification.getRecipientUserId()).isEqualTo(employee.getId());
					assertThat(notification.getType()).isEqualTo(NotificationType.CHECKLIST_OVERDUE);
					assertThat(notification.getResourceId()).isEqualTo(run.getId());
				});

		int secondRunUpdate = checklistSchedulerService.markOverdueRuns(
				organization.getId(),
				establishment.getId(),
				Instant.parse("2026-04-07T09:00:00Z"),
				currentUser(manager)
		);

		assertThat(secondRunUpdate).isZero();
		assertThat(notificationRepository.count()).isEqualTo(1);
	}

	private ChecklistDefinition createDefinition(Organization organization, Establishment establishment, User actor) {
		return checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Opening routine",
				List.of(new ChecklistDefinitionService.ChecklistTaskInput(
						"Check fridge temperature",
						"Record the opening fridge reading",
						ChecklistTaskKind.ACTION,
						true,
						0,
						null,
						null,
						null
				)),
				List.of(new ChecklistDefinitionService.ChecklistScheduleInput(
						org.kontrolla.checklists.domain.ChecklistScheduleType.DAILY,
						LocalDate.parse("2026-04-01"),
						null,
						null,
						null,
						null,
						"Europe/Oslo",
						true
				)),
				currentUser(actor)
		);
	}

	private ChecklistRun createRun(
			ChecklistDefinition definition,
			Establishment establishment,
			User actor,
			Instant dueAt,
			ChecklistRunStatus status
	) {
		ChecklistRun run = new ChecklistRun(
				definition,
				definition.getDefinitionGroupId(),
				establishment,
				definition.getServiceArea(),
				definition.getTitle(),
				definition.getDescription(),
				dueAt,
				status,
				actor
		);
		run.snapshotTasksFromDefinition(definition.getTasks());
		return checklistRunRepository.saveAndFlush(run);
	}

	private User createUser(String email) {
		User user = new User(
				email,
				"Test",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of()
		);
		return userRepository.saveAndFlush(user);
	}

	private Organization createOrganization(String name) {
		Organization organization = new Organization(name, OrganizationStatus.ACTIVE);
		return organizationRepository.saveAndFlush(organization);
	}

	private Establishment createEstablishment(Organization organization, String name) {
		Establishment establishment = new Establishment(
				organization,
				name,
				EstablishmentType.RESTAURANT,
				EstablishmentStatus.ACTIVE
		);
		return establishmentRepository.saveAndFlush(establishment);
	}

	private OrganizationMembership createMembership(
			Organization organization,
			User user,
			OrganizationRole role
	) {
		OrganizationMembership membership = new OrganizationMembership(organization, user, role, true);
		return organizationMembershipRepository.saveAndFlush(membership);
	}

	private CurrentUser currentUser(User user) {
		return new CurrentUser(user.getId(), user.getEmail(), Set.of());
	}
}
