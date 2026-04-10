package org.kontrolla.common.application;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks whether application startup has completed so readiness checks can
 * distinguish between a booting and fully ready backend instance.
 */
@Service
public class StartupReadinessService {

	private final AtomicReference<StartupReadinessStatus> status =
			new AtomicReference<>(StartupReadinessStatus.STARTING);

	/**
	 * Returns the current startup readiness state for the application.
	 *
	 * @return the current readiness status
	 */
	public StartupReadinessStatus getStatus() {
		return status.get();
	}

	/**
	 * Indicates whether the application has finished startup and is ready to
	 * serve requests.
	 *
	 * @return {@code true} when the readiness status is {@code READY}
	 */
	public boolean isReady() {
		return getStatus() == StartupReadinessStatus.READY;
	}

	/**
	 * Marks the application as fully started and ready.
	 */
	public void markReady() {
		status.set(StartupReadinessStatus.READY);
	}
}
