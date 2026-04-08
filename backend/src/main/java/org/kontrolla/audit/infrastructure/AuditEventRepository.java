package org.kontrolla.audit.infrastructure;

import org.kontrolla.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
