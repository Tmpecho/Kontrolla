package org.kontrolla.iam.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.iam.application.LoginAttemptTracker;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerIntegrationTest.TestClockConfiguration.class)
class AuthControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

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

	@Autowired
	private LoginAttemptTracker loginAttemptTracker;

	@Autowired
	private MutableClock mutableClock;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
		loginAttemptTracker.clear();
		mutableClock.set(Instant.parse("2026-04-07T08:00:00Z"));
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
	void refreshRejectsReplayOfPreviouslyUsedRefreshToken() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andReturn();

		String initialRefreshCookie = loginResult.getResponse().getCookie("kontrolla_refresh_token").getValue();

		MvcResult refreshResult = performRefresh(initialRefreshCookie)
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andReturn();

		String rotatedRefreshCookie = refreshResult.getResponse().getCookie("kontrolla_refresh_token").getValue();

		org.junit.jupiter.api.Assertions.assertNotEquals(initialRefreshCookie, rotatedRefreshCookie);

		performRefresh(initialRefreshCookie)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));

		performRefresh(rotatedRefreshCookie)
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"));
	}

	@Test
	void refreshRejectsMissingRefreshCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("missing_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is missing"));
	}

	@Test
	void refreshRejectsForgedRefreshCookie() throws Exception {
		performRefresh("forged-refresh-token")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));
	}

	@Test
	void loginLocksOutAccountAfterRepeatedFailedAttempts() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		for (int attempt = 0; attempt < 5; attempt++) {
			performLogin("alice@example.com", "wrong-password")
					.andExpect(status().isUnauthorized())
					.andExpect(cookie().doesNotExist("kontrolla_refresh_token"))
					.andExpect(jsonPath("$.message").value("Invalid email or password"));
		}

		performLogin("alice@example.com", "password123")
				.andExpect(status().isUnauthorized())
				.andExpect(cookie().doesNotExist("kontrolla_refresh_token"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void loginAllowsCorrectPasswordAfterLockoutWindowExpires() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		for (int attempt = 0; attempt < 5; attempt++) {
			performLogin("alice@example.com", "wrong-password")
					.andExpect(status().isUnauthorized());
		}

		performLogin("alice@example.com", "password123")
				.andExpect(status().isUnauthorized());

		mutableClock.advanceSeconds(15 * 60 + 1);

		performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andExpect(jsonPath("$.user.email").value("alice@example.com"));
	}

	@Test
	void successfulLoginResetsFailedAttemptCounter() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		for (int attempt = 0; attempt < 4; attempt++) {
			performLogin("alice@example.com", "wrong-password")
					.andExpect(status().isUnauthorized());
		}

		performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"));

		for (int attempt = 0; attempt < 4; attempt++) {
			performLogin("alice@example.com", "wrong-password")
					.andExpect(status().isUnauthorized());
		}

		performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andExpect(jsonPath("$.user.email").value("alice@example.com"));
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(email, password)));
	}

	private org.springframework.test.web.servlet.ResultActions performRefresh(String refreshCookie) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new jakarta.servlet.http.Cookie("kontrolla_refresh_token", refreshCookie)));
	}

	private void createUserWithOrganizationContext(String email, String password) {
		User user = new User(email, "Alice", "Example", passwordEncoder.encode(password), true, Set.of());
		userRepository.saveAndFlush(user);
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Alice Organization", OrganizationStatus.ACTIVE));
		establishmentRepository.saveAndFlush(
				new Establishment(organization, "Alice Establishment", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, user, OrganizationRole.ORG_MANAGER, true));
	}

	@TestConfiguration
	static class TestClockConfiguration {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(Instant.parse("2026-04-07T08:00:00Z"), ZoneId.of("UTC"));
		}

	}

	static class MutableClock extends Clock {

		private Instant currentInstant;
		private final ZoneId zoneId;

		MutableClock(Instant currentInstant, ZoneId zoneId) {
			this.currentInstant = currentInstant;
			this.zoneId = zoneId;
		}

		void set(Instant instant) {
			this.currentInstant = instant;
		}

		void advanceSeconds(long seconds) {
			this.currentInstant = this.currentInstant.plusSeconds(seconds);
		}

		@Override
		public ZoneId getZone() {
			return zoneId;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return new MutableClock(currentInstant, zone);
		}

		@Override
		public Instant instant() {
			return currentInstant;
		}

	}
}
