package org.kontrolla.audit.application;

public interface AuditRecorder {

	void record(AuditRecord auditRecord);

	void recordInNewTransaction(AuditRecord auditRecord);
}
