package org.kontrolla.temperatures.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.kontrolla.temperatures.domain.TemperatureLog;
import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.domain.TemperatureUnitType;
import org.kontrolla.temperatures.infrastructure.TemperatureUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TemperatureControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private OrganizationMembershipRepository organizationMembershipRepository;

  @Autowired
  private EstablishmentRepository establishmentRepository;

  @Autowired
  private TemperatureUnitRepository temperatureUnitRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private TestDataCleaner testDataCleaner;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void memberCanListUnitsAndCreateTemperatureLog() throws Exception {
    User member = createUser("temperature-api-member@example.com", "Maria", "Nilsen");
    User previousLogger = createUser("temperature-api-logger@example.com", "Jonas", "Berg");
    Organization organization = createOrganization("Kontrolla Temperature API");
    Establishment establishment = createEstablishment(organization, "Kitchen");
    createMembership(organization, member, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organization, previousLogger, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit fridge = createTemperatureUnit(
        organization,
        establishment,
        "Sushi prep fridge",
        "Hot kitchen",
        TemperatureUnitType.FRIDGE,
        LocalTime.of(8, 30),
        new BigDecimal("2.00"),
        new BigDecimal("4.00")
    );
    fridge.addLog(new TemperatureLog(
        Instant.parse("2026-04-08T06:10:00Z"),
        new BigDecimal("3.40"),
        "Morning opening check completed.",
        previousLogger
    ));
    temperatureUnitRepository.saveAndFlush(fridge);

    String token = issueAccessToken(member);

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/temperature-units".formatted(
            organization.getId(), establishment.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Sushi prep fridge"))
        .andExpect(jsonPath("$[0].type").value("FRIDGE"))
        .andExpect(jsonPath("$[0].dueByTime").value("08:30:00"))
        .andExpect(jsonPath("$[0].logs.length()").value(1))
        .andExpect(jsonPath("$[0].logs[0].loggedByName").value("Jonas Berg"));

    mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/temperature-units/%s/logs".formatted(
            organization.getId(), establishment.getId(), fridge.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateTemperatureLogRequest(
                new BigDecimal("3.20"),
                Instant.parse("2026-04-09T06:10:00Z"),
                "  Opening check completed.  "
            ))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.temperatureCelsius").value(3.2))
        .andExpect(jsonPath("$.note").value("Opening check completed."))
        .andExpect(jsonPath("$.loggedByName").value("Maria Nilsen"));

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/temperature-units".formatted(
            organization.getId(), establishment.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].logs.length()").value(2))
        .andExpect(jsonPath("$[0].logs[0].temperatureCelsius").value(3.2))
        .andExpect(jsonPath("$[0].logs[0].loggedByName").value("Maria Nilsen"));
  }

  @Test
  void outOfRangeTemperatureWithoutNoteReturnsBadRequest() throws Exception {
    User member = createUser("temperature-api-note@example.com", "Sofie", "Hansen");
    Organization organization = createOrganization("Kontrolla Temperature API Notes");
    Establishment establishment = createEstablishment(organization, "Freezer Room");
    createMembership(organization, member, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit unit = createTemperatureUnit(
        organization,
        establishment,
        "Frozen storage A",
        "Basement freezer room",
        TemperatureUnitType.FREEZER,
        LocalTime.of(7, 45),
        new BigDecimal("-24.00"),
        new BigDecimal("-18.00")
    );
    temperatureUnitRepository.saveAndFlush(unit);

    String token = issueAccessToken(member);

    mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/temperature-units/%s/logs".formatted(
            organization.getId(), establishment.getId(), unit.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateTemperatureLogRequest(
                new BigDecimal("-17.40"),
                Instant.parse("2026-04-09T05:45:00Z"),
                "   "
            ))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("temperature_note_required"));
  }

  @Test
  void outsiderFromAnotherOrganizationCannotListOrCreateTemperatureLogs() throws Exception {
    User member = createUser("temperature-api-org-member@example.com", "Henrik", "Solberg");
    User outsider = createUser("temperature-api-org-outsider@example.com", "Lina", "Dahl");
    Organization organizationA = createOrganization("Org A Temperature API");
    Organization organizationB = createOrganization("Org B Temperature API");
    Establishment establishment = createEstablishment(organizationA, "Front Bar");
    createMembership(organizationA, member, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organizationB, outsider, OrganizationRole.ORG_EMPLOYEE, true);

    TemperatureUnit unit = createTemperatureUnit(
        organizationA,
        establishment,
        "Bar garnish fridge",
        "Front bar",
        TemperatureUnitType.FRIDGE,
        LocalTime.of(10, 15),
        new BigDecimal("2.00"),
        new BigDecimal("5.00")
    );
    temperatureUnitRepository.saveAndFlush(unit);

    String outsiderToken = issueAccessToken(outsider);

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/temperature-units".formatted(
            organizationA.getId(), establishment.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/temperature-units/%s/logs".formatted(
            organizationA.getId(), establishment.getId(), unit.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateTemperatureLogRequest(
                new BigDecimal("4.40"),
                Instant.parse("2026-04-09T08:00:00Z"),
                null
            ))))
        .andExpect(status().isForbidden());
  }

  private User createUser(String email, String firstName, String lastName) {
    User user = new User(
        email,
        firstName,
        lastName,
        passwordEncoder.encode("password123"),
        true,
        Set.of()
    );
    return userRepository.saveAndFlush(user);
  }

  private Organization createOrganization(String name) {
    return organizationRepository.saveAndFlush(new Organization(name, OrganizationStatus.ACTIVE));
  }

  private Establishment createEstablishment(Organization organization, String name) {
    return establishmentRepository.saveAndFlush(new Establishment(
        organization,
        name,
        EstablishmentType.RESTAURANT,
        EstablishmentStatus.ACTIVE
    ));
  }

  private void createMembership(Organization organization, User user, OrganizationRole role, boolean active) {
    organizationMembershipRepository.saveAndFlush(new OrganizationMembership(organization, user, role, active));
  }

  private TemperatureUnit createTemperatureUnit(
      Organization organization,
      Establishment establishment,
      String name,
      String location,
      TemperatureUnitType type,
      LocalTime dueByTime,
      BigDecimal minimumTemperature,
      BigDecimal maximumTemperature
  ) {
    return new TemperatureUnit(
        organization,
        establishment,
        name,
        location,
        type,
        dueByTime,
        minimumTemperature,
        maximumTemperature
    );
  }

  private String issueAccessToken(User user) {
    return jwtService.issueAccessToken(user).token();
  }
}
