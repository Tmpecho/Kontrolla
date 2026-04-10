package org.kontrolla.common.api;

import org.kontrolla.common.application.StartupReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes API endpoints that report whether backend startup has completed. */
@RestController
@RequestMapping("/api/v1/system")
public class StartupStatusController {

  private final StartupReadinessService startupReadinessService;

  /**
   * Creates a controller backed by the shared startup readiness service.
   *
   * @param startupReadinessService service that tracks current startup status
   */
  public StartupStatusController(StartupReadinessService startupReadinessService) {
    this.startupReadinessService = startupReadinessService;
  }

  /**
   * Returns the current startup readiness state for the backend.
   *
   * @return the startup status response
   */
  @GetMapping("/startup-status")
  public StartupStatusResponse getStartupStatus() {
    return StartupStatusResponse.from(startupReadinessService.getStatus());
  }
}
