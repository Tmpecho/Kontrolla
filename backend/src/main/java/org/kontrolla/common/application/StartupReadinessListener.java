package org.kontrolla.common.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for the Spring application-ready event and marks the backend as ready
 * once startup has completed.
 */
@Component
public class StartupReadinessListener {

	private final StartupReadinessService startupReadinessService;

	/**
	 * Creates a listener that updates the shared startup readiness state.
	 *
	 * @param startupReadinessService service used to mark the application as ready
	 */
	public StartupReadinessListener(StartupReadinessService startupReadinessService) {
		this.startupReadinessService = startupReadinessService;
	}

	/**
	 * Handles the application-ready event emitted by Spring after startup has
	 * completed.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		startupReadinessService.markReady();
	}
}
