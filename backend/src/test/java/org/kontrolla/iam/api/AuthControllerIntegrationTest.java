package org.kontrolla.iam.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.kontrolla.support.TestDataCleaner;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void loginAndMeReturnAuthenticatedUser() throws Exception {
		User user = new User("alice@example.com", "Alice", "Example", passwordEncoder.encode("password123"), true, Set.of());
		userRepository.saveAndFlush(user);
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Alice Organization", OrganizationStatus.ACTIVE));
		establishmentRepository.saveAndFlush(
				new Establishment(organization, "Alice Establishment", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, user, OrganizationRole.ORG_MANAGER, true));

		String loginBody = """
				{
				  "email": "alice@example.com",
				  "password": "password123"
				}
				""";

		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.user.email").value("alice@example.com"))
				.andExpect(jsonPath("$.appContext.organizationName").value("Alice Organization"))
				.andExpect(jsonPath("$.appContext.establishmentName").value("Alice Establishment"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(loginResponse);
		String accessToken = json.get("accessToken").asText();

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.firstName").value("Alice"))
				.andExpect(jsonPath("$.lastName").value("Example"));
	}

	@Test
	void refreshReturnsSessionWithAppContext() throws Exception {
		User user = new User("alice@example.com", "Alice", "Example", passwordEncoder.encode("password123"), true, Set.of());
		userRepository.saveAndFlush(user);
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Alice Organization", OrganizationStatus.ACTIVE));
		establishmentRepository.saveAndFlush(
				new Establishment(organization, "Alice Establishment", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, user, OrganizationRole.ORG_MANAGER, true));

		String loginBody = """
				{
				  "email": "alice@example.com",
				  "password": "password123"
				}
				""";

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andReturn();

		String refreshCookie = loginResult.getResponse().getCookie("kontrolla_refresh_token").getValue();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.cookie(new jakarta.servlet.http.Cookie("kontrolla_refresh_token", refreshCookie)))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andExpect(jsonPath("$.appContext.organizationName").value("Alice Organization"))
				.andExpect(jsonPath("$.appContext.establishmentName").value("Alice Establishment"));
	}

	@Test
	void invitedUserCanAcceptInvitationAndThenLogIn() throws Exception {
		User platformAdmin = new User(
				"admin@example.com",
				"Admin",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of(org.kontrolla.iam.domain.GlobalRole.PLATFORM_ADMIN)
		);
		User orgAdmin = new User(
				"orgadmin@example.com",
				"Org",
				"Admin",
				passwordEncoder.encode("password123"),
				true,
				Set.of()
		);
		userRepository.saveAndFlush(platformAdmin);
		userRepository.saveAndFlush(orgAdmin);

		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Invite Organization", OrganizationStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, orgAdmin, OrganizationRole.ORG_ADMIN, true));

		String orgAdminLogin = """
				{
				  "email": "orgadmin@example.com",
				  "password": "password123"
				}
				""";

		String adminAccessToken = objectMapper.readTree(
				mockMvc.perform(post("/api/v1/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content(orgAdminLogin))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString()
		).get("accessToken").asText();

		String inviteResponse = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organization.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "invitee@example.com",
								  "firstName": "Invited",
								  "lastName": "User",
								  "role": "ORG_EMPLOYEE",
								  "active": true
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.inviteUrl").isString())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String inviteUrl = objectMapper.readTree(inviteResponse).get("inviteUrl").asText();
		String token = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);

		mockMvc.perform(get("/api/v1/auth/invitations/%s".formatted(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("invitee@example.com"))
				.andExpect(jsonPath("$.organizationName").value("Invite Organization"));

		mockMvc.perform(post("/api/v1/auth/invitations/%s/accept".formatted(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "invitee@example.com",
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("invitee@example.com"));
	}

	@Test
	void inactiveInvitedUserRemainsInactiveAfterAcceptingInvitation() throws Exception {
		User platformAdmin = new User(
				"admin@example.com",
				"Admin",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of(org.kontrolla.iam.domain.GlobalRole.PLATFORM_ADMIN)
		);
		User orgAdmin = new User(
				"orgadmin@example.com",
				"Org",
				"Admin",
				passwordEncoder.encode("password123"),
				true,
				Set.of()
		);
		userRepository.saveAndFlush(platformAdmin);
		userRepository.saveAndFlush(orgAdmin);

		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Invite Organization", OrganizationStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, orgAdmin, OrganizationRole.ORG_ADMIN, true));

		String adminAccessToken = objectMapper.readTree(
				mockMvc.perform(post("/api/v1/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{
										  "email": "orgadmin@example.com",
										  "password": "password123"
										}
										"""))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString()
		).get("accessToken").asText();

		String inviteResponse = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organization.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "inactive.invitee@example.com",
								  "firstName": "Inactive",
								  "lastName": "Invitee",
								  "role": "ORG_EMPLOYEE",
								  "active": false
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.membership.active").value(false))
				.andExpect(jsonPath("$.inviteUrl").isString())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String inviteUrl = objectMapper.readTree(inviteResponse).get("inviteUrl").asText();
		String token = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);

		mockMvc.perform(post("/api/v1/auth/invitations/%s/accept".formatted(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "inactive.invitee@example.com",
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void inactiveInvitedUserCanLogInAfterMembershipIsActivated() throws Exception {
		User platformAdmin = new User(
				"admin@example.com",
				"Admin",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of(org.kontrolla.iam.domain.GlobalRole.PLATFORM_ADMIN)
		);
		User orgAdmin = new User(
				"orgadmin@example.com",
				"Org",
				"Admin",
				passwordEncoder.encode("password123"),
				true,
				Set.of()
		);
		userRepository.saveAndFlush(platformAdmin);
		userRepository.saveAndFlush(orgAdmin);

		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Invite Organization", OrganizationStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, orgAdmin, OrganizationRole.ORG_ADMIN, true));

		String adminAccessToken = objectMapper.readTree(
				mockMvc.perform(post("/api/v1/auth/login")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{
										  "email": "orgadmin@example.com",
										  "password": "password123"
										}
										"""))
						.andExpect(status().isOk())
						.andReturn()
						.getResponse()
						.getContentAsString()
		).get("accessToken").asText();

		String inviteResponse = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organization.getId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "recoverable.invitee@example.com",
								  "firstName": "Recoverable",
								  "lastName": "Invitee",
								  "role": "ORG_EMPLOYEE",
								  "active": false
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.membership.active").value(false))
				.andExpect(jsonPath("$.inviteUrl").isString())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode inviteJson = objectMapper.readTree(inviteResponse);
		String membershipId = inviteJson.get("membership").get("id").asText();
		String inviteUrl = inviteJson.get("inviteUrl").asText();
		String token = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);

		mockMvc.perform(post("/api/v1/auth/invitations/%s/accept".formatted(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(organization.getId(), membershipId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "ORG_EMPLOYEE",
								  "active": true
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "recoverable.invitee@example.com",
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("recoverable.invitee@example.com"));
	}
}
