package org.kontrolla.common.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StartupReadinessServiceTest {

  @Test
  void startsInStartingStateAndCanBeMarkedReady() {
    StartupReadinessService startupReadinessService = new StartupReadinessService();

    assertThat(startupReadinessService.getStatus()).isEqualTo(StartupReadinessStatus.STARTING);
    assertThat(startupReadinessService.isReady()).isFalse();

    startupReadinessService.markReady();

    assertThat(startupReadinessService.getStatus()).isEqualTo(StartupReadinessStatus.READY);
    assertThat(startupReadinessService.isReady()).isTrue();
  }
}
