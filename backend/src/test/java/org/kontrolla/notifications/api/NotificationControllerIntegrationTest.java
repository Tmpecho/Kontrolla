package org.kontrolla.notifications.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.RefreshTokenRepository;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.notifications.domain.Notification;
import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationType;
import org.kontrolla.notifications.infrastructure.NotificationRepository;
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

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

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
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void notificationEndpointsAreScopedToTheAuthenticatedUser() throws Exception {
		User recipient = createUser("recipient@example.com");
		User otherUser = createUser("other@example.com");
		Organization organization = createOrganization("Notification Org");
		Establishment establishment = createEstablishment(organization, "Notification Restaurant");
		createMembership(organization, recipient, OrganizationRole.ORG_EMPLOYEE, true);
		createMembership(organization, otherUser, OrganizationRole.ORG_EMPLOYEE, true);

		Notification unread = createNotification(
				recipient.getId(),
				organization.getId(),
				establishment.getId(),
				NotificationType.CHECKLIST_ASSIGNED,
				"Morning shift",
				"You were assigned the morning shift checklist."
		);
		Notification read = createNotification(
				recipient.getId(),
				organization.getId(),
				establishment.getId(),
				NotificationType.DEVIATION_STATUS_CHANGED,
				"Fridge deviation",
				"The deviation status changed."
		);
		read.markRead(Instant.parse("2026-04-07T10:00:00Z"));
		notificationRepository.saveAndFlush(read);
		createNotification(
				otherUser.getId(),
				organization.getId(),
				establishment.getId(),
				NotificationType.DEVIATION_NOTE_ADDED,
				"Other user notification",
				"This should not be visible."
		);

		String token = login("recipient@example.com", "password123");

		mockMvc.perform(get("/api/v1/notifications")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].id").value(unread.getId().toString()))
				.andExpect(jsonPath("$.items[0].readAt").doesNotExist())
				.andExpect(jsonPath("$.items[1].id").value(read.getId().toString()));

		mockMvc.perform(get("/api/v1/notifications?status=UNREAD")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(unread.getId().toString()));

		mockMvc.perform(get("/api/v1/notifications/unread-count")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadCount").value(1));
	}

	@Test
	void notificationReadEndpointsPersistReadState() throws Exception {
		User recipient = createUser("read-recipient@example.com");
		Organization organization = createOrganization("Notification Read Org");
		Establishment establishment = createEstablishment(organization, "Notification Read Restaurant");
		createMembership(organization, recipient, OrganizationRole.ORG_EMPLOYEE, true);

		Notification first = createNotification(
				recipient.getId(),
				organization.getId(),
				establishment.getId(),
				NotificationType.CHECKLIST_ASSIGNED,
				"Opening checklist",
				"You were assigned the opening checklist."
		);
		Notification second = createNotification(
				recipient.getId(),
				organization.getId(),
				establishment.getId(),
				NotificationType.CHECKLIST_OVERDUE,
				"Closing checklist overdue",
				"The assigned checklist is overdue."
		);

		String token = login("read-recipient@example.com", "password123");

		mockMvc.perform(post("/api/v1/notifications/%s/read".formatted(first.getId()))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(first.getId().toString()))
				.andExpect(jsonPath("$.readAt").isNotEmpty());

		mockMvc.perform(get("/api/v1/notifications/unread-count")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadCount").value(1));

		mockMvc.perform(post("/api/v1/notifications/read-all")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/notifications/unread-count")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadCount").value(0));

		mockMvc.perform(get("/api/v1/notifications?status=UNREAD")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));

		notificationRepository.findById(second.getId())
				.filter(notification -> notification.getReadAt() != null)
				.orElseThrow();
	}

	private Notification createNotification(
			UUID recipientUserId,
			UUID organizationId,
			UUID establishmentId,
			NotificationType type,
			String title,
			String message
	) {
		return notificationRepository.saveAndFlush(new Notification(
				recipientUserId,
				organizationId,
				establishmentId,
				ChecklistServiceArea.IK_MAT,
				type,
				title,
				message,
				type.name().startsWith("CHECKLIST") ? NotificationResourceType.CHECKLIST_RUN : NotificationResourceType.DEVIATION,
				UUID.randomUUID()
		));
	}

	private User createUser(String email) {
		User user = new User(
				email,
				"Test",
				"User",
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
				EstablishmentType.RESTAURANT,
				EstablishmentStatus.ACTIVE
		);
		return establishmentRepository.saveAndFlush(establishment);
	}

	private OrganizationMembership createMembership(
			Organization organization,
			User user,
			OrganizationRole role,
			boolean active
	) {
		OrganizationMembership membership = new OrganizationMembership(organization, user, role, active);
		return organizationMembershipRepository.saveAndFlush(membership);
	}

	private String login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
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

		JsonNode json = objectMapper.readTree(response);
		return json.get("accessToken").asText();
	}
}
