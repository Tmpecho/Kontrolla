package org.kontrolla.organizations.application;

import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationMembership;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Profile("dev")
@Order(20)
public class BootstrapOrganizationContextInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapOrganizationContextInitializer.class);

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final EstablishmentRepository establishmentRepository;
	private final AppSecurityProperties properties;

	public BootstrapOrganizationContextInitializer(
			UserRepository userRepository,
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository organizationMembershipRepository,
			EstablishmentRepository establishmentRepository,
			AppSecurityProperties properties
	) {
		this.userRepository = userRepository;
		this.organizationRepository = organizationRepository;
		this.organizationMembershipRepository = organizationMembershipRepository;
		this.establishmentRepository = establishmentRepository;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String bootstrapUserEmail = Optional.ofNullable(properties.getBootstrapUser().getEmail()).orElse("").trim();
		String organizationName = Optional.ofNullable(properties.getBootstrapOrganization().getName()).orElse("").trim();
		String establishmentName = Optional.ofNullable(properties.getBootstrapEstablishment().getName()).orElse("").trim();

		if (bootstrapUserEmail.isBlank() || organizationName.isBlank() || establishmentName.isBlank()) {
			return;
		}

		User user = userRepository.findByEmailIgnoreCase(bootstrapUserEmail).orElse(null);
		if (user == null) {
			log.warn("Skipped bootstrap organization context because bootstrap user {} does not exist", bootstrapUserEmail);
			return;
		}

		Organization organization = organizationRepository.findByNameIgnoreCase(organizationName)
				.map(existing -> {
					existing.setStatus(properties.getBootstrapOrganization().getStatus());
					return existing;
				})
				.orElseGet(() -> {
					Organization created = new Organization(
							organizationName,
							properties.getBootstrapOrganization().getStatus()
					);
					organizationRepository.save(created);
					log.info("Created bootstrap organization {}", organizationName);
					return created;
				});

		organizationMembershipRepository.findByOrganizationIdAndUserId(organization.getId(), user.getId()).ifPresentOrElse(existing -> {
			existing.setRole(OrganizationRole.ORG_OWNER);
			existing.setActive(true);
		}, () -> {
			OrganizationMembership membership = new OrganizationMembership(
					organization,
					user,
					OrganizationRole.ORG_OWNER,
					true
			);
			organizationMembershipRepository.save(membership);
			log.info("Created bootstrap membership for {} in {}", bootstrapUserEmail, organizationName);
		});

		establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(organization.getId(), establishmentName)
				.map(existing -> {
					existing.setType(properties.getBootstrapEstablishment().getType());
					existing.setStatus(properties.getBootstrapEstablishment().getStatus());
					return existing;
				})
				.orElseGet(() -> {
					Establishment created = new Establishment(
							organization,
							establishmentName,
							properties.getBootstrapEstablishment().getType(),
							properties.getBootstrapEstablishment().getStatus()
					);
					establishmentRepository.save(created);
					log.info("Created bootstrap establishment {} in {}", establishmentName, organizationName);
					return created;
				});
	}
}
