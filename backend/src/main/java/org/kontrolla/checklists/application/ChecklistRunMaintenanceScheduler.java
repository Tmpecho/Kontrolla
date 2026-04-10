package org.kontrolla.checklists.application;

import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.iam.infrastructure.UserRepository;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled maintenance job that generates upcoming checklist runs and marks
 * overdue runs.
 */
@Component
@Profile("!test")
public class ChecklistRunMaintenanceScheduler {

	private static final Logger log = LoggerFactory.getLogger(ChecklistRunMaintenanceScheduler.class);
	private static final long GENERATION_LOOKBACK_DAYS = 1;
	private static final long GENERATION_LOOKAHEAD_DAYS = 60;

	private final EstablishmentRepository establishmentRepository;
	private final UserRepository userRepository;
	private final ChecklistSchedulerService checklistSchedulerService;
	private final ChecklistRunService checklistRunService;

	/**
	 * Creates the checklist maintenance scheduler.
	 *
	 * @param establishmentRepository repository for establishments
	 * @param userRepository repository for users
	 * @param checklistSchedulerService service for scheduled run generation
	 * @param checklistRunService service for checklist run lifecycle updates
	 */
	public ChecklistRunMaintenanceScheduler(
			EstablishmentRepository establishmentRepository,
			UserRepository userRepository,
			ChecklistSchedulerService checklistSchedulerService,
			ChecklistRunService checklistRunService
	) {
		this.establishmentRepository = establishmentRepository;
		this.userRepository = userRepository;
		this.checklistSchedulerService = checklistSchedulerService;
		this.checklistRunService = checklistRunService;
	}

	/**
	 * Performs periodic run generation and overdue maintenance for active
	 * establishments.
	 */
	@Scheduled(initialDelayString = "${app.checklists.overdue.initial-delay-ms:30000}", fixedDelayString = "${app.checklists.overdue.fixed-delay-ms:300000}")
	public void maintainChecklistRuns() {
		Instant now = Instant.now();
		var activeEstablishments = establishmentRepository.findByStatus(EstablishmentStatus.ACTIVE);
		var actor = userRepository.findFirstByActiveTrueOrderByCreatedAtAsc().orElse(null);
		int createdRuns = 0;

		if (actor != null) {
			Instant generationWindowStart = now.minus(GENERATION_LOOKBACK_DAYS, ChronoUnit.DAYS);
			Instant generationWindowEnd = now.plus(GENERATION_LOOKAHEAD_DAYS, ChronoUnit.DAYS);

			createdRuns = activeEstablishments.stream()
					.mapToInt(establishment -> checklistSchedulerService.generateRunsForWindowInternal(
							establishment,
							generationWindowStart,
							generationWindowEnd,
							actor.getId()
					))
					.sum();
		}

		int updatedRuns = activeEstablishments.stream()
				.mapToInt(establishment -> checklistRunService.markOverdueRuns(
						establishment.getOrganization().getId(),
						establishment.getId(),
						now,
						null
				))
				.sum();

		if (createdRuns > 0) {
			log.info("Created {} scheduled checklist runs", createdRuns);
		}

		if (updatedRuns > 0) {
			log.info("Marked {} checklist runs as overdue", updatedRuns);
		}
	}
}
