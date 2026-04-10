package org.kontrolla.establishments.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.JwtService;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstablishmentServingHoursControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private OrganizationMembershipRepository organizationMembershipRepository;

  @Autowired private EstablishmentRepository establishmentRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @Autowired private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void managerCanUpdateAndReadServingHours() throws Exception {
    User manager = createUser("serving-hours-api-manager@example.com");
    Organization organization = createOrganization("Serving Hours API Org");
    Establishment establishment = createEstablishment(organization, "City Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    String token = issueAccessToken(manager);

    List<UpdateServingHoursDayRequest> request =
        List.of(
            new UpdateServingHoursDayRequest(
                DayOfWeek.MONDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.TUESDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.WEDNESDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.THURSDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.FRIDAY, false, LocalTime.of(10, 0), LocalTime.of(23, 30)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.SATURDAY, false, LocalTime.of(12, 0), LocalTime.of(23, 30)),
            new UpdateServingHoursDayRequest(DayOfWeek.SUNDAY, true, null, null));

    mockMvc
        .perform(
            put("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(7))
        .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$[0].closed").value(false))
        .andExpect(jsonPath("$[0].opensAt").value("10:00:00"))
        .andExpect(jsonPath("$[0].closesAt").value("22:00:00"))
        .andExpect(jsonPath("$[6].dayOfWeek").value("SUNDAY"))
        .andExpect(jsonPath("$[6].closed").value(true))
        .andExpect(jsonPath("$[6].opensAt").doesNotExist())
        .andExpect(jsonPath("$[6].closesAt").doesNotExist());

    mockMvc
        .perform(
            get("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(7))
        .andExpect(jsonPath("$[4].dayOfWeek").value("FRIDAY"))
        .andExpect(jsonPath("$[4].closesAt").value("23:30:00"))
        .andExpect(jsonPath("$[6].dayOfWeek").value("SUNDAY"))
        .andExpect(jsonPath("$[6].closed").value(true));
  }

  @Test
  void employeeCanReadServingHoursButCannotUpdateThem() throws Exception {
    User manager = createUser("serving-hours-api-read-manager@example.com");
    User employee = createUser("serving-hours-api-employee@example.com");
    Organization organization = createOrganization("Serving Hours API Access Org");
    Establishment establishment = createEstablishment(organization, "Neighborhood Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);

    String managerToken = issueAccessToken(manager);
    String employeeToken = issueAccessToken(employee);

    mockMvc
        .perform(
            put("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        openWeekRequest(LocalTime.of(9, 0), LocalTime.of(21, 0)))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(7))
        .andExpect(jsonPath("$[0].opensAt").value("09:00:00"));

    mockMvc
        .perform(
            put("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        openWeekRequest(LocalTime.of(11, 0), LocalTime.of(23, 0)))))
        .andExpect(status().isForbidden());
  }

  @Test
  void getServingHoursReturnsClosedWeekWhenNotConfigured() throws Exception {
    User employee = createUser("serving-hours-api-default@example.com");
    Organization organization = createOrganization("Serving Hours API Default Org");
    Establishment establishment = createEstablishment(organization, "Default Cafe");
    createMembership(organization, employee, OrganizationRole.ORG_EMPLOYEE, true);

    String token = issueAccessToken(employee);

    mockMvc
        .perform(
            get("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(7))
        .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$[0].closed").value(true))
        .andExpect(jsonPath("$[6].dayOfWeek").value("SUNDAY"))
        .andExpect(jsonPath("$[6].closed").value(true));
  }

  @Test
  void updateServingHoursRejectsInvalidPayload() throws Exception {
    User manager = createUser("serving-hours-api-invalid@example.com");
    Organization organization = createOrganization("Serving Hours API Invalid Org");
    Establishment establishment = createEstablishment(organization, "Invalid Payload Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);

    String token = issueAccessToken(manager);

    List<UpdateServingHoursDayRequest> invalidRequest =
        List.of(
            new UpdateServingHoursDayRequest(
                DayOfWeek.MONDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.TUESDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.WEDNESDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.THURSDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.FRIDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            new UpdateServingHoursDayRequest(
                DayOfWeek.SATURDAY, false, LocalTime.of(10, 0), LocalTime.of(22, 0)));

    mockMvc
        .perform(
            put("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organization.getId(), establishment.getId()))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("invalid_serving_hours"));
  }

  @Test
  void memberFromAnotherOrganizationCannotReadServingHours() throws Exception {
    User manager = createUser("serving-hours-api-cross-manager@example.com");
    User outsider = createUser("serving-hours-api-cross-outsider@example.com");
    Organization organizationA = createOrganization("Serving Hours A");
    Organization organizationB = createOrganization("Serving Hours B");
    Establishment establishment = createEstablishment(organizationA, "Hidden Bar");
    createMembership(organizationA, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organizationB, outsider, OrganizationRole.ORG_EMPLOYEE, true);

    String outsiderToken = issueAccessToken(outsider);

    mockMvc
        .perform(
            get("/api/v1/organizations/%s/establishments/%s/serving-hours"
                    .formatted(organizationA.getId(), establishment.getId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
        .andExpect(status().isForbidden());
  }

  private List<UpdateServingHoursDayRequest> openWeekRequest(
      LocalTime opensAt, LocalTime closesAt) {
    return Arrays.stream(DayOfWeek.values())
        .map(day -> new UpdateServingHoursDayRequest(day, false, opensAt, closesAt))
        .toList();
  }

  private User createUser(String email) {
    return userRepository.saveAndFlush(
        new User(email, "Test", "User", passwordEncoder.encode("password123"), true, Set.of()));
  }

  private Organization createOrganization(String name) {
    return organizationRepository.saveAndFlush(new Organization(name, OrganizationStatus.ACTIVE));
  }

  private Establishment createEstablishment(Organization organization, String name) {
    return establishmentRepository.saveAndFlush(
        new Establishment(organization, name, EstablishmentType.BAR, EstablishmentStatus.ACTIVE));
  }

  private void createMembership(
      Organization organization, User user, OrganizationRole role, boolean active) {
    organizationMembershipRepository.saveAndFlush(
        new OrganizationMembership(organization, user, role, active));
  }

  private String issueAccessToken(User user) {
    return jwtService.issueAccessToken(user).token();
  }
}
