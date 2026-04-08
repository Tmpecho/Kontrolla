package org.kontrolla.checklists.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.application.ChecklistDefinitionService;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunAssignment;
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
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.kontrolla.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChecklistAccessIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private ChecklistDefinitionService checklistDefinitionService;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void organizationEmployeeCannotCreateOrUpdateChecklistDefinitions() throws Exception {
		User manager = createUser("checklist-manager@example.com");
		User employee = createUser("checklist-employee@example.com");
		Organization organization = createOrganization("Checklist Definition Org");
		Establishment establishment = createEstablishment(organization, "Checklist Definition Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		String employeeToken = login("checklist-employee@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/checklists/definitions"
						.formatted(organization.getId(), establishment.getId()))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(validCreateDefinitionPayload()))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/checklists/definitions/%s"
						.formatted(organization.getId(), establishment.getId(), definition.getId()))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(validUpdateDefinitionPayload()))
				.andExpect(status().isForbidden());
	}

	@Test
	void organizationEmployeeCannotAssignChecklistRunsOrFilterAnotherUsersAssignments() throws Exception {
		User manager = createUser("assignment-manager@example.com");
		User employee = createUser("assignment-employee@example.com");
		User coworker = createUser("assignment-coworker@example.com");
		Organization organization = createOrganization("Checklist Assignment Org");
		Establishment establishment = createEstablishment(organization, "Checklist Assignment Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);
		createMembership(organization, coworker, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		String employeeToken = login("assignment-employee@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/checklists/runs/%s/assignments"
						.formatted(organization.getId(), establishment.getId(), run.getId()))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "assignedUserIds": ["%s"]
								}
								""".formatted(coworker.getId())))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/checklists/runs?serviceArea=IK_MAT&assignedUserId=%s"
						.formatted(organization.getId(), establishment.getId(), coworker.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void unassignedEmployeeCannotUpdateAssignedChecklistRunTask() throws Exception {
		User manager = createUser("task-manager@example.com");
		User assignedEmployee = createUser("task-assigned@example.com");
		User outsiderEmployee = createUser("task-outsider@example.com");
		Organization organization = createOrganization("Checklist Task Org");
		Establishment establishment = createEstablishment(organization, "Checklist Task Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, assignedEmployee, OrganizationRole.ORG_EMPLOYEE);
		createMembership(organization, outsiderEmployee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		run.addAssignment(new ChecklistRunAssignment(assignedEmployee, manager, Instant.parse("2026-04-07T08:00:00Z")));
		checklistRunRepository.saveAndFlush(run);

		String outsiderToken = login("task-outsider@example.com", "password123");
		String taskId = run.getTaskExecutions().iterator().next().getId().toString();

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/checklists/runs/%s/tasks/%s"
						.formatted(organization.getId(), establishment.getId(), run.getId(), taskId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "executionStatus": "COMPLETED",
								  "comment": "Attempted completion by the wrong user"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void updatingCompletedChecklistRunTaskReturnsConflictProblemDetails() throws Exception {
		User manager = createUser("completed-task-manager@example.com");
		User employee = createUser("completed-task-employee@example.com");
		Organization organization = createOrganization("Checklist Completed Task Org");
		Establishment establishment = createEstablishment(organization, "Checklist Completed Task Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		String employeeToken = login("completed-task-employee@example.com", "password123");
		String taskId = run.getTaskExecutions().iterator().next().getId().toString();

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/checklists/runs/%s/tasks/%s"
						.formatted(organization.getId(), establishment.getId(), run.getId(), taskId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "executionStatus": "COMPLETED",
								  "comment": "Complete run"
								}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/checklists/runs/%s/tasks/%s"
						.formatted(organization.getId(), establishment.getId(), run.getId(), taskId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "executionStatus": "COMPLETED",
								  "comment": "Try to update completed run"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("checklist_run_update_invalid_state"))
				.andExpect(jsonPath("$.message").value("Completed or cancelled checklist runs cannot be updated"));
	}

	@Test
	void updatingChecklistRunTaskWithUnknownTaskIdReturnsNotFoundProblemDetails() throws Exception {
		User manager = createUser("missing-task-manager@example.com");
		User employee = createUser("missing-task-employee@example.com");
		Organization organization = createOrganization("Checklist Missing Task Org");
		Establishment establishment = createEstablishment(organization, "Checklist Missing Task Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		String employeeToken = login("missing-task-employee@example.com", "password123");

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/checklists/runs/%s/tasks/%s"
						.formatted(organization.getId(), establishment.getId(), run.getId(), UUID.randomUUID()))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "executionStatus": "COMPLETED",
								  "comment": "Try to update unknown task"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("checklist_task_execution_not_found"))
				.andExpect(jsonPath("$.message").value("Checklist task execution not found"));
	}

	@Test
	void resettingChecklistRunOutsideInProgressReturnsConflictProblemDetails() throws Exception {
		User manager = createUser("reset-state-manager@example.com");
		User employee = createUser("reset-state-employee@example.com");
		Organization organization = createOrganization("Checklist Reset State Org");
		Establishment establishment = createEstablishment(organization, "Checklist Reset State Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun run = createRun(definition, establishment, manager);
		String employeeToken = login("reset-state-employee@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/checklists/runs/%s/reset"
						.formatted(organization.getId(), establishment.getId(), run.getId()))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("checklist_run_reset_invalid_state"))
				.andExpect(jsonPath("$.message").value("Only in-progress checklist runs can be reset"));
	}

	@Test
	void userWithoutMembershipCannotReadChecklistDefinitionsOrRuns() throws Exception {
		User manager = createUser("read-manager@example.com");
		createUser("read-outsider@example.com");
		Organization organization = createOrganization("Checklist Read Org");
		Establishment establishment = createEstablishment(organization, "Checklist Read Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		createRun(definition, establishment, manager);

		String outsiderToken = login("read-outsider@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/checklists/definitions?serviceArea=IK_MAT"
						.formatted(organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/checklists/runs?serviceArea=IK_MAT"
						.formatted(organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
				.andExpect(status().isForbidden());
	}

	private ChecklistDefinition createDefinition(Organization organization, Establishment establishment, User manager) {
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
				Instant.parse("2026-04-07T09:00:00Z"),
				ChecklistRunStatus.PENDING,
				manager
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

	private void createMembership(Organization organization, User user, OrganizationRole role) {
		organizationMembershipRepository.saveAndFlush(new OrganizationMembership(organization, user, role, true));
	}

	private CurrentUser currentUser(User user) {
		return new CurrentUser(user.getId(), user.getEmail(), user.getGlobalRoles());
	}

	private String login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(response);
		return json.get("accessToken").asText();
	}

	private String validCreateDefinitionPayload() {
		return """
				{
				  "title": "Receiving checks",
				  "description": "Record food receiving controls",
				  "serviceArea": "IK_MAT",
				  "tasks": [
				    {
				      "title": "Record fridge temperature",
				      "details": "Measure the receiving fridge",
				      "taskKind": "MEASUREMENT",
				      "required": true,
				      "sortOrder": 0,
				      "measurementUnit": "C",
				      "minimumAllowedValue": 0,
				      "maximumAllowedValue": 4
				    }
				  ]
				}
				""";
	}

	private String validUpdateDefinitionPayload() {
		return """
				{
				  "title": "Receiving checks updated",
				  "description": "Record updated food receiving controls",
				  "serviceArea": "IK_MAT",
				  "status": "ACTIVE",
				  "tasks": [
				    {
				      "title": "Record fridge temperature",
				      "details": "Measure the receiving fridge",
				      "taskKind": "MEASUREMENT",
				      "required": true,
				      "sortOrder": 0,
				      "measurementUnit": "C",
				      "minimumAllowedValue": 0,
				      "maximumAllowedValue": 4
				    }
				  ]
				}
				""";
	}
}
