package org.kontrolla.iam.api;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthCsrfFlowIntegrationTest {

	@LocalServerPort
	private int port;

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
	void csrfEndpointReturnsTokenBootstrapPayload() throws Exception {
		HttpClientWithCookies client = newClient();

		HttpResponse<String> csrfResponse = client.httpClient().send(
				HttpRequest.newBuilder(uri("/api/v1/auth/csrf"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString()
		);

		org.assertj.core.api.Assertions.assertThat(csrfResponse.statusCode()).isEqualTo(200);

		JsonNode csrfJson = objectMapper.readTree(csrfResponse.body());
		HttpCookie csrfCookie = client.cookieManager().getCookieStore().get(uri("/")).stream()
				.filter(cookie -> "XSRF-TOKEN".equals(cookie.getName()))
				.findFirst()
				.orElseThrow();

		org.assertj.core.api.Assertions.assertThat(csrfJson.get("token").asText()).isEqualTo(csrfCookie.getValue());
		org.assertj.core.api.Assertions.assertThat(csrfJson.get("headerName").asText()).isEqualTo("X-XSRF-TOKEN");
		org.assertj.core.api.Assertions.assertThat(csrfJson.get("parameterName").asText()).isEqualTo("_csrf");
	}

	@Test
	void loginSucceedsWithFetchedCsrfCookieAndHeader() throws Exception {
		createUserWithOrganizationContext("alice@example.com", "password123");
		HttpClientWithCookies client = newClient();

		HttpResponse<String> csrfResponse = client.httpClient().send(
				HttpRequest.newBuilder(uri("/api/v1/auth/csrf"))
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofString()
		);
		JsonNode csrfJson = objectMapper.readTree(csrfResponse.body());
		String csrfToken = csrfJson.get("token").asText();
		String csrfHeaderName = csrfJson.get("headerName").asText();

		HttpResponse<String> loginResponse = client.httpClient().send(
				HttpRequest.newBuilder(uri("/api/v1/auth/login"))
						.header("Content-Type", "application/json")
						.header(csrfHeaderName, csrfToken)
						.POST(HttpRequest.BodyPublishers.ofString("""
								{
								  "email": "alice@example.com",
								  "password": "password123"
								}
								"""))
						.build(),
				HttpResponse.BodyHandlers.ofString()
		);

		org.assertj.core.api.Assertions.assertThat(loginResponse.statusCode()).isEqualTo(200);
		org.assertj.core.api.Assertions.assertThat(loginResponse.headers().allValues("set-cookie"))
				.anyMatch(headerValue -> headerValue.startsWith("kontrolla_refresh_token="));
		org.assertj.core.api.Assertions.assertThat(objectMapper.readTree(loginResponse.body()).at("/user/email").asText())
				.isEqualTo("alice@example.com");
	}

	private void createUserWithOrganizationContext(String email, String password) {
		User user = userRepository.saveAndFlush(
				new User(email, "Alice", "Example", passwordEncoder.encode(password), true, Set.of()));
		Organization organization = organizationRepository.saveAndFlush(
				new Organization("Alice Organization", OrganizationStatus.ACTIVE));
		establishmentRepository.saveAndFlush(
				new Establishment(organization, "Alice Establishment", EstablishmentType.RESTAURANT, EstablishmentStatus.ACTIVE));
		organizationMembershipRepository.saveAndFlush(
				new OrganizationMembership(organization, user, OrganizationRole.ORG_MANAGER, true));
	}

	private HttpClientWithCookies newClient() {
		CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		HttpClient httpClient = HttpClient.newBuilder()
				.cookieHandler(cookieManager)
				.build();
		return new HttpClientWithCookies(httpClient, cookieManager);
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private record HttpClientWithCookies(HttpClient httpClient, CookieManager cookieManager) {
	}
}
