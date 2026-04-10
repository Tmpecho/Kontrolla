package org.kontrolla.common.api;

import org.kontrolla.common.application.StartupReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class StartupStatusController {

	private final StartupReadinessService startupReadinessService;

	public StartupStatusController(StartupReadinessService startupReadinessService) {
		this.startupReadinessService = startupReadinessService;
	}

	@GetMapping("/startup-status")
	public StartupStatusResponse getStartupStatus() {
		return StartupStatusResponse.from(startupReadinessService.getStatus());
	}
}
