package org.kontrolla.checklists.application;

import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.infrastructure.EstablishmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("!test")
public class ChecklistRunMaintenanceScheduler {

	private static final Logger log = LoggerFactory.getLogger(ChecklistRunMaintenanceScheduler.class);

	private final EstablishmentRepository establishmentRepository;
	private final ChecklistRunService checklistRunService;

	public ChecklistRunMaintenanceScheduler(
			EstablishmentRepository establishmentRepository,
			ChecklistRunService checklistRunService
	) {
		this.establishmentRepository = establishmentRepository;
		this.checklistRunService = checklistRunService;
	}

	@Scheduled(initialDelayString = "${app.checklists.overdue.initial-delay-ms:30000}", fixedDelayString = "${app.checklists.overdue.fixed-delay-ms:300000}")
	public void markOverdueRuns() {
		Instant now = Instant.now();
		int updatedRuns = establishmentRepository.findByStatus(EstablishmentStatus.ACTIVE).stream()
				.mapToInt(establishment -> checklistRunService.markOverdueRuns(
						establishment.getOrganization().getId(),
						establishment.getId(),
						now,
						null
				))
				.sum();

		if (updatedRuns > 0) {
			log.info("Marked {} checklist runs as overdue", updatedRuns);
		}
	}
}
