package org.kontrolla.common.application;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class StartupReadinessService {

	private final AtomicReference<StartupReadinessStatus> status =
			new AtomicReference<>(StartupReadinessStatus.STARTING);

	public StartupReadinessStatus getStatus() {
		return status.get();
	}

	public boolean isReady() {
		return getStatus() == StartupReadinessStatus.READY;
	}

	public void markReady() {
		status.set(StartupReadinessStatus.READY);
	}
}
