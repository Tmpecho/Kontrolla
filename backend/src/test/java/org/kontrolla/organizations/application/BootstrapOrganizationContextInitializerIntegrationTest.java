package org.kontrolla.organizations.application;

import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.deviations.infrastructure.DeviationRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentServingHoursRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationRole;
import org.kontrolla.organizations.infrastructure.OrganizationMembershipRepository;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalTime;

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
		"app.security.bootstrap-establishments[0].name=Kontrolla Demo Restaurant",
		"app.security.bootstrap-establishments[0].type=RESTAURANT",
		"app.security.bootstrap-establishments[1].name=Kontrolla Demo Bar",
		"app.security.bootstrap-establishments[1].type=BAR"
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

	@Autowired
	private ChecklistDefinitionRepository checklistDefinitionRepository;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private DeviationRepository deviationRepository;

	@Autowired
	private EstablishmentServingHoursRepository establishmentServingHoursRepository;

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
		assertThat(userRepository.findAll()).hasSizeGreaterThanOrEqualTo(40);
		assertThat(organizationRepository.findAll()).hasSizeGreaterThanOrEqualTo(4);
		assertThat(establishmentRepository.findAll()).hasSizeGreaterThanOrEqualTo(12);
		assertThat(organizationMembershipRepository.findAll()).hasSizeGreaterThanOrEqualTo(55);
		assertThat(deviationRepository.count()).isGreaterThanOrEqualTo(40);

		Organization configuredOrganization = organizationRepository.findByNameIgnoreCase("Kontrolla Dev Org").orElseThrow();
		var restaurant = establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(
				configuredOrganization.getId(),
				"Kontrolla Demo Restaurant"
		).orElseThrow();
		var bar = establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(
				configuredOrganization.getId(),
				"Kontrolla Demo Bar"
		).orElseThrow();

		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(configuredOrganization.getId(), admin.getId()))
				.get()
				.satisfies(membership -> {
					assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_ADMIN);
					assertThat(membership.isAccessAllEstablishments()).isTrue();
				});
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(configuredOrganization.getId(), manager.getId()))
				.get()
				.satisfies(membership -> {
					assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_MANAGER);
					assertThat(membership.isAccessAllEstablishments()).isTrue();
				});
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(configuredOrganization.getId(), employeeOne.getId()))
				.get()
				.satisfies(membership -> {
					assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_EMPLOYEE);
					assertThat(membership.isAccessAllEstablishments()).isFalse();
					assertThat(membership.getAccessibleEstablishments())
							.extracting(establishment -> establishment.getName())
							.containsExactly("Kontrolla Demo Restaurant");
				});
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(configuredOrganization.getId(), employeeTwo.getId()))
				.get()
				.satisfies(membership -> {
					assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_EMPLOYEE);
					assertThat(membership.isAccessAllEstablishments()).isFalse();
					assertThat(membership.getAccessibleEstablishments())
							.extracting(establishment -> establishment.getName())
							.containsExactly("Kontrolla Demo Bar");
				});
		assertThat(organizationMembershipRepository.findByOrganizationId(
				configuredOrganization.getId(),
				org.springframework.data.domain.PageRequest.of(0, 50)
		).getTotalElements()).isGreaterThanOrEqualTo(12);
		assertThat(organizationMembershipRepository.findByOrganizationIdAndAccessibleEstablishmentId(
				configuredOrganization.getId(),
				restaurant.getId(),
				org.springframework.data.domain.PageRequest.of(0, 50)
		).getTotalElements()).isGreaterThanOrEqualTo(8);
		assertThat(organizationMembershipRepository.findByOrganizationIdAndAccessibleEstablishmentId(
				configuredOrganization.getId(),
				bar.getId(),
				org.springframework.data.domain.PageRequest.of(0, 50)
		).getTotalElements()).isGreaterThanOrEqualTo(8);

		assertThat(checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				restaurant.getId(),
				ChecklistDefinitionStatus.ACTIVE
		)).anySatisfy(definition -> {
			assertThat(definition.getServiceArea()).isEqualTo(ChecklistServiceArea.IK_MAT);
			assertThat(definition.getTasks()).hasSizeGreaterThanOrEqualTo(3);
			assertThat(definition.getSchedules()).isNotEmpty();
		});
		assertThat(checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				bar.getId(),
				ChecklistDefinitionStatus.ACTIVE
		)).extracting(definition -> definition.getServiceArea())
				.contains(ChecklistServiceArea.IK_MAT, ChecklistServiceArea.IK_ALKOHOL);
		assertThat(checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				bar.getId(),
				ChecklistDefinitionStatus.ACTIVE
		)).hasSizeGreaterThanOrEqualTo(6);

		assertThat(checklistRunRepository.search(
				restaurant.getId(),
				ChecklistServiceArea.IK_MAT,
				java.util.EnumSet.allOf(ChecklistRunStatus.class),
				null,
				null,
				null,
				org.springframework.data.domain.PageRequest.of(0, 20)
		).getTotalElements()).isGreaterThanOrEqualTo(30);
		assertThat(checklistRunRepository.search(
				bar.getId(),
				ChecklistServiceArea.IK_ALKOHOL,
				java.util.EnumSet.allOf(ChecklistRunStatus.class),
				null,
				null,
				null,
				org.springframework.data.domain.PageRequest.of(0, 20)
		).getTotalElements()).isGreaterThanOrEqualTo(18);

		assertThat(deviationRepository.findByEstablishmentIdAndOrganizationId(
				restaurant.getId(),
				configuredOrganization.getId(),
				org.springframework.data.domain.PageRequest.of(0, 20)
		).getTotalElements()).isGreaterThanOrEqualTo(5);
		assertThat(deviationRepository.findByEstablishmentIdAndOrganizationId(
				bar.getId(),
				configuredOrganization.getId(),
				org.springframework.data.domain.PageRequest.of(0, 20)
		).getTotalElements()).isGreaterThanOrEqualTo(5);
		assertThat(establishmentServingHoursRepository.findByEstablishmentIdOrderByDayOfWeekAsc(restaurant.getId()))
				.hasSize(7)
				.anySatisfy(hours -> {
					assertThat(hours.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
					assertThat(hours.isClosed()).isFalse();
					assertThat(hours.getOpensAt()).isEqualTo(LocalTime.of(11, 0));
					assertThat(hours.getClosesAt()).isEqualTo(LocalTime.of(22, 0));
				});
		assertThat(establishmentServingHoursRepository.findByEstablishmentIdOrderByDayOfWeekAsc(bar.getId()))
				.hasSize(7)
				.anySatisfy(hours -> {
					assertThat(hours.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
					assertThat(hours.isClosed()).isFalse();
					assertThat(hours.getOpensAt()).isEqualTo(LocalTime.of(16, 0));
					assertThat(hours.getClosesAt()).isEqualTo(LocalTime.of(2, 0));
				});

		Organization curatedOrganization = organizationRepository.findByNameIgnoreCase("Nordic Table Group").orElseThrow();
		var curatedBar = establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(
				curatedOrganization.getId(),
				"Vulkan Tasting Room"
		).orElseThrow();
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(curatedOrganization.getId(), manager.getId()))
				.get()
				.satisfies(membership -> {
					assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_OWNER);
					assertThat(membership.isAccessAllEstablishments()).isTrue();
				});
		assertThat(organizationMembershipRepository.findByOrganizationIdAndUserId(curatedOrganization.getId(), employeeOne.getId()))
				.get()
				.satisfies(membership -> {
					assertThat(membership.getRole()).isEqualTo(OrganizationRole.ORG_EMPLOYEE);
					assertThat(membership.isAccessAllEstablishments()).isFalse();
					assertThat(membership.getAccessibleEstablishments()).hasSize(2);
				});
		assertThat(organizationMembershipRepository.findByOrganizationId(
				curatedOrganization.getId(),
				org.springframework.data.domain.PageRequest.of(0, 50)
		).getTotalElements()).isGreaterThanOrEqualTo(18);
		assertThat(organizationMembershipRepository.findByOrganizationIdAndAccessibleEstablishmentId(
				curatedOrganization.getId(),
				curatedBar.getId(),
				org.springframework.data.domain.PageRequest.of(0, 50)
		).getTotalElements()).isGreaterThanOrEqualTo(8);
		assertThat(checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				curatedBar.getId(),
				ChecklistDefinitionStatus.ACTIVE
		)).extracting(definition -> definition.getServiceArea())
				.contains(ChecklistServiceArea.IK_MAT, ChecklistServiceArea.IK_ALKOHOL);

		Organization secondCuratedOrganization = organizationRepository.findByNameIgnoreCase("Fjord Service Collective").orElseThrow();
		var inactiveEstablishment = establishmentRepository.findFirstByOrganizationIdAndNameIgnoreCase(
				secondCuratedOrganization.getId(),
				"Festivalpaviljongen"
		).orElseThrow();
		assertThat(checklistDefinitionRepository.findByEstablishmentIdAndStatus(
				inactiveEstablishment.getId(),
				ChecklistDefinitionStatus.ACTIVE
		)).isEmpty();
		assertThat(deviationRepository.findByEstablishmentIdAndOrganizationId(
				inactiveEstablishment.getId(),
				secondCuratedOrganization.getId(),
				org.springframework.data.domain.PageRequest.of(0, 20)
		).getTotalElements()).isZero();
	}
}
