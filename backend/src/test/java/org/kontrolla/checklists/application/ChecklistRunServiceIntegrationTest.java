package org.kontrolla.checklists.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistResponseType;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ChecklistRunServiceIntegrationTest {

	@Autowired
	private ChecklistDefinitionService checklistDefinitionService;

	@Autowired
	private ChecklistRunService checklistRunService;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private ChecklistDefinitionRepository checklistDefinitionRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		checklistRunRepository.deleteAll();
		checklistDefinitionRepository.deleteAll();
		organizationMembershipRepository.deleteAll();
		establishmentRepository.deleteAll();
		organizationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void managerCanAssignAndEmployeeCanCompleteChecklistRun() {
		User manager = createUser("manager@example.com");
		User employee = createUser("employee@example.com");
		Organization organization = createOrganization("Kontrolla");
		Establishment establishment = createEstablishment(organization, "Sushi Oslo");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		ChecklistRun assignedRun = checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(employee.getId()),
				currentUser(manager)
		);

		assertThat(assignedRun.getAssignments()).hasSize(1);
		assertThat(assignedRun.getEvents())
				.extracting(ChecklistRunEvent::getEventType)
				.contains(ChecklistRunEventType.ASSIGNED);

		ChecklistRun startedRun = checklistRunService.startChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				currentUser(employee)
		);

		assertThat(startedRun.getStatus()).isEqualTo(ChecklistRunStatus.IN_PROGRESS);
		assertThat(startedRun.getStartedAt()).isNotNull();

		UUID runItemId = startedRun.getRunItems().getFirst().getId();
		ChecklistRun completedRun = checklistRunService.submitChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(new ChecklistRunService.ChecklistItemSubmissionInput(
						runItemId,
						Boolean.TRUE,
						null,
						null,
						"Completed during opening shift"
				)),
				currentUser(employee)
		);

		assertThat(completedRun.getStatus()).isEqualTo(ChecklistRunStatus.COMPLETED);
		assertThat(completedRun.getCompletedByUser().getId()).isEqualTo(employee.getId());
		assertThat(completedRun.getEvents())
				.extracting(ChecklistRunEvent::getEventType)
				.contains(ChecklistRunEventType.STARTED, ChecklistRunEventType.COMPLETED);

		ChecklistRun reopenedRun = checklistRunService.reopenChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				currentUser(manager)
		);

		assertThat(reopenedRun.getStatus()).isEqualTo(ChecklistRunStatus.PENDING);
		assertThat(reopenedRun.getCompletedAt()).isNull();
		assertThat(reopenedRun.getCompletedByUser()).isNull();

		ChecklistRun cancelledRun = checklistRunService.cancelChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				currentUser(manager)
		);

		assertThat(cancelledRun.getStatus()).isEqualTo(ChecklistRunStatus.CANCELLED);
		assertThat(cancelledRun.getEvents())
				.extracting(ChecklistRunEvent::getEventType)
				.contains(ChecklistRunEventType.REOPENED, ChecklistRunEventType.CANCELLED);
	}

	@Test
	void unassignedEmployeeCannotStartAssignedChecklistRun() {
		User manager = createUser("manager2@example.com");
		User assignedEmployee = createUser("assigned@example.com");
		User outsiderEmployee = createUser("outsider@example.com");
		Organization organization = createOrganization("Kontrolla Access");
		Establishment establishment = createEstablishment(organization, "Sushi Bergen");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, assignedEmployee, OrganizationRole.ORG_EMPLOYEE);
		createMembership(organization, outsiderEmployee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(assignedEmployee.getId()),
				currentUser(manager)
		);

		assertThatThrownBy(() -> checklistRunService.startChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				currentUser(outsiderEmployee)
		)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void checklistRunsCanOnlyBeAssignedToActiveOrganizationMembers() {
		User manager = createUser("manager3@example.com");
		User outsider = createUser("outsider-no-membership@example.com");
		Organization organization = createOrganization("Kontrolla Membership");
		Establishment establishment = createEstablishment(organization, "Sushi Trondheim");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		assertThatThrownBy(() -> checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(outsider.getId()),
				currentUser(manager)
		)).isInstanceOf(ForbiddenException.class);
	}

	private ChecklistDefinition createDefinition(Organization organization, Establishment establishment, User manager) {
		return checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Opening routine",
				List.of(new ChecklistDefinitionService.ChecklistItemInput(
						"Check fridge temperature",
						"Record the opening fridge reading",
						ChecklistResponseType.BOOLEAN,
						true,
						0
				)),
				List.of(),
				currentUser(manager)
		);
	}

	private ChecklistRun createRun(ChecklistDefinition definition, Establishment establishment, User manager) {
		ChecklistRun run = new ChecklistRun(
				definition,
				definition.getDefinitionGroupId(),
				establishment,
				definition.getServiceArea(),
				definition.getTitle(),
				definition.getDescription(),
				Instant.parse("2026-03-26T08:00:00Z"),
				ChecklistRunStatus.PENDING,
				manager
		);
		run.snapshotItemsFromDefinition(definition.getItems());
		return checklistRunRepository.saveAndFlush(run);
	}

	private User createUser(String email) {
		User user = new User(email, "Test", "User", "hashed-password", true, Set.of());
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

	private void createMembership(Organization organization, User user, OrganizationRole role) {
		OrganizationMembership membership = new OrganizationMembership(organization, user, role, true);
		organizationMembershipRepository.saveAndFlush(membership);
	}

	private CurrentUser currentUser(User user) {
		return new CurrentUser(user.getId(), user.getEmail(), user.getGlobalRoles());
	}
}
