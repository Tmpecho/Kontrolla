package org.kontrolla.audit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.audit.domain.AuditEvent;
import org.kontrolla.audit.infrastructure.AuditEventRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.application.AuthAttemptThrottleService;
import org.kontrolla.iam.domain.GlobalRole;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditFailureHandlingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@SpyBean
	private AuditEventRepository auditEventRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@Autowired
	private AuthAttemptThrottleService authAttemptThrottleService;

	@BeforeEach
	void setUp() {
		reset(auditEventRepository);
		testDataCleaner.clearAll();
		authAttemptThrottleService.clear();
	}

	@AfterEach
	void tearDown() {
		reset(auditEventRepository);
	}

	@Test
	void auditPersistenceFailureRollsBackAdminUserCreation() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		String adminToken = login("admin@example.com", "password123");
		doThrow(new DataAccessResourceFailureException("audit store unavailable"))
				.when(auditEventRepository)
				.save(any(AuditEvent.class));

		mockMvc.perform(post("/api/v1/admin/users")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "rolled-back@example.com",
								  "firstName": "Rolled",
								  "lastName": "Back",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("internal_error"));

		assertThat(userRepository.findByEmailIgnoreCase("rolled-back@example.com")).isEmpty();
	}

	@Test
	void auditPersistenceFailureRollsBackMembershipUpdate() throws Exception {
		User admin = createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User employee = createUser("employee@example.com", "Employee", "User", Set.of());
		Organization organization = organizationRepository.saveAndFlush(new Organization("Rollback Org", OrganizationStatus.ACTIVE));
		Establishment establishment = establishmentRepository.saveAndFlush(
				new Establishment(organization, "Restaurant", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		OrganizationMembership membership = new OrganizationMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, false, false);
		membership.replaceAccessibleEstablishments(List.of(establishment));
		membership = membershipRepository.saveAndFlush(membership);

		String adminToken = login(admin.getEmail(), "password123");
		doThrow(new DataAccessResourceFailureException("audit store unavailable"))
				.when(auditEventRepository)
				.save(any(AuditEvent.class));

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(organization.getId(), membership.getId()))
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
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("internal_error"));

		OrganizationMembership reloadedMembership = membershipRepository.findByIdAndOrganizationId(membership.getId(), organization.getId()).orElseThrow();
		assertThat(reloadedMembership.getRole()).isEqualTo(OrganizationRole.ORG_EMPLOYEE);
		assertThat(reloadedMembership.isActive()).isFalse();
		assertThat(reloadedMembership.isAccessAllEstablishments()).isFalse();
		assertThat(reloadedMembership.getAccessibleEstablishments()).hasSize(1);
	}

	@Test
	void auditPersistenceFailureDoesNotChangeFailedLoginResponse() throws Exception {
		createUser("alice@example.com", "Alice", "Example", Set.of());
		doThrow(new DataAccessResourceFailureException("audit store unavailable"))
				.when(auditEventRepository)
				.save(any(AuditEvent.class));

		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "alice@example.com",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_credentials"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));

		assertThat(auditEventRepository.findAll()).isEmpty();
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
}
