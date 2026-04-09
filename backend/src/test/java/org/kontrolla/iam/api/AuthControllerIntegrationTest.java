package org.kontrolla.iam.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.application.AuthAttemptThrottleService;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerIntegrationTest.TestClockConfiguration.class)
class AuthControllerIntegrationTest {

	private static final String REFRESH_COOKIE_NAME = "kontrolla_refresh_token";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private AppSecurityProperties securityProperties;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@Autowired
	private AuthAttemptThrottleService authAttemptThrottleService;

	@Autowired
	private MutableClock mutableClock;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
		authAttemptThrottleService.clear();
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

		String loginResponse = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.user.email").value("alice@example.com"))
				.andExpect(jsonPath("$.appContext.organizationName").value("Alice Organization"))
				.andExpect(jsonPath("$.appContext.establishmentName").value("Alice Establishment"))
				.andExpect(jsonPath("$.appContext.organizationRole").value("ORG_MANAGER"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode json = objectMapper.readTree(loginResponse);
		String accessToken = json.get("accessToken").asText();

		performMe(accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.firstName").value("Alice"))
				.andExpect(jsonPath("$.lastName").value("Example"));
	}

	@Test
	void loginRejectsMissingCsrfToken() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "alice@example.com",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	void updateMyProfilePersistsTrimmedNamesAndMeReturnsUpdatedUser() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		String accessToken = accessTokenFromLogin("alice@example.com", "password123");

		mockMvc.perform(put("/api/v1/auth/me")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName": "  Alicia  ",
								  "lastName": "  Example-Smith  "
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Alicia"))
				.andExpect(jsonPath("$.lastName").value("Example-Smith"));

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("alice@example.com"))
				.andExpect(jsonPath("$.firstName").value("Alicia"))
				.andExpect(jsonPath("$.lastName").value("Example-Smith"));
	}

	@Test
	void changeMyPasswordRevokesRefreshTokensAndRequiresNewPasswordForLogin() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		mockMvc.perform(put("/api/v1/auth/me/password")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "password123",
								  "newPassword": "new-password123"
								}
								"""))
				.andExpect(status().isNoContent());

		performLogin("alice@example.com", "password123")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_credentials"))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));

		performLogin("alice@example.com", "new-password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.user.email").value("alice@example.com"));

		performRefresh(refreshCookie)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));
	}

	@Test
	void changeMyPasswordRejectsIncorrectCurrentPassword() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		String accessToken = accessTokenFromLogin("alice@example.com", "password123");

		mockMvc.perform(put("/api/v1/auth/me/password")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "wrong-password",
								  "newPassword": "new-password123"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("invalid_current_password"))
				.andExpect(jsonPath("$.message").value("Current password is incorrect"));
	}

	@Test
	void changeMyPasswordRejectsUnchangedPassword() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		String accessToken = accessTokenFromLogin("alice@example.com", "password123");

		mockMvc.perform(put("/api/v1/auth/me/password")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "password123",
								  "newPassword": "password123"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("password_unchanged"))
				.andExpect(jsonPath("$.message").value("New password must be different from the current password"));
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

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		performRefresh(refreshCookie)
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.appContext.organizationName").value("Alice Organization"))
				.andExpect(jsonPath("$.appContext.establishmentName").value("Alice Establishment"))
				.andExpect(jsonPath("$.appContext.organizationRole").value("ORG_MANAGER"));
	}

	@Test
	void refreshRejectsMissingCsrfTokenEvenWithRefreshCookie() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	void refreshRejectsReplayOfPreviouslyUsedRefreshToken() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String initialRefreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		MvcResult refreshResult = performRefresh(initialRefreshCookie)
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String rotatedRefreshCookie = Objects.requireNonNull(refreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		org.junit.jupiter.api.Assertions.assertNotEquals(initialRefreshCookie, rotatedRefreshCookie);

		performRefresh(initialRefreshCookie)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));

		performRefresh(rotatedRefreshCookie)
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME));
	}

	@Test
	void refreshRejectsMissingRefreshCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.with(csrf()))
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
	void logoutRevokesRefreshTokenAndClearsRefreshCookie() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		mockMvc.perform(post("/api/v1/auth/logout")
						.with(csrf())
						.cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
				.andExpect(status().isNoContent())
				.andExpect(cookie().value(REFRESH_COOKIE_NAME, ""))
				.andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0));

		String hashedRefreshToken = hashToken(refreshCookie);
		org.assertj.core.api.Assertions.assertThat(refreshTokenRepository.findByTokenHash(hashedRefreshToken))
				.isPresent()
				.get()
				.satisfies(token -> org.assertj.core.api.Assertions.assertThat(token.getRevokedAt()).isNotNull());
	}

	@Test
	void logoutRejectsMissingCsrfToken() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		mockMvc.perform(post("/api/v1/auth/logout")
						.cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	void logoutMakesRefreshTokenUnusableForFutureRefresh() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		mockMvc.perform(post("/api/v1/auth/logout")
						.with(csrf())
						.cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
				.andExpect(status().isNoContent());

		performRefresh(refreshCookie)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));
	}

	@Test
	void logoutWithoutRefreshCookieStillReturnsNoContentAndClearsCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().value(REFRESH_COOKIE_NAME, ""))
				.andExpect(cookie().maxAge(REFRESH_COOKIE_NAME, 0));
	}

	@Test
	void meRejectsMalformedBearerToken() throws Exception {
		performMe("not-a-jwt")
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("Malformed token")));
	}

	@Test
	void meRejectsExpiredBearerToken() throws Exception {
		User user = createUserWithOrganizationContext("alice@example.com", "password123");
		String expiredToken = issueAccessToken(
				jwtEncoder,
				user.getId(),
				user.getEmail(),
				Instant.parse("2026-04-07T07:00:00Z"),
				Instant.parse("2026-04-07T07:15:00Z"));

		performMe(expiredToken)
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("Jwt expired")));
	}

	@Test
	void meRejectsBearerTokenSignedWithDifferentSecret() throws Exception {
		User user = createUserWithOrganizationContext("alice@example.com", "password123");

		byte[] secret = "different-test-secret-different-test-1234".getBytes(StandardCharsets.UTF_8);
		SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
		JwtEncoder wrongEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		String tokenWithWrongSignature = issueAccessToken(
				wrongEncoder,
				user.getId(),
				user.getEmail(),
				Instant.parse("2026-04-07T08:00:00Z"),
				Instant.parse("2026-04-07T08:15:00Z"));

		performMe(tokenWithWrongSignature)
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("Invalid signature")));
	}

	@Test
	void meRejectsBearerTokenWithWrongIssuer() throws Exception {
		User user = createUserWithOrganizationContext("alice@example.com", "password123");
		Instant issuedAt = Instant.now().minusSeconds(60);
		Instant expiresAt = Instant.now().plusSeconds(900);
		String wrongIssuerToken = issueAccessToken(
				jwtEncoder,
				user.getId(),
				user.getEmail(),
				"wrong-issuer",
				java.util.List.of(securityProperties.getJwt().getAudience()),
				issuedAt,
				expiresAt);

		performMe(wrongIssuerToken)
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("iss claim")));
	}

	@Test
	void meRejectsBearerTokenWithMissingAudience() throws Exception {
		User user = createUserWithOrganizationContext("alice@example.com", "password123");
		Instant issuedAt = Instant.now().minusSeconds(60);
		Instant expiresAt = Instant.now().plusSeconds(900);
		String missingAudienceToken = issueAccessToken(
				jwtEncoder,
				user.getId(),
				user.getEmail(),
				securityProperties.getJwt().getIssuer(),
				null,
				issuedAt,
				expiresAt);

		performMe(missingAudienceToken)
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("audience")));
	}

	@Test
	void meRejectsBearerTokenWithWrongAudience() throws Exception {
		User user = createUserWithOrganizationContext("alice@example.com", "password123");
		Instant issuedAt = Instant.now().minusSeconds(60);
		Instant expiresAt = Instant.now().plusSeconds(900);
		String wrongAudienceToken = issueAccessToken(
				jwtEncoder,
				user.getId(),
				user.getEmail(),
				securityProperties.getJwt().getIssuer(),
				java.util.List.of("wrong-audience"),
				issuedAt,
				expiresAt);

		performMe(wrongAudienceToken)
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("invalid_token")))
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("audience")));
	}

	@Test
	void loginLocksOutAccountAfterRepeatedFailedAttempts() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		for (int attempt = 0; attempt < 5; attempt++) {
			performLogin("alice@example.com", "wrong-password", "203.0.113." + (attempt + 1))
					.andExpect(status().isUnauthorized())
					.andExpect(cookie().doesNotExist(REFRESH_COOKIE_NAME))
					.andExpect(jsonPath("$.message").value("Invalid email or password"));
		}

		performLogin("alice@example.com", "password123", "198.51.100.10")
				.andExpect(status().isUnauthorized())
				.andExpect(cookie().doesNotExist(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void loginLocksOutIpAfterRepeatedFailedAttemptsAcrossAccounts() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");
		createUserWithOrganizationContext("bob@example.com", "password123");

		for (int attempt = 0; attempt < 5; attempt++) {
			String email = attempt % 2 == 0 ? "alice@example.com" : "bob@example.com";
			performLogin(email, "wrong-password", "203.0.113.50")
					.andExpect(status().isUnauthorized())
					.andExpect(cookie().doesNotExist(REFRESH_COOKIE_NAME));
		}

		performLogin("alice@example.com", "password123", "203.0.113.50")
				.andExpect(status().isUnauthorized())
				.andExpect(cookie().doesNotExist(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));

		performLogin("alice@example.com", "password123", "198.51.100.20")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.user.email").value("alice@example.com"));
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
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
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
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME));

		for (int attempt = 0; attempt < 4; attempt++) {
			performLogin("alice@example.com", "wrong-password")
					.andExpect(status().isUnauthorized());
		}

		performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andExpect(jsonPath("$.user.email").value("alice@example.com"));
	}

	@Test
	void scopedMembershipUsesAccessibleEstablishmentInAppContext() throws Exception {
		User user = new User("alice@example.com", "Alice", "Example", passwordEncoder.encode("password123"), true, Set.of());
		userRepository.saveAndFlush(user);
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Alice Organization", OrganizationStatus.ACTIVE));
		establishmentRepository.saveAndFlush(
				new Establishment(organization, "Blocked Establishment", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		Establishment accessibleEstablishment = establishmentRepository.saveAndFlush(
				new Establishment(organization, "Accessible Establishment", EstablishmentType.BAR, EstablishmentStatus.ACTIVE));
		OrganizationMembership membership = new OrganizationMembership(
				organization,
				user,
				OrganizationRole.ORG_EMPLOYEE,
				true,
				false
		);
		membership.replaceAccessibleEstablishments(java.util.List.of(accessibleEstablishment));
		organizationMembershipRepository.saveAndFlush(membership);

		performLogin("alice@example.com", "password123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.appContext.organizationName").value("Alice Organization"))
				.andExpect(jsonPath("$.appContext.establishmentName").value("Accessible Establishment"));
	}

	@Test
	void invitedUserCanAcceptInvitationAndThenLogIn() throws Exception {
		User orgAdmin = createInvitingOrgAdmin("orgadmin@example.com");
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Invite Organization", OrganizationStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, orgAdmin, OrganizationRole.ORG_ADMIN, true));

		String adminAccessToken = accessTokenFor("orgadmin@example.com", "password123");

		String inviteResponse = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organization.getId()))
						.with(csrf())
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
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isNoContent());

		performLogin("invitee@example.com", "newpassword123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("invitee@example.com"));
	}

	@Test
	void inactiveInvitedUserRemainsInactiveAfterAcceptingInvitation() throws Exception {
		User orgAdmin = createInvitingOrgAdmin("orgadmin@example.com");
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Invite Organization", OrganizationStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, orgAdmin, OrganizationRole.ORG_ADMIN, true));

		String adminAccessToken = accessTokenFor("orgadmin@example.com", "password123");

		String inviteResponse = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organization.getId()))
						.with(csrf())
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
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isNoContent());

		performLogin("inactive.invitee@example.com", "newpassword123")
				.andExpect(status().isUnauthorized());
	}

	@Test
	void inactiveInvitedUserCanLogInAfterMembershipIsActivated() throws Exception {
		User orgAdmin = createInvitingOrgAdmin("orgadmin@example.com");
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Invite Organization", OrganizationStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, orgAdmin, OrganizationRole.ORG_ADMIN, true));

		String adminAccessToken = accessTokenFor("orgadmin@example.com", "password123");

		String inviteResponse = mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organization.getId()))
						.with(csrf())
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
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "newpassword123"
								}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(organization.getId(), membershipId))
						.with(csrf())
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

		performLogin("recoverable.invitee@example.com", "newpassword123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("recoverable.invitee@example.com"));
	}

	@Test
	void refreshLocksOutIpAfterRepeatedFailedAttempts() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123", "198.51.100.31")
				.andExpect(status().isOk())
				.andExpect(cookie().exists("kontrolla_refresh_token"))
				.andReturn();

		String refreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		for (int attempt = 0; attempt < 5; attempt++) {
			performRefresh("forged-refresh-token", "203.0.113.60")
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
					.andExpect(jsonPath("$.message").value("Refresh token is invalid"));
		}

		performRefresh(refreshCookie, "203.0.113.60")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));

		performRefresh(refreshCookie, "198.51.100.61")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME));
	}

	@Test
	void refreshLocksOutAccountAfterRepeatedReplayAttempts() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123", "198.51.100.70")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String initialRefreshCookie = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		MvcResult refreshResult = performRefresh(initialRefreshCookie, "198.51.100.70")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String rotatedRefreshCookie = Objects.requireNonNull(refreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		for (int attempt = 0; attempt < 5; attempt++) {
			performRefresh(initialRefreshCookie, "203.0.113." + (70 + attempt))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
					.andExpect(jsonPath("$.message").value("Refresh token is invalid"));
		}

		performRefresh(rotatedRefreshCookie, "198.51.100.75")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("invalid_refresh_token"))
				.andExpect(jsonPath("$.message").value("Refresh token is invalid"));

		mutableClock.advanceSeconds(10 * 60 + 1);

		performRefresh(rotatedRefreshCookie, "198.51.100.75")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME));
	}

	@Test
	void successfulRefreshResetsFailedAttemptCounters() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");

		MvcResult loginResult = performLogin("alice@example.com", "password123", "198.51.100.80")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String tokenA = Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		MvcResult firstRefreshResult = performRefresh(tokenA, "198.51.100.80")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String tokenB = Objects.requireNonNull(firstRefreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		for (int attempt = 0; attempt < 4; attempt++) {
			performRefresh(tokenA, "198.51.100.80")
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("invalid_refresh_token"));
		}

		MvcResult secondRefreshResult = performRefresh(tokenB, "198.51.100.80")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		String tokenC = Objects.requireNonNull(secondRefreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

		for (int attempt = 0; attempt < 4; attempt++) {
			performRefresh(tokenB, "198.51.100.80")
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("invalid_refresh_token"));
		}

		performRefresh(tokenC, "198.51.100.80")
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME));
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(String email, String password) throws Exception {
		return performLogin(email, password, "127.0.0.1");
	}

	private String accessTokenFromLogin(String email, String password) throws Exception {
		MvcResult loginResult = performLogin(email, password)
				.andExpect(status().isOk())
				.andExpect(cookie().exists(REFRESH_COOKIE_NAME))
				.andReturn();

		return objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.get("accessToken")
				.asText();
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(String email, String password, String remoteAddr) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
				.with(csrf())
				.with(remoteAddr(remoteAddr))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(email, password)));
	}

	private org.springframework.test.web.servlet.ResultActions performMe(String accessToken) throws Exception {
		return mockMvc.perform(get("/api/v1/auth/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private org.springframework.test.web.servlet.ResultActions performRefresh(String refreshCookie) throws Exception {
		return performRefresh(refreshCookie, "127.0.0.1");
	}

	private org.springframework.test.web.servlet.ResultActions performRefresh(String refreshCookie, String remoteAddr) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh")
				.with(csrf())
				.with(remoteAddr(remoteAddr))
				.cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)));
	}

	private RequestPostProcessor remoteAddr(String remoteAddr) {
		return request -> {
			request.setRemoteAddr(remoteAddr);
			return request;
		};
	}

	private User createUserWithOrganizationContext(String email, String password) {
		User user = new User(email, "Alice", "Example", passwordEncoder.encode(password), true, Set.of());
		userRepository.saveAndFlush(user);
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Alice Organization", OrganizationStatus.ACTIVE));
		establishmentRepository.saveAndFlush(
				new Establishment(organization, "Alice Establishment", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, user, OrganizationRole.ORG_MANAGER, true));
		return user;
	}

	private User createInvitingOrgAdmin(String email) {
		User platformAdmin = new User(
				"admin@example.com",
				"Admin",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of(GlobalRole.PLATFORM_ADMIN)
		);
		User orgAdmin = new User(
				email,
				"Org",
				"Admin",
				passwordEncoder.encode("password123"),
				true,
				Set.of()
		);
		userRepository.saveAndFlush(platformAdmin);
		return userRepository.saveAndFlush(orgAdmin);
	}

	private String accessTokenFor(String email, String password) throws Exception {
		String response = performLogin(email, password)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response).get("accessToken").asText();
	}

	private String issueAccessToken(
			JwtEncoder encoder,
			UUID userId,
			String email,
			Instant issuedAt,
			Instant expiresAt
	) {
		return issueAccessToken(
				encoder,
				userId,
				email,
				securityProperties.getJwt().getIssuer(),
				java.util.List.of(securityProperties.getJwt().getAudience()),
				issuedAt,
				expiresAt
		);
	}

	private String issueAccessToken(
			JwtEncoder encoder,
			UUID userId,
			String email,
			String issuer,
			java.util.List<String> audience,
			Instant issuedAt,
			Instant expiresAt
	) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(userId.toString())
				.claim("email", email)
				.claim("roles", Set.of())
				.build();

		if (audience != null) {
			claims = JwtClaimsSet.from(claims).audience(audience).build();
		}

		return encoder.encode(
				JwtEncoderParameters.from(
						JwsHeader.with(MacAlgorithm.HS256).build(),
						claims
				)
		).getTokenValue();
	}

	private String hashToken(String rawRefreshToken) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(hash);
		} catch (java.security.NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 not available", exception);
		}
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
