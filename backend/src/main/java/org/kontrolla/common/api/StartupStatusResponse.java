package org.kontrolla.common.api;

import org.kontrolla.common.application.StartupReadinessStatus;

/**
 * Response payload describing the backend startup state.
 *
 * @param status the current startup status enum value
 * @param ready whether the backend is ready to serve requests
 */
public record StartupStatusResponse(
		StartupReadinessStatus status,
		boolean ready
) {

	/**
	 * Creates a response payload from the current startup readiness status.
	 *
	 * @param status the startup readiness status to expose
	 * @return the corresponding API response
	 */
	public static StartupStatusResponse from(StartupReadinessStatus status) {
		return new StartupStatusResponse(status, status == StartupReadinessStatus.READY);
	}
}
