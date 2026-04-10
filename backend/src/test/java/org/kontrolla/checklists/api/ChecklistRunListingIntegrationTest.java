package org.kontrolla.checklists.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.application.ChecklistDefinitionService;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChecklistRunListingIntegrationTest {

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
	void omittedStatusesReturnsChecklistRunsAcrossAllStatuses() throws Exception {
		User manager = createUser("listing-manager@example.com");
		Organization organization = createOrganization("Checklist Listing Org");
		Establishment establishment = createEstablishment(organization, "Checklist Listing Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T08:00:00Z"), ChecklistRunStatus.PENDING);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T09:00:00Z"), ChecklistRunStatus.IN_PROGRESS);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T10:00:00Z"), ChecklistRunStatus.COMPLETED);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T11:00:00Z"), ChecklistRunStatus.OVERDUE);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T12:00:00Z"), ChecklistRunStatus.CANCELLED);

		String accessToken = login("listing-manager@example.com", "password123");

		MvcResult result = mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/checklists/runs?serviceArea=IK_MAT"
						.formatted(organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn();

		List<String> returnedStatuses = extractItemValues(result, "status");

		assertThat(returnedStatuses).containsExactlyInAnyOrder(
				"PENDING",
				"IN_PROGRESS",
				"COMPLETED",
				"OVERDUE",
				"CANCELLED"
		);
	}

	@Test
	void explicitStatusesRestrictChecklistRunListing() throws Exception {
		User manager = createUser("listing-filter-manager@example.com");
		Organization organization = createOrganization("Checklist Listing Filter Org");
		Establishment establishment = createEstablishment(organization, "Checklist Listing Filter Restaurant");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER);

		ChecklistDefinition definition = createDefinition(organization, establishment, manager);
		ChecklistRun pendingRun = createRun(
				definition,
				establishment,
				manager,
				Instant.parse("2026-04-07T08:00:00Z"),
				ChecklistRunStatus.PENDING
		);
		ChecklistRun overdueRun = createRun(
				definition,
				establishment,
				manager,
				Instant.parse("2026-04-07T09:00:00Z"),
				ChecklistRunStatus.OVERDUE
		);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T10:00:00Z"), ChecklistRunStatus.COMPLETED);
		createRun(definition, establishment, manager, Instant.parse("2026-04-07T11:00:00Z"), ChecklistRunStatus.CANCELLED);

		String accessToken = login("listing-filter-manager@example.com", "password123");

		MvcResult result = mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/checklists/runs?serviceArea=IK_MAT&statuses=PENDING&statuses=OVERDUE"
						.formatted(organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn();

		List<String> returnedStatuses = extractItemValues(result, "status");
		List<String> returnedIds = extractItemValues(result, "id");

		assertThat(returnedStatuses).containsExactlyInAnyOrder("PENDING", "OVERDUE");
		assertThat(returnedIds).containsExactlyInAnyOrder(
				pendingRun.getId().toString(),
				overdueRun.getId().toString()
		);
	}

	private List<String> extractItemValues(MvcResult result, String fieldName) throws Exception {
		JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
		return java.util.stream.StreamSupport.stream(items.spliterator(), false)
				.map(item -> item.get(fieldName).asText())
				.toList();
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

	private ChecklistRun createRun(
			ChecklistDefinition definition,
			Establishment establishment,
			User manager,
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
}
