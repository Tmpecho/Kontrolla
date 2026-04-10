package org.kontrolla.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kontrolla.audit.domain.AuditEvent;
import org.kontrolla.audit.infrastructure.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default audit recorder that persists audit events and emits a structured log
 * entry after transaction commit.
 */
@Service
public class TransactionalAuditRecorder implements AuditRecorder {

	private static final Logger auditLog = LoggerFactory.getLogger("org.kontrolla.audit");
	private static final Logger log = LoggerFactory.getLogger(TransactionalAuditRecorder.class);

	private final AuditEventRepository auditEventRepository;
	private final AuditRequestContextResolver auditRequestContextResolver;
	private final ObjectMapper objectMapper;

	/**
	 * Creates an audit recorder backed by the audit event repository.
	 *
	 * @param auditEventRepository repository used to persist audit events
	 * @param auditRequestContextResolver resolver that enriches records with
	 * request context
	 * @param objectMapper object mapper used to serialize audit payloads
	 */
	public TransactionalAuditRecorder(
			AuditEventRepository auditEventRepository,
			AuditRequestContextResolver auditRequestContextResolver,
			ObjectMapper objectMapper
	) {
		this.auditEventRepository = auditEventRepository;
		this.auditRequestContextResolver = auditRequestContextResolver;
		this.objectMapper = objectMapper;
	}

	/**
	 * Records an audit event inside the caller's current transaction.
	 *
	 * @param auditRecord the audit record to persist
	 */
	@Override
	@Transactional
	public void record(AuditRecord auditRecord) {
		persistAndSchedule(auditRecord);
	}

	/**
	 * Records an audit event in a new transaction isolated from the caller.
	 *
	 * @param auditRecord the audit record to persist
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordInNewTransaction(AuditRecord auditRecord) {
		persistAndSchedule(auditRecord);
	}

	private void persistAndSchedule(AuditRecord auditRecord) {
		ResolvedAuditRecord resolvedAuditRecord = auditRequestContextResolver.resolve(auditRecord);
		AuditEvent auditEvent = auditEventRepository.save(new AuditEvent(
				resolvedAuditRecord.action(),
				resolvedAuditRecord.outcome(),
				resolvedAuditRecord.occurredAt(),
				resolvedAuditRecord.actorType(),
				resolvedAuditRecord.actorUserId(),
				resolvedAuditRecord.actorEmail(),
				resolvedAuditRecord.organizationId(),
				resolvedAuditRecord.targetType(),
				resolvedAuditRecord.targetId(),
				resolvedAuditRecord.requestMethod(),
				resolvedAuditRecord.requestPath(),
				resolvedAuditRecord.clientIp(),
				resolvedAuditRecord.userAgent(),
				resolvedAuditRecord.resultCode(),
				writeJsonOrThrow(resolvedAuditRecord.metadata())
		));
		scheduleLogEmission(auditEvent, resolvedAuditRecord.metadata());
	}

	private void scheduleLogEmission(AuditEvent auditEvent, Map<String, Object> metadata) {
		Runnable emitLogRunnable = () -> emitLog(auditEvent, metadata);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			emitLogRunnable.run();
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				emitLogRunnable.run();
			}
		});
	}

	private void emitLog(AuditEvent auditEvent, Map<String, Object> metadata) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("eventId", auditEvent.getId());
		payload.put("action", auditEvent.getAction());
		payload.put("outcome", auditEvent.getOutcome());
		payload.put("occurredAt", auditEvent.getOccurredAt());
		payload.put("actorType", auditEvent.getActorType());
		payload.put("actorUserId", auditEvent.getActorUserId());
		payload.put("actorEmail", auditEvent.getActorEmail());
		payload.put("organizationId", auditEvent.getOrganizationId());
		payload.put("targetType", auditEvent.getTargetType());
		payload.put("targetId", auditEvent.getTargetId());
		payload.put("requestMethod", auditEvent.getRequestMethod());
		payload.put("requestPath", auditEvent.getRequestPath());
		payload.put("clientIp", auditEvent.getClientIp());
		payload.put("userAgent", auditEvent.getUserAgent());
		payload.put("resultCode", auditEvent.getResultCode());
		payload.put("metadata", metadata);

		try {
			auditLog.info(writeJsonOrThrow(payload));
		} catch (RuntimeException exception) {
			log.error("Failed to emit audit log for event {}", auditEvent.getId(), exception);
		}
	}

	private String writeJsonOrThrow(Map<String, ?> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			log.error("Failed to serialize audit payload", exception);
			throw new IllegalStateException("Failed to serialize audit payload", exception);
		}
	}
}
