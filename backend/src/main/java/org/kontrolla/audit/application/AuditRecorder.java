package org.kontrolla.audit.application;

/** Persists audit records and optionally isolates audit persistence in a separate transaction. */
public interface AuditRecorder {

  /**
   * Records an audit event in the current transactional context.
   *
   * @param auditRecord the audit record to persist
   */
  void record(AuditRecord auditRecord);

  /**
   * Records an audit event in a new transaction independent of the caller's current transaction.
   *
   * @param auditRecord the audit record to persist
   */
  void recordInNewTransaction(AuditRecord auditRecord);
}
