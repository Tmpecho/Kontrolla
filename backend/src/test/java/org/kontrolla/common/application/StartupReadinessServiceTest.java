package org.kontrolla.common.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
