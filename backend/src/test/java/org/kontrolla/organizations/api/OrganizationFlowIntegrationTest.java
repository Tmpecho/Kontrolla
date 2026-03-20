package org.kontrolla.organizations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAll();
		establishmentRepository.deleteAll();
		membershipRepository.deleteAll();
		organizationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void platformAdminCanCreateOrganizationAndManagerCanCreateEstablishment() throws Exception {
		User admin = new User(
				"admin@example.com",
				"Admin",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of(GlobalRole.PLATFORM_ADMIN)
		);
		User manager = new User(
				"manager@example.com",
				"Manager",
				"User",
				passwordEncoder.encode("password123"),
				true,
				Set.of()
		);
		userRepository.saveAndFlush(admin);
		userRepository.saveAndFlush(manager);

		String adminToken = login("admin@example.com", "password123");

		String organizationResponse = mockMvc.perform(post("/api/v1/admin/organizations")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Kontrolla AS"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Kontrolla AS"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String organizationId = objectMapper.readTree(organizationResponse).get("id").asText();

		mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_MANAGER"
								}
								""".formatted(manager.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("ORG_MANAGER"));

		String managerToken = login("manager@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(organizationId));

		mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Downtown Bar",
								  "type": "BAR"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Downtown Bar"))
				.andExpect(jsonPath("$.type").value("BAR"));
	}

	private String login(String email, String password) throws Exception {
		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
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

		JsonNode json = objectMapper.readTree(loginResponse);
		return json.get("accessToken").asText();
	}
}
