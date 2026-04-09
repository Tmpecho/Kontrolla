package org.kontrolla.checklists.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.api.UpdateChecklistTaskRequest;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.kontrolla.checklists.domain.ChecklistRunEventType;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskExecutionStatus;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.domain.ChecklistVerificationResult;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.common.exception.ConflictException;
import org.kontrolla.common.exception.ForbiddenException;
import org.kontrolla.common.exception.ResourceNotFoundException;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
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

		UUID taskExecutionId = startedRun.getTaskExecutions().iterator().next().getId();
		ChecklistRun completedRun = checklistRunService.submitChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(new ChecklistRunService.ChecklistTaskExecutionInput(
						taskExecutionId,
						ChecklistTaskExecutionStatus.COMPLETED,
						"Completed during opening shift",
						null,
						null,
						null
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
	void removingAssignmentDoesNotRestoreItWhenAnotherUserIsAssignedLater() {
		User manager = createUser("manager-reassign@example.com");
		User removedEmployee = createUser("removed-employee@example.com");
		User addedEmployee = createUser("added-employee@example.com");
		Organization organization = createOrganization("Kontrolla Reassign");
		Establishment establishment = createEstablishment(organization, "Sushi Reassign");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, removedEmployee, OrganizationRole.ORG_EMPLOYEE);
		createMembership(organization, addedEmployee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		ChecklistRun initiallyAssignedRun = checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(removedEmployee.getId()),
				currentUser(manager)
		);

		UUID assignmentId = initiallyAssignedRun.getAssignments().iterator().next().getId();
		checklistRunService.removeChecklistRunAssignment(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				assignmentId,
				currentUser(manager)
		);

		ChecklistRun reassignedRun = checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(addedEmployee.getId()),
				currentUser(manager)
		);

		assertThat(reassignedRun.getAssignments())
				.extracting(assignment -> assignment.getAssignedUser().getId())
				.containsExactly(addedEmployee.getId());
	}

	@Test
	void updatingTheLastRequiredTaskMarksRunCompletedWithActorAndEvent() {
		User manager = createUser("manager-update@example.com");
		User employee = createUser("employee-update@example.com");
		Organization organization = createOrganization("Kontrolla Update");
		Establishment establishment = createEstablishment(organization, "Sushi Update");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		ChecklistRun updatedRun = checklistRunService.updateChecklistTask(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				run.getTaskExecutions().iterator().next().getId(),
				new UpdateChecklistTaskRequest(
						ChecklistTaskExecutionStatus.COMPLETED,
						"Completed inline",
						null,
						null,
						null
				),
				currentUser(employee)
		);

		assertThat(updatedRun.getStatus()).isEqualTo(ChecklistRunStatus.COMPLETED);
		assertThat(updatedRun.getCompletedAt()).isNotNull();
		assertThat(updatedRun.getCompletedByUser()).isNotNull();
		assertThat(updatedRun.getCompletedByUser().getId()).isEqualTo(employee.getId());
		assertThat(updatedRun.getEvents())
				.extracting(ChecklistRunEvent::getEventType)
				.contains(ChecklistRunEventType.COMPLETED);
	}

	@Test
	void updatingAnActionTaskWithVerificationDataIsRejected() {
		User manager = createUser("manager-invalid-update@example.com");
		User employee = createUser("employee-invalid-update@example.com");
		Organization organization = createOrganization("Kontrolla Invalid Update");
		Establishment establishment = createEstablishment(organization, "Sushi Invalid Update");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		assertThatThrownBy(() -> checklistRunService.updateChecklistTask(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				run.getTaskExecutions().iterator().next().getId(),
				new UpdateChecklistTaskRequest(
						ChecklistTaskExecutionStatus.COMPLETED,
						"Attempted invalid inline update",
						ChecklistVerificationResult.VERIFIED,
						null,
						null
				),
				currentUser(employee)
		)).isInstanceOf(ConflictException.class)
				.hasMessageContaining("Action checklist tasks may only record completion");
	}

	@Test
	void updatingACompletedRunReturnsConflictException() {
		User manager = createUser("manager-completed-update@example.com");
		User employee = createUser("employee-completed-update@example.com");
		Organization organization = createOrganization("Kontrolla Completed Update");
		Establishment establishment = createEstablishment(organization, "Sushi Completed Update");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		ChecklistRun completedRun = checklistRunService.updateChecklistTask(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				run.getTaskExecutions().iterator().next().getId(),
				new UpdateChecklistTaskRequest(
						ChecklistTaskExecutionStatus.COMPLETED,
						"Completed before invalid update",
						null,
						null,
						null
				),
				currentUser(employee)
		);

		assertThatThrownBy(() -> checklistRunService.updateChecklistTask(
				organization.getId(),
				establishment.getId(),
				completedRun.getId(),
				completedRun.getTaskExecutions().iterator().next().getId(),
				new UpdateChecklistTaskRequest(
						ChecklistTaskExecutionStatus.COMPLETED,
						"Attempted update after completion",
						null,
						null,
						null
				),
				currentUser(employee)
		)).isInstanceOfSatisfying(ConflictException.class, exception -> {
			assertThat(exception.getCode()).isEqualTo("checklist_run_update_invalid_state");
			assertThat(exception.getMessage()).isEqualTo("Completed or cancelled checklist runs cannot be updated");
		});
	}

	@Test
	void updatingWithTaskThatDoesNotBelongToRunReturnsNotFoundException() {
		User manager = createUser("manager-missing-task@example.com");
		User employee = createUser("employee-missing-task@example.com");
		Organization organization = createOrganization("Kontrolla Missing Task");
		Establishment establishment = createEstablishment(organization, "Sushi Missing Task");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		assertThatThrownBy(() -> checklistRunService.updateChecklistTask(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				UUID.randomUUID(),
				new UpdateChecklistTaskRequest(
						ChecklistTaskExecutionStatus.COMPLETED,
						"Attempted update with missing task",
						null,
						null,
						null
				),
				currentUser(employee)
		)).isInstanceOfSatisfying(ResourceNotFoundException.class, exception -> {
			assertThat(exception.getCode()).isEqualTo("checklist_task_execution_not_found");
			assertThat(exception.getMessage()).isEqualTo("Checklist task execution not found");
		});
	}

	@Test
	void resetClearsTaskExecutionState() {
		User manager = createUser("manager-reset@example.com");
		User employee = createUser("employee-reset@example.com");
		Organization organization = createOrganization("Kontrolla Reset");
		Establishment establishment = createEstablishment(organization, "Sushi Reset");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Measurement reset",
				"Reset routine",
				List.of(new ChecklistDefinitionService.ChecklistTaskInput(
						"Measure sanitizer concentration",
						"Record ppm value",
						ChecklistTaskKind.MEASUREMENT,
						true,
						0,
						"ppm",
						new BigDecimal("200"),
						new BigDecimal("400")
				)),
				List.of(),
				currentUser(manager)
		);
		ChecklistRun run = createRun(definition, establishment, manager);
		UUID taskId = run.getTaskExecutions().iterator().next().getId();

		ChecklistRun inProgressRun = checklistRunService.updateChecklistTask(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				taskId,
				new UpdateChecklistTaskRequest(
						ChecklistTaskExecutionStatus.PENDING,
						"Warming up",
						null,
						null,
						null
				),
				currentUser(employee)
		);

		ChecklistRun resetRun = checklistRunService.resetChecklistRun(
				organization.getId(),
				establishment.getId(),
				inProgressRun.getId(),
				currentUser(employee)
		);

		assertThat(resetRun.getStatus()).isEqualTo(ChecklistRunStatus.PENDING);
		assertThat(resetRun.getStartedAt()).isNull();
		assertThat(resetRun.getTaskExecutions()).singleElement().satisfies(taskExecution -> {
			assertThat(taskExecution.getExecutionStatus()).isEqualTo(ChecklistTaskExecutionStatus.PENDING);
			assertThat(taskExecution.getComment()).isNull();
			assertThat(taskExecution.getVerificationResult()).isNull();
			assertThat(taskExecution.getMeasuredValue()).isNull();
			assertThat(taskExecution.getEnteredText()).isNull();
			assertThat(taskExecution.getResolvedAt()).isNull();
			assertThat(taskExecution.getResolvedByUser()).isNull();
		});
	}

	@Test
	void resettingRunOutsideInProgressReturnsConflictException() {
		User manager = createUser("manager-reset-invalid@example.com");
		User employee = createUser("employee-reset-invalid@example.com");
		Organization organization = createOrganization("Kontrolla Invalid Reset");
		Establishment establishment = createEstablishment(organization, "Sushi Invalid Reset");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);

		assertThatThrownBy(() -> checklistRunService.resetChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				currentUser(employee)
		)).isInstanceOfSatisfying(ConflictException.class, exception -> {
			assertThat(exception.getCode()).isEqualTo("checklist_run_reset_invalid_state");
			assertThat(exception.getMessage()).isEqualTo("Only in-progress checklist runs can be reset");
		});
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

	@Test
	void verificationTaskCanBeCompletedEvenWhenResultIsNegative() {
		User manager = createUser("manager4@example.com");
		User employee = createUser("employee4@example.com");
		Organization organization = createOrganization("Kontrolla Verification");
		Establishment establishment = createEstablishment(organization, "Sushi Stavanger");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning verification",
				"Opening verification routine",
				List.of(new ChecklistDefinitionService.ChecklistTaskInput(
						"Verify handwash station is stocked",
						"Confirm soap and paper are available",
						ChecklistTaskKind.VERIFICATION,
						true,
						0,
						null,
						null,
						null
				)),
				List.of(),
				currentUser(manager)
		);
		ChecklistRun run = createRun(definition, establishment, manager);

		checklistRunService.assignChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(employee.getId()),
				currentUser(manager)
		);

		ChecklistRun submittedRun = checklistRunService.submitChecklistRun(
				organization.getId(),
				establishment.getId(),
				run.getId(),
				List.of(new ChecklistRunService.ChecklistTaskExecutionInput(
						run.getTaskExecutions().iterator().next().getId(),
						ChecklistTaskExecutionStatus.COMPLETED,
						"Soap was missing",
						ChecklistVerificationResult.NOT_VERIFIED,
						null,
						null
				)),
				currentUser(employee)
		);

		assertThat(submittedRun.getStatus()).isEqualTo(ChecklistRunStatus.COMPLETED);
		assertThat(submittedRun.getTaskExecutions()).singleElement().satisfies(taskExecution -> {
			assertThat(taskExecution.getExecutionStatus()).isEqualTo(ChecklistTaskExecutionStatus.COMPLETED);
			assertThat(taskExecution.getVerificationResult()).isEqualTo(ChecklistVerificationResult.NOT_VERIFIED);
			assertThat(taskExecution.getComment()).isEqualTo("Soap was missing");
		});
	}

	private ChecklistDefinition createDefinition(Organization organization, Establishment establishment, User manager) {
		return checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Opening routine",
				List.of(new ChecklistDefinitionService.ChecklistTaskInput(
						"Prepare oven for first shift",
						"Switch on the oven and verify it is heating",
						ChecklistTaskKind.ACTION,
						true,
						0,
						null,
						null,
						null
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
		run.snapshotTasksFromDefinition(definition.getTasks());
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
