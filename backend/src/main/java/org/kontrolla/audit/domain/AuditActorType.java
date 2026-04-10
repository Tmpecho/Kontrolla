package org.kontrolla.audit.domain;

/** Identifies the type of actor responsible for an audited action. */
public enum AuditActorType {
  ANONYMOUS,
  USER,
  SYSTEM
}
