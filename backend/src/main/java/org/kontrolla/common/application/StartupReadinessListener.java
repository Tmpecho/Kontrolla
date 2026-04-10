package org.kontrolla.common.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupReadinessListener {

	private final StartupReadinessService startupReadinessService;

	public StartupReadinessListener(StartupReadinessService startupReadinessService) {
		this.startupReadinessService = startupReadinessService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		startupReadinessService.markReady();
	}
}
