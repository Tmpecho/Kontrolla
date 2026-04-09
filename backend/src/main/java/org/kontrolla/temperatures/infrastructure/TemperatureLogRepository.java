package org.kontrolla.temperatures.infrastructure;

import org.kontrolla.temperatures.domain.TemperatureLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TemperatureLogRepository extends JpaRepository<TemperatureLog, UUID> {
}
