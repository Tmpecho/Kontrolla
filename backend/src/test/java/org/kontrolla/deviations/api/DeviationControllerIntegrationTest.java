package org.kontrolla.deviations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviationControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private ChecklistDefinitionRepository checklistDefinitionRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private DeviationRepository deviationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void managerCanCreateManageAndFetchDeviationTimeline() throws Exception {
		User manager = createUser("manager@example.com", "Manager", "User");
		User assignee = createUser("assignee@example.com", "Assignee", "User");
		Organization organization = createOrganization("Kontrolla API");
		Establishment establishment = createEstablishment(organization, "Downtown Kitchen");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
		createMembership(organization, assignee, OrganizationRole.ORG_EMPLOYEE, true);

		String token = login("manager@example.com", "password123");

		String createResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Walk-in fridge too warm",
								  "description": "Morning check recorded 10C in the walk-in fridge.",
								  "category": "TEMPERATURE",
								  "severity": "HIGH"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Walk-in fridge too warm"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.timeline[0].eventType").value("REPORTED"))
				.andExpect(jsonPath("$.timeline[0].note").value("Deviation reported."))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String deviationId = objectMapper.readTree(createResponse).get("id").asText();

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/deviations/%s/assignment".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "assignedUserId": "%s"
								}
								""".formatted(assignee.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assignedToUserId").value(assignee.getId().toString()))
				.andExpect(jsonPath("$.timeline[1].eventType").value("ASSIGNED"))
				.andExpect(jsonPath("$.timeline[1].note").value("Deviation assigned to Assignee User."));

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/deviations/%s/status".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "IN_PROGRESS"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.timeline[2].eventType").value("STATUS_CHANGED"));

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/deviations/%s".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Walk-in fridge temperature deviation",
								  "description": "Products were moved to backup cooling and the fridge was inspected.",
								  "category": "STORAGE",
								  "severity": "CRITICAL"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Walk-in fridge temperature deviation"))
				.andExpect(jsonPath("$.severity").value("CRITICAL"))
				.andExpect(jsonPath("$.category").value("STORAGE"))
				.andExpect(jsonPath("$.timeline[3].eventType").value("DETAILS_UPDATED"));

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations/%s/timeline".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "Thermostat recalibrated and follow-up temperature check passed."
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline[4].eventType").value("NOTE_ADDED"))
				.andExpect(jsonPath("$.timeline[4].note").value(
						"Thermostat recalibrated and follow-up temperature check passed."));

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/deviations/%s".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(deviationId))
				.andExpect(jsonPath("$.timeline.length()").value(5))
				.andExpect(jsonPath("$.timeline[0].authorName").value("Manager User"));

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(deviationId))
				.andExpect(jsonPath("$.items[0].status").value("IN_PROGRESS"));

		mockMvc.perform(get("/api/v1/organizations/%s/deviations".formatted(organization.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Walk-in fridge temperature deviation"));
	}

	@Test
	void listEndpointsReturnMostRecentDeviationsFirstByDefault() throws Exception {
		User manager = createUser("manager-order@example.com", "Manager", "Order");
		Organization organization = createOrganization("Kontrolla Ordering");
		Establishment establishment = createEstablishment(organization, "Ordering Kitchen");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

		String token = login("manager-order@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Older deviation",
								  "description": "Created first to verify default ordering.",
								  "category": "HYGIENE",
								  "severity": "LOW"
								}
								"""))
				.andExpect(status().isCreated());

		Thread.sleep(5);

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Newest deviation",
								  "description": "Created second to verify default ordering.",
								  "category": "TEMPERATURE",
								  "severity": "HIGH"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/deviations?size=1".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Newest deviation"));

		mockMvc.perform(get("/api/v1/organizations/%s/deviations?size=1".formatted(organization.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].title").value("Newest deviation"));
	}

	@Test
	void employeeCanCreateDeviationButCannotRunManagementEndpoints() throws Exception {
		User employee = createUser("employee@example.com", "Employee", "User");
		Organization organization = createOrganization("Kontrolla Employee Access");
		Establishment establishment = createEstablishment(organization, "Late Shift");
		createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);

		String token = login("employee@example.com", "password123");

		String createResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Cleaning step skipped",
								  "description": "Closing checklist was missing the cleaning signature.",
								  "category": "HYGIENE",
								  "severity": "MEDIUM"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String deviationId = objectMapper.readTree(createResponse).get("id").asText();

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/deviations/%s/status".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "status": "IN_PROGRESS"
								}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations/%s/timeline".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "Attempted follow-up note."
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void timelineNotesAreTrimmedAndBlankNotesAreRejected() throws Exception {
		User manager = createUser("manager-trim@example.com", "Manager", "Trim");
		Organization organization = createOrganization("Kontrolla Timeline Validation");
		Establishment establishment = createEstablishment(organization, "Service Area");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

		String token = login("manager-trim@example.com", "password123");
		String createResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Missing age control signage",
								  "description": "The ID control sign was not visible at the register.",
								  "category": "AGE_CONTROL",
								  "severity": "LOW"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String deviationId = objectMapper.readTree(createResponse).get("id").asText();

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations/%s/timeline".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "   Signage was replaced before opening.   "
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timeline[1].eventType").value("NOTE_ADDED"))
				.andExpect(jsonPath("$.timeline[1].note").value("Signage was replaced before opening."));

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations/%s/timeline".formatted(
						organization.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "note": "   "
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void memberFromAnotherOrganizationCannotReadDeviationDetails() throws Exception {
		User manager = createUser("manager-cross@example.com", "Manager", "Cross");
		User outsider = createUser("outsider-cross@example.com", "Outsider", "Cross");
		Organization organizationA = createOrganization("Org A Deviations");
		Organization organizationB = createOrganization("Org B Deviations");
		Establishment establishment = createEstablishment(organizationA, "Inspection Room");
		createMembership(organizationA, manager, OrganizationRole.ORG_MANAGER, true);
		createMembership(organizationB, outsider, OrganizationRole.ORG_MANAGER, true);

		String managerToken = login("manager-cross@example.com", "password123");
		String outsiderToken = login("outsider-cross@example.com", "password123");

		String createResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/deviations".formatted(
						organizationA.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Unlabelled allergen container",
								  "description": "Prepared sauce container was missing allergen labels.",
								  "category": "ALLERGEN",
								  "severity": "HIGH"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String deviationId = objectMapper.readTree(createResponse).get("id").asText();

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/deviations/%s".formatted(
						organizationA.getId(), establishment.getId(), deviationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/deviations".formatted(organizationA.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
				.andExpect(status().isForbidden());
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

	private void createMembership(Organization organization, User user, OrganizationRole role, boolean active) {
		OrganizationMembership membership = new OrganizationMembership(organization, user, role, active);
		organizationMembershipRepository.saveAndFlush(membership);
	}

	private String login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
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
