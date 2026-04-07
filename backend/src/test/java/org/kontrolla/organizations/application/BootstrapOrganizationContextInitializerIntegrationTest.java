package org.kontrolla.organizations.application;

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
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.security.bootstrap-user.email=demo.user@example.com",
		"app.security.bootstrap-organization.name=Kontrolla Dev Org",
		"app.security.bootstrap-organization.status=ACTIVE",
		"app.security.bootstrap-establishment.name=Kontrolla Demo Bar",
		"app.security.bootstrap-establishment.type=BAR",
		"app.security.bootstrap-establishment.status=ACTIVE"
})
@ActiveProfiles({"dev", "test"})
class BootstrapOrganizationContextInitializerIntegrationTest {

	@Autowired
	private BootstrapOrganizationContextInitializer bootstrapOrganizationContextInitializer;

	@Autowired
	private UserRepository userRepository;

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
	void bootstrapOrganizationContextIsCreatedWhenMissing() {
		User user = createUser("demo.user@example.com");

		bootstrapOrganizationContextInitializer.run(new DefaultApplicationArguments());

		Organization organization = organizationRepository.findByNameIgnoreCase("Kontrolla Dev Org")
				.orElseThrow();
		OrganizationMembership membership = organizationMembershipRepository
				.findByOrganizationIdAndUserId(organization.getId(), user.getId())
				.orElseThrow();
		Establishment establishment = establishmentRepository
				.findFirstByOrganizationIdAndNameIgnoreCase(organization.getId(), "Kontrolla Demo Bar")
				.orElseThrow();

		assertThat(organization.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_OWNER);
		assertThat(membership.isActive()).isTrue();
		assertThat(establishment.getType()).isEqualTo(EstablishmentType.BAR);
		assertThat(establishment.getStatus()).isEqualTo(EstablishmentStatus.ACTIVE);
	}

	@Test
	void bootstrapOrganizationContextUpdatesExistingRecords() {
		User user = createUser("demo.user@example.com");
		Organization organization = organizationRepository.saveAndFlush(new Organization(
				"Kontrolla Dev Org",
				OrganizationStatus.INACTIVE
		));
		organizationMembershipRepository.saveAndFlush(new OrganizationMembership(
				organization,
				user,
				OrganizationRole.ORG_EMPLOYEE,
				false
		));
		establishmentRepository.saveAndFlush(new Establishment(
				organization,
				"Kontrolla Demo Bar",
				EstablishmentType.CAFE,
				EstablishmentStatus.INACTIVE
		));

		bootstrapOrganizationContextInitializer.run(new DefaultApplicationArguments());

		Organization updatedOrganization = organizationRepository.findById(organization.getId())
				.orElseThrow();
		OrganizationMembership updatedMembership = organizationMembershipRepository
				.findByOrganizationIdAndUserId(organization.getId(), user.getId())
				.orElseThrow();
		Establishment updatedEstablishment = establishmentRepository
				.findFirstByOrganizationIdAndNameIgnoreCase(organization.getId(), "Kontrolla Demo Bar")
				.orElseThrow();

		assertThat(updatedOrganization.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(updatedMembership.getRole()).isEqualTo(OrganizationRole.ORG_OWNER);
		assertThat(updatedMembership.isActive()).isTrue();
		assertThat(updatedEstablishment.getType()).isEqualTo(EstablishmentType.BAR);
		assertThat(updatedEstablishment.getStatus()).isEqualTo(EstablishmentStatus.ACTIVE);
	}

	@Test
	void bootstrapOrganizationContextSkipsCreationWhenBootstrapUserIsMissing() {
		bootstrapOrganizationContextInitializer.run(new DefaultApplicationArguments());

		assertThat(organizationRepository.findByNameIgnoreCase("Kontrolla Dev Org")).isEmpty();
	}

	private User createUser(String email) {
		User user = new User(email, "Demo", "User", "hashed-password", true, Set.of());
		return userRepository.saveAndFlush(user);
	}
}
