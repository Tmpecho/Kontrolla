package org.kontrolla.organizations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.audit.domain.AuditEvent;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;
import org.kontrolla.audit.infrastructure.AuditEventRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MembershipAuditIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AuditEventRepository auditEventRepository;

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
	void membershipCreationPersistsAuditEvent() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		createUser("employee@example.com", "Employee", "User", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Membership Org");
		String establishmentId = createEstablishment(adminToken, organizationId, "Restaurant", "RESTAURANT");
		String employeeId = userRepository.findByEmailIgnoreCase("employee@example.com").orElseThrow().getId().toString();

		String response = mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE",
								  "active": false,
								  "allEstablishments": false,
								  "establishmentIds": ["%s"]
								}
								""".formatted(employeeId, establishmentId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String membershipId = objectMapper.readTree(response).get("id").asText();

		AuditEvent auditEvent = findSingleAuditEvent(AuditAction.MEMBERSHIP_CREATE, membershipId);
		assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
		assertThat(auditEvent.getResultCode()).isEqualTo("membership_created");
		assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
		assertThat(auditEvent.getActorEmail()).isEqualTo("admin@example.com");
		assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.MEMBERSHIP);
		assertThat(auditEvent.getOrganizationId()).isEqualTo(UUID.fromString(organizationId));

		JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
		assertThat(metadata.path("membershipId").asText()).isEqualTo(membershipId);
		assertThat(metadata.path("userEmail").asText()).isEqualTo("employee@example.com");
		assertThat(metadata.path("role").asText()).isEqualTo("ORG_EMPLOYEE");
		assertThat(metadata.path("active").asBoolean()).isFalse();
		assertThat(metadata.path("accessAllEstablishments").asBoolean()).isFalse();
		assertThat(metadata.path("establishmentIds")).hasSize(1);
		assertThat(metadata.path("establishmentIds").get(0).asText()).isEqualTo(establishmentId);
	}

	@Test
	void managedMembershipProvisionPersistsMembershipCreateAuditEvent() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		createUser("orgadmin@example.com", "Org", "Admin", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Managed Membership Org");
		String orgAdminId = userRepository.findByEmailIgnoreCase("orgadmin@example.com").orElseThrow().getId().toString();
		addMembership(adminToken, organizationId, orgAdminId, "ORG_ADMIN");

		String orgAdminToken = login("orgadmin@example.com", "password123");

		String response = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "managed.member@example.com",
								  "firstName": "Managed",
								  "lastName": "Member",
								  "role": "ORG_EMPLOYEE",
								  "active": true
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String membershipId = objectMapper.readTree(response).get("membership").get("id").asText();

		AuditEvent auditEvent = findSingleAuditEvent(AuditAction.MEMBERSHIP_CREATE, membershipId);
		assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
		assertThat(auditEvent.getResultCode()).isEqualTo("membership_created");
		assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
		assertThat(auditEvent.getActorEmail()).isEqualTo("orgadmin@example.com");
		assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.MEMBERSHIP);
		assertThat(auditEvent.getOrganizationId()).isEqualTo(UUID.fromString(organizationId));

		JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
		assertThat(metadata.path("membershipId").asText()).isEqualTo(membershipId);
		assertThat(metadata.path("userEmail").asText()).isEqualTo("managed.member@example.com");
		assertThat(metadata.path("role").asText()).isEqualTo("ORG_EMPLOYEE");
		assertThat(metadata.path("active").asBoolean()).isTrue();
		assertThat(metadata.path("accessAllEstablishments").asBoolean()).isTrue();
		assertThat(metadata.path("establishmentIds")).isEmpty();
	}

	@Test
	void membershipUpdatePersistsBeforeAndAfterAuditState() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		createUser("employee@example.com", "Employee", "User", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Update Membership Org");
		String restaurantId = createEstablishment(adminToken, organizationId, "Restaurant", "RESTAURANT");
		createEstablishment(adminToken, organizationId, "Bar", "BAR");
		String employeeId = userRepository.findByEmailIgnoreCase("employee@example.com").orElseThrow().getId().toString();

		String membershipResponse = mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE",
								  "active": false,
								  "allEstablishments": false,
								  "establishmentIds": ["%s"]
								}
								""".formatted(employeeId, restaurantId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String membershipId = objectMapper.readTree(membershipResponse).get("id").asText();

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(organizationId, membershipId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "ORG_MANAGER",
								  "active": true,
								  "allEstablishments": true
								}
								"""))
				.andExpect(status().isOk());

		AuditEvent auditEvent = findSingleAuditEvent(AuditAction.MEMBERSHIP_UPDATE, membershipId);
		assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
		assertThat(auditEvent.getResultCode()).isEqualTo("membership_updated");
		assertThat(auditEvent.getActorEmail()).isEqualTo("admin@example.com");
		assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.MEMBERSHIP);

		JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
		JsonNode before = metadata.path("before");
		JsonNode after = metadata.path("after");
		assertThat(before.path("role").asText()).isEqualTo("ORG_EMPLOYEE");
		assertThat(before.path("active").asBoolean()).isFalse();
		assertThat(before.path("accessAllEstablishments").asBoolean()).isFalse();
		assertThat(before.path("establishmentIds")).hasSize(1);
		assertThat(before.path("establishmentIds").get(0).asText()).isEqualTo(restaurantId);
		assertThat(after.path("role").asText()).isEqualTo("ORG_MANAGER");
		assertThat(after.path("active").asBoolean()).isTrue();
		assertThat(after.path("accessAllEstablishments").asBoolean()).isTrue();
		assertThat(after.path("establishmentIds")).isEmpty();
	}

	private AuditEvent findSingleAuditEvent(AuditAction action, String membershipId) {
		List<AuditEvent> matchingEvents = auditEventRepository.findAll().stream()
				.filter(auditEvent -> auditEvent.getAction() == action)
				.filter(auditEvent -> membershipId.equals(readJson(auditEvent.getMetadataJson()).path("membershipId").asText()))
				.toList();
		assertThat(matchingEvents).hasSize(1);
		return matchingEvents.getFirst();
	}

	private JsonNode readJson(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (java.io.IOException exception) {
			throw new IllegalStateException("Failed to read audit metadata JSON", exception);
		}
	}

	private User createUser(String email, String firstName, String lastName, Set<GlobalRole> globalRoles) {
		User user = new User(
				email,
				firstName,
				lastName,
				passwordEncoder.encode("password123"),
				true,
				globalRoles
		);
		return userRepository.saveAndFlush(user);
	}

	private String login(String email, String password) throws Exception {
		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
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

		return objectMapper.readTree(loginResponse).get("accessToken").asText();
	}

	private String createOrganization(String adminToken, String name) throws Exception {
		String response = mockMvc.perform(post("/api/v1/admin/organizations")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s"
								}
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(response).get("id").asText();
	}

	private String createEstablishment(String adminToken, String organizationId, String name, String type) throws Exception {
		String response = mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "type": "%s"
								}
								""".formatted(name, type)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(response).get("id").asText();
	}

	private void addMembership(String adminToken, String organizationId, String userId, String role) throws Exception {
		mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "%s"
								}
								""".formatted(userId, role)))
				.andExpect(status().isCreated());
	}
}
