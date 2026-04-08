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

@Service
public class TransactionalAuditRecorder implements AuditRecorder {

	private static final Logger auditLog = LoggerFactory.getLogger("org.kontrolla.audit");
	private static final Logger log = LoggerFactory.getLogger(TransactionalAuditRecorder.class);

	private final AuditEventRepository auditEventRepository;
	private final AuditRequestContextResolver auditRequestContextResolver;
	private final ObjectMapper objectMapper;

	public TransactionalAuditRecorder(
			AuditEventRepository auditEventRepository,
			AuditRequestContextResolver auditRequestContextResolver,
			ObjectMapper objectMapper
	) {
		this.auditEventRepository = auditEventRepository;
		this.auditRequestContextResolver = auditRequestContextResolver;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void record(AuditRecord auditRecord) {
		persistAndSchedule(auditRecord);
	}

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
				writeJson(resolvedAuditRecord.metadata())
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

		auditLog.info(writeJson(payload));
	}

	private String writeJson(Map<String, ?> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			log.error("Failed to serialize audit payload", exception);
			throw new IllegalStateException("Failed to serialize audit payload", exception);
		}
	}
}
