package org.kontrolla.checklists.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
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
import org.kontrolla.support.TestDataCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChecklistSchedulerServiceIntegrationTest {

	private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

	@Autowired
	private ChecklistSchedulerService checklistSchedulerService;

	@Autowired
	private ChecklistDefinitionService checklistDefinitionService;

	@Autowired
	private ChecklistRunRepository checklistRunRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private EstablishmentRepository establishmentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestDataCleaner testDataCleaner;

	@BeforeEach
	void setUp() {
		testDataCleaner.clearAll();
	}

	@Test
	void generateRunsForWindowCreatesMatchingRunsAndSkipsDuplicates() {
		User actor = createPlatformAdmin("scheduler-admin@example.com");
		Organization organization = createOrganization("Scheduler Org");
		Establishment establishment = createEstablishment(organization, "Scheduler Restaurant");
		CurrentUser currentUser = currentUser(actor);
		LocalDate dueDate = LocalDate.of(2026, 3, 26);

		createDefinition(
				organization,
				establishment,
				actor,
				"Daily opening",
				new ChecklistDefinitionService.ChecklistScheduleInput(
						ChecklistScheduleType.DAILY,
						dueDate.minusDays(7),
						null,
						LocalTime.of(9, 0),
						null,
						null,
						OSLO.getId(),
						true
				)
		);
		createDefinition(
				organization,
				establishment,
				actor,
				"Weekly handover",
				new ChecklistDefinitionService.ChecklistScheduleInput(
						ChecklistScheduleType.WEEKLY,
						dueDate.minusDays(7),
						null,
						LocalTime.of(10, 0),
						weekdayMaskFor(dueDate),
						null,
						OSLO.getId(),
						true
				)
		);
		createDefinition(
				organization,
				establishment,
				actor,
				"One-off inspection",
				new ChecklistDefinitionService.ChecklistScheduleInput(
						ChecklistScheduleType.ONE_OFF,
						dueDate,
						null,
						LocalTime.of(11, 0),
						null,
						null,
						OSLO.getId(),
						true
				)
		);
		createDefinition(
				organization,
				establishment,
				actor,
				"Inactive routine",
				new ChecklistDefinitionService.ChecklistScheduleInput(
						ChecklistScheduleType.DAILY,
						dueDate.minusDays(7),
						null,
						LocalTime.of(12, 0),
						null,
						null,
						OSLO.getId(),
						false
				)
		);

		Instant windowStart = atOslo(dueDate, LocalTime.MIN);
		Instant windowEnd = atOslo(dueDate, LocalTime.of(23, 59));

		int createdRuns = checklistSchedulerService.generateRunsForWindow(
				organization.getId(),
				establishment.getId(),
				windowStart,
				windowEnd,
				currentUser
		);

		assertThat(createdRuns).isEqualTo(3);
		assertThat(checklistRunRepository.findAll())
				.extracting(ChecklistRun::getTitleSnapshot)
				.containsExactlyInAnyOrder("Daily opening", "Weekly handover", "One-off inspection");
		assertThat(checklistRunRepository.findAll().stream()
				.map(ChecklistRun::getDueAt)
				.sorted()
				.toList())
				.containsExactly(
						atOslo(dueDate, LocalTime.of(9, 0)),
						atOslo(dueDate, LocalTime.of(10, 0)),
						atOslo(dueDate, LocalTime.of(11, 0))
				);

		int duplicateRuns = checklistSchedulerService.generateRunsForWindow(
				organization.getId(),
				establishment.getId(),
				windowStart,
				windowEnd,
				currentUser
		);

		assertThat(duplicateRuns).isZero();
		assertThat(checklistRunRepository.count()).isEqualTo(3);
	}

	@Test
	void generateRunsForWindowNormalizesMonthlyScheduleToLastDayOfShortMonth() {
		User actor = createPlatformAdmin("scheduler-monthly@example.com");
		Organization organization = createOrganization("Monthly Org");
		Establishment establishment = createEstablishment(organization, "Monthly Restaurant");
		CurrentUser currentUser = currentUser(actor);
		LocalDate dueDate = LocalDate.of(2026, 4, 30);

		createDefinition(
				organization,
				establishment,
				actor,
				"Month-end freezer check",
				new ChecklistDefinitionService.ChecklistScheduleInput(
						ChecklistScheduleType.MONTHLY,
						LocalDate.of(2026, 1, 1),
						null,
						LocalTime.of(8, 30),
						null,
						31,
						OSLO.getId(),
						true
				)
		);

		int createdRuns = checklistSchedulerService.generateRunsForWindow(
				organization.getId(),
				establishment.getId(),
				atOslo(dueDate, LocalTime.MIN),
				atOslo(dueDate, LocalTime.of(23, 59)),
				currentUser
		);

		assertThat(createdRuns).isEqualTo(1);
		assertThat(checklistRunRepository.findAll())
				.singleElement()
				.satisfies(run -> assertThat(run.getDueAt()).isEqualTo(atOslo(dueDate, LocalTime.of(8, 30))));
	}

	@Test
	void markOverdueRunsTransitionsOnlyPendingAndInProgressRuns() {
		User actor = createPlatformAdmin("scheduler-overdue@example.com");
		Organization organization = createOrganization("Overdue Org");
		Establishment establishment = createEstablishment(organization, "Overdue Restaurant");
		CurrentUser currentUser = currentUser(actor);
		ChecklistDefinition definition = createDefinition(organization, establishment, actor, "Overdue routine", null);
		Instant now = Instant.parse("2026-03-26T10:00:00Z");

		ChecklistRun pendingRun = createRun(
				definition,
				establishment,
				actor,
				Instant.parse("2026-03-26T08:00:00Z"),
				ChecklistRunStatus.PENDING
		);
		ChecklistRun inProgressRun = createRun(
				definition,
				establishment,
				actor,
				Instant.parse("2026-03-26T08:30:00Z"),
				ChecklistRunStatus.IN_PROGRESS
		);
		ChecklistRun overdueRun = createRun(
				definition,
				establishment,
				actor,
				Instant.parse("2026-03-26T07:30:00Z"),
				ChecklistRunStatus.OVERDUE
		);
		ChecklistRun completedRun = createRun(
				definition,
				establishment,
				actor,
				Instant.parse("2026-03-26T07:00:00Z"),
				ChecklistRunStatus.COMPLETED
		);
		ChecklistRun futureRun = createRun(
				definition,
				establishment,
				actor,
				Instant.parse("2026-03-26T11:00:00Z"),
				ChecklistRunStatus.PENDING
		);

		int updatedRuns = checklistSchedulerService.markOverdueRuns(
				organization.getId(),
				establishment.getId(),
				now,
				currentUser
		);

		assertThat(updatedRuns).isEqualTo(2);
		List<ChecklistRun> persistedRuns = checklistRunRepository.findAll().stream()
				.sorted(Comparator.comparing(ChecklistRun::getDueAt))
				.toList();

		assertThat(findStatus(persistedRuns, pendingRun.getId())).isEqualTo(ChecklistRunStatus.OVERDUE);
		assertThat(findStatus(persistedRuns, inProgressRun.getId())).isEqualTo(ChecklistRunStatus.OVERDUE);
		assertThat(findStatus(persistedRuns, overdueRun.getId())).isEqualTo(ChecklistRunStatus.OVERDUE);
		assertThat(findStatus(persistedRuns, completedRun.getId())).isEqualTo(ChecklistRunStatus.COMPLETED);
		assertThat(findStatus(persistedRuns, futureRun.getId())).isEqualTo(ChecklistRunStatus.PENDING);
	}

	private ChecklistDefinition createDefinition(
			Organization organization,
			Establishment establishment,
			User actor,
			String title,
			ChecklistDefinitionService.ChecklistScheduleInput schedule
	) {
		return checklistDefinitionService.createChecklistDefinition(
				organization.getId(),
				establishment.getId(),
				ChecklistServiceArea.IK_MAT,
				title,
				"Scheduler coverage routine",
				List.of(new ChecklistDefinitionService.ChecklistTaskInput(
						"Check cold storage",
						"Record and verify temperature",
						ChecklistTaskKind.MEASUREMENT,
						true,
						0,
						"C",
						java.math.BigDecimal.ZERO,
						java.math.BigDecimal.valueOf(4)
				)),
				schedule == null ? List.of() : List.of(schedule),
				currentUser(actor)
		);
	}

	private ChecklistRun createRun(
			ChecklistDefinition definition,
			Establishment establishment,
			User actor,
			Instant dueAt,
			ChecklistRunStatus status
	) {
		ChecklistRun run = new ChecklistRun(
				definition,
				definition.getDefinitionGroupId(),
				establishment,
				definition.getServiceArea(),
				definition.getTitle(),
				definition.getDescription(),
				dueAt,
				status,
				actor
		);
		run.snapshotTasksFromDefinition(definition.getTasks());
		if (status == ChecklistRunStatus.IN_PROGRESS) {
			run.setStartedAt(dueAt.minusSeconds(600));
		}
		if (status == ChecklistRunStatus.COMPLETED) {
			run.setCompletedAt(dueAt.plusSeconds(300));
			run.setCompletedByUser(actor);
		}
		return checklistRunRepository.saveAndFlush(run);
	}

	private ChecklistRunStatus findStatus(List<ChecklistRun> runs, java.util.UUID id) {
		return runs.stream()
				.filter(run -> run.getId().equals(id))
				.findFirst()
				.orElseThrow()
				.getStatus();
	}

	private int weekdayMaskFor(LocalDate date) {
		return 1 << (date.getDayOfWeek().getValue() - 1);
	}

	private Instant atOslo(LocalDate date, LocalTime time) {
		return date.atTime(time).atZone(OSLO).toInstant();
	}

	private User createPlatformAdmin(String email) {
		User user = new User(email, "Scheduler", "Admin", "hashed-password", true, Set.of(GlobalRole.PLATFORM_ADMIN));
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

	private CurrentUser currentUser(User user) {
		return new CurrentUser(user.getId(), user.getEmail(), user.getGlobalRoles());
	}
}
