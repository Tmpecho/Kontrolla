package org.kontrolla.audit.infrastructure;

import java.util.UUID;
import org.kontrolla.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for persisted audit events. */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {}
