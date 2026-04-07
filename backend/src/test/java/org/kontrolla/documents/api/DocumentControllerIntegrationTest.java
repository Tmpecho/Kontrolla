package org.kontrolla.documents.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

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

	@Autowired
	private Clock clock;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void managerCanCreateUpdateFetchAndListDocuments() throws Exception {
		User manager = createUser("documents-api-manager@example.com", "Manager", "API");
		Organization organization = createOrganization("Kontrolla Documents API");
		Establishment establishment = createEstablishment(organization, "Downtown Bar");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
		LocalDate today = LocalDate.now(clock);

		String token = login("documents-api-manager@example.com", "password123");

		String createResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "serviceArea": "IK_ALKOHOL",
								  "title": "  Alcohol service licence  ",
								  "holderName": "  Oslo Municipality  ",
								  "issueDate": "%s",
								  "renewalDate": "%s"
								}
								""".formatted(today.minusDays(365), today.plusDays(10))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Alcohol service licence"))
				.andExpect(jsonPath("$.holderName").value("Oslo Municipality"))
				.andExpect(jsonPath("$.serviceArea").value("IK_ALKOHOL"))
				.andExpect(jsonPath("$.status").value("EXPIRING"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String documentId = objectMapper.readTree(createResponse).get("id").asText();

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "serviceArea": "IK_ALKOHOL",
								  "title": "Responsible service certificate",
								  "holderName": "Lina Dahl",
								  "issueDate": "%s",
								  "renewalDate": "%s"
								}
								""".formatted(today.minusDays(200), today.plusDays(3))))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "serviceArea": "IK_MAT",
								  "title": "Cleaning routine",
								  "holderName": "Kitchen operations",
								  "issueDate": "%s",
								  "renewalDate": "%s"
								}
								""".formatted(today.minusDays(120), today.plusDays(90))))
				.andExpect(status().isCreated());

		mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
						organization.getId(), establishment.getId(), documentId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "serviceArea": "IK_ALKOHOL",
								  "title": "Alcohol service licence 2026",
								  "holderName": "Oslo Municipality Licensing",
								  "issueDate": "%s",
								  "renewalDate": "%s"
								}
								""".formatted(today.minusDays(365), today.plusDays(60))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Alcohol service licence 2026"))
				.andExpect(jsonPath("$.status").value("VALID"));

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
						organization.getId(), establishment.getId(), documentId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(documentId))
				.andExpect(jsonPath("$.holderName").value("Oslo Municipality Licensing"))
				.andExpect(jsonPath("$.status").value("VALID"));

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents?serviceArea=IK_ALKOHOL".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].title").value("Responsible service certificate"))
				.andExpect(jsonPath("$.items[0].status").value("EXPIRING"))
				.andExpect(jsonPath("$.items[1].title").value("Alcohol service licence 2026"));
	}

	@Test
	void invalidDateRangeReturnsBadRequest() throws Exception {
		User manager = createUser("documents-api-dates@example.com", "Manager", "Dates");
		Organization organization = createOrganization("Kontrolla API Dates");
		Establishment establishment = createEstablishment(organization, "Date Checks");
		createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
		LocalDate today = LocalDate.now(clock);

		String token = login("documents-api-dates@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents".formatted(
						organization.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "serviceArea": "IK_ALKOHOL",
								  "title": "Staff permit register",
								  "holderName": "People operations",
								  "issueDate": "%s",
								  "renewalDate": "%s"
								}
								""".formatted(today, today.minusDays(1))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_document_dates"));
	}

	@Test
	void outsiderFromAnotherOrganizationCannotReadDocuments() throws Exception {
		User manager = createUser("documents-api-cross-manager@example.com", "Manager", "Cross");
		User outsider = createUser("documents-api-cross-outsider@example.com", "Outsider", "Cross");
		Organization organizationA = createOrganization("Org A Documents");
		Organization organizationB = createOrganization("Org B Documents");
		Establishment establishment = createEstablishment(organizationA, "Inspection Bar");
		createMembership(organizationA, manager, OrganizationRole.ORG_MANAGER, true);
		createMembership(organizationB, outsider, OrganizationRole.ORG_MANAGER, true);
		LocalDate today = LocalDate.now(clock);

		String managerToken = login("documents-api-cross-manager@example.com", "password123");
		String outsiderToken = login("documents-api-cross-outsider@example.com", "password123");

		String createResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents".formatted(
						organizationA.getId(), establishment.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "serviceArea": "IK_ALKOHOL",
								  "title": "Incident reporting routine sign-off",
								  "holderName": "Shift supervisors",
								  "issueDate": "%s",
								  "renewalDate": "%s"
								}
								""".formatted(today.minusDays(180), today.plusDays(30))))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String documentId = objectMapper.readTree(createResponse).get("id").asText();

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
						organizationA.getId(), establishment.getId(), documentId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents?serviceArea=IK_ALKOHOL".formatted(
						organizationA.getId(), establishment.getId()))
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
				EstablishmentType.BAR,
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
