package org.kontrolla.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.audit.domain.AuditEvent;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;
import org.kontrolla.audit.infrastructure.AuditEventRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.application.AuthAttemptThrottleService;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAuditIntegrationTest {

  private static final String REFRESH_COOKIE_NAME = "kontrolla_refresh_token";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private AuditEventRepository auditEventRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private OrganizationMembershipRepository organizationMembershipRepository;

  @Autowired private EstablishmentRepository establishmentRepository;

  @Autowired private AuthAttemptThrottleService authAttemptThrottleService;

  @Autowired private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
    authAttemptThrottleService.clear();
  }

  @Test
  void failedLoginPersistsAuditEvent() throws Exception {
    createUserWithOrganizationContext("alice@example.com", "password123");

    performLogin("alice@example.com", "wrong-password", "203.0.113.10")
        .andExpect(status().isUnauthorized());

    AuditEvent auditEvent = singleAuditEvent();
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_LOGIN);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    assertThat(auditEvent.getResultCode()).isEqualTo("invalid_credentials");
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.ANONYMOUS);
    assertThat(auditEvent.getRequestPath()).isEqualTo("/api/v1/auth/login");
    assertThat(auditEvent.getClientIp()).isEqualTo("203.0.113.10");

    JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
    assertThat(metadata.path("attemptedEmail").asText()).isEqualTo("alice@example.com");
    assertThat(metadata.has("throttleDimension")).isFalse();
  }

  @Test
  void throttledLoginPersistsAuditEvent() throws Exception {
    createUserWithOrganizationContext("alice@example.com", "password123");

    for (int attempt = 0; attempt < 5; attempt++) {
      performLogin("alice@example.com", "wrong-password", "198.51.100." + attempt)
          .andExpect(status().isUnauthorized());
    }

    performLogin("alice@example.com", "password123", "203.0.113.20")
        .andExpect(status().isUnauthorized());

    AuditEvent auditEvent = auditEvent(AuditAction.AUTH_LOGIN, "throttled");
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_LOGIN);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    assertThat(auditEvent.getResultCode()).isEqualTo("throttled");

    JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
    assertThat(metadata.path("attemptedEmail").asText()).isEqualTo("alice@example.com");
    assertThat(metadata.path("throttleDimension").asText()).isEqualTo("ACCOUNT");
  }

  @Test
  void successfulRefreshPersistsAuditEvent() throws Exception {
    createUserWithOrganizationContext("alice@example.com", "password123");

    MvcResult loginResult =
        performLogin("alice@example.com", "password123", "203.0.113.30")
            .andExpect(status().isOk())
            .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
            .andReturn();

    String refreshCookie =
        Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

    performRefresh(refreshCookie, "203.0.113.31")
        .andExpect(status().isOk())
        .andExpect(cookie().exists(REFRESH_COOKIE_NAME));

    AuditEvent auditEvent = auditEvent(AuditAction.AUTH_REFRESH, "success");
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_REFRESH);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    assertThat(auditEvent.getResultCode()).isEqualTo("success");
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.REFRESH_TOKEN);
    assertThat(auditEvent.getTargetId()).isNotNull();
    assertThat(auditEvent.getOrganizationId()).isNotNull();
    assertThat(auditEvent.getRequestPath()).isEqualTo("/api/v1/auth/refresh");
    assertThat(auditEvent.getClientIp()).isEqualTo("203.0.113.31");

    JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
    assertThat(metadata.path("userEmail").asText()).isEqualTo("alice@example.com");
  }

  @Test
  void failedRefreshPersistsAuditEvent() throws Exception {
    performRefresh("forged-refresh-token", "203.0.113.40").andExpect(status().isUnauthorized());

    AuditEvent auditEvent = singleAuditEvent();
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_REFRESH);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    assertThat(auditEvent.getResultCode()).isEqualTo("invalid_refresh_token");
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.ANONYMOUS);
    assertThat(auditEvent.getRequestPath()).isEqualTo("/api/v1/auth/refresh");
    assertThat(auditEvent.getClientIp()).isEqualTo("203.0.113.40");
  }

  @Test
  void throttledRefreshPersistsAuditEvent() throws Exception {
    createUserWithOrganizationContext("alice@example.com", "password123");

    for (int attempt = 0; attempt < 5; attempt++) {
      performRefresh("forged-refresh-token", "203.0.113.50").andExpect(status().isUnauthorized());
    }

    performRefresh("forged-refresh-token", "203.0.113.50").andExpect(status().isUnauthorized());

    AuditEvent auditEvent = auditEvent(AuditAction.AUTH_REFRESH, "throttled");
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_REFRESH);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    assertThat(auditEvent.getResultCode()).isEqualTo("throttled");

    JsonNode metadata = objectMapper.readTree(auditEvent.getMetadataJson());
    assertThat(metadata.path("throttleDimension").asText()).isEqualTo("IP");
  }

  @Test
  void logoutPersistsSuccessAuditEvent() throws Exception {
    createUserWithOrganizationContext("alice@example.com", "password123");

    MvcResult loginResult =
        performLogin("alice@example.com", "password123", "203.0.113.60")
            .andExpect(status().isOk())
            .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
            .andReturn();

    String refreshCookie =
        Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(remoteAddr("203.0.113.61"))
                .cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
        .andExpect(status().isNoContent());

    AuditEvent auditEvent = auditEvent(AuditAction.AUTH_LOGOUT, "revoked");
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_LOGOUT);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
    assertThat(auditEvent.getResultCode()).isEqualTo("revoked");
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.REFRESH_TOKEN);
    assertThat(auditEvent.getTargetId()).isNotNull();
    assertThat(auditEvent.getRequestPath()).isEqualTo("/api/v1/auth/logout");
    assertThat(auditEvent.getClientIp()).isEqualTo("203.0.113.61");
  }

  @Test
  void logoutWithoutRefreshCookiePersistsIgnoredAuditEvent() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(remoteAddr("203.0.113.70")))
        .andExpect(status().isNoContent());

    AuditEvent auditEvent = singleAuditEvent();
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_LOGOUT);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.IGNORED);
    assertThat(auditEvent.getResultCode()).isEqualTo("missing_refresh_token");
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.ANONYMOUS);
    assertThat(auditEvent.getRequestPath()).isEqualTo("/api/v1/auth/logout");
    assertThat(auditEvent.getClientIp()).isEqualTo("203.0.113.70");
  }

  @Test
  void logoutWithAlreadyRevokedTokenPersistsIgnoredAuditEvent() throws Exception {
    createUserWithOrganizationContext("alice@example.com", "password123");

    MvcResult loginResult =
        performLogin("alice@example.com", "password123", "203.0.113.80")
            .andExpect(status().isOk())
            .andExpect(cookie().exists(REFRESH_COOKIE_NAME))
            .andReturn();

    String refreshCookie =
        Objects.requireNonNull(loginResult.getResponse().getCookie(REFRESH_COOKIE_NAME)).getValue();

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(remoteAddr("203.0.113.81"))
                .cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(remoteAddr("203.0.113.82"))
                .cookie(new jakarta.servlet.http.Cookie(REFRESH_COOKIE_NAME, refreshCookie)))
        .andExpect(status().isNoContent());

    AuditEvent auditEvent = auditEvent(AuditAction.AUTH_LOGOUT, "token_not_active");
    assertThat(auditEvent.getAction()).isEqualTo(AuditAction.AUTH_LOGOUT);
    assertThat(auditEvent.getOutcome()).isEqualTo(AuditOutcome.IGNORED);
    assertThat(auditEvent.getResultCode()).isEqualTo("token_not_active");
    assertThat(auditEvent.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(auditEvent.getTargetType()).isEqualTo(AuditTargetType.REFRESH_TOKEN);
    assertThat(auditEvent.getTargetId()).isNotNull();
    assertThat(auditEvent.getClientIp()).isEqualTo("203.0.113.82");
  }

  private AuditEvent singleAuditEvent() {
    List<AuditEvent> auditEvents = auditEventRepository.findAll();
    assertThat(auditEvents).hasSize(1);
    return auditEvents.getFirst();
  }

  private AuditEvent auditEvent(AuditAction action, String resultCode) {
    List<AuditEvent> auditEvents =
        auditEventRepository.findAll().stream()
            .filter(auditEvent -> auditEvent.getAction() == action)
            .filter(auditEvent -> resultCode.equals(auditEvent.getResultCode()))
            .toList();
    assertThat(auditEvents).hasSize(1);
    return auditEvents.getFirst();
  }

  private org.springframework.test.web.servlet.ResultActions performLogin(
      String email, String password, String remoteAddr) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/login")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .with(remoteAddr(remoteAddr))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
						{
						  "email": "%s",
						  "password": "%s"
						}
						"""
                    .formatted(email, password)));
  }

  private org.springframework.test.web.servlet.ResultActions performRefresh(
      String refreshCookie, String remoteAddr) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/refresh")
            .with(SecurityMockMvcRequestPostProcessors.csrf())
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
    User user =
        new User(email, "Alice", "Example", passwordEncoder.encode(password), true, Set.of());
    userRepository.saveAndFlush(user);
    Organization organization =
        organizationRepository.saveAndFlush(
            new Organization("Alice Organization", OrganizationStatus.ACTIVE));
    establishmentRepository.saveAndFlush(
        new Establishment(
            organization,
            "Alice Establishment",
            EstablishmentType.RESTAURANT,
            EstablishmentStatus.ACTIVE));
    organizationMembershipRepository.saveAndFlush(
        new OrganizationMembership(organization, user, OrganizationRole.ORG_MANAGER, true));
    return user;
  }
}
