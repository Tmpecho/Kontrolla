package org.kontrolla.documents.api;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

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
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private TestDataCleaner testDataCleaner;

  @Autowired
  private Clock clock;

  @BeforeEach
  void setUp() {
    testDataCleaner.clearAll();
  }

  @Test
  void managerCanCreateUpdateReplaceDownloadDeleteAndListDocuments() throws Exception {
    User manager = createUser("documents-api-manager@example.com", "Manager", "API");
    Organization organization = createOrganization("Kontrolla Documents API");
    Establishment establishment = createEstablishment(organization, "Downtown Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    LocalDate today = LocalDate.now(clock);

    String token = issueAccessToken(manager);

    String createResponse = mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organization.getId(), establishment.getId()))
            .file(metadataPart("IK_ALKOHOL", "  Alcohol service licence  ", "  Oslo Municipality  ", today.minusDays(365), today.plusDays(10)))
            .file(pdfPart("alcohol-service-licence.pdf", "license-v1"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Alcohol service licence"))
        .andExpect(jsonPath("$.holderName").value("Oslo Municipality"))
        .andExpect(jsonPath("$.serviceArea").value("IK_ALKOHOL"))
        .andExpect(jsonPath("$.contentType").value("application/pdf"))
        .andExpect(jsonPath("$.fileName").value("alcohol-service-licence.pdf"))
        .andExpect(jsonPath("$.status").value("EXPIRING"))
        .andReturn()
        .getResponse()
        .getContentAsString();

    String documentId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organization.getId(), establishment.getId()))
            .file(metadataPart("IK_ALKOHOL", "Responsible service certificate", "Lina Dahl", today.minusDays(200), today.plusDays(3)))
            .file(pdfPart("responsible-service-certificate.pdf", "certificate-v1"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organization.getId(), establishment.getId()))
            .file(metadataPart("IK_MAT", "Cleaning routine", "Kitchen operations", today.minusDays(120), today.plusDays(90)))
            .file(pdfPart("cleaning-routine.pdf", "cleaning-v1"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc.perform(put("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "serviceArea": "IK_ALKOHOL",
                  "title": "Alcohol service licence 2026",
                  "holderName": "Oslo Municipality Licensing",
                  "issueDate": "%s",
                  "renewalDate": "%s"
                }
                """.formatted(today.minusDays(365), today.plusDays(60))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Alcohol service licence 2026"))
        .andExpect(jsonPath("$.status").value("VALID"));

    mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents/%s/file".formatted(
            organization.getId(), establishment.getId(), documentId))
            .file(pdfPart("alcohol-service-licence-2026.pdf", "license-v2"))
            .with(request -> {
              request.setMethod("PUT");
              return request;
            })
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("alcohol-service-licence-2026.pdf"))
        .andExpect(jsonPath("$.contentType").value("application/pdf"));

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(documentId))
        .andExpect(jsonPath("$.holderName").value("Oslo Municipality Licensing"))
        .andExpect(jsonPath("$.status").value("VALID"));

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s/file".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("alcohol-service-licence-2026.pdf")))
        .andExpect(content().bytes(pdfBytes("license-v2")));

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents?serviceArea=IK_ALKOHOL".formatted(
            organization.getId(), establishment.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].title").value("Responsible service certificate"))
        .andExpect(jsonPath("$.items[0].status").value("EXPIRING"))
        .andExpect(jsonPath("$.items[1].title").value("Alcohol service licence 2026"));

    mockMvc.perform(delete("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s/file".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents?serviceArea=IK_ALKOHOL".formatted(
            organization.getId(), establishment.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Responsible service certificate"));
  }

  @Test
  void invalidDateRangeReturnsBadRequest() throws Exception {
    User manager = createUser("documents-api-dates@example.com", "Manager", "Dates");
    Organization organization = createOrganization("Kontrolla API Dates");
    Establishment establishment = createEstablishment(organization, "Date Checks");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    LocalDate today = LocalDate.now(clock);

    String token = issueAccessToken(manager);

    mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organization.getId(), establishment.getId()))
            .file(metadataPart("IK_ALKOHOL", "Staff permit register", "People operations", today, today.minusDays(1)))
            .file(pdfPart("staff-register.pdf", "staff-register"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("invalid_document_dates"));
  }

  @Test
  void outsiderFromAnotherOrganizationCannotReadDocuments() throws Exception {
    User manager = createUser("documents-api-cross-manager@example.com", "Manager", "Cross");
    User outsider = createUser("documents-api-cross-outsider@example.com", "Outsider", "Cross");
    Organization organizationA = createOrganization("Org A Documents");
    Organization organizationB = createOrganization("Org B Documents");
    Establishment establishment = createEstablishment(organizationA, "Inspection Bar");
    createMembership(organizationA, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organizationB, outsider, OrganizationRole.ORG_MANAGER, true);
    LocalDate today = LocalDate.now(clock);

    String managerToken = issueAccessToken(manager);
    String outsiderToken = issueAccessToken(outsider);

    String createResponse = mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organizationA.getId(), establishment.getId()))
            .file(metadataPart("IK_ALKOHOL", "Incident reporting routine sign-off", "Shift supervisors", today.minusDays(180), today.plusDays(30)))
            .file(pdfPart("incident-report.pdf", "incident-report"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String documentId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s".formatted(
            organizationA.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents/%s/file".formatted(
            organizationA.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s/documents?serviceArea=IK_ALKOHOL".formatted(
            organizationA.getId(), establishment.getId()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void managerCanAssignMultipleAuditReadersAndAssignedReaderCanAcknowledge() throws Exception {
    User manager = createUser("documents-api-audit-manager@example.com", "Manager", "Audit");
    User firstReader = createUser("documents-api-audit-reader-1@example.com", "Nora", "Hansen");
    User secondReader = createUser("documents-api-audit-reader-2@example.com", "Lina", "Dahl");
    Organization organization = createOrganization("Kontrolla Documents Audit API");
    Establishment establishment = createEstablishment(organization, "Downtown Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organization, firstReader, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organization, secondReader, OrganizationRole.ORG_EMPLOYEE, true);
    LocalDate today = LocalDate.now(clock);

    String managerToken = issueAccessToken(manager);
    String firstReaderToken = issueAccessToken(firstReader);

    String createResponse = mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organization.getId(), establishment.getId()))
            .file(metadataPart(
                "IK_ALKOHOL",
                "Responsible service handbook",
                "Bar team",
                today.minusDays(60),
                today.plusDays(30),
                java.util.List.of(firstReader.getId(), secondReader.getId())
            ))
            .file(pdfPart("responsible-service.pdf", "audit-handbook"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.auditAssignments.length()").value(2))
        .andExpect(jsonPath("$.auditAssignments[0].userId").isNotEmpty())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String documentId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents/%s/acknowledge-read".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstReaderToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.auditAssignments.length()").value(2))
        .andExpect(jsonPath("$.auditAssignments[0].acknowledgedAt").isNotEmpty());
  }

  @Test
  void unassignedReaderCannotAcknowledgeDocumentAudit() throws Exception {
    User manager = createUser("documents-api-audit-manager-2@example.com", "Manager", "Audit");
    User assignedReader = createUser("documents-api-audit-reader-3@example.com", "Nora", "Hansen");
    User otherReader = createUser("documents-api-audit-reader-4@example.com", "Lina", "Dahl");
    Organization organization = createOrganization("Kontrolla Documents Audit API Access");
    Establishment establishment = createEstablishment(organization, "Downtown Bar");
    createMembership(organization, manager, OrganizationRole.ORG_MANAGER, true);
    createMembership(organization, assignedReader, OrganizationRole.ORG_EMPLOYEE, true);
    createMembership(organization, otherReader, OrganizationRole.ORG_EMPLOYEE, true);
    LocalDate today = LocalDate.now(clock);

    String managerToken = issueAccessToken(manager);
    String otherReaderToken = issueAccessToken(otherReader);

    String createResponse = mockMvc.perform(multipart("/api/v1/organizations/%s/establishments/%s/documents".formatted(
            organization.getId(), establishment.getId()))
            .file(metadataPart(
                "IK_ALKOHOL",
                "Responsible service handbook",
                "Bar team",
                today.minusDays(60),
                today.plusDays(30),
                java.util.List.of(assignedReader.getId())
            ))
            .file(pdfPart("responsible-service.pdf", "audit-handbook"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String documentId = objectMapper.readTree(createResponse).get("id").asText();

    mockMvc.perform(post("/api/v1/organizations/%s/establishments/%s/documents/%s/acknowledge-read".formatted(
            organization.getId(), establishment.getId(), documentId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherReaderToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("document_audit_acknowledgement_forbidden"));
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
    Organization organization = new Organization(name, OrganizationStatus.ACTIVE);
    return organizationRepository.saveAndFlush(organization);
  }

  private Establishment createEstablishment(Organization organization, String name) {
    Establishment establishment = new Establishment(
        organization,
        name,
        EstablishmentType.BAR,
        EstablishmentStatus.ACTIVE
    );
    return establishmentRepository.saveAndFlush(establishment);
  }

  private void createMembership(Organization organization, User user, OrganizationRole role, boolean active) {
    OrganizationMembership membership = new OrganizationMembership(organization, user, role, active);
    organizationMembershipRepository.saveAndFlush(membership);
  }

  private String issueAccessToken(User user) {
    return jwtService.issueAccessToken(user).token();
  }

  private MockMultipartFile metadataPart(
      String serviceArea,
      String title,
      String holderName,
      LocalDate issueDate,
      LocalDate renewalDate
  ) {
    return metadataPart(serviceArea, title, holderName, issueDate, renewalDate, null);
  }

  private MockMultipartFile metadataPart(
      String serviceArea,
      String title,
      String holderName,
      LocalDate issueDate,
      LocalDate renewalDate,
      java.util.List<java.util.UUID> auditUserIds
  ) {
    String auditUsersJson = auditUserIds == null
        ? ""
        : """
          ,
          "auditUserIds": [%s]
          """.formatted(auditUserIds.stream().map(id -> "\"%s\"".formatted(id)).collect(java.util.stream.Collectors.joining(",")));

    String json = """
        {
          "serviceArea": "%s",
          "title": "%s",
          "holderName": "%s",
          "issueDate": "%s",
          "renewalDate": "%s"
          %s
        }
        """.formatted(serviceArea, title, holderName, issueDate, renewalDate, auditUsersJson);
    return new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
  }

  private MockMultipartFile pdfPart(String fileName, String marker) {
    return new MockMultipartFile("file", fileName, "application/pdf", pdfBytes(marker));
  }

  private byte[] pdfBytes(String marker) {
    return ("%PDF-1.7\n" + marker + "\n%%EOF").getBytes(StandardCharsets.UTF_8);
  }
}
