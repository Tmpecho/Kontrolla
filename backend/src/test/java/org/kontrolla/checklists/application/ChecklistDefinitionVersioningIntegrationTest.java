package org.kontrolla.checklists.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.api.ChecklistRunResponse;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskDefinition;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.infrastructure.ChecklistDefinitionRepository;
import org.kontrolla.checklists.infrastructure.ChecklistRunRepository;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.kontrolla.iam.domain.GlobalRole;
import org.kontrolla.iam.domain.User;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationStatus;
import org.kontrolla.organizations.infrastructure.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ChecklistDefinitionVersioningIntegrationTest {

	@Autowired
	private ChecklistDefinitionService checklistDefinitionService;

	@Autowired
	private ChecklistDefinitionRepository checklistDefinitionRepository;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		checklistRunRepository.deleteAll();
		checklistDefinitionRepository.deleteAll();
		establishmentRepository.deleteAll();
		organizationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void updateChecklistDefinitionCreatesNewVersionAndSupersedesOldVersion() {
		User actor = createPlatformAdmin("admin@example.com");
		Organization organization = createOrganization("Kontrolla");
		Establishment establishment = createEstablishment(organization, "Sushi Place");
		CurrentUser currentUser = currentUser(actor);

		ChecklistDefinition createdDefinition = checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Opening routine for the kitchen",
				List.of(
						new ChecklistDefinitionService.ChecklistTaskInput(
								"Prepare rice cooker",
								"Confirm the rice is prepared for service",
								ChecklistTaskKind.ACTION,
								true,
								0,
								null,
								null,
								null
						)
				),
				List.of(
						new ChecklistDefinitionService.ChecklistScheduleInput(
								ChecklistScheduleType.DAILY,
								LocalDate.of(2026, 3, 26),
								null,
								LocalTime.of(9, 0),
								null,
								null,
								"Europe/Oslo",
								true
						)
				),
				currentUser
		);

		ChecklistDefinition updatedDefinition = checklistDefinitionService.updateChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				createdDefinition.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Updated opening routine for the kitchen",
				ChecklistDefinitionStatus.ACTIVE,
				List.of(
						new ChecklistDefinitionService.ChecklistTaskInput(
								"Prepare rice cooker",
								"Confirm the rice is prepared for service",
								ChecklistTaskKind.ACTION,
								true,
								0,
								null,
								null,
								null
						),
						new ChecklistDefinitionService.ChecklistTaskInput(
								"Record fish fridge temperature",
								"Confirm cold storage is in range",
								ChecklistTaskKind.MEASUREMENT,
								true,
								1,
								"C",
								java.math.BigDecimal.valueOf(0),
								java.math.BigDecimal.valueOf(4)
						)
				),
				List.of(
						new ChecklistDefinitionService.ChecklistScheduleInput(
								ChecklistScheduleType.DAILY,
								LocalDate.of(2026, 3, 26),
								null,
								LocalTime.of(9, 30),
								null,
								null,
								"Europe/Oslo",
								true
						)
				),
				currentUser
		);

		List<ChecklistDefinition> versions = checklistDefinitionService.listChecklistDefinitionVersions(
				organization.getId(),
				establishment.getId(),
				createdDefinition.getId(),
				currentUser
		);

		assertThat(versions).hasSize(2);

		ChecklistDefinition oldVersion = versions.get(0);
		ChecklistDefinition newVersion = versions.get(1);

		assertThat(oldVersion.getId()).isEqualTo(createdDefinition.getId());
		assertThat(oldVersion.getStatus()).isEqualTo(ChecklistDefinitionStatus.SUPERSEDED);
		assertThat(oldVersion.getEffectiveTo()).isNotNull();
		assertThat(newVersion.getId()).isEqualTo(updatedDefinition.getId());
		assertThat(newVersion.getDefinitionGroupId()).isEqualTo(oldVersion.getDefinitionGroupId());
		assertThat(newVersion.getVersionNumber()).isEqualTo(2);
		assertThat(newVersion.getStatus()).isEqualTo(ChecklistDefinitionStatus.ACTIVE);
		assertThat(newVersion.getTasks())
				.extracting(ChecklistTaskDefinition::getTitle)
				.containsExactly("Prepare rice cooker", "Record fish fridge temperature");
		assertThat(newVersion.getSchedules())
				.singleElement()
				.satisfies(schedule -> {
					assertThat(schedule.getTimezone()).isEqualTo("Europe/Oslo");
					assertThat(schedule.getDueTime()).isEqualTo(LocalTime.of(9, 30));
				});
	}

	@Test
	void checklistRunResponseUsesTaskSnapshotsEvenIfDefinitionChangesLater() {
		User actor = createPlatformAdmin("snapshot-admin@example.com");
		Organization organization = createOrganization("Snapshot Org");
		Establishment establishment = createEstablishment(organization, "Snapshot Sushi");
		CurrentUser currentUser = currentUser(actor);

		ChecklistDefinition definition = checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Opening routine",
				List.of(
						new ChecklistDefinitionService.ChecklistTaskInput(
								"Record fridge temperature",
								"Read and record the main fridge",
								ChecklistTaskKind.MEASUREMENT,
								true,
								0,
								"C",
								java.math.BigDecimal.valueOf(0),
								java.math.BigDecimal.valueOf(4)
						)
				),
				List.of(),
				currentUser
		);

		ChecklistRun run = new ChecklistRun(
				definition,
				definition.getDefinitionGroupId(),
				establishment,
				definition.getServiceArea(),
				definition.getTitle(),
				definition.getDescription(),
				Instant.parse("2026-03-26T08:00:00Z"),
				ChecklistRunStatus.PENDING,
				actor
		);
		run.snapshotTasksFromDefinition(definition.getTasks());
		checklistRunRepository.saveAndFlush(run);

		definition.getTasks().getFirst().setTitle("Changed title after publication");
		checklistDefinitionRepository.saveAndFlush(definition);

		ChecklistRun persistedRun = checklistRunRepository.findByIdAndEstablishmentId(run.getId(), establishment.getId())
				.orElseThrow();
		ChecklistRunResponse response = ChecklistRunResponse.from(persistedRun);

		assertThat(response.tasks()).singleElement().satisfies(task -> {
			assertThat(task.title()).isEqualTo("Record fridge temperature");
			assertThat(task.sourceChecklistTaskDefinitionId()).isEqualTo(definition.getTasks().getFirst().getId());
		});
	}

	@Test
	void duplicateChecklistRunAssignmentForSameUserIsRejected() {
		User actor = createPlatformAdmin("assigner@example.com");
		User employee = createActiveUser("employee@example.com", Set.of());
		Organization organization = createOrganization("Assignments Org");
		Establishment establishment = createEstablishment(organization, "Assignments Sushi");
		CurrentUser currentUser = currentUser(actor);

		ChecklistDefinition definition = checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				"Morning shift",
				"Opening routine",
				List.of(
						new ChecklistDefinitionService.ChecklistTaskInput(
								"Prepare sushi rice",
								null,
								ChecklistTaskKind.ACTION,
								true,
								0,
								null,
								null,
								null
						)
				),
				List.of(),
				currentUser
		);

		ChecklistRun run = new ChecklistRun(
				definition,
				definition.getDefinitionGroupId(),
				establishment,
				definition.getServiceArea(),
				definition.getTitle(),
				definition.getDescription(),
				Instant.parse("2026-03-26T07:00:00Z"),
				ChecklistRunStatus.PENDING,
				actor
		);
		run.snapshotTasksFromDefinition(definition.getTasks());
		run.addAssignment(new ChecklistRunAssignment(employee, actor, Instant.parse("2026-03-26T06:45:00Z")));
		run.addAssignment(new ChecklistRunAssignment(employee, actor, Instant.parse("2026-03-26T06:50:00Z")));

		assertThatThrownBy(() -> checklistRunRepository.saveAndFlush(run))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private User createPlatformAdmin(String email) {
		return createActiveUser(email, Set.of(GlobalRole.PLATFORM_ADMIN));
	}

	private User createActiveUser(String email, Set<GlobalRole> roles) {
		User user = new User(email, "Test", "User", "hashed-password", true, roles);
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

	private CurrentUser currentUser(User actor) {
		return new CurrentUser(actor.getId(), actor.getEmail(), actor.getGlobalRoles());
	}
}
