package org.kontrolla.temperatures.infrastructure;

import java.util.UUID;
import org.kontrolla.temperatures.domain.TemperatureLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for temperature log entries. */
public interface TemperatureLogRepository extends JpaRepository<TemperatureLog, UUID> {}
