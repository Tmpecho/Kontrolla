package org.kontrolla.organizations.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
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

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
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
						.with(csrf())
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
						.with(csrf())
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
						.with(csrf())
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

	@Test
	void tenantMemberCannotReadAnotherOrganizationsData() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAManager = createUser("orga@example.com", "Org", "AManager", Set.of());
		User orgBManager = createUser("orgb@example.com", "Org", "BManager", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String orgAId = createOrganization(adminToken, "Org A");
		String orgBId = createOrganization(adminToken, "Org B");

		addMembership(adminToken, orgAId, orgAManager.getId(), "ORG_MANAGER");
		addMembership(adminToken, orgBId, orgBManager.getId(), "ORG_MANAGER");

		String establishmentResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(orgBId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Hidden Bar",
								  "type": "BAR"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String establishmentId = objectMapper.readTree(establishmentResponse).get("id").asText();
		String orgAToken = login("orga@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s".formatted(orgBId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/members".formatted(orgBId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments".formatted(orgBId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments/%s".formatted(orgBId, establishmentId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void organizationMemberCanListOwnOrganizationsMembers() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgManager = createUser("manager@example.com", "Manager", "User", Set.of());
		User orgEmployee = createUser("employee@example.com", "Employee", "User", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Readable Org");
		addMembership(adminToken, organizationId, orgManager.getId(), "ORG_MANAGER");
		addMembership(adminToken, organizationId, orgEmployee.getId(), "ORG_EMPLOYEE");

		String employeeToken = login("employee@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s/members".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").isNotEmpty());
	}

	@Test
	void memberListingHidesInactiveMembershipsByDefaultButCanIncludeThem() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAdmin = createUser("orgadmin@example.com", "Org", "Admin", Set.of());
		User activeEmployee = createUser("active@example.com", "Active", "Member", Set.of());
		User inactiveEmployee = createUser("inactive@example.com", "Inactive", "Member", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Filtered Org");
		addMembership(adminToken, organizationId, orgAdmin.getId(), "ORG_ADMIN");
		addMembership(adminToken, organizationId, activeEmployee.getId(), "ORG_EMPLOYEE");
		String inactiveMembershipResponse = mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE",
								  "active": false
								}
								""".formatted(inactiveEmployee.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.active").value(false))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String inactiveMembershipId = objectMapper.readTree(inactiveMembershipResponse).get("id").asText();
		String orgAdminToken = login("orgadmin@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s/members".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(inactiveMembershipId))));

		mockMvc.perform(get("/api/v1/organizations/%s/members?includeInactive=true".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id").value(org.hamcrest.Matchers.hasItem(inactiveMembershipId)));
	}

	@Test
	void memberListingCanBeFilteredByEstablishmentScope() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAdmin = createUser("orgadmin@example.com", "Org", "Admin", Set.of());
		User restaurantEmployee = createUser("restaurant@example.com", "Restaurant", "Employee", Set.of());
		User barEmployee = createUser("bar@example.com", "Bar", "Employee", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Scoped Org");
		addMembership(adminToken, organizationId, orgAdmin.getId(), "ORG_ADMIN");

		String restaurantResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Restaurant",
								  "type": "RESTAURANT"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String barResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Bar",
								  "type": "BAR"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String restaurantId = objectMapper.readTree(restaurantResponse).get("id").asText();
		String barId = objectMapper.readTree(barResponse).get("id").asText();

		mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE",
								  "allEstablishments": false,
								  "establishmentIds": ["%s"]
								}
								""".formatted(restaurantEmployee.getId(), restaurantId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.allEstablishments").value(false))
				.andExpect(jsonPath("$.establishments[0].id").value(restaurantId));

		mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE",
								  "allEstablishments": false,
								  "establishmentIds": ["%s"]
								}
								""".formatted(barEmployee.getId(), barId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.allEstablishments").value(false))
				.andExpect(jsonPath("$.establishments[0].id").value(barId));

		String orgAdminToken = login("orgadmin@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s/members?establishmentId=%s".formatted(organizationId, restaurantId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].userEmail").value(org.hamcrest.Matchers.hasItem("orgadmin@example.com")))
				.andExpect(jsonPath("$.items[*].userEmail").value(org.hamcrest.Matchers.hasItem("restaurant@example.com")))
				.andExpect(jsonPath("$.items[*].userEmail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("bar@example.com"))));

		mockMvc.perform(get("/api/v1/organizations/%s/members?establishmentId=%s".formatted(organizationId, barId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].userEmail").value(org.hamcrest.Matchers.hasItem("orgadmin@example.com")))
				.andExpect(jsonPath("$.items[*].userEmail").value(org.hamcrest.Matchers.hasItem("bar@example.com")))
				.andExpect(jsonPath("$.items[*].userEmail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("restaurant@example.com"))));
	}

	@Test
	void memberListingRejectsForeignEstablishmentId() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAdmin = createUser("orgadmin@example.com", "Org", "Admin", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String orgAId = createOrganization(adminToken, "Org A");
		String orgBId = createOrganization(adminToken, "Org B");
		addMembership(adminToken, orgAId, orgAdmin.getId(), "ORG_ADMIN");

		String foreignEstablishmentResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(orgBId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Foreign Bar",
								  "type": "BAR"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String foreignEstablishmentId = objectMapper.readTree(foreignEstablishmentResponse).get("id").asText();
		String orgAdminToken = login("orgadmin@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s/members?establishmentId=%s".formatted(orgAId, foreignEstablishmentId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void membershipUpdatePreservesScopedEstablishmentAccessWhenScopeIsOmitted() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAdmin = createUser("orgadmin@example.com", "Org", "Admin", Set.of());
		User scopedEmployee = createUser("scoped@example.com", "Scoped", "Employee", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Scoped Update Org");
		addMembership(adminToken, organizationId, orgAdmin.getId(), "ORG_ADMIN");

		String restaurantResponse = mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Restaurant",
								  "type": "RESTAURANT"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Bar",
								  "type": "BAR"
								}
								"""))
				.andExpect(status().isCreated());

		String restaurantId = objectMapper.readTree(restaurantResponse).get("id").asText();
		String membershipResponse = mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE",
								  "allEstablishments": false,
								  "establishmentIds": ["%s"]
								}
								""".formatted(scopedEmployee.getId(), restaurantId)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String membershipId = objectMapper.readTree(membershipResponse).get("id").asText();
		String orgAdminToken = login("orgadmin@example.com", "password123");

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(organizationId, membershipId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "ORG_EMPLOYEE",
								  "active": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.allEstablishments").value(false))
				.andExpect(jsonPath("$.establishments.length()").value(1))
				.andExpect(jsonPath("$.establishments[0].id").value(restaurantId));
	}

	@Test
	void tenantMemberCannotModifyAnotherOrganizationsData() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAManager = createUser("orga@example.com", "Org", "AManager", Set.of());
		User orgBUser = createUser("orgb@example.com", "Org", "BUser", Set.of());
		User outsider = createUser("outsider@example.com", "Out", "Sider", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String orgAId = createOrganization(adminToken, "Org A");
		String orgBId = createOrganization(adminToken, "Org B");

		addMembership(adminToken, orgAId, orgAManager.getId(), "ORG_MANAGER");

		String orgBMembershipResponse = addMembership(adminToken, orgBId, orgBUser.getId(), "ORG_EMPLOYEE");
		String orgBMembershipId = objectMapper.readTree(orgBMembershipResponse).get("id").asText();

		String orgAToken = login("orga@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(orgBId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Should Not Exist",
								  "type": "BAR"
								}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(orgBId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE"
								}
								""".formatted(outsider.getId())))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(orgBId, orgBMembershipId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "ORG_ADMIN",
								  "active": true
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void userWithoutMembershipCannotAccessOrganizationData() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		createUser("nomember@example.com", "No", "Member", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Protected Org");

		String noMemberToken = login("nomember@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + noMemberToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + noMemberToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void orgAdminCanInviteNewMemberWithoutExistingUserId() throws Exception {
		createUser("admin@example.com", "Admin", "User", Set.of(GlobalRole.PLATFORM_ADMIN));
		User orgAdmin = createUser("orgadmin@example.com", "Org", "Admin", Set.of());

		String adminToken = login("admin@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Managed Org");
		addMembership(adminToken, organizationId, orgAdmin.getId(), "ORG_ADMIN");

		String orgAdminToken = login("orgadmin@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/members/managed-users".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + orgAdminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "new.member@example.com",
								  "firstName": "New",
								  "lastName": "Member",
								  "role": "ORG_EMPLOYEE",
								  "active": true
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.membership.userEmail").value("new.member@example.com"))
				.andExpect(jsonPath("$.membership.userFirstName").value("New"))
				.andExpect(jsonPath("$.membership.userLastName").value("Member"))
				.andExpect(jsonPath("$.membership.role").value("ORG_EMPLOYEE"))
				.andExpect(jsonPath("$.membership.active").value(true))
				.andExpect(jsonPath("$.inviteUrl").isString())
				.andExpect(jsonPath("$.inviteExpiresAt").isNotEmpty());
	}

	@Test
	void nonPlatformAdminCannotAccessAdminOrganizationsOrUsersEndpoints() throws Exception {
		createUser("admin-access@example.com", "Admin", "Access", Set.of(GlobalRole.PLATFORM_ADMIN));
		User manager = createUser("manager-access@example.com", "Manager", "Access", Set.of());

		String adminToken = login("admin-access@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Manager Access Org");
		addMembership(adminToken, organizationId, manager.getId(), "ORG_MANAGER");

		String managerToken = login("manager-access@example.com", "password123");

		mockMvc.perform(get("/api/v1/admin/organizations")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/admin/organizations")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Should Not Be Created"
								}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/admin/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/admin/users")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "blocked-user@example.com",
								  "firstName": "Blocked",
								  "lastName": "User",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void organizationEmployeeCannotCreateEstablishmentInOwnOrganization() throws Exception {
		createUser("admin-establishments@example.com", "Admin", "Establishments", Set.of(GlobalRole.PLATFORM_ADMIN));
		User employee = createUser("employee-establishments@example.com", "Employee", "Establishments", Set.of());

		String adminToken = login("admin-establishments@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Establishment Guard Org");
		addMembership(adminToken, organizationId, employee.getId(), "ORG_EMPLOYEE");

		String employeeToken = login("employee-establishments@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/establishments".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Should Not Exist",
								  "type": "RESTAURANT"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void organizationManagerCannotManageMembersInOwnOrganization() throws Exception {
		createUser("admin-members@example.com", "Admin", "Members", Set.of(GlobalRole.PLATFORM_ADMIN));
		User manager = createUser("manager-members@example.com", "Manager", "Members", Set.of());
		User employee = createUser("employee-members@example.com", "Employee", "Members", Set.of());

		String adminToken = login("admin-members@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Member Guard Org");
		String membershipResponse = addMembership(adminToken, organizationId, manager.getId(), "ORG_MANAGER");
		String managerMembershipId = objectMapper.readTree(membershipResponse).get("id").asText();

		String managerToken = login("manager-members@example.com", "password123");

		mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "ORG_EMPLOYEE"
								}
								""".formatted(employee.getId())))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/organizations/%s/members/%s".formatted(organizationId, managerMembershipId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "ORG_ADMIN",
								  "active": true
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void membershipListCapsRequestedPageSize() throws Exception {
		User admin = createUser("admin-cap@example.com", "Admin", "Cap", Set.of(GlobalRole.PLATFORM_ADMIN));
		User manager = createUser("manager-cap@example.com", "Manager", "Cap", Set.of());
		User employee = createUser("employee-cap@example.com", "Employee", "Cap", Set.of());

		String adminToken = login("admin-cap@example.com", "password123");
		String organizationId = createOrganization(adminToken, "Capped Org");

		addMembership(adminToken, organizationId, manager.getId(), "ORG_MANAGER");
		addMembership(adminToken, organizationId, employee.getId(), "ORG_EMPLOYEE");

		String managerToken = login("manager-cap@example.com", "password123");

		mockMvc.perform(get("/api/v1/organizations/%s/members?size=500".formatted(organizationId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(100));
	}

	private User createUser(String email, String firstName, String lastName, Set<GlobalRole> roles) {
		User user = new User(
				email,
				firstName,
				lastName,
				passwordEncoder.encode("password123"),
				true,
				roles
		);
		return userRepository.saveAndFlush(user);
	}

	private String createOrganization(String adminToken, String name) throws Exception {
		String response = mockMvc.perform(post("/api/v1/admin/organizations")
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s"
								}
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(response).get("id").asText();
	}

	private String addMembership(String adminToken, String organizationId, Object userId, String role) throws Exception {
		return mockMvc.perform(post("/api/v1/organizations/%s/members".formatted(organizationId))
						.with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": "%s",
								  "role": "%s"
								}
								""".formatted(userId, role)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private String login(String email, String password) throws Exception {
		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
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

		JsonNode json = objectMapper.readTree(loginResponse);
		return json.get("accessToken").asText();
	}
}
