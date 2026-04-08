package org.kontrolla.organizations.application;

import org.junit.jupiter.api.Test;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.security.bootstrap-admin.email=admin@example.com",
		"app.security.bootstrap-admin.password=password123",
		"app.security.bootstrap-admin.first-name=Admin",
		"app.security.bootstrap-admin.last-name=User",
		"app.security.bootstrap-user.email=demo@example.com",
		"app.security.bootstrap-user.password=password123",
		"app.security.bootstrap-user.first-name=Demo",
		"app.security.bootstrap-user.last-name=User",
		"app.security.bootstrap-employees[0].email=emma.larsen@example.com",
		"app.security.bootstrap-employees[0].password=password123",
		"app.security.bootstrap-employees[0].first-name=Emma",
		"app.security.bootstrap-employees[0].last-name=Larsen",
		"app.security.bootstrap-employees[1].email=noah.berg@example.com",
		"app.security.bootstrap-employees[1].password=password123",
		"app.security.bootstrap-employees[1].first-name=Noah",
		"app.security.bootstrap-employees[1].last-name=Berg",
		"app.security.bootstrap-organization.name=Kontrolla Dev Org",
		"app.security.bootstrap-establishment.name=Kontrolla Demo Restaurant",
		"app.security.bootstrap-establishment.type=RESTAURANT"
})
@ActiveProfiles({"dev", "test"})
class BootstrapOrganizationContextInitializerIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository organizationMembershipRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Test
	void bootstrapDevelopmentTeamIsCreatedWithExpectedRoles() {
		User admin = userRepository.findByEmailIgnoreCase("admin@example.com").orElseThrow();
		User manager = userRepository.findByEmailIgnoreCase("demo@example.com").orElseThrow();
		User employeeOne = userRepository.findByEmailIgnoreCase("emma.larsen@example.com").orElseThrow();
		User employeeTwo = userRepository.findByEmailIgnoreCase("noah.berg@example.com").orElseThrow();

		assertThat(admin.getGlobalRoles()).containsExactly(GlobalRole.PLATFORM_ADMIN);
		assertThat(manager.getGlobalRoles()).isEmpty();
		assertThat(employeeOne.getGlobalRoles()).isEmpty();
		assertThat(employeeTwo.getGlobalRoles()).isEmpty();

		var organization = organizationRepository.findByNameIgnoreCase("Kontrolla Dev Org").orElseThrow();
		assertThat(establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(
				organization.getId(),
				"Kontrolla Demo Restaurant"
		)).isPresent();

		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(organization.getId(), admin.getId()))
				.get()
				.extracting(membership -> membership.getRole())
				.isEqualTo(OrganizationRole.ORG_ADMIN);
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(organization.getId(), manager.getId()))
				.get()
				.extracting(membership -> membership.getRole())
				.isEqualTo(OrganizationRole.ORG_MANAGER);
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(organization.getId(), employeeOne.getId()))
				.get()
				.extracting(membership -> membership.getRole())
				.isEqualTo(OrganizationRole.ORG_EMPLOYEE);
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(organization.getId(), employeeTwo.getId()))
				.get()
				.extracting(membership -> membership.getRole())
				.isEqualTo(OrganizationRole.ORG_EMPLOYEE);
	}
}
