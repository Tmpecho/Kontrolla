package org.kontrolla.common.api;

import org.kontrolla.common.application.StartupReadinessStatus;

public record StartupStatusResponse(
		StartupReadinessStatus status,
		boolean ready
) {

	public static StartupStatusResponse from(StartupReadinessStatus status) {
		return new StartupStatusResponse(status, status == StartupReadinessStatus.READY);
	}
}
