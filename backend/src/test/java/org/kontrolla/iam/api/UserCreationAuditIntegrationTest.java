package org.kontrolla.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserCreationAuditIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private AuditEventRepository auditEventRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void adminUserCreationPersistsAuditEvent() throws Exception {
    createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
    String adminToken = login("admin@example.com", "password123");

    mockMvc
        .perform(
            post("/api/v1/admin/users")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "email": "new-admin@example.com",
								  "firstName": "New",
								  "lastName": "Admin",
								  "password": "password123",
								  "active": false,
								  "globalRoles": ["PLATFORM_ADMIN"]
								}
								"""))
        .andExpect(status().isCreated());

    AuditEvent auditEvent = findSingleAuditEvent(AuditAction.USER_CREATE, "admin_user_created");
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(auditEvent.getActorEmail()).isEqualTo("admin@example.com");
    assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.USER);
    assertThat(auditEvent.getTargetId()).isNotNull();
    assertThat(auditEvent.getOrganizationId()).isNull();
    assertThat(auditEvent.getRequestPath()).isEqualTo("/api/v1/admin/users");

    JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
    assertThat(metadata.path("createdEmail").asText()).isEqualTo("new-admin@example.com");
    assertThat(metadata.path("active").asBoolean()).isFalse();
    assertThat(metadata.path("creationPath").asText()).isEqualTo("admin");
    assertThat(metadata.path("globalRoles")).hasSize(1);
    assertThat(metadata.path("globalRoles").get(0).asText()).isEqualTo("PLATFORM_ADMIN");
  }

  @Test
  void managedUserProvisionPersistsUserCreateAuditEvent() throws Exception {
    createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
    createUser("orgadmin@example.com", "Org", "Admin", Set.of());

    String adminToken = login("admin@example.com", "password123");
    String organizationId = createOrganization(adminToken, "Managed Org");
    String orgAdminId =
        userRepository
            .findByEmailIgnoreCase("orgadmin@example.com")
            .orElseThrow()
            .getId()
            .toString();
    addMembership(adminToken, organizationId, orgAdminId, "ORG_ADMIN");

    String orgAdminToken = login("orgadmin@example.com", "password123");

    mockMvc
        .perform(
            post("/api/v1/organizations/%s/members/managed-users".formatted(organizationId))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "email": "new.member@example.com",
								  "firstName": "New",
								  "lastName": "Member",
								  "role": "ORG_EMPLOYEE",
								  "active": true
								}
								"""))
        .andExpect(status().isCreated());

    AuditEvent auditEvent = findSingleAuditEvent(AuditAction.USER_CREATE, "managed_user_created");
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(auditEvent.getActorEmail()).isEqualTo("orgadmin@example.com");
    assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.USER);
    assertThat(auditEvent.getTargetId()).isNotNull();
    assertThat(auditEvent.getOrganizationId()).isEqualTo(java.util.UUID.fromString(organizationId));
    assertThat(auditEvent.getRequestPath())
        .isEqualTo("/api/v1/organizations/%s/members/managed-users".formatted(organizationId));

    JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
    assertThat(metadata.path("createdEmail").asText()).isEqualTo("new.member@example.com");
    assertThat(metadata.path("active").asBoolean()).isFalse();
    assertThat(metadata.path("creationPath").asText()).isEqualTo("managed_invite");
    assertThat(metadata.path("globalRoles")).isEmpty();
  }

  private AuditEvent findSingleAuditEvent(AuditAction action, String resultCode) {
    List<AuditEvent> matchingEvents =
        auditEventRepository.findAll().stream()
            .filter(auditEvent -> auditEvent.getAction() == action)
            .filter(auditEvent -> resultCode.equals(auditEvent.getResultCode()))
            .toList();
    assertThat(matchingEvents).hasSize(1);
    return matchingEvents.getFirst();
  }

  private User createUser(
      String email, String firstName, String lastName, Set<GlobalRole> globalRoles) {
    User user =
        new User(
            email, firstName, lastName, passwordEncoder.encode("password123"), true, globalRoles);
    return userRepository.saveAndFlush(user);
  }

  private String login(String email, String password) throws Exception {
    String loginResponse =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
								{
								  "email": "%s",
								  "password": "%s"
								}
								"""
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(loginResponse).get("accessToken").asText();
  }

  private String createOrganization(String adminToken, String name) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/api/v1/admin/organizations")
                    .with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
								{
								  "name": "%s"
								}
								"""
                            .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readTree(response).get("id").asText();
  }

  private void addMembership(String adminToken, String organizationId, String userId, String role)
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/organizations/%s/members".formatted(organizationId))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "userId": "%s",
								  "role": "%s"
								}
								"""
                        .formatted(userId, role)))
        .andExpect(status().isCreated());
  }
}
